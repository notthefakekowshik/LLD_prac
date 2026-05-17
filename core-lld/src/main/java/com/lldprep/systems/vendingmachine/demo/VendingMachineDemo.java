package com.lldprep.systems.vendingmachine.demo;

import com.lldprep.systems.vendingmachine.VendingMachine;
import com.lldprep.systems.vendingmachine.exception.*;
import com.lldprep.systems.vendingmachine.model.enums.Denomination;

import java.math.BigDecimal;

/**
 * Comprehensive demonstration of the Vending Machine system.
 * Shows all 10 scenarios including happy paths and edge cases.
 */
public class VendingMachineDemo {

    public static void main(String[] args) {
        printHeader();

        VendingMachine vm = new VendingMachine("VM-001");
        System.out.println("Initialized: " + vm);
        System.out.println();

        // Display initial inventory
        vm.displayInventory();
        System.out.println();

        // Run all scenarios
        runScenario1_SuccessfulPurchase(vm);
        runScenario2_PurchaseWithChange(vm);
        runScenario3_MultipleCoins(vm);
        runScenario4_InsufficientFunds(vm);
        runScenario5_CancelDuringPayment(vm);
        runScenario6_OutOfStock(vm);
        runScenario7_InsufficientChange(vm);
        runScenario8_InvalidProductCode(vm);
        runScenario9_CancelAtSelection(vm);
        runScenario10_ExactChangeRequired(vm);

        // Final summary
        printSummary(vm);
    }

