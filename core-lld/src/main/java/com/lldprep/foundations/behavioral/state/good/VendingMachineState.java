package com.lldprep.foundations.behavioral.state.good;

/**
 * State interface — each concrete state class handles actions for that specific state.
 * State transitions are decided inside the state itself, not in giant if-else blocks.
 */
public interface VendingMachineState {
    void insertCoin(VendingMachine machine);
    void selectProduct(VendingMachine machine);
    void dispense(VendingMachine machine);
    String stateName();
}
