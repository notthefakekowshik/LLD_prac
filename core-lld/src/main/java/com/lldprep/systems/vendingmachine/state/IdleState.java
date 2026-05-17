package com.lldprep.systems.vendingmachine.state;

import com.lldprep.systems.vendingmachine.VendingMachine;
import com.lldprep.systems.vendingmachine.exception.*;
import com.lldprep.systems.vendingmachine.model.Product;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;
import com.lldprep.systems.vendingmachine.model.enums.ProductCode;

/**
 * IDLE state: Waiting for product selection.
 * Valid operations: selectProduct()
 */
public class IdleState implements VendingMachineState {
    public static final IdleState INSTANCE = new IdleState();

    private IdleState() {}

    @Override
    public void selectProduct(VendingMachine vm, String productCode) throws VendingMachineException {
        // Parse and validate product code
        ProductCode code;
        try {
            code = ProductCode.fromString(productCode);
        } catch (IllegalArgumentException e) {
            throw new InvalidProductException(productCode);
        }

        // Check if product is available
        if (!vm.getProductManager().isAvailable(code)) {
            Product product = vm.getProductManager().getProduct(code);
            if (product == null) {
                throw new InvalidProductException(productCode);
            }
            throw new ProductOutOfStockException(code);
        }

        // Set selected product and transition
        vm.setSelectedProductCode(code);
        vm.setSelectedProduct(vm.getProductManager().getProduct(code));
        vm.setState(ProductSelectedState.INSTANCE);

        Product product = vm.getSelectedProduct();
        System.out.printf("Selected: %s (₹%s)%n", product.getName(), product.getPrice());

        // Check if exact change required
        if (vm.getCashManager().requiresExactChange()) {
            System.out.println("⚠ Exact change required. Machine low on coins.");
        }
    }

    @Override
    public void insertMoney(VendingMachine vm, Denomination denomination) throws VendingMachineException {
        throw new InvalidStateException(getStateName(), "insertMoney");
    }

    @Override
    public void confirmPurchase(VendingMachine vm) throws VendingMachineException {
        throw new InvalidStateException(getStateName(), "confirmPurchase");
    }

    @Override
    public void cancel(VendingMachine vm) throws VendingMachineException {
        // Already idle, nothing to cancel
        System.out.println("No active session to cancel.");
    }

    @Override
    public String getStateName() {
        return "IDLE";
    }
}
