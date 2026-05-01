# Order Book Engine — Design Document (D.I.C.E. Format)

Per-symbol order book engine with price-time priority matching and lock-free concurrency via thread confinement.

Follows the D.I.C.E. workflow from `INSTRUCTIONS.md`.

---

## Step 1 — DEFINE (Requirements & Constraints)

### Functional Requirements

1. A participant can **place a LIMIT order** (BUY or SELL at a specific price).
2. A participant can **place a MARKET order** (BUY or SELL at best available price).
3. Orders are **matched at price-time priority** — highest bid / lowest ask wins; FIFO within a price level.
4. Orders are **partially fillable** — a large order may fill across multiple resting orders.
5. A participant can **cancel an open order** by ID; filled or already-cancelled orders are silently ignored.
6. **Multiple symbols** (AAPL, AMZN, TSLA) can be matched **concurrently**.
7. A `TradeListener` is **notified of all events**: trade executed, order accepted, order cancelled.

### Non-Functional Requirements

- **O(log n) price-level access** via `TreeMap` — best bid/ask in O(1) via `firstEntry()`.
- **O(1) order removal** within a price level via `Deque.remove(order)` — O(n) worst case within a level but O(1) amortized for FIFO fills.
- **Lock-free inside each `OrderBook`** — thread confinement via per-symbol `SingleThreadExecutor` eliminates all `synchronized`/`ReentrantLock` inside matching logic.
- **Parallel matching** across symbols — each symbol's executor runs independently.
- **`Future<Void>`** returned from all mutating operations — callers can await completion or fire-and-forget.

### Constraints

- In-memory only — no persistence.
- Single JVM process.
- `OrderBook` is NOT thread-safe on its own — correctness guaranteed only via thread confinement from `OrderBookEngine`.
- No order amendment — only cancel + re-place.

### Out of Scope

- Persistent order log / audit trail.
- FIX protocol integration.
- Order expiry (GTC, IOC, FOK order types).
- Market depth reporting.

---

## Step 2 — IDENTIFY (Entities & Relationships)

### Noun → Verb extraction

> A **participant** *places* an **order** → **OrderBookEngine** *routes* it to the correct **OrderBook** via a **per-symbol executor** → **OrderBook** *matches* against resting **orders** at best price → **fills** both sides → notifies **TradeListener** → unfilled remainder *rests* in the book.

### Nouns → Candidate Entities

| Noun | Entity Type | Notes |
|---|---|---|
| Order | Class (model) | `symbol / side / type / price / quantity / status / sequence`; `fill(qty)` and `cancel()` called only from `OrderBook` thread |
| Order.Side | Enum (inner) | `BUY / SELL` |
| Order.Type | Enum (inner) | `LIMIT / MARKET` |
| Order.Status | Enum (inner) | `OPEN / PARTIALLY_FILLED / FILLED / CANCELLED` |
| MatchResult | Class (model) | Immutable: `symbol / buyOrderId / sellOrderId / price / quantity` — event data |
| OrderBook | Class | Per-symbol book: `TreeMap<price, Deque<Order>>` for bids (desc) and asks (asc); all mutations on one thread |
| OrderBookEngine | Class | Central router: `ConcurrentHashMap<symbol, SingleThreadExecutor>` + `ConcurrentHashMap<symbol, OrderBook>` |
| TradeListener | Interface | Strategy / Observer: `onTrade / onOrderAccepted / onOrderCancelled` |
| OrderNotFoundException | Exception | Unchecked; for invalid cancel operations |

### Verbs → Methods / Relationships

| Verb | Lives on |
|---|---|
| `placeOrder(order)` | `OrderBookEngine` → routed to `OrderBook.placeOrder()` |
| `cancelOrder(symbol, orderId)` | `OrderBookEngine` → routed to `OrderBook.cancelOrder()` |
| `matchLimit(order)` / `matchMarket(order)` | `OrderBook` (private) |
| `matchBuyAgainstAsks(order)` / `matchSellAgainstBids(order)` | `OrderBook` (private) |
| `fill(qty)` / `cancel()` | `Order` |
| `onTrade / onOrderAccepted / onOrderCancelled` | `TradeListener` |
| `shutdown()` | `OrderBookEngine` |

### Relationships

