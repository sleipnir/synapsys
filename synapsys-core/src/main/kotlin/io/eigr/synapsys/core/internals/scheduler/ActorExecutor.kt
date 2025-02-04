package io.eigr.synapsys.core.internals.scheduler

import io.eigr.synapsys.core.internals.BaseActor
import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.mailbox.AskMessage
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.PendingRequests
import io.eigr.synapsys.core.internals.supervisor.SupervisorMessage
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * The `ActorExecutor` class is responsible for managing the execution of an actor
 * within the system. It interacts with the actor's mailbox, processes messages,
 * and handles execution flow, including suspension and resumption.
 *
 * @param M The type of messages that this executor processes.
 * @property _actor The instance of the actor being executed.
 * @property mailbox The mailbox that holds messages for the actor.
 * @property supervisorChannel Optional channel to communicate with a supervisor in case of failures.
 */
class ActorExecutor<M : Any>(
    private val _actor: BaseActor,
    private val _mailbox: Mailbox<M>,
    private val supervisorChannel: Channel<SupervisorMessage>? = null
) {
    private val log = loggerFor(this::class.java)
    private var continuation: Continuation<Unit>? = null

    var isActive: Boolean = true

    /**
     * Retrieves the actor instance managed by this executor.
     */
    val actor: BaseActor get() = this._actor

    val mailbox: Mailbox<M> get() = this._mailbox

    /**
     * Sends a message to the actor's mailbox.
     *
     * @param message The message to be sent to the actor.
     */
    suspend fun send(message: M) {
        log.debug("[ActorExecutor] Actor {} sending message: {}", _actor.id, message)
        _mailbox.send(message)
    }

    /**
     * @internal
     * Retrieves the next message from the mailbox, if available.
     *
     * @return The next message in the mailbox, or `null` if empty.
     */
    internal suspend fun dequeueMessage(): Any? = _mailbox.receive()

    /**
     * @internal
     * Checks whether the actor's mailbox has pending messages.
     *
     * @return `true` if there are messages in the mailbox, `false` otherwise.
     */
    internal fun hasMessages(): Boolean = mailbox.hasMessages()

    /**
     * @internal
     * Processes a received message and invokes the corresponding actor method.
     *
     * This method handles both normal messages and request-response (`AskMessage`) messages.
     * If an exception occurs during processing, the failure is reported to the supervisor channel.
     *
     * @param message The message to be processed.
     */
    internal suspend fun processMessage(message: Any) {
        log.debug(
            "[ActorExecutor] Dispatching message to Actor {}. Message: {}",
            _actor.id,
            message
        )
        try {
            when (message) {
                is AskMessage<*> -> {
                    val (id, msg) = message

                    val (result, newState) = _actor.processMessageUntyped(
                        msg,
                        _actor.getState()
                    )

                    log.debug(
                        "[ActorExecutor] Actor {} processed ask message: {}, result: {}, current state: {}",
                        _actor.id,
                        message,
                        result,
                        newState
                    )

                    PendingRequests.completeRequest(id, result)
                }

                else -> {
                    val (result, newState) = _actor.processMessageUntyped(
                        message,
                        _actor.getState()
                    )
                    log.trace(
                        "[ActorExecutor] Actor {} processed normal message: {}, result: {}, current state: {}",
                        _actor.id,
                        message,
                        result,
                        newState
                    )
                }
            }
        } catch (e: Exception) {
            log.error(
                "[ActorExecutor] Actor {} failed to process message: {}",
                _actor.id,
                message,
                e
            )
            if (message is AskMessage<*>) {
                PendingRequests.failRequest(message.id, e)
            }
            isActive = false
            supervisorChannel?.send(SupervisorMessage.ActorFailed(this, e))
        }
    }

    /**
     * @internal
     * Suspends the execution of the actor.
     *
     * The actor will remain in a suspended state until explicitly resumed.
     */
    internal suspend fun suspendExecution() {
        suspendCoroutine { cont ->
            continuation = cont
            log.trace("[ActorExecutor] Actor {} suspended.", _actor.id)
        }
    }

    /**
     * @internal
     * Resumes the execution of the actor if it was previously suspended.
     *
     * If the actor was not suspended, a warning is logged.
     */
    internal fun resumeExecution() {
        continuation?.resume(Unit)
            ?: log.trace(
                "[ActorExecutor] WARNING: Tried to resume actor {} but it was not suspended.",
                _actor.id
            )
        continuation = null
        log.trace("[ActorExecutor] Actor {} resumed.", _actor.id)
    }
}

