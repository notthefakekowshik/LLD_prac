# CLAUDE.md — LLD Practice Repository

This file governs how Claude Code assists in this repository. All work here is **Java LLD interview preparation**. Follow every rule below without shortcuts.

---

## Repository Purpose

Building extensible, maintainable, enterprise-grade Java systems for LLD/machine coding interviews. This is NOT DSA — focus is on **design quality, SOLID principles, and clean code**, not algorithmic efficiency.

---

## Mandatory Workflow: D.I.C.E.

Every problem MUST follow these four steps in order:

### 1. Define (Requirements & Constraints)
Before writing any code, produce:
- **Functional Requirements** — actor-verb-noun form (e.g., "User can park a vehicle")
- **Non-Functional Requirements** — quality attributes (thread-safety, O(1) lookup, etc.)
- **Constraints** — hard limits (e.g., "in-memory only")
- **Out of Scope** — explicitly state what is NOT being built

### 2. Identify (Schema & Entities)
Produce a Mermaid.js class diagram and typed entity relationships:
- Use the **Noun-Verb technique**: nouns → entities, verbs → methods/relationships
- Type every relationship: Association / Aggregation / Composition / Inheritance / Realization / Dependency
- **Rule:** Prefer composition over inheritance

### 3. Code (Implementation)
Order of implementation:
1. Interfaces and abstract classes first
2. Model/entity classes (pure data)
3. Interface implementations
4. Service/orchestration layer
5. `Main` demo class last

Always code to interfaces (DIP). Apply relevant design patterns from `PATTERNS.md` with a `// Why:` comment.

### 4. Evolve (Curveball Handling)
Extend the design for a new requirement without modifying existing classes (OCP). If a curveball requires modifying an existing class, the original design violated OCP — refactor and note the lesson.

---

## SOLID Principles (Non-Negotiable)

| Principle | Rule | Test |
|---|---|---|
| **SRP** | One reason to change per class | Can you name the responsibility without "and"? |
| **OCP** | Open for extension, closed for modification | New variant = new class, zero edits to existing |
| **LSP** | Subclasses must honor parent's contract | Replace parent with child — does it still work? |
| **ISP** | No class implements methods it doesn't need | No `UnsupportedOperationException` in implementations |
| **DIP** | Depend on abstractions, not concretions | Constructor injection; no `new ConcreteType()` inside classes |

---

## Package Structure

```
com.lldprep.<problem>/
    model/        → POJOs, Entities, Enums
    policy/       → Strategy/Policy interfaces and implementations
    service/      → Core business logic, orchestration
    repository/   → In-memory data storage (Maps, Sets, Lists)
    exception/    → Custom checked/unchecked exceptions
    factory/      → Factory classes (if applicable)
```

---

## Coding Standards

- **Generics:** Use `Cache<K, V>` not `Cache<String, Object>`
- **Concurrency:** Use `synchronized`, `ReentrantLock`, or `Atomic*` for shared mutable state. Mark critical sections: `// CRITICAL SECTION — shared mutable state`
- **Exceptions:** Define custom exceptions in `exception/`. Never swallow silently. Checked = recoverable, unchecked = programming error.
- **No magic numbers/strings** — use `enum` or constants
- **No `instanceof` chains** — use polymorphism

---

## Anti-Patterns to Avoid

| Anti-Pattern | Fix |
|---|---|
| God Class (one class does everything) | Split by responsibility (SRP) |
| Switch on type / `instanceof` chains | Polymorphism |
| Anemic Model (only getters/setters) | Move behavior into the entity |
| Hard-coded dependencies (`new Concrete()`) | Constructor injection (DIP) |
| Catch-all exceptions | Handle specific exceptions |
| Premature abstraction | Wait until you have two concrete cases |

---

## Current Roadmap Progress

### Phase 1: Foundations
- [ ] SRP, OCP, LSP, ISP, DIP exercises
- [ ] Composition vs Inheritance (FlyingFish)
- [ ] Abstract Classes vs Interfaces (PaymentProcessor)
- [ ] Encapsulation (BankAccount)
- [ ] UML: Class Diagram + Sequence Diagram

### Phase 2: Design Patterns
**Creational:** Singleton, Factory Method, Abstract Factory, Builder, Prototype
**Structural:** Adapter, Bridge, Decorator, Facade, Flyweight, Proxy
**Behavioral:** Strategy, Observer, Command, State, Template Method, Iterator, Chain of Responsibility

### Phase 3: Building Blocks
- [x] In-Memory Cache (LRU via Strategy) — *Completed 2026-03-24*
- [ ] Custom Thread Pool
- [ ] Rate Limiter (Token Bucket + Leaky Bucket)
- [ ] Logging Framework
- [ ] Task Scheduler

### Phase 4: Machine Coding Problems
- [ ] Parking Lot System
- [ ] Movie Booking System (BookMyShow)
- [ ] Splitwise
- [ ] Snake and Ladder
- [ ] Chess
- [ ] Vending Machine
- [ ] ATM Machine
- [ ] Hotel Management System
- [ ] Library Management System

---

## Deliverables Checklist (Before Marking Any Problem Complete)

- [ ] Requirements written before code
- [ ] Mermaid.js class diagram present
- [ ] Every relationship typed
- [ ] Every class has a single, nameable responsibility
- [ ] New variants addable without modifying existing classes
- [ ] All subclasses honor parent contracts
- [ ] Interfaces are focused and minimal
- [ ] Classes depend on interfaces, not concrete types
- [ ] Design pattern documented with "why" comment
- [ ] No magic numbers/strings
- [ ] Thread-safety addressed
- [ ] Custom exceptions in `exception/`
- [ ] No `instanceof` chains
- [ ] `Main` class covers all functional requirements
- [ ] At least one curveball scenario demonstrated
- [ ] `README.md` inside the problem package

---

## Reference Files

- `INSTRUCTIONS.md` — Full D.I.C.E. workflow and architectural standards
- `ROADMAP.md` — Ordered learning path with completion tracking
- `PATTERNS.md` — Design pattern catalog (consult before writing custom logic)
- `PATTERNS_DECISION_TREE.md` — When to use which pattern
