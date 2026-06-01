# Gaps Tracker — LLD & Java Concurrency

> **Purpose:** Single source of truth for all missing/incomplete topics across the entire repo.
> **Generated:** 2026-05-31 | **Review cadence:** After each topic completion
> **How to use:** Pick the highest-priority 🔴 item, implement it, mark it `[x]`, commit.

---

## LLD — Low-Level Design Gaps

### Phase 4: Machine Coding Problems (Masterpieces)

#### 🔴 HIGH — Interview Staples (60%+ of LLD rounds)

- [x] **Movie Booking System (BookMyShow)** ✓ *Completed 2026-05-31*
  - 8 demo scenarios: search, seat view, happy path, concurrent locking, lock expiry, cancel, double-booking prevention, multi-user no-conflict
  - **Concurrency:** Pessimistic per-show locking via `synchronized` + `ConcurrentHashMap` — atomic check-and-lock
  - **Patterns:** Facade (`BookingFacade`), Strategy (`SeatLockService` expiry), Repository (`ShowRepository`, `BookingRepository`)
  - **4 docs:** DESIGN.md (DICE), DESIGN_DICE.md, SCHEMA.md (8 tables + Concurrency model in SQL), API_CONTRACT.md (7 endpoints + Postman traces), README.md
  - **Location:** `core-lld/src/main/java/com/lldprep/systems/moviebooking/`

- [ ] **Splitwise**
  - **ROI:** Very High — asked at Uber, Amazon, Flipkart heavily
  - **Tests:** Balance simplification algorithm (`minimizeCashFlow`), multi-currency, group management, expense types (Equal/Exact/Percentage)
  - **Algorithm core:** Graph-based debt settlement (NP-hard optimization → greedy approximation)
  - **Patterns expected:** Strategy (split types), Observer (balance notifications)
  - **Location:** `my_practice/splitwise/`

- [ ] **Chess**
  - **ROI:** High — SDE2→SDE3 differentiator
  - **Tests:** Composition over inheritance pushed to its limit, movement rules as Strategy, board representation, check/checkmate/stalemate detection
  - **Hardest parts:** King-in-check prevents all other moves, en passant, castling, pawn promotion
  - **Patterns expected:** Strategy (piece movement), State (turn management), Command (move tracking)
  - **Location:** `my_practice/chess/`

#### 🟡 MEDIUM — Strong Additions

- [ ] **Snake and Ladder**
  - **ROI:** Medium — quick win (4-6 hours), extensibility demo
  - **Tests:** Board game with multiple players, pluggable dice, extensible for Chess-style pieces
  - **Patterns expected:** State, Strategy (dice)
  - **Location:** `my_practice/snakeandladder/`

- [ ] **Hotel Management System**
  - **ROI:** Medium — system design + LLD blend
  - **Tests:** Room lifecycle, housekeeping scheduling, billing, multi-currency
  - **Patterns expected:** State (room lifecycle), Observer (housekeeping alerts)
  - **Location:** `my_practice/hotel/`

- [ ] **Library Management System**
  - **ROI:** Medium — classic CRUD-heavy LLD
  - **Tests:** Book catalog, member management, fine calculation, reservation queues
  - **Patterns expected:** Strategy (fine calculation), Observer (availability alerts)
  - **Location:** `my_practice/library/`

---

### Phase 2: Missing Behavioral Patterns (3 of 11 GoF)

- [ ] **Mediator Pattern**
  - **ROI:** Medium — chat systems, ATC, GUI frameworks (e.g., Swing dialog coordination)
  - **Use when:** Objects communicate in a complex web; want a central hub to reduce coupling
  - **Example scenario:** ChatRoom mediating messages between Users
  - **Location:** `core-lld/src/main/java/com/lldprep/foundations/behavioral/mediator/`

- [ ] **Visitor Pattern**
  - **ROI:** Low — rarely a standalone interview question
  - **Use when:** Add operations across a stable object hierarchy without modifying classes
  - **Example scenario:** Tax visitor for different product types
  - **Location:** `core-lld/src/main/java/com/lldprep/foundations/behavioral/visitor/`

- [ ] **Interpreter Pattern**
  - **ROI:** Low — DSL/rule engine context only
  - **Use when:** Grammar-based expression evaluation
  - **Example scenario:** Arithmetic expression evaluator, simple rule engine
  - **Location:** `core-lld/src/main/java/com/lldprep/foundations/behavioral/interpreter/`

---

