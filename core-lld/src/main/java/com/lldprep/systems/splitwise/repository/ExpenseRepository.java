package com.lldprep.systems.splitwise.repository;

import com.lldprep.systems.splitwise.model.Expense;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ExpenseRepository {
    private final List<Expense> expenses = new CopyOnWriteArrayList<>();

    public void save(Expense expense) {
        expenses.add(expense);
    }

    public List<Expense> findByUser(String userId) {
        return expenses.stream()
            .filter(expense -> expense.getPaidBy().getId().equals(userId)
                || expense.getSplits().stream().anyMatch(split -> split.getUser().getId().equals(userId)))
            .sorted(Comparator.comparing(Expense::getCreatedAt))
            .toList();
    }

    public List<Expense> findByGroup(String groupId) {
        return expenses.stream()
            .filter(expense -> expense.getGroup() != null && expense.getGroup().getId().equals(groupId))
            .sorted(Comparator.comparing(Expense::getCreatedAt))
            .toList();
    }

    public List<Expense> getAll() {
        return new ArrayList<>(expenses);
    }

    public int count() {
        return expenses.size();
    }
}
