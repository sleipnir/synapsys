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

data class ActorEntity<S : Any, M : Any, R : Any>(
    val actorExecutor: ActorExecutor<*>,
    val factory: (String, S) -> Actor<S, M, R>
)

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

    fun setConfig(config: Config) {
        this.config = config
    }

    fun setScheduler(scheduler: Scheduler) {
        this.scheduler = scheduler
    }

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

    fun getMessageChannel(): Channel<SupervisorMessage> = messageChannel

    fun <S : Any, M : Any, R : Any> addChild(actorExecutor: ActorExecutor<*>, factory: Any) {
        children[actorExecutor.actor.id] =
            ActorEntity(actorExecutor, factory as (String, S) -> Actor<S, M, R>)
    }

    private fun handleFailure(failedActor: ActorExecutor<*>, exception: Throwable) {
        val actorId = failedActor.actor.id
        val retries = failureCount.getOrDefault(actorId, 0)

        if (retries >= strategy.estimatedMaxRetries) {
            log.error("[$id] Actor {} exceeded max retries. Removing from system.", actorId)
            children.remove(failedActor.actor.id)
            scheduler.removeActor(actorId)
            return
        }

        val backoff = calculateBackoff(retries)
        log.warn(
            "[$id] Restarting actor {} in {}ms due to failure: {}",
            actorId,
            backoff,
            exception.message
        )

        failureCount[actorId] = retries + 1

        scope.launch {
            delay(backoff)
            restartActor(failedActor)
        }
    }

    private fun calculateBackoff(retries: Int): Long {
        val backoff = strategy.initialBackoffMillis * 2.0.pow(retries.toDouble()).toLong()
        return minOf(backoff, strategy.maxBackoffMillis)
    }

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

    private suspend fun restart(actorExecutor: ActorExecutor<*>) {
        val actorId = actorExecutor.actor.id
        scheduler.removeActor(actorId)

        val oldInstance = children[actorId]
        if (oldInstance != null) {
            log.info("[$id] Restarting actor {}", actorId)

            // create new instance
            val newActorExecutor = ActorSystem.createActorExecutor(
                actorId,
                actorExecutor.actor.getState()!!,
                config,
                this,
                oldInstance.factory as (String, Any) -> Actor<Any, Any, Any>
            )

            // enqueue
            children[actorId] = ActorEntity(actorExecutor, oldInstance.factory)
            scheduler.enqueue(newActorExecutor)
        }
    }

    private fun escalateFailure(actor: ActorExecutor<*>, exception: Throwable) {
        parent?.handleFailure(actor, exception)
    }
}
