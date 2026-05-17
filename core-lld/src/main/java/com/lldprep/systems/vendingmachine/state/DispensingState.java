package com.lldprep.systems.vendingmachine.state;

import com.lldprep.systems.vendingmachine.VendingMachine;
import com.lldprep.systems.vendingmachine.exception.*;
import com.lldprep.systems.vendingmachine.model.Product;
import com.lldprep.systems.vendingmachine.model.Transaction;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;
import com.lldprep.systems.vendingmachine.model.enums.ProductCode;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DISPENSING state: Finalizing transaction, dispensing product and change.
 * This is an automatic state that immediately completes.
 */
public class DispensingState implements VendingMachineState {
    public static final DispensingState INSTANCE = new DispensingState();

    private DispensingState() {}

    @Override
    public void selectProduct(VendingMachine vm, String productCode) throws VendingMachineException {
        throw new InvalidStateException(getStateName(), "selectProduct");
    }

    @Override
    public void insertMoney(VendingMachine vm, Denomination denomination) throws VendingMachineException {
        throw new InvalidStateException(getStateName(), "insertMoney");
    }

    @Override
    public void confirmPurchase(VendingMachine vm) throws VendingMachineException {
        ProductCode code = vm.getSelectedProductCode();
        Product product = vm.getSelectedProduct();
        BigDecimal price = product.getPrice();
        BigDecimal inserted = vm.getCashManager().getSessionAmount();

        try {
            // 1. Dispense product
            vm.getProductManager().dispenseOne(code);
            System.out.println("\n╔════════════════════════════════════╗");
            System.out.println("║         PRODUCT DISPENSED          ║");
            System.out.println("╠════════════════════════════════════╣");
            System.out.printf("║  %s%n", product.getName());
            System.out.println("╚════════════════════════════════════╝\n");

            // 2. Calculate and dispense change
            Map<Denomination, Integer> change = null;
            BigDecimal changeAmount = inserted.subtract(price);

            if (changeAmount.compareTo(BigDecimal.ZERO) > 0) {
                change = vm.getCashManager().dispenseChange(price);
                if (change != null && !change.isEmpty()) {
                    System.out.println("Change dispensed:");
                    for (Map.Entry<Denomination, Integer> entry : change.entrySet()) {
                        System.out.printf("  %d x %s%n", entry.getValue(), entry.getKey());
                    }
                }
            }

            // 3. Log transaction
            Transaction transaction = Transaction.builder()
                .slotCode(code)
                .productName(product.getName())
                .productPrice(price)
                .amountInserted(inserted)
                .changeReturned(changeAmount)
                .changeBreakdown(change)
                .status(Transaction.Status.SUCCESS)
                .build();

            vm.getTransactionLogger().log(transaction);
            vm.getCashManager().finalizeTransaction();

            System.out.printf("%n✓ Transaction complete. Thank you!%n");

        } catch (Exception e) {
            // Log failed transaction
            Transaction failedTx = Transaction.builder()
                .slotCode(code)
                .productName(product.getName())
                .productPrice(price)
                .amountInserted(inserted)
                .status(Transaction.Status.FAILED_OUT_OF_STOCK)
                .build();
            vm.getTransactionLogger().log(failedTx);

            // Return money
            vm.getCashManager().resetSession();
            throw new VendingMachineException("Failed to dispense: " + e.getMessage(), e);
        } finally {
            // Always reset session
            vm.resetSession();
            vm.setState(IdleState.INSTANCE);
        }
    }

    @Override
    public void cancel(VendingMachine vm) throws VendingMachineException {
        // Too late to cancel - already dispensing
        throw new InvalidStateException(getStateName(), "cancel");
    }

    @Override
    public String getStateName() {
        return "DISPENSING";
    }
}
