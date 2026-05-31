# ATM Machine — API Contract

> **Why this exists:** In LLD interviews, defining the system's external interface (REST endpoints, request/response schemas, error codes) is often the first deliverable before any class diagram. This doc demonstrates that skill.
> **Applies to:** Phase 4 systems (user-facing / multi-service). Phase 3 building blocks are exempt.
> **Reference:** `INSTRUCTIONS.md` Section 2.6

---

## Endpoint Summary

| Method | Endpoint | Purpose | State Required |
|--------|----------|---------|----------------|
| `POST` | `/api/v1/session` | Insert card, begin session | Idle → CardInserted |
| `POST` | `/api/v1/session/authenticate` | Validate PIN | CardInserted → PINEntered |
| `POST` | `/api/v1/session/select-account` | Choose Checking or Savings | PINEntered → TransactionMenu |
| `POST` | `/api/v1/transactions` | Execute a transaction | TransactionMenu |
| `GET`  | `/api/v1/accounts/{id}/balance` | Balance inquiry | TransactionMenu |
| `DELETE`| `/api/v1/session` | Cancel and eject card | Any active state |
| `GET`  | `/api/v1/session` | Get current session state | Any |
| `GET`  | `/api/v1/transactions` | Get transaction history | Any |

---

## Request / Response Schemas

### 1. Insert Card

Starts a new ATM session. Card is validated (not expired, not blocked).

```
POST /api/v1/session
Content-Type: application/json
```

**Request:**
```json
{
  "cardNumber": "1234-5678-9012-3456"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `cardNumber` | `String` | Yes | Format `XXXX-XXXX-XXXX-XXXX`, `\d{4}-\d{4}-\d{4}-\d{4}` |

**Success Response (200):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "state": "CARD_INSERTED",
  "message": "Card accepted. Please enter your PIN.",
  "linkedAccounts": ["CHECKING", "SAVINGS"]
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `404` | `CARD_NOT_FOUND` | Card number not recognized |
| `400` | `CARD_EXPIRED` | Card expired on {date} |
| `400` | `CARD_BLOCKED` | Card is blocked. Contact your bank. |
| `409` | `SESSION_ACTIVE` | Another session is active. Eject card first. |

---

### 2. Authenticate PIN

Validates the 4-digit PIN against the stored hash. 3 consecutive failures blocks the card.

```
POST /api/v1/session/authenticate
Content-Type: application/json
```

**Request:**
```json
{
  "sessionId": "s_A3F2B9C1",
  "pin": "1234"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `sessionId` | `String` | Yes | Current session identifier |
| `pin` | `String` | Yes | 4 digits, `\d{4}` |

**Success Response (200):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "state": "PIN_ENTERED",
  "message": "PIN validated. Select an account.",
  "remainingAttempts": 3,
  "accountOptions": [
    {"accountId": "ACC-1001", "type": "CHECKING"},
    {"accountId": "ACC-1002", "type": "SAVINGS"}
  ]
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `401` | `INVALID_PIN` | Incorrect PIN. {N} attempt(s) remaining. |
| `400` | `CARD_BLOCKED` | Card blocked after 3 failed attempts. |
| `400` | `INVALID_STATE` | Cannot authenticate — session is in state {actualState} |
| `404` | `SESSION_NOT_FOUND` | Session does not exist or expired |

---

### 3. Select Account

Chooses an account linked to the card.

```
POST /api/v1/session/select-account
Content-Type: application/json
```

**Request:**
```json
{
  "sessionId": "s_A3F2B9C1",
  "accountType": "CHECKING"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `sessionId` | `String` | Yes | |
| `accountType` | `String` | Yes | Must be one of the linked account types |

**Success Response (200):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "state": "TRANSACTION_MENU",
  "selectedAccount": {
    "accountId": "ACC-1001",
    "type": "CHECKING",
    "balance": 50000.00
  },
  "message": "Account selected. Choose a transaction."
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `400` | `INVALID_ACCOUNT` | Account type not linked to this card |
| `400` | `INVALID_STATE` | Cannot select account in state {actualState} |

---

### 4. Execute Transaction

Performs cash withdrawal or deposit. This is the primary "do something" endpoint.

```
POST /api/v1/transactions
Content-Type: application/json
```

**Request (Withdrawal):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "type": "CASH_WITHDRAWAL",
  "amount": 2700.00
}
```

**Request (Deposit with denomination breakdown):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "type": "CASH_DEPOSIT",
  "amount": 1500.00,
  "denominationBreakdown": {
    "NOTE_500": 2,
    "NOTE_200": 2,
    "NOTE_100": 1
  }
}
```

**Request (PIN Change):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "type": "PIN_CHANGE",
  "oldPin": "1234",
  "newPin": "5678"
}
```

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `sessionId` | `String` | Yes | |
| `type` | `String` | Yes | `BALANCE_INQUIRY`, `CASH_WITHDRAWAL`, `CASH_DEPOSIT`, `PIN_CHANGE` |
| `amount` | `Number` | Conditional | Required for WITHDRAWAL/DEPOSIT; omitted for BALANCE_INQUIRY/PIN_CHANGE |
| `denominationBreakdown` | `Object` | No | Deposit only; keyed by `NOTE_2000/500/200/100` |
| `oldPin` | `String` | Conditional | Required for PIN_CHANGE |
| `newPin` | `String` | Conditional | Required for PIN_CHANGE, 4 digits |

