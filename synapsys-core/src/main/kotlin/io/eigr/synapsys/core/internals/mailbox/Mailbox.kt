package io.eigr.synapsys.core.internals.mailbox

import io.eigr.synapsys.core.internals.mailbox.transport.ChannelMailbox

class Mailbox<M : Any>(private val queue: MailboxAbstractQueue<M> = ChannelMailbox()) {

    suspend fun send(message: M) {
        queue.send(message)
    }

    suspend fun receive(): M? {
        return queue.receive()
    }

    fun hasMessages(): Boolean = queue.hasMessages()
}