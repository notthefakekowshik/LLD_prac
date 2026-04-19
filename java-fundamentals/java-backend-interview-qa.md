# Java/Backend Interview Q&A

## 1. How HashMap Works Internally

**Q: Explain the internal working of HashMap in Java.**

**A:**
- HashMap stores key-value pairs in an array of `Node<K,V>` objects (buckets)
- When `put(key, value)` is called:
  1. `hashCode()` of key is computed and processed through `hash()` function: `(h = key.hashCode()) ^ (h >>> 16)`
  2. Index is calculated: `(n - 1) & hash` where n is table length
  3. If bucket is empty, new node is created
  4. If collision occurs, entries are stored as linked list (Java 7) or tree (Java 8+ if list length > 8)
- Default capacity: 16, load factor: 0.75
- When size exceeds `capacity * load factor`, rehashing occurs (doubling capacity)
- Get operation uses same hashing to locate the bucket and traverse if needed

---

## 2. ConcurrentHashMap vs HashMap

**Q: What are the differences between ConcurrentHashMap and HashMap?**

**A:**

| Aspect | HashMap | ConcurrentHashMap |
|--------|---------|-------------------|
| Thread Safety | Not thread-safe | Thread-safe |
| Null Keys/Values | Allows one null key, multiple null values | Does not allow null keys or values |
| Performance | Faster (no synchronization overhead) | Slower but scalable |
| Iterator | Fail-fast | Weakly consistent |
| Locking | N/A | Segment-level locking (Java 7) / CAS + synchronized (Java 8+) |

- ConcurrentHashMap uses fine-grained locking in Java 8: locks only at bucket level during write
- Read operations are generally lock-free
- Preferred for concurrent environments over `Collections.synchronizedMap()`

---

## 3. String Immutability in Java

**Q: Why is String immutable in Java?**

**A:**
- **Security**: String literals are used for network connections, file paths, etc. Immutability prevents tampering
- **String Pool**: Enables string interning in the constant pool, saving memory
- **HashCode Caching**: HashCode is computed once and cached (improves HashMap performance)
- **Synchronization**: Immutable objects are inherently thread-safe, no synchronization needed
- **Performance**: Eliminates need for defensive copies when passing strings between methods/threads

```java
// String pool example
String s1 = "Hello";  // Goes to string pool
String s2 = "Hello";  // Reuses from pool
String s3 = new String("Hello");  // Creates new object in heap
```

---

## 4. ArrayList vs LinkedList

**Q: Compare ArrayList and LinkedList.**

**A:**

| Operation | ArrayList | LinkedList |
|-----------|-----------|------------|
| Internal Structure | Dynamic array | Doubly-linked list |
| Access (get) | O(1) | O(n) |
| Add/Remove at end | O(1) amortized | O(1) |
| Add/Remove in middle | O(n) | O(1) if position known |
| Memory Overhead | Less | More (node objects) |
| Random Access | Fast | Slow |
| Iteration | Cache-friendly | Poor cache locality |

**When to use:**
- ArrayList: Frequent random access, mostly read operations, add/remove at end
- LinkedList: Frequent insertions/deletions in middle, memory not a constraint

---

## 5. Garbage Collection in Java

**Q: Explain Garbage Collection in Java and its types.**

**A:**
GC automatically reclaims memory from unreachable objects.

**Generational Hypothesis:** Most objects die young

**Heap Structure:**
- Young Generation (Eden, Survivor0, Survivor1)
- Old Generation (Tenured)
- Permanent Generation / Metaspace

**GC Algorithms:**
- **Serial GC**: Single-threaded, STW (Stop-The-World)
- **Parallel GC**: Multi-threaded throughput collector
- **CMS (Concurrent Mark Sweep)**: Low latency, deprecated in Java 14
- **G1 GC**: Region-based, target pause time, default since Java 9
- **ZGC**: Ultra-low latency (<10ms), scalable to TB heaps
- **Shenandoah**: Low latency with concurrent compaction