**Success Response (Withdrawal — 200):**
```json
{
  "transactionId": "txn_D7E2F4A1",
  "type": "CASH_WITHDRAWAL",
  "amount": 2700.00,
  "status": "COMPLETED",
  "denominationDispensed": {
    "NOTE_2000": 1,
    "NOTE_500": 1,
    "NOTE_200": 1
  },
  "remainingBalance": 47300.00,
  "receipt": "Transaction #{txn_D7E2F4A1}\nType: WITHDRAWAL\nAmount: ₹2,700.00\nDispensed: 2000×1 + 500×1 + 200×1\nBalance: ₹47,300.00\nTimestamp: 2026-05-31T14:30:00"
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `400` | `INSUFFICIENT_FUNDS` | Account balance ₹{balance} is less than amount ₹{amount} |
| `400` | `INSUFFICIENT_CASH` | ATM has insufficient cash for ₹{amount} |
| `400` | `UNAVAILABLE_DENOMINATION` | Cannot dispense ₹{amount} with available denominations |
| `400` | `INVALID_AMOUNT` | Amount must be a multiple of ₹100 |
| `400` | `INVALID_STATE` | Cannot perform transaction in state {state} |
| `400` | `INVALID_DENOMINATION` | Deposit rejection: note count mismatch for {denom} |

---

### 5. Cancel / Eject Card

Ends the session and ejects the card. Resets all session state.

```
DELETE /api/v1/session
Content-Type: application/json
```

**Request:**
```json
{
  "sessionId": "s_A3F2B9C1"
}
```

**Success Response (200):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "state": "IDLE",
  "message": "Card ejected. Thank you for banking with us.",
  "sessionDuration": "PT2M34S"
}
```

---

### 6. Get Session State

Returns the current state machine state. Idempotent, safe for polling.

```
GET /api/v1/session?sessionId=s_A3F2B9C1
```

**Response (200):**
```json
{
  "sessionId": "s_A3F2B9C1",
  "state": "CARD_INSERTED",
  "createdAt": "2026-05-31T14:28:30",
  "timeoutAt": "2026-05-31T14:38:30",
  "secondsRemaining": 570
}
```

**Error Responses:**

| Status | Error Code | Message |
|--------|-----------|---------|
| `404` | `SESSION_NOT_FOUND` | Session expired or does not exist |
| `410` | `SESSION_TIMEOUT` | Session timed out after 10 minutes of inactivity |

---

### 7. Transaction History

Returns paginated transaction log for the account. Immutable audit trail.

```
GET /api/v1/transactions?accountId=ACC-1001&page=0&size=10
```

**Response (200):**
```json
{
  "accountId": "ACC-1001",
  "transactions": [
    {
      "transactionId": "txn_D7E2F4A1",
      "type": "CASH_WITHDRAWAL",
      "amount": 2700.00,
      "status": "COMPLETED",
      "timestamp": "2026-05-31T14:30:00"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 5
}
```

---

## State Machine → HTTP Mapping

The ATM state machine drives what endpoints are valid at any given time:

```
IDLE                    → POST /session (insertCard)
CARD_INSERTED           → POST /session/authenticate (enterPIN)
                         → DELETE /session (cancel)
PIN_ENTERED             → POST /session/select-account
                         → DELETE /session (cancel)
TRANSACTION_MENU        → POST /transactions (withdrawal/deposit/inquiry)
                         → DELETE /session (cancel)
DISPENSING              → (auto-transitions back to TRANSACTION_MENU or IDLE)
```

