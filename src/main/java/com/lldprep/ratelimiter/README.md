# Rate Limiter - Implementation Guide

## Overview

A comprehensive, thread-safe rate limiting system implementing 5 different algorithms:
- **Token Bucket** - Allows bursts, best for APIs
- **Leaky Bucket** - Smooths bursts, best for traffic shaping
- **Fixed Window** - Simple but has boundary issues
- **Sliding Window Log** - Perfect accuracy, high memory
- **Sliding Window Counter** - Best balance for production

## Quick Start

```java
// Create a rate limiter: 100 requests per second
RateLimitConfig config = RateLimitConfig.builder()
    .maxRequests(100)
    .windowSize(Duration.ofSeconds(1))
    .build();

RateLimiter limiter = RateLimiterFactory.create(
    AlgorithmType.TOKEN_BUCKET,
    config
);

// Check if request is allowed
if (limiter.tryAcquire()) {
    // Process request
    processRequest();
} else {
    // Reject request (rate limit exceeded)
    return HTTP_429_TOO_MANY_REQUESTS;
}
```

## Algorithm Selection Guide

| Use Case | Recommended Algorithm | Why |
|----------|----------------------|-----|
| **Production API** | Sliding Window Counter | Best balance of accuracy and performance |
| **Allow burst traffic** | Token Bucket | Accumulates tokens, allows occasional spikes |
| **Strict constant rate** | Leaky Bucket | Smooths all bursts, enforces steady flow |
| **Security (login attempts)** | Sliding Window Log | Perfect accuracy, no boundary issues |
| **Simple/Low memory** | Fixed Window | O(1) space, but has 2x rate at boundaries |

## Algorithm Comparison

### Token Bucket 🪣
```java
RateLimiter limiter = RateLimiterFactory.createTokenBucket(config);
```

**How it works:**
- Bucket holds tokens
- Tokens refill at constant rate
- Each request consumes one token
- Can accumulate tokens up to capacity

**Characteristics:**
- ✅ Allows bursts (good for APIs)
- ✅ O(1) time, O(1) space
- ✅ Simple and efficient
- ⚠️ Can allow large bursts if bucket is full

**Best for:** API rate limiting, cloud quotas, allowing occasional traffic spikes

---

### Leaky Bucket 💧
```java
RateLimiter limiter = RateLimiterFactory.createLeakyBucket(config);
```

**How it works:**
- Requests enter a queue (bucket)
- Requests "leak" out at constant rate
- If queue is full, requests are denied

**Characteristics:**
- ✅ Smooths bursts (enforces constant rate)
- ✅ O(1) time
- ❌ O(n) space (queue size)
- ⚠️ Denies bursts even if system can handle them

**Best for:** Traffic shaping, network routers, background job processing

**Difference from Token Bucket:**
- Token Bucket: Allows immediate bursts if tokens available
- Leaky Bucket: Enforces constant rate, smooths all bursts

---

### Fixed Window 🪟
```java
RateLimiter limiter = RateLimiterFactory.createFixedWindow(config);
```

**How it works:**
- Divide time into fixed windows (e.g., 1-minute windows)
- Count requests in current window
- Reset counter at window boundary

**Characteristics:**
- ✅ O(1) time, O(1) space
- ✅ Very simple
- ❌ **Boundary problem** (can get 2x rate at edges)

**Boundary Problem Example:**
```
Window 1: [0s - 60s], limit = 100
Window 2: [60s - 120s], limit = 100

At 59s: 100 requests (allowed)
At 61s: 100 requests (allowed)
Result: 200 requests in 2 seconds! (2x rate)
```

**Best for:** Simple scenarios, analytics, low-memory constraints

---

### Sliding Window Log 📊
```java
RateLimiter limiter = RateLimiterFactory.createSlidingWindowLog(config);
```

**How it works:**
- Keep log of all request timestamps
- For each request, remove timestamps older than window
- Count remaining timestamps

**Characteristics:**
- ✅ Perfect accuracy (no boundary issues)
- ✅ True sliding window
- ❌ O(log n) time (TreeSet operations)
- ❌ O(n) space (stores all timestamps)
- ⚠️ Not suitable for very high traffic

**Best for:** Security (login attempts), critical systems, low-medium traffic

---

### Sliding Window Counter 🔄
```java
RateLimiter limiter = RateLimiterFactory.createSlidingWindowCounter(config);
```

**How it works:**
- Combines Fixed Window with weighted previous window
- Approximates true sliding window behavior

**Formula:**
```
estimatedCount = (prevWindowCount * overlapPercentage) + currentWindowCount

where:
  overlapPercentage = (windowSize - elapsedTimeInCurrentWindow) / windowSize
```

