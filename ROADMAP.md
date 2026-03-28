# LLD Preparation Roadmap

This roadmap takes you from writing "just-working" code to building extensible, enterprise-grade systems.
**Complete one phase before moving to the next.**

---

## Phase 1: The Foundations (The Grammar)

Master these before any design patterns. Patterns make no sense without a solid OOP and SOLID foundation.

### 1.1 S.O.L.I.D Principles Mastery
For each principle: write a *bad* version first, then refactor it. Understanding the violation is more important than memorizing the rule.

- [ ] **SRP** — Write a `ReportGenerator` class that fetches data, formats it, AND emails it. Refactor into `DataFetcher`, `ReportFormatter`, `EmailSender`.
- [ ] **OCP** — Write a `ShapeAreaCalculator` with a `switch` on shape type. Refactor so adding a new shape requires zero changes to existing classes.
- [ ] **LSP** — Write a `Square extends Rectangle` where `setWidth` also sets height. Observe the broken behavior. Refactor using composition.
- [ ] **ISP** — Write a fat `Worker` interface with `work()`, `eat()`, `sleep()`. Create a `Robot` that can't eat/sleep. Refactor into `Workable`, `Eatable`, `Restable`.
- [ ] **DIP** — Write a `UserService` that directly creates `new MySQLUserRepository()` inside it. Refactor to inject a `UserRepository` interface via constructor.

### 1.2 OOP Deep Dive
- [ ] **Composition vs Inheritance** — Build a `FlyingFish`. Try it with inheritance first (inheriting both `Fish` and `Bird`) — observe the conflict. Rebuild it using composition (`SwimBehavior`, `FlyBehavior` as injected strategies).
- [ ] **Abstract Classes vs Interfaces** — Build a `PaymentProcessor` hierarchy. Use an interface for the contract, an abstract class for shared boilerplate (e.g., logging, validation). Know when each applies:
  - Interface: pure contract, multiple inheritance needed, no shared state.
  - Abstract Class: shared implementation, enforce a template, "is-a" relationship is real.
- [ ] **Encapsulation** — Build a `BankAccount`. Fields are private. Balance can never go negative. The invariant is enforced inside the class — callers can't break it from outside.

### 1.3 UML with Mermaid.js
- [ ] **Class Diagram** — Draw the class diagram for your `BankAccount` exercise above. Include relationship types (composition, inheritance, association).
- [ ] **Sequence Diagram** — Draw the sequence of calls when a user books a movie ticket: `User → BookingFacade → SeatService → PaymentService → NotificationService`.

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

For each pattern: study the entry in `PATTERNS.md`, implement it in isolation (a small standalone class), then check it off. Use `PATTERNS_DECISION_TREE.md` to understand *when* to reach for it.

### 2.1 Creational Patterns
- [ ] **Singleton** — Implement a thread-safe `ConfigManager` using double-checked locking.
- [ ] **Factory Method** — Implement a `NotificationFactory` that creates `EmailNotification`, `SMSNotification`, or `PushNotification` based on a type enum.
- [ ] **Abstract Factory** — Implement a `DatabaseFactory` that creates paired `Connection` + `QueryBuilder` for either MySQL or PostgreSQL — can swap the entire family with one line.
- [ ] **Builder** — Implement an immutable `HttpRequest` with required (url, method) and optional (headers, body, timeout) fields.
- [ ] **Prototype** — Implement a `GamePieceRegistry` that stores prototype chess pieces and clones them on demand.

