# LLD Problems — ROI Ranking

> **ROI score (1–10):** interview frequency × pattern density × concurrency complexity.
> Scored for SDE-2/SDE-3 rounds at JPMC, Goldman, FAANG, and top Indian product companies.
>
> **Status key:** ✅ Done in this repo · 🔴 High priority · 🟡 Medium · 🟢 Low / niche

---

## Quick-Reference Table

| Rank | Problem | ROI | Tier | Concurrency | Key Patterns | Companies |
|------|---------|-----|------|-------------|-------------|-----------|
| 1 | Parking Lot | 10 | 1 | Medium | Strategy, Factory, Singleton, State, Observer | Amazon, Google, MS, Uber, JPMC |
| 2 | In-Memory Cache (LRU/LFU) | 10 | 1 | High | Strategy, Decorator, Factory, Singleton | All FAANG, all fintech |
| 3 | Rate Limiter | 9 | 1 | High | Strategy, Factory, Registry | Google, Stripe, JPMC, Goldman |
| 4 | ATM Machine | 9 | 1 | Medium | State, CoR, Strategy, Command | JPMC, Goldman, Barclays, MS Bank |
| 5 | Elevator System | 9 | 1 | High | State, Strategy, Observer | Amazon, Google, MS, Oracle |
| 6 | Movie Booking (BookMyShow) | 9 | 1 | Very High | Facade, Strategy, Observer, Repository | Flipkart, Amazon, Paytm, Ola |
| 7 | Custom Thread Pool | 9 | 1 | Very High | Builder, Strategy, Factory, State, Producer-Consumer | JPMC, Goldman, HFT firms |
| 8 | Logging Framework | 9 | 1 | Medium | CoR, Decorator, Strategy, Template Method, Singleton | JPMC, enterprise Java |
| 9 | Splitwise | 9 | 1 | Low | Strategy, Observer | Uber, Amazon, Flipkart, Swiggy |
| 10 | Vending Machine | 9 | 1 | Low | State, Strategy, Factory | Everywhere — classic State demo |
| 11 | Chess | 8 | 2 | Low | Strategy, State, Command, Composite | Google, Uber, Flipkart |
| 12 | Task Scheduler / Cron | 8 | 2 | High | Builder, Adapter, Strategy, Producer-Consumer | JPMC, Amazon, enterprise |
| 13 | Order Book Engine | 8 | 2 | Very High | Thread Confinement, Strategy, Producer-Consumer, Facade | JPMC, Goldman, HFT |
| 14 | Notification System | 7 | 2 | Medium | Observer, Strategy, Decorator, Factory | Common add-on to any system |
| 15 | Payment Gateway | 7 | 2 | Medium | Strategy, CoR, Facade, Decorator | Paytm, Razorpay, fintech |
| 16 | Circuit Breaker | 7 | 2 | High | State, Proxy, Strategy | Spring Cloud shops, microservices |
| 17 | Pub-Sub / Event Bus | 7 | 2 | High | Observer, Strategy, Decorator, Factory | Kafka-adjacent, event-driven shops |
| 18 | Library Management | 7 | 2 | Low | Strategy, Observer, Template Method | Common warm-up LLD |
| 19 | Hotel Management | 7 | 2 | Medium | State, Observer, Factory, Strategy | Amazon, Booking.com, OYO |
| 20 | Snake and Ladder | 7 | 2 | Low | State, Strategy, Observer | Flipkart, Amazon — quick extensibility demo |
| 21 | Ride-Sharing (Uber/Ola) | 7 | 2 | High | Strategy, Observer, State, Factory | Uber, Ola, Grab — domain-specific |
| 22 | Bloom Filter | 7 | 2 | Low | Strategy, Builder, Decorator | JPMC, infrastructure/DB companies |
| 23 | Symbol Search Engine | 7 | 2 | Low | Strategy, Decorator, Builder, Template Method | JetBrains, Google, IDE teams |
| 24 | Amazon Locker System | 6 | 3 | Low | Strategy, State, Factory | Amazon — domain-specific |
| 25 | Shopping Cart + Order | 6 | 3 | Low | Observer, Strategy, Composite, State, Command | E-commerce companies |
| 26 | Social Media Feed | 6 | 3 | Medium | Observer, Strategy, Decorator, Composite | Facebook, Twitter, LinkedIn |
| 27 | Coupon / Discount Engine | 6 | 3 | Low | Strategy, CoR, Decorator, Composite | E-commerce, fintech |
| 28 | Document Editor | 6 | 3 | Low | Command, Memento, Observer, Composite | Google, MS Office teams |
| 29 | Food Delivery (Zomato) | 6 | 3 | Medium | State, Observer, Strategy | Zomato, Swiggy, DoorDash |
| 30 | Traffic Light System | 6 | 3 | Low | State, Observer, Strategy | Common State pattern warm-up |
| 31 | Car Rental System | 6 | 3 | Low | State, Factory, Strategy | OYO, rental domain companies |
| 32 | Restaurant POS | 6 | 3 | Low | State, Observer, Strategy, Command | Hospitality domain |
| 33 | Tic Tac Toe | 6 | 3 | Low | State, Strategy, Observer | Warm-up — rarely the main problem |
| 34 | Hospital Management | 5 | 3 | Low | State, Factory, Observer, Strategy | Healthcare domain companies |
| 35 | Inventory Management | 5 | 3 | Low | Observer, Strategy, Template Method | Supply chain, e-commerce backend |
| 36 | Call Center System | 4 | 4 | Low | CoR, Strategy, Observer | BPO-adjacent, rarely asked |
| 37 | Flight Booking | 4 | 4 | Medium | Facade, State, Strategy, Observer | Travel domain only |
| 38 | Bowling Alley | 4 | 4 | Low | State, Strategy, Observer | Novelty — shows OOP breadth |
| 39 | Coffee Machine | 4 | 4 | Low | State, Strategy, Template Method | Embedded/IoT domain |
| 40 | Online Auction | 4 | 4 | Medium | Observer, Strategy, State, Command | Niche — eBay-like companies |

