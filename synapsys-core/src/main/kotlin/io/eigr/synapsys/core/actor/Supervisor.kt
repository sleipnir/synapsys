package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.Scheduler
import io.eigr.synapsys.core.internals.supervisor.SupervisorMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.pow
import kotlin.random.Random

data class ActorEntity<S : Any, M : Any, R : Any>(
    val actorExecutor: ActorExecutor<*>,
    val factory: (String, S) -> Actor<S, M, R>
)

/**
 * Hierarchical fault tolerance component implementing supervision strategies for actor systems.
 * Manages child actor lifecycles and implements failure recovery policies.
 *
 * <h2>Key Responsibilities:</h2>
 * <ul>
 *   <li>Failure detection and recovery of child actors</li>
 *   <li>Implementation of configurable restart strategies</li>
 *   <li>Exponential backoff calculation for restarts</li>
 *   <li>Supervision hierarchy management</li>
 * </ul>
 *
 * Architecture Notes:
 *
 * - Implements let-it-crash philosophy with controlled recovery
 * - Uses coroutine scopes for async failure handling
 * - Maintains separation between supervision logic and actor implementation
 * - Follows hierarchical error handling patterns from Erlang/OTP
 * - Provides configurable jitter-free exponential backoff
 * - Maybe supports multiple supervision topologies through strategy pattern
 *
 * @property id Unique supervisor identifier
 * @property strategy Configured supervision policy
 * @property parent Optional parent supervisor for escalation
 *
 * @see SupervisorStrategy
 * @see RestartStrategy
 * @see ActorExecutor
 */
@Suppress("UNCHECKED_CAST")
class Supervisor(
    val id: String,
    private val strategy: SupervisorStrategy = SupervisorStrategy(),
    private val parent: Supervisor? = null
) {
    private val log = loggerFor(this::class.java)
    private val children = ConcurrentHashMap<String, ActorEntity<*, *, *>>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val failureCount = mutableMapOf<String, Int>()
    private val messageChannel = Channel<SupervisorMessage>()
    private lateinit var config: Config
    private lateinit var scheduler: Scheduler

    init {
        scope.launch {
            for (message in messageChannel) {
                when (message) {
                    is SupervisorMessage.ActorFailed ->
                        handleFailure(message.actorExecutor, message.exception)
                }
            }
        }
    }

    /**
     * Configures system-wide settings for child management
     * @param config Runtime configuration parameters
     */
    fun setConfig(config: Config) {
        this.config = config
    }

    /**
     * Links to the system scheduler for actor lifecycle management
     * @param scheduler Reference to the system scheduler
     */
    fun setScheduler(scheduler: Scheduler) {
        this.scheduler = scheduler
    }

    /**
     * @internal
     * Provides access to the supervisor's message channel for failure notifications
     */
    fun getMessageChannel(): Channel<SupervisorMessage> = messageChannel

    /**
     * Registers a new child actor under this supervisor's jurisdiction
     * @param actorExecutor Actor's execution component
     * @param factory Factory method for actor recreation
     */
    fun <S : Any, M : Any, R : Any> addChild(actorExecutor: ActorExecutor<*>, factory: Any) {
        children[actorExecutor.actor.id] =
            ActorEntity(actorExecutor, factory as (String, S) -> Actor<S, M, R>)
    }

    /**
     * @internal
     * Handles actor failure events and executes recovery logic
     */
    private fun handleFailure(failedActor: ActorExecutor<*>, exception: Throwable) {
        val actorId = failedActor.actor.id
        val retries = failureCount.getOrPut(actorId) {0}

        if (retries >= strategy.estimatedMaxRetries) {
            log.error("[{}] Actor {} exceeded max retries. Removing from system.", id, actorId)
            children.remove(failedActor.actor.id)
            scheduler.removeActor(actorId)
            failureCount.remove(actorId)
            return
        }

        val backoff = calculateBackoff(retries)
        failureCount[actorId] = retries + 1

        scope.launch {
            log.warn(
                "[{}] Restarting actor {} in {}ms due to failure: {}",
                id,
                actorId,
                backoff,
                exception.message
            )
            delay(backoff)
            restartActor(failedActor)
        }
    }

    /**
     * Calculates exponential backoff delay
     * @param retries Current retry attempt count
     * @return Calculated delay in milliseconds
     */
    private fun calculateBackoff(retries: Int): Long {
        val magicNumber = Random.nextInt(0, retries + 1)
        val baseBackoff = strategy.initialBackoffMillis * 2.0.pow(magicNumber.toDouble())
        val randomFactor = Random.nextDouble(0.5, 1.5)
        val backoff = (baseBackoff * randomFactor).toLong()
        return minOf(backoff, strategy.maxBackoffMillis)
    }

    /**
     * @internal
     * Executes configured restart strategy for failed actors
     */
    private suspend fun restartActor(actorExecutor: ActorExecutor<*>) {
        when (strategy.kind) {
            RestartStrategy.OneForOne -> restart(actorExecutor)

            RestartStrategy.AllForOne -> {
                log.warn("[$id] Restarting all children due to failure.")
                children.values.forEach { restart(it.actorExecutor) }
            }

            RestartStrategy.Escalate -> TODO()
        }
    }

    /**
     * @internal
     * Performs individual actor restart with state preservation
     */
    private suspend fun restart(actorExecutor: ActorExecutor<*>) {
        val actorId = actorExecutor.actor.id
        val oldInstance = children[actorId]

        try {
            if (oldInstance != null) {
                log.debug("[{}] Restarting actor {}", id, actorId)

                // create new instance
                val newActorExecutor = ActorSystem.createActorExecutor(
                    id = actorId,
                    initialState = actorExecutor.actor.getState()!!,
                    config = config,
                    supervisor = this,
                    actorFactory = oldInstance.factory as (String, Any) -> Actor<Any, Any, Any>,
                    mailbox = oldInstance.actorExecutor.mailbox,
                )
                newActorExecutor.isActive = true

                // rehydrate state of actor
                newActorExecutor.actor.rehydrate()

                // enqueue
                children[actorId] = ActorEntity(newActorExecutor, oldInstance.factory)
                scheduler.removeActor(actorId)
                scheduler.enqueue(newActorExecutor)
            }
        }catch (e: Exception) {
            parent?.addChild<Any, Any, Any>(actorExecutor, oldInstance!!.factory)
            escalateFailure(actorExecutor, e)
        }
    }

    /**
     * @internal
     * Propagates failure to parent supervisor hierarchy
     */
    private fun escalateFailure(actor: ActorExecutor<*>, exception: Throwable) {
        parent?.handleFailure(actor, exception)
    }
}