**Phases:**
1. Mark: Identify live objects
2. Sweep: Remove dead objects
3. Compact: Defragment memory (optional)

---

## 6. JVM vs JRE vs JDK

**Q: What is the difference between JVM, JRE, and JDK?**

**A:**

**JVM (Java Virtual Machine)**
- Runtime engine that executes Java bytecode
- Provides platform independence (Write Once, Run Anywhere)
- Includes JIT compiler, GC, classloader, execution engine

**JRE (Java Runtime Environment)**
- JVM + Core Libraries + Supporting Files
- Minimum environment to run Java applications
- Does not include development tools

**JDK (Java Development Kit)**
- JRE + Development Tools
- Includes: javac (compiler), javadoc, jar, jdb (debugger), javap
- Required for developing Java applications

```
JDK = JRE + Development Tools
JRE = JVM + Core Libraries + Runtime Files
```

---

## 7. Checked vs Unchecked Exceptions

**Q: Differentiate between Checked and Unchecked Exceptions.**

**A:**

| Feature | Checked Exceptions | Unchecked Exceptions |
|---------|-------------------|---------------------|
| Inheritance | Extends Exception (not RuntimeException) | Extends RuntimeException |
| Compile-time check | Yes | No |
| Handling | Must handle or declare with throws | Optional handling |
| Examples | IOException, SQLException | NullPointerException, IllegalArgumentException |
| Use case | Recoverable conditions | Programming errors |

**Best Practice:**
- Use checked exceptions for conditions client can recover from
- Use unchecked for programming errors or unrecoverable conditions
- Never catch and ignore exceptions without logging

---

## 8. try-catch-finally Flow

**Q: Explain the execution flow of try-catch-finally blocks.**

**A:**

**Basic Flow:**
1. Try block executes
2. If exception occurs, matching catch block executes
3. Finally block always executes (unless JVM exits)

**Special Cases:**
- **Return in try**: Finally executes before return
- **Return in catch**: Finally executes before return
- **Exception in finally**: Suppresses any previous exception
- **System.exit()**: Finally won't execute

```java
try {
    return 1;  // executes, stores result
} finally {
    return 2;  // overrides previous return
}
// Returns 2
```

**try-with-resources (Java 7+):**
```java
try (Resource r = new Resource()) {
    // Auto-closeable resources automatically closed
} catch (Exception e) {
    // Handle exception
}
```

---

## 9. Multithreading, Synchronization, Race Conditions

**Q: Explain multithreading, synchronization, and race conditions in Java.**

**A:**

**Multithreading:**
- Multiple threads executing concurrently within a single process
- Threads share heap memory but have separate stack
- Created via: extending Thread, implementing Runnable, or using ExecutorService

**Race Condition:**
- Occurs when multiple threads access shared data concurrently
- At least one thread performs a write operation
- Result depends on the non-deterministic timing of thread execution

**Synchronization:**
- `synchronized` keyword ensures mutual exclusion
- Can be applied to methods or blocks
- Uses intrinsic locks (monitor locks) per object

```java
// Method-level
public synchronized void increment() { count++; }

// Block-level (preferred - finer granularity)
public void increment() {
    synchronized(this) { count++; }
}
```

**Alternatives to synchronized:**
- `ReentrantLock` (explicit locks, more flexible)
- `AtomicInteger`, `AtomicReference` (CAS operations)
- `volatile` (visibility guarantee only)

---

## 10. Runnable vs Callable

**Q: What are the differences between Runnable and Callable?**

**A:**

| Feature | Runnable | Callable |
|---------|----------|----------|
| Return value | void | Returns a value of type V |
| Exception | Cannot throw checked exceptions | Can throw checked exceptions |
| Method | `run()` | `call()` |
| Since | Java 1.0 | Java 5 (java.util.concurrent) |
| Future | Cannot be used with Future directly | Returns Future<V> |

