package io.eigr.synapsys.core.internals.mailbox

abstract class MailboxAbstractQueue<M> {
    abstract suspend fun send(message: M)
    abstract suspend fun receive(): M?
    abstract fun hasMessages(): Boolean
}
