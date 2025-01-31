package io.creativesource.synapsys.core.internals.mailbox.transport

import io.creativesource.synapsys.core.internals.MessageSerializer
import io.creativesource.synapsys.core.internals.loggerFor
import io.creativesource.synapsys.core.internals.mailbox.SynapsysAbstractQueue
import io.creativesource.synapsys.core.internals.serialization.ProtobufMessageSerializer
import org.zeromq.ZMQ

class ZeroMQMailbox<M : Any>(private val serializer: MessageSerializer = ProtobufMessageSerializer()) :
    SynapsysAbstractQueue<M>() {
    private lateinit var messageClass: Class<M>

    private val log = loggerFor(this::class.java)
    private val context = ZMQ.context(1)
    private val socket = context.socket(ZMQ.PAIR)

    init {
        log.debug("Starting ZeroMQ mailbox")
        socket.bind("inproc://mailbox")
    }

    override suspend fun send(message: M) {
        // This only works because there need to be messages to receive the result of the deserialization,
        // otherwise there would be an error.
        messageClass = message.javaClass
        val msg = serializer.serialize(message)
        log.debug("Sending message: {}", message)
        socket.send(msg, 0)
        log.debug("Message sent")
    }

    override suspend fun receive(): M? {
        return if (socket.hasReceiveMore()) {
            val msgBytes: ByteArray = socket.recv(0)
            serializer.deserialize(msgBytes, messageClass)
        } else {
            null
        }
    }

    override fun hasMessages(): Boolean {
        socket.recv(ZMQ.DONTWAIT)?.let { return@let true }
        return false
    }
}
