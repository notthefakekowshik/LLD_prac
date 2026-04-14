package com.lldprep.foundations.behavioral.state.good;

public class OutOfStockState implements VendingMachineState {

    @Override
    public void insertCoin(VendingMachine machine) {
        System.out.println("  Machine is out of stock. Returning coin.");
    }

    @Override
    public void selectProduct(VendingMachine machine) {
        System.out.println("  Machine is out of stock.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("  Machine is out of stock.");
    }

    @Override
    public String stateName() { return "OUT_OF_STOCK"; }
}
