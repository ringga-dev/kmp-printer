package ngga.ring.printer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import ngga.ring.printer.util.ReconnectPolicy

class ReconnectPolicyTest {

    @Test
    fun `first retry uses base delay plus jitter`() {
        val delay = ReconnectPolicy.calculateDelay(
            attempt = 0,
            baseDelayMs = 150,
            multiplier = 2.0,
            maxDelayMs = 30_000,
            jitterMs = 500
        )
        assertTrue(delay in 150..650, "Expected 150..650, got $delay")
    }

    @Test
    fun `delay doubles with each attempt`() {
        val d0 = ReconnectPolicy.calculateDelay(0, 100, 2.0, 30_000, 0)
        val d1 = ReconnectPolicy.calculateDelay(1, 100, 2.0, 30_000, 0)
        val d2 = ReconnectPolicy.calculateDelay(2, 100, 2.0, 30_000, 0)
        assertEquals(100, d0)
        assertEquals(200, d1)
        assertEquals(400, d2)
    }

    @Test
    fun `delay is capped at maxDelayMs`() {
        val delay = ReconnectPolicy.calculateDelay(
            attempt = 10,
            baseDelayMs = 100,
            multiplier = 2.0,
            maxDelayMs = 500,
            jitterMs = 0
        )
        assertEquals(500, delay)
    }

    @Test
    fun `jitter is applied when configured`() {
        val delay = ReconnectPolicy.calculateDelay(
            attempt = 0,
            baseDelayMs = 150,
            multiplier = 2.0,
            maxDelayMs = 30_000,
            jitterMs = 500
        )
        assertTrue(delay >= 150, "Delay should be >= baseDelayMs")
        assertTrue(delay <= 650, "Delay should be <= baseDelayMs + jitterMs")
    }

    @Test
    fun `shouldRetry returns true when attempts remain`() {
        assertTrue(ReconnectPolicy.shouldRetry(0, 3))
        assertTrue(ReconnectPolicy.shouldRetry(2, 3))
    }

    @Test
    fun `shouldRetry returns false when exhausted`() {
        assertTrue(!ReconnectPolicy.shouldRetry(3, 3))
    }

    @Test
    fun `shouldRetry allows infinite retries by default`() {
        assertTrue(ReconnectPolicy.shouldRetry(999))
    }
}
