package build.designand.riselife.patterns

// Debouncer.kt

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * A singleton class to debounce function calls.
 *
 * **Purpose**:
 * - Debouncing delays the execution of a function until a specified time has elapsed since the last invocation.
 * - Useful for preventing excessive calls to costly operations.
 *
 * **Features**:
 * 1. Supports suspending functions.
 * 2. Automatically cancels pending tasks when [cancelAllPendingTasks] is called.
 * 3. Thread-safe implementation using [ConcurrentHashMap].
 *
 * **Usage**:
 * ```kotlin
 * Debouncer.debounceCall(delay = 2.seconds) {
 *     uploadDataToCloud() // Debounced suspending function
 * }
 * ```
 */
object Debouncer {

    // CoroutineScope for managing debounce jobs.
    // Using SupervisorJob to prevent failure of one job from cancelling others.
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Map to hold pending debounce jobs identified by unique IDs.
    private val pendingJobs = ConcurrentHashMap<String, Job>()

    /**
     * Debounce a function call with a specified delay.
     *
     * @param id A unique identifier for the debounced task. Defaults to a random UUID.
     * @param delay The delay duration before executing the task.
     * @param task The suspending function to execute after the delay.
     */
    fun debounceCall(
        id: String = UUID.randomUUID().toString(),
        delay: Duration,
        task: suspend () -> Unit
    ) {
        // Cancel any existing job with the same ID.
        pendingJobs[id]?.cancel()

        // Launch a new coroutine for the debounced task.
        val job = scope.launch {
            delay(delay.inWholeMilliseconds)
            try {
                task()
            } catch (e: CancellationException) {
                // Task was cancelled; no action needed.
                // Optionally, log if needed.
            }
        }

        // Store the new job in the pendingJobs map.
        pendingJobs[id] = job
    }

    /**
     * Cancel all pending debounce tasks.
     * Call this method when the app goes to the background or exits.
     */
    fun cancelAllPendingTasks() {
        for ((id, job) in pendingJobs) {
            job.cancel()
        }
        pendingJobs.clear()
    }

    /**
     * Cancel a specific pending debounce task by its ID.
     *
     * @param id The unique identifier of the task to cancel.
     */
    fun cancelTask(id: String) {
        pendingJobs[id]?.cancel()
        pendingJobs.remove(id)
    }
}
