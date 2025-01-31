package io.creativesource.synapsys.core.internals.mailbox.transport

import io.creativesource.synapsys.core.internals.MessageSerializer
import io.creativesource.synapsys.core.internals.loggerFor
import io.creativesource.synapsys.core.internals.mailbox.SynapsysAbstractQueue
import io.creativesource.synapsys.core.internals.serialization.ProtobufMessageSerializer

import org.zeromq.ZMQ
import org.zeromq.ZMQException

class ZeroMQMailbox<M : Any>(
    private val serializer: MessageSerializer = ProtobufMessageSerializer(),
    private val endpoint: String = "inproc://mailbox"
) : SynapsysAbstractQueue<M>() {

    private val log = loggerFor(this::class.java)
    private val context = ZMQ.context(1)
    private lateinit var messageClass: Class<M>

    private val sender = context.socket(ZMQ.PAIR).apply {
        try {
            connect(endpoint)
        } catch (e: ZMQException) {
            log.error("Error connecting to endpoint: $endpoint", e)
            throw e
        }
    }

    private val receiver = context.socket(ZMQ.PAIR).apply {
        try {
            bind(endpoint)
        } catch (e: ZMQException) {
            log.error("Error binding to endpoint: $endpoint", e)
            throw e
        }
    }

    @Volatile
    private var closed = false

    override suspend fun send(message: M) {
        checkNotClosed()
        // This only works because there need to be messages to receive the result of the deserialization,
        // otherwise there would be an error.
        messageClass = message.javaClass
        val msg = serializer.serialize(message)
        log.debug("Sending message: {}", message)
        synchronized(sender) {
            sender.send(msg, ZMQ.DONTWAIT)
        }
    }

    override suspend fun receive(): M? {
        checkNotClosed()
        return synchronized(receiver) {
            val msgBytes = receiver.recv(ZMQ.DONTWAIT)
            if (msgBytes != null) {
                serializer.deserialize(msgBytes, messageClass)
            } else {
                null
            }
        }
    }

    override fun hasMessages(): Boolean {
        checkNotClosed()
        return synchronized(receiver) {
            receiver.hasReceiveMore()
        }
    }

    fun close() {
        closed = true
        try {
            sender.close()
            receiver.close()
            context.close()
        } catch (e: Exception) {
            log.error("Error closing ZeroMQ resources", e)
        }
    }

    private fun checkNotClosed() {
        if (closed) {
            throw IllegalStateException("Mailbox is closed")
        }
    }
}
