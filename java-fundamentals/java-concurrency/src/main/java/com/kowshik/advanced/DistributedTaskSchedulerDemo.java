package com.kowshik.advanced;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Distributed Task Scheduler Demo - Coordinated Task Execution
 *
 * INTERVIEW PREP - Key Topics:
 * =============================
 * 1. Distributed System Concepts:
 *    - Leader election (Raft, Paxos, ZooKeeper)
 *    - Task distribution and load balancing
 *    - Fault tolerance and failover
 *    - Work stealing for efficiency
 *    - Health monitoring
 *
 * 2. Common Interview Questions:
 *    - How to design a distributed task scheduler?
 *    - Explain leader election algorithms
 *    - How to handle node failures?
 *    - Work stealing vs work sharing?
 *    - Consistency vs availability tradeoffs
 *    - How to prevent duplicate task execution?
 *
 * 3. Key Design Patterns:
 *    - Leader-Worker pattern
 *    - Work stealing queues
 *    - Health check/heartbeat
 *    - Task ownership tracking
 *    - Graceful degradation
 *
 * 4. Real-world Systems:
 *    - Apache Spark (distributed computing)
 *    - Kubernetes (container orchestration)
 *    - Celery (distributed task queue)
 *    - Quartz (job scheduling)
 *    - Amazon SWF (workflow service)
 *
 * 5. Challenges:
 *    - Split-brain scenarios
 *    - Network partitions
 *    - Clock synchronization
 *    - Task idempotency
 *    - Resource contention
 *
 * Demonstrates a distributed task scheduler implementation with:
 * - Leader election
 * - Task distribution
 * - Fault tolerance
 * - Work stealing
 * - Health monitoring
 */
public class DistributedTaskSchedulerDemo {
    private final String nodeId;
    private final AtomicBoolean isLeader;
    private final Map<String, WorkerNode> workerNodes;
    private final BlockingQueue<Task> taskQueue;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService taskExecutor;
    private final AtomicInteger taskIdGenerator;
    private final Map<String, CompletableFuture<TaskResult>> taskResults;
    private final HealthMonitor healthMonitor;

    /**
     * Represents a unit of work to be executed.
     */
    static class Task {
        final String id;
        final String description;
        final long estimatedDuration;
        final Consumer<TaskContext> action;

        Task(String id, String description, long estimatedDuration, Consumer<TaskContext> action) {
            this.id = id;
            this.description = description;
            this.estimatedDuration = estimatedDuration;
            this.action = action;
        }
    }

    /**
     * Execution context for a task, allowing progress tracking.
     */
    static class TaskContext {
        final String taskId;
        final String nodeId;
        final Map<String, Object> data;

        TaskContext(String taskId, String nodeId) {
            this.taskId = taskId;
            this.nodeId = nodeId;
            this.data = new ConcurrentHashMap<>();
        }

        public void updateProgress(int progress) {
            data.put("progress", progress);
        }
    }

    /**
     * Result of task execution with metadata.
     */
    static class TaskResult {
        final String taskId;
        final String nodeId;
        final boolean success;
        final String message;
        final long duration;

        TaskResult(String taskId, String nodeId, boolean success, String message, long duration) {
            this.taskId = taskId;
            this.nodeId = nodeId;
            this.success = success;
            this.message = message;
            this.duration = duration;
        }
    }

    /**
     * Represents a worker node in the distributed system.
     */
    static class WorkerNode {
        final String nodeId;
        final AtomicBoolean healthy;
        final BlockingQueue<Task> localQueue;
        final AtomicInteger activeTaskCount;
        final long startTime;

        WorkerNode(String nodeId) {
            this.nodeId = nodeId;
            this.healthy = new AtomicBoolean(true);
            this.localQueue = new LinkedBlockingQueue<>();
            this.activeTaskCount = new AtomicInteger(0);
            this.startTime = System.currentTimeMillis();
        }
    }

    /**
     * Monitors health of worker nodes and triggers failover on failures.
     */
    static class HealthMonitor {
        private final Map<String, WorkerNode> nodes;
        private final ScheduledExecutorService scheduler;
        private final Consumer<String> nodeDownCallback;

        HealthMonitor(Map<String, WorkerNode> nodes, Consumer<String> nodeDownCallback) {
            this.nodes = nodes;
            this.nodeDownCallback = nodeDownCallback;
            this.scheduler = Executors.newSingleThreadScheduledExecutor();
            startMonitoring();
        }

        private void startMonitoring() {
            scheduler.scheduleAtFixedRate(() -> {
                nodes.forEach((nodeId, node) -> {
                    if (node.healthy.get() && !checkNodeHealth(node)) {
                        node.healthy.set(false);
                        nodeDownCallback.accept(nodeId);
                    }
                });
            }, 0, 5, TimeUnit.SECONDS);
        }

        private boolean checkNodeHealth(WorkerNode node) {
            // Simulate health check
            return System.currentTimeMillis() - node.startTime < 3600000; // 1 hour lifetime
        }

        void shutdown() {
            scheduler.shutdown();
        }
    }

