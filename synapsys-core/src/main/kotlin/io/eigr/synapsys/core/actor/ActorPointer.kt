package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.mailbox.AskMessage
import io.eigr.synapsys.core.internals.mailbox.PendingRequests
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import java.util.UUID

/**
 * Reference handle for interacting with actors in the Synapsys framework.
 * Provides mechanisms for both fire-and-forget and request-response message patterns.
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Type-safe message sending</li>
 *   <li>Asynchronous request-response support</li>
 *   <li>Actor addressing abstraction</li>
 * </ul>
 *
 * @param M Type of messages this pointer can send
 * @property actorId Unique identifier of the target actor
 * @property executor Internal message routing component
 *
 * @see ActorExecutor
 * @see AskMessage
 * @see PendingRequests
 */
class ActorPointer<M : Any>(private val actorId: String, private val executor: ActorExecutor<M>) {
    private val log = loggerFor(this::class.java)

    /**
     * Retrieves the actor's canonical address in the system.
     * @return Formatted address string using actor ID (lowercase enclosed in angle brackets)
     *
     * @sample
     * val address = pointer.getAddress() // Returns "<counter-actor>"
     */
    fun getAddress(): String {
        return "<${actorId.lowercase()}>"
    }

    /**
     * Sends a message to the actor without expecting a response (fire-and-forget pattern).
     *
     * @param message Message to send to the actor
     * @throws IllegalStateException If actor system is not properly initialized
     *
     * @sample
     * pointer.send(UpdateCount(5))
     */
    suspend fun send(message: M) {
        log.debug("[ActorSystem] Sending message: {} to actor: {}", message, actorId)
        executor.send(message)
    }

    /**
     * Sends a message and awaits a response (request-response pattern).
     *
     * @param message Message to send to the actor
     * @return Deferred result that will be completed by the actor's response
     * @throws TimeoutException If response not received within configured timeout
     *
     * @sample
     * val count = pointer.ask<Int>(GetCount())
     */
    suspend fun <R : Any> ask(message: M): R {
        val requestId = UUID.randomUUID().toString()
        val responseDeferred = PendingRequests.createRequest<R>(requestId)

        val requestMessage = AskMessage(requestId, message)
        log.debug("[ActorSystem] Sending request message: {} to actor: {}", message, actorId)

        executor.send(requestMessage as M)

        return responseDeferred.await()
    }
}