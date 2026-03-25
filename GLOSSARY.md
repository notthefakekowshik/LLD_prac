# LLD Glossary

Quick reference for terms used across this repository. When an interviewer uses one of these terms, you should be able to use it back precisely.

---

## A

**Abstraction**
Hiding internal complexity behind a simple interface. The caller knows *what* an object does, not *how* it does it. In Java: interfaces and abstract classes are the primary abstraction tools.

**Aggregation**
A "has-a" relationship where the contained object can exist independently of the container. Example: `ParkingLot` has `Levels`, but a `Level` can conceptually exist without the lot. In Java: a field reference where the contained object is created *outside* and passed in.

**Association**
The weakest relationship — one object simply *uses* another. Example: `OrderService` uses a `Customer` object (passed as a method parameter). No ownership implied.

---

## C

**Cohesion**
How strongly related the responsibilities of a single class are. **High cohesion** = a class does one thing well (good). **Low cohesion** = a class does many unrelated things (bad — SRP violation). Ask: "Can I name this class's job in one sentence without 'and'?"

**Composition**
A strong "owns" relationship where the contained object cannot exist without the container. Example: `Order` owns `OrderItems` — if the order is deleted, its items are too. In Java: the contained object is instantiated *inside* the owning class.

**Concrete Class**
A class that provides full implementations for all its methods. Can be instantiated directly. Contrast with abstract class or interface.

**Contract**
The guarantee a class or interface makes to its callers. Includes: what inputs are valid, what the output will be, and what side effects (if any) will occur. Violating a contract (e.g., a subclass behaves differently than its parent) is an LSP violation.

**Coupling**
How much one class *depends on* another. **Tight coupling** = class A directly references concrete class B (hard to change/test). **Loose coupling** = class A depends on an interface that B implements (easy to swap B for another implementation). Goal: high cohesion, low coupling.

---

## D

**Delegation**
One object forwarding a call to another object to do the actual work. Composition uses delegation. Example: `Cache.get()` delegates eviction to `EvictionPolicy.evict()`. Prefer delegation over inheritance for sharing behavior.

**Dependency Injection (DI)**
Providing an object's dependencies from the outside (via constructor, setter, or method parameter) rather than creating them internally. Enables loose coupling and testability. The *mechanism* behind DIP.

---

## E

**Encapsulation**
Bundling data and the methods that operate on it into a single class, and controlling access via visibility modifiers (`private`, `protected`, `public`). Key benefit: the internal state can only be changed in controlled ways, preserving **invariants**.

**Extensibility**
The ability to add new features or variants to a system without modifying existing code. Achieved through OCP — new behavior = new class, not edited existing class.

---

## I

**Immutability**
An object whose state cannot change after construction. Immutable objects are inherently thread-safe and easier to reason about. In Java: use `final` fields, no setters, defensive copies in constructors. Example: `String`, `LocalDate`.

**Inheritance**
An "is-a" relationship where a subclass inherits state and behavior from a parent. Use sparingly — prefer composition. Only use when the subclass truly *is* a specialized version of the parent and will always honor the parent's contract (LSP).

**Interface**
A pure contract in Java — defines what methods an implementing class must provide, with no implementation (pre-Java 8). Use interfaces to define roles and capabilities, not shared implementation. Enables polymorphism and loose coupling.

**Invariant**
A condition that must always be true about an object's state, regardless of what operations are called. Example: `BankAccount.balance >= 0` is an invariant. Enforcing invariants is a core purpose of encapsulation.

---

## M

**Maintainability**
How easy a system is to change over time. High maintainability = changes are localized, side effects are minimal, code is readable. The primary goal of LLD.

---

## O

**Object Graph**
The network of objects and their relationships at runtime. Understanding the object graph helps you identify tight coupling and circular dependencies.

**Overloading**
Defining multiple methods with the same name but different parameter types/counts in the same class. Resolved at *compile time*. Not polymorphism.

**Overriding**
A subclass providing its own implementation of a method defined in the parent. Resolved at *runtime*. This IS polymorphism (dynamic dispatch).

---

## P

**Polymorphism**
The ability to treat objects of different types uniformly through a common interface. In Java: method overriding + interface references. Example: `EvictionPolicy policy` can point to an `LRUEvictionPolicy` or `LFUEvictionPolicy` — caller doesn't care which.

---

## R

**Realization**
A class *realizes* (implements) an interface's contract. The class promises to fulfill every method in the interface.

**Refactoring**
Restructuring existing code without changing its external behavior. The goal is to improve design, readability, or maintainability. Requires tests to confirm behavior is preserved.

---

## S

**Separation of Concerns (SoC)**
Dividing a system into distinct sections, each addressing a separate concern. Example: separating persistence (`repository/`), business logic (`service/`), and data models (`model/`). SRP is SoC applied at the class level.

**Side Effect**
Any change to state outside the local scope of a method — modifying a field, writing to a file, sending a network request. Methods with side effects are harder to test and reason about. Pure functions (no side effects) are predictable.

**State**
The data held by an object at a point in time. Mutable state (can change) is the source of most concurrency bugs. Favor immutable state where possible.

---

## T

**Tight Coupling**
See *Coupling*. The bad kind — one class knows too much about another's internals. Signs: `new ConcreteClass()` inside a constructor, accessing another class's fields directly, `instanceof` checks.

**Type Safety**
The guarantee that operations are performed on compatible types. Java's generics (`Cache<K, V>`) provide compile-time type safety, catching errors before runtime.

---

## U

**UML (Unified Modeling Language)**
A standard notation for visualizing software design. In this repo, use Mermaid.js to write UML in code (renders as diagrams in GitHub/IDEs). Key diagrams: Class Diagram (structure), Sequence Diagram (runtime behavior).

---

## V

**Visibility**
Controls which other classes can access a member:
| Modifier | Same Class | Same Package | Subclass | Everywhere |
|---|---|---|---|---|
| `private` | ✓ | ✗ | ✗ | ✗ |
| (default) | ✓ | ✓ | ✗ | ✗ |
| `protected` | ✓ | ✓ | ✓ | ✗ |
| `public` | ✓ | ✓ | ✓ | ✓ |

**Rule of thumb:** Default to `private`. Promote only when there's a reason.
