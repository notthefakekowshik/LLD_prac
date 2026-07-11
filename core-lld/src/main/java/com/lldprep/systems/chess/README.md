# Chess

Standard chess engine demonstrating **Template Method**, **Factory**, and **Facade** patterns with full move validation, castling, en passant, and pawn promotion.

## Features

- **Complete piece movement** — King, Queen, Rook, Bishop, Knight, Pawn with type-specific move generation
- **Self-check prevention** — every candidate move is simulated on a copy of the board; moves that leave the king in check are filtered out
- **Check detection** — after every move, the opponent's king is scanned for threats
- **Checkmate / Stalemate** — detected via `hasNoLegalMoves(color)` combined with `isCheck(color)`
- **Castling** — kingside (O-O) and queenside (O-O-O) with all conditions: unmoved king/rook, clear path, king does not pass through or into check
- **En passant** — pawn captures an opponent's pawn that just advanced two squares; state tracked via `enPassantTarget` on Board
- **Pawn promotion** — pawn reaching the last rank promotes; default Queen, configurable via API
- **Algebraic notation** — moves are input/output as `"e2"→"e4"`; `Square` record handles conversion
- **Move history** — `Game.getMoves()` returns the chronologically-ordered list of all played moves

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **Template Method** | `Piece.getValidMoves()` → calls abstract `getCandidateSquares()`, Board filters for self-check | Every piece shares the same post-filter; subtypes vary only move-generation |
| **Factory** | `PieceFactory.create(PieceType, Color)` | Centralises piece creation; callers never use `new King(...)` directly |
| **Repository** | `GameRepository` | In-memory store; swappable for DB |
| **Facade** | `ChessFacade` | Single entry point: `createGame()`, `makeMove()`, `getBoard()` |

## Package Structure

```
com.lldprep.systems.chess/
├── model/
│   ├── Piece.java                  # Abstract — color, type, hasMoved; getCandidateSquares(), getValidMoves()
│   ├── King.java                   # 1-square moves + castling candidates
│   ├── Queen.java                  # Sliding rook + bishop
│   ├── Rook.java                   # Sliding orthogonal
│   ├── Bishop.java                 # Sliding diagonal
│   ├── Knight.java                 # L-shaped jumps (no blocking)
│   ├── Pawn.java                   # Forward/diagonal captures + en passant + double-step
│   ├── Board.java                  # 8×8 grid; copy(); isCheck/isCheckmate/isStalemate; executeMove()
│   ├── Move.java                   # Record — from/to/piece/captured/promotion/castling/enPassant
│   ├── Player.java                 # id, name, color
│   ├── Game.java                   # Board + 2 players + GameState + move history
│   ├── PieceFactory.java           # create(type, color)
│   └── model/enums/
│       ├── Color.java              # WHITE, BLACK
│       ├── PieceType.java          # KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
│       └── GameState.java          # WHITE_TO_MOVE, BLACK_TO_MOVE, WHITE_WINS, BLACK_WINS, STALEMATE
├── exception/
│   ├── ChessException.java
│   ├── InvalidMoveException.java
│   ├── GameNotFoundException.java
│   └── GameOverException.java
├── factory/
├── repository/
│   └── GameRepository.java
├── service/
│   ├── ChessFacade.java            # FACADE: single entry point
│   └── GameService.java            # Move validation pipeline + state management
└── demo/
    └── ChessDemo.java
```

## Core Algorithm — Move Validation Pipeline

```
1. Parse from/to squares from algebraic notation (e.g., "e2" → Square(6,4))
2. Validate game is active (not checkmate/stalemate)
3. Validate correct player's turn
4. Get piece at 'from' — must exist and belong to current player
5. piece.getValidMoves(Board, from)
   ├── piece.getCandidateSquares(Board, from) — geometry only
   └── For each candidate: simulate on Board.copy(), test isCheck(currentColor)
       → Keep only moves where king is NOT in check after simulation
6. Validate 'to' is in the filtered legal-moves set
7. Identify special move: castling (|dCol|=2, King), en passant (pawn diagonal to empty square), promotion
8. Board.executeMove(move) — updates grid, handles rook-jump for castling, removes captured pawn for en passant
9. Advance turn; detect check/checkmate/stalemate on opponent
```

## Check / Checkmate / Stalemate

```
isCheck(defendingColor):
    kingSquare = findKing(defendingColor)
    for every attacker piece on board:
        if kingSquare in piece.getCandidateSquares() → true

isCheckmate(defendingColor):
    isCheck(defendingColor) AND hasNoLegalMoves(defendingColor)

isStalemate(defendingColor):
    !isCheck(defendingColor) AND hasNoLegalMoves(defendingColor)

hasNoLegalMoves(color):
    for every friendly piece on board:
        if piece.getValidMoves().size() > 0 → false
    return true
```

## Quick Start

```bash
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.chess.demo.ChessDemo" -pl core-lld
```

## Demo Scenarios

1. **Scholar's Mate** — 4-move checkmate (e4 e5, Qh5 Nc6, Bc4 Nf6, Qxf7#)
2. **Castling** — kingside O-O and queenside O-O-O for both colors
3. **En passant** — pawn captures opponent's double-step pawn
4. **Pawn promotion** — pawn reaches last rank, promotes to Queen
5. **Stalemate** — mechanic explanation and implementation walkthrough
6. **Validation failures** — wrong turn, blocked rook, empty square, backward pawn, pinned piece
7. **Move history** — reads `Game.getMoves()` directly for the chronologically-ordered move list

## Extending the System

| Curveball | Extension Strategy | Pattern |
|-----------|-------------------|---------|
| Fischer Random (Chess960) | `StartingPositionStrategy` — generates back-rank layout | Strategy |
| Three-check variant | `WinCondition` interface checked after every move | Strategy |
| Draw by repetition | `Board` gains Zobrist hash; `Game` tracks position count → auto-draw | |
| Pause / resign | `GameState` gains `PAUSED`, `RESIGNED`; `GameService.resign(id)` | State |
| Undo move | `Game` stores `Stack<Board>` snapshots; `undo()` pops | Memento |


## Documentation

- `DESIGN_DICE.md` — Full D.I.C.E. workflow, class diagram, algorithms
- `API_CONTRACT.md` — API endpoints, request/response schemas, validation rules
- `class_diagram.mermaid` — Standalone class diagram
