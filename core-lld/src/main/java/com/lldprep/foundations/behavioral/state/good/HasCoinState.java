package com.lldprep.foundations.behavioral.state.good;

public class HasCoinState implements VendingMachineState {

    @Override
    public void insertCoin(VendingMachine machine) {
        System.out.println("  Coin already inserted. Returning extra coin.");
    }

    @Override
    public void selectProduct(VendingMachine machine) {
        System.out.println("  Product selected. Dispensing...");
        machine.setState(new DispensingState());
    }

    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("  Please select a product first.");
    }

    @Override
    public String stateName() { return "HAS_COIN"; }
}
