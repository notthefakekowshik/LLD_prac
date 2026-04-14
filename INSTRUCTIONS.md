# Architectural Design Protocol (The Architect's Brain)

This document defines the mandatory workflow for every Low-Level Design (LLD) and Machine Coding problem addressed in this repository. Unlike DSA, which focuses on algorithmic efficiency, this repository focuses on **extensibility, maintainability, and clean code**.

---

## 1. The D.I.C.E. Workflow

Every problem must follow this iterative design lifecycle:

### Step 1: **D**efine (Requirements & Constraints)

**What to produce:** A written list of requirements before touching any code.

- **Functional Requirements** — What the system *does*. Use actor-verb-noun form.
  - e.g., "User can park a vehicle", "System assigns nearest available spot".
- **Non-Functional Requirements** — Quality attributes the system must have.
  - e.g., "Thread-safe for concurrent access", "O(1) lookup time".
- **Constraints** — Hard limits on the system.
  - e.g., "Max 10,000 parking slots", "No external database — in-memory only".
- **Out of Scope** — Explicitly state what you are NOT building.
  - e.g., "Payment processing is out of scope".

**How to think:** Ask yourself — *"What would make this system unusable if missing?"* Those are your functional requirements. Then ask *"What would make this system fail under load or change?"* — those are non-functional.

- **CRITICAL AI INSTRUCTION - PRESERVE TRUST:** The user relies heavily on this repository and the AI agent for their interview prep. DO NOT take shortcuts. You MUST NOT declare a topic complete until every single important pattern, math proof, and design question is covered from standard lists. Leaving gaps violates the user's trust.

---

### Step 2: **I**dentify (Schema & Entities)

**What to produce:** A class diagram (Mermaid.js) and a list of entities with their relationships.

**How to think — the Noun-Verb technique:**
1. Highlight every **noun** in your requirements → candidate entities.
2. Highlight every **verb** → candidate methods or relationships.
3. Ask: "Is this a *thing* (entity/class) or a *behavior* (method/interface)?"

**Relationship types (pick the right one):**

| Relationship | Meaning | Java Code | Example |
|---|---|---|---|
| **Association** | A uses B | Field reference | `Order` has a `Customer` |
| **Aggregation** | A "has-a" B, B can exist alone | Field reference | `Parking Lot` has `Levels` |
| **Composition** | A "owns" B, B cannot exist without A | Created inside A | `Order` owns `OrderItems` |
| **Inheritance** | A "is-a" B | `extends` | `Car` is a `Vehicle` |
| **Realization** | A "behaves-as" B | `implements` | `LRUPolicy` implements `EvictionPolicy` |
| **Dependency** | A temporarily uses B | Method parameter | `ParkingService` uses `SpotFinder` |

**Class Diagram Template (Mermaid.js):**
```
classDiagram
    class Vehicle {
        <<abstract>>
        -String licensePlate
        +VehicleType getType()
    }
    Vehicle <|-- Car
    Vehicle <|-- Bike
    ParkingLot "1" *-- "many" Level : owns
    Level "1" o-- "many" ParkingSpot : has
```

**Rule:** Prefer composition over inheritance. Only use inheritance when there is a true "is-a" relationship that will never change.

---

### Step 3: **C**ode (Implementation)

**What to produce:** Clean, SOLID-compliant Java code.

**Order of implementation:**
1. Define all **interfaces** and **abstract classes** first — do not write implementations yet.
2. Write **model/entity classes** (pure data, minimal logic).
3. Write **implementations** of the interfaces.
4. Write **service/orchestration layer** that wires everything together.
5. Write a **Main demo class** last.

**Coding-for-interfaces rule:** If a class directly instantiates a concrete type, ask yourself — "Will this ever need to change?" If yes, extract an interface. Example:
```java
// Bad: hard-wired to LRU
Cache cache = new Cache(new LRUEvictionPolicy());

// Good: depends on abstraction
EvictionPolicy policy = new LRUEvictionPolicy();
Cache cache = new Cache(policy);
```

**Apply relevant Design Patterns** — refer to `PATTERNS.md` and see completed examples in `foundations/creational/`. Document *why* in a comment, not just *what*.

**Ensure SOLID principles** — see Section 4 below.

---

### Step 4: **E**volve (Curveball Handling)

**What to produce:** A refactored version that accommodates a new requirement without breaking existing code (Open/Closed Principle in action).

**Common curveball categories — prepare for these:**

| Category | Example Curveball |
|---|---|
| **New variant** | "Add EV charging spots to the parking lot" |
| **New algorithm** | "Add LFU eviction policy alongside LRU" |
| **New actor** | "Add an Admin role with different permissions" |
| **Scaling constraint** | "Now support multiple floors/levels/regions" |
| **State change** | "Bookings can now be cancelled or modified" |
| **Observability** | "Log every state change for audit purposes" |
| **Integration** | "Send a notification when a spot is booked" |

**How to evaluate your design:** If a curveball requires you to modify an existing class (not just add a new one), your original design likely violated OCP. Refactor and note the lesson.

---

## 2. SOLID Principles — Quick Reference

These are non-negotiable. Internalize them before writing any class.

### S — Single Responsibility Principle (SRP)
> A class should have only one reason to change.

- **Bad:** A `ParkingLot` class that handles spot allocation, fee calculation, AND sends notifications.
- **Good:** Separate classes — `SpotAllocator`, `FeeCalculator`, `NotificationService`.
- **Test:** Can you name your class's responsibility in one sentence without using "and"?

### O — Open/Closed Principle (OCP)
> Open for extension, closed for modification.

- **Bad:** Adding a new vehicle type requires editing a `switch` statement in an existing class.
- **Good:** New vehicle type = new class that implements `Vehicle` interface. Existing code untouched.
- **Key tool:** Interfaces + polymorphism.