---

## Tier 1 — Interview Staples (ROI 9–10)

Not knowing any of these in a Java interview is a red flag. Target 90–120 min to design + code + evolve each.

---

### 1. Parking Lot ✅
**ROI: 10/10** · Concurrency: Medium

The most-asked LLD problem. Every interviewer uses it as a baseline. Rich enough to fill 90 min, familiar enough that you won't waste time understanding the domain.

| Pattern | Why it appears |
|---------|---------------|
| Strategy | `SpotAllocationPolicy` — nearest spot vs random vs handicap-priority |
| Factory | `VehicleFactory`, `SpotFactory` — type-driven creation |
| Singleton | `ParkingLotRegistry` — one instance per lot |
| State | `ParkingSpot` — AVAILABLE / OCCUPIED / RESERVED |
| Observer | `DisplayBoard` updates when spot availability changes |

**Concurrency curveball:** Two threads trying to book the last spot simultaneously. Requires `synchronized` on allocation + `ConcurrentHashMap` for spot registry.

**DSA overlap:** Spot allocation can use a `PriorityQueue` ordered by distance from entrance.

---

### 2. In-Memory Cache (LRU / LFU) ✅
**ROI: 10/10** · Concurrency: High

Asked everywhere because it sits at the intersection of DSA (O(1) data structures) and design (pluggable eviction). Interviewers immediately probe thread-safety.

| Pattern | Why it appears |
|---------|---------------|
| Strategy | `EvictionPolicy` interface — LRU, LFU, FIFO swappable |
| Decorator | `LoggingCache`, `MetricsCache` wrap the core without touching it |
| Factory | `CacheFactory.create(type, capacity)` |
| Singleton | `CacheManager` registry |

**Concurrency curveball:** Thread-safe reads with `ReentrantReadWriteLock` — reads don't block each other.

**DSA overlap:** LRU requires DLL + HashMap for O(1). LFU requires `TreeMap<frequency, LinkedHashSet<key>>`.

---

### 3. Rate Limiter ✅
**ROI: 9/10** · Concurrency: High

Canonical SDE-2/3 problem. Interviewer always asks you to implement Token Bucket first, then asks: "How would you change the algorithm?" — that is the OCP/Strategy test.

| Pattern | Why it appears |
|---------|---------------|
| Strategy | 5 algorithms interchangeable: Token Bucket, Leaky Bucket, Fixed Window, Sliding Window Log, Sliding Window Counter |
| Factory | `RateLimiterFactory.create(algorithm, config)` |
| Registry | Per-user `RateLimiter` instances via `ConcurrentHashMap` |

**Concurrency curveball:** `AtomicLong` vs `ReentrantLock` — when does CAS beat a mutex?

