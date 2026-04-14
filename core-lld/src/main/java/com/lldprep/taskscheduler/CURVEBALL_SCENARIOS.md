# Task Scheduler - Curve Ball Scenarios & Alternative Implementations

This document covers alternative `TaskScheduler` implementations for interview curve ball questions. Each implementation targets a specific constraint or scale requirement.

---

## Overview Table

| Implementation | Best For | Time Precision | Scale | Use When |
|----------------|----------|----------------|-------|----------|
| **DelayQueue (Our Impl)** | General purpose | Millisecond | 10K tasks | Interview demo, moderate load |
| **Hashed Wheel Timer** | Millions of short timeouts | Coarse | 1M+ tasks | Connection timeouts, session expiry |
| **Tiered Timing Wheels** | Mixed delay ranges | Millisecond | 100K+ tasks | Kafka-style, varied delays |
| **Database-Backed** | Persistence, distributed | Second | Unlimited | Must survive restarts, multi-node |
| **Redis-Backed** | Distributed, horizontal | Millisecond | 100K+ tasks | Microservices, cloud-native |
| **Disruptor-Based** | Ultra-low latency | Microsecond | 10K tasks | Trading, sub-millisecond required |
| **Cron-Expression** | Calendar rules | Second | 1K tasks | Business hours, holidays, DST |
| **Message Broker** | Offload to infrastructure | Depends on broker | Unlimited | When "buy vs build" trade-off acceptable |

---

## 1. Hashed Wheel Timer (Netflix/Hazelcast Style)

### Problem It Solves
> "What if you have 1 million connection timeouts to track?"

### Core Idea
```
┌─────────────────────────────────────────────────────────┐
│                     WHEEL (512 buckets)                   │
│  ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐       │
│  │  0  │  1  │  2  │ ... │ 509 │ 510 │ 511 │  0  │  ◀───┐│
│  └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┘      ││
│     │     │     │     │     │     │     │               ││
│    ▼▼    ▼▼    ▼▼    ▼▼    ▼▼    ▼▼    ▼▼              ││
│   [T1]  [T2]  [T3]  [T4]  [T5]  [T6]  [T7]              ││
│   [T8]        [T9]        [T10]                         ││
│                                                         ││
│  Tick every 100ms → process current bucket              ││
│  Tasks overflow to next round: expiration =             ││
│  (currentTick + delay/tickDuration) % wheelSize         ││
└─────────────────────────────────────────────────────────┘┘
                                                            │
                                                            ▼
                                                  ┌─────────────────┐
                                                  │  Next Round     │
                                                  │  (overflow)     │
                                                  └─────────────────┘
```

### Implementation Sketch
```java
public class HashedWheelScheduler implements TaskScheduler {
    private final Bucket[] wheel;
    private final int wheelSize;
    private final long tickDuration;
    private final AtomicLong tick = new AtomicLong(0);
    
    // O(1) insert: hash task into bucket
    public ScheduledTask scheduleOnce(String name, Runnable task, long delay, TimeUnit unit) {
        long ticks = unit.toMillis(delay) / tickDuration;
        int bucketIndex = (int) ((tick.get() + ticks) % wheelSize);
        
        WheelTask wheelTask = new WheelTask(name, task, tick.get() + ticks);
        wheel[bucketIndex].add(wheelTask);  // Linked list per bucket
        return new WheelScheduledTask(wheelTask);
    }
    
    // Dispatcher thread
    private void startTicker() {
        while (running) {
            int currentBucket = (int) (tick.get() % wheelSize);
            processBucket(wheel[currentBucket]);
            tick.incrementAndGet();
            Thread.sleep(tickDuration);  // Coarse timing
        }
    }
    
    private void processBucket(Bucket bucket) {
        Iterator<WheelTask> it = bucket.iterator();
        while (it.hasNext()) {
            WheelTask task = it.next();
            if (task.getExpirationTick() <= tick.get()) {
                it.remove();
                workerPool.execute(task.getRunnable());
            }
        }
    }
}
```

### Trade-offs
| Pros | Cons |
|------|------|
| O(1) insert, O(1) delete | Coarse time precision (tick-based) |
| O(1) tick processing (per bucket) | Memory overhead for empty buckets |
| Handles 1M+ tasks efficiently | Tasks in same bucket executed together |
| Simple, lock-free design | Not suitable for millisecond precision |

