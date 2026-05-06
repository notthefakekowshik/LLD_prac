package com.lldprep.systems.atm.state;

import com.lldprep.systems.atm.ATM;
import com.lldprep.systems.atm.exception.ATMException;
import com.lldprep.systems.atm.model.enums.AccountType;
import com.lldprep.systems.atm.model.enums.TransactionType;

import java.math.BigDecimal;
import java.util.Map;

public interface ATMState {
    void insertCard(ATM atm, String cardNumber) throws ATMException;
    void enterPIN(ATM atm, String pin) throws ATMException;
    void selectAccount(ATM atm, AccountType accountType) throws ATMException;
    void performTransaction(ATM atm, TransactionType type, BigDecimal amount, Map<?, ?> extra) throws ATMException;
    void cancel(ATM atm) throws ATMException;
    String getStateName();
}
