package com.lldprep.systems.atm.state;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.ATMException;
import com.lldprep.systems.atm.exception.CardBlockedException;
import com.lldprep.systems.atm.exception.InvalidCardException;
import com.lldprep.systems.atm.exception.InvalidStateException;
import com.lldprep.systems.atm.model.Card;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public class IdleState implements ATMState {
    public static final IdleState INSTANCE = new IdleState();

    private IdleState() {}

    @Override
    public void insertCard(ATM atm, String cardNumber) throws ATMException {
        Card card = atm.getCardManager().getCard(cardNumber);
        
        if (card == null) {
            throw new InvalidCardException(cardNumber);
        }
        
        if (card.isExpired()) {
            throw new InvalidCardException(cardNumber);
        }
        
        if (card.isBlocked()) {
            throw new CardBlockedException(cardNumber);
        }
        
        atm.setCurrentCard(card);
        atm.resetFailedPinAttempts();
        atm.setState(CardInsertedState.INSTANCE);
        System.out.println("Card accepted. Please enter your PIN.");
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
        throw new InvalidStateException(getStateName(), "performTransaction");
    }

    @Override
    public void cancel(ATM atm) throws ATMException {
        System.out.println("No active session to cancel.");
    }

    @Override
    public String getStateName() {
        return "IDLE";
    }
}
