# In-Memory Cache (LRU/LFU + TTL)

## Functional Requirements

- User can **put** a key-value pair into the cache
- User can **get** a value by key (returns null if absent or expired)
- User can **remove** a key from the cache
- Cache automatically **evicts** the least recently/frequently used entry when full
- Cache automatically **expires** entries after a configurable TTL (time-to-live)

## Non-Functional Requirements

- Thread-safe: concurrent reads and writes must not corrupt state
- O(1) average put/get/remove operations
- TTL enforcement: lazy check on access + background periodic cleanup
- Eviction policy must be swappable without modifying the cache

## Constraints

- In-memory only — no persistence
- Capacity-based eviction (by entry count, not byte size)
- Single JVM process

## Out of Scope

- Distributed caching
- Disk-backed storage
- Cache warming / pre-loading
- Metrics / hit-rate tracking

---

## Class Diagram

```mermaid
classDiagram
    class Cache~K,V~ {
        -Storage~K,V~ storage
        -EvictionPolicy~K~ evictionPolicy
        -long ttlMillis
        -ScheduledExecutorService cleanupService
        +put(K, V) void
        +get(K) V
        +remove(K) void
        +shutdown() void
        -evictExpiredEntries() void
        -isExpired(CacheEntry~V~) boolean
    }

    class Storage~K,V~ {
        <<interface>>
        +add(K, CacheEntry~V~) void
        +remove(K) void
        +get(K) CacheEntry~V~
        +size() int
        +getAllKeys() Set~K~
    }

    class HashMapStorage~K,V~ {
        -ConcurrentHashMap~K,CacheEntry~V~~ storageMap
        -int capacity
        +add(K, CacheEntry~V~) void
        +remove(K) void
        +get(K) CacheEntry~V~
        +size() int
        +getAllKeys() Set~K~
    }

    class EvictionPolicy~K~ {
        <<interface>>
        +keyAccessed(K) void
        +evictKey() K
        +removeKey(K) void
    }

    class LRUEvictionPolicy~K~ {
        -LinkedHashSet~K~ keyOrder
        +keyAccessed(K) void
        +evictKey() K
        +removeKey(K) void
    }

    class LFUEvictionPolicy~K~ {
        -Map~K,Integer~ frequencyMap
        +keyAccessed(K) void
        +evictKey() K
        +removeKey(K) void
    }

    class CacheEntry~V~ {
        -V value
        -Instant createdAt
        -Instant lastAccessedAt
        +updateLastAccessed() void
        +getValue() V
        +getCreatedAt() Instant
        +getLastAccessedAt() Instant
    }

    class CacheFullException {
        +CacheFullException(String)
    }

    Cache~K,V~ "1" *-- "1" Storage~K,V~ : composition
    Cache~K,V~ "1" *-- "1" EvictionPolicy~K~ : composition (Strategy)
    Cache~K,V~ ..> CacheEntry~V~ : creates
    Storage~K,V~ <|.. HashMapStorage~K,V~ : realization
    EvictionPolicy~K~ <|.. LRUEvictionPolicy~K~ : realization
    EvictionPolicy~K~ <|.. LFUEvictionPolicy~K~ : realization
    HashMapStorage~K,V~ ..> CacheFullException : throws
    Storage~K,V~ ..> CacheEntry~V~ : uses
```

---

## Design Decisions

| Decision | Rationale |
|---|---|
| `EvictionPolicy` as Strategy interface | Swap LRU ↔ LFU ↔ FIFO without touching `Cache` (OCP) |
| `Storage` as interface | Swap `HashMap` for off-heap or Redis without touching `Cache` (DIP) |
| `synchronized` on `Cache` methods | Coarse-grained lock; simple and correct for this scope |
| Dual TTL enforcement (lazy + background) | Lazy catches expired entries on access; background reclaims memory proactively |
| `LinkedHashSet` in LRU | Maintains insertion order; O(1) remove + re-insert on access |
| `ConcurrentHashMap` in `HashMapStorage` | Background cleanup thread reads `getAllKeys()` concurrently with main thread |

---

## Package Structure

```
cache/
├── Cache.java                    ← Core orchestration
├── CacheMain.java                ← Demo runner
├── model/
│   └── CacheEntry.java           ← Value wrapper with TTL metadata
├── policy/
│   ├── EvictionPolicy.java       ← Strategy interface
│   ├── LRUEvictionPolicy.java    ← Least Recently Used
│   └── LFUEvictionPolicy.java    ← Least Frequently Used (Curveball)
├── storage/
│   ├── Storage.java              ← Storage abstraction
│   └── HashMapStorage.java       ← ConcurrentHashMap implementation
└── exception/
    └── CacheFullException.java
```

---

## Curveball: Add LFU Eviction

**Requirement:** Support Least Frequently Used eviction in addition to LRU.

**Solution:** New class `LFUEvictionPolicy<K>` implementing `EvictionPolicy<K>`. Zero changes to `Cache`, `Storage`, or `LRUEvictionPolicy`.

**Lesson (OCP):** The `EvictionPolicy` interface was the right abstraction — adding LFU required only a new file, not a single edit to existing classes.

**Trade-off:**

| Policy | Evicts | Best For |
|---|---|---|
| LRU | Least recently accessed | General workloads, recency matters |
| LFU | Least frequently accessed | Workloads with stable "hot" keys |

---

## Usage

```java
Storage<String, String> storage = new HashMapStorage<>(100);
EvictionPolicy<String> policy = new LRUEvictionPolicy<>();
Cache<String, String> cache = new Cache<>(storage, policy, 60_000); // 60s TTL

cache.put("user:1", "Alice");
String value = cache.get("user:1"); // "Alice"
cache.remove("user:1");
cache.shutdown();
```
