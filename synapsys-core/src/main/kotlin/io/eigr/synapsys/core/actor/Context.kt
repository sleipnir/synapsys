package io.eigr.synapsys.core.actor

class Context<S>(private var internalState: S?) {
    val state get() = internalState

    fun update(newState: S) {
        this.internalState = newState
    }
}