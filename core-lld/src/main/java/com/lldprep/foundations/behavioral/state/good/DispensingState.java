package com.lldprep.foundations.behavioral.state.good;

public class DispensingState implements VendingMachineState {

    @Override
    public void insertCoin(VendingMachine machine) {
        System.out.println("  Wait, currently dispensing...");
    }

    @Override
    public void selectProduct(VendingMachine machine) {
        System.out.println("  Already dispensing. Please wait.");
    }

    @Override
    public void dispense(VendingMachine machine) {
        machine.decrementStock();
        System.out.println("  Product dispensed! Stock remaining: " + machine.getStock());

        if (machine.getStock() == 0) {
            System.out.println("  Machine is now out of stock.");
            machine.setState(new OutOfStockState());
        } else {
            machine.setState(new IdleState());
        }
    }

    @Override
    public String stateName() { return "DISPENSING"; }
}
