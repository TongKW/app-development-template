package build.designand.riselife.patterns

import kotlinx.coroutines.*
import java.util.LinkedList
import kotlin.time.Duration

/**
 * A singleton class to queue tasks and execute them sequentially.
 *
 * **Purpose**:
 * - QueueWorker allows tasks to be queued and ensures they are executed one after another, in sequence.
 * - Each task can have an optional delay before execution.
 * - Useful for ensuring order of operations where tasks must not run concurrently.
 *
 * **Features**:
 * 1. Supports suspending functions as tasks.
 * 2. Tasks can have individual delays before execution.
 * 3. Thread-safe implementation using synchronization.
 *
 * **Usage**:
 * ```kotlin
 * QueueWorker.enqueueTask(delay = 500.milliseconds) {
 *     performNetworkOperation()
 * }
 * ```
 */
object QueueWorker {

    // CoroutineScope for managing the queue worker.
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // A linked list to hold tasks to be executed.
    private val taskQueue = LinkedList<Task>()

    // Flag to indicate if a task is currently being processed.
    @Volatile
    private var isProcessing = false

    // A lock object for synchronizing access to the queue and flag.
    private val lock = Any()

    /**
     * Enqueues a task to be executed sequentially.
     *
     * @param delay The delay before executing the task. Defaults to zero.
     * @param action The suspending function to execute.
     * @return The enqueued Task object.
     */
    fun enqueueTask(
        delay: Duration = Duration.ZERO,
        action: suspend () -> Unit
    ): Task {
        val task = Task(delay, action)
        synchronized(lock) {
            taskQueue.add(task)
            if (!isProcessing) {
                isProcessing = true
                processNextTask()
            }
        }
        return task
    }

    // Processes the next task in the queue.
    private fun processNextTask() {
        val task: Task?
        synchronized(lock) {
            task = if (taskQueue.isNotEmpty()) taskQueue.removeFirst() else null
            if (task == null) {
                isProcessing = false
                return
            }
        }

        scope.launch {
            task?.let {
                if (task.isCancelled) {
                    processNextTask()
                    return@launch
                }

                delay(task.delay)

                if (!task.isCancelled) {
                    try {
                        task.action()
                    } catch (e: CancellationException) {
                        // Task was cancelled; no action needed.
                    } catch (e: Exception) {
                        // Handle exceptions from task execution.
                    }
                }

                processNextTask()
            }
        }
    }

    /**
     * Cancel all pending tasks and clear the queue.
     */
    fun cancelAllTasks() {
        synchronized(lock) {
            taskQueue.clear()
            isProcessing = false
        }
    }

    /**
     * Data class representing a task with an action, an optional delay, and a cancellation flag.
     */
    class Task(
        val delay: Duration,
        val action: suspend () -> Unit
    ) {
        @Volatile
        var isCancelled: Boolean = false
            private set

        /**
         * Cancels the task if it hasn't been executed yet.
         */
        fun cancel() {
            isCancelled = true
        }
    }
}