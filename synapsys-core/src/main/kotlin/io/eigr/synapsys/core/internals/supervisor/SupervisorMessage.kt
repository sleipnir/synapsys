package io.eigr.synapsys.core.internals.supervisor

import io.eigr.synapsys.core.internals.scheduler.ActorExecutor

sealed class SupervisorMessage {
    data class ActorFailed(val actorExecutor: ActorExecutor<*>, val exception: Throwable) : SupervisorMessage()
}
