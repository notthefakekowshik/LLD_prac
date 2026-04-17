# Order Book Engine

## Overview

A thread-safe, high-performance in-memory order book matching engine. Buyers and sellers place limit or market orders; the engine matches them by **price-time priority** and notifies listeners of trades, resting orders, and cancellations.

**Key design insight**: Each symbol gets its own `SingleThreadExecutor`, so the matching engine requires **zero locks** — thread confinement makes it inherently safe while allowing different symbols to match in parallel.

---

## Quick Start

```java
// 1. Implement TradeListener to receive events
TradeListener listener = new TradeListener() {
    public void onTrade(MatchResult r)      { System.out.println("TRADE: " + r); }
    public void onOrderAccepted(Order o)    { System.out.println("RESTING: " + o); }
    public void onOrderCancelled(Order o)   { System.out.println("CANCELLED: " + o); }
};

// 2. Create the engine
OrderBookEngine engine = new OrderBookEngine(listener);

// 3. Place orders (safe from any thread)
engine.placeOrder(new Order("AAPL", Order.Side.SELL, Order.Type.LIMIT, 150.00, 10));
engine.placeOrder(new Order("AAPL", Order.Side.BUY,  Order.Type.LIMIT, 150.00, 10));
// → onTrade fires: TRADE symbol=AAPL price=150.00 qty=10

// 4. Cancel a resting order
Future<Void> f = engine.cancelOrder("AAPL", orderId);
f.get(); // block until processed

// 5. Shutdown gracefully
engine.shutdown();
```

---

## Order Types

### LIMIT Order
```java
new Order("AAPL", Order.Side.BUY, Order.Type.LIMIT, 150.00, 10)
```
- Matches only if a resting order exists at a compatible price
- Unfilled portion rests in the book

### MARKET Order
```java
new Order("AAPL", Order.Side.BUY, Order.Type.MARKET, 0, 10)
```
- Fills immediately at best available price
- No price constraint — will sweep multiple levels
- Unfilled quantity (if book is empty) is **discarded**

---

## Matching Rules

1. **Price priority**: Highest bid matches first; lowest ask matches first
2. **Time priority**: Within the same price level, earliest order matches first (FIFO)
3. **Matched price**: Always the resting (passive) order's price
4. **Partial fills**: A single incoming order can fill against multiple resting orders

```
Resting book:
  ASKS: 100 x5, 101 x5, 102 x5

Incoming: BUY LIMIT 102 qty=12
→ TRADE price=100 qty=5   (sweeps level 100)
→ TRADE price=101 qty=5   (sweeps level 101)
→ TRADE price=102 qty=2   (partial fill at 102)
→ RESTING: remaining 3 qty rests at 102
```

---

## Concurrency Model

```
Any Thread  ─────► engine.placeOrder("AAPL", ...)
                         │
                         ▼
              executors["AAPL"]          executors["AMZN"]
              SingleThreadExecutor       SingleThreadExecutor
                         │                       │
                         ▼                       ▼
                 OrderBook(AAPL)         OrderBook(AMZN)
               [no locks needed]        [no locks needed]
               [runs in parallel]       [runs in parallel]
```

- `placeOrder()` and `cancelOrder()` are safe from **any number of concurrent threads**
- **Within a symbol**: all operations are serialized on one dedicated thread
- **Across symbols**: AAPL and AMZN process simultaneously with zero contention

### Why not `PriorityBlockingQueue`?
A `PriorityBlockingQueue` gives you a thread-safe queue but **not atomic match transactions** — you'd need external locks anyway. A single-thread executor makes the `OrderBook` itself lock-free; the serialization happens at the queue boundary inside the executor.

---

## Package Structure

```
com.lldprep.orderbook/
    model/
        Order.java              ← Value object (symbol, side, type, price, qty, status)
        MatchResult.java        ← Immutable fill event
    service/
        TradeListener.java      ← Event callback interface
        OrderBook.java          ← Per-symbol price-time priority matching
        OrderBookEngine.java    ← Per-symbol executor dispatch
    exception/
        OrderNotFoundException.java
    demo/
        OrderBookDemo.java      ← 5 scenarios: basic, partial fill, cancel, market, concurrent
```

---

## Design Patterns

| Pattern | Where | Why |
|---|---|---|
| **Thread Confinement** | `OrderBookEngine` | Eliminates locking inside `OrderBook` |
| **Strategy** | `TradeListener` | Swap logging / DB persistence / alerting without touching matching logic |
| **Producer-Consumer** | Caller threads → Executor | Decouples order submission from processing |
| **Facade** | `OrderBookEngine` | Simple `placeOrder` / `cancelOrder` API hides executor management |

---

## Data Structure — Why `TreeMap<price, Deque<Order>>`

| Need | Structure | Reason |
|---|---|---|
| Best price O(log n) | `TreeMap` | `firstEntry()` on sorted map |
| Time priority within level | `Deque` | FIFO — `addLast` / `pollFirst` |
| Cancel O(1) lookup | `HashMap<orderId, Order>` | Direct reference, no scan |
| Iterate all levels | `TreeMap` | In-order traversal |

A flat `PriorityQueue<Order>` cannot maintain time priority within the same price level.

---

## Running the Demo

```
com.lldprep.orderbook.demo.OrderBookDemo
```

Covers:
1. Basic match (sell rests → buy triggers trade)
2. Partial fill sweeping 3 price levels
3. Cancel before a buyer arrives → no trade
4. Market order consuming 2 levels
5. 4 concurrent threads on AAPL + AMZN with no shared locks
