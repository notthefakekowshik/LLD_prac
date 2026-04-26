# HikariCP Pool Sizing — Science Over Guesswork

> **Context:** Spring Boot backend engineer. No hand-wavy numbers like 10, 30, or 100.
> We derive the right pool size from measurable system properties.

---

## 1. Why Getting It Wrong Is Expensive

Most teams configure HikariCP like this:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10   # <- Why 10? "It's the default."
```

**Too small:**
- Threads queue behind the pool waiting for a connection
- `SQLTransientConnectionException: Unable to acquire JDBC Connection within 30000ms`
- Your application is serializing work that could be parallel

**Too large:**
- Hundreds of idle connections parked on the DB server
- DB allocates memory + file descriptors per connection: PostgreSQL uses ~5MB per connection
- Context-switch overhead on the DB side for too many active connections
- No additional throughput — you just add memory pressure and noise

The correct size sits in a sweet spot derived from **queuing theory**, specifically **Little's Law**.

---

## 2. Little's Law — The Foundation

Little's Law from queuing theory states:

```
L = λ × W

L  = average number of requests in the system (concurrency)
λ  = average arrival rate (requests per second, rps)
W  = average time each request spends in the system (latency, seconds)
```

Applied to your DB layer:

```
DB Connections Needed = Throughput (queries/sec) × Avg Query Latency (sec)
```

### Concrete Example

```
Your service handles: 500 requests/sec
Each request makes:   2 DB queries
→ DB query rate:      1,000 queries/sec

Average query time:   20ms = 0.020 sec

L = 1,000 × 0.020 = 20 connections
```

**That's it. You need ~20 connections, not 100.**

Add a 20–30% headroom buffer for bursts:

```
Pool size = ceil(L × 1.25) = ceil(20 × 1.25) = 25 connections
```

---

## 3. What You Actually Need to Measure

You cannot apply Little's Law without real numbers. Here is what to measure and where:

### 3.1 Query Rate (λ)

Instrument your application. In Spring Boot with Micrometer:

```java
// Actuator metrics auto-exposes HikariCP stats at:
// /actuator/metrics/hikaricp.connections.active
// /actuator/metrics/hikaricp.connections.pending
```

Or count from your query layer:

```sql
-- PostgreSQL: queries per second
SELECT sum(calls) / extract(epoch from (now() - stats_reset)) AS qps
FROM pg_stat_statements
WHERE query NOT LIKE '%pg_stat%';
```

### 3.2 Average Query Latency (W)

```sql
-- PostgreSQL: mean execution time per query type
SELECT query, calls, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;
```

Or from the application side, use Micrometer's `@Timed` or Spring Data JPA's query metrics.

### 3.3 Combining the Two

```
Scenario:
  Peak rps:          800
  Queries per req:   3
  λ (query rate):    2,400 queries/sec
  W (mean latency):  15ms = 0.015 sec

L = 2,400 × 0.015 = 36 connections
Pool size = ceil(36 × 1.25) = 45 connections
```

---

## 4. The DB Server Ceiling

Little's Law gives you the **application demand**. You must also respect the **DB server supply**.

### PostgreSQL

```sql
-- See configured max
SHOW max_connections;  -- default 100

-- Subtract internal/superuser overhead (3-5 connections)
-- Usable connections ≈ max_connections - 5

-- See active connections right now
SELECT count(*) FROM pg_stat_activity;
```

**PostgreSQL spawns a full OS process per connection.**
- Each process: ~5MB RAM
- `max_connections = 200` → ~1GB just for connection overhead before any actual queries run
- Rule: Keep `max_connections` at the minimum needed, not "just in case"

### MySQL / MariaDB

```sql
SHOW VARIABLES LIKE 'max_connections';  -- default 151
SHOW STATUS LIKE 'Threads_connected';
```

MySQL uses threads (cheaper than PG processes), but thread-per-connection still has limits.

### The Constraint

```
Effective pool size ≤ (DB max_connections - reserved connections) / number of app instances
```

Example:
```
DB max_connections:  100
Reserved for admin:    5
Usable:               95

App instances:         3

Max pool per instance = floor(95 / 3) = 31 connections
```

Cross-check against Little's Law result. **Take the minimum of the two.**

---

## 5. HikariCP Properties That Actually Matter

```yaml
spring:
  datasource:
    hikari:
      # The core limit — Little's Law result, capped by DB ceiling
      maximum-pool-size: 25

      # Minimum connections to keep alive (warm pool for burst absorption)
      # For consistent traffic: set equal to maximum-pool-size (avoids pool resize overhead)
      # For bursty/scheduled workloads: set lower (5–10)
      minimum-idle: 10

      # How long a thread waits for a connection before throwing
      # Should be > p99 query latency but < your HTTP timeout
      connection-timeout: 3000  # ms — don't set this > 30s

      # How long a connection can be idle before being evicted
      idle-timeout: 600000  # 10 minutes

      # How long a connection can live before being retired (avoid stale connections)
      max-lifetime: 1800000  # 30 minutes — must be less than DB wait_timeout

      # How long a query can run before HikariCP kills the connection
      # Set this generously; it's a circuit breaker, not a normal timeout
      validation-timeout: 5000  # ms

      # Pool name for metrics disambiguation (critical when you have multiple DataSources)
      pool-name: main-hikari-pool
