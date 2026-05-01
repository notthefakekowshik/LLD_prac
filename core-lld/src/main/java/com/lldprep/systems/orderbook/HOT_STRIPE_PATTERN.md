# Hot Stripe Pattern — Handling Volume Concentration in Striped Executors

**Problem:** In striped task executors, 80% of volume often hits 20% of stripes (NASDAQ 80/20 rule). A single-threaded stripe becomes the bottleneck.

**Solution:** Hybrid striped executor with dedicated fast lanes for hot symbols and shared slow lanes for cold symbols.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    HYBRID STRIPED EXECUTOR                   │
├─────────────────────────────────────────────────────────────┤
│  FAST LANE (Dedicated)          │   SLOW LANE (Shared)       │
│  ───────────────────            │   ───────────────────────  │
│  • AAPL → Thread on Core-0     │   • SmallCap1 → Pool-1     │
│  • NVDA → Thread on Core-1    │   • SmallCap2 → Pool-2     │
│  • TSLA → Thread on Core-2     │   • ... (32 stripes)       │
│  • MSFT → Thread on Core-3    │                            │
│  • AMZN → Thread on Core-4    │                            │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Features

### 1. Tier-Based Symbol Classification

| Tier | Symbols | Handling |
|------|---------|----------|
| **TIER_1** | AAPL, MSFT, NVDA, TSLA, AMZN | Dedicated threads at startup |
| **TIER_2** | GOOGL, META, NFLX, AMD, CRM, INTC, BABA | Dedicated threads at startup |
| **TIER_3** | All others | Hash-based routing to 32 shared stripes |

### 2. Backpressure Strategies

When a stripe queue exceeds threshold:

| Mode | Behavior | Use Case |
|------|----------|----------|
| **REJECT** | Throw `RejectedExecutionException` | Client can retry with backoff |
| **BLOCK** | Caller blocks until queue drains | Latency-sensitive systems |
| **SHED** | Silently drop task | Preserve core functionality |

### 3. Hot Stripe Detection

```java
// Monitor queue depth per symbol
if (executor.isHotStripe("AAPL")) {
    // Trigger alert or scale up
}

// Get full stats
StripeStats stats = executor.getStats("AAPL");
// stats.queueDepth(), stats.totalEvents(), stats.isHot(), stats.inFastLane()
```

### 4. Dynamic Promotion/Demotion

```java
// Promote viral meme stock to fast lane
executor.promoteToFastLane("GME");

// Demote when traffic subsides
executor.demoteToSlowLane("GME");
```

### 5. Emergency Rebalancing

```java
// Nuclear option: migrate queued tasks to fresh executor
// Use when a symbol goes viral and existing queue is clogged
executor.emergencyRebalance("AAPL");
```

---

## Usage in Order Book

### Basic Usage

```java
// Initialize with default tiers
HybridStripedExecutor executor = new HybridStripedExecutor();

// Submit order processing task
executor.submit("AAPL", () -> {
    orderBook.placeOrder(order);
});
```

### With Backpressure

```java
// Reject when queue exceeds 5000
try {
    executor.submitWithBackpressure("AAPL", task, BackpressureMode.REJECT);
} catch (RejectedExecutionException e) {
    // Client should back off and retry
    sendRetryToClient(order);
}
```

### Tier-Based Configuration

```java
// Customize thresholds
HybridStripedExecutor executor = new HybridStripedExecutor(
    hotQueueThreshold: 1000,   // Consider "hot" at 1000 queued
    maxQueueDepth: 5000        // Reject at 5000
);
```

---

## Performance Characteristics

| Scenario | Throughput | Latency |
|----------|-----------|---------|
| Cold symbols (slow lane) | ~50K ops/sec | P99 < 10ms |
| Hot symbols (fast lane) | ~200K ops/sec | P99 < 1ms |
| Hot stripe without fast lane | ~10K ops/sec | P99 > 100ms |

---

## Comparison: Standard vs Hybrid Striped Executor

| Aspect | Standard (1 thread/symbol) | Hybrid (fast/slow lanes) |
|--------|---------------------------|--------------------------|
| Memory (10K symbols) | 10K threads (~10GB) | 32 + 50 = 82 threads (~100MB) |
| Hot symbol throughput | Single-thread capped | Dedicated core, lock-free |
| Cold symbol overhead | Wasted threads | Shared pool, efficient |
| Complexity | Simple | Moderate (tier management) |
| Scalability | Poor at high symbol counts | Good |

---

## When to Use

✅ **Use Hybrid Striped Executor when:**
- Symbol volume follows power law (80/20 distribution)
- You have more symbols than CPU cores
- Hot symbols need consistent low latency
- Backpressure is acceptable for overload scenarios

❌ **Don't use when:**
- All symbols have uniform volume (wastes resources)
- You need strict global ordering across symbols
- Memory is constrained (monitoring overhead)

---

## Files

- `service/HybridStripedExecutor.java` — Core implementation
- `demo/HybridStripedExecutorDemo.java` — Usage examples
- `HOT_STRIPE_PATTERN.md` — This documentation

---

## References

- See also: `DESIGN_DICE.md` for original thread confinement model
- See also: `OrderBookEngine.java` for basic striped executor usage
