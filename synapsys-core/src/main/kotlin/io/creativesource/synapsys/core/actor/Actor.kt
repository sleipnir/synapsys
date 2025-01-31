package io.creativesource.synapsys.core.actor

import io.creativesource.synapsys.core.internals.persistence.Store

abstract class Actor<S, M, R>(val id: String?, initialState: S?) {
    var state: S? = initialState

    internal var store: Store<S>? = null

    abstract fun onReceive(message: M, state: S): Pair<S, R>

    internal suspend fun onStart()  {
        this.state = store?.load(id!!)
    }

    internal suspend fun mutate(state: S): S {
        this.state = state
        store?.save(id!!, state)
        return state
    }
}