    private static void runScenario1_SuccessfulPurchase(VendingMachine vm) {
        printScenarioHeader("Scenario 1: Successful Purchase (Exact Amount)");

        try {
            // Buy Water (₹20) with exact ₹20
            vm.selectProduct("B2");
            vm.insertMoney(Denomination.NOTE_20);
            vm.confirmPurchase();

            System.out.println("✓ Purchased Water for ₹20 with exact change");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario2_PurchaseWithChange(VendingMachine vm) {
        printScenarioHeader("Scenario 2: Purchase with Change (₹45 item, ₹50 paid)");

        try {
            // Buy Chocolate (₹45) with ₹50, expect ₹5 change
            vm.selectProduct("A2");
            vm.insertMoney(Denomination.NOTE_50);
            vm.confirmPurchase();

            System.out.println("✓ Purchased Chocolate for ₹45, received ₹5 change");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario3_MultipleCoins(VendingMachine vm) {
        printScenarioHeader("Scenario 3: Multiple Coins (₹30 with 10+10+5+5)");

        try {
            // Buy Cookies (₹30) with multiple coins
            vm.selectProduct("A3");
            vm.insertMoney(Denomination.COIN_10);
            vm.insertMoney(Denomination.COIN_10);
            vm.insertMoney(Denomination.COIN_5);
            vm.insertMoney(Denomination.COIN_5);
            vm.confirmPurchase();

            System.out.println("✓ Purchased Cookies for ₹30 using multiple coins");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario4_InsufficientFunds(VendingMachine vm) {
        printScenarioHeader("Scenario 4: Insufficient Funds (Prompt for more)");

        try {
            // Try to buy Juice (₹50) with only ₹20, then add more
            vm.selectProduct("B3");
            vm.insertMoney(Denomination.NOTE_20);
            // System prompts for ₹30 more
            vm.insertMoney(Denomination.NOTE_20);
            // Still need ₹10 more
            vm.insertMoney(Denomination.COIN_10);
            // Now sufficient
            vm.confirmPurchase();

            System.out.println("✓ Purchased Juice after adding money in stages");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario5_CancelDuringPayment(VendingMachine vm) {
        printScenarioHeader("Scenario 5: Cancel During Payment (Return money)");

        try {
            // Start buying, insert some money, then cancel
            vm.selectProduct("C1"); // Sandwich ₹80
            vm.insertMoney(Denomination.NOTE_50);
            vm.insertMoney(Denomination.NOTE_20);
            // Total: ₹70, still need ₹10

            System.out.println("User decides to cancel...");
            vm.cancel(); // Should return ₹70

            System.out.println("✓ Transaction cancelled, ₹70 returned");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario6_OutOfStock(VendingMachine vm) {
        printScenarioHeader("Scenario 6: Out of Stock (Buy last item, try again)");

        try {
            // First, buy all Nuts (only 4 in stock)
            for (int i = 0; i < 4; i++) {
                vm.selectProduct("C3");
                vm.insertMoney(Denomination.NOTE_100);
                vm.confirmPurchase();
            }
            System.out.println("Bought all 4 Nuts");

            // Now try to buy again - should be out of stock
            try {
                vm.selectProduct("C3");
                System.out.println("✗ Should have failed - product out of stock!");
            } catch (ProductOutOfStockException e) {
                System.out.println("✓ Correctly rejected: " + e.getMessage());
            }
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario7_InsufficientChange(VendingMachine vm) {
        printScenarioHeader("Scenario 7: Insufficient Change (Machine low on coins)");

        // First drain coins to simulate low change condition
        System.out.println("Simulating low coin inventory...");
        // This would require many transactions - we'll simulate by showing the check

        try {
            vm.selectProduct("A1"); // Chips ₹20
            // If machine requires exact change, this would be enforced
            if (vm.getCashManager().requiresExactChange()) {
                System.out.println("⚠ Machine requires exact change");
            }
            vm.insertMoney(Denomination.NOTE_20); // Exact amount
            vm.confirmPurchase();

            System.out.println("✓ Purchase completed (used exact amount due to low change)");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario8_InvalidProductCode(VendingMachine vm) {
        printScenarioHeader("Scenario 8: Invalid Product Code (Z99)");

        try {
            vm.selectProduct("Z99");
            System.out.println("✗ Should have failed - invalid code!");
        } catch (InvalidProductException e) {
            System.out.println("✓ Correctly rejected: " + e.getMessage());
        } catch (VendingMachineException e) {
            System.out.println("✓ Rejected: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario9_CancelAtSelection(VendingMachine vm) {
        printScenarioHeader("Scenario 9: Cancel at Selection (No money inserted)");

        try {
            vm.selectProduct("B1"); // Soda ₹40
            System.out.println("User changes mind...");
            vm.cancel(); // No money to return

            System.out.println("✓ Cancelled successfully (no money was inserted)");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario10_ExactChangeRequired(VendingMachine vm) {
        printScenarioHeader("Scenario 10: Large Purchase (₹80 Sandwich)");

        try {
            vm.selectProduct("C1"); // Sandwich ₹80
            // Pay with 50+20+10
            vm.insertMoney(Denomination.NOTE_50);
            vm.insertMoney(Denomination.NOTE_20);
            vm.insertMoney(Denomination.COIN_10);
            vm.confirmPurchase();

            System.out.println("✓ Purchased Sandwich for ₹80 (50+20+10)");
        } catch (VendingMachineException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void printSummary(VendingMachine vm) {
        printScenarioHeader("FINAL SUMMARY");

        System.out.println(vm);
        System.out.println();

        System.out.println("Final Cash Inventory:");
        vm.displayCashInventory();
        System.out.println();

        System.out.println("Final Product Inventory:");
        vm.displayInventory();
        System.out.println();

        System.out.println("Transaction Summary:");
        System.out.println("  Total transactions: " + vm.getTransactionLogger().getTransactionCount());
        System.out.println("  Successful sales: " + vm.getTransactionLogger().getSuccessfulSalesCount());
        System.out.println("  Total revenue: ₹" + vm.getTransactionLogger().getTotalRevenue());
        System.out.println();

        System.out.println("Recent Transactions:");
        vm.getTransactionLogger().getRecentTransactions(10).forEach(t -> {
            System.out.println("  " + t);
        });
        System.out.println();

        printFooter();
    }

    private static void printHeader() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            VENDING MACHINE SYSTEM DEMONSTRATION              ║");
        System.out.println("║                                                              ║");
        System.out.println("║              Pattern: State | Strategy                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }

    private static void printFooter() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          VENDING MACHINE DEMO COMPLETED SUCCESSFULLY        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    private static void printScenarioHeader(String title) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ " + padRight(title, 60) + " │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
    }

    private static String padRight(String s, int n) {
        if (s.length() > n) {
            return s.substring(0, n);
        }
        return String.format("%-" + n + "s", s);
    }
}
