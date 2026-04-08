package com.kowshik.threads;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ThreadLocal Demo - Thread-Confined Variables Tutorial
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. What is ThreadLocal and when to use it?
 *    - Each thread has its own isolated copy of the variable
 *    - No synchronization needed (thread-confined state)
 *    - Useful for per-request context (user session, transaction ID)
 *    - Common in web applications for request scoping
 *
 * 2. Common Interview Questions:
 *    - Explain how ThreadLocal works internally
 *    - When should you use ThreadLocal vs synchronized?
 *    - What are the memory leak risks with ThreadLocal?
 *    - How does ThreadLocal relate to thread pools?
 *    - Explain ThreadLocal.remove() and why it's important
 *    - Difference between ThreadLocal and InheritableThreadLocal
 *
 * 3. Real-world Use Cases:
 *    - Web request context (Spring's RequestHolder)
 *    - Database transaction management (Hibernate Session)
 *    - Security context (Spring Security)
 *    - User session management
 *    - Logging context (MDC in logback/log4j)
 *
 * 4. Memory Leak Scenario:
 *    - ThreadLocal holds reference in ThreadLocalMap
 *    - In thread pools, threads are reused
 *    - If you don't call remove(), old data persists
 *    - Can lead to memory leaks and data bleeding between requests
 *
 * 5. Best Practices:
 *    - Always call remove() in finally block
 *    - Use try-finally pattern for cleanup
 *    - Be extra careful with thread pools
 *    - Consider using try-with-resources pattern
 *
 * 6. Internal Implementation:
 *    - Each Thread has ThreadLocalMap (like HashMap)
 *    - ThreadLocal acts as key, your value as value
 *    - Uses WeakReference for keys to prevent some leaks
 */

/**
 * A simple POJO to represent a user in the banking system
 */
class BankUser {
    private String userId;
    private String name;

    public BankUser(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() { return userId; }
    public String getName() { return name; }
}

/**
 * A shared context class to manage thread-local data for a banking application.
 */
class TransactionContext {

    // ThreadLocal for storing the current user object
    private static final ThreadLocal<BankUser> currentUser = new ThreadLocal<>();

    // ThreadLocal for storing the unique transaction ID
    private static final ThreadLocal<String> transactionId = new ThreadLocal<>();

    public static void setCurrentUser(BankUser user) {
        currentUser.set(user);
    }

    public static BankUser getCurrentUser() {
        return currentUser.get();
    }

    public static void setTransactionId(String id) {
        transactionId.set(id);
    }

    public static String getTransactionId() {
        return transactionId.get();
    }

    /**
     * Removes current thread's data from the ThreadLocal map.
     * CRUCIAL cleanup method to prevent memory leaks in thread pool environments.
     *
     * Memory Leak Scenario:
     * 1. Thread pool reuses threads across multiple requests
     * 2. Alice completes transaction on Thread-1
     * 3. If clear() is not called, Thread-1 still holds Alice's data
     * 4. Bob's request uses Thread-1, might see Alice's data (security issue!)
     * 5. References never get garbage collected (memory leak)
     *
     * Solution: Always call clear() in finally block after request processing
     */
    public static void clear() {
        currentUser.remove();
        transactionId.remove();
    }
}

/**
 * Simulates a banking application using ThreadLocal for per-thread context management.
 * Demonstrates how ThreadLocal enables passing contextual information without
 * explicitly threading it through method parameters.
 */
public class ThreadLocalDemo {

    /**
     * Demonstrates ThreadLocal usage with a thread pool to simulate
     * concurrent transaction processing for multiple users.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Simulate two concurrent users initiating transactions
        executor.submit(() -> processTransaction(new BankUser("user123", "Alice")));
        executor.submit(() -> processTransaction(new BankUser("user456", "Bob")));

        executor.shutdown();
    }

    /**
     * Processes a bank transaction for a given user.
     * Sets up ThreadLocal context at the start and ensures cleanup in finally block.
     *
     * @param user the bank user initiating the transaction
     */
    private static void processTransaction(BankUser user) {
        // Generate a unique transaction ID
        String currentTransactionId = "TX-" + UUID.randomUUID().toString();

        try {
            // Set the thread-local context at the start of the "request"
            TransactionContext.setCurrentUser(user);
            TransactionContext.setTransactionId(currentTransactionId);

            System.out.println(Thread.currentThread().getName() + " -> Starting transaction " + currentTransactionId + " for user " + user.getName());

            // Perform business logic without passing user or transaction ID as arguments
            BankService.executeTransfer();

        } finally {
            // Always clean up the thread-local context when done
            System.out.println(Thread.currentThread().getName() + " -> Ending transaction " + TransactionContext.getTransactionId());
            TransactionContext.clear();
        }
    }
}

/**
 * A service layer class that performs a bank transfer.
 * It accesses the thread-local context directly.
 */
class BankService {
    public static void executeTransfer() {
        // Access the transaction ID and user from the thread-local context
        String txId = TransactionContext.getTransactionId();
        BankUser user = TransactionContext.getCurrentUser();

        System.out.println(Thread.currentThread().getName() + " -> BankService: Executing transfer for user " + user.getName() + " (ID: " + user.getUserId() + ") with transaction ID " + txId);

        // Simulate a call to another layer, like a repository or fraud detection service
        FraudService.checkTransaction();
    }
}

/**
 * A hypothetical fraud detection service.
 * It also accesses the thread-local context.
 */
class FraudService {
    public static void checkTransaction() {
        // Access the transaction ID and user from the thread-local context
        String txId = TransactionContext.getTransactionId();
        BankUser user = TransactionContext.getCurrentUser();

        System.out.println(Thread.currentThread().getName() + " -> FraudService: Checking for fraud for user " + user.getName() + " and transaction " + txId);

        // Simulate fraud check logic
        System.out.println(Thread.currentThread().getName() + " -> FraudService: Check passed.");
    }
}