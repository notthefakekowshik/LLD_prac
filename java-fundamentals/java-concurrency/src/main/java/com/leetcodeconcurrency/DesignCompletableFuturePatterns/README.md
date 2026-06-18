# DesignCompletableFuturePatterns

Comprehensive CompletableFuture patterns for SDE2/SDE2+ concurrency interviews.

## Files

| File | Covers |
|------|--------|
| `BasicChaining.java` | thenApply, thenCompose, thenAccept, thenRun + async variants, thenCombine |
| `CombiningPatterns.java` | allOf, anyOf, fan-out/fan-in, partial failure with fallback |
| `ErrorHandlingPatterns.java` | exceptionally, handle, whenComplete, chained fallback, retry with backoff |
| `TimeoutAndCancellationPatterns.java` | orTimeout, completeOnTimeout, cancel, legacy timeout trick, structured shutdown |
| `ParallelServiceAggregation.java` | Real-world API gateway, time-bounded aggregation, graceful degradation, batch processing, data pipeline |
| `DemoRunner.java` | Drives all demos in sequence |

## Key Interview Distinctions

### thenApply vs thenCompose vs thenCombine
- **thenApply(Function<T,U>)** — synchronous map. Returns `CF<U>`.
- **thenCompose(Function<T,CF<U>>)** — async flatMap. Flattens nested CF. Returns `CF<U>`.
- **thenCombine(CF<U>, BiFunction<T,U,V>)** — combine results of TWO independent futures.

### exceptionally vs handle vs whenComplete
- **exceptionally** — failure only, takes `Throwable`, returns fallback **value**.
- **handle** — always called, takes `(result, throwable)`, returns **value**. Can transform both paths.
- **whenComplete** — always called, `BiConsumer`, **cannot change result**. Side-effect only (logging/cleanup).

### allOf vs anyOf
- **allOf** — returns `CF<Void>`. Must `.thenApply(v -> f1.join() + f2.join())` to extract results.
- **anyOf** — returns `CF<Object>`. First to complete wins. Others keep running.

## Compilation & Run

```bash
cd java-fundamentals/java-concurrency
mvn compile exec:java -pl . \
  -Dexec.mainClass=com.leetcodeconcurrency.DesignCompletableFuturePatterns.DemoRunner
```

Or run individual demos via their own `main()` methods.

## Common Interview Pitfalls

1. **Blocking ForkJoinPool.commonPool()** — use custom `Executor` for I/O tasks.
2. **Forgetting that `allOf` returns `Void`** — results must be extracted manually.
3. **Swallowing `InterruptedException`** — always `Thread.currentThread().interrupt()` to preserve the flag.
4. **`join()` vs `get()`** — `join()` throws unchecked `CompletionException`, `get()` throws checked `ExecutionException`.
