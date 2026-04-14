# Design Pattern Catalog

For each pattern: the **problem it solves**, a **minimal Java skeleton**, **when to use it**, and **when NOT to use it**.
Use `PATTERNS_DECISION_TREE.md` to quickly pick the right pattern. Check off each pattern after you implement it in a real problem.

---

## 1. Creational Patterns — The Birth of Objects

These patterns control *how objects are created*, decoupling the caller from the concrete type.

---

### Singleton
- [ ] Implemented

**Problem:** You need exactly one instance of a class shared across the entire application (e.g., a config manager, connection pool, logger).

**Skeleton:**
```java
public class ConfigManager {
    private static volatile ConfigManager instance;

    private ConfigManager() { }

    public static ConfigManager getInstance() {
        if (instance == null) {
            synchronized (ConfigManager.class) {
                if (instance == null) {          // double-checked locking
                    instance = new ConfigManager();
                }
            }
        }
        return instance;
    }
}
```

**Use when:**
- Exactly one shared resource must exist (DB connection pool, thread pool, registry).
- Global state must be controlled and accessed uniformly.

**Avoid when:**
- You're tempted to use it just to avoid passing objects around — that's a smell.
- You need to unit test in isolation (Singletons make mocking hard; prefer DI instead).

**LLD Example:** `ParkingLotRegistry`, `LogManager`, `CacheManager`.

---

### Factory Method
- [ ] Implemented

**Problem:** You need to create objects, but the exact type shouldn't be hardcoded in the calling code — let subclasses or a factory decide.

**Skeleton:**
```java
// Abstract creator
public interface VehicleFactory {
    Vehicle createVehicle(String licensePlate);
}

// Concrete creators
public class CarFactory implements VehicleFactory {
    public Vehicle createVehicle(String licensePlate) {
        return new Car(licensePlate);
    }
}

public class BikeFactory implements VehicleFactory {
    public Vehicle createVehicle(String licensePlate) {
        return new Bike(licensePlate);
    }
}
```

**Use when:**
- The type of object to create is determined at runtime.
- You want to isolate object creation from business logic.
- Adding new types should not require modifying existing code (OCP).

**Avoid when:**
- There is only ever one type — a factory adds complexity for no gain.
- The creation logic is trivial (a single `new`).

**LLD Example:** `SpotFactory` (creates Regular/EV/Handicap spots), `NotificationFactory` (creates SMS/Email/Push notifiers).

---

### Abstract Factory
- [ ] Implemented

**Problem:** You need to create *families* of related objects that must be used together, without specifying their concrete classes.

**Skeleton:**
```java
// Abstract factory
public interface UIFactory {
    Button createButton();
    Checkbox createCheckbox();
}

// Concrete factories — each produces a consistent family
public class DarkThemeFactory implements UIFactory {
    public Button createButton()     { return new DarkButton(); }
    public Checkbox createCheckbox() { return new DarkCheckbox(); }
}

public class LightThemeFactory implements UIFactory {
    public Button createButton()     { return new LightButton(); }
    public Checkbox createCheckbox() { return new LightCheckbox(); }
}
```

**Use when:**
- The system must work with multiple families of products (themes, OS-specific UI, payment gateways + notifiers).
- You want to enforce that only compatible objects are used together.

**Avoid when:**
- You only have one product family — Factory Method is simpler.
- Adding a new product type requires changing every factory (high maintenance cost).

**LLD Example:** `PaymentEcosystemFactory` that creates a paired `PaymentGateway` + `ReceiptGenerator` for each provider (Stripe, PayPal).

---

### Builder
- [x] Implemented (TaskBuilder in Task Scheduler)

**Problem:** Creating an object requires many parameters, some optional. Telescoping constructors become unreadable.

