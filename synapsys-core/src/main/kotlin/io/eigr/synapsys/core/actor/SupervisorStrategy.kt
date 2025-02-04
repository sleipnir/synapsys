package io.eigr.synapsys.core.actor

/**
 * Defines actor recovery strategies
 */
enum class RestartStrategy {
    OneForOne,
    AllForOne,
    Escalate
}

/**
 * Configuration container for supervision policies
 * @property kind Restart strategy type
 * @property estimatedMaxRetries Maximum recovery attempts
 * @property initialBackoffMillis Initial delay for first retry
 * @property maxBackoffMillis Maximum allowed delay between retries
 */
data class SupervisorStrategy(
    val kind: RestartStrategy = RestartStrategy.OneForOne,
    val estimatedMaxRetries: Int = 3,
    val initialBackoffMillis: Long = 1000L,
    val maxBackoffMillis: Long = 10_000L
)
