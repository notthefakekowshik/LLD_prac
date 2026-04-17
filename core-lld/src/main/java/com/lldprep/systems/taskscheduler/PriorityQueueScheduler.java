package com.lldprep.systems.taskscheduler;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Task Scheduler implementation using PriorityQueue instead of DelayQueue.
 * 
 * THIS IS INTENTIONALLY INFERIOR - demonstrates why DelayQueue exists.
 * 
 * The Pain Points:
 * 1. BUSY-WAITING: Must constantly poll to check if tasks are ready
  * 2. INEFFICIENT: O(log n) for both insert AND remove (DelayQueue has O(1) peek)
 * 3. SLEEP DILEMMA: Short sleep = CPU burn. Long sleep = imprecise timing.
 * 4. NO BLOCKING: Can't block until task ready - must poll repeatedly
 * 5. WAKEUP PROBLEM: New earlier task inserted? Already sleeping, won't wake.
 * 
 * Compare to DelayQueue:
 * - DelayQueue.take() blocks precisely until delay expires
 * - DelayQueue uses "leader" thread pattern - only one thread waits with timeout
 * - DelayQueue wakes immediately when shorter-delay task inserted
 */
public class PriorityQueueScheduler implements TaskScheduler {
    
    // Core data structure: PriorityQueue ordered by execution time
    // PROBLEM 1: No built-in delay awareness - just a sorted collection
    private final PriorityQueue<Task> taskQueue;
    
    // Need external lock because PriorityQueue is NOT thread-safe
    // DelayQueue has internal ReentrantLock - we must manage our own
    private final ReentrantLock queueLock = new ReentrantLock();
    
    // Dispatcher thread - must POLL instead of blocking
    private final Thread dispatcherThread;
    private volatile boolean running = true;
    
    // THE SLEEP DILEMMA: How long to sleep between polls?
    // - 1ms: Responsive but burns CPU
    // - 100ms: CPU-friendly but tasks execute up to 100ms late
    private static final long POLL_INTERVAL_MS = 10;  // Trade-off: accuracy vs CPU
    
    public PriorityQueueScheduler() {
        // PriorityQueue ordered by Task.compareTo() (execution time)
        this.taskQueue = new PriorityQueue<>();
        
        // Start the polling dispatcher
        this.dispatcherThread = new Thread(this::pollLoop, "pq-dispatcher");
        this.dispatcherThread.setDaemon(true);
        this.dispatcherThread.start();
    }
    
