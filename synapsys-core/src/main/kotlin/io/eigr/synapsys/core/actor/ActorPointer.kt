package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor

class ActorPointer<M : Any>(private val actorId: String, private val executor: ActorExecutor<M>) {
    private val log = loggerFor(this::class.java)

    fun getAddress(): String {
        return "<${actorId.lowercase()}>"
    }

    suspend fun send(message: M) {
        log.debug("[ActorSystem] Sending message: {} to actor: {}", message, actorId)
        executor.send(message)
    }
}