# Java Future and CompletableFuture: The Evolution of Asynchronous Programming

To understand why `CompletableFuture` exists, we need to trace the evolution of asynchronous programming in Java, starting with the basic `Runnable` interface, moving to `Callable` and `Future`, and finally arriving at `CompletableFuture`.

---

## 1. The Pre-Future Era: `Runnable` (Java 1.0)

Initially, tasks in Java were executed using the `Runnable` interface.

```java
Runnable task = () -> {
    System.out.println("Processing...");
};
new Thread(task).start();
```

### Limitations of `Runnable`

1. **No Return Value:** The `run()` method returns `void`. There is no built-in way to get a computed result back from the background thread. You had to rely on shared memory and complex synchronization mechanisms.
2. **No Exception Handling:** The `run()` method cannot throw checked exceptions. You are forced to catch everything inside the `run()` block.

---

## 2. The Introduction of `Callable` and `Future` (Java 5)

To address the shortcomings of `Runnable`, Java 5 introduced the `Callable` and `Future` interfaces, along with the Executor framework.

### `Callable<V>`

Similar to `Runnable`, but solving both of its problems:

1. Its `call()` method returns a result of type `V`.
2. It can throw checked exceptions.

### `Future<V>`

When you submit a `Callable` to an `ExecutorService`, the execution happens in the background. Because the result isn't immediately available, the Executor immediately returns a `Future` object. The `Future` acts as a placeholder or a "promise" for the result that will be available later.

**Key Methods of `Future`:**

- `get()`: Blocks the calling thread until the computation is complete, then retrieves the result.
- `get(long timeout, TimeUnit unit)`: Blocks for a specified time, throwing a `TimeoutException` if the result is not ready.
- `isDone()`: Non-blocking check to see if the computation is completed.
- `cancel(boolean mayInterruptIfRunning)`: Attempts to cancel the execution.

### Limitations of `Future`

While `Future` enabled getting results from threads, it was insufficient for building complex, non-blocking asynchronous pipelines:

1. **Blocking `.get()`:** To retrieve the result, you had to call `future.get()`. This call **blocks** the thread calling it until the background task is finished, completely defeating the purpose of being asynchronous if you need to process the result immediately in a pipeline.
2. **No Callbacks:** You couldn't say "execute this callback function automatically when the Future completes."
3. **Cannot Chain Futures:** You couldn't easily chain multiple Futures together (e.g., "when Future A finishes, take its result, start Future B, and then combine with Future C").
4. **No Exception Handling Pipeline:** There was no elegant way to handle exceptions globally across multiple asynchronous steps.
5. **Cannot be manually completed:** You couldn't manually set the result of a `Future` if you already knew the answer.

---

## 3. The Modern Era: `CompletableFuture` (Java 8)

To solve the severe limitations of `Future`, Java 8 introduced `CompletableFuture` (which implements both `Future` and `CompletionStage`).

`CompletableFuture` is the backbone of modern asynchronous/reactive programming in Java.

### Why does it exist?

It exists to allow **composition** of asynchronous operations **without blocking threads**. Instead of submitting a task and blocking to wait for the result, you submit a task and attach a declarative pipeline of actions to execute *whenever* the result becomes available.

### Key Features and Methods

#### 1. Running Asynchronous Tasks

You don't even need an ExecutorService for basic tasks (though you can provide one). It defaults to `ForkJoinPool.commonPool()`.

- `CompletableFuture.supplyAsync(Supplier<U>)`: Runs a task asynchronously and returns a result.

> [!WARNING] ⚠️ **Dangerous Default Thread Pool**
> `CompletableFuture.supplyAsync` uses `ForkJoinPool.commonPool()` when no executor is supplied. This shared pool can become a bottleneck or cause unexpected thread starvation, especially in server‑side applications that already use the common pool for other tasks. Over‑subscribing the pool may lead to deadlocks, reduced throughput, and unpredictable latency. It is recommended to provide a dedicated `Executor` (e.g., a fixed thread pool) for I/O‑bound or long‑running tasks to isolate them from the common pool.

