package io.creativesource.synapsys.core.internals.mailbox.transport

import io.creativesource.synapsys.core.internals.serialization.ProtobufMessageSerializer

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
        val testMessage = "Hello ZeroMQ"

        launch {
            mailbox.send(testMessage)
        }

        val received = mailbox.receive()
        assertEquals(testMessage, received)
    }

    @Test
    fun `hasMessages should return correct status`() = runBlocking {
        assertFalse(mailbox.hasMessages())

        mailbox.send("test")
        assertTrue(mailbox.hasMessages())

        mailbox.receive()
        assertFalse(mailbox.hasMessages())
    }
}