### When Interviewers Ask
> "Design a scheduler for 10 million WebSocket connection timeouts (30 seconds each)"

**Answer:** Hashed wheel with:
- Wheel size: 1024 buckets
- Tick duration: 100ms  
- Round overflow: Tasks > 102.4s go to "remaining rounds" map
- Memory: ~10M tasks × (pointer overhead) vs DelayQueue O(n log n) maintenance

---

## 2. Tiered Timing Wheels (Kafka Style)

### Problem It Solves
> "How do you schedule tasks ranging from 100ms to 30 days without scanning them all?"

### Core Idea
```
┌────────────────────────────────────────────────────────────────┐
│                    TIERED WHEEL SYSTEM                          │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐    ┌─────────────────┐    ┌──────────────┐ │
│  │  Millis Wheel   │───▶│  Seconds Wheel  │───▶│  Minutes     │ │
│  │  20 slots × 1ms │    │  60 slots × 1s  │    │  60 slots    │ │
│  │  (20ms max)     │    │  (60s max)      │    │  (60min max) │ │
│  └─────────────────┘    └─────────────────┘    └──────────────┘ │
│           │                                               │     │
│           │                                               ▼     │
│           │                                          ┌────────┐│
│           │                                          │ Hours  ││
│           │                                          │ Wheel  ││
│           │                                          └────────┘│
│           │                                                     │
│  Task: 5min 30s delay                                          │
│  1. Start in Seconds Wheel at slot 30                          │
│  2. After 30s, cascade to Minutes Wheel at slot 5            │
│  3. Execute after 5 minutes                                    │
└────────────────────────────────────────────────────────────────┘
```

### Key Insight
- Short delays handled by fast wheel (milliseconds)
- Long delays cascade up to slower wheels (hours/days)
- No need to "tick" through empty time slots

### Implementation Sketch
```java
public class TieredWheelScheduler {
    private final Wheel[] wheels;  // ms, sec, min, hour, day
    
    public void schedule(Task task, long delayMs) {
        // Find appropriate wheel for this delay
        Wheel targetWheel = selectWheel(delayMs);
        int slot = calculateSlot(targetWheel, delayMs);
        targetWheel.addToSlot(slot, task);
    }
    
    // Cascade: when a wheel completes a revolution
    private void onWheelRevolution(Wheel completedWheel) {
        Wheel nextWheel = getNextLargerWheel(completedWheel);
        if (nextWheel != null) {
            // Move tasks from next wheel's current slot down
            List<Task> cascading = nextWheel.getCurrentSlotAndAdvance();
            for (Task task : cascading) {
                completedWheel.add(task);  // Now appropriate for this wheel
            }
        }
    }
}
```

### Interview Curve Ball
> "You have 1000 tasks: 500 expire in 100ms, 500 expire in 30 days. How to avoid scanning the 30-day tasks every tick?"

**Answer:** Tiered wheels with overflow handling:
- Millis wheel: 0-20ms tasks
- Seconds wheel: 1-60s tasks  
- Minutes wheel: 1-60min tasks
- Days wheel: overflow bucket
- Tasks cascade down as time approaches

---

## 3. Database-Backed Scheduler (Persistent)

### Problem It Solves
> "How do you survive a server restart without losing scheduled tasks?"
> "Multiple scheduler instances—how to prevent double execution?"

### Core Idea
```
┌─────────────────────────────────────────────────────────────┐
│              DATABASE TABLE: scheduled_tasks               │
├─────────────────────────────────────────────────────────────┤
│  id | name | execute_at | status | worker_id | payload     │
├─────────────────────────────────────────────────────────────┤
│  1  | T1   | 09:00:00  | PENDING| NULL       | {...}       │
│  2  | T2   | 09:01:00  | CLAIMED| node-A     | {...}       │  ◀── Being executed
│  3  | T3   | 09:02:00  | DONE   | node-B     | {...}       │  ◀── Completed
└─────────────────────────────────────────────────────────────┘
                              │
                              │ 1. Poll: SELECT * WHERE execute_at < NOW() 
                              │    AND status='PENDING' FOR UPDATE
                              │ 2. UPDATE status='CLAIMED', worker_id='node-A'
                              │ 3. Execute task
                              │ 4. UPDATE status='DONE' (or DELETE)
                              ▼
              ┌───────────────────────────────┐
              │  Multiple Scheduler Nodes     │
              │  ┌──────────┐  ┌──────────┐ │
              │  │ Node A   │  │ Node B   │ │
              │  │ (claims) │  │ (claims) │ │
              │  └──────────┘  └──────────┘ │
              └───────────────────────────────┘
```

