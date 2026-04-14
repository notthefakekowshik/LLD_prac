package com.lldprep.foundations.behavioral.state.bad;

/**
 * BAD: State is managed via a String/enum field with if-else chains in every method.
 *
 * Problems:
 * 1. Every action method has an if-else on state — logic is spread and hard to follow.
 * 2. Adding a new state (e.g., "MAINTENANCE") requires editing every single method.
 * 3. State transitions are implicit and easy to get wrong (no single place that owns them).
 * 4. Unit testing one state's behavior is impossible without navigating through all states.
 */
public class VendingMachineBad {

    private enum State { IDLE, HAS_COIN, DISPENSING, OUT_OF_STOCK }

    private State state = State.IDLE;
    private int stock = 3;

    public void insertCoin() {
        if (state == State.IDLE) {
            System.out.println("Coin inserted.");
            state = State.HAS_COIN;
        } else if (state == State.HAS_COIN) {
            System.out.println("Coin already inserted.");
        } else if (state == State.OUT_OF_STOCK) {
            System.out.println("Machine out of stock. Returning coin.");
        }
        // Adding new state → add else-if here. Repeat for selectProduct and dispense.
    }

    public void selectProduct() {
        if (state == State.HAS_COIN) {
            System.out.println("Product selected.");
            state = State.DISPENSING;
        } else if (state == State.IDLE) {
            System.out.println("Insert coin first.");
        } else if (state == State.OUT_OF_STOCK) {
            System.out.println("Out of stock.");
        }
    }

    public void dispense() {
        if (state == State.DISPENSING) {
            stock--;
            System.out.println("Dispensing product. Stock left: " + stock);
            state = (stock == 0) ? State.OUT_OF_STOCK : State.IDLE;
        } else {
            System.out.println("Select a product first.");
        }
    }
}