**Algorithm comparison interviewers love:**

| Algorithm | Space | Burst? | Boundary Spike? | Best for |
|-----------|-------|--------|----------------|---------|
| Token Bucket | O(1) | Yes | No | APIs allowing controlled bursts |
| Leaky Bucket | O(n) | No | No | Smooth traffic shaping |
| Fixed Window | O(1) | Yes | Yes (2x at boundary) | Coarse-grained limits |
| Sliding Window Log | O(n) | No | No | Perfect accuracy, high memory |
| Sliding Window Counter | O(1) | No | Approximate | Best production trade-off |

---

### 4. ATM Machine ✅
**ROI: 9/10** · Concurrency: Medium

Mandatory for fintech interviews (JPMC, Goldman, Barclays). State pattern is unavoidable here — any other approach leads to a God Class. Interviewers want to see the state machine drawn first.

| Pattern | Why it appears |
|---------|---------------|
| State | `Idle → CardInserted → PINVerified → Dispensing → PrintingReceipt` |
| CoR | `CashDispenser` chain — `$100 → $50 → $20 → $10 → $5 → $1` |
| Strategy | `AuthenticationStrategy` (PIN / biometric), `DispensingStrategy` |
| Command | `WithdrawCommand`, `DepositCommand` — transaction log, undo |

**Concurrency curveball:** Concurrent ATM sessions for the same account — optimistic locking on balance.

---

### 5. Elevator System
**ROI: 9/10** · Concurrency: High

Amazon asks this constantly. The hard part is the scheduling strategy — interviewers discard naive FCFS and ask for LOOK or SCAN to minimize travel distance. Multiple elevators require dispatcher logic.

| Pattern | Why it appears |
|---------|---------------|
| State | `Idle / MovingUp / MovingDown / DoorOpen` per elevator |
| Strategy | Scheduling: FCFS / LOOK / SCAN — swappable without changing Elevator |
| Observer | Floor arrival triggers door open + request removal |

**Concurrency curveball:** Multiple requests arriving simultaneously — `PriorityQueue` for pending floors, thread-safe request queue per elevator.

---

### 6. Movie Booking (BookMyShow) ✅
**ROI: 9/10** · Concurrency: Very High

The concurrency challenge IS the problem. Any design that doesn't handle double-booking prevention explicitly will fail. Interviewers check whether you use pessimistic vs optimistic locking and why.

| Pattern | Why it appears |
|---------|---------------|
| Facade | `BookingFacade` unifies `SeatService + PaymentService + NotificationService` |
| Strategy | `SeatLockService` — lock expiry policy (time-based) |
| Observer | `BookingEventListener` — seat release on payment failure |
| Repository | `ShowRepository`, `BookingRepository` — clean data access layer |

**Concurrency model:** Pessimistic per-show locking via `synchronized` on `Show` object stored in `ConcurrentHashMap`. Atomic check-and-lock within the critical section.

---

### 7. Custom Thread Pool ✅
**ROI: 9/10** · Concurrency: Very High

The highest-signal concurrency problem for Java. Building it from scratch (no `ThreadPoolExecutor`) demonstrates deep understanding of `BlockingQueue`, worker lifecycle, and graceful shutdown. JPMC asks this directly.

| Pattern | Why it appears |
|---------|---------------|
| Builder | Fluent configuration — core threads, max threads, queue capacity, rejection policy |
| Strategy | `RejectionPolicy` — CallerRuns / Discard / DiscardOldest / Abort |
| Factory | `WorkerFactory` — creates and names threads |
| State | Worker state machine: IDLE / RUNNING / TERMINATED |
| Producer-Consumer | Task queue + worker pool |

**Concurrency curveball:** Graceful shutdown — `shutdown()` drains the queue; `shutdownNow()` interrupts workers.

---

### 8. Logging Framework ✅
**ROI: 9/10** · Concurrency: Medium

Underestimated — most candidates think "just print to console." The real ask is: how do you add a new output sink (Slack, PagerDuty) without modifying existing handlers? That's CoR + OCP.

| Pattern | Why it appears |
|---------|---------------|
| CoR | `ConsoleHandler → FileHandler → EmailAlertHandler` — severity-gated chain |
| Decorator | `TimestampDecorator`, `ColorDecorator`, `FilterDecorator` on top of any handler |
| Strategy | `Formatter` — JSON / text / structured |
| Template Method | `AbstractHandler.log()` skeleton — subclasses implement `doLog()` |
| Singleton | `LogManager` — one per JVM |

