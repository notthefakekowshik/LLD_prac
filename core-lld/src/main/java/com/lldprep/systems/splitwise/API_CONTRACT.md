# Splitwise — API Contract

---

## Endpoint Summary

| Method | Endpoint | Purpose | Notes |
|--------|----------|---------|-------|
| `POST` | `/api/v1/users` | Register a user | |
| `POST` | `/api/v1/groups` | Create a group | |
| `POST` | `/api/v1/groups/{groupId}/members` | Add member to group | |
| `POST` | `/api/v1/expenses` | Add an expense | Body specifies split type |
| `GET` | `/api/v1/users/{userId}/balances` | Get balance summary | All users this person owes or is owed by |
| `GET` | `/api/v1/users/{userId}/balances/{otherUserId}` | Get balance between two users | Single net figure |
| `GET` | `/api/v1/users/{userId}/expenses` | Get expense history | |
| `GET` | `/api/v1/groups/{groupId}/expenses` | Get group expense history | |
| `POST` | `/api/v1/settlements` | Settle up | Reduces balance between two users |
| `GET` | `/api/v1/groups/{groupId}/simplify` | Get simplified debt transactions | Returns minimum transactions to settle group |

---

## Request / Response Schemas

### 1. Register User

```
POST /api/v1/users
Content-Type: application/json
```

