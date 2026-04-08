package com.kowshik.patterns;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;

/**
 * Producer-Consumer Pattern Demo - BlockingQueue Tutorial
 *
 * Classic concurrency pattern where producers create data and consumers
 * process it. BlockingQueue handles synchronization automatically.
 *
 * Key Points:
 * - BlockingQueue automatically handles synchronization
 * - put() blocks when queue is full
 * - take() blocks when queue is empty
 * - Different implementations: ArrayBlockingQueue, LinkedBlockingQueue, PriorityBlockingQueue
 *
 * Use Cases:
 * - Task queues
 * - Message passing between threads
 * - Event processing pipelines
 * - Work distribution systems
 */
public class ProducerConsumerDemo {

    private static final Logger logger = LoggerFactory.getLogger(ProducerConsumerDemo.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("=== Producer-Consumer Demo ===\n");

        // Demo 1: Basic producer-consumer
        basicProducerConsumerDemo();

        Thread.sleep(2000);

        // Demo 2: Multiple producers and consumers
        multipleProducersConsumersDemo();

        Thread.sleep(2000);

        // Demo 3: Priority queue
        priorityQueueDemo();

        Thread.sleep(2000);

        // Demo 4: Brute force with wait() and notify()
        bruteForceWaitNotifyDemo();
    }