---

### 9. Splitwise
**ROI: 9/10** · Concurrency: Low

The DSA-in-LLD problem. The design (Strategy for split types) is straightforward; the hard part is the balance simplification algorithm. Interviewers at product companies (Uber, Flipkart) explicitly ask for graph-based debt minimization.

| Pattern | Why it appears |
|---------|---------------|
| Strategy | `SplitStrategy` — Equal, Exact, Percentage, Ratio |
| Observer | Balance change triggers notification to all participants |

**DSA overlap:** Minimize cash flow = graph problem. Greedy approximation: repeatedly settle largest creditor vs largest debtor. NP-hard optimal solution is a discussion point, not an implementation requirement.

---

### 10. Vending Machine ✅
**ROI: 9/10** · Concurrency: Low

The canonical State pattern demo. Every interviewer who wants to test State will use this or ATM. Simpler domain than ATM — ideal when you have 60 min, not 90.

| Pattern | Why it appears |
|---------|---------------|
| State | `Idle → HasCoin → ProductSelected → Dispensing → OutOfStock` |
| Strategy | `PaymentHandler` — exact change vs overpayment policy |
| Factory | `ProductFactory` — creates dispense items |

---

## Tier 2 — High Value (ROI 7–8)

These appear in 30–50% of LLD rounds. Strong additions that differentiate SDE-2 from SDE-3 profiles.

---

### 11. Chess
**ROI: 8/10** · Concurrency: Low

The SDE-2→SDE-3 differentiator. The domain is complex enough that a shallow design falls apart immediately. Composition over inheritance is pushed to its limit here — a `Piece` should not use inheritance for movement rules.

| Pattern | Why it appears |
|---------|---------------|
| Strategy | `MovementStrategy` per piece type — King, Queen, Rook, Bishop, Knight, Pawn each implement their own |
| State | `GameState` — `IN_PROGRESS / CHECK / CHECKMATE / STALEMATE / DRAW` |
| Command | `MoveCommand` — full move history, undo last move |
| Composite | Board + Pieces, move validation traversal |

**Hard parts interviewers probe:** King-in-check prevents all other moves (must validate every candidate move), en passant, castling, pawn promotion.

---

### 12. Task Scheduler / Cron ✅
**ROI: 8/10** · Concurrency: High

Demonstrates producer-consumer + delayed execution. Interviewers check whether you know `DelayQueue` semantics and can separate scheduling from execution (dispatcher thread vs worker pool).

| Pattern | Why it appears |
|---------|---------------|
| Builder | Fluent task config — `one-time / fixed-rate / fixed-delay / cron` |
| Adapter | `DelayedTask` adapts `Task` to `Delayed` interface for `DelayQueue` |
| Strategy | `SchedulingPolicy` — fixed-rate vs fixed-delay differ in whether delay is from start or end |
| Producer-Consumer | Dispatcher thread enqueues tasks; worker pool executes |

---

### 13. Order Book Engine ✅
**ROI: 8/10** · Concurrency: Very High

Fintech-specific. The key insight (thread confinement per symbol) is rarely seen in textbooks — it demonstrates production-grade thinking about concurrency without a single `synchronized` inside the matching core.

| Pattern | Why it appears |
|---------|---------------|
| Thread Confinement | `SingleThreadExecutor` per symbol — serial safety without locks inside matching engine |
| Strategy | `TradeListener` — decouple event handling from matching |
| Producer-Consumer | External producers (order sources) + per-symbol executor queue |
| Facade | `OrderBookFacade` — submit/cancel unified API |

**DSA overlap:** `TreeMap<BigDecimal, Deque<Order>>` for price-time priority — O(log n) price access, FIFO within a price level.

---

### 14. Notification System
**ROI: 7/10** · Concurrency: Medium

Rarely the primary problem — almost always a curveball add-on ("now send a confirmation email when a seat is booked"). Knowing it cold means you can deliver it in 10 min during a BookMyShow round.

| Pattern | Why it appears |
|---------|---------------|
| Observer | `NotificationService` subscribes to domain events |
| Strategy | Channel routing — Email / SMS / Push / Slack |
| Decorator | `RetryDecorator`, `ThrottleDecorator` on top of any channel |
| Factory | `NotificationChannelFactory` |

---

### 15. Payment Gateway
**ROI: 7/10** · Concurrency: Medium

