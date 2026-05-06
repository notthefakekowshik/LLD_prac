# ATM Machine System

A full-featured ATM implementation demonstrating the **State** pattern and **Chain of Responsibility** pattern with a clean, extensible architecture.

## Features

- **Card Authentication** — Validate card number, check expiry, detect blocked cards
- **PIN Validation** — 3-attempt limit with auto-block on failure
- **Account Selection** — Choose between Checking/Savings accounts
- **Transaction Types**:
  - Balance Inquiry
  - Cash Withdrawal with optimal denomination dispensing
  - Cash Deposit
  - PIN Change
- **Cash Dispensing** — Chain of Responsibility for optimal note selection (2000→500→200→100)
- **Receipt Printing** — Formatted transaction receipts
- **Transaction Logging** — Immutable audit trail
- **State Machine** — Type-safe state transitions prevent invalid operations

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **State** | `ATMState` interface + 5 concrete states | Enforces valid operation sequences; prevents calling `withdraw()` before `enterPIN()` |
| **Chain of Responsibility** | `CashDispenser` hierarchy | Optimal cash dispensing: tries 2000s first, then cascades down to 100s |
| **Strategy** | `ReceiptPrinter` interface | Pluggable receipt formats (text, HTML, JSON) |
| **Singleton** | `ATM` per physical machine | Single instance manages cash inventory |

## State Machine

```
IDLE → insertCard() → CARD_INSERTED → enterPIN() → PIN_ENTERED
                                                      ↓
TRANSACTION_MENU ← selectAccount() ←┘
    ↓ performTransaction()
DISPENSING → (auto) → TRANSACTION_MENU or IDLE
```

Invalid operations throw `InvalidStateException` — enforced at compile time via the State pattern.

## Quick Start

```bash
# Compile and run demo
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.atm.demo.ATMDemo" -pl core-lld
```

## Package Structure

```
com.lldprep.atm/
├── ATM.java                      # Main orchestrator (Singleton-like)
├── model/
│   ├── Card.java                 # Card data + PIN validation
│   ├── Account.java              # Account with thread-safe balance
│   ├── Transaction.java          # Transaction record
│   ├── CashInventory.java        # Denomination counts
│   └── enums/                    # Denomination, TransactionType, etc.
├── state/                        # State Pattern
│   ├── ATMState.java             # State interface
│   ├── IdleState.java
│   ├── CardInsertedState.java
│   ├── PINEnteredState.java
│   ├── TransactionMenuState.java
│   └── DispensingState.java
├── dispenser/                    # Chain of Responsibility
│   ├── CashDispenser.java        # Abstract handler
│   ├── Dispenser2000.java
│   ├── Dispenser500.java
│   ├── Dispenser200.java
│   └── Dispenser100.java
├── service/
│   ├── CardManager.java          # Card repository
│   ├── AccountManager.java       # Account repository
│   ├── TransactionLogger.java    # Audit logging
│   └── ReceiptPrinter.java       # Receipt interface + text impl
├── exception/                    # Custom exceptions
│   ├── ATMException.java
│   ├── InsufficientFundsException.java
│   ├── InsufficientCashException.java
│   ├── InvalidPINException.java
│   ├── CardBlockedException.java
│   ├── InvalidCardException.java
│   └── InvalidStateException.java
└── demo/
    └── ATMDemo.java              # 9 comprehensive scenarios
```

## Demo Scenarios

1. **Successful Withdrawal** — ₹2,700 dispensed as 2000+500+200
2. **Balance Inquiry** — Check savings account balance
3. **Cash Deposit** — Deposit 2×500 + 2×200 + 1×100 = ₹1,500
4. **Invalid PIN** — 3 failed attempts → card blocked
5. **Insufficient Funds** — Withdrawal exceeds balance
6. **Blocked Card** — Rejected at insertion
7. **Expired Card** — Rejected at insertion
8. **Cancel Transaction** — Mid-session cancellation
9. **Complex Withdrawal** — ₹4,300 dispensed as 2×2000 + 1×200 + 1×100

## Example Usage

```java
ATM atm = new ATM("ATM-001");

try {
    // Insert card
    atm.insertCard("1234-5678-9012-3456");
    
    // Enter PIN
    atm.enterPIN("1234");
    
    // Select account
    atm.selectAccount(AccountType.CHECKING);
    
    // Withdraw cash
    atm.performTransaction(TransactionType.CASH_WITHDRAWAL, new BigDecimal("2700"));
    
    // Eject card
    atm.cancel();
    
} catch (ATMException e) {
    System.out.println("Error: " + e.getMessage());
}
```

## Cash Dispensing Algorithm

The Chain of Responsibility ensures optimal denomination selection:

```
Request: ₹2,700
├── Dispenser2000: 1 note (₹2,000) → remaining ₹700
├── Dispenser500:  1 note (₹500)  → remaining ₹200
├── Dispenser200:  1 note (₹200)  → remaining ₹0 ✓
└── Dispenser100:  not needed

Result: {2000=1, 500=1, 200=1}
```

## Extending the System

### Add New Denomination (e.g., ₹50)

1. Add `NOTE_50(50)` to `Denomination` enum
2. Create `Dispenser50 extends CashDispenser`
3. Add to chain: `d100.setNext(new Dispenser50())`

### Add New Transaction Type

1. Add to `TransactionType` enum
2. Handle in `TransactionMenuState.performTransaction()`

### Add Digital Receipt

```java
public class EmailReceiptPrinter implements ReceiptPrinter {
    @Override
    public String printReceipt(Transaction t, BigDecimal balance) {
        // Format as email HTML
    }
}
```

## Thread Safety

| Component | Strategy |
|-----------|----------|
| `ATM` | Single session per instance; external synchronization if shared |
| `CashInventory` | `synchronized` methods |
| `Account` | `synchronized` balance operations |
| `CardManager` | `ConcurrentHashMap` |
| `AccountManager` | `ConcurrentHashMap` + `synchronized` on Account |

## Documentation

- `DESIGN.md` — Full D.I.C.E. workflow (Define, Identify, Code, Evolve)
- Class-level Javadoc on all public APIs

---

**Completed:** 2026-05-06 | **Patterns:** State, Chain of Responsibility, Strategy
