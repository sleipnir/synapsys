package io.creativesource.synapsys.core.internals.mailbox

import io.creativesource.synapsys.core.internals.mailbox.transport.ChannelMailbox

class Mailbox<M : Any>(private val queue: SynapsysAbstractQueue<M> = ChannelMailbox()) {

    suspend fun send(message: M) {
        queue.send(message)
    }

    suspend fun receive(): M? {
        return queue.receive()
    }

    fun hasMessages(): Boolean = queue.hasMessages()
}