# LLD Preparation Roadmap

This roadmap takes you from writing "just-working" code to building extensible, enterprise-grade systems.
**Complete one phase before moving to the next.**

---

## Phase 1: The Foundations (The Grammar)

Master these before any design patterns. Patterns make no sense without a solid OOP and SOLID foundation.

### 1.1 S.O.L.I.D Principles Mastery
For each principle: write a *bad* version first, then refactor it. Understanding the violation is more important than memorizing the rule.

- [x] **SRP** — Write a `ReportGenerator` class that fetches data, formats it, AND emails it. Refactor into `DataFetcher`, `ReportFormatter`, `EmailSender`.
- [x] **OCP** — Write a `ShapeAreaCalculator` with a `switch` on shape type. Refactor so adding a new shape requires zero changes to existing classes.
- [x] **LSP** — Write a `Square extends Rectangle` where `setWidth` also sets height. Observe the broken behavior. Refactor using composition.
- [x] **ISP** — Write a fat `Worker` interface with `work()`, `eat()`, `sleep()`. Create a `Robot` that can't eat/sleep. Refactor into `Workable`, `Eatable`, `Restable`.
- [x] **DIP** — Write a `UserService` that directly creates `new MySQLUserRepository()` inside it. Refactor to inject a `UserRepository` interface via constructor.

### 1.2 OOP Deep Dive
- [x] **Composition vs Inheritance** — Build a `FlyingFish`. Try it with inheritance first (inheriting both `Fish` and `Bird`) — observe the conflict. Rebuild it using composition (`SwimBehavior`, `FlyBehavior` as injected strategies).
- [x] **Abstract Classes vs Interfaces** — Build a `PaymentProcessor` hierarchy. Use an interface for the contract, an abstract class for shared boilerplate (e.g., logging, validation). Know when each applies:
  - Interface: pure contract, multiple inheritance needed, no shared state.
  - Abstract Class: shared implementation, enforce a template, "is-a" relationship is real.
- [x] **Encapsulation** — Build a `BankAccount`. Fields are private. Balance can never go negative. The invariant is enforced inside the class — callers can't break it from outside.

### 1.3 UML with Mermaid.js
- [x] **Class Diagram** — Draw the class diagram for your `BankAccount` exercise above. Include relationship types (composition, inheritance, association).
- [x] **Sequence Diagram** — Draw the sequence of calls when a user books a movie ticket: `User → BookingFacade → SeatService → PaymentService → NotificationService`.

**Mermaid sequence template:**
```
sequenceDiagram
    actor User
    User->>BookingFacade: book(seatId, userId)
    BookingFacade->>SeatService: reserve(seatId)
    SeatService-->>BookingFacade: confirmed
    BookingFacade->>PaymentService: charge(userId, amount)
    PaymentService-->>BookingFacade: receipt
    BookingFacade->>NotificationService: notify(userId, receipt)
    BookingFacade-->>User: bookingConfirmation
```

---

## Phase 2: Design Pattern Catalog (The Vocabulary)

> **✅ CREATIONAL PATTERNS COMPLETED (2026-04-14)** — All 5 creational patterns implemented with comprehensive good/bad examples. Located in `core-lld/src/main/java/com/lldprep/foundations/creational/`.

> **✅ STRUCTURAL PATTERNS COMPLETED (2026-04-17)** — All 6 structural patterns implemented with bad/good examples. Located in `core-lld/src/main/java/com/lldprep/foundations/structural/`.

### 2.1 Creational Patterns ✅ **COMPLETED**
- [x] **Singleton** — 7 implementations: Eager, DCL, Bill Pugh (recommended), Enum (most robust), Thread-Local, Registry, Fully Protected. All with reflection/serialization protection.
- [x] **Factory** — Simple Static Factory, Factory Method, Dynamic Registry with runtime registration.
- [x] **Abstract Factory** — Cross-platform UI factory (Windows/Mac) with guaranteed family consistency.
- [x] **Builder** — Basic Builder, Validation Builder, Hierarchical Builder with inheritance, Director for common configurations.
- [x] **Prototype** — Shallow/Deep copy examples, Prototype Registry pattern, proper clone() implementation.

### 2.2 Structural Patterns ✅ **COMPLETED**
- [x] **Adapter** — `LegacyPaymentGateway` adapted to `PaymentProcessor` interface. Two adapters (Legacy + Stripe) show provider swap with zero client changes.
- [x] **Bridge** — `Message` (Simple / Urgent) × `MessageSender` (Email / SMS / Slack) — 2+3 classes instead of 2×3=6. Runtime sender swap demonstrated.
- [x] **Decorator** — `Beverage` + `Coffee`/`Espresso` bases + `MilkDecorator`/`SugarDecorator`/`VanillaDecorator`. Stacking, double add-on, and different base all shown.
- [x] **Facade** — `HomeTheaterFacade` wraps Amplifier/DVDPlayer/Projector/Lights into `watchMovie()` / `endMovie()`. Direct subsystem access still works.
- [x] **Flyweight** — `TreeType` (intrinsic: name/color/texture) shared via `TreeTypeFactory`. `Tree` stores only extrinsic (x,y). 1,000 trees → 3 flyweight objects.
- [x] **Proxy** — `ImageProxy` wraps `RealImage`: lazy init (no disk I/O until `display()`), caching (second call reuses loaded image), never-displayed = zero I/O.

