package com.lldprep.systems.vendingmachine;

import com.lldprep.systems.vendingmachine.exception.VendingMachineException;
import com.lldprep.systems.vendingmachine.model.CashInventory;
import com.lldprep.systems.vendingmachine.model.Product;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;
import com.lldprep.systems.vendingmachine.model.enums.ProductCode;
import com.lldprep.systems.vendingmachine.service.CashManager;
import com.lldprep.systems.vendingmachine.service.ProductManager;
import com.lldprep.systems.vendingmachine.service.TransactionLogger;
import com.lldprep.systems.vendingmachine.state.*;

import java.math.BigDecimal;

/**
 * Main orchestrator for the Vending Machine system.
 * Implements the State pattern for workflow management.
 * Single instance per physical machine.
 */
public class VendingMachine {
    private final String machineId;
    private VendingMachineState currentState;
    private final ProductManager productManager;
    private final CashInventory cashInventory;
    private final CashManager cashManager;
    private final TransactionLogger transactionLogger;

    // Session state
    private ProductCode selectedProductCode;
    private Product selectedProduct;

    public VendingMachine(String machineId) {
        this.machineId = machineId;
        this.currentState = IdleState.INSTANCE;
        this.cashInventory = new CashInventory();
        this.productManager = new ProductManager();
        this.cashManager = new CashManager(cashInventory);
        this.transactionLogger = new TransactionLogger();

        initializeInventory();
    }

    private void initializeInventory() {
        // Load initial cash for change: 50x1, 30x5, 20x10, 10x20, 5x50, 2x100 = ₹1060
        cashInventory.addCash(Denomination.COIN_1, 50);
        cashInventory.addCash(Denomination.COIN_5, 30);
        cashInventory.addCash(Denomination.COIN_10, 20);
        cashInventory.addCash(Denomination.NOTE_20, 10);
        cashInventory.addCash(Denomination.NOTE_50, 5);
        cashInventory.addCash(Denomination.NOTE_100, 2);

        // Load products
        productManager.loadProduct(ProductCode.A1, new Product("CH001", "Chips", new BigDecimal("20")), 5);
        productManager.loadProduct(ProductCode.A2, new Product("CH002", "Chocolate", new BigDecimal("45")), 5);
        productManager.loadProduct(ProductCode.A3, new Product("CO001", "Cookies", new BigDecimal("30")), 5);
        productManager.loadProduct(ProductCode.B1, new Product("SO001", "Soda", new BigDecimal("40")), 5);
        productManager.loadProduct(ProductCode.B2, new Product("WA001", "Water", new BigDecimal("20")), 5);
        productManager.loadProduct(ProductCode.B3, new Product("JU001", "Juice", new BigDecimal("50")), 5);
        productManager.loadProduct(ProductCode.C1, new Product("SN001", "Sandwich", new BigDecimal("80")), 3);
        productManager.loadProduct(ProductCode.C2, new Product("CA001", "Candy", new BigDecimal("10")), 10);
        productManager.loadProduct(ProductCode.C3, new Product("NU001", "Nuts", new BigDecimal("60")), 4);
    }

    // State transition
    public void setState(VendingMachineState state) {
        this.currentState = state;
        System.out.println("[STATE] → " + state.getStateName());
    }

    // Public API methods - delegate to state
    public void selectProduct(String productCode) throws VendingMachineException {
        currentState.selectProduct(this, productCode);
    }

    public void insertMoney(Denomination denomination) throws VendingMachineException {
        currentState.insertMoney(this, denomination);
    }

    public void confirmPurchase() throws VendingMachineException {
        currentState.confirmPurchase(this);
    }

    public void cancel() throws VendingMachineException {
        currentState.cancel(this);
    }

    // Session management
    public void resetSession() {
        this.selectedProductCode = null;
        this.selectedProduct = null;
    }

    // Display methods
    public void displayInventory() {
        System.out.println(productManager.getInventorySummary());
    }

    public void displayCashInventory() {
        System.out.println(cashInventory);
    }

    // Getters for state classes
    public ProductManager getProductManager() {
        return productManager;
    }

    public CashManager getCashManager() {
        return cashManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public CashInventory getCashInventory() {
        return cashInventory;
    }

    public ProductCode getSelectedProductCode() {
        return selectedProductCode;
    }

    public void setSelectedProductCode(ProductCode code) {
        this.selectedProductCode = code;
    }

    public Product getSelectedProduct() {
        return selectedProduct;
    }

    public void setSelectedProduct(Product product) {
        this.selectedProduct = product;
    }

    public VendingMachineState getCurrentState() {
        return currentState;
    }

    public String getCurrentStateName() {
        return currentState.getStateName();
    }

    public String getMachineId() {
        return machineId;
    }

    @Override
    public String toString() {
        return "VendingMachine{" +
            "id='" + machineId + '\'' +
            ", state=" + currentState.getStateName() +
            ", cash=" + cashInventory.getTotalValue() +
            '}';
    }
}
