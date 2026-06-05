# Rate Limiter — AI Agent Curveball

> **Premise:** Your rate limiter now serves APIs where 60-80% of traffic comes from AI agents, not humans.
> Agents don't browse — they make programmatic, high-throughput, deterministic requests. Every assumption
> in the current design was built for human-speed traffic. This curveball breaks those assumptions.

---

## Current Design (What Exists)

- Algorithms: Fixed Window, Sliding Window Counter, Sliding Window Log, Token Bucket, Leaky Bucket
- Per-user isolation via `UserRateLimiterRegistry`
- Striped executor variant for lock-free concurrent limiting
- Config: `RateLimitConfig` with max requests + window duration per user

---

## Why Agent Traffic Breaks This

| Assumption (Human Traffic) | Reality (Agent Traffic) |
|----------------------------|------------------------|
| 10-50 requests/min per user | 1,000-100,000 requests/min per agent |
| Diurnal pattern (peak during day, low at night) | Flat or burst patterns — agents run 24/7 |
| Diverse access patterns across users | Synchronized access — 1000 agents hitting the same endpoint in the same second |
| Cost per request is uniform | A complex search query costs 100x more server resources than a health check |
| One user = one human = one device | One API key = one agent = thousands of concurrent workflows |
| Rate limit violations are rare mistakes | Agents hit rate limits deliberately to test boundaries or as a side effect of optimization |

---

## Curveball Scenarios

### Scenario 1: Agent Token Budget (Cost-Weighted Limiting)

**Problem:** Current design counts requests equally. But a vector similarity search consuming 500ms of GPU time
shouldn't cost the same as a `GET /health` check. Agents exploit this by making expensive requests that individually
pass the rate limit but collectively exhaust server capacity.

**Required Changes:**
- Each request type has a **cost weight** (health check = 1 token, search = 10 tokens, batch operation = 50 tokens)
- `RateLimitConfig` extended with `maxTokens` (budget) instead of `maxRequests`
- Token bucket algorithm already supports variable refill — extend it to support variable **consumption**
- New interface: `CostAwareRateLimiter` that wraps existing algorithms

```java
// Extension point — not yet implemented
public interface CostAwareRateLimiter {
    boolean tryAcquire(String userId, int cost);
    long getRemainingBudget(String userId);
    void refillBudget(String userId, int tokens);
}
```

**Key Design Decision:** Where does the cost mapping live? Options:
1. Caller specifies cost in header (`X-Request-Cost: 10`) — untrusted, agents will lie
2. Server determines cost after request processing — too late to reject
3. Endpoint-level cost table configured server-side — **correct answer**, but needs to be kept in sync with API evolution

---

### Scenario 2: Agent Identity Classification

**Problem:** Current design uses per-user rate limiting. But "user" is ambiguous in the agent world.
An agent acts on behalf of a user but makes requests at machine speed. Should the agent's requests
count against the user's quota? Against its own quota? Both?

**Required Changes:**
- New entity: `AgentIdentity` — separate from `UserId`. An agent has:
  - `agentId` (unique to the agent instance)
  - `ownerUserId` (the human who created/authorized it)
  - `trustLevel` (PROVISIONAL, VERIFIED, PREMIUM)
  - `rateLimitTier` (mapped from trust level)
- Request classification at the gateway: is this request from a human or an agent?
- Dual rate limiting: agent has its own limit AND contributes to owner's aggregate quota

**Interview Question:** If an agent with a PREMIUM tier exhausts its own limit but the owner has remaining quota, should the agent be allowed to continue using the owner's budget? What are the abuse implications?

---

### Scenario 3: Coordinated Agent Swarm (Cache Stampede by Agents)

**Problem:** 10,000 agents built by the same company all run the same logic: "at 9:00 AM UTC, fetch fresh data for all my clients." They all hit the same endpoint at the same millisecond. This is a coordinated cache stampede that no per-user rate limiter can prevent — each individual agent is well within its limit.

**Current design gap:** `PerUserTokenBucket` only checks individual user limits. There is no global backpressure mechanism for aggregate traffic patterns.

**Required Changes:**
- Global traffic anomaly detection: if request rate for a single endpoint exceeds 3x its 7-day average, activate **crowd control**
- Crowd control modes:
  1. **Graceful degradation:** return stale data with `X-Stale: true` header
  2. **Jitter injection:** respond with `Retry-After: <random 0-5s>` to desynchronize agent retries
  3. **Queue mode:** accept the request but return 202 Accepted + polling URL (async processing)

**Key Insight:** Per-user rate limiting protects against individual abuse. Agent swarm protection requires **endpoint-level** and **fleet-level** backpressure.

---

### Scenario 4: Fair Queuing (Human Priority Over Agents)

**Problem:** When the system is overloaded, human users should get priority. Agents should be throttled first.
Current design has no concept of traffic source priority — a rate-limited agent and a rate-limited human get the same 429 response.

**Required Changes:**
- Request classification: `TrafficSource.HUMAN` vs `TrafficSource.AGENT` (determined via User-Agent, API key type, or behavioral fingerprinting)
- Two separate token pools: human pool (70% capacity) and agent pool (30% capacity)
- When system load > 80%: agent pool shrinks to 10%, human pool stays at 90%
- Adaptive partitioning: if agent pool is empty but human pool has capacity, agents CAN use overflow (with higher cost weight). Not vice versa.

```java
// Pseudocode for adaptive partitioning
public boolean tryAcquire(Request request) {
    int load = systemMetrics.getCurrentLoad();
    if (request.getSource() == HUMAN) {
        return humanPool.tryAcquire(request.getCost());
    } else {
        int agentCapacity = load > 80 ? 10 : 30; // percent of total capacity
        if (agentPool.getUsage() < agentCapacity) {
            return agentPool.tryAcquire(request.getCost());
        }
        // Agents can use human overflow only if humans aren't using it
        if (humanPool.getUsage() < 70 && load < 90) {
            return agentPool.tryAcquire(request.getCost() * 2); // double cost for overflow
        }
        return false;
    }
}
```

---

## What This Means for the Existing Codebase

| Class | Change Required |
|-------|----------------|
| `RateLimitConfig` | Add `costWeights` map, `budgetPerPeriod`, `trustLevel` |
| `PerUserTokenBucket` | Support variable-cost deduction, not just count |
| `UserRateLimiterRegistry` | Support agent identity alongside user identity |
| `StripedExecutorRateLimiter` | Add priority lanes (human vs agent) |
| New: `TrafficClassifier` | Classify requests as HUMAN vs AGENT at the gateway |
| New: `CrowdControlManager` | Endpoint-level backpressure for agent swarms |
| New: `AdaptivePartitioner` | Dynamic capacity split between human/agent pools |

---

## Interview Talking Points

1. **"How would you design a rate limiter for a world where AI agents make 80% of API calls?"**
   → Cost-weighted token budgets, not request counts. Agents consume compute, not just "requests."

2. **"How do you prevent one agent from monopolizing shared resources?"**
   → Agent identity + trust tiers + adaptive capacity partitioning between human and agent traffic.

3. **"10,000 agents from the same company hit your API at the same time. Each is within its rate limit. Your servers melt. What went wrong?"**
   → Per-user rate limiting doesn't protect against coordinated fleet behavior. You need endpoint-level and fleet-level backpressure (crowd control).

4. **"Should agents and humans share the same rate limit pool?"**
   → No. Priority-based partitioning. Humans get guaranteed capacity. Agents get best-effort with overflow rules.
