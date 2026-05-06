package com.lldprep.systems.atm.service;

import com.lldprep.systems.atm.model.Transaction;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public class TextReceiptPrinter implements ReceiptPrinter {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public String printReceipt(Transaction transaction, BigDecimal balance) {
        StringBuilder receipt = new StringBuilder();
        receipt.append("================================\n");
        receipt.append("         BANK ATM RECEIPT       \n");
        receipt.append("================================\n");
        receipt.append(String.format("Transaction ID: %s\n", transaction.getTransactionId()));
        receipt.append(String.format("Date/Time:      %s\n", transaction.getTimestamp().format(FORMATTER)));
        receipt.append(String.format("Type:           %s\n", formatType(transaction.getType())));
        
        if (transaction.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Amount:         ₹%,.2f\n", transaction.getAmount()));
        }
        
        receipt.append(String.format("Status:         %s\n", transaction.getStatus()));
        receipt.append("--------------------------------\n");
        receipt.append(String.format("Current Balance: ₹%,.2f\n", balance));
        receipt.append("================================\n");
        receipt.append("   Thank you for banking with us!\n");
        receipt.append("================================");
        
        return receipt.toString();
    }

    private String formatType(Enum<?> type) {
        return type.toString().replace("_", " ");
    }
}