**Skeleton:**
```java
public class Pizza {
    private final String size;       // required
    private final String crust;      // required
    private final boolean cheese;    // optional
    private final boolean pepperoni; // optional

    private Pizza(Builder builder) {
        this.size      = builder.size;
        this.crust     = builder.crust;
        this.cheese    = builder.cheese;
        this.pepperoni = builder.pepperoni;
    }

    public static class Builder {
        private final String size;
        private final String crust;
        private boolean cheese    = false;
        private boolean pepperoni = false;

        public Builder(String size, String crust) {
            this.size  = size;
            this.crust = crust;
        }
        public Builder cheese()    { this.cheese    = true; return this; }
        public Builder pepperoni() { this.pepperoni = true; return this; }
        public Pizza build()       { return new Pizza(this); }
    }
}

// Usage
Pizza pizza = new Pizza.Builder("Large", "Thin").cheese().pepperoni().build();
```

**Use when:**
- An object has 4+ constructor parameters, especially optional ones.
- You want immutable objects with readable construction.
- The construction process involves validation before the object is created.

**Avoid when:**
- The object is simple with 1–3 required fields — a plain constructor is cleaner.

**LLD Example:** `BookingRequest.Builder`, `QueryBuilder`, `HttpRequest.Builder`.

---

### Prototype
- [ ] Implemented

**Problem:** Creating a new object from scratch is expensive (involves DB calls, heavy computation). Clone an existing object instead.

**Skeleton:**
```java
public abstract class Shape implements Cloneable {
    protected String color;
    public abstract Shape clone();
}

public class Circle extends Shape {
    private int radius;

    public Circle(Circle other) {     // copy constructor
        this.color  = other.color;
        this.radius = other.radius;
    }

    @Override
    public Shape clone() { return new Circle(this); }
}

// Registry pattern (often paired with Prototype)
public class ShapeRegistry {
    private Map<String, Shape> cache = new HashMap<>();

    public void register(String key, Shape shape) { cache.put(key, shape); }
    public Shape get(String key) { return cache.get(key).clone(); }
}
```

**Use when:**
- Object creation is expensive and you need many similar objects.
- You want to create objects without knowing their exact class (work through the interface).

**Avoid when:**
- Objects have deep references that are hard to clone correctly (circular references, shared mutable state).
- Object creation is cheap — cloning adds complexity for no gain.

**LLD Example:** Cloning pre-configured `ParkingSpot` templates, cloning game piece configurations in Chess.

---

## 2. Structural Patterns — The Skeleton of Systems

These patterns describe how to *compose objects and classes* into larger structures.

---

### Adapter
- [x] Implemented (DelayedTask adapter in Task Scheduler)

**Problem:** Two interfaces are incompatible and can't be changed. You need a translator between them.

**Skeleton:**
```java
// Existing interface your code expects
public interface PaymentProcessor {
    void pay(double amount);
}

// Third-party library with incompatible interface
public class StripeSDK {
    public void charge(int amountInCents) { /* ... */ }
}

// Adapter: wraps Stripe to look like PaymentProcessor
public class StripeAdapter implements PaymentProcessor {
    private final StripeSDK stripe;

    public StripeAdapter(StripeSDK stripe) { this.stripe = stripe; }

    @Override
    public void pay(double amount) {
        stripe.charge((int)(amount * 100));  // converts dollars → cents
    }
}
```

**Use when:**
- Integrating a third-party library whose interface doesn't match yours.
- Wrapping legacy code without modifying it.

**Avoid when:**
- You control both interfaces — just change one of them instead.
- The impedance mismatch is too deep (complex data transformation) — consider a mapper layer.

**LLD Example:** Adapting a legacy `LegacyAuthService` to a new `AuthProvider` interface.

---

### Bridge
- [ ] Implemented

**Problem:** You have two independent dimensions of variation (e.g., shape type AND rendering API). Without Bridge, you get a class explosion (`RedCircle`, `BlueCircle`, `RedSquare`, ...).

**Skeleton:**
```java
// Implementation dimension
public interface Renderer {
    void renderCircle(int radius);
}
public class VectorRenderer implements Renderer { ... }
public class RasterRenderer  implements Renderer { ... }

// Abstraction dimension — holds a reference to the implementation
public abstract class Shape {
    protected Renderer renderer;   // Bridge
    public Shape(Renderer renderer) { this.renderer = renderer; }
    public abstract void draw();
}

public class Circle extends Shape {
    private int radius;
    public Circle(Renderer renderer, int radius) {
        super(renderer);
        this.radius = radius;
    }
    @Override
    public void draw() { renderer.renderCircle(radius); }
}
```

