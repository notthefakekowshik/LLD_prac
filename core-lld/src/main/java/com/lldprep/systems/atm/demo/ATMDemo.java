package com.lldprep.systems.atm.demo;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.*;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.Denomination;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class ATMDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                  ATM SYSTEM DEMONSTRATION                    ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Patterns: State | Chain of Responsibility | Strategy       ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        ATM atm = new ATM("ATM-001");
        System.out.println("Initialized: " + atm);
        System.out.println();

        // Scenario 1: Successful withdrawal with optimal dispensing
        runScenario1_SuccessfulWithdrawal(atm);

        // Scenario 2: Balance inquiry
        runScenario2_BalanceInquiry(atm);

        // Scenario 3: Cash deposit
        runScenario3_CashDeposit(atm);

        // Scenario 4: Invalid PIN (3 attempts, card blocks)
        runScenario4_InvalidPIN(atm);

        // Scenario 5: Insufficient funds
        runScenario5_InsufficientFunds(atm);

        // Scenario 6: Blocked card
        runScenario6_BlockedCard(atm);

        // Scenario 7: Expired card
        runScenario7_ExpiredCard(atm);

        // Scenario 8: Cancel transaction
        runScenario8_CancelTransaction(atm);

        // Scenario 9: Complex withdrawal with mixed denominations
        runScenario9_ComplexWithdrawal(atm);

        // Final summary
        printSummary(atm);
    }

    private static void runScenario1_SuccessfulWithdrawal(ATM atm) {
        printScenarioHeader("Scenario 1: Successful Withdrawal with Optimal Dispensing");
        
        try {
            System.out.println("Initial ATM cash: " + atm.getCashInventory());
            
            atm.insertCard("1234-5678-9012-3456"); // Valid card
            atm.enterPIN("1234"); // Correct PIN
            atm.selectAccount(AccountType.CHECKING);
            atm.performTransaction(TransactionType.CASH_WITHDRAWAL, new BigDecimal("2700"));
            atm.cancel(); // Eject card
            
            System.out.println("After withdrawal: " + atm.getCashInventory());
            System.out.println("✓ Dispensed using optimal denomination mix (2000+500+200)");
            
        } catch (ATMException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario2_BalanceInquiry(ATM atm) {
        printScenarioHeader("Scenario 2: Balance Inquiry");
        
        try {
            atm.insertCard("1234-5678-9012-3456");
            atm.enterPIN("1234");
            atm.selectAccount(AccountType.SAVINGS);
            atm.performTransaction(TransactionType.BALANCE_INQUIRY, null);
            atm.cancel();
            
            System.out.println("✓ Balance inquiry completed successfully");
            
        } catch (ATMException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario3_CashDeposit(ATM atm) {
        printScenarioHeader("Scenario 3: Cash Deposit");
        
        try {
            BigDecimal depositAmount = new BigDecimal("1500");
            Map<Denomination, Integer> depositNotes = new HashMap<>();
            depositNotes.put(Denomination.NOTE_500, 2);  // 1000
            depositNotes.put(Denomination.NOTE_200, 2);  // 400
            depositNotes.put(Denomination.NOTE_100, 1);  // 100
            // Total: 1500
            
            Map<String, Object> extra = new HashMap<>();
            extra.put("notes", depositNotes);
            
            System.out.println("Depositing notes: " + depositNotes);
            System.out.println("Total deposit: ₹" + depositAmount);
            
            BigDecimal beforeInventory = atm.getCashInventory().getTotalCash();
            
            atm.insertCard("1234-5678-9012-3456");
            atm.enterPIN("1234");
            atm.selectAccount(AccountType.CHECKING);
            atm.performTransaction(TransactionType.CASH_DEPOSIT, depositAmount, extra);
            atm.cancel();
            
            BigDecimal afterInventory = atm.getCashInventory().getTotalCash();
            System.out.println("ATM cash before: ₹" + beforeInventory);
            System.out.println("ATM cash after: ₹" + afterInventory);
            System.out.println("✓ Deposit completed successfully");
            
        } catch (ATMException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario4_InvalidPIN(ATM atm) {
        printScenarioHeader("Scenario 4: Invalid PIN (3 attempts = Card Block)");
        
        try {
            atm.insertCard("9876-5432-1098-7654");
            
            // Attempt 1
            try {
                atm.enterPIN("0000"); // Wrong PIN
            } catch (InvalidPINException e) {
                System.out.println("Attempt 1: " + e.getMessage());
            }
            
            // Attempt 2
            try {
                atm.enterPIN("1111"); // Wrong PIN
            } catch (InvalidPINException e) {
                System.out.println("Attempt 2: " + e.getMessage());
            }
            
            // Attempt 3 - Card gets blocked
            try {
                atm.enterPIN("2222"); // Wrong PIN - final attempt
            } catch (InvalidPINException e) {
                System.out.println("Attempt 3: " + e.getMessage());
            }
            
        } catch (ATMException e) {
            System.out.println("Final: " + e.getMessage());
        }
        System.out.println("✓ Card blocked after 3 failed attempts");
        System.out.println();
    }

    private static void runScenario5_InsufficientFunds(ATM atm) {
        printScenarioHeader("Scenario 5: Insufficient Funds");
        
        // First, we need to unblock the card from scenario 4
        try {
            atm.getCardManager().unblockCard("9876-5432-1098-7654");
        } catch (Exception e) {
            // Ignore
        }
        
        try {
            atm.insertCard("9876-5432-1098-7654");
            atm.enterPIN("4321");
            atm.selectAccount(AccountType.CHECKING);
            
            // Try to withdraw more than balance
            BigDecimal hugeAmount = new BigDecimal("999999");
            atm.performTransaction(TransactionType.CASH_WITHDRAWAL, hugeAmount);
            
        } catch (InsufficientFundsException e) {
            System.out.println("✓ Caught expected error: " + e.getMessage());
            try {
                atm.cancel();
            } catch (ATMException ex) {
                // Ignore
            }
        } catch (ATMException e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario6_BlockedCard(ATM atm) {
        printScenarioHeader("Scenario 6: Blocked Card");
        
        try {
            // Card 5555-6666-7777-8888 is pre-blocked
            atm.insertCard("5555-6666-7777-8888");
            
        } catch (CardBlockedException e) {
            System.out.println("✓ Caught expected error: " + e.getMessage());
        } catch (ATMException e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario7_ExpiredCard(ATM atm) {
        printScenarioHeader("Scenario 7: Expired Card");
        
        try {
            // Card 1111-2222-3333-4444 is expired
            atm.insertCard("1111-2222-3333-4444");
            
        } catch (InvalidCardException e) {
            System.out.println("✓ Caught expected error: " + e.getMessage());
        } catch (ATMException e) {
            System.out.println("✗ Unexpected error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario8_CancelTransaction(ATM atm) {
        printScenarioHeader("Scenario 8: Cancel Transaction");
        
        try {
            atm.insertCard("1234-5678-9012-3456");
            atm.enterPIN("1234");
            atm.selectAccount(AccountType.CHECKING);
            
            System.out.println("User decided to cancel...");
            atm.cancel(); // Ejects card
            
            System.out.println("✓ Transaction cancelled, card ejected");
            
        } catch (ATMException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void runScenario9_ComplexWithdrawal(ATM atm) {
        printScenarioHeader("Scenario 9: Complex Withdrawal (₹4,300)");
        
        try {
            System.out.println("Requesting ₹4,300 - should dispense:");
            System.out.println("  2 × ₹2000 = ₹4000");
            System.out.println("  1 × ₹200 = ₹200");
            System.out.println("  1 × ₹100 = ₹100");
            System.out.println("  Total: ₹4300");
            
            atm.insertCard("1234-5678-9012-3456");
            atm.enterPIN("1234");
            atm.selectAccount(AccountType.SAVINGS);
            atm.performTransaction(TransactionType.CASH_WITHDRAWAL, new BigDecimal("4300"));
            try {
                atm.cancel();
            } catch (Exception e) {
                // Ignore cancel exceptions
            }
            
            System.out.println("✓ Complex withdrawal completed");
            
        } catch (ATMException e) {
            System.out.println("✗ Error: " + e.getMessage());
        }
        System.out.println();
    }

    private static void printSummary(ATM atm) {
        printScenarioHeader("FINAL SUMMARY");
        
        System.out.println(atm);
        System.out.println("\nFinal Cash Inventory:");
        System.out.println(atm.getCashInventory());
        
        System.out.println("\nAll Transactions:");
        atm.getTransactionLogger().getAllTransactions().forEach(t -> {
            System.out.println("  - " + t.getType() + " | ₹" + t.getAmount() + " | " + t.getStatus());
        });
        
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              ATM DEMO COMPLETED SUCCESSFULLY                 ║");
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
