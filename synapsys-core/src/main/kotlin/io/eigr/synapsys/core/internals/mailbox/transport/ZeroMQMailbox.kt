package io.eigr.synapsys.core.internals.mailbox.transport

import io.eigr.synapsys.core.internals.MessageSerializer
import io.eigr.synapsys.core.internals.loggerFor
import io.eigr.synapsys.core.internals.mailbox.MailboxAbstractQueue
import io.eigr.synapsys.core.internals.serialization.ProtobufMessageSerializer
import org.zeromq.ZMQ
import kotlin.random.Random

class ZeroMQMailbox<M : Any>(
    private val serializer: MessageSerializer = ProtobufMessageSerializer(),
    private val endpoint: String = "inproc://mailbox-${System.currentTimeMillis()}-${Random.nextInt()}"
) : MailboxAbstractQueue<M>() {

    private val log = loggerFor(this::class.java)
    private val context = ZMQ.context(1)
    private lateinit var messageClass: Class<M>

    private val sender = context.socket(ZMQ.DEALER).apply {
        connect(endpoint)
    }

    private val receiver = context.socket(ZMQ.ROUTER).apply {
        bind(endpoint)
    }

    @Volatile
    private var closed = false

    override suspend fun send(message: M) {
        message::class.java.also { messageClass = it as Class<M> }
        val msg = serializer.serialize(message)

        // Add empty envelope to ROUTER/DEALER (necessary!)
        synchronized(sender) {
            sender.send("", ZMQ.SNDMORE) // Envelope
            sender.send(msg, ZMQ.DONTWAIT)
        }
    }

    override suspend fun receive(): M? {
        // Ignore the ROUTER/DEALER envelope ( first frame ;) )
        val identityFrameBytes = receiver.recv(ZMQ.DONTWAIT) ?: return null
        val emptyFrame = receiver.recv(ZMQ.DONTWAIT)
        val messageFrame = receiver.recv(ZMQ.DONTWAIT) // real message
        println("Frame: $messageFrame")

        if (messageFrame == null) {
            return null
        }

        return serializer.deserialize(messageFrame, messageClass)
    }

    override fun hasMessages(): Boolean {
        checkNotClosed()
        return synchronized(receiver) {
            //receiver.hasReceiveMore()
            true
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