**Use when:**
- You have two orthogonal hierarchies that need to vary independently.
- You want to avoid a combinatorial explosion of subclasses.

**Avoid when:**
- You only have one dimension of variation — Bridge adds unnecessary indirection.

**LLD Example:** `Notification` (Email/SMS/Push) × `Formatter` (Plain/HTML/JSON) — vary both independently.

---

### Decorator
- [ ] Implemented

**Problem:** You need to add behavior to an object dynamically at runtime without subclassing it (subclassing for every combination is explosive).

**Skeleton:**
```java
public interface Logger {
    void log(String message);
}

public class ConsoleLogger implements Logger {
    public void log(String message) { System.out.println(message); }
}

// Base decorator — holds a reference to the wrapped object
public abstract class LoggerDecorator implements Logger {
    protected final Logger wrapped;
    public LoggerDecorator(Logger logger) { this.wrapped = logger; }
}

// Concrete decorators
public class TimestampDecorator extends LoggerDecorator {
    public TimestampDecorator(Logger logger) { super(logger); }
    public void log(String message) {
        wrapped.log("[" + LocalDateTime.now() + "] " + message);
    }
}

public class PrefixDecorator extends LoggerDecorator {
    private final String prefix;
    public PrefixDecorator(Logger logger, String prefix) {
        super(logger);
        this.prefix = prefix;
    }
    public void log(String message) { wrapped.log(prefix + " " + message); }
}

// Usage — stack decorators at runtime
Logger logger = new TimestampDecorator(new PrefixDecorator(new ConsoleLogger(), "[INFO]"));
```

**Use when:**
- You need to add responsibilities to objects at runtime.
- Inheritance would cause a class explosion (every combination needs a subclass).

**Avoid when:**
- You need to remove decorators — unwrapping is messy.
- The order of decoration matters and is hard to control.

**LLD Example:** `LoggingCache` wraps `Cache`, `EncryptedStorage` wraps `Storage`, `AuthenticatedService` wraps `Service`.

---

### Facade
- [x] Implemented (DelayQueueScheduler facade over concurrent machinery)

**Problem:** A subsystem has many complex components. Callers shouldn't need to know the internals — give them one simple entry point.

**Skeleton:**
```java
// Complex subsystem classes
class CPU       { void freeze() {} void execute() {} }
class Memory    { void load(long pos, byte[] data) {} }
class HardDrive { byte[] read(long lba, int size) { return new byte[0]; } }

// Facade — simple interface over the complexity
public class ComputerFacade {
    private final CPU       cpu       = new CPU();
    private final Memory    memory    = new Memory();
    private final HardDrive hardDrive = new HardDrive();

    public void start() {
        cpu.freeze();
        memory.load(0, hardDrive.read(0, 1024));
        cpu.execute();
    }
}
```

**Use when:**
- You want to provide a simple API over a complex subsystem.
- You want to layer your architecture (high-level layer talks only to the Facade).

**Avoid when:**
- The "facade" becomes a God Object that does too much — break it up.
- Callers legitimately need fine-grained access to the subsystem.

**LLD Example:** `BookingFacade` that internally coordinates `SeatService`, `PaymentService`, `NotificationService`.

---

### Flyweight
- [ ] Implemented

**Problem:** You need a large number of similar objects, and storing all of them fully would consume too much memory. Share the *intrinsic* (constant) state, keep *extrinsic* (variable) state outside.

**Skeleton:**
```java
// Flyweight — contains only intrinsic (shared) state
public class CharacterGlyph {
    private final char   symbol; // intrinsic — same for all 'A's
    private final String font;   // intrinsic

    public CharacterGlyph(char symbol, String font) {
        this.symbol = symbol;
        this.font   = font;
    }

    public void render(int x, int y) { // x,y are extrinsic — passed in
        System.out.println("Render " + symbol + " at (" + x + "," + y + ")");
    }
}

// Flyweight factory — ensures sharing
public class GlyphFactory {
    private final Map<String, CharacterGlyph> cache = new HashMap<>();

    public CharacterGlyph get(char symbol, String font) {
        String key = symbol + font;
        return cache.computeIfAbsent(key, k -> new CharacterGlyph(symbol, font));
    }
}
```

