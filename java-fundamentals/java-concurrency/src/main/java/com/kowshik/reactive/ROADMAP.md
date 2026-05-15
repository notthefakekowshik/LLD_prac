# Reactive Programming & Project Reactor — Learning Roadmap

> **Goal**: Master reactive programming from the underlying spec to production-grade Spring WebFlux applications.
> **Prerequisite**: Complete `CompletableFuture` and Virtual Threads sections first (see Section 5.1 in theory doc).

---

## Phase 1: Foundations — COMPLETE BEFORE STARTING

**Prerequisites from java-concurrency module:**
- [x] Thread fundamentals, JMM, `volatile`, happens-before
- [x] `synchronized`, `ReentrantLock`, `ReadWriteLock`
- [x] Executors, thread pools, `ForkJoinPool`
- [x] `CompletableFuture` — chaining, composition, error handling
- [x] Virtual Threads (Java 21+) — understand how they change the reactive vs blocking decision

**Key Decision Framework** (see theory doc Section 5.1):
- Need streaming 0..N with backpressure? → Reactive
- Simple async 0..1 on Java 21+ with blocking libs? → Virtual Threads
- Complex stream operators (buffer, window, retry)? → Reactive

---

## Phase 2: Reactive Streams Specification (The Protocol)

> **Objective**: Understand the 4 interfaces. Build toy implementations to internalize the backpressure protocol.

