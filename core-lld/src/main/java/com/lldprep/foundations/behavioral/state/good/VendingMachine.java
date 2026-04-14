package com.lldprep.foundations.behavioral.state.good;

/**
 * Context — delegates all actions to the current state.
 * The machine itself has no if-else; all behavior lives in the state classes.
 */
public class VendingMachine {

    private VendingMachineState currentState;
    private int stock;

    public VendingMachine(int stock) {
        this.stock = stock;
        this.currentState = (stock > 0) ? new IdleState() : new OutOfStockState();
    }

    public void setState(VendingMachineState state) {
        System.out.printf("    [State transition: %s → %s]%n",
                this.currentState.stateName(), state.stateName());
        this.currentState = state;
    }

    public void insertCoin()    { currentState.insertCoin(this); }
    public void selectProduct() { currentState.selectProduct(this); }
    public void dispense()      { currentState.dispense(this); }

    public void decrementStock() { stock--; }
    public int  getStock()       { return stock; }
    public String getStateName() { return currentState.stateName(); }
}