```

### The One Setting Engineers Get Wrong: `minimum-idle`

Most guides set `minimum-idle = 5` regardless of workload.

| Traffic Pattern | Recommendation |
|----------------|----------------|
| Constant steady load | `minimum-idle = maximum-pool-size` (keeps pool full, no resize churn) |
| Bursty (e.g. batch jobs every 5 min) | `minimum-idle = 5–10` (save DB resources between bursts) |
| Always-on API with <5% variance | `minimum-idle = maximum-pool-size` |

HikariPool internally sizes up from `minimum-idle` to `maximum-pool-size` under load — that resize itself takes latency. For consistent traffic, just start full.

---

## 6. Multiple DataSources

If your service connects to two databases (e.g., read replica + primary), **each pool is independent**:

```
Primary (writes):   Little's Law on write query rate
Read replica:       Little's Law on read query rate

Total DB connections occupied = primary pool + replica pool
```

Each pool needs its own HikariCP config bean. Do not share pool instances.

---

## 7. Read Replicas and the Effect on Pool Size

If you route reads to a replica:

```
Before replica: 100% of queries hit primary
  λ = 2,400 qps → pool size = 45

After 80% reads offloaded to replica:
  Primary λ = 480 qps  → primary pool = ceil(480 × 0.015 × 1.25) = 9
  Replica  λ = 1,920 qps → replica pool  = ceil(1,920 × 0.015 × 1.25) = 36

Total connections across both: 45 (same)
But primary is no longer the bottleneck
```

---

## 8. The Connection Timeout Tells You When Sizing Is Wrong

HikariCP records `hikaricp.connections.pending` — threads waiting for a connection.

```
pending > 0 consistently     → pool too small OR queries too slow
pending = 0, pool mostly idle → pool too large, waste DB resources
pending spikes during bursts  → minimum-idle too low, or burst headroom insufficient
```

Enable HikariCP logging to see this in real time:

```yaml
logging:
  level:
    com.zaxxer.hikari: DEBUG
    com.zaxxer.hikari.pool.HikariPool: DEBUG
```

Key log lines to watch:
```
HikariPool-1 - Pool stats (total=25, active=22, idle=3, waiting=4)
                                                              ^^^^^^
                                               waiting > 0 = pool exhausted
```

---

## 9. The Problems Virtual Threads Cannot Fix

This is the key insight when moving to Java 21+ virtual threads:

```
Virtual threads remove:  OS thread cost (memory, context-switch)
Virtual threads do NOT:  Add more DB connections
                         Reduce query latency
                         Expand DB server capacity
```

| Bottleneck | Virtual Threads Help? | Real Fix |
|------------|-----------------------|----------|
| Too many OS threads blocked on I/O | ✅ Yes | Already solved |
| DB connection pool exhausted | ❌ No | Size pool with Little's Law |
| Slow queries (full table scan, missing index) | ❌ No | Index, EXPLAIN ANALYZE, query rewrite |
| DB server CPU saturated | ❌ No | Read replica, query optimisation, caching |
| DB server memory pressure (too many connections) | ❌ Makes it worse | Reduce pool size |
| High query latency under DB lock contention | ❌ No | Optimistic locking, shorter transactions |
| Downstream API latency (third-party HTTP) | ✅ Yes (threads unmount) | + Circuit breaker + timeout |

---

## 10. Iterative Tuning Process

```
Step 1: Baseline
  - Load test at realistic peak rps
  - Record: avg query latency (W), query rate (λ)
  - Record: hikaricp.connections.active at peak

Step 2: Apply Little's Law
  - Calculate L = λ × W
  - Add 25% buffer
  - Cap at (DB max_connections - reserved) / instances

Step 3: Set and observe
  - Deploy with calculated pool size
  - Monitor: pending connections, active connections, connection-timeout exceptions

Step 4: Tune
  - If pending > 0 under normal (non-peak) load → increase pool size
  - If active is always < 40% of maximum → decrease pool size
  - If connection-timeout exceptions appear → check query latency first before increasing pool
  - If DB CPU is high with large pool → your queries are the problem, not pool size

Step 5: Repeat at each traffic milestone
  (2× traffic ≈ 2× pool size, proportional by Little's Law)
```

---

## 11. Quick Reference: Formulas

```
Pool size  =  ceil( λ × W × 1.25 )

λ  = peak query rate (queries/sec)
     = rps × avg queries per request

W  = avg query execution time (seconds)
     = measured from pg_stat_statements or Micrometer

Hard cap = floor( (DB max_connections - 5) / app_instance_count )

Final pool = min( Little's Law result, Hard cap )
```

### Example Numbers Cheat Sheet

| Scenario | λ (qps) | W (ms) | L (connections) | +25% buffer |
|----------|---------|--------|-----------------|-------------|
| Light API | 200 | 10 | 2 | 3 |
| Typical CRUD service | 1,000 | 20 | 20 | 25 |
| Heavy read workload | 3,000 | 15 | 45 | 57 |
| Analytics queries | 100 | 500 | 50 | 63 |
| Write-heavy OLTP | 2,000 | 30 | 60 | 75 |

---

## 12. Summary

1. **Little's Law is the formula**: `connections = query_rate × query_latency`
2. **Measure, don't guess** — `pg_stat_statements` + HikariCP Micrometer metrics are your inputs
3. **DB server has a hard ceiling** — always cross-check against `max_connections` and instance count
4. **`maximum-pool-size` ≠ `minimum-idle`** — distinguish steady vs bursty workloads
5. **`connection-timeout` exceptions mean slow queries**, not always pool-too-small
6. **Virtual threads move the bottleneck** from OS threads to the DB pool — pool sizing becomes more critical, not less
7. **Re-derive pool size** every time throughput doubles or query latency changes significantly