**Use when:**
- You need thousands/millions of similar objects (game sprites, text characters, map tiles).
- Most of the object's state is intrinsic (can be shared).

**Avoid when:**
- The extrinsic state is complex to manage externally.
- You don't actually have a memory problem — premature optimization.

**LLD Example:** Chess piece types in a game with many boards; parking spot type objects shared across thousands of spots.

---

### Proxy
- [ ] Implemented

**Problem:** You need to control access to an object — for lazy initialization, access control, logging, or remote access — without changing the real object.

**Skeleton:**
```java
public interface DatabaseService {
    String query(String sql);
}

public class RealDatabaseService implements DatabaseService {
    public String query(String sql) { return "result"; }
}

// Proxy: same interface, adds access control
public class AuthenticatedDatabaseProxy implements DatabaseService {
    private final DatabaseService real;
    private final String          currentUser;

    public AuthenticatedDatabaseProxy(DatabaseService real, String user) {
        this.real        = real;
        this.currentUser = user;
    }

    public String query(String sql) {
        if (!hasPermission(currentUser)) throw new SecurityException("Access denied");
        return real.query(sql);
    }

    private boolean hasPermission(String user) { return user.equals("admin"); }
}
```

**Proxy Types:**
| Type | Purpose |
|---|---|
| **Virtual Proxy** | Lazy initialization — defer expensive creation until needed |
| **Protection Proxy** | Access control — check permissions before delegating |
| **Logging Proxy** | Log all calls to the real object |
| **Remote Proxy** | Represent a remote object locally (RMI, gRPC stub) |
| **Caching Proxy** | Cache results of expensive calls |

**Use when:**
- You need to add cross-cutting concerns (auth, logging, caching) without touching the real class.

**Avoid when:**
- You're adding business logic — that belongs in the service, not a proxy.
- Decorator is a better fit (Proxy is about access control; Decorator is about adding behavior).

**LLD Example:** `CachingSpotFinder` proxy caches `SpotFinder` results; `AuditLogProxy` logs all `BookingService` calls.

---

## 3. Behavioral Patterns — The Nervous System

These patterns handle *communication and responsibility* between objects.

---

### Strategy
- [ ] Implemented

**Problem:** You have a family of algorithms that are interchangeable, and you want to swap them without changing the calling code.

**Skeleton:**
```java
public interface EvictionPolicy<K> {
    void recordAccess(K key);
    K evict();
}

public class LRUEvictionPolicy<K> implements EvictionPolicy<K> {
    private final LinkedHashMap<K, Boolean> order = new LinkedHashMap<>();
    public void recordAccess(K key) { order.remove(key); order.put(key, true); }
    public K evict() { return order.keySet().iterator().next(); }
}

public class LFUEvictionPolicy<K> implements EvictionPolicy<K> {
    // ... frequency-based implementation
    public void recordAccess(K key) { /* increment count */ }
    public K evict() { /* return least frequently used */ return null; }
}

public class Cache<K, V> {
    private final EvictionPolicy<K> policy; // strategy is injected

    public Cache(EvictionPolicy<K> policy) { this.policy = policy; }

    public void put(K key, V value) {
        policy.recordAccess(key);
        // store...
    }
}
```

**Use when:**
- Multiple algorithms/behaviors exist for the same operation.
- You need to swap the algorithm at runtime or based on configuration.
- You want to eliminate conditionals (`if typeA ... else if typeB ...`).

**Avoid when:**
- You only ever use one algorithm — an interface adds complexity for no gain.
- The algorithms are trivial one-liners.

**LLD Example:** `EvictionPolicy` (LRU/LFU/FIFO), `SortStrategy`, `DiscountStrategy`, `RoutingStrategy`.

---

### Observer
- [ ] Implemented (consider adding to Task Scheduler for task lifecycle events)

**Problem:** When one object changes state, multiple other objects need to be notified automatically, without tight coupling.

