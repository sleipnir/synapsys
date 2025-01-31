package io.creativesource.synapsys.core.internals.scheduler

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@ExperimentalCoroutinesApi
class SchedulerTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var scheduler: Scheduler
    private lateinit var mockActorExecutor: ActorExecutor<*>

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockActorExecutor = mockk(relaxed = true)
        every { mockActorExecutor.actor.id } returns UUID.randomUUID().toString()
    }

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `should initialize with correct number of workers`() {
        val numWorkers = 4
        scheduler = Scheduler(maxReductions = 10, numWorkers = numWorkers)

        assertEquals(numWorkers, scheduler.actorExecutorQueues.size)
    }

    @Test
    fun `enqueue should add actor to random worker queue`() = runTest {
        scheduler = Scheduler(maxReductions = 10, numWorkers = 2)
        val fixedIndex = 0
        val random = mockk<Random>()
        every { random.nextInt(any()) } returns fixedIndex
        scheduler.actorExecutorQueues.forEach { it.clear() }

        scheduler.enqueue(mockActorExecutor)

        assertEquals(1, scheduler.actorExecutorQueues[fixedIndex].size)
    }

    @Test
    fun `worker should process own queue items`() = runTest {
        scheduler = Scheduler(maxReductions = 2, numWorkers = 1)
        every { mockActorExecutor.hasMessages() } returns true
        coEvery { mockActorExecutor.dequeueMessage() } returns "TestMessage"

        scheduler.enqueue(mockActorExecutor)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 4) { mockActorExecutor.processMessage(any()) }
        coVerify { mockActorExecutor.suspendExecution() }
    }

    @Test
    fun `should steal work from other workers`() = runTest {
        scheduler = Scheduler(maxReductions = 2, numWorkers = 2)
        scheduler.actorExecutorQueues[1].add(mockActorExecutor)
        scheduler.actorExecutorQueues[1].add(mockActorExecutor)
        scheduler.actorExecutorQueues[1].add(mockActorExecutor)
        scheduler.actorExecutorQueues[1].add(mockActorExecutor)

        val stolen = scheduler.stealWork(0)

        assertNotNull(stolen)
        assertEquals(mockActorExecutor.actor.id, stolen.actor.id)
    }

    @Test
    fun `should stop processing when reaching max reductions`() = runTest {
        // TODO: check this test
        scheduler = Scheduler(maxReductions = 3, numWorkers = 1)
        every { mockActorExecutor.hasMessages() } returns true
        coEvery { mockActorExecutor.dequeueMessage() } returns "TestMessage"

        scheduler.enqueue(mockActorExecutor)
        //testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 3) { mockActorExecutor.processMessage(any()) }
        coVerify { mockActorExecutor.suspendExecution() }
    }

    @Test
    fun `should re-enqueue actor after processing`() = runTest {
        scheduler = Scheduler(maxReductions = 1, numWorkers = 1)
        every { mockActorExecutor.hasMessages() } returns true
        coEvery { mockActorExecutor.dequeueMessage() } returns "TestMessage"

        scheduler.enqueue(mockActorExecutor)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockActorExecutor.suspendExecution() }
    }

    @Test
    fun `should handle empty queues gracefully`() = runTest {
        scheduler = Scheduler(maxReductions = 10, numWorkers = 2)

        val result = scheduler.stealWork(0)

        assertNull(result)
    }

    @Test
    fun `should resume execution before processing`() = runTest {
        scheduler = Scheduler(maxReductions = 1, numWorkers = 1)
        coEvery { mockActorExecutor.hasMessages() } returns true
        coEvery { mockActorExecutor.dequeueMessage() } returns "TestMessage"

        scheduler.enqueue(mockActorExecutor)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockActorExecutor.resumeExecution() }
    }

    @Test
    fun `should handle actors without messages`() = runTest {
        scheduler = Scheduler(maxReductions = 10, numWorkers = 1)
        every { mockActorExecutor.hasMessages() } returns false

        scheduler.enqueue(mockActorExecutor)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockActorExecutor.processMessage(any()) }
        coVerify { mockActorExecutor.suspendExecution() }
    }
}