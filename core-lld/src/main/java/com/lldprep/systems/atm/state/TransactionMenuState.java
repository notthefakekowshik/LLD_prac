package com.lldprep.systems.atm.state;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.*;
import com.lldprep.systems.atm.model.Account;
import com.lldprep.systems.atm.model.Transaction;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.Denomination;
import com.lldprep.systems.atm.model.enums.TransactionStatus;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public class TransactionMenuState implements ATMState {
    public static final TransactionMenuState INSTANCE = new TransactionMenuState();

    private TransactionMenuState() {}

    @Override
    public void insertCard(ATM atm, String cardNumber) throws ATMException {
        throw new InvalidStateException(getStateName(), "insertCard");
    }

    @Override
    public void enterPIN(ATM atm, String pin) throws ATMException {
        throw new InvalidStateException(getStateName(), "enterPIN");
    }

    @Override
    public void selectAccount(ATM atm, AccountType accountType) throws ATMException {
        throw new InvalidStateException(getStateName(), "selectAccount");
    }

    @Override
    public void performTransaction(ATM atm, TransactionType type, BigDecimal amount, Map<?, ?> extra) throws ATMException {
        Account account = atm.getCurrentAccount();
        Transaction transaction = new Transaction(type, amount != null ? amount : BigDecimal.ZERO, account.getAccountId());
        
        try {
            switch (type) {
                case BALANCE_INQUIRY:
                    handleBalanceInquiry(atm, account, transaction);
                    break;
                case CASH_WITHDRAWAL:
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Amount required for withdrawal");
                    }
                    handleCashWithdrawal(atm, account, transaction, amount);
                    break;
                case CASH_DEPOSIT:
                    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Amount required for deposit");
                    }
                    @SuppressWarnings("unchecked")
                    Map<Denomination, Integer> depositNotes = extra != null ? 
                        (Map<Denomination, Integer>) extra.get("notes") : null;
                    handleCashDeposit(atm, account, transaction, amount, depositNotes);
                    break;
                case PIN_CHANGE:
                    @SuppressWarnings("unchecked")
                    Map<String, String> pinData = extra != null ? 
                        (Map<String, String>) extra.get("pinData") : null;
                    handlePinChange(atm, transaction, pinData);
                    break;
                default:
                    throw new ATMException("Unknown transaction type: " + type);
            }
            
            transaction.complete();
            atm.getTransactionLogger().log(transaction);
            System.out.println(atm.getReceiptPrinter().printReceipt(transaction, account.getBalance()));
            
        } catch (Exception e) {
            transaction.fail();
            atm.getTransactionLogger().log(transaction);
            throw e;
        }
        
        // Return to menu or eject
        System.out.println("\nTransaction complete. Select another transaction or press Cancel to exit.");
    }

    private void handleBalanceInquiry(ATM atm, Account account, Transaction transaction) {
        BigDecimal balance = account.getBalance();
        System.out.println("Current Balance: ₹" + balance);
        transaction.setDescription("Balance inquiry");
    }

    private void handleCashWithdrawal(ATM atm, Account account, Transaction transaction, 
                                       BigDecimal amount) throws ATMException {
        // Check account balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(amount, account.getBalance());
        }
        
        // Check ATM cash availability
        if (!atm.getCashInventory().hasSufficientCash(amount)) {
            throw new InsufficientCashException(amount);
        }
        
        // Calculate dispense plan
        Map<Denomination, Integer> dispensePlan = atm.getDispenserChain().dispense(amount, atm.getCashInventory());
        if (dispensePlan == null) {
            throw new InsufficientCashException(amount);
        }
        
        // Verify we can actually dispense this
        int total = 0;
        for (Map.Entry<Denomination, Integer> entry : dispensePlan.entrySet()) {
            total += entry.getKey().getValue() * entry.getValue();
        }
        if (total != amount.intValue()) {
            throw new InsufficientCashException(amount);
        }
        
        // Perform withdrawal
        atm.setState(DispensingState.INSTANCE);
        System.out.println("Dispensing cash...");
        
        account.debit(amount);
        atm.getCashInventory().dispense(dispensePlan);
        
        System.out.println("Dispensed: " + dispensePlan);
        transaction.setDescription("Cash withdrawal");
        
        atm.setState(TransactionMenuState.INSTANCE);
    }

    private void handleCashDeposit(ATM atm, Account account, Transaction transaction, 
                                    BigDecimal amount, Map<Denomination, Integer> notes) {
        if (notes == null || notes.isEmpty()) {
            throw new IllegalArgumentException("Deposit notes must be specified");
        }
        
        // Verify amount matches notes
        BigDecimal calculatedAmount = BigDecimal.ZERO;
        for (Map.Entry<Denomination, Integer> entry : notes.entrySet()) {
            calculatedAmount = calculatedAmount.add(
                BigDecimal.valueOf(entry.getKey().getValue() * entry.getValue())
            );
        }
        
        if (calculatedAmount.compareTo(amount) != 0) {
            throw new IllegalArgumentException(
                "Deposit amount ₹" + amount + " doesn't match notes value ₹" + calculatedAmount);
        }
        
        account.credit(amount);
        atm.getCashInventory().addCash(notes);
        
        System.out.println("Deposited: " + notes);
        transaction.setDescription("Cash deposit");
    }

    private void handlePinChange(ATM atm, Transaction transaction, Map<String, String> pinData) throws ATMException {
        if (pinData == null) {
            throw new IllegalArgumentException("PIN data required");
        }
        
        String oldPin = pinData.get("oldPin");
        String newPin = pinData.get("newPin");
        
        if (oldPin == null || newPin == null) {
            throw new IllegalArgumentException("Both old and new PIN required");
        }
        
        if (!atm.getCardManager().verifyPIN(atm.getCurrentCard(), oldPin)) {
            throw new InvalidPINException(0);
        }
        
        // In a real system, we'd update the PIN hash
        System.out.println("PIN changed successfully.");
        transaction.setDescription("PIN change");
    }

    @Override
    public void cancel(ATM atm) throws ATMException {
        System.out.println("Transaction cancelled.");
        atm.ejectCard();
    }

    @Override
    public String getStateName() {
        return "TRANSACTION_MENU";
    }
}
