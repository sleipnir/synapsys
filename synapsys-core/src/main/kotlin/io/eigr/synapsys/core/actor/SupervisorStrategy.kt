package io.eigr.synapsys.core.actor

enum class RestartStrategy {
    OneForOne,
    AllForOne,
    Escalate
}

data class SupervisorStrategy(
    val kind: RestartStrategy = RestartStrategy.OneForOne,
    val estimatedMaxRetries: Int = 3,
    val initialBackoffMillis: Long = 1000L,
    val maxBackoffMillis: Long = 10_000L
)