    /**
     * THE PAIN: Polling loop instead of blocking take()
     * 
     * With DelayQueue:
     *   DelayedTask task = queue.take(); // Blocks precisely until ready
     * 
     * With PriorityQueue:
     *   while (true) {
     *       Task task = queue.peek();
     *       if (task != null && task.getNextExecutionTime() <= now) {
     *           queue.poll(); // Remove and execute
     *       } else {
     *           Thread.sleep(POLL_INTERVAL); // IMPRECISE!
     *       }
     *   }
     */
    private void pollLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            queueLock.lock();
            try {
                Task task = taskQueue.peek();
                
                if (task == null) {
                    // Queue empty - what to do?
                    // Option A: Sleep fixed time (miss new tasks briefly)
                    // Option B: Wait on condition (complex - need signal on insert)
                    queueLock.unlock();
                    sleep(POLL_INTERVAL_MS);
                    continue;
                }
                
                long now = System.currentTimeMillis();
                long delay = task.getNextExecutionTime() - now;
                
                if (delay <= 0) {
                    // Task ready - remove and execute
                    taskQueue.poll();  // O(log n) - same as DelayQueue
                    queueLock.unlock();
                    executeTask(task);
                } else {
                    // Task not ready yet - must sleep
                    // PROBLEM: What if new task inserted with SHORTER delay?
                    // We won't know until we wake up!
                    queueLock.unlock();
                    
                    // Sleep the full delay? NO - blocks everything else
                    // Sleep poll interval? YES - imprecise but responsive
                    sleep(Math.min(delay, POLL_INTERVAL_MS));
                }
                
            } catch (Exception e) {
                System.err.println("Poll error: " + e.getMessage());
                if (queueLock.isHeldByCurrentThread()) {
                    queueLock.unlock();
                }
            }
        }
    }
    
    /**
     * THE PAIN: Signaling complexity
     * 
     * When we insert a task, if it's the EARLIEST task, we should wake
     * the dispatcher immediately. But with simple PriorityQueue + sleep,
     * the dispatcher is sleeping and won't know.
     * 
     * Options:
     * 1. Accept imprecision (what we do here)
     * 2. Use Condition variable + signal (more complex)
     * 3. Interrupt sleep (messy)
     */
    @Override
    public ScheduledTask scheduleOnce(String name, Runnable action, long delay, TimeUnit unit) {
        long delayMillis = unit.toMillis(delay);
        String taskId = generateTaskId();
        
        DefaultTask task = new DefaultTask(taskId, name, action, delayMillis, 0, 1);
        
        queueLock.lock();
        try {
            taskQueue.offer(task);  // O(log n)
            
            // PROBLEM: If this task is now the HEAD (earliest),
            // dispatcher is sleeping and won't check until it wakes!
            // With DelayQueue: signal() wakes waiting thread immediately
            
        } finally {
            queueLock.unlock();
        }
        
        return new DefaultScheduledTask(task);
    }
    
    // Stub implementations - not the focus
    @Override
    public ScheduledTask scheduleAt(String name, Runnable action, LocalDateTime dateTime) {
        long delayMillis = calculateDelayFromDateTime(dateTime);
        return scheduleOnce(name, action, delayMillis, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public ScheduledTask scheduleAt(String name, Runnable action, LocalTime time) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduledTime = now.with(time);
        if (scheduledTime.isBefore(now)) {
            scheduledTime = scheduledTime.plusDays(1);
        }
        return scheduleAt(name, action, scheduledTime);
    }
    
    @Override
    public ScheduledTask scheduleAtFixedRate(String name, Runnable action,
                                              long initialDelay, long period, TimeUnit unit) {
        long initialDelayMillis = unit.toMillis(initialDelay);
        long periodMillis = unit.toMillis(period);
        String taskId = generateTaskId();
        
        DefaultTask task = new DefaultTask(taskId, name, action, initialDelayMillis, periodMillis, 0);
        
        queueLock.lock();
        try {
            taskQueue.offer(task);
        } finally {
            queueLock.unlock();
        }
        
        return new DefaultScheduledTask(task);
    }
    
    @Override
    public ScheduledTask scheduleWithFixedDelay(String name, Runnable action,
                                                 long initialDelay, long delay, TimeUnit unit) {
        // Same as fixed rate for this demo - real impl would track completion time
        return scheduleAtFixedRate(name, action, initialDelay, delay, unit);
    }
    
    @Override
    public ScheduledTask scheduleRecurring(String name, Runnable action, Duration interval) {
        return scheduleAtFixedRate(name, action, 0, interval.toMillis(), TimeUnit.MILLISECONDS);
    }
    
    @Override
    public ScheduledTask submit(String name, Runnable action) {
        return scheduleOnce(name, action, 0, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public boolean cancel(String taskId) {
        queueLock.lock();
        try {
            // PROBLEM: PriorityQueue.remove() is O(n) - must scan entire queue!
            // DelayQueue uses HashMap for O(1) removal
            for (Task task : taskQueue) {
                if (task.getId().equals(taskId)) {
                    taskQueue.remove(task);  // O(n) - yuck!
                    task.cancel();
                    return true;
                }
            }
            return false;
        } finally {
            queueLock.unlock();
        }
    }
    
    @Override
    public void cancelAll() {
        queueLock.lock();
        try {
            for (Task task : taskQueue) {
                task.cancel();
            }
            taskQueue.clear();
        } finally {
            queueLock.unlock();
        }
    }
    
    @Override
    public int getScheduledTaskCount() {
        queueLock.lock();
        try {
            return taskQueue.size();
        } finally {
            queueLock.unlock();
        }
    }
    
    @Override
    public List<String> getScheduledTaskIds() {
        queueLock.lock();
        try {
            return taskQueue.stream()
                .map(Task::getId)
                .toList();
        } finally {
            queueLock.unlock();
        }
    }
    
    @Override
    public boolean shutdown(long timeout, TimeUnit unit) {
        running = false;
        dispatcherThread.interrupt();
        return true;
    }
    
    @Override
    public boolean isShutdown() {
        return !running;
    }
    
    @Override
    public SchedulerMetrics getMetrics() {
        return new SchedulerMetrics(0, 0, 0, getScheduledTaskCount(), 1);
    }
    
    // Helper methods
    private void executeTask(Task task) {
        // Simplified - just execute
        new Thread(task::execute).start();
    }
    
    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private long calculateDelayFromDateTime(LocalDateTime dateTime) {
        long targetMillis = dateTime.atZone(java.time.ZoneId.systemDefault())
            .toInstant().toEpochMilli();
        long nowMillis = System.currentTimeMillis();
        return Math.max(0, targetMillis - nowMillis);
    }
    
    private String generateTaskId() {
        return "pq-task-" + System.currentTimeMillis() % 10000;
    }
}

/**
 * SUMMARY OF PAIN:
 * 
 * 1. POLLING BURN (CPU waste)
 *    - Must constantly check if tasks ready
 *    - Sleep too short = CPU burn
 *    - Sleep too long = imprecise timing
 * 
 * 2. NO BLOCKING TAKE (Complexity)
 *    - Can't block until task ready
 *    - Must manage sleep/wake manually
 * 
 * 3. WAKEUP PROBLEM (Race condition)
 *    - Insert earlier task while dispatcher sleeping
 *    - Dispatcher won't know until it wakes
 *    - Need Condition variable + signal (extra complexity)
 * 
 * 4. O(n) CANCELLATION (Performance)
 *    - PriorityQueue.remove() scans entire queue
 *    - DelayQueue uses HashMap for O(1) removal
 * 
 * 5. NO LEADER PATTERN (Inefficiency)
 *    - DelayQueue: only leader thread times out, others block forever
 *    - PriorityQueue: must all poll or have complex coordination
 * 
 * DelayQueue solves ALL of these:
 *    - Blocking take() with nanosecond precision
 *    - Internal HashMap for O(1) removal
 *    - Leader-follower pattern for efficient waiting
 *    - Automatic signal on insert of earlier task
 */


/*

Key differences from DelayQueue:

Aspect	PriorityQueue	DelayQueue
Waiting	Busy-polling with Thread.sleep()	take() blocks precisely until ready
Precision	Poll interval trade-off (10ms = 10ms max drift)	Nanosecond precision
Cancellation	O(n) scan entire queue	O(1) HashMap lookup
Wakeup	Sleeping thread misses earlier tasks	Signal wakes immediately
CPU	Burns cycles polling	Zero CPU when idle
Complexity	Must manage locking + signaling internally	All handled internally


With 10ms poll interval, tasks can execute up to 10ms late.
With 100ms poll, up to 100ms late. With DelayQueue: exactly on time.

This is exactly why java.util.concurrent has DelayQueue instead of just telling people to use PriorityQueue with polling.

 */