**Request:**
```json
{
  "name": "Alice",
  "email": "alice@example.com"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `name` | string | Yes | Non-empty, max 100 chars |
| `email` | string | Yes | Valid email format, unique |

**Success Response (201):**
```json
{
  "id": "usr_a1b2c3",
  "name": "Alice",
  "email": "alice@example.com"
}
```

**Error Responses:**
| Status | Error Code | Message |
|--------|-----------|---------|
| 409 | `EMAIL_ALREADY_EXISTS` | A user with this email already exists |
| 400 | `INVALID_EMAIL` | Email format is invalid |

---

### 2. Add Expense — Equal Split

```
POST /api/v1/expenses
Content-Type: application/json
```

**Request:**
```json
{
  "description": "Dinner at Truffles",
  "amount": 900.00,
  "paidByUserId": "usr_a1b2c3",
  "splitType": "EQUAL",
  "participantIds": ["usr_a1b2c3", "usr_b4c5d6", "usr_c7d8e9"],
  "groupId": "grp_x1y2z3"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `description` | string | Yes | Non-empty, max 200 chars |
| `amount` | decimal | Yes | > 0, max 2 decimal places |
| `paidByUserId` | string | Yes | Must exist, must be in `participantIds` |
| `splitType` | enum | Yes | `EQUAL`, `EXACT`, `PERCENTAGE` |
| `participantIds` | array | Yes | Min 2, max 50 |
| `groupId` | string | No | Must exist if provided; all participants must be members |

**Success Response (201):**
```json
{
  "id": "exp_p1q2r3",
  "description": "Dinner at Truffles",
  "amount": 900.00,
  "paidBy": { "id": "usr_a1b2c3", "name": "Alice" },
  "splitType": "EQUAL",
  "splits": [
    { "user": { "id": "usr_a1b2c3", "name": "Alice" }, "amount": 300.00 },
    { "user": { "id": "usr_b4c5d6", "name": "Bob" },   "amount": 300.00 },
    { "user": { "id": "usr_c7d8e9", "name": "Charlie"}, "amount": 300.00 }
  ],
  "groupId": "grp_x1y2z3",
  "createdAt": "2026-06-08T14:30:00Z"
}
```

---

### 3. Add Expense — Exact Split

```
POST /api/v1/expenses
```

**Request:**
```json
{
  "description": "Hotel room",
  "amount": 5000.00,
  "paidByUserId": "usr_a1b2c3",
  "splitType": "EXACT",
  "participantIds": ["usr_a1b2c3", "usr_b4c5d6"],
  "exactAmounts": {
    "usr_a1b2c3": 2000.00,
    "usr_b4c5d6": 3000.00
  }
}
```

**Validation:** `sum(exactAmounts.values)` must equal `amount`. All `participantIds` must appear in `exactAmounts`.

**Error Responses:**
| Status | Error Code | Message |
|--------|-----------|---------|
| 400 | `SPLIT_AMOUNTS_MISMATCH` | Exact amounts sum to 4500.00 but expense is 5000.00 |
| 400 | `MISSING_SPLIT_PARTICIPANT` | Participant usr_c7d8e9 has no exact amount specified |

---

### 4. Add Expense — Percentage Split

```
POST /api/v1/expenses
```

**Request:**
```json
{
  "description": "Cab to airport",
  "amount": 800.00,
  "paidByUserId": "usr_b4c5d6",
  "splitType": "PERCENTAGE",
  "participantIds": ["usr_a1b2c3", "usr_b4c5d6"],
  "percentages": {
    "usr_a1b2c3": 60.0,
    "usr_b4c5d6": 40.0
  }
}
```

**Validation:** `sum(percentages.values)` must equal 100.0 (tolerance ±0.01).

**Error Responses:**
| Status | Error Code | Message |
|--------|-----------|---------|
| 400 | `PERCENTAGES_DO_NOT_SUM_TO_100` | Percentages sum to 95.0, must be 100.0 |

---

### 5. Get Balance Summary

```
GET /api/v1/users/{userId}/balances
```

**Success Response (200):**
```json
{
  "userId": "usr_a1b2c3",
  "totalOwed": 600.00,
  "totalOwing": 200.00,
  "net": 400.00,
  "balances": [
    {
      "withUser": { "id": "usr_b4c5d6", "name": "Bob" },
      "amount": 300.00,
      "direction": "OWED_TO_YOU"
    },
    {
      "withUser": { "id": "usr_c7d8e9", "name": "Charlie" },
      "amount": 300.00,
      "direction": "OWED_TO_YOU"
    },
    {
      "withUser": { "id": "usr_d1e2f3", "name": "Dave" },
      "amount": 200.00,
      "direction": "YOU_OWE"
    }
  ]
}
```

`direction`:
- `OWED_TO_YOU` — the other user owes you `amount`
- `YOU_OWE` — you owe the other user `amount`

---

### 6. Settle Up

```
POST /api/v1/settlements
Content-Type: application/json
```

**Request:**
```json
{
  "payerUserId": "usr_b4c5d6",
  "payeeUserId": "usr_a1b2c3",
  "amount": 300.00
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `payerUserId` | string | Yes | Must exist |
| `payeeUserId` | string | Yes | Must exist, must differ from payer |
| `amount` | decimal | Yes | > 0, must not exceed outstanding payer → payee balance |

**Success Response (201):**
```json
{
  "id": "stl_s1t2u3",
  "payer": { "id": "usr_b4c5d6", "name": "Bob" },
  "payee": { "id": "usr_a1b2c3", "name": "Alice" },
  "amount": 300.00,
  "settledAt": "2026-06-08T15:00:00Z",
  "remainingBalance": 0.00
}
```

**Error Responses:**
| Status | Error Code | Message |
|--------|-----------|---------|
| 400 | `SELF_SETTLEMENT` | Payer and payee cannot be the same user |
| 400 | `SETTLEMENT_EXCEEDS_BALANCE` | Settlement amount exceeds outstanding balance |
| 404 | `USER_NOT_FOUND` | User usr_b4c5d6 does not exist |

---

### 7. Simplify Debts

```
GET /api/v1/groups/{groupId}/simplify
```

**Success Response (200):**
```json
{
  "groupId": "grp_x1y2z3",
  "originalTransactionCount": 6,
  "simplifiedTransactionCount": 3,
  "transactions": [
    {
      "payer": { "id": "usr_b4c5d6", "name": "Bob" },
      "payee": { "id": "usr_a1b2c3", "name": "Alice" },
      "amount": 400.00
    },
    {
      "payer": { "id": "usr_c7d8e9", "name": "Charlie" },
      "payee": { "id": "usr_a1b2c3", "name": "Alice" },
      "amount": 200.00
    },
    {
      "payer": { "id": "usr_d1e2f3", "name": "Dave" },
      "payee": { "id": "usr_c7d8e9", "name": "Charlie" },
      "amount": 100.00
    }
  ]
}
```

**Note:** This is a **read-only suggestion**. It does not record settlements. Users must call `POST /settlements` for each transaction to actually reduce balances.

---

## Error Response Format

All errors follow this structure:

```json
{
  "error": {
    "code": "SPLIT_AMOUNTS_MISMATCH",
    "message": "Exact amounts sum to 4500.00 but expense is 5000.00",
    "field": "exactAmounts"
  }
}
```

| Field | Description |
|-------|-------------|
| `code` | Machine-readable error code |
| `message` | Human-readable explanation |
| `field` | Request field that caused the error (nullable for non-field errors) |

---

## Key Validation Rules (Server-Side)

| Rule | Enforced at |
|------|------------|
| `paidByUserId` must be in `participantIds` | `ExpenseService.addExpense()` |
| EXACT: amounts must sum to total (±0.01 tolerance) | `ExactSplitStrategy.calculate()` |
| PERCENTAGE: percentages must sum to 100 (±0.01 tolerance) | `PercentageSplitStrategy.calculate()` |
| All participants must be members of the group (if groupId provided) | `SplitwiseFacade.addExpense()` |
| Settlement amount > 0 | `SplitwiseFacade.settle()` |
| `payer != payee` | `SplitwiseFacade.settle()` |

---

## Example Flow: Happy Path (Group Trip)

```
1. POST /users → Alice, Bob, Charlie registered
2. POST /groups → "Goa Trip" group created with all 3
3. POST /expenses → Alice pays ₹900 for dinner (EQUAL split) → Bob owes ₹300, Charlie owes ₹300
4. POST /expenses → Bob pays ₹600 for hotel (EXACT: Alice ₹400, Charlie ₹200) → Alice owes ₹400, Charlie owes ₹200
5. GET  /users/alice/balances → Alice: net +₹(300−400) = −₹100 (she owes ₹100 to Bob)
6. GET  /groups/goaTrip/simplify → [Alice→Bob ₹100, Charlie→Bob ₹500]
7. POST /settlements → Alice pays Bob ₹100
8. GET  /users/alice/balances → net ₹0 (settled)
```
