# Vending Machine System

A full-featured Vending Machine implementation demonstrating the **State** pattern with a clean, extensible architecture.

## Features

- **Product Selection** — Select products by slot code (A1, B2, etc.)
- **Payment** — Accepts coins (₹1, ₹5, ₹10) and notes (₹20, ₹50, ₹100)
- **Change Management** — Automatic change calculation and dispensing
- **Inventory Tracking** — Per-slot quantity management
- **Exact Change Mode** — Enforces exact payment when machine is low on coins
- **Transaction Logging** — Immutable audit trail
- **State Machine** — Type-safe state transitions prevent invalid operations

## Design Patterns

| Pattern | Implementation | Purpose |
|---------|---------------|---------|
| **State** | `VendingMachineState` interface + 4 concrete states | Enforces valid operation sequences (can't dispense before payment) |
| **Strategy** | `PaymentStrategy` ready for extension | Pluggable payment methods |
| **Builder** | `Transaction.Builder` | Clean transaction creation |

## State Machine

```
IDLE → selectProduct() → PRODUCT_SELECTED → insertMoney() → PAYMENT
  ↑                                                        ↓ confirmPurchase()
  └──── cancel() ←──────────────────────── cancel() ←───────┘ dispense() → DISPENSING → IDLE
```

**States:**
1. **IDLE** — Waiting for product selection
2. **PRODUCT_SELECTED** — Product chosen, waiting for first payment
3. **PAYMENT** — Accepting additional money, can confirm or cancel
4. **DISPENSING** — Automatic state, dispenses product and change

Invalid operations throw `InvalidStateException` — enforced at runtime via the State pattern.

## Quick Start

```bash
# Compile and run demo
cd /Users/knarayanam/backup_folder/MY_FILES/LLD_prac
mvn compile exec:java -Dexec.mainClass="com.lldprep.systems.vendingmachine.demo.VendingMachineDemo" -f core-lld/pom.xml
```

## Package Structure

```
com.lldprep.systems.vendingmachine/
├── VendingMachine.java              # Main orchestrator
├── model/
│   ├── Product.java                 # Product data (SKU, name, price)
│   ├── Slot.java                    # Slot with product + quantity
│   ├── CashInventory.java           # Change inventory with greedy algorithm
│   ├── Transaction.java             # Sale record with Builder pattern
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
│   ├── CashManager.java             # Money handling + change calculation
│   └── TransactionLogger.java       # Audit logging
├── exception/                       # Custom exceptions
│   ├── VendingMachineException.java # Base
│   ├── InsufficientFundsException.java
│   ├── InsufficientChangeException.java
│   ├── ProductOutOfStockException.java
│   ├── InvalidProductException.java
│   └── InvalidStateException.java
└── demo/
    └── VendingMachineDemo.java      # 10 comprehensive scenarios
```

## Demo Scenarios

1. **Successful Purchase** — Exact payment, no change
2. **Purchase with Change** — ₹45 item, ₹50 paid, ₹5 returned
3. **Multiple Coins** — ₹30 item paid with 10+10+5+5
4. **Insufficient Funds** — Add money in stages until sufficient
5. **Cancel During Payment** — Return inserted money
6. **Out of Stock** — Buy last item, verify second attempt fails
7. **Insufficient Change** — Exact change mode when low on coins
8. **Invalid Product Code** — Z99 rejected
9. **Cancel at Selection** — Cancel before inserting money
10. **Large Purchase** — ₹80 sandwich with 50+20+10

## Example Usage

```java
VendingMachine vm = new VendingMachine("VM-001");

try {
    // Display available products
    vm.displayInventory();
    
    // Select product
    vm.selectProduct("A2");  // Chocolate ₹45
    
    // Insert money
    vm.insertMoney(Denomination.NOTE_50);
    
    // Confirm purchase
    vm.confirmPurchase();  // Dispenses Chocolate + ₹5 change
    
} catch (VendingMachineException e) {
    System.out.println("Error: " + e.getMessage());
}
```

## Change Calculation Algorithm

Uses greedy algorithm (works for Indian currency denominations):

```
Request: Return ₹15 change
├── Try NOTE_20: 0 (15 < 20)
├── Try COIN_10: 1 note (₹10) → remaining ₹5
├── Try COIN_5:  1 coin (₹5)  → remaining ₹0 ✓
└── Done: {COIN_10=1, COIN_5=1}
```

## Extending the System

### Add New Denomination

1. Add to `Denomination` enum
2. Update `CashInventory.calculateChange()` to include it in the loop

### Add New Product

```java
vm.getProductManager().loadProduct(
    ProductCode.C4,
    new Product("SN002", "Protein Bar", new BigDecimal("45")),
    5  // quantity
);
```

### Add Digital Payment

```java
public interface PaymentStrategy {
    boolean processPayment(BigDecimal amount);
}

public class UPIPaymentStrategy implements PaymentStrategy {
    // Implementation
}
```

## Thread Safety

| Component | Strategy |
|-----------|----------|
| `Slot` | `synchronized` methods for inventory |
| `CashInventory` | `ConcurrentHashMap` + `synchronized` for change calculation |
| `TransactionLogger` | `synchronizedList` |
| `VendingMachine` | Single session per instance |

## Documentation

- `DESIGN.md` — Full D.I.C.E. workflow (Define, Identify, Code, Evolve)

---

**Completed:** 2026-05-17 | **Patterns:** State, Builder, Strategy
