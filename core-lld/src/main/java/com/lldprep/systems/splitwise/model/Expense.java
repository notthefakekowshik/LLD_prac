package com.lldprep.systems.splitwise.model;

import com.lldprep.systems.splitwise.model.enums.SplitType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Expense {
    private final String id;
    private final String description;
    private final BigDecimal amount;
    private final User paidBy;
    private final List<Split> splits;
    private final SplitType splitType;
    private final Group group;
    private final LocalDateTime createdAt;

    public Expense(String description, BigDecimal amount, User paidBy, List<Split> splits,
                   SplitType splitType, Group group) {
        this.id = "EXP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.description = description;
        this.amount = amount;
        this.paidBy = paidBy;
        this.splits = List.copyOf(splits);
        this.splitType = splitType;
        this.group = group;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public User getPaidBy() {
        return paidBy;
    }

    public List<Split> getSplits() {
        return splits;
    }

    public SplitType getSplitType() {
        return splitType;
    }

    public Group getGroup() {
        return group;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        String groupLabel = group == null ? "no group" : group.getName();
        return id + " | " + description + " | amount=" + amount + " | paidBy=" + paidBy.getName()
            + " | splitType=" + splitType + " | " + groupLabel;
    }
}
