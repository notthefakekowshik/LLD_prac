# OOP Relationships — Quick Reference

---

## At a Glance

| Relationship  | Keyword      | Arrow (UML)        | Lifecycle Dependency | How to Initialise in Java                        | Example              |
|---------------|--------------|--------------------|----------------------|--------------------------------------------------|----------------------|
| Dependency    | uses         | `A - - -> B`       | None (transient)     | Pass B as a **method parameter**                 | OrderService uses EmailService per call |
| Association   | uses-a       | `A ------> B`      | None (independent)   | Pass B via **constructor** (stored as field)     | Department holds Teacher |
| Aggregation   | has-a (weak) | `A <>-----> B`     | B survives A         | Pass List\<B\> via **constructor** (shared ref)  | Team has Players |
| Composition   | has-a (strong)| `A ◆-----> B`     | B dies with A        | **Create B inside A's constructor** (`new B()`)  | House creates its Rooms |
| Realization   | implements   | `A - - -|> B`      | N/A (type contract)  | `class A implements B`                           | Circle implements Drawable |
| Inheritance   | is-a         | `A ———|> B`        | N/A (class hierarchy)| `class A extends B`                              | Dog extends Animal |

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

> See `composition/` package — `FlyingFish` owns its `SwimBehavior`/`FlyBehavior` implementations internally,
> while the *strategies* (BasicSwim, GlideFly) are injected → that's actually association / Strategy pattern.
> True composition: `Engine` created inside `Car`'s constructor.

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
