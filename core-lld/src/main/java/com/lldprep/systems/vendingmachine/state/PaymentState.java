package com.lldprep.systems.vendingmachine.state;

import com.lldprep.systems.vendingmachine.VendingMachine;
import com.lldprep.systems.vendingmachine.exception.*;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;

import java.math.BigDecimal;
import java.util.Map;

/**
 * PAYMENT state: Accepting money, can confirm or add more.
 * Valid operations: insertMoney(), confirmPurchase(), cancel()
 */
public class PaymentState implements VendingMachineState {
    public static final PaymentState INSTANCE = new PaymentState();

    private PaymentState() {}

    @Override
    public void selectProduct(VendingMachine vm, String productCode) throws VendingMachineException {
        throw new InvalidStateException(getStateName(), "selectProduct");
    }

    @Override
    public void insertMoney(VendingMachine vm, Denomination denomination) throws VendingMachineException {
        vm.getCashManager().acceptMoney(denomination);
        BigDecimal inserted = vm.getCashManager().getSessionAmount();
        BigDecimal price = vm.getSelectedProduct().getPrice();

        System.out.printf("Inserted: %s (Total: ₹%s)%n", denomination, inserted);

        if (inserted.compareTo(price) < 0) {
            BigDecimal needed = price.subtract(inserted);
            System.out.printf("Still need ₹%s more.%n", needed);
        } else {
            System.out.println("✓ Sufficient funds. Ready to dispense.");
        }
    }

    @Override
    public void confirmPurchase(VendingMachine vm) throws VendingMachineException {
        BigDecimal inserted = vm.getCashManager().getSessionAmount();
        BigDecimal price = vm.getSelectedProduct().getPrice();

        // Validate sufficient funds
        if (inserted.compareTo(price) < 0) {
            throw new InsufficientFundsException(price, inserted);
        }

        // Check if we can give change (if needed)
        if (inserted.compareTo(price) > 0) {
            if (!vm.getCashManager().canGiveChange(price)) {
                throw new InsufficientChangeException(inserted.subtract(price));
            }
        }

        // Transition to dispensing
        vm.setState(DispensingState.INSTANCE);
        vm.getCurrentState().confirmPurchase(vm); // Trigger dispensing
    }

    @Override
    public void cancel(VendingMachine vm) throws VendingMachineException {
        BigDecimal toReturn = vm.getCashManager().getSessionAmount();
        System.out.printf("Transaction cancelled. Returning ₹%s%n", toReturn);

        vm.getCashManager().resetSession(); // Returns money from inventory
        vm.resetSession();
        vm.setState(IdleState.INSTANCE);
    }

    @Override
    public String getStateName() {
        return "PAYMENT";
    }
}