**Skeleton:**
```java
public interface EventListener {
    void onEvent(String eventType, Object data);
}

public class EventManager {
    private final Map<String, List<EventListener>> listeners = new HashMap<>();

    public void subscribe(String eventType, EventListener listener) {
        listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
    }

    public void notify(String eventType, Object data) {
        List<EventListener> users = listeners.getOrDefault(eventType, List.of());
        for (EventListener listener : users) listener.onEvent(eventType, data);
    }
}

public class BookingService {
    private final EventManager events;
    public BookingService(EventManager events) { this.events = events; }

    public void book(Seat seat) {
        // ... booking logic
        events.notify("SEAT_BOOKED", seat);
    }
}

// Listeners
public class EmailNotifier   implements EventListener { public void onEvent(...) { /* send email */ } }
public class AuditLogger     implements EventListener { public void onEvent(...) { /* log */ } }
public class InventoryUpdater implements EventListener { public void onEvent(...) { /* update count */ } }
```

**Use when:**
- One event needs to trigger reactions in multiple independent components.
- You want loose coupling between the event source and its consumers.

**Avoid when:**
- The chain of notifications is hard to follow (debugging becomes painful).
- Observers modify the subject during notification (can cause infinite loops).

**LLD Example:** `ParkingSpot` notifies `DisplayBoard` + `BookingService` on availability change; order status triggers email + SMS.

---

### Command
- [ ] Implemented

**Problem:** You want to encapsulate a request as an object, so you can queue it, log it, or undo it.

**Skeleton:**
```java
public interface Command {
    void execute();
    void undo();
}

public class LightOnCommand implements Command {
    private final Light light;
    public LightOnCommand(Light light) { this.light = light; }
    public void execute() { light.turnOn(); }
    public void undo()    { light.turnOff(); }
}

// Invoker — doesn't know what the command does
public class RemoteControl {
    private final Deque<Command> history = new ArrayDeque<>();

    public void press(Command command) {
        command.execute();
        history.push(command);
    }

    public void undoLast() {
        if (!history.isEmpty()) history.pop().undo();
    }
}
```

**Use when:**
- You need undo/redo functionality.
- You need to queue, log, or schedule operations.
- You want to build transactional systems (commit/rollback).

**Avoid when:**
- The operation is simple and will never need to be undone or queued.

**LLD Example:** Text editor operations (undo/redo), ATM transaction log, game move history.

---

### State
- [ ] Implemented

**Problem:** An object's behavior changes dramatically based on its internal state. Conditional logic (`if state == IDLE ... else if state == PROCESSING`) becomes unmanageable.

**Skeleton:**
```java
public interface VendingMachineState {
    void insertCoin(VendingMachine machine);
    void selectProduct(VendingMachine machine);
    void dispense(VendingMachine machine);
}

public class IdleState implements VendingMachineState {
    public void insertCoin(VendingMachine m) {
        System.out.println("Coin inserted");
        m.setState(new HasCoinState());
    }
    public void selectProduct(VendingMachine m) { System.out.println("Insert coin first"); }
    public void dispense(VendingMachine m)       { System.out.println("Insert coin first"); }
}

public class HasCoinState implements VendingMachineState {
    public void insertCoin(VendingMachine m)    { System.out.println("Coin already inserted"); }
    public void selectProduct(VendingMachine m) {
        System.out.println("Product selected");
        m.setState(new DispensingState());
    }
    public void dispense(VendingMachine m)      { System.out.println("Select a product first"); }
}

public class VendingMachine {
    private VendingMachineState state = new IdleState();
    public void setState(VendingMachineState state) { this.state = state; }
    public void insertCoin()    { state.insertCoin(this); }
    public void selectProduct() { state.selectProduct(this); }
    public void dispense()      { state.dispense(this); }
}
```

**Use when:**
- An object has 3+ distinct states with significantly different behavior per state.
- State transitions have rules (only some transitions are valid).

**Avoid when:**
- You only have 2 states with minimal logic difference — a simple boolean flag is cleaner.

**LLD Example:** `VendingMachine`, `ATM` (Idle/CardInserted/PINEntered/Dispensing), `Order` (Placed/Confirmed/Shipped/Delivered/Cancelled).

---