```java
// Runnable
ExecutorService executor = Executors.newFixedThreadPool(2);
executor.submit(() -> System.out.println("Task done"));

// Callable
Future<Integer> future = executor.submit(() -> {
    Thread.sleep(1000);
    return 42;
});
Integer result = future.get();  // Blocks until available
```

---

## 11. Executor Framework

**Q: Explain the Executor Framework in Java.**

**A:**
The Executor Framework (java.util.concurrent since Java 5) decouples task submission from execution mechanics.

**Key Components:**
- **Executor**: Basic interface with `execute(Runnable)`
- **ExecutorService**: Adds lifecycle management, task submission methods
- **ScheduledExecutorService**: Adds scheduling capabilities

**Thread Pool Types:**
```java
Executors.newFixedThreadPool(n);        // Fixed size pool
Executors.newCachedThreadPool();         // Elastic pool
Executors.newSingleThreadExecutor();     // Sequential execution
Executors.newScheduledThreadPool(n);   // Scheduled tasks
Executors.newWorkStealingPool();         // ForkJoinPool-based (Java 8)
```

**Best Practices:**
- Prefer `ThreadPoolExecutor` constructor for custom configuration
- Always shut down: `executor.shutdown()` or `shutdownNow()`
- Use `CompletableFuture` (Java 8+) for composing async operations
- Set thread names and use appropriate rejection policies

---

## 12. Deadlock, Starvation, Livelock

**Q: Explain Deadlock, Starvation, and Livelock.**

**A:**

**Deadlock:**
- Two or more threads blocked forever, waiting for each other
- Coffman conditions: Mutual exclusion, Hold and wait, No preemption, Circular wait

```java
// Classic deadlock scenario
Thread 1: lock A -> request B
Thread 2: lock B -> request A
```

**Prevention:**
- Lock ordering (always acquire locks in same order)
- Try-lock with timeout: `lock.tryLock(timeout, TimeUnit.SECONDS)`
- Use `Lock` instead of `synchronized` for more control

**Starvation:**
- Thread unable to gain access to shared resource because other threads have priority
- Common with unfair locks or improper thread priority

**Livelock:**
- Threads are not blocked but keep responding to each other without making progress
- Similar to deadlock but threads are active, not waiting

---

## 13. volatile vs synchronized

**Q: Compare volatile and synchronized keywords.**

**A:**

| Feature | volatile | synchronized |
|---------|----------|--------------|
| Visibility | Yes (all threads see latest value) | Yes |
| Atomicity | No | Yes |
| Memory barrier | Read/write barriers | Enter/exit barriers |
| Blocking | Non-blocking | Blocking |
| Use case | Simple flags, status indicators | Compound actions, critical sections |

**volatile guarantees:**
1. Visibility: Changes visible to all threads immediately
2. Ordering: Prevents instruction reordering

**When to use volatile:**
- Single read/write operations on primitive variables
- Status flags (e.g., `volatile boolean running = true`)
- Happens-before relationship

```java
// NOT thread-safe (check-then-act race condition)
if (volatileCounter == 0) {
    volatileCounter++;  // Not atomic!
}

// Thread-safe
synchronized(this) {
    if (counter == 0) {
        counter++;
    }
}
```

---

## 14. Java 8 Streams

**Q: Explain Java 8 Streams and their characteristics.**

**A:**
Streams provide functional-style operations on collections.