Invalid state transitions return `400 INVALID_STATE` with the actual and expected states.

---

## Error Response Format

All errors follow a consistent structure:

```json
{
  "error": {
    "code": "INSUFFICIENT_FUNDS",
    "message": "Account balance ₹2,500.00 is less than amount ₹5,000.00",
    "details": {
      "balance": 2500.00,
      "requested": 5000.00,
      "shortfall": 2500.00
    }
  }
}
```

| Field | Purpose |
|-------|---------|
| `code` | Machine-readable error code (snake_case) |
| `message` | Human-readable description with dynamic values |
| `details` | Contextual debugging data (nullable) |

---

## Authentication & Security

- **No authentication header** — session-based via `sessionId` parameter. The card + PIN authenticate the user; subsequent requests carry the session token.
- **PINs are never logged or returned** in API responses. Only `remainingAttempts` is surfaced.
- **Session timeout:** 10 minutes of inactivity triggers auto-eject (`SESSION_TIMEOUT`).
- **Card blocking:** 3 consecutive failed PIN attempts → card blocked → `CARD_BLOCKED` response. Block persists until bank admin unblocks.

---

## DTO → In-Memory Mapping

| API DTO | In-Memory Model | Notes |
|---------|----------------|-------|
| `POST /session` `cardNumber` | `Card.cardNumber` | Direct field mapping |
| `POST /session/authenticate` `pin` | `Card.validatePIN(pin)` | Hashed comparison, never stored |
| `POST /session/select-account` `accountType` | `AccountType` enum | |
| `POST /transactions` `type` + `amount` | `Transaction(TransactionType, BigDecimal, accountId)` | Creates new transaction entity |
| `POST /transactions` `denominationBreakdown` | `CashInventory.dispense(Map<Denomination, Integer>)` | Passed via `extra` map |

**Design decision:** The API contract maps 1:1 to the in-memory `ATMState` interface methods. No translation layer needed — the state machine IS the API. This is intentional: keep the API surface identical to the domain model to reduce mapping bugs.

---

## Rate Limiting

| Endpoint | Rate Limit | Reason |
|----------|-----------|--------|
| `POST /session/authenticate` | 3 per session | Enforced by PIN attempt counter, not a network rate limiter |
| `POST /transactions` | 5 per minute per account | Prevent rapid-fire in test environments (not enforced in demo) |
| All others | Unrestricted | Session-based, single user per ATM |

---

## Postman Collection (Interview-Friendly)

To demonstrate in an interview, you could trace these 3 scenarios:

**Happy Path: Withdraw ₹2,700**
```
1. POST /session           {"cardNumber": "1234-5678-9012-3456"}         → 200, sessionId
2. POST /session/authenticate  {"sessionId": "...", "pin": "1234"}       → 200, state=PIN_ENTERED
3. POST /session/select-account {"sessionId": "...", "accountType": "CHECKING"}  → 200
4. POST /transactions      {"sessionId": "...", "type": "CASH_WITHDRAWAL", "amount": 2700}  → 200
5. DELETE /session         {"sessionId": "..."}                          → 200, card ejected
```

**Error Path: 3 Invalid PINs → Block**
```
1. POST /session           {"cardNumber": "9876-5432-1098-7654"}  → 200
2. POST /session/authenticate  {"sessionId": "...", "pin": "0000"}   → 401, 2 remaining
3. POST /session/authenticate  {"sessionId": "...", "pin": "1111"}   → 401, 1 remaining
4. POST /session/authenticate  {"sessionId": "...", "pin": "2222"}   → 400, CARD_BLOCKED
```

**Edge Path: Insufficient Funds**
```
1-3. (normal setup)
4. POST /transactions      {"...", "type": "CASH_WITHDRAWAL", "amount": 999999}  → 400, INSUFFICIENT_FUNDS
```

---

## Extending the API

| Curveball | API Change | Breaking? |
|-----------|-----------|-----------|
| Add UPI payment | `POST /transactions` with `type: "UPI_PAYMENT"` and `upiId` + `amount` | No — new type in existing enum |
| Add account-to-account transfer | `POST /transactions` with `type: "TRANSFER"`, `fromAccount`, `toAccount`, `amount` | No |
| Admin endpoints (refill cash, unblock card) | New endpoints under `/api/v1/admin/` | No — separate namespace |
| Multi-language support | `Accept-Language` header + `messages` in response | No — backward compatible |

**OCP check:** Every curveball adds a new endpoint or a new `type` value — zero modifications to existing endpoints.
