package io.eigr.synapsys.core.internals.mailbox

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class MailboxTest {
    private val mockQueue = mockk<MailboxAbstractQueue<String>>(relaxed = true)
    private val mailbox = Mailbox(mockQueue)

    @Test
    fun `send should delegate to queue implementation`() = runTest {
        val message = "test"
        mailbox.send(message)
        coVerify { mockQueue.send(message) }
    }

    @Test
    fun `receive should delegate to queue implementation`() = runTest {
        mailbox.receive()
        coVerify { mockQueue.receive() }
    }

    @Test
    fun `hasMessages should delegate to queue implementation`() {
        mailbox.hasMessages()
        coVerify { mockQueue.hasMessages() }
    }
}