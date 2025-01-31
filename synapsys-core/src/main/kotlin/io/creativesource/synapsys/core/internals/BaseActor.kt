package io.creativesource.synapsys.core.internals

interface BaseActor {
    val id: String
    suspend fun processMessageUntyped(message: Any, currentState: Any?): Pair<Any?, Any?>
    fun getState(): Any?
}
