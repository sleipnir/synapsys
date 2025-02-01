package io.eigr.synapsys.core.actor

class Context<S : Any>(private var internalState: S?) {
    val state get() = internalState

    fun update(newState: S): Context<S> {
        this.internalState = newState
        return this
    }
}