**Key Characteristics:**
- Not data structures, but views for computation
- Lazy evaluation (intermediate ops don't execute until terminal op)
- Can be consumed only once
- Support parallel execution with `.parallelStream()`

**Operations:**
```java
// Intermediate (lazy, return Stream)
filter(), map(), flatMap(), distinct(), sorted(), limit(), peek()

// Terminal (trigger execution)
collect(), forEach(), reduce(), count(), anyMatch(), findFirst(), toArray()

// Short-circuiting
anyMatch(), allMatch(), noneMatch(), findFirst(), findAny()
```

**Example:**
```java
List<Integer> evens = numbers.stream()
    .filter(n -> n % 2 == 0)
    .map(n -> n * n)
    .collect(Collectors.toList());
```

**Best Practices:**
- Avoid side effects in stream operations
- Prefer method references for readability
- Consider parallel streams only for large datasets

---

## 15. Optional in Java

**Q: What is Optional in Java and how should it be used?**

**A:**
`Optional<T>` is a container object that may or may not contain a non-null value (introduced in Java 8).

**Purpose:**
- Avoid NullPointerExceptions
- Express optional values explicitly in API design
- Encourage null-checking at compile time

**Key Methods:**
```java
Optional.of(value);           // Throws NPE if null
Optional.ofNullable(value);   // Allows null
Optional.empty();             // Empty optional

optional.isPresent();         // Check if value exists
optional.ifPresent(consumer); // Execute if value exists
optional.orElse(defaultVal);  // Get value or default
optional.orElseGet(supplier); // Lazy default evaluation
optional.orElseThrow();       // Throw if empty
optional.map(Function);       // Transform if present
optional.flatMap(Function);   // Chain optionals
```

**Best Practices:**
- Never use `Optional.get()` without `isPresent()` check
- Don't use Optional as method parameters or fields (usually)
- Prefer `orElseGet()` over `orElse()` for expensive defaults

---

## 16. LRU Cache Design in Java

**Q: Design an LRU (Least Recently Used) Cache in Java.**

**A:**

**Approach:**
- Use `LinkedHashMap` with accessOrder=true, or
- Combine `HashMap` with `DoublyLinkedList` for O(1) operations

**LinkedHashMap Approach:**
```java
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    
    public LRUCache(int capacity) {
        super(capacity, 0.75f, true);  // accessOrder=true
        this.capacity = capacity;
    }
    
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

**Custom Implementation (Interview Standard):**
```java
class LRUCache {
    private final int capacity;
    private final Map<Integer, Node> cache;
    private final DoublyLinkedList dll;
    
    // O(1) get and put
    // HashMap for O(1) lookup
    // DoublyLinkedList for O(1) move-to-head and removal
}
```

---

## 17. Microservices vs Monolith

**Q: Compare Microservices and Monolithic architectures.**

**A:**

| Aspect | Monolith | Microservices |
|--------|----------|---------------|
| Deployment | Single deployable unit | Multiple independent services |
| Scalability | Scale entire application | Scale individual services |
| Technology | Single stack | Polyglot (different per service) |
| Complexity | Simpler initially | Higher operational complexity |
| Data | Shared database | Database per service |
| Communication | In-process | Network (HTTP/gRPC/messaging) |
| Fault isolation | Lower | Higher |
| Team structure | Co-located teams | Autonomous, cross-functional teams |

**When to choose:**
- **Monolith**: Small team, rapid prototyping, simple domain
- **Microservices**: Large scale, independent deployability, diverse tech needs

**Challenges with Microservices:**
- Distributed transactions
- Service discovery, load balancing
- Observability and monitoring
- Data consistency across services

---

## 18. Idempotent APIs

**Q: What are Idempotent APIs and why are they important?**

**A:**
An operation is idempotent if performing it multiple times produces the same result as performing it once.

**HTTP Methods:**
- **Idempotent**: GET, PUT, DELETE, HEAD, OPTIONS
- **Non-idempotent**: POST, PATCH (generally)

**Examples:**
```
PUT /users/123 {name: "John"}  // Idempotent - same result every time
POST /users {name: "John"}      // Non-idempotent - creates new user each time
```

**Implementation Strategies:**
1. **Idempotency Keys**: Client generates unique key, server tracks processed keys
2. **ETags/Conditional Requests**: `If-Match` header for optimistic locking
3. **Database Constraints**: Unique constraints prevent duplicate inserts
4. **Token Buckets**: Rate limiting with token validation

**Use Cases:**
- Retry logic in unreliable networks
- Payment processing (prevent double charges)
- Webhook handling

---

## 19. SQL vs NoSQL

**Q: Compare SQL and NoSQL databases.**

**A:**

| Feature | SQL (Relational) | NoSQL |
|---------|------------------|-------|
| Schema | Fixed schema | Schema-less / flexible |
| Scalability | Vertical scaling | Horizontal scaling |
| ACID | Full ACID support | Varies (eventual consistency common) |
| Joins | Supported | Generally not supported |
| Data | Structured | Structured, semi-structured, unstructured |
| Use case | Complex queries, transactions | High write throughput, flexible data |

**NoSQL Types:**
- **Document** (MongoDB): JSON-like documents
- **Key-Value** (Redis, DynamoDB): Simple lookup by key
- **Column-Family** (Cassandra): Wide columns, high write throughput
- **Graph** (Neo4j): Relationship-heavy data

**When to use:**
- SQL: Complex relationships, strict consistency, analytical queries
- NoSQL: Rapid iteration, massive scale, unstructured data, high write rates

---

## 20. Database Indexing

**Q: Explain Database Indexing and its types.**

**A:**
Indexes speed up data retrieval at the cost of additional storage and slower writes.

**Index Types:**

| Type | Description | Use Case |
|------|-------------|----------|
| B-Tree | Default balanced tree index | Range queries, equality searches |
| Hash | Hash table structure | Exact match only |
| Bitmap | Bitmap per distinct value | Low cardinality columns |
| Full-text | Inverted index | Text search |
| Composite | Multiple columns | Multi-column queries |
| Covering | Includes all queried columns | Avoid table lookups |
| Clustered | Data stored in index order | Primary key (usually one per table) |

**Best Practices:**
- Index columns used in WHERE, JOIN, ORDER BY clauses
- Don't over-index (slows INSERT/UPDATE/DELETE)
- Consider index selectivity (cardinality)
- Use EXPLAIN to analyze query plans

**When NOT to index:**
- Small tables
- Columns with low cardinality (e.g., boolean)
- Frequently updated columns

---

## 21. Redis Caching Use Cases

**Q: What are common Redis caching use cases?**

**A:**

**Primary Use Cases:**

1. **Session Storage**
   - Distributed session management across multiple app servers
   - TTL for automatic expiration

2. **Rate Limiting**
   - Token bucket or sliding window algorithms
   - Atomic INCR/EXPIRE operations

3. **Leaderboards/Sorted Sets**
   - `ZADD`, `ZREVRANGE` for real-time rankings
   - Gaming, analytics dashboards

4. **Real-time Analytics**
   - HyperLogLog for cardinality estimation
   - Pub/Sub for real-time notifications

5. **Caching Layer**
   - Cache-aside or write-through patterns
   - Frequently accessed database queries

6. **Distributed Locks**
   - `SET resource_name my_random_value NX PX 30000`
   - Redlock algorithm for multiple Redis instances

7. **Message Queue**
   - `LPUSH/RPOP` or `BLPOP` for reliable queues
   - Redis Streams for event sourcing

**Data Structures:**
- Strings: Caching, counters
- Hashes: Objects, sessions
- Lists: Queues, timelines
- Sets: Tags, relationships
- Sorted Sets: Leaderboards, time-series

---

## 22. Pagination for Large Datasets

**Q: How do you implement pagination for large datasets?**

**A:**

**Offset-Based Pagination (Traditional):**
```sql
SELECT * FROM orders LIMIT 10 OFFSET 10000;
```
- Simple but slow for deep pages (O(offset + limit))
- Inconsistent results if data changes during pagination

**Cursor-Based Pagination (Keyset):**
```sql
SELECT * FROM orders 
WHERE (created_at, id) > (last_created_at, last_id) 
ORDER BY created_at, id 
LIMIT 10;
```
- O(limit) performance regardless of page depth
- Consistent results, no skipping/duplicates
- Requires unique, sortable column(s)

**Seek Method:**
- Similar to cursor-based but uses only one column
- More efficient but less flexible

**Hybrid Approach (Google/Instagram style):**
- Show approximate page numbers for UX
- Use cursor for actual data fetching

**Best Practices:**
- Always have index on ORDER BY columns
- Consider denormalized search engines (Elasticsearch) for complex filtering
- Implement max page limits to prevent abuse

---

## 23. REST API Best Practices

**Q: What are REST API best practices?**

**A:**

**1. Resource Naming**
```
GET /users          # Collection
GET /users/123      # Single resource
GET /users/123/orders  # Sub-resource
```

**2. HTTP Methods**
- GET: Read
- POST: Create
- PUT/PATCH: Update (PUT = full replace, PATCH = partial)
- DELETE: Remove

**3. Status Codes**
- 200 OK, 201 Created, 204 No Content
- 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found
- 500 Internal Server Error, 502/503/504 Gateway errors

**4. Versioning**
```
/api/v1/users
/api/v2/users
# or header: Accept: application/vnd.api.v2+json
```

**5. Request/Response Format**
```json
{
  "data": { ... },
  "meta": { "total": 100 },
  "error": { "code": "ERR_001", "message": "..." }
}
```

**6. Filtering, Sorting, Field Selection**
```
GET /users?role=admin&sort=-created_at&fields=id,name,email
```

**7. Other Best Practices:**
- Use HATEOAS for discoverability (optional)
- Implement rate limiting
- Use HTTPS only
- Support caching headers (ETag, Last-Modified)
- Document with OpenAPI/Swagger

---

## 24. Authentication vs Authorization

**Q: What is the difference between Authentication and Authorization?**

**A:**

| Aspect | Authentication | Authorization |
|--------|---------------|---------------|
| Definition | Verifying identity | Verifying permissions |
| Question | "Who are you?" | "What can you do?" |
| Timing | First step | After authentication |
| Methods | Passwords, tokens, biometrics, MFA | Roles, permissions, policies (RBAC, ABAC) |

**Authentication Methods:**
- **Session-based**: Server-side sessions with cookies
- **Token-based**: JWT, OAuth 2.0
- **MFA**: SMS, TOTP, hardware keys

**Authorization Models:**
- **RBAC** (Role-Based): Roles assigned permissions, users assigned roles
- **ABAC** (Attribute-Based): Policies based on user/resource/environment attributes
- **ACL** (Access Control List): Direct user-resource permission mapping

**JWT Structure:**
```
Header.Payload.Signature
```
- Stateless authentication
- Contains claims (user info, permissions)
- Must validate signature and expiration

---

## 25. Connection Pooling

**Q: Explain Connection Pooling and its benefits.**

**A:**
Connection pooling maintains a cache of database connections that can be reused.

**Why Pooling:**
- Creating connections is expensive (TCP handshake, auth, memory allocation)
- Prevents connection exhaustion
- Improves application response time

**Common Pool Libraries:**
- **Java**: HikariCP (fastest), Apache DBCP, c3p0
- **Node**: generic-pool, pg-pool

**Key Configuration Parameters:**
| Parameter | Description |
|-----------|-------------|
| minimumIdle | Minimum connections to maintain |
| maximumPoolSize | Maximum connections allowed |
| connectionTimeout | Max wait for connection |
| idleTimeout | Max time connection can sit idle |
| maxLifetime | Max lifetime of a connection |
| leakDetectionThreshold | Log warnings for unreleased connections |

**Best Practices:**
- Size pool based on database connection limit and app instances
- Monitor `connectionWaitMillis` and `totalConnections`
- Enable prepared statement caching
- Use connection validation (testOnBorrow/testWhileIdle)

---

## 26. Debugging a Slow Spring Boot API

**Q: How would you debug a slow Spring Boot API?**

**A:**

**1. Identify the Bottleneck**
```bash
# Enable actuator metrics
management.endpoints.web.exposure.include=*
management.metrics.enabled=true
```

**2. Database Layer**
- Enable SQL logging: `spring.jpa.show-sql=true`
- Use `EXPLAIN ANALYZE` on slow queries
- Check for N+1 query problems
- Verify connection pool metrics

**3. Application Profiling**
- Use Java Flight Recorder (JFR) or async-profiler
- Analyze CPU hotspots and memory allocation
- Check for thread contention with thread dumps

**4. External Service Calls**
- Add timeouts and circuit breakers (Resilience4j)
- Use distributed tracing (Micrometer + Zipkin/Jaeger)
- Monitor call latency with Micrometer metrics

**5. JVM Tuning**
- Check GC logs for frequent/lengthy pauses
- Adjust heap size: `-Xms -Xmx`
- Choose appropriate GC algorithm

**6. Infrastructure**
- CPU/Memory utilization on containers/VMs
- Network latency between services
- Load balancer health and distribution

**Tools:**
- Spring Boot Actuator + Micrometer
- Prometheus + Grafana for metrics
- ELK/EFK for log aggregation
- Distributed tracing (OpenTelemetry)

---

## 27. Dependency Injection in Spring

**Q: Explain Dependency Injection in Spring.**

**A:**
Dependency Injection (DI) is a design pattern where objects receive dependencies from external sources rather than creating them internally.

**DI Types:**
1. **Constructor Injection** (Preferred)
   ```java
   @Service
   public class OrderService {
       private final PaymentService paymentService;
       
       public OrderService(PaymentService paymentService) {
           this.paymentService = paymentService;
       }
   }
   ```

2. **Setter Injection**
   ```java
   @Autowired
   public void setPaymentService(PaymentService ps) { ... }
   ```

3. **Field Injection** (Not recommended)
   ```java
   @Autowired
   private PaymentService paymentService;
   ```

**Benefits:**
- Loose coupling between components
- Easier unit testing (mock dependencies)
- Single Responsibility Principle
- Lifecycle management by container

**Spring Bean Scopes:**
- singleton (default): One instance per container
- prototype: New instance per request
- request: One per HTTP request
- session: One per HTTP session

**Configuration Methods:**
- `@Component` scanning with `@ComponentScan`
- `@Bean` methods in `@Configuration` classes
- XML configuration (legacy)

---

## 28. Designing for High Throughput and Low Latency

**Q: How do you design systems for high throughput and low latency?**

**A:**

**1. Caching Strategy**
- Multi-layer: CDN -> Edge -> Application -> Database cache
- Cache-aside vs Write-through vs Write-behind
- Redis/Memcached for hot data

**2. Database Optimization**
- Read replicas for read-heavy workloads
- Sharding for write scalability
- Proper indexing and query optimization
- Connection pooling (HikariCP)

**3. Asynchronous Processing**
- Message queues (Kafka, RabbitMQ) for decoupling
- Reactive programming (WebFlux, RxJava)
- Async I/O and non-blocking operations

**4. Concurrency**
- Thread pool tuning based on workload type (CPU vs I/O bound)
- Fork-Join for parallel processing
- Lock-free data structures where possible

**5. Network Optimization**
- Connection keep-alive and HTTP/2
- Efficient serialization (Protobuf, Avro over JSON)
- Load balancing strategies (least connections, consistent hashing)

**6. JVM Tuning**
- G1GC or ZGC for low pause times
- Off-heap memory for large caches (Ehcache, Chronicle Map)
- JIT warm-up and code cache tuning

**7. Monitoring & Observability**
- Distributed tracing for latency analysis
- Percentile metrics (p50, p95, p99) not just averages
- Circuit breakers and bulkheads (Resilience4j)

**8. Architecture Patterns**
- CQRS for read/write separation
- Event sourcing for audit trails and replay
- Microservices with bounded contexts
