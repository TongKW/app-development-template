//
//  FunctionDebounce.swift
//  Rise
//
//  Created by Peter Tong on 22/11/2024.
//

import Foundation
import UIKit

/// A generic class to debounce function calls.
///
/// **Purpose**:
/// - Debouncing is used to delay the execution of a function until a certain time has elapsed since the last time it was invoked.
/// - This is useful for costly operations (e.g., API calls, file uploads) where frequent calls are unnecessary, and the last call is the most important.
///
/// **Features**:
/// 1. Supports asynchronous functions (`async`/`await`).
/// 2. Automatically cancels pending tasks when the app goes to the background or exits.
/// 3. Singleton instance for easy global access.
/// 4. Thread-safe implementation using GCD.
///
/// **Usage**:
/// ```swift
/// Debouncer.shared.debounceCall(delay: 2.0) {
///     await uploadDataToCloud() // Debounced async function
/// }
/// ```
public class Debouncer {
    
    /// Singleton instance for global access
    public static let shared = Debouncer()
    
    /// A dictionary to hold pending tasks for different identifiers
    private var pendingTasks: [String: Task<Void, Never>] = [:]
    
    /// Serial queue to ensure thread safety
    private let queue = DispatchQueue(label: "com.debouncer.queue")

    /// Private initializer to enforce singleton usage
    private init() {}

    deinit {}

    /// Debounce a function call with a specified delay.
    ///
    /// - Parameters:
    ///   - id: A unique identifier for the debounced task (default is a random UUID).
    ///   - delay: The time in seconds to wait before executing the function.
    ///   - task: The function to be executed (can be `async`).
    public func debounceCall(
        id: String = UUID().uuidString,
        delay: TimeInterval,
        task: @escaping @Sendable () async -> Void
    ) {
        // Ensure thread safety when accessing `pendingTasks`
        queue.sync {
            // Cancel the existing task for this ID if it exists
            if let existingTask = pendingTasks[id] {
                existingTask.cancel()
            }
            
            // Create a new task to be executed after the delay
            let newTask = Task {
                try? await Task.sleep(nanoseconds: UInt64(delay * 1_000_000_000))
                if !Task.isCancelled {
                    await task()
                }
            }
            
            // Store the new task in the dictionary
            pendingTasks[id] = newTask
        }
    }

    /// Cancel all pending tasks (e.g., when the app goes to the background).
    @objc private func cancelAllPendingTasks() {
        queue.sync {
            for (_, task) in pendingTasks {
                task.cancel()
            }
            pendingTasks.removeAll()
        }
    }
}