### Cross-Cutting Gaps (Not System-Specific)

- [ ] **Event Bus / Pub-Sub Integration**
  - **ROI:** Medium — none of the 11 systems integrate Observer/notification into a real workflow
  - **Action:** Add notification layer (Observer pattern) to ParkingLot or BookMyShow
  - **Why:** Real skill is pattern composition, not pattern isolation

- [x] **Database Schema Design** ✓ *Fixed 2026-05-31*
  - ATM `SCHEMA.md` created as reference example (ER diagram, 4 tables, optimistic locking, migration path)
  - `INSTRUCTIONS.md` Section 2.5 now mandates `SCHEMA.md` for all Phase 4 systems
  - **Remaining:** ParkingLot, OrderBook, VendingMachine pending (covered when those systems are built or docs are backfilled)
  - **Location reference:** `core-lld/src/main/java/com/lldprep/systems/atm/SCHEMA.md`

- [x] **REST API Contract Design** ✓ *Fixed 2026-05-31*
  - ATM `API_CONTRACT.md` created as reference example: 7 endpoints, JSON schemas, error codes, state machine → HTTP mapping, Postman traces
  - `INSTRUCTIONS.md` Section 2.6 now mandates `API_CONTRACT.md` for user-facing Phase 4 systems
  - **Applicability rules baked in:** user-facing systems → required, backend engines → optional, building blocks/games → skip
  - **Location reference:** `core-lld/src/main/java/com/lldprep/systems/atm/API_CONTRACT.md`

---

### Stale Documentation

- [x] **Update ROADMAP.md** — TaskScheduler, ParkingLot, VendingMachine still show `[ ]` but are fully implemented ✓ *Fixed 2026-05-31*
- [x] **Add README.md** to `logging/` system ✓ *Fixed 2026-05-31*
- [x] **Add README.md** to `symbolsearch/` system ✓ *Fixed 2026-05-31*
- [x] **Normalize DESIGN_DICE.md** naming (atm, vendingmachine, symbolsearch) ✓ *Fixed 2026-05-31*
- [x] **Remove VENDING_MACHINE_PLAN.md** — redundant planning doc post-implementation ✓ *Fixed 2026-05-31*

---

## Java — Concurrency Gaps

### 🔴 HIGH — Interview Critical

- [x] **Double-Checked Locking Demo** ✓ *Completed 2026-05-31*
  - 5-part demo: why DCL exists (perf comparison), broken DCL simulation (100 threads, checks for partial init), correct DCL with volatile (zero corruption), 3 singleton alternatives compared (eager, Bill Pugh, enum), memory barriers under the hood (x86 mfence vs ARM dmb)
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/threads/DoubleCheckedLockingDemo.java`

- [x] **Thread Interruption Contract Demo** ✓ *Completed 2026-05-31*
  - 7-part demo: isInterrupted() vs Thread.interrupted(), sleep interruption, BlockingQueue interruption, non-blocking interruption (polling), restore vs propagate (3 rules), lockInterruptibly(), thread pool shutdown pattern (shutdown → awaitTermination → shutdownNow)
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/threads/ThreadInterruptionDemo.java`

- [ ] **Livelock Demo**
  - **ROI:** Medium — classic concurrency interview topic
  - **What:** Two threads constantly yielding to each other, neither making progress
  - **Include:** Detection via `ThreadMXBean` (though harder than deadlock), fix via random backoff
  - **Current coverage:** Only a table-row mention in `Synchronization_Theory.md`
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/synchronization/LivelockDemo.java`

- [ ] **Starvation Demo**
  - **ROI:** Medium — completes the deadlock/livelock/starvation trilogy
  - **What:** Low-priority thread never gets CPU due to high-priority threads, or writer starvation in ReadWriteLock
  - **Current coverage:** Table row mention + implicit in fair-lock sections
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/synchronization/StarvationDemo.java`

- [ ] **ConcurrentLinkedQueue Demo**
  - **ROI:** Low-Medium — mentioned in theory but no dedicated demo
  - **What:** Lock-free FIFO queue, Michael-Scott algorithm, compare to BlockingQueue
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/collections/ConcurrentLinkedQueueDemo.java`

- [ ] **CopyOnWriteArrayList Demo**
  - **ROI:** Low-Medium — mentioned in theory but no dedicated demo
  - **What:** Snapshot iteration, write-copy semantics, when to use (read-heavy, small list)
  - **Include:** Memory cost visualization, concurrent iteration without ConcurrentModificationException
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/collections/CopyOnWriteArrayListDemo.java`

