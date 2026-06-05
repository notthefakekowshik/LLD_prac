# Order Book Engine — AI Agent Curveball

> **Premise:** AI trading agents now account for 60-80% of order flow in electronic markets.
> These agents detect latency patterns, exploit predictability in matching logic, and coordinate
> in ways human traders never could. The current engine was designed for human-speed trading.
> This curveball explores what breaks.

---

## Current Design (What Exists)

- **Matching:** `OrderBook` with `TreeMap<price, Deque<Order>>` for price-time priority
- **Execution:** `HybridStripedExecutor` — per-symbol `SingleThreadExecutor` for lock-free matching
- **Order types:** LIMIT, MARKET with partial fills and cancellation
- **Concurrency:** Thread confinement per symbol (no cross-symbol locking)
- **Trade notification:** `TradeListener` callback interface

---

## Why Agent Traffic Breaks This

| Assumption (Human Trading) | Reality (Agent Trading) |
|----------------------------|------------------------|
| Orders arrive at human reaction speed (200ms+) | Agents submit orders in microseconds |
| Order patterns are diverse and unpredictable | Agents follow deterministic strategies — same conditions, same order |
| Spoofing requires manual effort (one order at a time) | Agents can place and cancel 10,000 orders/second to create fake depth |
| Market makers are a handful of firms | Thousands of agent instances all trying to be market makers |
| Price discovery is driven by human judgment | Price discovery is driven by agent interaction — emergent behavior, sometimes pathological |
| Latency advantage is measured in milliseconds | Agent latency advantage is measured in microseconds — they detect engine processing time |

---

## Curveball Scenarios

### Scenario 1: Latency Arbitrage by Agents

**Problem:** Agents measure the time between their order submission and the trade notification.
Over thousands of orders, they build a statistical model of the engine's processing time per symbol.
They then use this model to predict short-term price movements — if symbol A's matching takes 2μs longer
than usual, it means there's a large order in the queue, which signals impending price movement.

**Current design gap:** `HybridStripedExecutor` processes symbols in isolation with consistent timing.
This predictability is an information leak. Agents exploit the **determinism** of the matching engine.

**Required Changes:**
- **Timing jitter:** Add controlled random delay (0-5μs) to trade notifications so agents can't build accurate timing models
- **Batch matching:** Instead of processing orders one-by-one in arrival order, batch orders in 100μs windows and match them as a group. This hides individual order processing time.
- **Notification randomization:** Trade callbacks fire at slightly randomized intervals, not immediately after match

**Trade-off:** Adding jitter and batching increases latency for legitimate human traders. The interview question: "How do you protect the market from agent exploitation without penalizing human participants?"

---

### Scenario 2: Order Book Spoofing at Scale

**Problem:** An agent places 5,000 large LIMIT BUY orders at prices just below the current market — creating
the illusion of massive demand. Other agents (and humans) see this depth and adjust their strategies.
Then the spoofer cancels all 5,000 orders in a single batch and sells into the artificially inflated price.

**Current design gap:** `OrderBook.cancel(orderId)` is O(1) per order — cancellation is cheap and fast.
Nothing prevents an agent from placing and cancelling thousands of orders per second. The engine treats
cancellations as first-class operations without questioning their intent.

**Required Changes:**
- **Order-to-trade ratio tracking:** Per-agent metric: orders_submitted / trades_executed. Healthy market makers: 5-10x. Spoofer: 1000x+. Flag agents exceeding threshold.
- **Cancellation cooldown:** After N cancellations in a time window, impose an escalating delay on future cancellations from that agent (10ms → 50ms → 200ms).
- **Depth validation:** Before broadcasting order book depth to other participants, apply a minimum resting time filter — orders that existed for < 100ms don't count toward published depth.

```java
// New component — not yet implemented
public class AgentBehaviorTracker {
    // Per-agent sliding window metrics
    private final Map<String, AgentMetrics> metrics = new ConcurrentHashMap<>();

    public void recordOrderPlacement(String agentId) {
        metrics.computeIfAbsent(agentId, AgentMetrics::new).incrementOrders();
    }

    public void recordCancellation(String agentId) {
        AgentMetrics m = metrics.get(agentId);
        if (m != null) {
            m.incrementCancellations();
            if (m.getCancelToTradeRatio() > SPOOFING_THRESHOLD) {
                activateCooldown(agentId);
            }
        }
    }

    public boolean isAgentRestricted(String agentId) {
        AgentMetrics m = metrics.get(agentId);
        return m != null && m.getCurrentCooldownUntil() > System.nanoTime();
    }
}
```