### Template Method
- [ ] Implemented

**Problem:** Multiple classes share the same algorithm skeleton, but differ in specific steps. Avoid duplicating the skeleton.

**Skeleton:**
```java
public abstract class DataExporter {

    // Template method — defines the skeleton, cannot be overridden
    public final void export(String destination) {
        List<Object> data  = fetchData();        // step 1
        String formatted   = formatData(data);   // step 2 — varies
        writeToDestination(formatted, destination); // step 3
    }

    protected abstract List<Object> fetchData();
    protected abstract String formatData(List<Object> data);

    // Default implementation — subclasses can override if needed
    protected void writeToDestination(String data, String dest) {
        System.out.println("Writing to " + dest);
    }
}

public class CSVExporter  extends DataExporter {
    protected List<Object> fetchData()            { return List.of(); }
    protected String formatData(List<Object> data) { return "csv..."; }
}

public class JSONExporter extends DataExporter {
    protected List<Object> fetchData()            { return List.of(); }
    protected String formatData(List<Object> data) { return "json..."; }
}
```

**Use when:**
- Multiple classes share the same overall algorithm with minor variations in specific steps.
- You want to enforce a process invariant (the steps must always happen in a fixed order).

**Avoid when:**
- The skeleton itself varies between subclasses — use Strategy instead (Strategy is more flexible; Template Method locks the skeleton).

**LLD Example:** `ReportGenerator` (fetch → format → export), `DataMigration` (read → transform → write), game turn sequences.

---

### Iterator
- [ ] Implemented

**Problem:** You need to traverse a collection without exposing its internal structure (whether it's a list, tree, or graph).

**Skeleton:**
```java
public interface Iterator<T> {
    boolean hasNext();
    T next();
}

public class ParkingFloorIterator implements Iterator<ParkingSpot> {
    private final List<ParkingSpot> spots;
    private int index = 0;

    public ParkingFloorIterator(List<ParkingSpot> spots) { this.spots = spots; }

    public boolean hasNext() { return index < spots.size(); }
    public ParkingSpot next() { return spots.get(index++); }
}
```

**Note:** In Java, prefer implementing `java.lang.Iterable<T>` + `java.util.Iterator<T>` so your class works in a for-each loop.

**Use when:**
- You're building a custom collection and want to hide its structure.
- You need multiple traversal strategies for the same collection (BFS vs DFS for a tree).

**Avoid when:**
- You're using Java's built-in collections — `ArrayList`, `Map` already implement `Iterable`. Don't reinvent it.

**LLD Example:** Iterating `ParkingFloor` spots, traversing an org hierarchy, custom graph traversal.

---

### Chain of Responsibility
- [ ] Implemented

**Problem:** Multiple handlers can process a request. You don't want the sender to know which handler will process it — pass the request along a chain until someone handles it.

**Skeleton:**
```java
public abstract class SupportHandler {
    protected SupportHandler next;

    public SupportHandler setNext(SupportHandler next) {
        this.next = next;
        return next; // enables chaining: h1.setNext(h2).setNext(h3)
    }

    public abstract void handle(int severity);
}

public class Level1Support extends SupportHandler {
    public void handle(int severity) {
        if (severity <= 1) System.out.println("L1 handles it");
        else if (next != null) next.handle(severity);
    }
}

public class Level2Support extends SupportHandler {
    public void handle(int severity) {
        if (severity <= 2) System.out.println("L2 handles it");
        else if (next != null) next.handle(severity);
    }
}

// Setup
SupportHandler l1 = new Level1Support();
SupportHandler l2 = new Level2Support();
l1.setNext(l2);
l1.handle(2); // l1 passes to l2
```

**Use when:**
- Multiple objects may handle a request, and the handler isn't known until runtime.
- You want to avoid hardcoded conditionals for routing requests.
- Building middleware pipelines, log handlers, approval workflows.

**Avoid when:**
- Every request must be handled — CoR has no guarantee a handler will be found (add a default handler).
- Performance is critical — long chains add latency.

**LLD Example:** Logger severity chain (DEBUG → INFO → WARN → ERROR), authentication middleware pipeline, ATM cash dispensing chain.