- [ ] **ConcurrentLinkedDeque Coverage**
  - **ROI:** Low — not covered at all
  - **What:** Double-ended lock-free queue, work-stealing deque foundations
  - **Location:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/collections/ConcurrentLinkedDequeDemo.java`

---

## Java — Plain Java Gaps

### 🔴 HIGH — Daily Usage + Interview

- [ ] **Sealed Classes (Java 17)**
  - **ROI:** Very High — modern replacement for `instanceof` chains, permits hierarchies
  - **What:** `sealed class/interface permits`, exhaustive `switch` with pattern matching, hierarchy control
  - **Include:** Compare to `final`, `non-sealed`, package-private. Show why `instanceof` chains are unsafe.
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/sealed/SealedClassesDemo.java`

- [ ] **Pattern Matching for instanceof (Java 16)**
  - **ROI:** Very High — eliminates boilerplate casts
  - **What:** `if (obj instanceof String s && s.length() > 5)` — binding variable + condition
  - **Include:** Scope rules, when it fails, compare to old-style
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/sealed/PatternMatchingInstanceofDemo.java`

- [ ] **Pattern Matching for switch (Java 21)**
  - **ROI:** High — exhaustiveness checking at compile time
  - **What:** `case String s ->`, `case null ->`, guarded patterns (`when`), exhaustive sealed hierarchies
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/sealed/PatternMatchingSwitchDemo.java`

- [x] **Streams Deep Dive** ✓ *Completed 2026-05-31*
  - `Streams_Theory.md` — 12-section theory: creation, intermediate ops, terminal ops, collectors, primitive streams, parallel, infinite, reduce(), spliterator, common mistakes, interview Q&A
  - `CollectorsDeepDiveDemo.java` — groupingBy + downstream, multi-level, partitioningBy, teeing, collectingAndThen, joining, summarizingInt, toMap with merge, filtering/mapping (Java 9), custom Collector.of()
  - `ParallelStreamDeepDiveDemo.java` — when parallel wins/loses, shared mutable state bug, findFirst vs findAny, forEach/forEachOrdered, reduce combiner, LinkedList split trap
  - `PrimitiveStreamsAndInfiniteDemo.java` — IntStream/LongStream/DoubleStream, summaryStatistics, boxing cost, stream type conversions, iterate (Java 8 vs 9), generate, takeWhile/dropWhile, reduce deep dive, flatMap vs map
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/streamplayground/`

- [ ] **Reflection API**
  - **ROI:** High — SDE3 differentiator, framework-building essential
  - **What:** `Class`, `Field`, `Method`, `Constructor`, `setAccessible`, annotations at runtime
  - **Include:** Build a miniature DI container, show how Spring finds `@Autowired` fields
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/reflection/ReflectionDemo.java`

### 🟡 MEDIUM

- [ ] **Custom Annotations**
  - **ROI:** Medium — framework building
  - **What:** Define `@Retention(RUNTIME)` vs `SOURCE` vs `CLASS`, annotation processors, parameterized annotations
  - **Include:** Build a `@Validate` annotation with runtime processor
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/annotations/CustomAnnotationDemo.java`

- [ ] **NIO Demos**
  - **ROI:** Medium — 1032-line theory file exists, zero runnable demos
  - **What:** `FileChannel` (direct buffers, memory-mapped files, zero-copy `transferTo`), `Selector` (multiplexed non-blocking server), `AsynchronousFileChannel`
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/nio/`

- [ ] **Optional API Deep Dive**
  - **ROI:** Medium — bug prevention, functional style
  - **Current:** Only `orElse` vs `orElseGet` in FI interview doc
  - **Needed:** `flatMap`, `filter`, `ifPresentOrElse`, `stream()`, `or()`, `orElseThrow`
  - **Location:** `java-fundamentals/java-plain/src/main/java/com/kowshik/optional/OptionalDeepDiveDemo.java`

- [ ] **Comparator.comparing / Chaining**
  - **ROI:** Low-Medium — touched only in `CustomComparator.java`
  - **What:** `Comparator.comparing().thenComparing().reversed()`, null handling, type inference gotchas
  - **Location:** Can fold into streams deep dive or existing lambdas package

### 🟢 LOW — Already Partially Present or Self-Explanatory

