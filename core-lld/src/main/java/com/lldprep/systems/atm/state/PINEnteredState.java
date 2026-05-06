package com.lldprep.systems.atm.state;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.ATMException;
import com.lldprep.systems.atm.exception.InvalidStateException;
import com.lldprep.systems.atm.model.Account;
import com.lldprep.systems.atm.model.Card;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public class PINEnteredState implements ATMState {
    public static final PINEnteredState INSTANCE = new PINEnteredState();

    private PINEnteredState() {}

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
        Card card = atm.getCurrentCard();
        Account account = atm.getAccountManager().getAccountByType(card.getCardNumber(), accountType);
        
        if (account == null) {
            System.out.println("No " + accountType + " account linked to this card.");
            return;
        }
        
        atm.setCurrentAccount(account);
        atm.setState(TransactionMenuState.INSTANCE);
        System.out.println(accountType + " account selected. Choose transaction: BALANCE_INQUIRY, CASH_WITHDRAWAL, CASH_DEPOSIT, PIN_CHANGE");
    }

    @Override
    public void performTransaction(ATM atm, TransactionType type, BigDecimal amount, Map<?, ?> extra) throws ATMException {
        throw new InvalidStateException(getStateName(), "performTransaction");
    }

    @Override
    public void cancel(ATM atm) throws ATMException {
        System.out.println("Transaction cancelled.");
        atm.ejectCard();
    }

    @Override
    public String getStateName() {
        return "PIN_ENTERED";
    }
}
