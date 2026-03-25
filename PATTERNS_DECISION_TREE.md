# Pattern Decision Tree

Use this when you're staring at a design problem and need to pick the right pattern.
Start at Step 1. Follow the branch that matches your situation.

---

## Step 1 — What kind of problem is this?

```
Is the problem about...

  HOW OBJECTS ARE CREATED?  ──────────────────────► Go to Section A (Creational)

  HOW OBJECTS ARE COMPOSED/STRUCTURED?  ──────────► Go to Section B (Structural)

  HOW OBJECTS COMMUNICATE/BEHAVE?  ───────────────► Go to Section C (Behavioral)
```

---

## Section A — Creational Patterns

```
A1. Do you need EXACTLY ONE instance shared across the whole app?
    YES ──► SINGLETON
            (e.g., ConfigManager, LogManager, ConnectionPool)
    NO  ──► Continue to A2

A2. Do you need to CREATE OBJECTS without hardcoding the exact class?
    YES ──► Continue to A3
    NO  ──► (Plain constructor is fine — no pattern needed)

A3. Are you creating a SINGLE type of object?
    YES ──► FACTORY METHOD
            (e.g., NotificationFactory creates Email/SMS/Push)

    Are you creating a FAMILY of RELATED objects that must go together?
    YES ──► ABSTRACT FACTORY
            (e.g., DatabaseFactory creates paired Connection + QueryBuilder)

A4. Does the object have MANY PARAMETERS (4+), especially optional ones?
    YES ──► BUILDER
            (e.g., HttpRequest.Builder, Pizza.Builder)

A5. Is creating the object EXPENSIVE, and you need MANY similar copies?
    YES ──► PROTOTYPE
            (e.g., clone pre-configured game pieces, template objects)
```

---

## Section B — Structural Patterns

```
B1. Are you trying to make TWO INCOMPATIBLE INTERFACES work together?
    YES ──► ADAPTER
            (e.g., wrapping a third-party SDK to match your internal interface)
    NO  ──► Continue to B2

B2. Do you have TWO INDEPENDENT DIMENSIONS that both vary?
    (e.g., message types × delivery channels, shape types × rendering APIs)
    YES ──► BRIDGE
            (avoids class explosion: 2×2 = 4 classes, not 4 subclasses)
    NO  ──► Continue to B3

B3. Do you need to ADD BEHAVIOR TO AN OBJECT AT RUNTIME without subclassing?
    YES ──► DECORATOR
            (e.g., stacking Logger decorators: Timestamp + Severity + Console)

    Quick check — Decorator vs Proxy:
    ┌─────────────────────────────────────────────────────────────────┐
    │  DECORATOR  = add new behavior (wraps to enhance)               │
    │  PROXY      = control access (wraps to gate/log/cache)          │
    └─────────────────────────────────────────────────────────────────┘
    NO  ──► Continue to B4

B4. Do you need to SIMPLIFY A COMPLEX SUBSYSTEM behind one clean API?
    YES ──► FACADE
            (e.g., BookingFacade hides SeatService + PaymentService + NotificationService)
    NO  ──► Continue to B5

B5. Do you need CONTROLLED ACCESS to an object?
    (lazy init, security check, logging calls, caching results)
    YES ──► PROXY
            (e.g., CachingProxy, AuthProxy, LazyLoadingProxy)
    NO  ──► Continue to B6

B6. Do you need THOUSANDS of similar objects and memory is a concern?
    YES ──► FLYWEIGHT
            (e.g., 10,000 forest trees sharing TreeType; text character glyphs)
    NO  ──► (No structural pattern needed — reassess the problem)
```

---

## Section C — Behavioral Patterns

