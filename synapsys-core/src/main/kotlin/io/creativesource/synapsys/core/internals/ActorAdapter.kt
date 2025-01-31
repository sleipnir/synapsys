package io.creativesource.synapsys.core.internals

import io.creativesource.synapsys.core.actor.Actor

class ActorAdapter<S, M, R>(private val actor: Actor<S, M, R>) : BaseActor {

    override val id: String
        get() = actor.id!!

    override fun getState(): S? = actor.state

    override suspend fun processMessageUntyped(message: Any, currentState: Any?): Pair<Any?, Any?> {
        @Suppress("UNCHECKED_CAST")
        val typedState = currentState as? S
            ?: throw IllegalStateException("Actor $id has an invalid state type.")
        @Suppress("UNCHECKED_CAST")
        val typedMessage = message as? M
            ?: throw IllegalArgumentException("Actor $id received a message of invalid type.")

        val (newState, result) = actor.onReceive(typedMessage, typedState)
        return Pair(result, actor.mutate(newState))
    }
}
