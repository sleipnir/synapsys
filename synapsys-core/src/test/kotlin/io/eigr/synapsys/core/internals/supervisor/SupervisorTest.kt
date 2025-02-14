package io.eigr.synapsys.core.internals.supervisor

import io.eigr.synapsys.core.actor.*
import io.eigr.synapsys.core.internals.scheduler.ActorExecutor
import io.eigr.synapsys.core.internals.scheduler.WorkingStealingScheduler
import io.mockk.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

data class MockMessage(val data: String)
data class MockResponse(val result: String)
data class MockState(val value: String)

class MockActor(id: String, state: MockState) : Actor<MockState, MockMessage, MockResponse>(id, state) {
    override fun onReceive(message: MockMessage, ctx: Context<MockState>) : Pair<Context<MockState>, MockResponse>{
        return ctx to MockResponse("Processed: $message")
    }
}

class SupervisorTest {

    private lateinit var supervisor: Supervisor
    private lateinit var scheduler: WorkingStealingScheduler
    private lateinit var mockActor: ActorExecutor<MockMessage>
    private lateinit var config: Config

    @BeforeEach
    fun setup() {
        config = mockk<Config>()

        scheduler = mockk(relaxed = true)
        supervisor = Supervisor("test-supervisor", SupervisorStrategy(estimatedMaxRetries = 2))
        supervisor.setScheduler(scheduler)
        supervisor.setConfig(config)

        mockActor = mockk(relaxed = true) {
            every { actor.id } returns "mock-actor"
            coEvery { actor.getState() } returns MockState("initial-state")
        }

        val factory: (String, MockState) -> MockActor = { id, state -> MockActor(id, state) }

        supervisor.addChild<MockState, MockMessage, MockResponse>(mockActor, factory)
    }

    @Test
    fun `should restart actor after failure`() = runBlocking {
        supervisor.getMessageChannel()
            .send(SupervisorMessage.ActorFailed(mockActor, RuntimeException("Test failure")))

        Thread.sleep(1000)

        verify { scheduler.removeActor("mock-actor") }
        //verify { scheduler.enqueue(any()) }
    }

    @Test
    fun `should remove actor after max retries exceeded`() = runBlocking {
        repeat(3) {
            supervisor.getMessageChannel().send(SupervisorMessage.ActorFailed(mockActor, RuntimeException("Test failure")))
            Thread.sleep(50)
        }

        verify(exactly = 1) { scheduler.removeActor("mock-actor") }
        verify(exactly = 0) { scheduler.enqueue(any()) }
    }
}
