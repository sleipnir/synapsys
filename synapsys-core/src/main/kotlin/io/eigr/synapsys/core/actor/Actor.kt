package io.eigr.synapsys.core.actor

import io.eigr.synapsys.core.internals.persistence.Store

abstract class Actor<S, M, R>(val id: String?, initialState: S?) {
    var state: Context<S> = Context(initialState)

    internal var store: Store<S>? = null

    abstract fun onReceive(message: M, state: Context<S>): Pair<S, R>

    internal suspend fun onStart()  {
        this.state = Context(store?.load(id!!))
    }

    internal suspend fun mutate(state: S): S {
        this.state = Context(state)
        store?.save(id!!, state)
        return state
    }
}