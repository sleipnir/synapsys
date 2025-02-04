package io.eigr.synapsys.core.internals

import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.Context

class BaseActorAdapter<S : Any, M : Any, R>(private val actor: Actor<S, M, R>) :
    BaseActor {

    override val id: String
        get() = actor.id!!

    override suspend fun getState(): S? {
        return actor.state.state
    }

    override suspend fun rehydrate() = actor.rehydrate()

    override suspend fun processMessageUntyped(message: Any, currentState: Any?): Pair<Any?, Any?> {
        @Suppress("UNCHECKED_CAST")
        val typedState = currentState as? S
            ?: throw IllegalStateException("Actor $id has an invalid state type.")
        @Suppress("UNCHECKED_CAST")
        val typedMessage = message as? M
            ?: throw IllegalArgumentException("Actor $id received a message of invalid type.")

        val ctx: Context<S> = Context(typedState)
        val (newCtx, result) = actor.onReceive(typedMessage, ctx)
        return Pair(result, newCtx.state?.let { actor.mutate(it) })
    }
}
