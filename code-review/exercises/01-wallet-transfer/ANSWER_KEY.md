# Answer Key — 01 Wallet Transfer  🔒 GRADER ONLY

Do not show this to the candidate before they submit. Findings are the ground
truth for grading. Credit any valid finding the candidate raises even if worded
differently; credit valid findings **not** listed here (note them as "bonus").

Total seeded issues: **13 real + 2 decoys.** Points in parentheses feed the
rubric coverage score.

## Critical (must-find — missing these caps the band at "No Hire" / "Lean")

| # | File · location | Category | Issue | Expected fix |
|---|-----------------|----------|-------|--------------|
| C1 | `TransferService.transfer` (check-then-act on balance) | Concurrency / Data integrity | Read-modify-write on balance with **no locking / atomicity**. Two concurrent transfers from the same wallet both pass the `>=` check and overdraw the account (lost-update race). | Row-level DB lock (`SELECT ... FOR UPDATE`) or optimistic version/CAS, or single-writer per account. In-memory `setBalance` on detached objects also races. |
| C2 | `TransferService.transfer` (the two `save` calls) | Data integrity / Correctness | The debit and credit are **not in a transaction**. If the second `save` (or the process) fails after the first, money is destroyed or duplicated. | Wrap both writes in a single DB transaction; commit/rollback atomically. |
| C3 | `WalletRepository.findById` / `save` | Security | **SQL injection** — `id` and `balance` are concatenated straight into SQL. `id = "x' OR '1'='1"` etc. | Use `PreparedStatement` with bind parameters. |
| C4 | `Wallet.balance` is `double` (and all money math) | Correctness / Money | **Floating-point money.** `double` loses cents (e.g. 0.1 + 0.2). Balances and comparisons drift. | Use `BigDecimal` (or integer minor units / `long` cents) with defined rounding. |

## Major

| # | File · location | Category | Issue | Expected fix |
|---|-----------------|----------|-------|--------------|
| M1 | `TransferService.transfer` (catch block) | Error handling | **Swallowed exception** — `catch (Exception e) { return false; }` hides SQL/NPE failures; caller can't tell "insufficient funds" from "DB down". | Let it propagate or map to typed results/exceptions; at minimum log with stack trace. |
| M2 | `TransferService.transfer` (after `findById`) | Correctness | **No null check** on `from`/`to`. Unknown wallet id → `findById` returns `null` → NPE → swallowed → generic `FAILED`. | Validate both exist; return a clear "wallet not found" error. |
| M3 | `TransferService.transfer` (top) | Validation | **No amount validation.** Negative or zero amount transfers "succeed": a negative amount lets you *pull* money from the payee. | Reject `amount <= 0` before any work. |
| M4 | `TransferController.handle` (`Double.parseDouble`, `params.get`) | Validation / Error handling | Unvalidated input: `parseDouble` throws `NumberFormatException` on bad/missing `amount`; missing `from`/`to` → nulls flow downstream. No 400-style handling. | Validate presence + parse safely; return a client error. |
| M5 | `TransferService` responsibilities | Design / SRP | Service mixes orchestration, business rules, persistence sequencing, and the audit-threshold policy. Hard to test; audit rule is buried. | Extract transaction boundary; make the "large transfer" threshold a policy/config. |

## Minor

| # | File · location | Category | Issue | Expected fix |
|---|-----------------|----------|-------|--------------|
| N1 | `TransferService.transfer` (`> 10000`) | Readability | **Magic number** for the audit threshold. | Named constant / config. |
| N2 | `AuditLogger.log` call | Security / Privacy | Audit line concatenates owner name + note (potential PII / injectable log content) and writes to **stdout only** — not durable for "compliance." | Structured, persisted audit sink; avoid logging sensitive free-text. |
| N3 | `Wallet` | Correctness (contract) | `equals` overridden **without `hashCode`** — breaks `HashMap`/`HashSet` usage. | Override both, keyed on `id`. |
| N4 | `TransferController.handle` return | API design | `"OK"`/`"FAILED"` strings collapse all failure reasons into one — caller can't react. | Typed response / status codes. |
| N5 | `TransferService.transfer` | Correctness (edge) | **Self-transfer** (`fromId == toId`) not handled; combined with double math can misreport. | Reject or short-circuit self-transfers. |

## Decoys (raising these as blocking = judgment penalty; fine as a nit at most)

| D | File · location | Why it's NOT a real defect |
|---|-----------------|----------------------------|
| DEC1 | `Wallet` uses field access + getters | Standard POJO; not a bug. Flagging "anemic model, add behavior" is a *weak* comment here — the entity legitimately holds no invariant of its own; the transfer invariant belongs to the transaction boundary. |
| DEC2 | `System.out.println` inside `AuditLogger` alone | The real issue is durability/PII (N2), not "println is bad style." A candidate who only says "don't use println" caught the symptom, not the cause — half credit. |

## Grading notes

- **Band gate:** all four Criticals (C1–C4) found + solid communication → *Strong
  Hire*. Miss C1 or C2 (the money-safety pair) → cap at *Lean Hire* no matter how
  many minors they list. Miss C3 → cap at *Hire* (still shipped an injectable
  endpoint).
- Reward candidates who **prioritize** (call C1/C2/C3/C4 blocking, N* as nits) and
  who **explain impact** ("this overdraws under concurrency", not "add a lock").
- Penalize per the Judgment dimension for chasing decoys / stylistic nits while
  missing a Critical.