Good pattern combination. The key design decision is whether to use CoR (sequential validation pipeline) or Strategy (provider selection). Answer: both — CoR for validation, Strategy for provider.

| Pattern | Why it appears |
|---------|---------------|
| Strategy | Payment provider — Stripe / PayPal / Razorpay swappable |
| CoR | Validation pipeline — `FraudCheck → LimitCheck → CurrencyCheck → Execute` |
| Facade | Single `PaymentFacade.pay()` hides provider + validation complexity |
| Decorator | `RetryDecorator`, `AuditDecorator` around any provider |

---

### 16. Circuit Breaker
**ROI: 7/10** · Concurrency: High

Underrated for Java/Spring engineers — Spring Cloud Circuit Breaker (Resilience4j) uses exactly this design internally. Shows production microservices thinking.

| Pattern | Why it appears |
|---------|---------------|
| State | `CLOSED (healthy) → OPEN (failing) → HALF_OPEN (testing)` — atomically managed |
| Proxy | `CircuitBreakerProxy` wraps the real service; caller never touches the real service |
| Strategy | `FallbackStrategy` — what to return when the circuit is open |

**Concurrency:** State transitions must be atomic under concurrent requests — `AtomicReference<State>` + CAS.

---

### 17. Pub-Sub / Event Bus
**ROI: 7/10** · Concurrency: High

The "build Kafka in memory" problem. Tests whether you understand backpressure and async delivery. A synchronous implementation is a junior answer; an async one with `BlockingQueue` per subscriber is the target.

| Pattern | Why it appears |
|---------|---------------|
| Observer | Core — topic → subscribers mapping |
| Strategy | Delivery guarantee — at-most-once / at-least-once |
| Decorator | Message filtering, transformation, dead-letter routing |
| Factory | `TopicFactory`, `SubscriberFactory` |

---

### 18–23 (Summary)

| # | Problem | Standout pattern | Why it matters |
|---|---------|-----------------|----------------|
| 18 | Library Management | Strategy (fine calc), Observer (availability) | Classic CRUD LLD — good 60-min warm-up |
| 19 | Hotel Management | State (room lifecycle), Observer (housekeeping) | State machine + multi-actor system |
| 20 | Snake and Ladder | State, Strategy (dice) | Quick extensibility demo — 4-6 hrs |
| 21 | Ride-Sharing | Strategy (matching/pricing), Observer (location) | Domain-specific — Uber/Ola rounds |
| 22 | Bloom Filter ✅ | Strategy (hash), Builder (optimal params), Decorator (CountingBF) | DSA + OCP combo — infra companies |
| 23 | Symbol Search ✅ | Strategy (match), Decorator (LRU cache), Template Method | Trigram index DSA + design — IDE teams |

---

## Tier 3 — Good Additions (ROI 5–6)

Appear in 10–25% of rounds. Worth practicing after Tiers 1–2 are solid.

| # | Problem | Key Patterns | Notes |
|---|---------|-------------|-------|
| 24 | Amazon Locker | Strategy, State, Factory | Amazon-specific — domain prompt at Amazon rounds |
| 25 | Shopping Cart + Order | Observer, Strategy, Composite, State, Command | E-commerce baseline CRUD LLD |
| 26 | Social Media Feed | Observer, Strategy, Decorator | Fanout-on-write vs read is the interesting HLD angle |
| 27 | Coupon / Discount Engine | Strategy, CoR, Decorator, Composite | Stacking rules + priority chains |
| 28 | Document Editor | Command, Memento, Observer, Composite | Command + Memento is the clean undo/redo combo |
| 29 | Food Delivery (Zomato) | State, Observer, Strategy | Order lifecycle state machine |
| 30 | Traffic Light System | State, Observer, Strategy | Simpler than ATM — good State practice |
| 31 | Car Rental | State, Factory, Strategy | Similar lifecycle to Hotel |
| 32 | Restaurant POS | State, Observer, Strategy, Command | Table + order lifecycle |
| 33 | Tic Tac Toe | State, Strategy, Observer | Warm-up only — rarely the main ask |
| 34 | Hospital Management | State, Factory, Observer | Broad domain — good for documentation practice |
| 35 | Inventory Management | Observer, Strategy, Template Method | CRUD-heavy — low design density |

---

## Tier 4 — Niche (ROI 3–4)

Low frequency. Practice only after Tiers 1–3 are comfortable.

