# Minesweeper

A classic Minesweeper engine — configurable board, first-click-safe mine placement, recursive
flood-fill reveal, flagging, and win/loss detection, with a **pluggable mine-placement strategy**.

## Quick Start

```java
// Build a board (Beginner preset: 9x9, 10 mines) with first-click-safe placement
Board board = BoardFactory.fromDifficulty(
    Difficulty.BEGINNER, new SafeFirstClickMinePlacementStrategy());
MinesweeperGame game = new MinesweeperGame(board);

// Reveal — mines are scattered lazily on this first click, away from it
MoveResult r = game.reveal(0, 0);   // flood-fills the empty region

// Flag a suspected mine (flagged cells cannot be revealed)
game.toggleFlag(4, 4);

// Read status and render
System.out.println(game.getStatus());       // IN_PROGRESS
System.out.println(board.render());
```

## Board Display

| Char | Meaning |
|------|---------|
| `.`  | Hidden (covered) |
| `F`  | Flagged |
| `_`  | Revealed, 0 adjacent mines |
| `1`–`8` | Revealed, N adjacent mines |
| `*`  | Revealed mine (shown after a loss) |

## Difficulty Presets

| Preset | Rows × Cols | Mines |
|--------|-------------|-------|
| BEGINNER | 9 × 9 | 10 |
| INTERMEDIATE | 16 × 16 | 40 |
| EXPERT | 16 × 30 | 99 |

Or build any board with `BoardFactory.custom(rows, cols, mines, strategy)` (requires `0 < mines < rows×cols`).

## How Reveal Works

1. **Lazy placement** — mines are scattered on the *first* reveal, so the strategy can keep the
   opening click safe. Adjacency counts are computed once, immediately after placement.
2. **Flood-fill** — revealing a cell with **0 adjacent mines** cascades (BFS) to its neighbours,
   stopping at cells that border a mine. Flagged cells are never auto-revealed.
3. **Loss** — revealing a mine flips status to `LOST` and reveals all mines for display.
4. **Win** — an O(1) `revealedSafeCells` counter; the game is `WON` the moment it equals
   `rows×cols − mines`.

## Mine Placement Strategies

| Strategy | First click safe? | Use |
|----------|-------------------|-----|
| `RandomMinePlacementStrategy` | No — can lose on click 1 | Classic hard mode; deterministic with a seeded `Random` |
| `SafeFirstClickMinePlacementStrategy` | Yes — click + 8 neighbours kept clear | Modern default |

Both accept an injectable `Random` so tests/demos can pin a seed. Swapping strategies is a
one-line change at build time — **the board never changes** (Strategy + DIP).

## Thread Safety

| Operation | Mechanism |
|-----------|-----------|
| `reveal` / `toggleFlag` | `synchronized` on the game — a move and its win/loss check are one atomic unit |
| Move after game over | `GameOverException` — status guarded inside the lock |

A game is a single interactive session, so a coarse per-game lock is the right granularity.

## Design Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| **Strategy** | `MinePlacementStrategy` | Swap placement policy without touching the board |
| **Facade** | `MinesweeperGame` (concrete class) | One API hides board, placement, flood-fill, status. No interface — one engine, nothing swaps it |
| **Factory** | `BoardFactory` | Preset → board dims in one place; injects the strategy |
| **Rich Domain Model** | `Cell` | Cell owns its reveal/flag transitions — no illegal external state changes |

## Extending the System

### First-click-safe (already supported — swap the strategy)

```java
new MinesweeperGame(
    BoardFactory.fromDifficulty(Difficulty.EXPERT, new SafeFirstClickMinePlacementStrategy()));
```

### Add chording (reveal a number's neighbours)

```java
// Board.chord(r, c) reuses flood-fill per neighbour; MinesweeperGame.chord() exposes it.
// Existing reveal() is untouched (OCP).
```

## Demo

`MinesweeperDemo.main()` covers all 9 functional requirements — reveal, flood-fill, flag/unflag,
protected-flag, loss, win, status tracking — plus the first-click-safe curveball and both
exception paths (out-of-bounds, move-after-game-over). Runs are reproducible via seeded RNG.

```
cd core-lld/src/main/java
javac -d /tmp/ms $(find com/lldprep/systems/minesweeper -name "*.java")
java -cp /tmp/ms com.lldprep.systems.minesweeper.demo.MinesweeperDemo
```
