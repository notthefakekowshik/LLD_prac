# LLD Quick Journal (Hurry Mode)

> **Mode:** Hurry learning — no fixed phase order. Topics chosen based on what feels weak.
> **Writes:** All new entries go here. `JOURNAL.md` is read-only historical reference (structured sessions).
> **Rule:** AI agents must NOT auto-populate this file. Wait for the user to explicitly request a journal entry with the topics they covered that day.

---

| S.No | Date | Problem/System | Complexity | Pattern(s) | Key Architectural Insight |
|------|------|----------------|------------|------------|---------------------------|
| 1 | 2026-07-08 | Snake & Ladder | O(1) per turn | Strategy (×2), Facade, Factory, Inheritance/Polymorphism | Snakes and ladders are the *same* mechanic — a jump from a start cell to an end cell — so both extend one abstract `Jump`; the `Board` indexes jumps in a `Map<startCell, Jump>` for O(1) destination lookup and reads `getEnd()` polymorphically, never branching on snake-vs-ladder (no `instanceof`). Subclasses self-validate direction in their own constructors (Snake head>tail, Ladder bottom<top) — validation *after* `super()` avoids the overridable-method-in-constructor smell. Two Strategies keep the loop closed for modification: `Die` (roll source, seedable for determinism) and `MovePolicy` for the overshoot rule (`ExactLanding` = stay put vs `BounceBack` = reflect off the end) — a genuine two-impl case that justifies the abstraction rather than premature. `Board` is fail-fast: it rejects out-of-bounds jumps, duplicate start cells, and *chained* jumps (one ending where another begins) at construction, so the game loop trusts its inputs. Turn resolution is a clean pipeline: die → MovePolicy.computeTarget → Board.getDestination → win-check → rotate. |
| 2 | 2026-07-08 | Minesweeper | Marked for revision | Strategy, Facade, Factory, Rich Domain Model | Built but flagged for revision — felt implementation-heavy relative to the design effort. Plan to spend 1-2 more hours on a dedicated revision pass (re-derive lazy mine placement, BFS flood-fill, and O(1) win-count from the design rather than re-reading the code) before marking this problem complete. |