| # | Problem | Key Patterns | Notes |
|---|---------|-------------|-------|
| 36 | Call Center | CoR, Strategy, Observer | BPO/telecom domain-specific |
| 37 | Flight Booking | Facade, State, Strategy | Simpler than MovieBooking — rarely asked standalone |
| 38 | Bowling Alley | State, Strategy, Observer | Novelty — demonstrates OOP breadth |
| 39 | Coffee Machine | State, Strategy, Template Method | Embedded/IoT companies only |
| 40 | Online Auction | Observer, Strategy, State, Command | eBay-like companies only |

---

## Pattern Frequency Analysis

Which patterns recur most across all 40 problems — these are the ones to know cold.

| Pattern | Appears in (of 40) | Must-know depth |
|---------|-------------------|-----------------|
| **Strategy** | 38 | Core — every algorithm variation uses this |
| **Observer** | 34 | Core — every event / notification uses this |
| **State** | 28 | Core — every lifecycle / mode switch uses this |
| **Factory** | 26 | Core — every type-driven creation uses this |
| **Decorator** | 18 | High — add-ons without modifying original |
| **Facade** | 16 | High — multi-subsystem orchestration |
| **Command** | 12 | High — undo/redo, transaction log |
| **CoR** | 10 | High — pipelines, escalation chains |
| **Template Method** | 10 | Medium — shared algorithm skeletons |
| **Builder** | 9 | Medium — complex object construction |
| **Composite** | 8 | Medium — tree structures |
| **Proxy** | 6 | Medium — access control, lazy init |
| **Singleton** | 6 | Medium — registries, managers |
| **Adapter** | 4 | Low — legacy integration |
| **Memento** | 3 | Low — snapshot / undo |
| **Prototype** | 2 | Low — expensive object cloning |
| **Flyweight** | 2 | Low — memory optimization |
| **Bridge** | 1 | Low — dual-axis variation |

**Takeaway:** Strategy + Observer + State cover 90% of behavioral design. If you can reach for these three without hesitation, you can sketch a credible design for almost any problem.

---

## Concurrency Complexity Breakdown

Problems ordered by concurrency difficulty — relevant for JPMC / fintech rounds.

| Level | Problems |
|-------|---------|
| **Very High** | Custom Thread Pool, Order Book Engine, Movie Booking (seat locking), Pub-Sub |
| **High** | Cache (read-write lock), Rate Limiter (atomic counters), Elevator (multi-elevator dispatcher), Task Scheduler (DelayQueue), Circuit Breaker (CAS state), Ride-Sharing |
| **Medium** | Parking Lot (spot allocation), ATM (transaction isolation), Logging (async handler), Notification (async delivery), Hotel (room booking), Food Delivery |
| **Low** | Chess, Splitwise, Vending Machine, Library, Snake & Ladder, Bloom Filter, Symbol Search |

---

## Recommended Attack Order

### Sprint 1 — Interview-Critical (do these first)
1. ✅ ~~Parking Lot~~
2. ✅ ~~Cache (LRU/LFU)~~
3. ✅ ~~Rate Limiter~~
4. ✅ ~~ATM Machine~~
5. Elevator System — **next up** 🔴
6. ✅ ~~Movie Booking~~
7. ✅ ~~Custom Thread Pool~~
8. ✅ ~~Logging Framework~~
9. Splitwise 🔴
10. ✅ ~~Vending Machine~~

### Sprint 2 — Depth & Differentiation
11. Chess 🔴
12. ✅ ~~Task Scheduler~~
13. ✅ ~~Order Book Engine~~
14. Notification System 🟡
15. Payment Gateway 🟡
16. Circuit Breaker 🟡
17. Library Management 🟡
18. Hotel Management 🟡
19. Snake and Ladder 🟡

### Sprint 3 — Polish
20–35. Tier 3 problems as domain-specific preparation

---

## Completion Status in This Repo

| Status | Count | Problems |
|--------|-------|---------|
| ✅ Done | 10 | Parking Lot, Cache, Rate Limiter, ATM, Movie Booking, Thread Pool, Logging, Vending Machine, Order Book, Symbol Search |
| 🔴 Up next | 4 | Splitwise, Chess, Elevator, Snake & Ladder |
| 🟡 Backlogged | 2 | Hotel Management, Library Management |
| ⬜ Not started | 24 | Rest of Tier 2–4 |