### 2.2 Structural Patterns
- [ ] **Adapter** — Adapt a `LegacyXMLParser` (returns `org.w3c.dom.Document`) to your `DataParser` interface (returns `Map<String, Object>`).
- [ ] **Bridge** — Implement `Message` (SimpleMessage / UrgentMessage) × `MessageSender` (Email / SMS) — 2 × 2 combinations with no class explosion.
- [ ] **Decorator** — Implement a `Logger` and stack decorators: `TimestampDecorator` + `SeverityDecorator` + `ConsoleLogger`.
- [ ] **Facade** — Implement a `HomeTheaterFacade` that simplifies starting (`dvd.on()`, `amplifier.setVolume()`, `projector.on()`, ...) into one `watchMovie()` call.
- [ ] **Flyweight** — Implement a `ForestRenderer` with 1,000 trees. Share the `TreeType` (name, color, texture) as a flyweight; keep position (x, y) as extrinsic state.
- [ ] **Proxy** — Implement a `CachingImageProxy` that loads an image from disk only on first access, then serves from cache.

### 2.3 Behavioral Patterns
- [ ] **Strategy** — Implement a `Sorter` that accepts a `SortStrategy` (`BubbleSort`, `QuickSort`, `MergeSort`). Swappable at runtime.
- [ ] **Observer** — Implement a `WeatherStation` that notifies `PhoneDisplay`, `TVDisplay`, and `Logger` when temperature changes.
- [ ] **Command** — Implement a text editor with `TypeCommand`, `DeleteCommand`, and a history stack for undo/redo.
- [ ] **State** — Implement a `TrafficLight` with states (Red/Yellow/Green) that transitions automatically and behaves differently per state.
- [ ] **Template Method** — Implement a `DataMigration` skeleton with `readFromSource()`, `transformData()`, `writeToTarget()`. Implement `CSVtoJSON` and `XMLtoCSV` subclasses.
- [ ] **Iterator** — Implement a `BinaryTree` with in-order, pre-order, and post-order iterators without exposing the tree's internal structure.
- [ ] **Chain of Responsibility** — Implement a `LogHandler` chain: `DebugHandler → InfoHandler → WarnHandler → ErrorHandler`. Each handles its level and passes the rest up.

---

## Phase 3: Building Blocks (Reusable Components)

Each of these is a real-world system component. Apply the full D.I.C.E. workflow from `INSTRUCTIONS.md`.

- [x] **In-Memory Cache** — LRU eviction policy via Strategy pattern. ✓ *Completed 2026-03-24*
- [ ] **Custom Thread Pool** — Implement `Executor` with a task queue and fixed worker thread pool. No `java.util.concurrent.ThreadPoolExecutor` — build it from scratch.
- [x] **Rate Limiter** — Implement both Token Bucket and Leaky Bucket algorithms. Expose a `RateLimiter` interface; algorithms are strategies. ✓ *Completed 2026-03-28*
- [ ] **Logging Framework** — Implement `Logger` with severity levels, multiple `Handler` types (Console, File), and `Formatter` types (Plain, JSON). Use Chain of Responsibility + Decorator.
- [ ] **Task Scheduler** — Implement a cron-like scheduler that accepts tasks with delay/period and executes them on a thread pool.

---

## Phase 4: Masterpieces (Machine Coding Problems)

These are full interview-style problems. Target 90–120 minutes per problem following the time splits in `INSTRUCTIONS.md`. Each must have a `README.md` and a working `Main` demo class.

- [ ] **Parking Lot System** — Multiple levels, multi-vehicle types (Car/Bike/Truck), EV charging spots, concurrent access.
- [ ] **Movie Booking System (BookMyShow)** — Multiple cities/theaters/screens, seat selection, concurrent booking prevention.
- [ ] **Splitwise** — Expense tracking, multiple split types (Equal/Exact/Percentage), balance simplification.
- [ ] **Snake and Ladder** — Board game with multiple players, pluggable dice, extensible for Chess pieces.
- [ ] **Chess** — Board, pieces with movement rules, turn management, check detection.
- [ ] **Vending Machine** — Product inventory, coin handling, state machine (Idle/HasCoin/Dispensing/OutOfStock).
- [ ] **ATM Machine** — Card authentication, PIN validation, cash dispensing chain, transaction logging.
- [ ] **Hotel Management System** — Room types, booking lifecycle, housekeeping scheduling, billing.
- [ ] **Library Management System** — Book catalog, member management, fine calculation, reservation queue.