**Key Design Decision:** Where is the tracking done — inside the matching engine (adds latency to every order) or as a sidecar observer (eventually consistent, might miss fast spoof-and-cancel)?

---

### Scenario 3: Agent Herding and Flash Crashes

**Problem:** 500 agents from different firms all use similar ML models trained on the same market data.
When a specific pattern appears, ALL 500 agents simultaneously try to sell. The order book gets hammered
with MARKET SELL orders that exhaust all BUY-side depth in microseconds — a flash crash caused by
agent herding, not fundamental value change.

**Current design gap:** The engine processes MARKET orders greedily — match against whatever limit orders
exist. There's no circuit breaker, no speed bump, no "market is moving too fast" detection.

**Required Changes:**
- **Circuit breaker:** If price moves > 5% in 1 second for any symbol, halt trading for that symbol for 30 seconds. All pending MARKET orders get queued, not executed.
- **Speed bump:** MARKET orders have a mandatory 50ms delay before execution. This is enough to break synchronous herding loops (agent A sells → price drops → agent B sees drop and sells → cascading).
- **Volatility-aware matching:** When volatility exceeds threshold, widen the minimum price increment (tick size). This reduces the granularity of price discovery during panic, preventing the "race to zero" effect.

**Interview Question:** "A circuit breaker protects against flash crashes but also prevents legitimate price discovery. How do you set the threshold so you don't trigger it on normal volatility but catch genuine cascading failures?"

---

### Scenario 4: Agent-Driven Information Asymmetry

**Problem:** A sophisticated agent subscribes to trade notifications via `TradeListener` and reconstructs
the full order book state in real-time. It then uses this reconstructed state to predict which limit orders
are likely to be filled next, and places orders ahead of them (latency front-running). This doesn't require
insider information — just faster order book reconstruction than other participants.

**Current design gap:** `TradeListener` broadcasts ALL trade events to ALL listeners with no filtering.
Every agent gets the same data, but faster agents (or agents with better reconstruction algorithms) gain
an asymmetric advantage.

**Required Changes:**
- **Selective disclosure:** Don't broadcast individual trade events to all listeners. Instead, publish aggregated snapshots at fixed intervals (e.g., every 10ms). This prevents microstructure exploitation.
- **Blind order matching:** For MARKET orders, don't reveal the counterparty's identity or the exact queue position. Agents can't front-run what they can't see.
- **Fair access queue:** Agents that consume trade data must also submit their own orders through the same latency-bounded channel. No read-without-write asymmetry.

---

## What This Means for the Existing Codebase

| Class | Change Required |
|-------|----------------|
| `OrderBook` | Add minimum resting time filter for published depth; circuit breaker integration |
| `HybridStripedExecutor` | Add batch matching mode (100μs windows); timing jitter on trade notifications |
| `Order` | Add `agentId` field; add `placedAtNano` for resting time calculation |
| `TradeListener` | Change from real-time broadcast to aggregated snapshot publishing |
| New: `AgentBehaviorTracker` | Per-agent metrics: order-to-trade ratio, cancellation rate, herding detection |
| New: `CircuitBreaker` | Per-symbol volatility monitor with trading halt capability |
| New: `FairAccessManager` | Speed bump enforcement, read-write symmetry validation |

---

## Interview Talking Points

1. **"How would you redesign an order book engine for a market where 80% of participants are AI agents?"**
   → Batch matching, timing jitter, agent behavior tracking, circuit breakers. The engine must be adversarial by design, not just fast.

2. **"Your exchange experiences a flash crash — price drops 20% in 3 seconds, recovers in 10 seconds. All caused by agent herding. What mechanisms should have prevented this?"**
   → Circuit breaker (halt on > 5% move/sec), speed bump on MARKET orders (50ms delay breaks synchronous cascading), volatility-aware tick size widening.

3. **"An agent places and cancels 10,000 orders/second without executing a single trade. Is this a problem? What do you do?"**
   → Yes — it's spoofing. Track order-to-trade ratio per agent, impose cancellation cooldowns, filter sub-100ms orders from published depth.

4. **"How do you protect human traders in a market dominated by agents?"**
   → Priority-based order processing (human orders get executed first during high load), speed bumps that slow everyone equally, and transparent circuit breakers that prevent agent-driven volatility from impacting human portfolios.
