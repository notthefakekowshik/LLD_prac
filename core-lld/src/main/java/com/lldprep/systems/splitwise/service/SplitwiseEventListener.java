package com.lldprep.systems.splitwise.service;

import com.lldprep.systems.splitwise.model.Expense;
import com.lldprep.systems.splitwise.model.Settlement;

public interface SplitwiseEventListener {
    void onExpenseAdded(Expense expense);

    void onSettlementRecorded(Settlement settlement);
}
