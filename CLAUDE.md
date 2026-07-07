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
    class_diagram.mermaid → Raw classDiagram block (no fences) extracted from DESIGN_DICE.md — renderable directly in Mermaid tooling

com.lldprep.foundations/
    creational/   → All 5 creational patterns with good/bad examples ✅ COMPLETED
    structural/   → 7 structural patterns (coming next)
    behavioral/   → 7 behavioral patterns
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
- [x] SRP, OCP, LSP, ISP, DIP exercises
- [x] Composition vs Inheritance (FlyingFish)
- [x] Abstract Classes vs Interfaces (PaymentProcessor)
- [x] Encapsulation (BankAccount)
- [x] UML: Class Diagram + Sequence Diagram

### Phase 2: Design Patterns
**Creational:** ✅ All 5 patterns completed — `foundations/creational/`
  - Singleton (7 variations), Factory (3 types), Abstract Factory, Builder (4 variants), Prototype
**Structural:** ✅ All 6 patterns completed — `foundations/structural/`
  - Adapter, Bridge, Decorator, Facade, Flyweight, Proxy
**Behavioral:** ✅ All 7 patterns completed — `foundations/behavioral/`
  - Strategy, Observer, Command, State, Template Method, Iterator, Chain of Responsibility

### Phase 3: Building Blocks
- [x] In-Memory Cache (LRU + LFU via Strategy) — *Completed 2026-03-24; LFU O(1) + TTL + write-behind added 2026-06-09*
- [x] Custom Thread Pool
- [x] Rate Limiter (Token Bucket + Leaky Bucket)
- [x] Logging Framework
- [x] Task Scheduler

### Phase 4: Machine Coding Problems
- [x] Order Book Engine
- [x] Symbol Search Engine
- [x] Parking Lot System
- [x] Vending Machine
- [x] ATM Machine
- [x] Movie Booking System (BookMyShow)
- [ ] Splitwise
- [ ] Chess
- [ ] Snake and Ladder
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

## Code Review Exercises

`code-review/` holds interview-style **code review** practice — realistic
multi-file PRs with seeded issues, graded against `code-review/RUBRIC.md`.
Two project skills drive it (in `.claude/skills/`):
- **code-review-author** — create a new exercise ("make a code review exercise about X").
- **code-review-interview** — run + grade a mock review ("run code review exercise 01").

Never reveal an exercise's `ANSWER_KEY.md` to the candidate before they submit.

---

## Reference Files

- `INSTRUCTIONS.md` — Full D.I.C.E. workflow and architectural standards
- `ROADMAP.md` — Ordered learning path with completion tracking
- `PATTERNS.md` — Design pattern catalog (consult before writing custom logic)
- `PATTERNS_DECISION_TREE.md` — When to use which pattern
- `QUICK_PATTERN_REFERENCE.md` — 2-minute pattern cheat sheet for interviews
- `LLD_ROI.md` — 40 LLD problems ranked by ROI with pattern breakdown and attack order

## Related Repositories

| Repo | Path | Purpose |
|------|------|---------|
| DSA POTD | `/Volumes/Crucial_X9/DSA_POTD` | Daily algorithm problems — maintenance mode (1 easy/medium every 2 days) |
| HLD Prep | `/Volumes/Crucial_X9/HLD_prep` | System design — Phases 1–5, SCALE framework, concept deep dives |
