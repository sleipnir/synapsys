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

    fun enqueue(actorExecutor: ActorExecutor<*>) {
        val workerIndex = (0 until numWorkers).random()
        log.debug("[Scheduler] Enqueuing actor {} to worker {}", actorExecutor.actor.id, workerIndex)

        actorExecutor.resumeExecution()
        actorExecutorQueues[workerIndex].offer(actorExecutor)
    }

    fun removeActor(actorId: String): Boolean {
        val removed = actorExecutorQueues.any { queue ->
            val originalSize = queue.size
            queue.removeIf { it.actor.id == actorId }
            originalSize != queue.size
        }

        return if (removed) {
            log.info("[Scheduler] Removed actor {} from scheduler", actorId)
            true
        } else {
            log.warn("[Scheduler] Actor {} not found in scheduler", actorId)
            false
        }
    }

    fun cleanAllWorkerQueues() {
        actorExecutorQueues.forEach { queue ->
            queue.clear()
        }
        log.info("[Scheduler] All worker queues cleared")
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

        while (isProcessable(actorExecutor, reductions)) {
            val message = actorExecutor.dequeueMessage()
            if (message != null) {
                actorExecutor.processMessage(message)
                reductions++
            }
        }

        if (isNotProcessable(actorExecutor, reductions)) {
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

    private fun isProcessable(actorExecutor: ActorExecutor<*>, reductions: Int): Boolean =
        actorExecutor.hasMessages() && reductions < maxReductions

    private fun isNotProcessable(actorExecutor: ActorExecutor<*>, reductions: Int): Boolean =
        !actorExecutor.hasMessages() || reductions >= maxReductions

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

