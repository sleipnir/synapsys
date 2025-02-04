package io.eigr.synapsys.core.internals

interface BaseActor {
    val id: String
    suspend fun getState(): Any?
    suspend fun processMessageUntyped(message: Any, currentState: Any?): Pair<Any?, Any?>
    suspend fun rehydrate()

}