**Characteristics:**
- ✅ O(1) time, O(1) space
- ✅ Good accuracy (better than Fixed Window)
- ✅ No boundary problem
- ✅ Low memory usage

**Best for:** Production APIs, high-traffic systems (Cloudflare, AWS use this)

**Recommended for most use cases!**

---

## Configuration

### Basic Configuration
```java
RateLimitConfig config = RateLimitConfig.builder()
    .maxRequests(1000)              // Required: max requests in window
    .windowSize(Duration.ofMinutes(1))  // Required: time window
    .build();
```

### With Burst Size (Token Bucket)
```java
RateLimitConfig config = RateLimitConfig.builder()
    .maxRequests(100)               // 100 requests/second average
    .windowSize(Duration.ofSeconds(1))
    .burstSize(200)                 // Allow bursts up to 200
    .build();
```

### Common Configurations

**API Rate Limiting:**
```java
// 1000 requests per minute
RateLimitConfig.builder()
    .maxRequests(1000)
    .windowSize(Duration.ofMinutes(1))
    .build();
```

**Login Attempt Limiting:**
```java
// 5 attempts per 15 minutes
RateLimitConfig.builder()
    .maxRequests(5)
    .windowSize(Duration.ofMinutes(15))
    .build();
```

**File Upload Limiting:**
```java
// 10 uploads per hour
RateLimitConfig.builder()
    .maxRequests(10)
    .windowSize(Duration.ofHours(1))
    .build();
```

---

## Per-User Rate Limiting

Use `UserRateLimiterRegistry` for per-user/per-resource limiting:

```java
// Create registry with default config
RateLimitConfig config = RateLimitConfig.builder()
    .maxRequests(100)
    .windowSize(Duration.ofSeconds(1))
    .build();

UserRateLimiterRegistry registry = new UserRateLimiterRegistry(
    AlgorithmType.SLIDING_WINDOW_COUNTER,
    config
);

// Get limiter for specific user (created lazily)
RateLimiter userLimiter = registry.getLimiter("user-123");

if (userLimiter.tryAcquire()) {
    // Process request for user-123
}

// Each user has independent rate limit
RateLimiter anotherUser = registry.getLimiter("user-456");
```

**Use Cases:**
- API rate limiting per API key
- Resource quotas per tenant
- Per-IP rate limiting
- Per-user action limiting

---

## Thread Safety

All rate limiter implementations are **thread-safe**:

```java
RateLimiter limiter = RateLimiterFactory.createTokenBucket(config);

// Safe to use from multiple threads
ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        if (limiter.tryAcquire()) {
            // Process request
        }
    });
}
```

**Thread Safety Strategies:**
- **Token Bucket, Leaky Bucket, Sliding Window Log**: `ReentrantLock`
- **Fixed Window, Sliding Window Counter**: `AtomicLong` + `ReentrantLock`
- **UserRateLimiterRegistry**: `ConcurrentHashMap`

---

## Advanced Usage

### Multiple Permits
```java
// Consume multiple permits at once
// Useful for operations with different costs
if (limiter.tryAcquire(5)) {
    // Upload 5MB file (costs 5 permits)
}
```

### Check Available Permits
```java
long available = limiter.getAvailablePermits();
System.out.println("Remaining quota: " + available);
```

### Reset Limiter
```java
// Reset to initial state (useful for testing)
limiter.reset();
```

### Custom Configuration Per User
```java
// VIP user gets higher rate limit
RateLimitConfig vipConfig = RateLimitConfig.builder()
    .maxRequests(1000)
    .windowSize(Duration.ofSeconds(1))
    .build();

RateLimiter vipLimiter = registry.getLimiter(
    "vip-user",
    AlgorithmType.TOKEN_BUCKET,
    vipConfig
);
```

---

## Real-World Examples

### REST API Rate Limiting
```java
@RestController
public class ApiController {
    
    private final UserRateLimiterRegistry registry;
    
    public ApiController() {
        RateLimitConfig config = RateLimitConfig.builder()
            .maxRequests(100)
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        this.registry = new UserRateLimiterRegistry(
            AlgorithmType.SLIDING_WINDOW_COUNTER,
            config
        );
    }
    
    @GetMapping("/api/data")
    public ResponseEntity<?> getData(@RequestHeader("X-API-Key") String apiKey) {
        RateLimiter limiter = registry.getLimiter(apiKey);
        
        if (!limiter.tryAcquire()) {
            return ResponseEntity.status(429)
                .header("X-RateLimit-Remaining", "0")
                .header("Retry-After", "60")
                .body("Rate limit exceeded");
        }
        
        return ResponseEntity.ok()
            .header("X-RateLimit-Remaining", 
                   String.valueOf(limiter.getAvailablePermits()))
            .body(fetchData());
    }
}
```