```
OrderBookEngine   ──routes to──►   OrderBook (per symbol)    (Composition via computeIfAbsent)
OrderBookEngine   ──owns──►        ConcurrentHashMap<symbol, ExecutorService>  (Composition)
OrderBookEngine   ──owns──►        ConcurrentHashMap<symbol, OrderBook>        (Composition)
OrderBook         ──notifies──►    TradeListener              (Association — Observer/Strategy)
OrderBook         ──owns──►        TreeMap<price, Deque<Order>> bids            (Composition)
OrderBook         ──owns──►        TreeMap<price, Deque<Order>> asks            (Composition)
OrderBook         ──owns──►        Map<orderId, Order> allOrders               (Composition)
OrderBook         ──creates──►     MatchResult                (Dependency)
TradeListener     ──receives──►    MatchResult, Order         (Dependency)
Order             ──has──►         Order.Side, Order.Type, Order.Status         (Composition)
```

### Design Patterns Applied

| Pattern | Where | Why |
|---|---|---|
| **Thread Confinement** | Per-symbol `SingleThreadExecutor` in `OrderBookEngine` | All mutations to an `OrderBook` happen on one dedicated thread — zero locking needed inside the book. Different symbols match in parallel across separate threads. |
| **Strategy / Observer** | `TradeListener` interface | Decouples event handling from matching logic. Inject a console printer, database writer, or WebSocket publisher without touching `OrderBook`. |
| **Producer-Consumer** | Callers submit to executor queue; executor thread consumes | `CompletableFuture.runAsync(task, executor)` — caller threads never enter the matching engine; executor serializes all operations per symbol. |
| **Facade** | `OrderBookEngine` | Callers see `placeOrder(order)` / `cancelOrder(symbol, id)` — all executor routing, book creation, and symbol registration is hidden. |

---

## Step 3 — CLASS DIAGRAM (Mermaid.js)

```mermaid
classDiagram
    class OrderBookEngine {
        -TradeListener listener
        -ConcurrentHashMap~String,ExecutorService~ executors
        -ConcurrentHashMap~String,OrderBook~ books
        +placeOrder(Order) Future~Void~
        +cancelOrder(String symbol, String orderId) Future~Void~
        +shutdown()
        -executorFor(symbol) ExecutorService
        -bookFor(symbol) OrderBook
    }

    class OrderBook {
        -String symbol
        -TradeListener listener
        -NavigableMap~Double,Deque~Order~~ bids
        -NavigableMap~Double,Deque~Order~~ asks
        -Map~String,Order~ allOrders
        +placeOrder(Order)
        +cancelOrder(String orderId)
        -matchLimit(Order)
        -matchMarket(Order)
        -matchBuyAgainstAsks(Order)
        -matchSellAgainstBids(Order)
        -addToBook(side, price, order)
        -removeFromBook(order)
    }

    class Order {
        -String id
        -String symbol
        -Side side
        -Type type
        -double price
        -int remainingQty
        -Status status
        -Instant placedAt
        -long sequence
        +fill(int qty)
        +cancel()
    }

    class MatchResult {
        -String symbol
        -String buyOrderId
        -String sellOrderId
        -double price
        -int quantity
    }

    class TradeListener {
        <<interface>>
        +onTrade(MatchResult result)
        +onOrderAccepted(Order order)
        +onOrderCancelled(Order order)
    }

    class OrderNotFoundException {
        +OrderNotFoundException(orderId)
    }

    OrderBookEngine --> OrderBook : creates and routes to
    OrderBookEngine --> TradeListener : injects into OrderBook
    OrderBook --> TradeListener : notifies
    OrderBook --> Order : matches and fills
    OrderBook --> MatchResult : creates
    OrderBook ..> OrderNotFoundException : throws
    Order +-- "Side" : inner enum
    Order +-- "Type" : inner enum
    Order +-- "Status" : inner enum
```

---

## Step 4 — PACKAGE STRUCTURE

```
com.lldprep.orderbook/
│
├── DESIGN_DICE.md                      ← this file
├── DESIGN.md                           ← original design (retained)
├── README.md
│
├── service/
│   ├── OrderBookEngine.java            ← Facade + routing: symbol → SingleThreadExecutor → OrderBook
│   ├── OrderBook.java                  ← matching engine: TreeMap<price, Deque<Order>>; single-threaded
│   └── TradeListener.java              ← Strategy/Observer interface: onTrade / onOrderAccepted / onOrderCancelled
│
├── model/
│   ├── Order.java                      ← entity: side / type / status / fill() / cancel()
│   └── MatchResult.java                ← immutable event: symbol / buyId / sellId / price / qty
│
├── exception/
│   └── OrderNotFoundException.java     ← unchecked; thrown on invalid cancel
│
└── demo/
    └── OrderBookDemo.java              ← LIMIT / MARKET / partial fills / cancel / concurrent symbols
```

---

## Step 5 — IMPLEMENTATION ORDER (per INSTRUCTIONS.md)

