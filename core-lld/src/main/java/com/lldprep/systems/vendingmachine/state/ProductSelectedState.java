package com.lldprep.systems.vendingmachine.state;

import com.lldprep.systems.vendingmachine.VendingMachine;
import com.lldprep.systems.vendingmachine.exception.InvalidStateException;
import com.lldprep.systems.vendingmachine.exception.VendingMachineException;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;

import java.math.BigDecimal;

/**
 * PRODUCT_SELECTED state: Product chosen, waiting for payment.
 * Valid operations: insertMoney(), cancel()
 */
public class ProductSelectedState implements VendingMachineState {
    public static final ProductSelectedState INSTANCE = new ProductSelectedState();

    private ProductSelectedState() {}

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

        // Transition to PAYMENT state
        vm.setState(PaymentState.INSTANCE);

        // Check if sufficient funds
        if (inserted.compareTo(price) < 0) {
            BigDecimal needed = price.subtract(inserted);
            System.out.printf("Insufficient funds. Need ₹%s more.%n", needed);
        } else {
            System.out.println("Sufficient funds. Press confirm to dispense.");
        }
    }

    @Override
    public void confirmPurchase(VendingMachine vm) throws VendingMachineException {
        throw new InvalidStateException(getStateName(), "confirmPurchase");
    }

    @Override
    public void cancel(VendingMachine vm) throws VendingMachineException {
        System.out.println("Transaction cancelled. No money to return.");
        vm.resetSession();
        vm.setState(IdleState.INSTANCE);
    }

    @Override
    public String getStateName() {
        return "PRODUCT_SELECTED";
    }
}
