package com.lldprep.foundations.behavioral.state.good;

/**
 * State Pattern — Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * An object behaves very differently depending on its internal state, and you have many states.
 * Without State pattern, you end up with massive if-else chains in every method.
 * Every new state requires editing every method — unmaintainable at scale.
 *
 * <p><b>How it works:</b><br>
 * - Each state is a separate class implementing the {@code VendingMachineState} interface.<br>
 * - The context (VendingMachine) holds a reference to the current state and delegates all actions to it.<br>
 * - State classes transition the context by calling {@code machine.setState(new NextState())}.<br>
 * - The machine itself has zero if-else logic.
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>An object has 3+ distinct states with significantly different behavior per state.</li>
 *   <li>State transitions have rules — only certain transitions are valid.</li>
 *   <li>The number of states or transitions is likely to grow (e.g., adding MAINTENANCE state).</li>
 * </ul>
 *
 * <p><b>State vs Strategy (most common confusion):</b><br>
 * - {@code State}: behavior changes <i>from inside</i> — states know about each other, drive transitions.<br>
 * - {@code Strategy}: algorithm is <i>injected from outside</i> — strategies don't know each other, context is stable.
 *
 * <p><b>Real-world examples:</b> ATM (Idle/CardInserted/PINEntered/Dispensing),
 * Order lifecycle (Placed/Confirmed/Shipped/Delivered/Cancelled), TCP connection states.
 *
 * <p><b>Covered variations:</b>
 * <ol>
 *   <li>Normal happy path: insert coin → select → dispense</li>
 *   <li>Invalid transitions handled gracefully per state</li>
 *   <li>Auto-transition to OutOfStock when stock hits 0</li>
 *   <li>Evolve: adding a new state requires only a new class</li>
 * </ol>
 */
public class StateDemo {

    public static void main(String[] args) {
        demo1_HappyPath();
        demo2_InvalidTransitions();
        demo3_DrainStock();
    }

    // -------------------------------------------------------------------------

    private static void demo1_HappyPath() {
        section("Demo 1: Happy path — insert coin → select → dispense");

        VendingMachine machine = new VendingMachine(3);
        machine.insertCoin();
        machine.selectProduct();
        machine.dispense();
    }

    private static void demo2_InvalidTransitions() {
        section("Demo 2: Invalid transitions handled gracefully");

        VendingMachine machine = new VendingMachine(3);

        // Try to select/dispense without inserting a coin
        machine.selectProduct(); // blocked by IdleState
        machine.dispense();      // blocked by IdleState

        machine.insertCoin();
        machine.insertCoin();    // duplicate coin — HasCoinState handles it
        machine.selectProduct();
        machine.insertCoin();    // mid-dispense — DispensingState handles it
        machine.dispense();
    }

    private static void demo3_DrainStock() {
        section("Demo 3: Machine transitions to OutOfStock automatically");

        VendingMachine machine = new VendingMachine(2);

        for (int i = 1; i <= 3; i++) {
            System.out.println("\n  --- Purchase attempt " + i + " ---");
            machine.insertCoin();
            machine.selectProduct();
            machine.dispense();
        }
    }

    // -------------------------------------------------------------------------

    private static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }
}
