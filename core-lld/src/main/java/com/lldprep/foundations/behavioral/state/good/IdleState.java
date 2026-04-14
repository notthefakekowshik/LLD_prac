package com.lldprep.foundations.behavioral.state.good;

public class IdleState implements VendingMachineState {

    @Override
    public void insertCoin(VendingMachine machine) {
        System.out.println("  Coin inserted. Ready to select product.");
        machine.setState(new HasCoinState());
    }

    @Override
    public void selectProduct(VendingMachine machine) {
        System.out.println("  Please insert a coin first.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        System.out.println("  Please insert a coin first.");
    }

    @Override
    public String stateName() { return "IDLE"; }
}