### Login Attempt Limiting
```java
public class LoginService {
    
    private final UserRateLimiterRegistry registry;
    
    public LoginService() {
        RateLimitConfig config = RateLimitConfig.builder()
            .maxRequests(5)
            .windowSize(Duration.ofMinutes(15))
            .build();
        
        this.registry = new UserRateLimiterRegistry(
            AlgorithmType.SLIDING_WINDOW_LOG,  // High accuracy for security
            config
        );
    }
    
    public boolean login(String username, String password) {
        RateLimiter limiter = registry.getLimiter(username);
        
        if (!limiter.tryAcquire()) {
            throw new TooManyLoginAttemptsException(
                "Too many login attempts. Try again in 15 minutes."
            );
        }
        
        return authenticateUser(username, password);
    }
}
```

### File Upload Rate Limiting
```java
public class FileUploadService {
    
    private final RateLimiter limiter;
    
    public FileUploadService() {
        RateLimitConfig config = RateLimitConfig.builder()
            .maxRequests(100)  // 100 MB per minute
            .windowSize(Duration.ofMinutes(1))
            .build();
        
        this.limiter = RateLimiterFactory.createTokenBucket(config);
    }
    
    public void uploadFile(byte[] fileData) {
        int fileSizeMB = fileData.length / (1024 * 1024);
        
        if (!limiter.tryAcquire(fileSizeMB)) {
            throw new RateLimitExceededException(
                "Upload quota exceeded. Try again later."
            );
        }
        
        saveFile(fileData);
    }
}
```

---

## Performance Characteristics

| Algorithm | Time | Space | Accuracy | Burst Handling |
|-----------|------|-------|----------|----------------|
| Token Bucket | O(1) | O(1) | High | ✅ Allows |
| Leaky Bucket | O(1) | O(n) | High | ❌ Smooths |
| Fixed Window | O(1) | O(1) | Low | ⚠️ Boundary issue |
| Sliding Window Log | O(log n) | O(n) | Perfect | ✅ Controlled |
| Sliding Window Counter | O(1) | O(1) | High | ✅ Controlled |

---

## Design Patterns Used

### Strategy Pattern
Different rate limiting algorithms are interchangeable via `RateLimiter` interface.

### Factory Pattern
`RateLimiterFactory` creates appropriate limiter based on algorithm type.

### Builder Pattern
`RateLimitConfig.Builder` for clean configuration with validation.

### Registry Pattern
`UserRateLimiterRegistry` manages per-user limiters with lazy initialization.

---

## Running the Demo

```bash
cd /Volumes/Crucial_X9/LLD_prep
mvn compile
mvn exec:java -Dexec.mainClass="com.lldprep.ratelimiter.RateLimiterDemo"
```

The demo covers:
1. All 5 algorithms in action
2. Algorithm comparison
3. Thread safety verification
4. Per-user rate limiting
5. Burst handling differences
6. Fixed Window boundary problem demonstration

---

## Key Takeaways

### When to Use Each Algorithm

**Production Recommendation:** Sliding Window Counter
- Best balance of accuracy and performance
- No boundary issues
- O(1) time and space
- Used by Cloudflare, AWS

**Allow Bursts:** Token Bucket
- Good for APIs that can handle occasional spikes
- Accumulates tokens during idle periods

**Strict Rate:** Leaky Bucket
- Enforces constant output rate
- Good for traffic shaping

**High Accuracy:** Sliding Window Log
- Perfect accuracy for security-critical scenarios
- Higher memory cost

**Simple/Low Memory:** Fixed Window
- Simplest implementation
- Acceptable for non-critical scenarios

### SOLID Principles Applied

- **SRP**: Each algorithm in separate class
- **OCP**: New algorithms extend without modifying existing code
- **LSP**: All algorithms substitutable via `RateLimiter` interface
- **ISP**: Minimal, focused interface
- **DIP**: Depend on `RateLimiter` abstraction, not concrete types

---

## Further Reading

- Token Bucket: https://en.wikipedia.org/wiki/Token_bucket
- Leaky Bucket: https://en.wikipedia.org/wiki/Leaky_bucket
- Cloudflare's approach: https://blog.cloudflare.com/counting-things-a-lot-of-different-things/
- Redis rate limiting: https://redis.io/commands/incr#pattern-rate-limiter
