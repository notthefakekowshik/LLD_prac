package com.lldprep.systems.atm;

import com.lldprep.systems.atm.dispenser.*;
import com.lldprep.systems.atm.exception.ATMException;
import com.lldprep.systems.atm.model.Account;
import com.lldprep.systems.atm.model.Card;
import com.lldprep.systems.atm.model.CashInventory;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.Denomination;
import com.lldprep.systems.atm.model.enums.TransactionType;
import com.lldprep.systems.atm.service.*;
import com.lldprep.systems.atm.state.*;

import java.math.BigDecimal;
import java.util.Map;

public class ATM {
    private final String atmId;
    private ATMState currentState;
    private final CashInventory cashInventory;
    private final CardManager cardManager;
    private final AccountManager accountManager;
    private final TransactionLogger transactionLogger;
    private final ReceiptPrinter receiptPrinter;
    private final CashDispenser dispenserChain;
    
    private Card currentCard;
    private Account currentAccount;
    private int failedPinAttempts;

    public ATM(String atmId) {
        this.atmId = atmId;
        this.currentState = IdleState.INSTANCE;
        this.cashInventory = new CashInventory();
        this.cardManager = new CardManager();
        this.accountManager = new AccountManager();
        this.transactionLogger = new TransactionLogger();
        this.receiptPrinter = new TextReceiptPrinter();
        this.dispenserChain = buildDispenserChain();
        this.failedPinAttempts = 0;
        
        initializeCashInventory();
    }

    private CashDispenser buildDispenserChain() {
        CashDispenser d2000 = new Dispenser2000();
        CashDispenser d500 = new Dispenser500();
        CashDispenser d200 = new Dispenser200();
        CashDispenser d100 = new Dispenser100();
        
        d2000.setNext(d500);
        d500.setNext(d200);
        d200.setNext(d100);
        
        return d2000;
    }

    private void initializeCashInventory() {
        // Initial cash loading: 100x2000, 200x500, 300x200, 500x100 = ₹320,000
        cashInventory.addCash(Denomination.NOTE_2000, 100);
        cashInventory.addCash(Denomination.NOTE_500, 200);
        cashInventory.addCash(Denomination.NOTE_200, 300);
        cashInventory.addCash(Denomination.NOTE_100, 500);
    }

    // State transition methods
    public void setState(ATMState state) {
        this.currentState = state;
        System.out.println("[STATE] Transitioned to: " + state.getStateName());
    }

    // Public API methods
    public void insertCard(String cardNumber) throws ATMException {
        currentState.insertCard(this, cardNumber);
    }

    public void enterPIN(String pin) throws ATMException {
        currentState.enterPIN(this, pin);
    }

    public void selectAccount(AccountType accountType) throws ATMException {
        currentState.selectAccount(this, accountType);
    }

    public void performTransaction(TransactionType type, BigDecimal amount) throws ATMException {
        currentState.performTransaction(this, type, amount, null);
    }

    public void performTransaction(TransactionType type, BigDecimal amount, Map<?, ?> extra) throws ATMException {
        currentState.performTransaction(this, type, amount, extra);
    }

    public void cancel() throws ATMException {
        currentState.cancel(this);
    }

    public void ejectCard() {
        System.out.println("Card ejected. Thank you!");
        resetSession();
    }

    private void resetSession() {
        this.currentCard = null;
        this.currentAccount = null;
        this.failedPinAttempts = 0;
        this.currentState = IdleState.INSTANCE;
    }

    // Getters for state classes
    public Card getCurrentCard() {
        return currentCard;
    }

    public void setCurrentCard(Card card) {
        this.currentCard = card;
    }

    public Account getCurrentAccount() {
        return currentAccount;
    }

    public void setCurrentAccount(Account account) {
        this.currentAccount = account;
    }

    public CardManager getCardManager() {
        return cardManager;
    }

    public AccountManager getAccountManager() {
        return accountManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public ReceiptPrinter getReceiptPrinter() {
        return receiptPrinter;
    }

    public CashInventory getCashInventory() {
        return cashInventory;
    }

    public CashDispenser getDispenserChain() {
        return dispenserChain;
    }

    public int incrementFailedPinAttempts() {
        return ++failedPinAttempts;
    }

    public void resetFailedPinAttempts() {
        this.failedPinAttempts = 0;
    }

    public String getAtmId() {
        return atmId;
    }

    public String getCurrentStateName() {
        return currentState.getStateName();
    }

    @Override
    public String toString() {
        return "ATM{" + "atmId='" + atmId + '\'' + ", state=" + currentState.getStateName() + ", cash=" + cashInventory.getTotalCash() + '}';
    }
}
