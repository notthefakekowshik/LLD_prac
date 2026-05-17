package com.lldprep.systems.vendingmachine.state;

import com.lldprep.systems.vendingmachine.VendingMachine;
import com.lldprep.systems.vendingmachine.exception.VendingMachineException;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;

/**
 * State interface for the Vending Machine state pattern.
 * Each state implements only the valid operations for that state.
 */
public interface VendingMachineState {

    /**
     * Select a product by code.
     * Valid in: IDLE
     */
    void selectProduct(VendingMachine vm, String productCode) throws VendingMachineException;

    /**
     * Insert money (coin or note).
     * Valid in: PRODUCT_SELECTED, PAYMENT
     */
    void insertMoney(VendingMachine vm, Denomination denomination) throws VendingMachineException;

    /**
     * Confirm purchase and dispense product.
     * Valid in: PAYMENT (when sufficient funds)
     */
    void confirmPurchase(VendingMachine vm) throws VendingMachineException;

    /**
     * Cancel transaction and return money.
     * Valid in: PRODUCT_SELECTED, PAYMENT
     */
    void cancel(VendingMachine vm) throws VendingMachineException;

    /**
     * Get state name for logging/debugging.
     */
    String getStateName();
}
