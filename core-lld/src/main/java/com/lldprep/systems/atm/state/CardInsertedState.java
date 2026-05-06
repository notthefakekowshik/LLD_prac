package com.lldprep.systems.atm.state;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.ATMException;
import com.lldprep.systems.atm.exception.InvalidPINException;
import com.lldprep.systems.atm.exception.InvalidStateException;
import com.lldprep.systems.atm.model.Card;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public class CardInsertedState implements ATMState {
    public static final CardInsertedState INSTANCE = new CardInsertedState();
    private static final int MAX_PIN_ATTEMPTS = 3;

    private CardInsertedState() {}

    @Override
    public void insertCard(ATM atm, String cardNumber) throws ATMException {
        throw new InvalidStateException(getStateName(), "insertCard");
    }

    @Override
    public void enterPIN(ATM atm, String pin) throws ATMException {
        Card card = atm.getCurrentCard();
        
        if (atm.getCardManager().verifyPIN(card, pin)) {
            atm.setState(PINEnteredState.INSTANCE);
            System.out.println("PIN correct. Please select account type (CHECKING/SAVINGS).");
        } else {
            int attempts = atm.incrementFailedPinAttempts();
            int remaining = MAX_PIN_ATTEMPTS - attempts;
            
            if (remaining <= 0) {
                atm.getCardManager().blockCard(card.getCardNumber());
                atm.ejectCard();
                throw new InvalidPINException(0);
            }
            
            throw new InvalidPINException(remaining);
        }
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
        System.out.println("Transaction cancelled.");
        atm.ejectCard();
    }

    @Override
    public String getStateName() {
        return "CARD_INSERTED";
    }
}
