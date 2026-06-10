# Concurrent Collections — Index

Read by family. Each file follows theory-note style: problem solved, quick choice, practical examples, internals that explain behavior, comparisons, traps, and interview Q&A.

```text
Concurrent Collections
├── Concurrent_Maps.md
│   ├── ConcurrentHashMap
│   └── ConcurrentSkipListMap
├── Concurrent_Sets.md
│   ├── ConcurrentHashMap.newKeySet()
│   ├── CopyOnWriteArraySet
│   └── ConcurrentSkipListSet
├── Concurrent_Lists.md
│   └── CopyOnWriteArrayList
└── Concurrent_Queues.md
    ├── ConcurrentLinkedQueue
    ├── ConcurrentLinkedDeque
    ├── ArrayBlockingQueue
    ├── LinkedBlockingQueue
    ├── PriorityBlockingQueue
    ├── DelayQueue
    ├── SynchronousQueue
    └── LinkedTransferQueue
```

## Fast Selection

| Need | Start With |
|---|---|
| key-value lookup, counters, cache | `Concurrent_Maps.md` |
| unique IDs, dedupe, membership | `Concurrent_Sets.md` |
| listener/callback lists | `Concurrent_Lists.md` |
| producer-consumer / handoff / back-pressure | `Concurrent_Queues.md` |

## Reading Order

1. `Concurrent_Collections_Index.md` — map of what exists.
2. Family file — choose map/set/list/queue based on problem.
3. `Collections_Theory.md` — older consolidated theory and Q&A.
4. `executors/BlockingQueue_Theory.md` — deeper queue-specific theory.

## One-Line Mental Model

Pick concurrent collection by access pattern: exact lookup, sorted lookup, rare-write iteration, non-blocking handoff, or blocking back-pressure.