    public DistributedTaskSchedulerDemo(String nodeId) {
        this.nodeId = nodeId;
        this.isLeader = new AtomicBoolean(false);
        this.workerNodes = new ConcurrentHashMap<>();
        this.taskQueue = new LinkedBlockingQueue<>();
        this.taskResults = new ConcurrentHashMap<>();
        this.taskIdGenerator = new AtomicInteger(0);

        this.scheduler = Executors.newScheduledThreadPool(2);
        this.taskExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "task-executor-" + counter.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
            }
        );

        // Register this node
        workerNodes.put(nodeId, new WorkerNode(nodeId));

        // Setup health monitoring
        this.healthMonitor = new HealthMonitor(workerNodes, this::handleNodeDown);

        // Start leader election and task distribution
        startLeaderElection();
        startTaskDistribution();
    }

    private void startLeaderElection() {
        scheduler.scheduleAtFixedRate(() -> {
            // Simulate leader election
            if (workerNodes.size() == 1 ||
                nodeId.equals(Collections.min(workerNodes.keySet()))) {
                if (!isLeader.get()) {
                    isLeader.set(true);
                    System.out.println("Node " + nodeId + " became leader");
                }
            } else {
                isLeader.set(false);
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    private void startTaskDistribution() {
        scheduler.scheduleWithFixedDelay(() -> {
            if (isLeader.get()) {
                distributeTasksToWorkers();
            }
            executeLocalTasks();
        }, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void distributeTasksToWorkers() {
        Task task;
        while ((task = taskQueue.poll()) != null) {
            // Find least loaded worker
            Optional<Map.Entry<String, WorkerNode>> worker = workerNodes.entrySet().stream()
                .filter(e -> e.getValue().healthy.get())
                .min(Comparator.comparingInt(e -> e.getValue().activeTaskCount.get()));

            Task finalTask = task;
            worker.ifPresent(w -> {
                w.getValue().localQueue.offer(finalTask);
                System.out.printf("Task %s assigned to node %s%n", finalTask.id, w.getKey());
            });
        }
    }

    private void executeLocalTasks() {
        WorkerNode localNode = workerNodes.get(nodeId);
        if (localNode != null && localNode.healthy.get()) {
            Task task = localNode.localQueue.poll();
            if (task != null) {
                localNode.activeTaskCount.incrementAndGet();
                executeTask(task).whenComplete((result, ex) -> {
                    localNode.activeTaskCount.decrementAndGet();
                    if (ex != null) {
                        System.err.printf("Task %s failed: %s%n", task.id, ex.getMessage());
                    } else {
                        System.out.printf("Task %s completed on node %s%n", task.id, nodeId);
                    }
                });
            }
        }
    }

    private CompletableFuture<TaskResult> executeTask(Task task) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            try {
                TaskContext context = new TaskContext(task.id, nodeId);
                task.action.accept(context);
                return new TaskResult(
                    task.id,
                    nodeId,
                    true,
                    "Success",
                    System.currentTimeMillis() - startTime
                );
            } catch (Exception e) {
                return new TaskResult(
                    task.id,
                    nodeId,
                    false,
                    e.getMessage(),
                    System.currentTimeMillis() - startTime
                );
            }
        }, taskExecutor);
    }

    public CompletableFuture<TaskResult> submitTask(String description,
                                                  long estimatedDuration,
                                                  Consumer<TaskContext> action) {
        String taskId = "task-" + taskIdGenerator.incrementAndGet();
        Task task = new Task(taskId, description, estimatedDuration, action);
        CompletableFuture<TaskResult> resultFuture = new CompletableFuture<>();
        taskResults.put(taskId, resultFuture);
        taskQueue.offer(task);
        return resultFuture;
    }

    private void handleNodeDown(String nodeId) {
        System.out.printf("Node %s is down, redistributing tasks%n", nodeId);
        WorkerNode node = workerNodes.remove(nodeId);
        if (node != null) {
            Task task;
            while ((task = node.localQueue.poll()) != null) {
                taskQueue.offer(task);
            }
        }
    }

    public void shutdown() {
        healthMonitor.shutdown();
        scheduler.shutdown();
        taskExecutor.shutdown();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
            taskExecutor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        // Create two nodes
        DistributedTaskSchedulerDemo node1 = new DistributedTaskSchedulerDemo("node-1");
        DistributedTaskSchedulerDemo node2 = new DistributedTaskSchedulerDemo("node-2");

        // Submit some tasks
        List<CompletableFuture<TaskResult>> results = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int taskNum = i;
            results.add(node1.submitTask(
                "Task " + taskNum,
                1000,
                context -> {
                    // Simulate work
                    try {
                        for (int progress = 0; progress <= 100; progress += 20) {
                            context.updateProgress(progress);
                            Thread.sleep(200);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Task interrupted", e);
                    }
                }
            ));
        }

        // Wait for all tasks to complete
        CompletableFuture.allOf(results.toArray(new CompletableFuture[0]))
            .get(1, TimeUnit.MINUTES);

        // Print results
        results.stream()
            .map(CompletableFuture::join)
            .forEach(result -> System.out.printf(
                "Task %s completed on %s in %dms%n",
                result.taskId, result.nodeId, result.duration
            ));

        // Cleanup
        node1.shutdown();
        node2.shutdown();
    }
}
