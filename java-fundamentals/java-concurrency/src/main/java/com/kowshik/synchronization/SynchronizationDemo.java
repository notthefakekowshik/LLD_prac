package com.kowshik.synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronization Demo - Thread Safety with synchronized keyword
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. What is the synchronized keyword and when to use it?
 *    - Provides mutual exclusion (mutex) lock
 *    - Only one thread can execute synchronized code at a time
 *    - Prevents race conditions on shared mutable state
 *    - Can be applied to methods or blocks
 *
 * 2. Common Interview Questions:
 *    - Difference between synchronized method vs synchronized block?
 *    - What is the lock object in synchronized(this)?
 *    - Can two threads execute different synchronized methods simultaneously? (No, if on same instance)
 *    - Synchronized on static method vs instance method?
 *    - What is monitor/intrinsic lock in Java?
 *    - Explain happens-before relationship
 *    - What is reentrant locking?
 *
 * 3. Synchronized Method vs Block:
 *    - Synchronized method: Locks entire method (lock on 'this' for instance methods)
 *    - Synchronized block: Locks only critical section (better performance)
 *    - Block allows choosing lock object explicitly
 *
 * 4. Static vs Instance Synchronization:
 *    - Instance synchronized: Lock on object instance (this)
 *    - Static synchronized: Lock on Class object (ClassName.class)
 *    - Different locks, can execute simultaneously
 *
 * 5. Best Practices:
 *    - Minimize synchronized scope (use blocks, not methods)
 *    - Avoid synchronizing on public objects
 *    - Consider using concurrent collections
 *    - Use ReentrantLock for advanced features (tryLock, timed locks)
 *    - Don't synchronize on String literals or boxed primitives
 *
 * 6. Common Pitfalls:
 *    - Forgetting to synchronize all access to shared state
 *    - Deadlocks (circular lock dependencies)
 *    - Performance degradation from over-synchronization
 *    - Synchronizing on wrong object
 */
public class SynchronizationDemo {
    private static final Logger log = LoggerFactory.getLogger(SynchronizationDemo.class);

    private int counter = 0;
    private final Object lock = new Object(); // Explicit lock object

    /**
     * Synchronized method - locks on 'this' object.
     * Only one thread can execute this method at a time for this instance.
     */
    public synchronized void increment() {
        counter++;
    }

    /**
     * Synchronized block - locks on 'this' object.
     * Equivalent to synchronized method but offers more flexibility.
     */
    public void incrementWithBlock() {
        synchronized (this) {
            counter++;
        }
    }

    /**
     * Synchronized block with explicit lock object.
     * Provides better encapsulation and control over locking.
     */
    public void incrementWithExplicitLock() {
        synchronized (lock) {
            counter++;
        }
    }

    /**
     * Gets the current counter value.
     * Should be synchronized for complete thread safety.
     */
    public synchronized int getCounter() {
        return counter;
    }

    /**
     * Demonstrates synchronized method usage and race condition prevention.
     *
     * @param args command line arguments
     * @throws InterruptedException if thread is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        log.info("=== Synchronization Demo ===");

        // Demo 1: With synchronization
        log.info("\nDemo 1: WITH synchronization (thread-safe)");
        SynchronizationDemo demo1 = new SynchronizationDemo();
        Runnable task1 = () -> {
            for (int i = 0; i < 1000; i++) {
                demo1.increment();
            }
        };

        Thread t1 = new Thread(task1, "Thread-1");
        Thread t2 = new Thread(task1, "Thread-2");
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        log.info("Expected: 2000, Actual: {}", demo1.getCounter());

        // Demo 2: Without synchronization (race condition)
        log.info("\nDemo 2: WITHOUT synchronization (race condition)");
        UnsafeCounter demo2 = new UnsafeCounter();
        Runnable task2 = () -> {
            for (int i = 0; i < 1000; i++) {
                demo2.increment();
            }
        };

        Thread t3 = new Thread(task2, "Thread-3");
        Thread t4 = new Thread(task2, "Thread-4");
        t3.start();
        t4.start();
        t3.join();
        t4.join();

        log.info("Expected: 2000, Actual: {} (likely wrong due to race condition)", demo2.getCounter());

        // Demo 3: Fine-grained locking
        log.info("\nDemo 3: Fine-grained locking with synchronized blocks");
        demonstrateFinegrainedLocking();
    }

    /**
     * Demonstrates fine-grained locking for better performance.
     */
    private static void demonstrateFinegrainedLocking() throws InterruptedException {
        BankAccount account = new BankAccount(1000);

        Runnable deposit = () -> {
            for (int i = 0; i < 100; i++) {
                account.deposit(10);
            }
        };

        Runnable withdraw = () -> {
            for (int i = 0; i < 100; i++) {
                account.withdraw(5);
            }
        };

        Thread t1 = new Thread(deposit, "Deposit-Thread");
        Thread t2 = new Thread(withdraw, "Withdraw-Thread");

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        log.info("Final balance: {} (expected: {})", account.getBalance(), 1000 + (100 * 10) - (100 * 5));
    }
}

/**
 * Example of UNSAFE code without synchronization.
 * Demonstrates race condition issue.
 */
class UnsafeCounter {
    private int counter = 0;

    // NOT synchronized - race condition!
    public void increment() {
        counter++; // This is actually three operations: read, increment, write
    }

    public int getCounter() {
        return counter;
    }
}

/**
 * Example of fine-grained locking with synchronized blocks.
 */
class BankAccount {
    private static final Logger log = LoggerFactory.getLogger(BankAccount.class);
    private int balance;
    private final Object balanceLock = new Object();

    public BankAccount(int initialBalance) {
        this.balance = initialBalance;
    }

    /**
     * Deposits money into account.
     * Only the critical section is synchronized for better performance.
     *
     * @param amount amount to deposit
     */
    public void deposit(int amount) {
        // Non-critical work outside synchronized block
        log.debug("Preparing to deposit {}", amount);

        synchronized (balanceLock) {
            // Critical section - synchronized
            balance += amount;
            log.debug("Deposited {}. New balance: {}", amount, balance);
        }

        // More non-critical work can happen here
    }

    /**
     * Withdraws money from account.
     *
     * @param amount amount to withdraw
     */
    public void withdraw(int amount) {
        synchronized (balanceLock) {
            if (balance >= amount) {
                balance -= amount;
                log.debug("Withdrew {}. New balance: {}", amount, balance);
            } else {
                log.warn("Insufficient balance. Cannot withdraw {}", amount);
            }
        }
    }

    public int getBalance() {
        synchronized (balanceLock) {
            return balance;
        }
    }
}
