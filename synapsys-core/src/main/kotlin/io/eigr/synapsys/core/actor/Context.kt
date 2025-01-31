package io.eigr.synapsys.core.actor

class Context<S>(private val internalState: S?) {
    val state get() = this.internalState
}