package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.mailbox.AskMessage
import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.mailbox.PendingRequests
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

class ActorPointer<M : Any>(private val actorId: String, private val executor: ActorExecutor<M>) {
    private val log = loggerFor(this::class.java)

    fun getAddress(): String {
        return "<${actorId.lowercase()}>"
    }

    suspend fun send(message: M) {
        log.debug("[ActorSystem] Sending message: {} to actor: {}", message, actorId)
        executor.send(message)
    }

    suspend fun <R : Any> ask(message: M): R {
        val requestId = UUID.randomUUID().toString()
        val responseDeferred = PendingRequests.createRequest<R>(requestId)

        val requestMessage = AskMessage(requestId, message)
        log.debug("[ActorSystem] Sending request message: {} to actor: {}", message, actorId)

        executor.send(requestMessage as M)

        return responseDeferred.await()
    }
}