### Implementation Sketch
```java
public class DatabaseScheduler implements TaskScheduler {
    private final DataSource dataSource;
    private final String workerId = UUID.randomUUID().toString();
    
    // Polling loop (every second)
    public void pollAndExecute() {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            
            // Claim tasks atomically
            List<TaskRecord> tasks = claimTasks(conn, 10); // batch size
            
            for (TaskRecord task : tasks) {
                executeTask(task);
                markDone(conn, task.getId());
            }
            
            conn.commit();
        }
    }
    
    private List<TaskRecord> claimTasks(Connection conn, int limit) {
        String sql = """
            UPDATE scheduled_tasks 
            SET status = 'CLAIMED', worker_id = ?, claimed_at = NOW()
            WHERE id IN (
                SELECT id FROM scheduled_tasks 
                WHERE execute_at <= NOW() 
                AND status = 'PENDING'
                ORDER BY execute_at
                LIMIT ?
                FOR UPDATE SKIP LOCKED  -- PostgreSQL/MySQL 8.0
            )
            RETURNING id, name, payload
            """;
        // Execute and return claimed tasks
    }
}
```

### Distributed Coordination
| Strategy | Mechanism | Trade-off |
|----------|-----------|-----------|
| **SELECT FOR UPDATE** | Row-level locks | Simple, DB-dependent |
| **Optimistic Locking** | Version column | No DB locks, retry on conflict |
| **Lease-based** | Claim with TTL, heartbeat extend | Handles worker crashes |
| **Distributed Lock** | Redis/Zookeeper lock | Extra infrastructure |

### Interview Curve Ball
> "Two scheduler nodes pick up the same task at the same millisecond—how to prevent double execution?"

**Answer Options:**
1. **Row-level locking** (`SELECT FOR UPDATE`) - one wins, other waits
2. **Atomic claim** (`UPDATE ... WHERE status='PENDING'`) - only one succeeds
3. **Idempotency key** - task execution is idempotent, both can "execute" but effect happens once
4. **Distributed lock** - Redis `SETNX task-id true` with TTL

---

## 4. Redis-Backed Distributed Scheduler

### Problem It Solves
> "Build a scheduler that works across 10 microservice instances"

### Core Idea
```
┌─────────────────────────────────────────────────────────────┐
│                    REDIS DATA STRUCTURES                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  Sorted Set (ZSET): "scheduler:tasks"               │   │
│  │  ├─ score: execution timestamp (epoch millis)       │   │
│  │  └─ member: task_id                                  │   │
│  │                                                      │   │
│  │  ZADD scheduler:tasks 1715689200000 "task-123"      │   │
│  │  ZRANGEBYSCORE scheduler:tasks -inf 1715689201000    │   │
│  └─────────────────────────────────────────────────────┘   │
│                           │                                 │
│  ┌────────────────────────▼────────────────────────────┐    │
│  │  Hash: "scheduler:task:task-123"                 │    │
│  │  ├─ name: "email-job"                              │    │
│  │  ├─ payload: "{...}"                               │    │
│  │  ├─ status: "pending"                              │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
│  Lua Script (Atomic Pop + Claim):                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1. ZRANGEBYSCORE tasks -inf now LIMIT 0 1           │   │
│  │ 2. HSET task:{id} status claimed worker {me}         │   │
│  │ 3. ZREM tasks {id}  ▶ removes from queue             │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### Implementation Sketch
```java
public class RedisScheduler implements TaskScheduler {
    private final JedisPool jedisPool;
    private final String workerId = UUID.randomUUID().toString();
    