- `CompletableFuture.runAsync(Runnable)`: Runs a task asynchronously returning `void`.

#### 2. Attaching Callbacks (Processing results non-blockingly)

You can chain operations using methods that execute automatically when the previous stage completes:

- `thenApply(Function)`: Transforms the result (like `map` in Streams).
- `thenAccept(Consumer)`: Consumes the result without returning a new one.
- `thenRun(Runnable)`: Executes a runnable after completion (doesn't care about the result).

#### 3. Chaining Futures

When a callback itself returns a `CompletableFuture`:

- `thenCompose(Function)`: Used to chain two dependent `CompletableFuture` operations together (like `flatMap` in Streams) so you don't get a nested `CompletableFuture<CompletableFuture<T>>`.

#### 4. Combining Futures

- `thenCombine(CompletionStage, BiFunction)`: Runs two independent CompletableFutures concurrently. When *both* finish, it combines their results using the `BiFunction`.
- `CompletableFuture.allOf(...)`: Waits for a vararg list of CompletableFutures to all complete.
- `CompletableFuture.anyOf(...)`: Completes as soon as *any* of the provided CompletableFutures completes.

#### 5. Exception Handling

Instead of verbose try-catch blocks around `.get()`, you chain exception handlers:

- `exceptionally(Function)`: Acts like a `catch` block. If an exception occurs in the chain, it catches it and allows you to return a default/fallback value to keep the pipeline going.
- `handle(BiFunction)`: Acts like a `finally` block (or `try-catch` combined). It is always called, giving you access to both the result (if successful) and the exception (if it failed).

#### 6. Manual Completion

- `complete(T value)`: Manually sets the result of the future if not already completed. Excellent for building custom async APIs or handling caches.
- `completeExceptionally(Throwable)`: Manually fails the future.

---

## Example: Building a Non-Blocking Pipeline

```java
import java.util.concurrent.CompletableFuture;

public class CompletableFutureExample {
    public static void main(String[] args) {
        // 1. Start an asynchronous task
        CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() -> {
            simulateDelay(1000); // Wait 1 second
            System.out.println("1. Fetched Order from DB (Thread: " + Thread.currentThread().getName() + ")");
            return "Order #12345";
        });

        // 2. Chain operations without blocking the main thread!
        orderFuture.thenApply(order -> {
                  // This runs automatically when the previous step finishes
                  System.out.println("2. Enriching " + order + " (Thread: " + Thread.currentThread().getName() + ")");
                  return order + " - Enriched Data";
              })
              .thenAccept(enrichedOrder -> {
                  // Terminal operation
                  System.out.println("3. Saving to Database: " + enrichedOrder + " (Thread: " + Thread.currentThread().getName() + ")");
              })
              .exceptionally(ex -> {
                  // If ANY stage above threw an exception, it drops down to here.
                  System.err.println("Something went wrong at some stage: " + ex.getMessage());
                  return null;
              });

        // The main thread gets here IMMEDIATELY, before the async tasks even finish running.
        System.out.println("Main thread is NOT blocked, doing other work...");

        // Just to prevent the JVM from shutting down before the async ForkJoin pool tasks complete
        simulateDelay(2000);
    }

    private static void simulateDelay(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) {}
    }
}
```

---

## Summary Comparison Table

| Feature | `Future` | `CompletableFuture` |
| :--- | :--- | :--- |
| **Get Result** | `get()` (Blocks thread) | `join()`, `get()`, or use callbacks (Non-blocking) |
| **Callbacks** | Not possible | Yes (`thenApply`, `thenAccept`, etc.) |
| **Chaining/Pipelines**| Not possible | Yes (`thenCompose`, `thenCombine`) |
| **Exception Handling**| `try-catch` around `get()` | Built-in (`exceptionally`, `handle`) |
| **Combining Futures** | Complex internal loops | Built-in (`allOf`, `anyOf`, `thenCombine`) |
| **Manual Completion** | Not possible | Yes (`complete()`, `completeExceptionally()`) |

---

## 3.1 CompletableFuture — Full API Reference

The methods in Section 3 cover the *categories*. This section is the **complete method inventory** — every method you'll encounter, grouped by purpose, with usage frequency ratings.

### Creation (Starting a pipeline)

| Method | Returns | Use Case | Frequency |
| :--- | :--- | :--- | :--- |
| `supplyAsync(Supplier)` | `CF<T>` | Run task, return result | ✅ Most common |
| `supplyAsync(Supplier, Executor)` | `CF<T>` | Same, but on your executor (recommended for I/O) | ✅ Production must-use |
| `runAsync(Runnable)` | `CF<Void>` | Fire-and-forget task, no result | ✅ Common |
| `runAsync(Runnable, Executor)` | `CF<Void>` | Same, on your executor | ✅ Common |
| `completedFuture(value)` | `CF<T>` | Wrap an already-known value (testing, caching, early-return) | ✅ Common |
| `failedFuture(ex)` | `CF<T>` | Wrap an already-known error (Java 9+) | ✅ Common |
| `new CompletableFuture<>()` | `CF<T>` | Empty future — complete later via `complete()`/`completeExceptionally()` | ✅ Custom async APIs |
| `delayedExecutor(delay, unit)` | `Executor` | Creates an executor that delays task start (Java 9+) | 🟡 Niche |
| `delayedExecutor(delay, unit, executor)` | `Executor` | Same, wrapping your executor | 🟡 Niche |

```java
// completedFuture — useful for cache hits, default values, testing
public CompletableFuture<User> findUser(String id) {
    User cached = cache.get(id);
    if (cached != null) return CompletableFuture.completedFuture(cached);
    return CompletableFuture.supplyAsync(() -> db.findUser(id), dbExecutor);
}

// failedFuture — useful for validation failures before any async work
public CompletableFuture<Order> placeOrder(Order order) {
    if (order.getItems().isEmpty()) {
        return CompletableFuture.failedFuture(new IllegalArgumentException("Empty order"));
    }
    return CompletableFuture.supplyAsync(() -> orderService.save(order), executor);
}

// Manual completion — bridging callback-based APIs to CompletableFuture
CompletableFuture<String> cf = new CompletableFuture<>();
legacyAsyncClient.fetch(url, new Callback() {
    @Override public void onSuccess(String result) { cf.complete(result); }
    @Override public void onFailure(Exception e)   { cf.completeExceptionally(e); }
});
return cf;  // callers chain on this like any other CF
```

### Transform (1 → 1 mapping — the workhorse methods)

| Method | Signature | What It Does | Analogy | Frequency |
| :--- | :--- | :--- | :--- | :--- |
| `thenApply` | `Function<T, U>` | Transform result: `T → U` | `Stream.map()` | ✅ **Most used** |
| `thenAccept` | `Consumer<T>` | Consume result, return `CF<Void>` | `Stream.forEach()` | ✅ Very common |
| `thenRun` | `Runnable` | Run action after completion, ignore result | "do this when done" | ✅ Common |

```java
CompletableFuture.supplyAsync(() -> fetchUser(id))
    .thenApply(user -> user.getName().toUpperCase())   // transform: User → String
    .thenAccept(name -> log.info("Processed: {}", name)) // consume: terminal
    .thenRun(() -> metrics.incrementProcessed());         // side-effect: fire-and-forget
```

### Chain / FlatMap (dependent async steps)

| Method | Signature | What It Does | Analogy | Frequency |
| :--- | :--- | :--- | :--- | :--- |
| `thenCompose` | `Function<T, CF<U>>` | Chain dependent futures — **this is flatMap** | `Stream.flatMap()` | ✅ **Critical** |

**This is the #1 method people miss.** Without it you get `CF<CF<T>>` (nested futures):

```java
// ❌ WRONG — thenApply with an async function gives CF<CF<Order>>
CompletableFuture<CompletableFuture<List<Order>>> nested =
    fetchUser(id).thenApply(user -> fetchOrders(user.getId()));

// ✅ RIGHT — thenCompose flattens to CF<List<Order>>
CompletableFuture<List<Order>> flat =
    fetchUser(id).thenCompose(user -> fetchOrders(user.getId()));
```

**Rule of thumb:**
- If your lambda returns a **plain value** → use `thenApply` (map)
- If your lambda returns a **CompletableFuture** → use `thenCompose` (flatMap)

### Combine (independent parallel futures)

| Method | Signature | What It Does | Frequency |
| :--- | :--- | :--- | :--- |
| `thenCombine` | `CF<U>, BiFunction<T, U, V>` | Run 2 futures in parallel, combine results | ✅ Very common |
| `thenAcceptBoth` | `CF<U>, BiConsumer<T, U>` | Like `thenCombine` but returns `CF<Void>` | 🟡 Less common |
| `runAfterBoth` | `CF<?>, Runnable` | Run action when both complete, ignore results | 🟡 Rare |
| `allOf` | `CF<?>...` | Wait for ALL to complete (returns `CF<Void>`) | ✅ **Very common** |
| `anyOf` | `CF<?>...` | Complete when FIRST completes | ✅ Common (racing/timeout) |

```java
// thenCombine — two independent calls in parallel, combine when both done
CompletableFuture<String> userFuture    = supplyAsync(() -> fetchUser(id));
CompletableFuture<String> productFuture = supplyAsync(() -> fetchProduct(pid));

CompletableFuture<String> combined = userFuture.thenCombine(productFuture,
    (user, product) -> user + " bought " + product);

// allOf — fan-out N parallel tasks, collect results
List<CompletableFuture<Price>> priceFutures = suppliers.stream()
    .map(s -> supplyAsync(() -> s.getPrice(item)))
    .toList();

CompletableFuture<Void> all = CompletableFuture.allOf(
    priceFutures.toArray(new CompletableFuture[0]));

// allOf returns CF<Void>, so you extract results after join:
CompletableFuture<List<Price>> allPrices = all.thenApply(v ->
    priceFutures.stream()
        .map(CompletableFuture::join)  // safe here — all are already complete
        .toList());

// anyOf — first-wins pattern (e.g., race multiple mirrors, use fastest)
CompletableFuture<Object> fastest = CompletableFuture.anyOf(
    supplyAsync(() -> fetchFromMirror1()),
    supplyAsync(() -> fetchFromMirror2()),
    supplyAsync(() -> fetchFromMirror3()));
```

### Either (first-to-complete of two)

| Method | What It Does | Frequency |
| :--- | :--- | :--- |
| `applyToEither(CF, Function)` | Transform result of whichever completes first | 🟡 Niche |
| `acceptEither(CF, Consumer)` | Consume result of whichever completes first | 🟡 Niche |
| `runAfterEither(CF, Runnable)` | Run action when either completes | 🟡 Rare |

```java
// Race two sources — use whichever responds first
CompletableFuture<String> result = primaryDb.query(sql)
    .applyToEither(replicaDb.query(sql), Function.identity());
```

### Error Handling

| Method | Signature | What It Does | Analogy | Frequency |
| :--- | :--- | :--- | :--- | :--- |
| `exceptionally` | `Function<Throwable, T>` | Catch exception, return fallback | `catch` block | ✅ **Most used** |
| `handle` | `BiFunction<T, Throwable, U>` | Always called — result OR exception | `try-finally` | ✅ Common |
| `whenComplete` | `BiConsumer<T, Throwable>` | Side-effect (logging), doesn't transform result | Observer/listener | ✅ Common |
| `exceptionallyCompose` | `Function<Throwable, CF<T>>` | Catch + fallback to another async call (Java 12+) | `catch` with async retry | 🟡 Useful |

```java
// exceptionally — return a default on failure
CompletableFuture<String> safe = fetchData()
    .exceptionally(ex -> {
        log.warn("Fetch failed: {}", ex.getMessage());
        return "DEFAULT";
    });

// handle — unified processing (always called, result OR exception)
CompletableFuture<Response> response = callService()
    .handle((result, ex) -> {
        if (ex != null) {
            return Response.error(ex.getMessage());
        }
        return Response.ok(result);
    });

// whenComplete — observe without transforming (great for logging/metrics)
fetchUser(id)
    .whenComplete((user, ex) -> {
        if (ex != null) metrics.incrementFailure();
        else metrics.incrementSuccess();
    })
    .thenApply(user -> toDto(user));  // pipeline continues unchanged

// exceptionallyCompose — async fallback (Java 12+)
fetchFromPrimary()
    .exceptionallyCompose(ex -> fetchFromBackup());  // fallback is also async
```

### Async Variants — The `*Async` Twins

**Every** `then*` method has an `Async` version that forces execution on a different thread:

| Non-Async | Async | Async + Executor |
| :--- | :--- | :--- |
| `thenApply` | `thenApplyAsync` | `thenApplyAsync(fn, executor)` |
| `thenAccept` | `thenAcceptAsync` | `thenAcceptAsync(fn, executor)` |
| `thenRun` | `thenRunAsync` | `thenRunAsync(fn, executor)` |
| `thenCompose` | `thenComposeAsync` | `thenComposeAsync(fn, executor)` |
| `thenCombine` | `thenCombineAsync` | `thenCombineAsync(cf, fn, executor)` |
| `handle` | `handleAsync` | `handleAsync(fn, executor)` |
| `whenComplete` | `whenCompleteAsync` | `whenCompleteAsync(fn, executor)` |
| `exceptionally` | `exceptionallyAsync` | `exceptionallyAsync(fn, executor)` (Java 12+) |

**When to use `*Async`?**
- The **non-async** version runs on whatever thread completed the previous stage (often a ForkJoinPool worker). Cheap for lightweight transforms.
- Use **`*Async`** when the transformation is heavy (CPU-bound) or you need to control which thread pool runs it (e.g., I/O work on `boundedElastic`, compute on a dedicated pool).

```java
// Non-async: toUpperCase() is trivial, no need for a thread hop
cf.thenApply(s -> s.toUpperCase());

// Async: heavy processing, run on dedicated pool
cf.thenApplyAsync(data -> expensiveComputation(data), computePool);
```

### Timeout Support (Java 9+)

| Method | What It Does | Frequency |
| :--- | :--- | :--- |
| `orTimeout(long, TimeUnit)` | Fails with `TimeoutException` if not complete in time | ✅ Common |
| `completeOnTimeout(value, long, TimeUnit)` | Completes with default value if not done in time | ✅ Common |

```java
// Fail with TimeoutException after 5 seconds
CompletableFuture<String> result = callSlowService()
    .orTimeout(5, TimeUnit.SECONDS);

// Return a default instead of failing
CompletableFuture<String> result = callSlowService()
    .completeOnTimeout("CACHED_DEFAULT", 5, TimeUnit.SECONDS);
```

### Result Retrieval

| Method | Blocks? | What It Does | Frequency |
| :--- | :--- | :--- | :--- |
| `get()` | ✅ Blocks | Returns result or throws checked `ExecutionException` | 🟡 Avoid in async code |
| `get(timeout, unit)` | ✅ Blocks (with timeout) | Same + `TimeoutException` | 🟡 Acceptable in tests |
| `join()` | ✅ Blocks | Same as `get()` but throws unchecked `CompletionException` | ✅ Preferred over `get()` |
| `getNow(defaultValue)` | ❌ Non-blocking | Returns result if done, else default | 🟡 Polling |
| `resultNow()` | ❌ Non-blocking | Returns result if done, else throws (Java 19+) | 🟡 Niche |
| `exceptionNow()` | ❌ Non-blocking | Returns exception if failed (Java 19+) | 🟡 Niche |

**Rule:** In async pipelines, prefer `thenApply`/`thenAccept`/`subscribe` over `get()`/`join()`. Only use `join()` at the outermost boundary (e.g., `main()`, test assertions, or inside `allOf().thenApply()`).

### The "Top 8" — 95% of Production Usage

These 8 methods cover the vast majority of real-world `CompletableFuture` code:

| # | Method | Role | Mental Model |
| :--- | :--- | :--- | :--- |
| 1 | `supplyAsync` | Start an async task that returns a value | "kick off work" |
| 2 | `thenApply` | Transform the result | `map` |
| 3 | `thenCompose` | Chain dependent async calls | `flatMap` |
| 4 | `thenAccept` | Consume the final result | `forEach` |
| 5 | `exceptionally` | Error fallback | `catch` |
| 6 | `thenCombine` | Combine 2 parallel results | `zip` |
| 7 | `allOf` | Wait for N parallel results | `Promise.all()` |
| 8 | `handle` | Unified success/error processing | `try-finally` |

Everything else in this section is a variant of these 8.

---

## 4. The Successors: Beyond `CompletableFuture`

`CompletableFuture` solved a lot, but it still has pain points at scale:

- **Not truly reactive:** It still models a *single* eventual value. Streaming multiple values over time (e.g., a live feed, paginated DB results) is not its design goal.
- **Thread-per-task cost:** Each async stage still occupies a real OS thread. Under high concurrency this becomes expensive.
- **Backpressure:** No built-in way to tell a fast producer to slow down for a slow consumer.

These gaps pushed the Java ecosystem toward three major successors.

---

### 4.1 Java 9 `Flow` API — Reactive Streams in the JDK

Java 9 standardised the **Reactive Streams** specification directly in the JDK under `java.util.concurrent.Flow`. It is a set of four interfaces:

| Interface | Role |
| :--- | :--- |
| `Flow.Publisher<T>` | Produces items; accepts a `Subscriber` |
| `Flow.Subscriber<T>` | Receives items (`onNext`), errors (`onError`), and completion (`onComplete`) |
| `Flow.Subscription` | Controls the flow — `request(n)` (backpressure), `cancel()` |
| `Flow.Processor<T,R>` | Both a `Publisher` and a `Subscriber`; transforms the stream |

```java
// Minimal subscriber skeleton
Flow.Subscriber<String> sub = new Flow.Subscriber<>() {
    Flow.Subscription subscription;

    @Override public void onSubscribe(Flow.Subscription s) {
        this.subscription = s;
        s.request(1); // demand the first item (backpressure!)
    }
    @Override public void onNext(String item) {
        System.out.println("Got: " + item);
        subscription.request(1); // demand next
    }
    @Override public void onError(Throwable t)  { t.printStackTrace(); }
    @Override public void onComplete()           { System.out.println("Done"); }
};
```

> **Key insight:** `Flow` defines the *contracts*. You rarely implement it directly — you use a library like Project Reactor or RxJava that already implements it.

---

### 4.2 Project Reactor — `Mono<T>` and `Flux<T>` (Spring WebFlux)

Project Reactor is the reactive library powering **Spring WebFlux**. It implements `Flow` (Reactive Streams) and adds a rich operator library.

| Type | Represents |
| :--- | :--- |
| `Mono<T>` | 0 or 1 value — direct conceptual successor to `CompletableFuture<T>` |
| `Flux<T>` | 0 to N values over time — a stream |

```java
// Mono — replaces CompletableFuture for a single async result
Mono<String> orderMono = Mono.fromCallable(() -> fetchOrderFromDb(orderId))
    .subscribeOn(Schedulers.boundedElastic()) // run on I/O thread pool
    .map(order -> order.toUpperCase())
    .onErrorReturn("DEFAULT_ORDER");          // elegant error fallback

// Flux — streaming multiple results
Flux<String> events = Flux.interval(Duration.ofMillis(500))
    .map(tick -> "Event #" + tick)
    .take(10);                                // only 10 events

events.subscribe(System.out::println);
```

**Why `Mono` beats `CompletableFuture` in reactive stacks:**
- Fully non-blocking end-to-end (no thread blocking anywhere).
- Built-in backpressure support.
- Richer operator set (`flatMap`, `zip`, `merge`, `retry`, `timeout`, etc.).
- Integrates with Spring's reactive HTTP, R2DBC (reactive DB), reactive Redis, etc.

---

### 4.3 RxJava — `Single<T>`, `Observable<T>`, `Flowable<T>`

RxJava (ReactiveX for Java) predates Project Reactor and introduced the reactive model to the Java world. It is still widely used on Android and in non-Spring backends.

| RxJava Type | Equivalent |
| :--- | :--- |
| `Single<T>` | `Mono<T>` — exactly one value or error |
| `Maybe<T>` | `Mono<T>` that may be empty |
| `Observable<T>` | `Flux<T>` without backpressure |
| `Flowable<T>` | `Flux<T>` with backpressure (implements Reactive Streams) |
| `Completable` | `Mono<Void>` — completes or errors, no value |

```java
Single<String> result = Single.fromCallable(() -> fetchUser(id))
    .subscribeOn(Schedulers.io())
    .map(User::getName)
    .onErrorReturnItem("Unknown");

result.subscribe(
    name  -> System.out.println("User: " + name),
    error -> System.err.println("Error: " + error)
);
```

---

### 4.4 Project Loom — Virtual Threads & `StructuredTaskScope` (Java 21+)

Project Loom takes a *completely different philosophical approach*. Instead of eliminating threads via callbacks/reactive pipelines, it makes threads **so cheap** that you can spawn millions of them.

#### Virtual Threads (Java 21 — GA)

- A **virtual thread** is a lightweight thread managed by the JVM, not the OS.
- OS threads cost ~1 MB of stack and are limited to thousands. Virtual threads cost ~few KB and can scale to **millions**.
- You write **blocking, imperative code** — `future.get()`, `Thread.sleep()`, JDBC calls — and the JVM automatically unmounts the virtual thread from the OS carrier thread during a blocking operation.

```java
// Pre-Loom: thread-per-request is expensive → must use async/reactive
// Post-Loom: just block! The JVM handles the rest.
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> {
            String result = callExternalApi(); // blocking HTTP call — totally fine!
            System.out.println(result);
        });
    }
} // auto-shutdown (try-with-resources on ExecutorService, Java 19+)
```

#### `StructuredTaskScope` (Java 21 Preview → Java 23+ Finalisation)

`StructuredTaskScope` is the higher-level API built on virtual threads. It enforces **structured concurrency**: child tasks cannot outlive the scope that created them, preventing the "fire-and-forget" leak problem of raw `CompletableFuture.allOf`.

```java
// "ShutdownOnFailure" policy: if ANY subtask fails, cancel the rest.
try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
    Subtask<String> user    = scope.fork(() -> fetchUser(userId));
    Subtask<String> product = scope.fork(() -> fetchProduct(productId));

    scope.join();           // wait for all forks
    scope.throwIfFailed();  // propagate first failure as an exception

    // Both succeeded — safe to use results
    return new Response(user.get(), product.get());
}
```

**`ShutdownOnSuccess` policy** is the `anyOf` equivalent — completes as soon as the first subtask succeeds and cancels the rest.

---

### Summary: The Full Evolution

```
Java 1.0   Runnable       — fire and forget, no result
Java 5     Callable+Future — result retrieval, but blocking .get()
Java 8     CompletableFuture — composable, non-blocking pipelines
Java 9     Flow API        — standardised Reactive Streams contracts in JDK
(Library)  RxJava / Reactor — rich reactive operators, Flux/Mono, backpressure
Java 21    Virtual Threads + StructuredTaskScope — cheap threads, write blocking code again
```

| Dimension | `CompletableFuture` | `Mono`/`Flux` (Reactor) | Virtual Threads (Loom) |
| :--- | :--- | :--- | :--- |
| **Programming style** | Callback chaining | Declarative operators | Imperative blocking |
| **Value cardinality** | Single value | Single (`Mono`) or stream (`Flux`) | Any |
| **Backpressure** | No | Yes | N/A |
| **Thread usage** | Platform threads | Minimal — event loop | Virtual threads (cheap) |
| **Learning curve** | Medium | High | Low |
| **Best fit** | Simple async tasks | High-throughput reactive services | General-purpose server workloads |
