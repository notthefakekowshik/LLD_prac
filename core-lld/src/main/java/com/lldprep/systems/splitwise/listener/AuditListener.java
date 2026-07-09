package com.lldprep.systems.splitwise.listener;

import java.util.List;

import com.lldprep.systems.splitwise.model.Expense;
import com.lldprep.systems.splitwise.model.Settlement;
import com.lldprep.systems.splitwise.service.SplitwiseEventListener;

public class AuditListener implements SplitwiseEventListener {
    private final List<String> auditLog;

    public AuditListener(List<String> auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void onExpenseAdded(Expense expense) {
        auditLog.add("EXPENSE " + expense.getId() + " | " + expense.getDescription()
            + " | " + expense.getAmount() + " | paidBy=" + expense.getPaidBy().getName());
    }

    @Override
    public void onSettlementRecorded(Settlement settlement) {
        auditLog.add("SETTLEMENT " + settlement.getId() + " | " + settlement);
    }
}