```
C1. Do you have MULTIPLE ALGORITHMS for the same operation
    and need to SWAP them at runtime?
    YES ──► STRATEGY
            (e.g., EvictionPolicy: LRU/LFU/FIFO; SortStrategy; DiscountStrategy)
    NO  ──► Continue to C2

C2. When ONE OBJECT changes, do MULTIPLE OTHER OBJECTS need to react?
    (one-to-many event notification)
    YES ──► OBSERVER
            (e.g., BookingService notifies EmailSender + AuditLogger + InventoryUpdater)
    NO  ──► Continue to C3

C3. Does the object's BEHAVIOR CHANGE DRAMATICALLY based on its INTERNAL STATE?
    (and you have 3+ distinct states)
    YES ──► STATE
            (e.g., VendingMachine: Idle/HasCoin/Dispensing/OutOfStock)

    Quick check — State vs Strategy:
    ┌────────────────────────────────────────────────────────────────────┐
    │  STRATEGY = algorithm is injected from OUTSIDE, context is stable  │
    │  STATE    = behavior changes from INSIDE, state drives transitions  │
    └────────────────────────────────────────────────────────────────────┘
    NO  ──► Continue to C4

C4. Do you need to UNDO/REDO operations, or QUEUE/LOG requests?
    YES ──► COMMAND
            (e.g., text editor undo stack, ATM transaction log, task queue)
    NO  ──► Continue to C5

C5. Do multiple classes share the SAME ALGORITHM SKELETON
    but differ in SPECIFIC STEPS?
    YES ──► TEMPLATE METHOD
            (e.g., DataMigration: read → transform → write; subclasses vary each step)

    Quick check — Template Method vs Strategy:
    ┌──────────────────────────────────────────────────────────────────────┐
    │  TEMPLATE METHOD = skeleton is fixed (inheritance), steps vary       │
    │  STRATEGY        = the whole algorithm is swappable (composition)    │
    │  Rule: Strategy is more flexible. Prefer it when the skeleton itself  │
    │  might also change in the future.                                    │
    └──────────────────────────────────────────────────────────────────────┘
    NO  ──► Continue to C6

C6. Do you need to PASS A REQUEST along a CHAIN of handlers
    until one of them processes it?
    YES ──► CHAIN OF RESPONSIBILITY
            (e.g., log severity chain, authentication middleware, approval workflows)
    NO  ──► Continue to C7

C7. Do you need to TRAVERSE A COLLECTION without exposing its internal structure?
    YES ──► ITERATOR
            (e.g., custom tree traversal, graph walk, multi-floor parking spot scan)
    NO  ──► (No behavioral pattern needed — reassess the problem)
```

---

## Quick Confusion Resolver

The most commonly confused pairs, side by side:

| Confused With | Pattern A | Pattern B | The Difference |
|---|---|---|---|
| Decorator vs Proxy | **Decorator** | **Proxy** | Decorator *adds behavior*; Proxy *controls access*. Both wrap the same interface. |
| Strategy vs State | **Strategy** | **State** | Strategy is injected externally and stays stable; State transitions internally and drives its own changes. |
| Strategy vs Template Method | **Strategy** | **Template Method** | Strategy = whole algorithm is swappable (composition). Template Method = skeleton is fixed, steps vary (inheritance). |
| Factory Method vs Abstract Factory | **Factory Method** | **Abstract Factory** | Factory Method creates one product type. Abstract Factory creates a *family* of related products. |
| Adapter vs Facade | **Adapter** | **Facade** | Adapter makes *one* incompatible interface compatible. Facade *simplifies* a complex subsystem behind a new interface. |
| Aggregation vs Composition | **Aggregation** | **Composition** | Aggregation: child can exist without parent. Composition: child cannot exist without parent. |

---

## Cheat Sheet — Pattern → Signal Words

When you hear these words in a problem statement, think of these patterns:

| Signal Word / Phrase | Consider This Pattern |
|---|---|
| "only one instance", "global access", "shared resource" | Singleton |
| "create without knowing the exact type", "plug in a new type" | Factory Method |
| "family of related objects", "must be compatible with each other" | Abstract Factory |
| "too many constructor parameters", "optional fields", "immutable object" | Builder |
| "clone", "copy", "expensive to create" | Prototype |
| "legacy code", "third-party SDK", "incompatible interface" | Adapter |
| "two independent hierarchies", "class explosion" | Bridge |
| "add behavior at runtime", "wrap with extra functionality" | Decorator |
| "simplify", "single entry point", "hide complexity" | Facade |
| "memory", "thousands of objects", "shared state" | Flyweight |
| "control access", "lazy load", "audit", "cache calls" | Proxy |
| "swap algorithm", "interchangeable behavior", "eliminate if-else" | Strategy |
| "notify multiple objects", "event", "publish-subscribe" | Observer |
| "undo/redo", "queue requests", "transaction", "log actions" | Command |
| "behavior depends on state", "state machine", "transitions" | State |
| "same steps, different details", "common skeleton" | Template Method |
| "traverse without exposing internals", "custom collection" | Iterator |
| "pass along a chain", "middleware", "handler pipeline" | Chain of Responsibility |
