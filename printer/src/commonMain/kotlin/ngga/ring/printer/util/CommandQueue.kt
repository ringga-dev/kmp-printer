package ngga.ring.printer.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.concurrent.Volatile

/**
 * CommandQueue — Robust Print Execution
 * - Channel based queue that never closes to prevent ClosedSendChannelException.
 * - Worker coroutine that automatically restarts on error.
 * - Thread-safe enqueueing from any thread.
 * - Supports suspending writer (e.g. socket write, bluetooth write).
 */
class CommandQueue(
    private val writer: suspend (ByteArray) -> Unit
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private data class QueueItem(val data: ByteArray, val onComplete: (() -> Unit)?)

    // UNLIMITED BUFFER so it never rejects/suspends the caller
    private val channel = Channel<QueueItem>(Channel.UNLIMITED)

    @Volatile
    private var started = false

    /**
     * Starts the worker coroutine. Will be called automatically by [enqueue] if not started.
     */
    fun start() {
        if (started) return
        started = true
        spawnWorker()
    }

    private fun spawnWorker() {
        scope.launch {
            while (isActive) {
                try {
                    val job = launchWorker()
                    job.join()   // Wait until worker is done or fails
                } catch (e: Exception) {
                    PrinterLogger.error("CommandQueue", "worker outer loop caught exception", e)
                    delay(100)
                }
            }
        }
    }

    private fun launchWorker() = scope.launch {
        while (isActive) {
            val item = channel.receive()
            try {
                writer(item.data)
                item.onComplete?.invoke()
            } catch (e: Exception) {
                PrinterLogger.error("CommandQueue", "error executing command, restarting worker", e)
                item.onComplete?.invoke() // Notify completion even on failure to avoid UI hanging
                throw e // Break inner loop to trigger restart in outer loop
            }
        }
    }

    /**
     * Add a command to the queue. Auto-starts the worker if needed.
     */
    fun enqueue(bytes: ByteArray, onComplete: (() -> Unit)? = null) {
        if (!started) start()
        channel.trySend(QueueItem(bytes, onComplete))
    }

    /**
     * Stop the queue and cancel all pending/active jobs.
     */
    fun stop() {
        started = false
        scope.cancel()
    }
}
