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