### 2.3 Behavioral Patterns ✅ **COMPLETED**
- [x] **Strategy** — `Sorter` with `BubbleSortStrategy`, `MergeSortStrategy`, runtime swap. `DiscountStrategy` as a real-world example.
- [x] **Observer** — `WeatherStation` with `EventManager`, generic typed observers, subscribe/unsubscribe, multi-event support.
- [x] **Command** — `TextEditor` with `TypeCommand`, `DeleteCommand`, full undo/redo stacks, `MacroCommand` (composite).
- [x] **State** — `VendingMachine` with `IdleState`, `HasCoinState`, `DispensingState`, `OutOfStockState`. Auto-transitions.
- [x] **Template Method** — `DataExporter` skeleton with `CSVExporter`, `JSONExporter`, `XMLExporter`. Abstract + hook methods.
- [x] **Iterator** — `BinaryTree` with private `Node`, three traversal iterators, native `Iterable` for-each support.
- [x] **Chain of Responsibility** — `LogHandler` chain: `ConsoleHandler` → `FileHandler` → `EmailAlertHandler`. Runtime reconfiguration.

---

## Phase 3: Building Blocks (Reusable Components)

Each of these is a real-world system component. Apply the full D.I.C.E. workflow from `INSTRUCTIONS.md`.

- [x] **In-Memory Cache** — LRU eviction policy via Strategy pattern. ✓ *Completed 2026-03-24*
- [x] **Custom Thread Pool** — Implement `Executor` with a task queue and fixed worker thread pool. No `java.util.concurrent.ThreadPoolExecutor` — build it from scratch. ✓ *Completed 2026-04-07 (Builder, Strategy, Factory, State, Future/Callable, rejection policies, metrics)*
- [x] **Rate Limiter** — Implement both Token Bucket and Leaky Bucket algorithms. Expose a `RateLimiter` interface; algorithms are strategies. ✓ *Completed 2026-03-28*
- [x] **Logging Framework** — Chain of Responsibility (Handler chain: Console → File → Alert), Decorator (ColorDecorator, TimestampPrefixDecorator, FilterDecorator), Strategy (Formatter), Template Method (AbstractHandler). ✓ *Completed 2026-05-01*
- [x] **Task Scheduler** — Implement a cron-like scheduler that accepts tasks with delay/period and executes them on a thread pool. ✓ *Completed 2026-05-01 (PriorityQueue, DelayQueue, metrics, curveball scenarios)*

---

## Phase 4: Masterpieces (Machine Coding Problems)

These are full interview-style problems. Target 90–120 minutes per problem following the time splits in `INSTRUCTIONS.md`. Each must have a `README.md` and a working `Main` demo class.

- [x] **Order Book Engine** — Per-symbol SingleThreadExecutor for lock-free matching. Price-time priority via `TreeMap<price, Deque<Order>>`. LIMIT/MARKET orders, partial fills, cancellation, concurrent multi-symbol producers. ✓ *Completed 2026-04-17 (Thread Confinement, Strategy, Producer-Consumer, Facade)*
- [x] **Symbol Search Engine** — IntelliJ-style "Search Everywhere" with trigram inverted index, three match strategies (Exact/CamelCase/Subsequence), LRU query cache as Decorator. ✓ *Completed 2026-04-29 (Strategy, Decorator, Builder, Template Method)*
- [x] **Parking Lot System** — Multiple levels, multi-vehicle types (Car/Bike/Truck), EV charging spots, concurrent access. ✓ *Completed 2026-04-17 (spot allocation, fee calculation, vehicle hierarchy)*
- [x] **Movie Booking System (BookMyShow)** — Multiple cities/theaters/screens, seat selection, concurrent booking prevention. ✓ *Completed 2026-05-31 (Facade, SeatLockService with timeout, BookingEventListener)*
- [ ] **Splitwise** *(needs revision — debt/single-edge invariant + pair-locking not yet solid)* — Expense tracking, multiple split types (Equal/Exact/Percentage), balance simplification.
- [ ] **Snake and Ladder** — Board game with multiple players, pluggable dice, extensible for Chess pieces.
- [x] **Chess** — Board, pieces with movement rules, turn management, check detection. ✓ *Completed 2026-07-09 (Template Method, Factory, Observer, Facade — Scholar's Mate, castling, en passant, promotion, pin detection)*
- [x] **Vending Machine** — Product inventory, coin handling, state machine (Idle/HasCoin/Dispensing/OutOfStock). ✓ *Completed 2026-04-14 (State Pattern, exact change mode, transaction logging)*
- [x] **ATM Machine** — Card authentication, PIN validation, cash dispensing chain, transaction logging. ✓ *Completed 2026-05-06 (State Pattern, Chain of Responsibility, Strategy)*
- [ ] **Hotel Management System** — Room types, booking lifecycle, housekeeping scheduling, billing.
- [ ] **Library Management System** — Book catalog, member management, fine calculation, reservation queue.