- [ ] **Text Blocks (Java 15)** — used once in `DesignPrinciplesMain.java`, no dedicated coverage
- [ ] **Switch Expressions (Java 14)** — used in core-lld code, never explicitly demonstrated
- [ ] **String API additions (Java 11+)** — `isBlank`, `lines`, `repeat`, `strip`, `indent`, `transform`
- [ ] **ScopedValue (Java 21)** — mentioned in VirtualThreads theory, no demo

---

## Reactive Programming — Entire Track (0% Complete)

> **Importance:** High for SDE3 Spring WebFlux roles. Low-Medium for SDE2 generalist roles.
> **ROADMAP exists:** `java-fundamentals/java-concurrency/src/main/java/com/kowshik/reactive/ROADMAP.md`
> **Status:** 8 phases defined, 0 implementations. All `⬜`.

### Phase-by-Phase Status

- [ ] **Phase 2: Reactive Streams Spec** — ToyPublisher, ToySubscriber, BackpressureDemo, TransformProcessor
- [ ] **Phase 3: Project Reactor Core** — MonoBasicsDemo, FluxOperatorsDemo, CombiningStreamsDemo, ErrorHandlingDemo, SchedulerComparisonDemo
- [ ] **Phase 4: Hot vs Cold Publishers** — ColdVsHotDemo, SharedFluxDemo, SinkEmitterDemo
- [ ] **Phase 5: Testing & Debugging** — StepVerifierTest, VirtualTimeTest, DebuggingDemo
- [ ] **Phase 6: Spring WebFlux Integration** — ReactiveUserController, WebClientDemo, FunctionalRouter, SSEStockPriceController
- [ ] **Phase 7: Advanced Patterns** — BackpressureStrategiesDemo, ContextPropagationDemo, ReactiveCacheDemo, RetryCircuitBreakerDemo
- [ ] **Phase 8: Capstone** — Reactive Order Processing Service (complete microservice)

**Decision:** If targeting SDE2 generalist, defer reactive. If targeting Spring WebFlux roles, start Phase 2 immediately after finishing concurrency gaps.

---

## Summary — Recommended Execution Order

### First Sprint (Interview-Readiness)
1. ✅ ~~Movie Booking System (BookMyShow)~~ *Done 2026-05-31*
2. 🔴 Splitwise
3. 🔴 Chess
4. 🔴 Sealed Classes + Pattern Matching (all 3)
5. ✅ ~~Streams Deep Dive~~ *Done 2026-05-31*
6. ✅ ~~Double-Checked Locking + Thread Interruption~~ *Done 2026-05-31*

### Second Sprint (Depth & Polish)
7. 🟡 Snake and Ladder
8. 🟡 Hotel Management
9. 🟡 Library Management
10. 🟡 Reflection API
11. 🟡 Livelock + Starvation demos
12. 🟡 Mediator pattern

### Third Sprint (SDE3 Differentiator)
13. 🟢 Reactive Programming (if WebFlux target)
14. 🟡 Custom Annotations
15. 🟡 NIO Demos
16. 🟢 Remaining behavioral patterns (Visitor, Interpreter)
17. 🟢 Low-priority Java gaps (Text Blocks, Switch Expressions, String API)

### Documentation Sweep
- ✅ ~~Update ROADMAP.md (stale checkmarks)~~ *Done 2026-05-31*
- ✅ ~~Add missing README.md files (logging, symbolsearch)~~ *Done 2026-05-31*
- ✅ ~~Normalize DESIGN_DICE.md naming~~ *Done 2026-05-31*
- ✅ ~~Add Schema design doc to one system~~ *Done 2026-05-31 (ATM SCHEMA.md)*
- ✅ ~~Add API contract to one system~~ *Done 2026-05-31 (ATM API_CONTRACT.md)*

---

## Stats

| Category | Total Gaps | 🔴 High | 🟡 Medium | 🟢 Low |
|----------|-----------|---------|-----------|--------|
| LLD Systems | 5 | 2 | 3 | 0 |
| LLD Patterns | 3 | 0 | 1 | 2 |
| LLD Cross-Cutting | 1 | 0 | 1 | 0 |
| Java Concurrency | 5 | 0 | 4 | 1 |
| Java Plain | 7 | 3 | 4 | 1 |
| Reactive | 1 (track) | 0 | 1 | 0 |
| Documentation | 0 ✅ | 0 | 0 | 0 |
| **TOTAL** | **22** | **5** | **14** | **5** |

> BookMyShow completed 2026-05-31. Remaining: 22 gaps across 6 categories.
