package io.creativesource.synapsys.core.internals.mailbox

abstract class SynapsysAbstractQueue<M> {
    abstract suspend fun send(message: M)
    abstract suspend fun receive(): M?
    abstract fun hasMessages(): Boolean
}
