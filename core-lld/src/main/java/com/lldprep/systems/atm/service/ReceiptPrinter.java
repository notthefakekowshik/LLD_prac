package com.lldprep.systems.atm.service;

import com.lldprep.systems.atm.model.Transaction;

import java.math.BigDecimal;

public interface ReceiptPrinter {
    String printReceipt(Transaction transaction, BigDecimal balance);
}
