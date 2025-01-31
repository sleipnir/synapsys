package io.eigr.synapsys.core.internals.mailbox.transport

import io.creativesource.synapsys.core.internals.serialization.ProtobufMessageSerializer

import io.mockk.coVerify
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.*

class ChannelMailboxTest {
    private val serializer = spyk<ProtobufMessageSerializer>()
    private val mailbox = ChannelMailbox<String>(serializer)

    @Test
    fun `should send and receive message`() = runTest {
        val message = "test message"

        mailbox.send(message)
        val received = mailbox.receive()

        assertEquals(message, received)
        coVerify { serializer.serialize(message) }
        coVerify { serializer.deserialize(any(), eq(String::class.java)) }
    }

    @Test
    fun `hasMessages should reflect queue state`() = runTest {
        assertFalse(mailbox.hasMessages())

        mailbox.send("test")
        assertTrue(mailbox.hasMessages())

        mailbox.receive()
        assertFalse(mailbox.hasMessages())
    }

    @Test
    fun `receive should return null when empty`() = runTest {
        val result = mailbox.receive()
        assertNull(result)
    }
}