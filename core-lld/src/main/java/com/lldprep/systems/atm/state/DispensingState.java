package com.lldprep.systems.atm.state;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.ATMException;
import com.lldprep.systems.atm.exception.InvalidStateException;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public class DispensingState implements ATMState {
    public static final DispensingState INSTANCE = new DispensingState();

    private DispensingState() {}

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
        throw new InvalidStateException(getStateName(), "performTransaction");
    }

    @Override
    public void cancel(ATM atm) throws ATMException {
        System.out.println("Cannot cancel during cash dispensing.");
    }

    @Override
    public String getStateName() {
        return "DISPENSING";
    }
}
