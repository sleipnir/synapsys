package io.eigr.synapsys.core.internals.scheduler

import io.eigr.synapsys.core.internals.loggerFor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.util.concurrent.ConcurrentLinkedQueue

class Scheduler(
    private val maxReductions: Int,
    private val numWorkers: Int = Runtime.getRuntime().availableProcessors()
) {
    private val log = loggerFor(this::class.java)
    internal val actorExecutorQueues = List(numWorkers) { ConcurrentLinkedQueue<ActorExecutor<*>>() }
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        log.info("[Scheduler] Starting Scheduler with {} workers", numWorkers)
        repeat(numWorkers) { workerId ->
            scope.launch {
                workerLoop(workerId)
            }
        }
    }

    fun cleanAllWorkerQueues() {
        actorExecutorQueues.forEach { queue ->
            queue.clear()
        }
        log.info("[Scheduler] All worker queues cleared")
    }

    fun enqueue(actorExecutor: ActorExecutor<*>) {
        val workerIndex = (0 until numWorkers).random()
        log.debug("[Scheduler] Enqueuing actor {} to worker {}", actorExecutor.actor.id, workerIndex)
        actorExecutorQueues[workerIndex].offer(actorExecutor)
    }

    private suspend fun workerLoop(workerId: Int) {
        val queue = actorExecutorQueues[workerId]

        while (true) {
            val actorExecutor = queue.poll() ?: stealWork(workerId)

            if (actorExecutor != null) {
                scope.launch {
                    processActor(actorExecutor)
                }
            } else {
                delay(10)
            }
        }
    }

    private suspend fun processActor(actorExecutor: ActorExecutor<*>) {
        actorExecutor.resumeExecution()
        var reductions = 0

        while (actorExecutor.hasMessages() && reductions < maxReductions) {
            val message = actorExecutor.dequeueMessage()
            if (message != null) {
                actorExecutor.processMessage(message)
                reductions++
            }
        }

        if (!actorExecutor.hasMessages() || reductions >= maxReductions) {
            log.trace(
                "[Scheduler] Has messages: {}. Reductions: {}. Max reductions: {}",
                actorExecutor.hasMessages(),
                reductions,
                maxReductions
            )
            log.trace(
                "[Scheduler] Suspending actor {} on Thread: {}",
                actorExecutor.actor.id,
                Thread.currentThread()
            )

            scope.launch {
                actorExecutor.suspendExecution()
            }

            enqueue(actorExecutor)
            log.trace(
                "[Scheduler] Enqueued actor {} from Thread: {}",
                actorExecutor.actor.id,
                Thread.currentThread()
            )
        }
    }

    internal fun stealWork(workerId: Int): ActorExecutor<*>? {
        actorExecutorQueues.indices.forEach { otherWorkerId ->
            if (otherWorkerId != workerId) {
                log.debug(
                    "[Scheduler] Worker sScheduler {} stealing work from {}",
                    workerId,
                    otherWorkerId
                )
                val stolen = actorExecutorQueues[otherWorkerId].poll()
                if (stolen != null) return stolen
            }
        }
        return null
    }
}

