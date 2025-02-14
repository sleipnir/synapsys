package io.eigr.synapsys.core.internals

import io.eigr.synapsys.core.actor.Actor

interface BaseActor {
    val id: String
    fun <S : Any, M : Any, R: Any> getActor(): Actor<S, M, R>
    suspend fun getState(): Any?
    suspend fun processMessageUntyped(message: Any, currentState: Any?): Pair<Any?, Any?>
    suspend fun rehydrate()
}
