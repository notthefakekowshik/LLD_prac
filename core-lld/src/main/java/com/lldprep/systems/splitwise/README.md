# Splitwise

Expense splitting system demonstrating **Strategy**, **Factory**, **Observer**, and **Repository** patterns with a graph-based debt simplification algorithm.

## Features

- **Register users** and organise them into groups
- **Add expenses** with three split types: Equal, Exact, Percentage
- **Real-time balances** — O(1) lookup per user pair via in-memory balance map
- **Settle up** — record a direct payment to reduce a balance; over-settlement is rejected
- **Debt simplification** — standard greedy minimum-cash-flow suggestion reduces noisy balances into fewer transactions
- **Group expenses** — scoped expenses with member validation
- **Thread-safe** — per ordered-pair locks prevent concurrent expense additions from corrupting balances
- **Money-safe** — `BigDecimal` scale 2; equal/percentage rounding remainder goes to final participant

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Strategy** | `SplitStrategy` → `EqualSplitStrategy`, `ExactSplitStrategy`, `PercentageSplitStrategy` | New split type = new class only. Zero changes to `Expense` or `ExpenseService`. |
| **Factory** | `SplitStrategyFactory.get(SplitType)` | Centralises strategy instantiation; callers never use `new ConcreteStrategy()` |
| **Observer** | `SplitwiseEventListener` implementations registered on `SplitwiseFacade` | Decouples post-expense/settlement reactions (audit log, notifications) |
| **Repository** | `UserRepository`, `GroupRepository`, `ExpenseRepository` | In-memory stores; swappable for DB |
| **Facade** | `SplitwiseFacade` | Single entry point hiding expense, balance, and split complexity |

## Quick Start

```bash
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.splitwise.demo.SplitwiseDemo" -pl core-lld
```

## Package Structure

```
com.lldprep.systems.splitwise/
├── model/
│   ├── User.java                  # id, name, email
│   ├── Group.java                 # id, name, List<User> members
│   ├── Expense.java               # amount, paidBy, splits, splitType, group
│   ├── Split.java                 # user + their share amount
│   └── Settlement.java            # direct payment between two users
├── model/enums/
│   └── SplitType.java             # EQUAL, EXACT, PERCENTAGE
├── policy/
│   ├── SplitStrategy.java         # interface
│   ├── EqualSplitStrategy.java
│   ├── ExactSplitStrategy.java
│   └── PercentageSplitStrategy.java
├── factory/
│   └── SplitStrategyFactory.java
├── service/
│   ├── SplitwiseFacade.java       # FACADE: single entry point
│   ├── BalanceService.java        # balance map + simplification algorithm
│   ├── ExpenseService.java        # expense creation + split orchestration
│   └── SplitwiseEventListener.java # OBSERVER: receives balance events
├── repository/
│   ├── UserRepository.java
│   ├── GroupRepository.java
│   ├── ExpenseRepository.java
│   └── SettlementRepository.java
├── exception/
│   ├── SplitwiseException.java
│   ├── UserNotFoundException.java
│   ├── GroupNotFoundException.java
│   ├── DuplicateEmailException.java
│   ├── SplitValidationException.java
│   ├── ExpenseValidationException.java
│   └── InvalidSettlementException.java
└── demo/
    └── SplitwiseDemo.java
```

## Core Algorithm — Debt Simplification

Given N users with a complex web of debts, the system computes a standard greedy settlement suggestion:

```
1. Compute net balance per user:
     net[user] = total owed to them − total they owe others

2. MaxHeap of creditors (net > 0), MinHeap of debtors (net < 0)

3. While both heaps non-empty:
     Pick largest creditor C and largest debtor D
     settlement = min(C.net, |D.net|)
     Record: D pays C `settlement`
     Reduce both by settlement; push back if non-zero

Complexity: O(n log n)
```

This is read-only. It returns suggested settlements; it does not mutate balances.

**Example:**
```
Before: Alice owed ₹600, Bob owes ₹300, Charlie owes ₹300
After simplification: Bob → Alice ₹300, Charlie → Alice ₹300
(2 transactions, down from however many expenses created these balances)
```

## Concurrency Model

Balance updates are the critical section. Two concurrent `addExpense` calls sharing the same user pair must not interleave.

**Approach:** One lock object per ordered user pair (consistent ordering by userId string prevents deadlock).

```java
// Lock key always uses lexicographically smaller userId first
String lockKey = userA.compareTo(userB) < 0 ? userA + ":" + userB : userB + ":" + userA;
synchronized(lockMap.computeIfAbsent(lockKey, k -> new Object())) {
    balances.get(debtor).merge(creditor, amount, BigDecimal::add);
}
```

**Why not lock the whole balance map?** That would serialise all expense additions across all user pairs — Alice adding an expense with Bob would block Charlie adding an expense with Dave, even though they share no state.

### Known Trade-offs

- Pair locks are retained for each unique user pair. This keeps monitor identity simple and safe for the interview implementation, but the map can grow with pair cardinality. Production would use striped locks or safe lock lifecycle management.
- Balance summaries and debt simplification use a weakly consistent snapshot of `ConcurrentHashMap`. Good enough for display/read-only suggestions; not a transactional settlement boundary.

## Demo Scenarios

1. **Equal split** — ₹900 dinner split 3 ways → ₹300 each
2. **Exact split** — ₹5000 hotel, Alice pays ₹2000, Bob pays ₹3000
3. **Percentage split** — ₹800 cab, 60% Alice, 40% Bob
4. **Balance summary** — Alice sees she is owed ₹300 by Bob and owes ₹200 to Charlie
5. **Settle up** — Bob pays Alice ₹300 → balance zeroes
6. **Group expense** — member validation: cannot add non-member to group expense
7. **Simplify debts** — read-only greedy suggestions
8. **Concurrent expense addition** — two threads add expenses involving same pair → balances correct

## Extending the System

| Curveball | Extension |
|-----------|----------|
| Ratio split (2:3:5) | New `RatioSplitStrategy implements SplitStrategy` — zero other changes |
| Multi-currency | `CurrencyConversionStrategy`; `Expense` gains `Currency` field |
| Notifications | `SplitwiseEventListener` implementations (email, push) |
| Expense categories | `Category` enum on `Expense`; new query on `ExpenseRepository` |
| Recurring expenses | `RecurringExpenseScheduler` wraps `addExpense` on a timer |

## Documentation

- `DESIGN_DICE.md` — Full D.I.C.E. workflow, class diagram, algorithm, concurrency model
- `SCHEMA.md` — ER diagram, 6 tables, balance derivation query, migration guide
- `API_CONTRACT.md` — REST endpoints, request/response schemas, validation rules, example flow
