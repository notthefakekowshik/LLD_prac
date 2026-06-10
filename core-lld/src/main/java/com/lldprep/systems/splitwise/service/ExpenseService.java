package com.lldprep.systems.splitwise.service;

import com.lldprep.systems.splitwise.exception.ExpenseValidationException;
import com.lldprep.systems.splitwise.exception.GroupNotFoundException;
import com.lldprep.systems.splitwise.exception.UserNotFoundException;
import com.lldprep.systems.splitwise.factory.SplitStrategyFactory;
import com.lldprep.systems.splitwise.model.Expense;
import com.lldprep.systems.splitwise.model.Group;
import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;
import com.lldprep.systems.splitwise.model.enums.SplitType;
import com.lldprep.systems.splitwise.repository.ExpenseRepository;
import com.lldprep.systems.splitwise.repository.GroupRepository;
import com.lldprep.systems.splitwise.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExpenseService {
    private static final int MONEY_SCALE = 2;
    private static final int MIN_PARTICIPANTS = 2;
    private static final int MAX_PARTICIPANTS = 50;

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final ExpenseRepository expenseRepository;
    private final BalanceService balanceService;
    private final SplitStrategyFactory splitStrategyFactory;

    public ExpenseService(UserRepository userRepository, GroupRepository groupRepository,
                          ExpenseRepository expenseRepository, BalanceService balanceService,
                          SplitStrategyFactory splitStrategyFactory) {
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.expenseRepository = expenseRepository;
        this.balanceService = balanceService;
        this.splitStrategyFactory = splitStrategyFactory;
    }

    public Expense addExpense(String description, BigDecimal amount, String paidByUserId, List<String> participantIds,
                              SplitType splitType, Map<String, BigDecimal> metadata, String groupId) {
        validateDescription(description);
        BigDecimal normalizedAmount = normalize(amount);
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ExpenseValidationException("Expense amount must be positive");
        }

        User paidBy = getUserOrThrow(paidByUserId);
        List<User> participants = resolveParticipants(participantIds);
        if (participants.stream().noneMatch(user -> user.getId().equals(paidByUserId))) {
            throw new ExpenseValidationException("Payer must be part of participants");
        }

        Group group = null;
        if (groupId != null && !groupId.isBlank()) {
            group = groupRepository.getById(groupId);
            if (group == null) {
                throw new GroupNotFoundException(groupId);
            }
            validateGroupMembership(group, paidBy, participants);
        }

        List<Split> splits = splitStrategyFactory.get(splitType).calculate(normalizedAmount, participants, metadata);
        Expense expense = new Expense(description, normalizedAmount, paidBy, splits, splitType, group);
        expenseRepository.save(expense);
        balanceService.updateOnExpense(expense);
        return expense;
    }

    public List<Expense> getExpensesForUser(String userId) {
        getUserOrThrow(userId);
        return expenseRepository.findByUser(userId);
    }

    public List<Expense> getExpensesForGroup(String groupId) {
        if (groupRepository.getById(groupId) == null) {
            throw new GroupNotFoundException(groupId);
        }
        return expenseRepository.findByGroup(groupId);
    }

    private List<User> resolveParticipants(List<String> participantIds) {
        if (participantIds == null || participantIds.size() < MIN_PARTICIPANTS || participantIds.size() > MAX_PARTICIPANTS) {
            throw new ExpenseValidationException("Participants must be between " + MIN_PARTICIPANTS + " and " + MAX_PARTICIPANTS);
        }

        Set<String> uniqueIds = new LinkedHashSet<>(participantIds);
        if (uniqueIds.size() != participantIds.size()) {
            throw new ExpenseValidationException("Participants must not contain duplicates");
        }

        List<User> participants = new ArrayList<>();
        for (String participantId : uniqueIds) {
            participants.add(getUserOrThrow(participantId));
        }
        return participants;
    }

    private void validateGroupMembership(Group group, User paidBy, List<User> participants) {
        if (!group.hasMember(paidBy.getId())) {
            throw new ExpenseValidationException("Payer " + paidBy.getName() + " is not a group member");
        }
        for (User participant : participants) {
            if (!group.hasMember(participant.getId())) {
                throw new ExpenseValidationException("Participant " + participant.getName() + " is not a group member");
            }
        }
    }

    private void validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new ExpenseValidationException("Description cannot be empty");
        }
    }

    private User getUserOrThrow(String userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }

    private BigDecimal normalize(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
