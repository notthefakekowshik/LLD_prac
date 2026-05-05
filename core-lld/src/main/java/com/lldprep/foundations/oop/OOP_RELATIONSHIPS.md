# OOP Relationships — Quick Reference

---

## At a Glance

| Relationship | Keyword | UML Arrow | Lifecycle | Initialization (Java) | Senior / Staff Nuance |
|--------------|---------|-----------|-----------|----------------------|----------------------|
| Dependency | Uses-a | `- - ->` | Transient: B exists only during method execution. | Method Parameter: `void call(Service s)` | Use for stateless utilities or high-volatility objects. |
| Association | Knows-a | `—————>` | Independent: A and B are peers; both exist before and after. | Constructor/Setter Injection: Stored as a field. | Avoid bi-directional association to prevent Circular Dependencies. |
| Aggregation | Has-a (Weak) | `<>————>` | Independent: B survives if A is deleted (Shared Part). | Injection + Defensive Copy: `this.list = new ArrayList(in);` | Use Collections.unmodifiableList in getters to prevent state leakage. |
| Composition | Has-a (Strong) | `◆—————>` | Dependent: B is owned by A; B dies when A dies. | Internal Creation: `this.part = new Part();` | If using DI for testing, ensure B is not shared with any other object. |
| Realization | Implements | `- - -▷` | Contract: A fulfills the behavior defined by B. | `class A implements B` | Program to Interfaces, not Implementations (Dependency Inversion). |
| Inheritance | Is-a | `—————▷` | Identity: A is a specialized version of B. | `class A extends B` | Prefer Composition over Inheritance to avoid the "Fragile Base Class" problem. |

---

## Detailed Breakdown

### 1. Dependency  `A - - -> B`
- **What**: A *uses* B only during a method call. B is NOT stored as a field in A.
- **Coupling**: Loosest of all.
- **Initialise**: Pass B as a **method argument**.
- **Lifecycle**: B has no lifecycle link to A.

```java
class OrderService {
    // NO field for EmailService
    void placeOrder(String id, EmailService email) {   // <-- method param
        email.sendConfirmation(...);
    }
}
```

---

### 2. Association  `A ------> B`
- **What**: A *uses* B long-term. B is stored as a field but created **outside** A.
- **Coupling**: Moderate. B is independent — can be shared with other classes.
- **Initialise**: Inject via **constructor** or setter.
- **Lifecycle**: B outlives A. Destroying A does NOT destroy B.

```java
class Department {
    private final Teacher teacher;              // stored field

    Department(String name, Teacher teacher) {  // <-- constructor injection
        this.teacher = teacher;
    }
}

// Usage
Teacher alice = new Teacher("Alice", "Math");   // created outside
Department d = new Department("Math", alice);   // injected in
```

---

### 3. Aggregation  `A <>-----> B`  (open diamond on A)
- **What**: A is the *whole*, B is a *part* — but B can exist without A (weak ownership).
- **Coupling**: Moderate. B is shared / re-usable across multiple wholes.
- **Initialise**: Pass a `List<B>` (or single B) created outside, via **constructor**.
- **Lifecycle**: B outlives A. Players exist after Team is disbanded.

```java
class Team {
    private final List<Player> players;         // stored field

    Team(String name, List<Player> players) {   // <-- constructor injection
        this.players = players;
    }
}

// Usage
List<Player> squad = List.of(new Player("Rohit"), new Player("Virat"));
Team india = new Team("India", squad);          // squad passed in, not owned
```

> **Aggregation vs Association**: Both inject externally. The distinction is *semantic*:
> - Association → peer relationship (Department uses Teacher, neither is "part of" the other)
> - Aggregation → whole-part relationship (Team is the whole, Players are parts — but parts are independent)

---

### 4. Composition  `A ◆-----> B`  (filled diamond on A)
- **What**: A *owns* B. B is created **inside** A. B cannot exist without A.
- **Coupling**: Strongest. B's lifecycle is controlled by A.
- **Initialise**: `new B()` **inside A's constructor** (or field declaration).
- **Lifecycle**: B dies when A is destroyed.

```java
class House {
    private final Room room;

    House() {
        this.room = new Room();   // <-- created internally, tight ownership
    }
}
```

> See `composition/car/` package for **true composition**: `Car` creates `Engine` internally via `new Engine()`.
> See `composition/good/` package for **composition over inheritance** via Strategy pattern: `FlyingFish` injects
> `SwimBehavior`/`FlyBehavior` — note that injection makes this technically association, not strict composition.

---

### 5. Realization  `A - - -|> B`  (dashed line + open triangle)
- **What**: A *fulfils the contract* defined by interface B. A promises to implement all methods.
- **Coupling**: None at runtime — caller codes to B, not A. Supports polymorphism.
- **Initialise**: `class A implements B`
- **Lifecycle**: N/A — purely a type relationship.

```java
interface Drawable { void draw(); double area(); }

class Circle implements Drawable {      // <-- realizes Drawable
    @Override public void draw()  { ... }
    @Override public double area() { ... }
}

Drawable shape = new Circle(5);         // caller uses interface type
shape.draw();
```

---

### 6. Inheritance  `A ———|> B`  (solid line + open triangle)
- **What**: A *is-a* B. A inherits state and behaviour from B.
- **Coupling**: Tight — changes to B cascade to A.
- **Initialise**: `class A extends B`
- **Lifecycle**: N/A — type hierarchy.

```java
class Animal { void breathe() { ... } }
class Dog extends Animal {              // Dog IS-A Animal
    void bark() { ... }
}
```

---

## Memory Hook

```
Dependency   →  method param      (no field, no lifecycle)
Association  →  constructor field  (field, independent lifecycle)
Aggregation  →  constructor list   (field, parts survive whole)
Composition  →  new inside ctor    (field, parts die with whole)
Realization  →  implements         (contract only)
Inheritance  →  extends            (is-a hierarchy)
```

---

## Arrow Cheat-Sheet

```
Dependency    A - - - - -> B          dashed open arrow
Association   A ----------> B          solid open arrow
Aggregation   A <>---------> B         solid line, OPEN diamond on A
Composition   A ◆----------> B         solid line, FILLED diamond on A
Realization   A - - - -|>  B           dashed line, open triangle on B
Inheritance   A ————————|>  B           solid line, open triangle on B
```
