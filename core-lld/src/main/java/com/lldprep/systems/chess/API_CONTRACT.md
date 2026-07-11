# Chess — API Contract

---

## Endpoint Summary

| Method | Endpoint | Purpose | Notes |
|--------|----------|---------|-------|
| `POST` | `/api/v1/games` | Create a new game | White and Black player names |
| `POST` | `/api/v1/games/{gameId}/moves` | Make a move | Algebraic from/to squares |
| `GET` | `/api/v1/games/{gameId}` | Get game state | Board, current turn, move history |
| `GET` | `/api/v1/games/{gameId}/board` | Get board as text | 8×8 ASCII representation |

---

## Request / Response Schemas

### 1. Create Game

```
POST /api/v1/games
Content-Type: application/json
```

**Request:**
```json
{
  "whitePlayer": "Morphy",
  "blackPlayer": "Amateur"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `whitePlayer` | string | Yes | Non-empty, max 100 chars |
| `blackPlayer` | string | Yes | Non-empty, max 100 chars, must differ from white |

**Success Response (201):**
```json
{
  "id": "GAME-a1b2c3d4",
  "white": { "id": "P-W", "name": "Morphy", "color": "WHITE" },
  "black": { "id": "P-B", "name": "Amateur", "color": "BLACK" },
  "state": "WHITE_TO_MOVE",
  "board": "rnbqkbnr\npppppppp\n........\n........\n........\n........\nPPPPPPPP\nRNBQKBNR",
  "moveCount": 0
}
```

---

### 2. Make a Move

```
POST /api/v1/games/{gameId}/moves
Content-Type: application/json
```

**Request:**
```json
{
  "from": "e2",
  "to": "e4",
  "promotion": null
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `from` | string | Yes | Algebraic square, e.g., "e2", "a1", "h8" |
| `to` | string | Yes | Algebraic square, must be a legal destination |
| `promotion` | string | No | `QUEEN`, `ROOK`, `BISHOP`, `KNIGHT` — only valid when pawn reaches last rank |

**Success Response (200):**
```json
{
  "move": {
    "from": "e2",
    "to": "e4",
    "piece": "P",
    "captured": null,
    "notation": "e2e4",
    "isCastling": false,
    "isEnPassant": false,
    "isPromotion": false
  },
  "state": "BLACK_TO_MOVE",
  "isCheck": false,
  "isCheckmate": false,
  "isStalemate": false
}
```

**Error Responses:**
| Status | Error Code | Message |
|--------|-----------|---------|
| 400 | `NOT_YOUR_TURN` | Not WHITE's turn |
| 400 | `NO_PIECE` | No piece at e4 |
| 400 | `ILLEGAL_MOVE` | Pawn from e2 to e5 is illegal |
| 400 | `SELF_CHECK` | Would leave king in check |
| 400 | `GAME_OVER` | Game has already ended (WHITE_WINS) |
| 404 | `GAME_NOT_FOUND` | Game GAME-x1y2z3 does not exist |

---

### 3. Get Game State

```
GET /api/v1/games/{gameId}
```

**Success Response (200):**
```json
{
  "id": "GAME-a1b2c3d4",
  "white": { "id": "P-W", "name": "Morphy", "color": "WHITE" },
  "black": { "id": "P-B", "name": "Amateur", "color": "BLACK" },
  "state": "WHITE_TO_MOVE",
  "fullMoveNumber": 1,
  "moveHistory": [
    { "notation": "e2e4", "piece": "P", "from": "e2", "to": "e4" }
  ]
}
```

---

### 4. Get Board

```
GET /api/v1/games/{gameId}/board
```

**Success Response (200):**
```
  a b c d e f g h
8 r n b q k b n r 8
7 p p p p p p p p 7
6 . . . . . . . . 6
5 . . . . . . . . 5
4 . . . . P . . . 4
3 . . . . . . . . 3
2 P P P P . P P P 2
1 R N B Q K B N R 1
  a b c d e f g h
```

---

## Move Notation

| Scenario | Notation | Example |
|----------|----------|---------|
| Pawn move | `from*to` | `e2e4` |
| Piece move | `[Piece]*from*to` | `Ng1f3`, `Qd1h5` |
| Capture | `[Piece]*from*x*to` | `Qh5xf7`, `exf6` |
| Castling kingside | `O-O` | |
| Castling queenside | `O-O-O` | |
| Promotion | `[from]*to*=*[Piece]` | `b7b8=Q` |
| En passant | Same as pawn capture | `exf6` |

---

## State Machine

```
           createGame()
                │
          WHITE_TO_MOVE ──White moves──► BLACK_TO_MOVE ──Black moves──► WHITE_TO_MOVE
                │                              │
                │ checkmate                    │ checkmate
                ▼                              ▼
          WHITE_WINS                     BLACK_WINS

           WHITE_TO_MOVE ──stalemate detected──► STALEMATE
           BLACK_TO_MOVE ──stalemate detected──► STALEMATE
```

---

## Key Validation Rules

| Rule | Enforced at |
|------|------------|
| Only current turn's player can move | `GameService.validateGameActive()` |
| Move must be in piece's `getValidMoves()` set | `GameService.makeMove()` |
| Self-check filter: illegal if king left in check after move | `Piece.getValidMoves()` — Board.copy() simulation |
| Castling: king and rook unmoved | `King.addCastlingCandidates()` |
| Castling: squares between king and rook empty | `King.squaresEmpty()` |
| Castling: king does not pass through check | Covered by `getValidMoves()` — self-check filter catches intermediates |
| En passant: only immediately after double-step | `Board.enPassantTarget` expires each turn |
| Promotion: mandatory when pawn reaches last rank | `GameService.isPromotion()` |
| Promotion type must be a valid piece type | `PieceFactory.create()` accepts KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN |

---

## Example Flow: Scholar's Mate

```
1. POST /games → WHITE_TO_MOVE
2. POST /games/gid/moves { from: "e2", to: "e4" } → BLACK_TO_MOVE, e-pawn on e4
3. POST /games/gid/moves { from: "e7", to: "e5" } → WHITE_TO_MOVE
4. POST /games/gid/moves { from: "d1", to: "h5" } → BLACK_TO_MOVE, Queen to h5
5. POST /games/gid/moves { from: "b8", to: "c6" } → WHITE_TO_MOVE
6. POST /games/gid/moves { from: "f1", to: "c4" } → BLACK_TO_MOVE, Bishop to c4
7. POST /games/gid/moves { from: "g8", to: "f6" } → WHITE_TO_MOVE
8. POST /games/gid/moves { from: "h5", to: "f7" } → WHITE_WINS (checkmate)
```

## Error Response Format

All errors follow this structure:

```json
{
  "error": {
    "code": "ILLEGAL_MOVE",
    "message": "Pawn from e2 to e5 is illegal",
    "from": "e2",
    "to": "e5"
  }
}
```
