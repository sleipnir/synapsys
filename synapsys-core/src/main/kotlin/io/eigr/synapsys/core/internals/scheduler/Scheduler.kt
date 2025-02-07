package io.eigr.synapsys.core.internals.scheduler

/**
 * Interface for actor schedulers.
 */
interface Scheduler {
    /**
     * Queues an actor for execution.
     */
    fun enqueue(actorExecutor: ActorExecutor<*>)

    /**
     * Removes an actor from the scheduler by ID.
     * @return `true` if removed successfully.
     */
    fun removeActor(actorId: String): Boolean

    /**
     * Clears all work queues from the scheduler.
     */
    fun cleanAllWorkerQueues()
}