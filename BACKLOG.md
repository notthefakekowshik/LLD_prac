# Backlog

Items deferred from the main roadmap. Pick these up opportunistically — while solving machine coding problems or when a pattern comes up naturally.

---

## Phase 2: Design Pattern Catalog

**Decision (2026-04-07):** Deferred. Patterns will be learned on the go while building Phase 3 & 4 problems. Each machine coding problem naturally exercises several patterns — learn them in context rather than in isolation.

**How to use this backlog:**
- When a Phase 4 problem requires a pattern (e.g., Parking Lot needs State + Strategy), implement it there and check it off here.
- If a pattern never appears organically, revisit after Phase 4 is done.

---

### 2.1 Creational Patterns

- [ ] **Singleton** — Thread-safe `ConfigManager` using double-checked locking.
  - *Likely appears in:* any system needing a shared config/registry (Parking Lot, ATM)

- [ ] **Factory Method** — `NotificationFactory` creating `EmailNotification` / `SMSNotification` / `PushNotification` based on a type enum.
  - *Likely appears in:* BookMyShow (notification on booking), Hotel Management

- [ ] **Abstract Factory** — `DatabaseFactory` creating paired `Connection` + `QueryBuilder` for MySQL or PostgreSQL.
  - *Likely appears in:* any persistence-heavy problem

- [ ] **Builder** — Immutable `HttpRequest` with required (`url`, `method`) and optional (`headers`, `body`, `timeout`) fields.
  - *Already practiced in:* FixedThreadPool.Builder (thread pool refactor)

- [ ] **Prototype** — `GamePieceRegistry` storing prototype chess pieces and cloning on demand.
  - *Likely appears in:* Chess

---

### 2.2 Structural Patterns

- [ ] **Adapter** — Adapt `LegacyXMLParser` (returns `org.w3c.dom.Document`) to `DataParser` interface (returns `Map<String, Object>`).

- [ ] **Bridge** — `Message` (Simple / Urgent) × `MessageSender` (Email / SMS) — 2×2 without class explosion.

- [ ] **Decorator** — Stack `TimestampDecorator` + `SeverityDecorator` on a `ConsoleLogger`.
  - *Likely appears in:* Logging Framework (Phase 3)

- [ ] **Facade** — `HomeTheaterFacade` wrapping `dvd.on()`, `amplifier.setVolume()`, `projector.on()` into `watchMovie()`.
  - *Likely appears in:* BookMyShow (BookingFacade), Hotel Management

- [ ] **Flyweight** — `ForestRenderer` with 1,000 trees sharing `TreeType` (name, color, texture); position as extrinsic state.

- [ ] **Proxy** — `CachingImageProxy` loading from disk only on first access, then serving from cache.
  - *Already practiced in:* In-Memory Cache (Phase 3)

---

### 2.3 Behavioral Patterns

- [ ] **Strategy** — `Sorter` accepting `SortStrategy` (`BubbleSort` / `QuickSort` / `MergeSort`), swappable at runtime.
  - *Already practiced in:* Cache eviction policy, Rate Limiter algorithms, Rejection policies (thread pool), Payment strategies (factory demo)

- [ ] **Observer** — `WeatherStation` notifying `PhoneDisplay`, `TVDisplay`, `Logger` on temperature change.
  - *Likely appears in:* BookMyShow (seat availability), Parking Lot (spot availability)

- [ ] **Command** — Text editor with `TypeCommand`, `DeleteCommand`, undo/redo history stack.
  - *Likely appears in:* ATM (transaction commands), Vending Machine

- [ ] **State** — `TrafficLight` with Red/Yellow/Green states, auto-transitioning, different behavior per state.
  - *Likely appears in:* Vending Machine, ATM, Parking Lot gate

- [ ] **Template Method** — `DataMigration` skeleton with `readFromSource()`, `transformData()`, `writeToTarget()`; subclasses: `CSVtoJSON`, `XMLtoCSV`.
  - *Likely appears in:* Logging Framework (Handler base), Task Scheduler

- [ ] **Iterator** — `BinaryTree` with in-order, pre-order, post-order iterators without exposing internal structure.

- [ ] **Chain of Responsibility** — `LogHandler` chain: `DebugHandler → InfoHandler → WarnHandler → ErrorHandler`.
  - *Likely appears in:* Logging Framework (Phase 3)

---

## Notes

- Strategy, Builder, and Proxy are already well-practiced through Phase 3 work.
- State and Chain of Responsibility will be exercised naturally in the Logging Framework.
- Observer and Command will come up in BookMyShow and ATM respectively.