    // Atomic claim using Lua script
    private static final String CLAIM_TASK_SCRIPT = """
        local tasks = redis.call('zrangebyscore', KEYS[1], '-inf', ARGV[1], 'limit', 0, 1)
        if #tasks == 0 then return nil end
        local taskId = tasks[1]
        local status = redis.call('hget', 'scheduler:task:' .. taskId, 'status')
        if status == 'pending' then
            redis.call('hset', 'scheduler:task:' .. taskId, 'status', 'claimed', 'worker', ARGV[2])
            redis.call('zrem', KEYS[1], taskId)
            return taskId
        end
        return nil
        """;
    
    public void pollAndExecute() {
        try (Jedis jedis = jedisPool.getResource()) {
            long now = System.currentTimeMillis();
            String taskId = (String) jedis.eval(
                CLAIM_TASK_SCRIPT,
                List.of("scheduler:tasks"),
                List.of(String.valueOf(now), workerId)
            );
            
            if (taskId != null) {
                String payload = jedis.hget("scheduler:task:" + taskId, "payload");
                executeTask(taskId, payload);
                jedis.del("scheduler:task:" + taskId);  // Clean up
            }
        }
    }
}
```

### Redis Cluster Considerations
- **Hash tags**: `{scheduler}:tasks` and `{scheduler}:task:123` hash to same slot
- **Redlock**: For distributed locking across Redis masters
- **Streams**: Redis 5.0+ can use Streams for event sourcing pattern

---

## 5. Disruptor-Based Scheduler (Ultra-Low Latency)

### Problem It Solves
> "Design a scheduler with sub-millisecond precision for high-frequency trading"

### Core Idea
```
┌─────────────────────────────────────────────────────────────┐
│                    DISRUPTOR RING BUFFER                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌───────────────────────────────────────────────────────┐ │
│  │  Ring Buffer (power-of-2 size, e.g., 1024 slots)       │ │
│  │                                                        │ │
│  │   ┌─────┬─────┬─────┬─────┬─────┬─────┬─────┬─────┐ │ │
│  │   │  0  │  1  │  2  │  3  │  4  │  5  │  6  │  7  │ │ │
│  │   └──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┴──┬──┘ │ │
│  │      │     │     │     │     │     │     │          │ │
│  │   ┌──┴─────┴─────┴─────┴─────┴─────┴─────┴──┐      │ │
│  │   │     Single Writer (Dispatcher)            │      │ │
│  │   │     CAS sequence++ ▶ claim slot            │      │ │
│  │   └───────────────────────────────────────────┘      │ │
│  │                                                       │ │
│  │   ┌───────────────────────────────────────────┐      │ │
│  │   │     Multiple Readers (Worker Threads)     │      │ │
│  │   │     Wait for sequence, process, notify  │      │ │
│  │   └───────────────────────────────────────────┘      │ │
│  │                                                       │ │
│  │  WaitStrategy: BusySpinWaitStrategy (no sleep!)       │ │
│  │  SequenceBarrier: Coordinates reader progress         │ │
│  └───────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Key Differences from DelayQueue
| Aspect | DelayQueue | Disruptor |
|--------|------------|-----------|
| Locking | ReentrantLock | Lock-free (CAS) |
| Sleep | Condition.await() | Busy-spin (burns CPU) |
| Latency | Milliseconds | Microseconds |
| Throughput | 100K ops/sec | 1M+ ops/sec |
| Memory | Object overhead | Pre-allocated, cache-line padded |

### Implementation Notes
```java
// Not a full implementation—illustrates approach
public class DisruptorScheduler {
    private final Disruptor<ScheduledEvent> disruptor;
    private final WaitStrategy waitStrategy = new BusySpinWaitStrategy();
    
    public DisruptorScheduler() {
        ThreadFactory threadFactory = r -> new Thread(r, "scheduler-worker");
        
        disruptor = new Disruptor<>(
            ScheduledEvent::new,
            1024,  // Ring buffer size (power of 2)
            threadFactory,
            ProducerType.SINGLE,  // One dispatcher thread
            waitStrategy
        );
        
        // Single event handler (worker)
        disruptor.handleEventsWith((event, sequence, endOfBatch) -> {
            event.getTask().execute();
        });
        
        disruptor.start();
    }
}
```

### Trade-off
> "You want sub-millisecond precision? Burn a CPU core busy-waiting."

---

## 6. Cron-Expression Scheduler (Calendar-Aware)

### Problem It Solves
> "Run tasks every Monday at 9 AM, except holidays, handling DST"

