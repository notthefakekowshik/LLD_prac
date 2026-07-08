# Snake & Ladder

A turn-based Snake & Ladder engine with a **unified jump abstraction**, a **pluggable die**, a
**pluggable overshoot rule**, and **pluggable turn rules** — all validated up front so the game loop
trusts its board.

## Quick Start

```java
Board board = BoardFactory.standard();                       // classic 100-cell board
SnakeAndLadderGame game = new SnakeAndLadderGame(
    board,
    new StandardDie(),                                       // fair 6-sided die
    new ExactLandingMovePolicy(),                            // must land exactly on 100
    new SimpleTurnPolicy(),                                  // (or ExtraTurnOnMaxRollPolicy for the six-rule)
    List.of(new Player("p1", "Alice"), new Player("p2", "Bob")));

Player winner = game.play();                                 // run to completion
System.out.println(winner.getName() + " wins!");

// …or step turn-by-turn:
while (!game.isFinished()) {
    TurnResult t = game.playTurn();
    System.out.println(t.playerName() + " rolled " + t.diceRoll()
        + " : " + t.fromPosition() + " -> " + t.toPosition());
}
```

## Core Model: one "Jump", two directions

Snakes and ladders are the **same mechanic** — a teleport from a start cell to an end cell — so both
extend a single `Jump`. The board indexes jumps by start cell and reads `getEnd()` polymorphically;
it never asks "snake or ladder?". Each subclass self-validates its direction:

| Kind | Rule | Constructor |
|------|------|-------------|
| `Ladder` | end (top) **above** start (bottom) | `new Ladder(4, 14)` |
| `Snake`  | end (tail) **below** start (head)  | `new Snake(17, 7)` |

Adding a new jump kind (teleporter, wormhole) needs **zero** changes to `Board` or the game loop.

## Pluggable Strategies

| Strategy | Impls | Purpose |
|----------|-------|---------|
| `Die` | `StandardDie` (seedable, n-faced) | Roll source — a **pure randomizer** (`roll()` + `maxValue()`); swap in a loaded or multi-die |
| `MovePolicy` | `ExactLandingMovePolicy`, `BounceBackMovePolicy` | What happens when a roll overshoots the final cell |
| `TurnPolicy` | `SimpleTurnPolicy`, `ExtraTurnOnMaxRollPolicy` | Turn rules — extra turn / forfeit. A **pure function**: the game owns the streak state and passes it in |

**Overshoot on a size-100 board, sitting at 98, rolling 5:**
- `ExactLandingMovePolicy` → stays at 98 (no exact finish, turn forfeited)
- `BounceBackMovePolicy` → 100 then reflects back to 97

**Turn rules (why `TurnPolicy`, not the `Die`):** "extra turn on a six, three-in-a-row forfeits" is a
*turn rule*, not a die property — a physical die has no memory. So it lives in `TurnPolicy`, which
returns an explicit `TurnDecision` (`applyMove` / `grantsExtraTurn` / `nextConsecutiveBonus`) rather
than smuggling a signal through the roll value. `ExtraTurnOnMaxRollPolicy` keys off `die.maxValue()`,
so it works for any die size — no hard-coded 6.

## Turn Resolution (per `playTurn`)

1. Current player rolls the `Die`.
2. `TurnPolicy.decide(roll, dieMax, streak)` → a `TurnDecision` (move? extra turn? new streak?).
3. If it applies a move: `MovePolicy.computeTarget(...)` → landing cell, then `Board.getDestination(landing)` applies any snake/ladder (O(1) map lookup).
4. If the new cell is the final cell → status `FINISHED`, winner recorded.
5. **One** decision point advances to the next player — unless the policy granted an extra turn.

## Board Validation (fail-fast at construction)

`Board` rejects, via `InvalidBoardException`:
- jumps out of `[1, size]`,
- a jump starting on the winning cell,
- two jumps sharing a start cell,
- **chained** jumps (one jump ending where another begins),
- and each `Snake`/`Ladder` rejects the wrong direction in its own constructor.

## Thread Safety

A game is a single turn-based session, so `SnakeAndLadderGame` guards each turn with a coarse
per-game lock (`synchronized`) — a roll, move, jump, and win-check apply as one atomic unit, and a
turn after `FINISHED` throws `InvalidGameStateException`.

## Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy (×3)** | `Die`, `MovePolicy`, `TurnPolicy` | Swap roll source / overshoot rule / turn rule without touching the loop — the real extension seams (≥2 impls each) |
| **Facade** | `SnakeAndLadderGame` (concrete class) | One API hides dice, jumps, turn ordering. No interface — Facade is a role, and with one engine a one-impl interface would be premature |
| **Factory** | `BoardFactory` | Canonical layout in one place + custom builder |
| **Inheritance + Polymorphism** | `Jump` → `Snake`/`Ladder` | Board reads `getEnd()`, never branches on type |

## Demo

`SnakeAndLadderDemo.main()` covers all 9 functional requirements — a seeded turn-by-turn
playthrough (showing jumps and overshoot-stays), the exact-vs-bounce overshoot swap, all five
board-validation rejections, and the move-after-game-over guard.

```
cd core-lld/src/main/java
javac -d /tmp/snl $(find com/lldprep/systems/snakeandladder -name "*.java")
java -cp /tmp/snl com.lldprep.systems.snakeandladder.demo.SnakeAndLadderDemo
```
