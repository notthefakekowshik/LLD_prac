# Vending Machine — Design Document (D.I.C.E. Format)

A full-featured vending machine with product selection, multi-denomination payment, automatic change dispensing, and state machine workflow.

---

## Step 1 — DEFINE (Requirements & Constraints)

### Functional Requirements

1. **Product Selection** — User enters slot code (A1, B2, etc.) to select product
2. **Payment** — Accepts multiple denominations: coins (₹1, ₹5, ₹10) and notes (₹20, ₹50, ₹100)
3. **Incremental Payment** — Allow user to add money until sufficient
4. **Change Dispensing** — Automatically calculate and return optimal change mix
5. **Inventory Management** — Track quantity per slot, reject out-of-stock selections
6. **Exact Change Mode** — When low on coins, require exact amount
7. **Cancel Transaction** — Return all inserted money and reset session
8. **Transaction Logging** — Record all sales for audit

### Non-Functional Requirements

- **Thread-safe** — Concurrent inventory access must be safe
- **Fail-safe** — Return money on any failure
- **Extensible** — Easy to add new products, denominations, payment methods

### Constraints

- In-memory storage (no database)
- 15 slots (3 rows × 5 columns)
- Greedy change algorithm works for Indian currency

### Out of Scope

- Physical hardware interfaces
- Network payments (UPI, cards) — extensible via Strategy pattern
- Temperature control for cold items

---

## Step 2 — IDENTIFY (Entities & Relationships)

### Noun → Verb Extraction

> A **customer** *enters* a **product code** → the **machine** *validates* the **slot** → *checks* **inventory** → *displays* **price** → customer *inserts* **money** → machine *accumulates* **amount** → *checks* if **sufficient** → *calculates* **change** → *dispenses* **product** → *returns* **change** → *logs* **transaction** → returns to **idle**.

### Nouns → Candidate Entities

| Noun | Entity Type | Responsibility |
|------|-------------|----------------|
| Product | Model | SKU, name, price (immutable) |
| Slot | Model | Slot code, current product, quantity, capacity |
| CashInventory | Model | Denomination counts, change calculation |
| Transaction | Model | Sale record with Builder pattern |
| Denomination | Enum | COIN_1, COIN_5, COIN_10, NOTE_20, NOTE_50, NOTE_100 |
| ProductCode | Enum | A1-A5, B1-B5, C1-C5 |
| VendingMachine | Service | Main orchestrator, state holder, session manager |
| VendingMachineState | Interface | State pattern base |
| IdleState, ProductSelectedState, PaymentState, DispensingState | Classes | Concrete state implementations |
| ProductManager | Service | Slot lookup, inventory operations |
| CashManager | Service | Accept money, calculate change, session tracking |
| TransactionLogger | Service | Audit logging |

### Relationships

```
VendingMachine     ──has──►   VendingMachineState (current)        (Composition)
VendingMachine     ──uses──►  ProductManager                       (Association)
VendingMachine     ──uses──►  CashManager                          (Association)
VendingMachine     ──uses──►  TransactionLogger                    (Association)
VendingMachine     ──has──►    selectedProductCode, selectedProduct (Session state)

VendingMachineState ◄──implements── IdleState, PaymentState, ...   (Realization)

ProductManager     ──manages──► Slot[15]                           (Composition)
Slot               ──contains──► Product (nullable)               (Association)

CashManager        ──uses──►   CashInventory                       (Composition)
```

---

## Step 3 — CODE (Implementation Details)

### State Machine Design

```
                    selectProduct()                    insertMoney()
    ┌──────────┐  ─────────────────►  ┌────────────────┐ ───────────► ┌──────────┐
    │   IDLE   │                     │ PRODUCT_SELECTED │              │  PAYMENT  │
    └──────────┘  ◄─────────────────  └────────────────┘ ◄─────────── └──────────┘
           ▲         cancel()                │ cancel()                │  confirmPurchase()
           │                                  │                       │ (sufficient funds)
           │                                  └───────────────────────┘
           │                                                           │
           │                              ┌─────────────┐◄────────────┘
           └──────────────────────────────│ DISPENSING  │  dispense()
                                         └─────────────┘
                                                │
                                                ▼ (auto)
                                         ┌─────────────┐
                                         │    IDLE     │
                                         └─────────────┘
```

### Change Calculation Algorithm

Greedy approach works for canonical coin systems like Indian currency:

```java
public Map<Denomination, Integer> calculateChange(BigDecimal amount) {
    int remaining = amount.intValue();
    Map<Denomination, Integer> change = new HashMap<>();
    
    // Highest to lowest denomination
    Denomination[] values = {NOTE_100, NOTE_50, NOTE_20, COIN_10, COIN_5, COIN_1};
    
    for (Denomination d : values) {
        int count = remaining / d.getValue();
        if (count > 0 && inventory.has(d, count)) {
            change.put(d, count);
            remaining -= count * d.getValue();
        }
    }
    
    return remaining == 0 ? change : null; // null = can't make exact change
}
```

### Thread Safety Strategy

| Component | Synchronization |
|-----------|-----------------|
| `Slot.dispenseOne()` | `synchronized` method |
| `Slot.restock()` | `synchronized` method |
| `CashInventory.calculateChange()` | `synchronized` method |
| `CashInventory.addCash()` | `synchronized` method |
| `TransactionLogger` | `synchronizedList` wrapper |

### Exception Hierarchy

```
VendingMachineException (checked)
├── InsufficientFundsException
├── InsufficientChangeException
├── ProductOutOfStockException
├── InvalidProductException
└── InvalidStateException
```

---

## Step 4 — EVOLVE (Extensibility)

### Extension Points

1. **New Payment Method** — Implement `PaymentStrategy` interface
2. **New Denomination** — Add to enum, automatically picked up by loops
3. **Discount System** — Add `PricingStrategy` for time-based discounts
4. **Remote Monitoring** — Add `MetricsReporter` interface

### Future Enhancements

- **Promotions** — Buy 2 get 1 free, happy hour pricing
- **Cashless** — UPI, credit card, mobile wallet integration
- **Inventory Alerts** — Low stock notifications
- **Analytics** — Sales reports, popular items, peak hours

---

## File Count Summary

| Category | Count |
|----------|-------|
| Model + Enums | 6 |
| Exceptions | 6 |
| Services | 3 |
| States | 5 |
| Main + Demo | 2 |
| Documentation | 2 |
| **Total** | **24** |

---

**Design Completed:** 2026-05-17