### L — Liskov Substitution Principle (LSP)
> Subclasses must be usable wherever their parent is used, without breaking the program.

- **Bad:** `Square extends Rectangle` — setting width on a Square also changes height, violating Rectangle's contract.
- **Good:** If a subclass can't honor the parent's contract, use composition instead of inheritance.
- **Test:** Replace every usage of the parent with the child — does the program still behave correctly?

### I — Interface Segregation Principle (ISP)
> No class should be forced to implement methods it doesn't need.

- **Bad:** One fat `Vehicle` interface with `fly()`, `sail()`, `drive()` — a Car has to implement `fly()`.
- **Good:** Separate interfaces: `Drivable`, `Flyable`, `Sailable`. Car only implements `Drivable`.
- **Test:** If a class implements an interface but some methods throw `UnsupportedOperationException`, you've violated ISP.

### D — Dependency Inversion Principle (DIP)
> Depend on abstractions, not concretions.

- **Bad:** `Cache` directly creates `new LRUEvictionPolicy()` inside its constructor.
- **Good:** `Cache` takes an `EvictionPolicy` interface via constructor injection.
- **Key tool:** Constructor injection / Dependency Injection.

---

## 3. Mandatory Architectural Standards

### Package Structure
```
com.lldprep.<problem>/
    model/        → POJOs, Entities, Enums
    policy/       → Strategy/Policy interfaces and implementations
    service/      → Core business logic, orchestration
    repository/   → In-memory data storage (Maps, Sets, Lists)
    exception/    → Custom checked/unchecked exceptions
    factory/      → Factory classes (if applicable)

com.lldprep.foundations/
    creational/   → ✅ All 5 creational patterns (good/bad examples)
    structural/   → 7 structural patterns (next)
    behavioral/   → 7 behavioral patterns
```

### Concurrency
- Use `synchronized`, `ReentrantLock`, or `Atomic*` variables where shared state is mutated.
- Prefer `ConcurrentHashMap` over `HashMap` in multi-threaded contexts.
- Identify critical sections explicitly with a comment: `// CRITICAL SECTION — shared mutable state`.

### Exception Handling
- Define custom exceptions in `exception/` (e.g., `CacheFullException`, `SpotUnavailableException`).
- Never swallow exceptions silently (`catch(Exception e) {}`).
- Use checked exceptions for recoverable conditions, unchecked for programming errors.

### Generics
- Use generics to make components reusable: `Cache<K, V>` instead of `Cache<String, Object>`.

---

## 4. Self-Review Checklist

Run through this before marking any problem as complete:

**Design**
- [ ] Did I write requirements before writing code?
- [ ] Do I have a class diagram (even a rough one)?
- [ ] Is every relationship typed (composition vs. aggregation vs. inheritance)?

**SOLID**
- [ ] Does every class have a single, nameable responsibility?
- [ ] Can I add a new variant without modifying existing classes?
- [ ] Does every subclass honor its parent's contract?
- [ ] Are interfaces focused and minimal?
- [ ] Do my classes depend on interfaces, not concrete types?

**Design Patterns**
- [ ] Is the chosen pattern documented with a "why" comment?
- [ ] Did I consult `PATTERNS.md` before writing custom logic?

**Code Quality**
- [ ] No magic numbers or strings — use `enum` or constants.
- [ ] Thread-safety addressed where applicable.
- [ ] Custom exceptions defined in `exception/`.
- [ ] No `instanceof` chains — use polymorphism instead.

**Demo**
- [ ] Does the `Main` class cover all functional requirements?
- [ ] Is at least one curveball scenario demonstrated?

---

## 5. Common Anti-Patterns (What NOT to Do)

Recognizing bad design is as important as knowing good design.

| Anti-Pattern | What It Looks Like | Fix |
|---|---|---|
| **God Class** | One class does everything (`ParkingLotManager` with 20 methods) | Split by responsibility (SRP) |
| **Switch on Type** | `if (vehicle instanceof Car)` or `switch(type)` | Polymorphism — subclass handles its own behavior |
| **Anemic Model** | Entity classes with only getters/setters, zero logic | Move relevant behavior into the entity |
| **Feature Envy** | A method constantly accesses data of another class | Move the method to the class it envies |
| **Premature Abstraction** | Creating interfaces for everything before there's a second implementation | Wait until you have two concrete cases |
| **Hard-coded Dependencies** | `new ConcreteClass()` inside a constructor or method | Constructor injection (DIP) |
| **Catch-All Exception** | `catch (Exception e) { }` | Handle specific exceptions; never suppress silently |

---

## 6. Interview Communication Protocol

LLD interviews are 50% design, 50% communication. Follow this talk-track:

1. **Clarify first (2–3 min):** Ask clarifying questions before designing. "Should the system support concurrent users?", "Is persistence required?"
2. **State your approach (1 min):** "I'll start by identifying the core entities, then define interfaces before any implementation."
3. **Think out loud:** Narrate your reasoning as you draw/code. "I'm making `EvictionPolicy` an interface here because we may want to swap LRU for LFU later — that's the Strategy pattern."
4. **Proactively mention trade-offs:** "I chose composition over inheritance here because the vehicle type could change at runtime."
5. **Invite curveballs:** After your base solution, say "Would you like me to extend this for [X]?" — shows you think about extensibility.

---

## 7. Interview Simulation Rules

- **Time Boxing:** Full machine coding problems (Phase 4) should be completed in **90–120 minutes**.
  - Define (10 min) → Identify (15 min) → Code (60 min) → Evolve (15 min)
- **Demo:** Every system must have a `Main` class demonstrating all functional requirements.
- **Documentation:** Include a `README.md` within each problem package explaining the design choices and which patterns were applied.