### Topics
- [ ] **The 4 Interfaces** — `Publisher<T>`, `Subscriber<T>`, `Subscription`, `Processor<T,R>`
- [ ] **Signal Protocol** — `onSubscribe` → `request(n)` → `onNext` × N → (`onComplete` | `onError`)
- [ ] **Backpressure Mechanics** — consumer-driven flow control via `request(n)`
- [ ] **Spec Rules** — [reactive-streams.org](https://www.reactive-streams.org/)

### Deliverables (Code to Write)
- [ ] **ToyPublisher.java** — `Publisher<String>` emitting items from list with delay
- [ ] **ToySubscriber.java** — `Subscriber<String>` with configurable `request(n)` batch size
- [ ] **BackpressureDemo.java** — fast producer (1000/s), slow consumer (100/s), bounded memory
- [ ] **TransformProcessor.java** — `Processor<String, Integer>` mapping length

---

## Phase 3: Project Reactor Core — Mono & Flux

> **Objective**: Master Reactor library. Lazy evaluation, operators, threading.

### 3.1 Mono (0..1 items)
- [ ] **Creation** — `Mono.just()`, `Mono.empty()`, `Mono.error()`, `Mono.fromCallable()`, `Mono.fromFuture()`, `Mono.defer()`
- [ ] **Laziness vs Eagerness** — compare to `CompletableFuture`
- [ ] **Operators** — `map`, `flatMap`, `filter`, `defaultIfEmpty`, `switchIfEmpty`
- [ ] **Terminal** — `subscribe()`, `block()` (testing only), `toFuture()`

### 3.2 Flux (0..N items)
- [ ] **Creation** — `Flux.just()`, `Flux.fromIterable()`, `Flux.range()`, `Flux.interval()`, `Flux.generate()`
- [ ] **Transformation** — `map`, `flatMap`, `concatMap` (ordered), `filter`, `distinct`, `groupBy`
- [ ] **Reduction** — `reduce`, `collectList`, `collectMap`, `count`, `all`/`any`
- [ ] **Windowing** — `buffer(10)`, `bufferTimeout()`, `window(Duration)`

### 3.3 Combining Streams
- [ ] **Static** — `zip`, `merge`, `concat`
- [ ] **Dynamic** — `combineLatest`, `switchMap`, `flatMapSequential`

### 3.4 Error Handling
- [ ] **Recovery** — `onErrorResume`, `onErrorReturn`, `onErrorMap`
- [ ] **Retry** — `retry()`, `retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))`

### 3.5 Schedulers & Threading
- [ ] **Types** — `parallel()` (CPU), `boundedElastic()` (I/O), `single()`, `immediate()`
- [ ] **Operators** — `publishOn` (downstream), `subscribeOn` (upstream)
- [ ] **Non-blocking rule** — never block on reactive threads

### Deliverables
- [ ] **MonoBasicsDemo.java** — all creation methods, demonstrate laziness
- [ ] **FluxOperatorsDemo.java** — map, flatMap, filter with marble comments
- [ ] **CombiningStreamsDemo.java** — zip, merge, concat with timing
- [ ] **ErrorHandlingDemo.java** — retry with exponential backoff
- [ ] **SchedulerComparisonDemo.java** — `publishOn` vs `subscribeOn`

---

## Phase 4: Hot vs Cold Publishers

> **Objective**: Understand replay vs live emission. Critical for shared streams.

### Topics
- [ ] **Cold Publisher** — fresh stream per subscriber (Netflix model)
- [ ] **Hot Publisher** — emits regardless of subscribers (live TV model)
- [ ] **Making Cold Hot** — `share()`, `replay()`, `ConnectableFlux`
- [ ] **Sinks** — programmatic emission with `Sinks.many()` / `Sinks.one()`

### Deliverables
- [ ] **ColdVsHotDemo.java** — show multiple subscribers getting different/same data
- [ ] **SharedFluxDemo.java** — `share()` to convert cold to hot
- [ ] **SinkEmitterDemo.java** — `Sinks.many().multicast()` for event bus pattern

---

## Phase 5: Testing & Debugging

> **Objective**: Learn tools to test and debug reactive code. Stack traces are painful without these.

### Topics
- [ ] **StepVerifier** — test reactive sequences with time control
- [ ] **log() operator** — trace all signals through pipeline
- [ ] **checkpoint()** — add labels for debugging
- [ ] **Hooks.onOperatorDebug()** — enhanced stack traces (dev only, expensive)

### Deliverables
- [ ] **StepVerifierTest.java** — test Mono/Flux with `expectNext`, `expectComplete`, `verify`
- [ ] **VirtualTimeTest.java** — `StepVerifier.withVirtualTime()` for `Flux.interval()`
- [ ] **DebuggingDemo.java** — `log()`, `checkpoint("after-db-call")`

---

## Phase 6: Spring WebFlux Integration

> **Objective**: Build reactive web applications with Spring WebFlux.

### Topics
- [ ] **WebFlux Controllers** — `@RestController` returning `Mono<T>` / `Flux<T>`
- [ ] **WebClient** — reactive HTTP client (replaces `RestTemplate`)
- [ ] **Functional Endpoints** — `RouterFunction` + `HandlerFunction`
- [ ] **Server-Sent Events** — `Flux<ServerSentEvent<T>>` for real-time streaming
- [ ] **R2DBC** — reactive database access (replaces JDBC)

### Deliverables
- [ ] **ReactiveUserController.java** — CRUD with `Mono<ResponseEntity<User>>`
- [ ] **WebClientDemo.java** — async HTTP calls with retry
- [ ] **FunctionalRouter.java** — `RouterFunction` style routes
- [ ] **SSEStockPriceController.java** — `Flux<ServerSentEvent<StockPrice>>`

---

## Phase 7: Advanced Patterns & Production

> **Objective**: Handle real-world complexity — backpressure, caching, context.

### Topics
- [ ] **Backpressure Strategies** — `onBackpressureBuffer`, `onBackpressureDrop`, `onBackpressureLatest`
- [ ] **Context Propagation** — `contextWrite()`, `deferContextual()` for request-scoped data (MDC equivalent)
- [ ] **Reactive Caching** — `cache()`, `CacheMono`, `CacheFlux`
- [ ] **Retry Patterns** — exponential backoff, jitter, circuit breaker style
- [ ] **reactor-kafka** — reactive Kafka consumer/producer
- [ ] **RSocket** — reactive messaging protocol

### Deliverables
- [ ] **BackpressureStrategiesDemo.java** — buffer vs drop vs latest
- [ ] **ContextPropagationDemo.java** — pass requestId through chain
- [ ] **ReactiveCacheDemo.java** — `cache()` operator, TTL patterns
- [ ] **RetryCircuitBreakerDemo.java** — backoff with max attempts
- [ ] **KafkaReactiveConsumer.java** — `KafkaReceiver` with backpressure

---

## Phase 8: Build a Complete System

> **Capstone**: Build a production-grade reactive microservice demonstrating all concepts.

### Project: Reactive Order Processing Service

**Requirements**:
- [ ] Accept orders via REST endpoint (WebFlux)
- [ ] Validate asynchronously with external service (WebClient)
- [ ] Enqueue to Kafka (reactive producer)
- [ ] Consume from Kafka with backpressure (reactive consumer)
- [ ] Process and store to DB (R2DBC)
- [ ] Stream real-time status via SSE to clients
- [ ] Implement retry with exponential backoff
- [ ] Add context propagation for tracing

**Patterns to Apply**:
- Mono/Flux chains with proper error handling
- Hot publishers for SSE streaming
- Backpressure on Kafka consumer
- Context for distributed tracing
- Testing with StepVerifier

---

## File Naming Conventions

- `*Demo.java` — Runnable demonstrations
- `*Test.java` — StepVerifier unit tests
- `*Controller.java` — WebFlux REST controllers
- `*Router.java` — Functional endpoint routers

---

## Maven Dependencies

```xml
<dependencies>
    <!-- Reactor Core -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-core</artifactId>
    </dependency>
    
    <!-- Testing -->
    <dependency>
        <groupId>io.projectreactor</groupId>
        <artifactId>reactor-test</artifactId>
        <scope>test</scope>
    </dependency>
    
    <!-- Spring WebFlux (Phase 6+) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
    
    <!-- R2DBC (Phase 6+) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-r2dbc</artifactId>
    </dependency>
    
    <!-- Kafka (Phase 7+) -->
    <dependency>
        <groupId>io.projectreactor.kafka</groupId>
        <artifactId>reactor-kafka</artifactId>
    </dependency>
</dependencies>
```

---

## Progress Tracking

| Phase | Topic | Status | Notes |
|-------|-------|--------|-------|
| 1 | Prerequisites | ⬜ / ✅ | Complete before starting |
| 2 | Reactive Streams Spec | ⬜ | Build toy implementations |
| 3.1 | Mono Basics | ⬜ | Creation, laziness |
| 3.2 | Flux Basics | ⬜ | Operators, transformation |
| 3.3 | Combining Streams | ⬜ | zip, merge, concat |
| 3.4 | Error Handling | ⬜ | retry, onErrorResume |
| 3.5 | Schedulers | ⬜ | publishOn, subscribeOn |
| 4 | Hot vs Cold | ⬜ | share(), Sinks |
| 5 | Testing | ⬜ | StepVerifier, debugging |
| 6 | Spring WebFlux | ⬜ | Controllers, WebClient, SSE |
| 7 | Advanced | ⬜ | Backpressure, context, caching |
| 8 | Capstone Project | ⬜ | Reactive order service |

---

## Key Decision Checklist

**When to use Reactive vs Virtual Threads:**
- [ ] Streaming 0..N items continuously? → Reactive Flux
- [ ] Need backpressure (fast producer/slow consumer)? → Reactive
- [ ] Complex operators (buffer, window, throttle)? → Reactive
- [ ] Java 21+ with blocking libraries (JDBC)? → Virtual Threads
- [ ] Simple 0..1 async result? → Either works

**Do NOT use Reactive when:**
- [ ] CPU-bound tasks → Use parallel streams / ForkJoinPool
- [ ] Team lacks reactive experience → Debugging cost is real
- [ ] Simple CRUD with low concurrency → Spring MVC + CompletableFuture
- [ ] Using JDBC without R2DBC → Traditional JDBC is blocking
