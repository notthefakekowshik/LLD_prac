package com.lldprep.systems.atm.service;

import com.lldprep.systems.atm.model.Account;
import com.lldprep.systems.atm.model.enums.AccountType;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountManager {
    private final Map<String, Account> accounts = new ConcurrentHashMap<>();

    public AccountManager() {
        initializeSampleAccounts();
    }

    private void initializeSampleAccounts() {
        // Account 1: Checking account for card 1
        Account acc1 = new Account("ACC001", AccountType.CHECKING, "1234-5678-9012-3456", new BigDecimal("50000.00"));
        accounts.put(acc1.getAccountId(), acc1);

        // Account 2: Savings account for card 1
        Account acc2 = new Account("ACC002", AccountType.SAVINGS, "1234-5678-9012-3456", new BigDecimal("100000.00"));
        accounts.put(acc2.getAccountId(), acc2);

        // Account 3: Checking for card 2
        Account acc3 = new Account("ACC003", AccountType.CHECKING, "9876-5432-1098-7654", new BigDecimal("25000.00"));
        accounts.put(acc3.getAccountId(), acc3);

        // Account 4: For expired card
        Account acc4 = new Account("ACC004", AccountType.CHECKING, "1111-2222-3333-4444", new BigDecimal("1000.00"));
        accounts.put(acc4.getAccountId(), acc4);

        // Account 5: For blocked card
        Account acc5 = new Account("ACC005", AccountType.CHECKING, "5555-6666-7777-8888", new BigDecimal("75000.00"));
        accounts.put(acc5.getAccountId(), acc5);
    }

    public Account getAccount(String accountId) {
        return accounts.get(accountId);
    }

    public Account getAccountByType(String cardNumber, AccountType type) {
        for (Account account : accounts.values()) {
            if (account.getCardNumber().equals(cardNumber) && account.getType() == type) {
                return account;
            }
        }
        return null;
    }

    public void updateBalance(String accountId, BigDecimal newBalance) {
        Account account = accounts.get(accountId);
        if (account != null) {
            BigDecimal current = account.getBalance();
            BigDecimal diff = newBalance.subtract(current);
            if (diff.compareTo(BigDecimal.ZERO) > 0) {
                account.credit(diff);
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                account.debit(diff.abs());
            }
        }
    }
}
