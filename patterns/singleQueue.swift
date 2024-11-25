//
//  QueueWorker.swift
//  YourApp
//
//  Created by Your Name on Date.
//

import Foundation

/// A class to queue tasks and execute them sequentially.
///
/// **Purpose**:
/// - `QueueWorker` allows tasks to be queued and ensures they are executed one after another, in sequence.
/// - Each task can have an optional delay before execution.
/// - Useful for ensuring order of operations where tasks must not run concurrently.
///
/// **Features**:
/// 1. Supports asynchronous functions (`async`/`await`).
/// 2. Tasks can have individual delays before execution.
/// 3. Thread-safe implementation using Swift actors.
///
/// **Usage**:
/// ```swift
/// await QueueWorker.shared.enqueueTask(delay: 2.0) {
///     await performNetworkOperation() // Task to be executed sequentially
/// }
/// ```
public actor QueueWorker {

    /// Singleton instance for global access
    public static let shared = QueueWorker()

    /// A queue to hold pending tasks
    private var taskQueue: [TaskItem] = []

    /// Indicates whether the queue is currently processing tasks
    private var isProcessing = false

    /// Private initializer to enforce singleton usage
    private init() {}

    /// Enqueue a task to be executed sequentially.
    ///
    /// - Parameters:
    ///   - delay: The time in seconds to wait before executing the task (default is zero).
    ///   - task: The asynchronous function to be executed.
    /// - Returns: The `TaskItem` representing the enqueued task.
    @discardableResult
    public func enqueueTask(
        id: String = UUID().uuidString,
        delay: TimeInterval = 0.0,
        task: @Sendable @escaping () async -> Void
    ) -> TaskItem {
        let taskItem = TaskItem(id: id, delay: delay, action: task)
        taskQueue.append(taskItem)

        if !isProcessing {
            isProcessing = true
            Task {
                await self.processNextTask()
            }
        }

        return taskItem
    }

    /// Processes the next task in the queue.
    private func processNextTask() async {
        while !taskQueue.isEmpty {
            let taskItem = taskQueue.removeFirst()
            if await taskItem.isCancelled {
                continue
            }

            if taskItem.delay > 0 {
                try? await Task.sleep(nanoseconds: UInt64(taskItem.delay * 1_000_000_000))
            }

            if await !taskItem.isCancelled {
                do {
                    await taskItem.action()
                } catch {
                    // Handle errors if needed
                }
            }
        }
        isProcessing = false
    }

    /// Cancel all pending tasks.
    public func cancelAllTasks() async {
        for taskItem in taskQueue {
            await taskItem.cancel()
        }
        taskQueue.removeAll()
        isProcessing = false
    }

    /// Cancel a specific task by its identifier.
    ///
    /// - Parameter id: The unique identifier of the task to cancel.
    public func cancelTask(id: String) async {
        if let index = taskQueue.firstIndex(where: { $0.id == id }) {
            let taskItem = taskQueue[index]
            await taskItem.cancel()
            taskQueue.remove(at: index)
        }
    }
}

/// Represents a task enqueued in the `QueueWorker`.
///
/// **Features**:
/// - Holds the task's action, delay, and a unique identifier.
/// - Allows for individual task cancellation.
public actor TaskItem {
    /// The unique identifier for the task.
    public nonisolated let id: String

    // The delay before executing the task, in seconds.
    let delay: TimeInterval

    /// The asynchronous function to execute.
    let action: @Sendable () async -> Void

    /// Indicates whether the task has been cancelled.
    private(set) var isCancelled = false

    init(id: String, delay: TimeInterval, action: @escaping @Sendable () async -> Void) {
        self.id = id
        self.delay = delay
        self.action = action
    }

    /// Cancels the task if it hasn't been executed yet.
    public func cancel() {
        isCancelled = true
    }
}
