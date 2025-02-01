package io.eigr.synapsys.core.internals.scheduler

import io.eigr.synapsys.core.Supervisor
import io.eigr.synapsys.core.internals.mailbox.AskMessage
import io.eigr.synapsys.core.internals.BaseActor
import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.mailbox.Mailbox
import io.eigr.synapsys.core.internals.mailbox.PendingRequests
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ActorExecutor<M : Any>(
    private val _actor: BaseActor,
    private val mailbox: Mailbox<M>,
    private val _supervisor: Supervisor? = null
) {
    private val log = loggerFor(this::class.java)
    private var continuation: Continuation<Unit>? = null

    val actor: BaseActor get() = this._actor

    val supervisor: Supervisor? get() = this._supervisor

    suspend fun send(message: M) {
        log.debug("[ActorExecutor] Actor {} sending message: {}", _actor.id, message)
        mailbox.send(message)
    }

    internal suspend fun dequeueMessage(): Any? = mailbox.receive()

    internal fun hasMessages(): Boolean = mailbox.hasMessages()

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
            //supervisor?.handleFailure(actor, e)
        }
    }

    internal suspend fun suspendExecution() {
        suspendCoroutine { cont ->
            continuation = cont
            log.trace("[ActorExecutor] Actor {} suspended.", _actor.id)
        }
    }

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

