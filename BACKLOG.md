# Backlog

Topics that surfaced during design sessions but couldn't be actioned at the time.
Pick these up between implementation sprints.

> **Note:** Implementation gaps (missing systems, patterns, Java demos) are tracked in
> [GAPS_TRACKER.md](./GAPS_TRACKER.md). BACKLOG is for meta/quality/documentation gaps
> that don't fit the per-item checklist format.

---

## 🔴 Meta — Stale Tracker Files

| # | Issue | Impact | Status |
|---|-------|--------|--------|
| 1 | **CLAUDE.md roadmap section** — Shows `[ ]` for Phase 1 SOLID exercises, Phase 3 building blocks (ThreadPool, RateLimiter, Logging, TaskScheduler), and Phase 4 systems (ParkingLot, MovieBooking, VendingMachine, ATM) — all completed in core-lld. New AI sessions get wrong completion state. | High — fresh sessions start with incorrect context | Done ✅ 2026-06-08 |
| 2 | **PATTERNS.md Iterator + Chain of Responsibility** — Both show `[ ] Implemented` but have full good/bad examples in `core-lld/src/.../foundations/behavioral/`. Iterator: in-order/pre-order/post-order with BinaryTree. CoR: ConsoleHandler → FileHandler → EmailAlertHandler. | Medium — documentation says "missing" when code exists | Done ✅ — already `[x]` in PATTERNS.md |
| 3 | **ROADMAP.md Phase 4** — Splitwise, Chess, SnakeLadder, Hotel, Library correctly show `[ ]` (genuinely unimplemented). Need to verify no other stale checkmarks exist. | Low — appears accurate after last sync | Not Started |

---

## 🟡 Quality — Missing / Incomplete Documentation

| # | Gap | Why It Matters | Status |
|---|-----|---------------|--------|
| 4 | **No init.md exists** — Unlike HLD's `scenarios/init.md`, the LLD repo has no directory-level context file. `CLAUDE.md` covers the whole repo, but content-heavy subdirectories (`core-lld/`, `java-fundamentals/`) have no bootstrap docs for fresh AI sessions. | Medium — taste preference for content-heavy directories | Not Started |
| 5 | **No test files for LLD systems** — Every system has a `Main` demo class but zero unit tests. Systems like OrderBook (concurrent matching), MovieBooking (seat locking), and RateLimiter (algorithm correctness) would benefit from targeted tests. | Medium — catching regressions when adding curveballs | Not Started |
| 6 | **Concurrency correctness proofs** — Only MovieBooking DESIGN.md explicitly documents its concurrency model. OrderBook uses thread confinement but doesn't explain *why* it's safe. ThreadPool's executor has subtle correctness properties. | Medium — interviewers probe "how do you know this is thread-safe?" | Not Started |
| 7 | **Performance benchmarking framework** — Concurrent data structures (CopyOnWriteArrayList, ConcurrentHashMap) and LLD systems (OrderBook matching throughput) have no benchmark tooling. Theory files mention "better throughput" but no numbers. | Low — SDE-3 differentiator | Not Started |

---

## 🟢 Nice to Have — Content Expansions

| # | Gap | Why It Matters | Status |
|---|-----|---------------|--------|
| 8 | **Design Pattern Composition** — Each pattern is implemented in isolation (good/bad). No file demonstrates composing 3+ patterns together (e.g., Facade + Strategy + Observer in one system). Real interviews require this. | Medium — interview realism | Not Started |
| 9 | **Interview Walkthrough Videos/Scripts** — No "think aloud" examples of solving a machine coding problem in ~60 min under interview pressure. The DESIGN.md files are post-hoc, not real-time. | Low — soft skill, not technical gap | Not Started |
| 10 | **Common Interview Pitfalls** — Reverse reference: what NOT to do when the interviewer says "design a parking lot." No anti-pattern catalog specific to LLD machine coding rounds. | Medium — avoiding known traps | Not Started |
| 11 | **Curveball Swarm** — Some systems have single curveball scenarios. No multi-curveball composition (e.g., "add EV charging to Parking Lot AND add dynamic pricing AND add reservation system" all at once). | Low — stress-testing OCP | Not Started |
| 12 | **my_practice/ directory** — 14 empty stub directories created with `.gitkeep`. These were intended for personal practice re-implementations of completed systems. Either populate or remove. | Low — repo cleanliness | Not Started |

---

## Quick Summary

| Priority | Count | What |
|----------|-------|------|
| 🔴 Meta | 1 | Stale ROADMAP.md Phase 4 checkmarks (verify no other staleness) |
| 🟡 Quality | 4 | Missing docs, no tests, no concurrency proofs |
| 🟢 Nice to have | 5 | Pattern composition, interview pitfalls, practice stubs |

> **Implementation gaps** (Splitwise, Chess, Mediator, Sealed Classes, Livelock, Reactive, etc.) →
> tracked in [GAPS_TRACKER.md](./GAPS_TRACKER.md). Do NOT duplicate here.

---

## 🔵 Documentation Gaps in PATTERNS.md

| # | Gap | Why It Matters | Status |
|---|-----|---------------|--------|
| 13 | **Memento — implemented but undocumented** — `foundations/behavioral/memento/` has good/bad examples (TextEditor, EditorMemento, EditorHistory) but PATTERNS.md has no section. Only appears in GoF overview diagram. | Medium — code exists, catalog is incomplete | Not Started |
