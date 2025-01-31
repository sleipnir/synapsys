package io.creativesource.synapsys.core.internals.mailbox.transport

import io.creativesource.synapsys.core.internals.MessageSerializer
import io.creativesource.synapsys.core.internals.mailbox.SynapsysAbstractQueue
import io.creativesource.synapsys.core.internals.serialization.ProtobufMessageSerializer
import kotlinx.coroutines.ExperimentalCoroutinesApi

import kotlinx.coroutines.channels.Channel

class ChannelMailbox<M : Any>(private val serializer: MessageSerializer = ProtobufMessageSerializer()) :
    SynapsysAbstractQueue<M>() {
    private lateinit var messageClass: Class<M>
    private val channel = Channel<ByteArray>(Channel.UNLIMITED)

    override suspend fun send(message: M) {
        messageClass = message.javaClass
        val bytes = serializer.serialize(message)
        channel.send(bytes)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun receive(): M? {
        return if (!channel.isEmpty) serializer.deserialize(
            channel.receive(),
            messageClass
        ) else null
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun hasMessages(): Boolean {
        return !channel.isEmpty
    }
}