    /**
     * Demo 1: Basic producer-consumer with bounded queue
     */
    private static void basicProducerConsumerDemo() throws InterruptedException {
        logger.info("Demo 1: Basic Producer-Consumer (bounded queue)");

        BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    String item = "Item-" + i;
                    logger.info("Producer: Producing {} (queue size: {})", item, queue.size());
                    queue.put(item); // Blocks if queue is full
                    logger.info("Producer: Produced {} successfully", item);
                    Thread.sleep(500);
                }
                queue.put("POISON_PILL"); // Signal completion
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                while (true) {
                    String item = queue.take(); // Blocks if queue is empty
                    if ("POISON_PILL".equals(item)) {
                        logger.info("Consumer: Received poison pill, stopping");
                        break;
                    }
                    logger.info("Consumer: Consuming {} (queue size: {})", item, queue.size());
                    Thread.sleep(1000); // Simulate slow processing
                    logger.info("Consumer: Finished processing {}", item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
        logger.info("");
    }

    /**
     * Demo 2: Multiple producers and consumers
     */
    private static void multipleProducersConsumersDemo() throws InterruptedException {
        logger.info("Demo 2: Multiple Producers and Consumers");

        BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>(10);
        int numProducers = 2;
        int numConsumers = 3;

        ExecutorService executorService = Executors.newFixedThreadPool(numProducers + numConsumers);

        // Start producers
        for (int i = 1; i <= numProducers; i++) {
            final int producerId = i;
            executorService.submit(() -> {
                try {
                    for (int j = 1; j <= 5; j++) {
                        Task task = new Task(producerId, j);
                        logger.info("Producer-{}: Creating task {}", producerId, task);
                        taskQueue.put(task);
                        Thread.sleep((long) (Math.random() * 500));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Start consumers
        for (int i = 1; i <= numConsumers; i++) {
            final int consumerId = i;
            executorService.submit(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Task task = taskQueue.poll(2, TimeUnit.SECONDS);
                        if (task != null) {
                            logger.info("Consumer-{}: Processing {}", consumerId, task);
                            Thread.sleep((long) (Math.random() * 1000));
                            logger.info("Consumer-{}: Completed {}", consumerId, task);
                        } else if (taskQueue.isEmpty()) {
                            break;
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executorService.shutdown();
        executorService.awaitTermination(15, TimeUnit.SECONDS);
        logger.info("");
    }

    /**
     * Demo 3: Priority-based processing
     */
    private static void priorityQueueDemo() throws InterruptedException {
        logger.info("Demo 3: Priority-based task processing");

        BlockingQueue<PriorityTask> priorityQueue = new PriorityBlockingQueue<>();

        // Producer
        Thread producer = new Thread(() -> {
            try {
                priorityQueue.put(new PriorityTask("Low priority task", 3));
                priorityQueue.put(new PriorityTask("High priority task", 1));
                priorityQueue.put(new PriorityTask("Medium priority task", 2));
                priorityQueue.put(new PriorityTask("Critical task", 0));
                priorityQueue.put(new PriorityTask("Another low task", 3));
                logger.info("Producer: All tasks submitted");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Consumer
        Thread consumer = new Thread(() -> {
            try {
                Thread.sleep(1000); // Let producer add all tasks
                while (!priorityQueue.isEmpty()) {
                    PriorityTask task = priorityQueue.take();
                    logger.info("Consumer: Processing {}", task);
                    Thread.sleep(500);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
    }

    /**
     * Demo 4: Brute force approach using wait() and notify()
     * This demonstrates the low-level synchronization mechanism
     * that BlockingQueue abstracts away.
     */
    private static void bruteForceWaitNotifyDemo() throws InterruptedException {
        logger.info("Demo 4: Brute Force with wait() and notify()");

        SharedQueue sharedQueue = new SharedQueue(5);

        // Producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 8; i++) {
                    String item = "Item-" + i;
                    logger.info("Producer: Attempting to produce {} (queue size: {})", item, sharedQueue.size());
                    sharedQueue.produce(item);
                    logger.info("Producer: Produced {} successfully", item);
                    Thread.sleep(500);
                }
                sharedQueue.produce("POISON_PILL");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer");

        // Consumer thread
        Thread consumer = new Thread(() -> {
            try {
                // Give an headstart to producer
                Thread.sleep(100000);
                while (true) {
                    String item = sharedQueue.consume();
                    if ("POISON_PILL".equals(item)) {
                        logger.info("Consumer: Received poison pill, stopping");
                        break;
                    }
                    logger.info("Consumer: Consuming {} (queue size: {})", item, sharedQueue.size());
                    Thread.sleep(1000); // Simulate slow processing
                    logger.info("Consumer: Finished processing {}", item);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
        logger.info("");
    }

    /**
     * Shared queue with manual synchronization using wait() and notify()
     * This is the "brute force" approach that BlockingQueue simplifies
     */
    static class SharedQueue {
        private final Queue<String> queue;
        private final int capacity;
        private final Object lock = new Object();
        private static final Logger logger = LoggerFactory.getLogger(SharedQueue.class);

        public SharedQueue(int capacity) {
            this.queue = new LinkedList<>();
            this.capacity = capacity;
        }

        /**
         * Add item to queue. Blocks if queue is full.
         */
        public void produce(String item) throws InterruptedException {
            synchronized (lock) {
                // Wait while queue is full
                while (queue.size() == capacity) {
                    logger.info("Producer: Queue is FULL, waiting...");
                    lock.wait(); // Release lock and wait
                }

                // Add item to queue
                queue.add(item);
                logger.info("Producer: Added to queue (size now: {})", queue.size());

                // Notify ALL waiting threads (both consumers and producers)
                // This is inefficient because it might wake up other producers who
                // will just check the condition and go back to sleep.
                // lock is being used by both producer and consumer threads.
                lock.notifyAll();
            }
        }

        /**
         * Remove item from queue. Blocks if queue is empty.
         */
        public String consume() throws InterruptedException {
            synchronized (lock) {
                // Wait while queue is empty
                while (queue.isEmpty()) {
                    logger.info("Consumer: Queue is EMPTY, waiting...");
                    lock.wait(); // Release lock and wait
                }

                // Remove item from queue
                String item = queue.poll();
                logger.info("Consumer: Removed from queue (size now: {})", queue.size());

                // Notify ALL waiting threads (both producers and consumers)
                // Ideally we only want to wake up producers, but with a single lock
                // we wake up everyone.
                // lock is being used by both producer and consumer threads.
                lock.notifyAll();

                return item;
            }
        }

        public int size() {
            synchronized (lock) {
                return queue.size();
            }
        }
    }

    /**
     * Task class for demo
     */
    static class Task {
        private final int producerId;
        private final int taskId;

        public Task(int producerId, int taskId) {
            this.producerId = producerId;
            this.taskId = taskId;
        }

        @Override
        public String toString() {
            return String.format("Task[P%d-T%d]", producerId, taskId);
        }
    }

    /**
     * Priority task for priority queue demo
     */
    static class PriorityTask implements Comparable<PriorityTask> {
        private final String description;
        private final int priority;

        public PriorityTask(String description, int priority) {
            this.description = description;
            this.priority = priority;
        }

        @Override
        public int compareTo(PriorityTask other) {
            return Integer.compare(this.priority, other.priority);
        }

        @Override
        public String toString() {
            return String.format("PriorityTask[priority=%d, desc='%s']", priority, description);
        }
    }

    /**
     * Real-world example: Order processing system
     */
    static class OrderProcessingSystem {
        private final BlockingQueue<Order> orderQueue;
        private final ExecutorService processors;
        private static final Logger logger = LoggerFactory.getLogger(OrderProcessingSystem.class);

        public OrderProcessingSystem(int queueSize, int numProcessors) {
            this.orderQueue = new ArrayBlockingQueue<>(queueSize);
            this.processors = Executors.newFixedThreadPool(numProcessors);
        }

        public void submitOrder(Order order) throws InterruptedException {
            logger.info("Submitting order: {}", order);
            orderQueue.put(order);
        }

        public void startProcessing() {
            for (int i = 0; i < 3; i++) {
                final int processorId = i;
                processors.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            Order order = orderQueue.poll(1, TimeUnit.SECONDS);
                            if (order != null) {
                                processOrder(processorId, order);
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                });
            }
        }

        private void processOrder(int processorId, Order order) {
            logger.info("Processor-{}: Processing order {}", processorId, order.orderId);
            try {
                Thread.sleep(1000); // Simulate processing
                logger.info("Processor-{}: Completed order {}", processorId, order.orderId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        public void shutdown() {
            processors.shutdown();
        }

        static class Order {
            private final String orderId;
            private final String customer;

            public Order(String orderId, String customer) {
                this.orderId = orderId;
                this.customer = customer;
            }
        }
    }
}
