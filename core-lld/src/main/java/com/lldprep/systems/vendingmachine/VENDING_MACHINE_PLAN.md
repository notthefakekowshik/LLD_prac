# Vending Machine System — Implementation Plan

> **Status**: PENDING REVIEW | **Theme**: State Pattern | **Reference**: ATM Machine

---

## Overview

A full-featured Vending Machine implementation demonstrating the **State** pattern with clean, extensible architecture similar to the ATM system.

---

## Step 1 — DEFINE (Requirements)

### Functional Requirements

1. **Product Selection** — User selects a product by code (A1, B2, etc.)
2. **Payment** — Accept coins (1, 5, 10) and notes (20, 50, 100)
3. **Insufficient Funds Handling** — Prompt for more money if selected product costs more than inserted
4. **Dispensing** — Release product and return change
5. **Inventory Management** — Track product quantities per slot
6. **Change Management** — Maintain coin/note inventory for returning change
7. **Cancel Transaction** — Return inserted money and reset session
8. **Out of Stock Handling** — Reject selection if product unavailable
9. **Exact Change Enforcement** — Optionally require exact change if machine low on coins
10. **Transaction Logging** — Record all sales for audit

### Non-Functional Requirements

- Thread-safe for concurrent access (multiple users can't use same machine, but inventory needs protection)
- Consistent inventory (no overselling)
- Fail-safe (return money on any failure)

### Constraints

- In-memory storage
- Fixed product slots (A1-A5, B1-B5, etc.)
- Limited change capacity

---

## Step 2 — IDENTIFY (Entities & Patterns)

### Design Patterns (Same as ATM)

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **State** | `VendingMachineState` interface + 4-5 concrete states | Enforce valid sequences (idle → selecting → paying → dispensing → idle) |
| **Strategy** | `PaymentStrategy` (coin vs note), `PricingStrategy` | Pluggable behaviors |
| **Singleton** | `VendingMachine` per physical machine | Single instance per machine |

### Nouns → Entities

| Noun | Entity Type | Responsibility |
|------|-------------|----------------|
| VendingMachine | Main Class | Orchestrator, holds current state, inventory, cash |
| Product | Model | SKU, name, price, quantity per slot |
| Slot | Model | Slot code (A1), current product, capacity |
| CashInventory | Model | Coin/note counts available for change |
| Coin/Note | Enum | Denominations accepted (1, 5, 10, 20, 50, 100) |
| Transaction | Model | Sale record with timestamp, product, amount |
| VendingMachineState | Interface | State pattern base |
| IdleState, ProductSelectedState, PaymentState, DispensingState | Classes | Concrete states |
| ProductManager | Service | Product lookup, inventory check, decrement |
| CashManager | Service | Accept money, calculate change, validate sufficient change |
| TransactionLogger | Service | Audit logging |

### State Machine

```
IDLE → selectProduct() → PRODUCT_SELECTED → insertMoney() → PAYMENT
  ↓                                                        ↓
IDLE ← cancel() ←────────────────────────── cancel() ←─────┘
  ↑                                                        ↓
  └──── dispense() ←───────────────────────── DISPENSING
```

**States:**
1. **IDLE** — Waiting for product selection
2. **PRODUCT_SELECTED** — Product chosen, waiting for payment
3. **PAYMENT** — Accepting coins/notes, may loop for more money
4. **DISPENSING** — Product released, change returned, transaction complete

---

## Step 3 — STRUCTURE (Package Layout)

```
com.lldprep.systems.vendingmachine/
├── VendingMachine.java              # Main orchestrator
├── README.md                        # User guide (like ATM)
├── DESIGN.md                        # D.I.C.E. document (like ATM)
├── model/
│   ├── Product.java                 # Product data
│   ├── Slot.java                    # Slot with product + quantity
│   ├── CashInventory.java           # Change inventory
│   ├── Transaction.java             # Sale record
│   └── enums/
│       ├── Denomination.java        # COIN_1, COIN_5, NOTE_20, etc.
│       └── ProductCode.java         # A1, A2, B1, etc.
├── state/                           # State Pattern
│   ├── VendingMachineState.java     # Interface
│   ├── IdleState.java
│   ├── ProductSelectedState.java
│   ├── PaymentState.java
│   └── DispensingState.java
├── service/
│   ├── ProductManager.java          # Inventory management
│   ├── CashManager.java             # Money handling
│   └── TransactionLogger.java       # Audit logging
├── exception/                       # Custom exceptions
│   ├── VendingMachineException.java # Base
│   ├── InsufficientFundsException.java
│   ├── InsufficientChangeException.java
│   ├── ProductOutOfStockException.java
│   ├── InvalidProductException.java
│   └── InvalidStateException.java
└── demo/
    └── VendingMachineDemo.java      # 8-10 scenarios
```

---

## Step 4 — APIs & Contracts

### VendingMachine Public API

```java
// Main lifecycle
public void selectProduct(String productCode) throws VendingMachineException;
public void insertMoney(Denomination denomination) throws VendingMachineException;
public void confirmPurchase() throws VendingMachineException;
public void cancel() throws VendingMachineException;

// Getters for state classes
public ProductManager getProductManager();
public CashManager getCashManager();
public TransactionLogger getTransactionLogger();
public int getInsertedAmount();  // Current money inserted
public Product getSelectedProduct();
```

### State Interface

```java
public interface VendingMachineState {
    void selectProduct(VendingMachine vm, String productCode) throws VendingMachineException;
    void insertMoney(VendingMachine vm, Denomination denomination) throws VendingMachineException;
    void confirmPurchase(VendingMachine vm) throws VendingMachineException;
    void cancel(VendingMachine vm) throws VendingMachineException;
    String getStateName();
}
```

---

## Step 5 — Demo Scenarios (10 scenarios like ATM)

| # | Scenario | Tests |
|---|----------|-------|
| 1 | Successful Purchase | Select → Insert exact → Dispense |
| 2 | Purchase with Change | Select (₹45) → Insert 50 → Get ₹5 change |
| 3 | Multiple Coins | Select → Insert 10+10+5 → Dispense |
| 4 | Insufficient Funds | Select (₹30) → Insert 20 → Prompt more → Insert 10 → OK |
| 5 | Cancel During Payment | Select → Insert 10 → Cancel → Return 10 |
| 6 | Out of Stock | Try to buy last item → OK → Try again → Out of stock |
| 7 | Insufficient Change | Machine low on coins → Require exact amount |
| 8 | Invalid Product Code | Try code "Z99" → Invalid |
| 9 | Cancel at Selection | Select → Cancel → Return to idle |
| 10 | Exact Change Required | Machine has no coins → ₹30 item requires ₹30 exact |

---

## Step 6 — File Checklist (Implementation Order)

### Phase 1: Foundation (Model + Enums)
- [ ] `Denomination.java` — enum with values 1, 5, 10, 20, 50, 100
- [ ] `ProductCode.java` — enum/grid of slot codes A1-A5, B1-B5
- [ ] `Product.java` — name, price, SKU
- [ ] `Slot.java` — slot code, current product, quantity
- [ ] `CashInventory.java` — Map<Denomination, Integer> counts
- [ ] `Transaction.java` — timestamp, product, amount, change given

### Phase 2: Exceptions
- [ ] `VendingMachineException.java` — base checked exception
- [ ] `InsufficientFundsException.java` — money inserted < price
- [ ] `InsufficientChangeException.java` — can't give change
- [ ] `ProductOutOfStockException.java` — slot empty
- [ ] `InvalidProductException.java` — bad product code
- [ ] `InvalidStateException.java` — wrong operation for state

### Phase 3: Services
- [ ] `ProductManager.java` — slot lookup, inventory check, decrement stock
- [ ] `CashManager.java` — accept money, calculate change, hasSufficientChange()
- [ ] `TransactionLogger.java` — log sale, get transaction history

### Phase 4: State Pattern
- [ ] `VendingMachineState.java` — interface
- [ ] `IdleState.java` — only selectProduct() valid
- [ ] `ProductSelectedState.java` — insertMoney() or cancel()
- [ ] `PaymentState.java` — insertMoney(), confirmPurchase(), cancel()
- [ ] `DispensingState.java` — dispense, return change, log, reset

### Phase 5: Main + Demo
- [ ] `VendingMachine.java` — orchestrator, state holder
- [ ] `VendingMachineDemo.java` — 10 scenarios with headers

### Phase 6: Documentation
- [ ] `README.md` — features, patterns, quick start, scenarios
- [ ] `DESIGN.md` — D.I.C.E. format (Define, Identify, Code, Evolve)

---

## Comparison with ATM

| Aspect | ATM | Vending Machine |
|--------|-----|-----------------|
| **State trigger** | Card insert | Product selection |
| **Input collection** | PIN entry | Money insertion (iterative) |
| **Final action** | Cash dispensing | Product + change dispensing |
| **Complexity** | High (accounts, cards, PIN) | Medium (products, change) |
| **Patterns** | State, Chain of Responsibility, Strategy | State, Strategy |

---

## Estimated Lines of Code

| Component | Files | Est. Lines |
|-----------|-------|------------|
| Model + Enums | 6 | ~250 |
| Exceptions | 6 | ~60 |
| Services | 3 | ~200 |
| States | 5 | ~250 |
| Main + Demo | 2 | ~400 |
| Documentation | 2 | ~400 |
| **Total** | **24** | **~1560** |

---

## Review Checklist for Approval

- [ ] State machine covers all valid/invalid transitions
- [ ] Demo scenarios are comprehensive (edge cases included)
- [ ] Package structure matches ATM convention
- [ ] Documentation plan includes README + DESIGN.md
- [ ] Thread safety considered for inventory

**Awaiting your review and approval to proceed with implementation.**