1. `exception/OrderNotFoundException.java`
2. `model/MatchResult.java` — immutable event
3. `model/Order.java` — entity with inner enums
4. `service/TradeListener.java` — interface
5. `service/OrderBook.java` — matching engine
6. `service/OrderBookEngine.java` — router/facade
7. `demo/OrderBookDemo.java` — last

---

## Step 6 — EVOLVE (Curveballs)

| Curveball | Impact | Extension strategy |
|---|---|---|
| **IOC orders** (Immediate-Or-Cancel) | Match what you can, cancel the rest | Add `Order.TimeInForce.IOC`. After matching in `matchLimit()`, if `remainingQty > 0`, cancel instead of adding to book. One-line change in `matchLimit()`. |
| **Stop-loss orders** | Triggered when price crosses threshold | `StopOrderBook` monitors last trade price. When triggered, converts stop to MARKET and calls `placeOrder()`. Zero changes to `OrderBook`. |
| **Persistent audit log** | All events must survive restart | `AuditingTradeListener implements TradeListener` — writes events to a log file/DB. Inject alongside console listener. Decorator or composite listener. |
| **Market depth report** | Aggregated bid/ask levels | `OrderBook.getDepth(int levels)` — iterate `bids.entrySet()` and `asks.entrySet()` for top N levels. Read-only; no locking needed (called from same executor thread). |
| **Multiple listeners** | More than one listener needs events | `CompositeTradeListener implements TradeListener` — holds `List<TradeListener>`, delegates all calls. Zero changes to `OrderBook`. |
| **Hot Stripe (80/20 volume)** | AAPL/NVDA/TSLA generate 80% of traffic, overwhelm single thread | `HybridStripedExecutor` — dedicated fast lanes for Tier 1/2 symbols, shared slow lanes for others. See `HOT_STRIPE_PATTERN.md`. |
| **Backpressure on overload** | Queue depth exceeds memory limits | Implement `submitWithBackpressure()` — REJECT, BLOCK, or SHED modes. See `HybridStripedExecutor.BackpressureMode`. |
| **Viral symbol (meme stock)** | Suddenly hot symbol needs dedicated resources | `executor.promoteToFastLane(symbol)` — dynamically migrate from slow lane to dedicated thread. |

---

## Why `TreeMap<price, Deque<Order>>` and Not `PriorityQueue<Order>`?

`PriorityQueue` cannot efficiently:
- Access the best price in O(1) — needs `peek()` which is O(1), but removing a cancelled order anywhere is O(n).
- Maintain FIFO within a price level — heap ordering ignores insertion time at equal prices.

`TreeMap<Double, Deque<Order>>` provides:
- **O(log n)** price-level insert/remove.
- **O(1)** best bid/ask via `firstEntry()`.
- **FIFO within a level** via `Deque.addLast()` / `pollFirst()`.
- **O(1) cancellation** at known price via `level.remove(order)` (Deque is doubly-linked).

---

## Thread Confinement Model

```
Caller Thread A ──► placeOrder(AAPL) ──► CompletableFuture.runAsync(─┐
Caller Thread B ──► placeOrder(AAPL) ──► CompletableFuture.runAsync(─┤──► executor-AAPL (single thread) ──► OrderBook(AAPL)
Caller Thread C ──► cancelOrder(AAPL) ──► CompletableFuture.runAsync(┘

Caller Thread D ──► placeOrder(AMZN) ──► CompletableFuture.runAsync ──► executor-AMZN (single thread) ──► OrderBook(AMZN)
```

`OrderBook(AAPL)` and `OrderBook(AMZN)` run in parallel.
`OrderBook(AAPL)` itself is fully serial — no `synchronized`, no `ReentrantLock`, no `AtomicReference` needed inside it.
`ConcurrentHashMap` is the **only** shared data structure — used solely for `computeIfAbsent` during symbol registration.

---

## Self-Review Checklist

- [x] Requirements written before any class design
- [x] Class diagram with typed relationships
- [x] Every class has a single nameable responsibility
- [x] `OrderBook` is zero-lock because of thread confinement (not `synchronized`)
- [x] Adding a new order type (IOC, FOK) requires changes only in `OrderBook.matchLimit()` (OCP near-satisfied)
- [x] Adding a new event handler requires zero changes to `OrderBook` (OCP via `TradeListener`)
- [x] `OrderBookEngine` depends on `TradeListener` interface, not concrete handler (DIP)
- [x] `TreeMap` vs `PriorityQueue` decision documented
- [x] Thread confinement model explained
- [x] Patterns documented with "why"
- [x] Custom exception in `exception/`
- [x] Demo covers LIMIT / MARKET / partial fills / cancel / concurrent symbols
