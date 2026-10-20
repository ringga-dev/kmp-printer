package ngga.ring.printer.util

import kotlin.random.Random

/**
 * Exponential backoff with full jitter for printer reconnection.
 *
 * Formula: delay = min(baseDelay * multiplier^attempt, maxDelay) + random(0, jitter)
 *
 * Full jitter prevents thundering herd when multiple printers disconnect simultaneously
 * (e.g. power outage recovery).
 */
object ReconnectPolicy {

    /**
     * Calculate delay before the next reconnect attempt.
     *
     * @param attempt Zero-based attempt number (0 = first retry)
     * @param baseDelayMs Initial delay in milliseconds
     * @param multiplier Backoff multiplier (e.g. 2.0 = double each time)
     * @param maxDelayMs Maximum delay cap in milliseconds
     * @param jitterMs Maximum random jitter in milliseconds
     * @return Delay in milliseconds to wait before next attempt
     */
    fun calculateDelay(
        attempt: Int,
        baseDelayMs: Long,
        multiplier: Double,
        maxDelayMs: Long,
        jitterMs: Long
    ): Long {
        val exponential = baseDelayMs * Math.pow(multiplier, attempt.toDouble())
        val capped = minOf(exponential.toLong(), maxDelayMs)
        val jitter = if (jitterMs > 0) Random.nextLong(jitterMs + 1) else 0L
        return capped + jitter
    }

    /**
     * Check if we should keep retrying given the attempt count.
     *
     * @param attempt Current attempt number (0-based)
     * @param maxAttempts Maximum number of attempts (use Int.MAX_VALUE for infinite)
     * @return true if should retry, false if exhausted
     */
    fun shouldRetry(attempt: Int, maxAttempts: Int = Int.MAX_VALUE): Boolean {
        return attempt < maxAttempts
    }
}
