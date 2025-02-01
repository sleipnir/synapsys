package io.eigr.synapsys.core.internals.mailbox.transport

import io.eigr.synapsys.core.internals.serialization.ProtobufMessageSerializer
import kotlinx.coroutines.delay

import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZeroMQMailboxTest {
    private lateinit var mailbox: ZeroMQMailbox<String>

    @BeforeEach
    fun setup() {
        mailbox = ZeroMQMailbox(serializer = ProtobufMessageSerializer(), endpoint = "inproc://test")
    }

    @Test
    fun `should send and receive messages`() = runBlocking {
        val mailbox = ZeroMQMailbox<String>()
        val testMessage = "Hello ZeroMQ"

        // ensure ordering
        launch {
            mailbox.send(testMessage)
        }.join() // wait to complete

        val received = mailbox.receive()
        assertEquals(testMessage, received)

        mailbox.close()
    }

    @Test
    fun `hasMessages should return correct status`() = runBlocking {
        assertFalse(mailbox.hasMessages())

        launch {
            mailbox.send("test")
            delay(10)
        }.join()
        assertTrue(mailbox.hasMessages())

        mailbox.receive()
        assertFalse(mailbox.hasMessages())
    }
}