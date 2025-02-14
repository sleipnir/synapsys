package io.eigr.synapsys.core.internals

import io.eigr.synapsys.core.actor.Actor
import io.eigr.synapsys.core.actor.ActorSystem
import io.eigr.synapsys.core.actor.Context

class BaseActorAdapter<S : Any, M : Any, R>(
    private val _actor: Actor<S, M, R>,
    private val actorSystem: ActorSystem
) :
    BaseActor {

    override val id: String
        get() = _actor.id!!

    @Suppress("UNCHECKED_CAST")
    override fun <S : Any, M : Any, R : Any> getActor(): Actor<S, M, R> = _actor as Actor<S, M, R>

    override suspend fun getState(): S? {
        return _actor.state?.state
    }

    override suspend fun rehydrate() = _actor.rehydrate()

    override suspend fun processMessageUntyped(message: Any, currentState: Any?): Pair<Any?, Any?> {
        @Suppress("UNCHECKED_CAST")
        val typedState = if (currentState != null) {
            currentState as? S
                ?: throw IllegalStateException("Actor $id has an invalid state type.")
        } else {
            null
        }

        @Suppress("UNCHECKED_CAST")
        val typedMessage = message as? M
            ?: throw IllegalArgumentException("Actor $id received a message of invalid type.")

        val ctx: Context<S> = Context(typedState, actorSystem)
        val (newCtx, result) = _actor.onReceive(typedMessage, ctx)
        return Pair(result, newCtx.state?.let { _actor.mutate(it) })
    }
}