### Core Idea
```java
public class CronScheduler implements TaskScheduler {
    
    public ScheduledTask scheduleCron(String name, String cronExpr, Runnable task) {
        // Parse: "0 9 * * MON" (9 AM every Monday)
        CronExpression cron = new CronExpression(cronExpr);
        
        // Calculate next execution
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = cron.getNextValidTimeAfter(now);
        
        // Delegate to underlying scheduler
        return scheduleAt(name, task, next);
    }
}

// DST handling
class CronExpression {
    LocalDateTime getNextValidTimeAfter(LocalDateTime after) {
        // Handle spring forward: 2:00 AM becomes 3:00 AM
        // Skip missing hour, or adjust?
        
        // Handle fall back: 2:00 AM repeats (2:00-2:59 twice)
        // Execute once or twice?
    }
}
```

### Interview Curve Ball
> "Task scheduled for 2:30 AM daily. Clock springs forward 1 hour (DST)—what happens?"

**Answer Options:**
1. **Skip**: 2:30 AM doesn't exist that day, skip execution
2. **Execute once**: Run at 3:30 AM (adjusted time)
3. **Execute immediately**: When clock hits 3:00 AM, realize we missed 2:30
4. **Config-driven**: Let user decide policy

---

## 7. Message Broker Scheduler ("Buy vs Build")

### Problem It Solves
> "Why build a scheduler when message brokers already support delayed delivery?"

### Pulsar Example
```java
public class PulsarScheduler implements TaskScheduler {
    private final Producer<byte[]> producer;
    
    public ScheduledTask scheduleOnce(String name, Runnable task, long delay, TimeUnit unit) {
        // Serialize task
        byte[] payload = serialize(task);
        
        // Pulsar handles persistence + delayed delivery
        producer.newMessage()
            .key(name)
            .value(payload)
            .deliverAfter(delay, unit)
            .sendAsync();
        
        return new PulsarScheduledTask(name);
    }
}

// Consumer side (separate service)
class TaskConsumer implements MessageListener<byte[]> {
    @Override
    void received(Consumer<byte[]> consumer, Message<byte[]> msg) {
        Runnable task = deserialize(msg.getValue());
        task.run();
        consumer.acknowledge(msg);
    }
}
```

### Trade-off Analysis
| Approach | Pros | Cons |
|----------|------|------|
| **Build Custom** | Full control, no dependency, optimized for use case | Complexity, maintenance |
| **Use Pulsar** | Persistence, horizontal scale, battle-tested | Infrastructure dependency, latency |
| **Use DB** | Simple, existing infra, transactional | Polling overhead, precision |

### Interview Curve Ball
> "Your custom scheduler crashed and lost 1000 scheduled tasks. How do you prevent this?"

**Answer:** Persistence layer + WAL (Write-Ahead Log):
```
1. Write to WAL: "SCHEDULE task-123 at 09:00:00"
2. Ack to client
3. Async: Load into DelayQueue
4. On recovery: Replay WAL, reconstruct state
```

---

## Summary: Which One to Pick?

| Constraint | Recommended Implementation |
|------------|---------------------------|
| **< 10K tasks, interview demo** | DelayQueue (our implementation) |
| **> 1M short timeouts** | Hashed Wheel Timer |
| **Mixed delays (ms to days)** | Tiered Timing Wheels |
| **Must survive restart** | Database-Backed |
| **Distributed/clustered** | Redis-Backed |
| **Sub-millisecond latency** | Disruptor-Based |
| **Calendar rules (DST, holidays)** | Cron-Expression |
| **Existing message infrastructure** | Message Broker |

## Common Interview Follow-ups

1. **"How do you handle task failures?"**
   - Exponential backoff retry
   - Dead letter queue
   - Circuit breaker for cascading failures

2. **"What if a task runs longer than its interval?"**
   - Fixed rate: Skip or queue
   - Fixed delay: Next execution delayed by overrun
   - Timeout enforcement (interrupt long tasks)

3. **"How to cancel a running task?"**
   - Interrupt thread (cooperative cancellation)
   - Future.cancel(true) for executor-based
   - Separate "kill switch" flag

4. **"How do you monitor this?"**
   - Metrics: queue depth, lag, execution time
   - Health checks: dispatcher thread alive?
   - Alerting: tasks backing up, high failure rate
