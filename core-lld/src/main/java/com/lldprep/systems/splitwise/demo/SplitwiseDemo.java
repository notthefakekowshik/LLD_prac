package com.lldprep.systems.splitwise.demo;

import com.lldprep.systems.splitwise.exception.SplitwiseException;
import com.lldprep.systems.splitwise.model.Expense;
import com.lldprep.systems.splitwise.model.Group;
import com.lldprep.systems.splitwise.model.Settlement;
import com.lldprep.systems.splitwise.model.Split;
import com.lldprep.systems.splitwise.model.User;
import com.lldprep.systems.splitwise.model.enums.SplitType;
import com.lldprep.systems.splitwise.service.SplitwiseEventListener;
import com.lldprep.systems.splitwise.service.SplitwiseFacade;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SplitwiseDemo {
    private static final BigDecimal ZERO = money("0");

    public static void main(String[] args) throws InterruptedException {
        System.out.println("============================================================");
        System.out.println("SPLITWISE LLD DEMO");
        System.out.println("Patterns: Strategy | Factory | Repository | Facade | Observer");
        System.out.println("============================================================\n");

        SplitwiseFacade splitwise = new SplitwiseFacade();
        List<String> auditLog = Collections.synchronizedList(new ArrayList<>());
        splitwise.addListener(new AuditListener(auditLog));

        User alice = splitwise.registerUser("Alice", "alice@example.com");
        User bob = splitwise.registerUser("Bob", "bob@example.com");
        User charlie = splitwise.registerUser("Charlie", "charlie@example.com");
        User dave = splitwise.registerUser("Dave", "dave@example.com");

        scenario("1. Register users");
        printUsers(alice, bob, charlie, dave);

        scenario("2. Create group");
        Group goaTrip = splitwise.createGroup("Goa Trip", ids(alice, bob, charlie));
        System.out.println("Group: " + goaTrip);
        System.out.println("Members: " + names(goaTrip.getMembers()));
        System.out.println();

        scenario("3. Equal split - Alice pays 900 dinner for Alice/Bob/Charlie");
        Expense dinner = splitwise.addExpense("Dinner", money("900"), alice.getId(),
            ids(alice, bob, charlie), SplitType.EQUAL, Map.of(), goaTrip.getId());
        printExpense(dinner);
        System.out.println("Expected: Bob owes Alice 300.00, Charlie owes Alice 300.00");
        printPairBalance(splitwise, bob, alice);
        printPairBalance(splitwise, charlie, alice);
        System.out.println();

        scenario("4. Exact split - Bob pays 600 hotel: Alice 250, Bob 200, Charlie 150");
        Expense hotel = splitwise.addExpense("Hotel", money("600"), bob.getId(),
            ids(alice, bob, charlie), SplitType.EXACT,
            Map.of(alice.getId(), money("250"), bob.getId(), money("200"), charlie.getId(), money("150")),
            goaTrip.getId());
        printExpense(hotel);
        System.out.println("Expected net: Bob now owes Alice 50.00; Charlie owes Bob 150.00; Charlie still owes Alice 300.00");
        printPairBalance(splitwise, bob, alice);
        printPairBalance(splitwise, charlie, bob);
        printPairBalance(splitwise, charlie, alice);
        System.out.println();

        scenario("5. Percentage split - Charlie pays 800 cab: Alice 50%, Bob 25%, Charlie 25%");
        Expense cab = splitwise.addExpense("Cab", money("800"), charlie.getId(),
            ids(alice, bob, charlie), SplitType.PERCENTAGE,
            Map.of(alice.getId(), money("50"), bob.getId(), money("25"), charlie.getId(), money("25")),
            goaTrip.getId());
        printExpense(cab);
        System.out.println("Expected net: Bob owes Alice 50.00; Alice owes Charlie 100.00; Bob owes Charlie 50.00");
        printPairBalance(splitwise, bob, alice);
        printPairBalance(splitwise, alice, charlie);
        printPairBalance(splitwise, bob, charlie);
        System.out.println();

        scenario("6. Balance summary for Alice");
        printSummary(splitwise, alice, List.of(bob, charlie, dave));
        System.out.println("Expected: Alice is owed 50.00 by Bob, owes 100.00 to Charlie, net -50.00");
        System.out.println();

        scenario("7. Settlement - Bob pays Alice 50");
        Settlement settlement = splitwise.settle(bob.getId(), alice.getId(), money("50"));
        System.out.println("Settlement recorded: " + settlement);
        System.out.println("Expected: Bob/Alice pair is now zero");
        printPairBalance(splitwise, bob, alice);
        printSummary(splitwise, alice, List.of(bob, charlie, dave));
        System.out.println();

        scenario("8. Expense history");
        printHistory("Alice expense history", splitwise.getExpensesForUser(alice.getId()));
        printHistory("Goa Trip expense history", splitwise.getExpensesForGroup(goaTrip.getId()));
        System.out.println();

        scenario("9. Expected validation failures");
        expectFailure("non-member Dave in Goa Trip expense", () -> splitwise.addExpense("Snacks", money("300"), alice.getId(),
            ids(alice, dave), SplitType.EQUAL, Map.of(), goaTrip.getId()));
        expectFailure("bad exact split sum", () -> splitwise.addExpense("Bad exact", money("500"), alice.getId(),
            ids(alice, bob), SplitType.EXACT,
            Map.of(alice.getId(), money("100"), bob.getId(), money("300")), null));
        expectFailure("bad percentage split sum", () -> splitwise.addExpense("Bad percentage", money("500"), alice.getId(),
            ids(alice, bob), SplitType.PERCENTAGE,
            Map.of(alice.getId(), money("40"), bob.getId(), money("40")), null));
        expectFailure("settlement over outstanding balance", () -> splitwise.settle(alice.getId(), charlie.getId(), money("999")));
        System.out.println();

        scenario("10. Debt simplification suggestion - read-only");
        BigDecimal beforeAliceCharlie = splitwise.getBalance(alice.getId(), charlie.getId());
        List<Settlement> suggestions = splitwise.simplifyDebts(goaTrip.getId());
        System.out.println("Suggestions:");
        for (Settlement suggested : suggestions) {
            System.out.println("  " + suggested);
        }
        BigDecimal afterAliceCharlie = splitwise.getBalance(alice.getId(), charlie.getId());
        System.out.println("Read-only check: Alice/Charlie before=" + beforeAliceCharlie + ", after=" + afterAliceCharlie);
        System.out.println("Expected: same value; simplifyDebts does not mutate balances");
        System.out.println();

        scenario("11. Concurrency - two threads add same-pair expenses");
        splitwise.addMember(goaTrip.getId(), dave.getId());
        runConcurrencyScenario(splitwise, alice, dave);
        System.out.println("Expected: 50 expenses * 5.00 Dave share = Dave owes Alice 250.00");
        printPairBalance(splitwise, dave, alice);
        System.out.println();

        scenario("12. Observer audit log");
        System.out.println("Audit events captured: " + auditLog.size());
        auditLog.stream().limit(10).forEach(entry -> System.out.println("  " + entry));
        if (auditLog.size() > 10) {
            System.out.println("  ... " + (auditLog.size() - 10) + " more events");
        }
        System.out.println();

        scenario("13. Success summary");
        System.out.println("Total users: " + splitwise.totalUsers());
        System.out.println("Total groups: " + splitwise.totalGroups());
        System.out.println("Total expenses: " + splitwise.totalExpenses());
        System.out.println("Total settlements: " + splitwise.totalSettlements());
        System.out.println("Final key balances:");
        printPairBalance(splitwise, bob, alice);
        printPairBalance(splitwise, alice, charlie);
        printPairBalance(splitwise, bob, charlie);
        printPairBalance(splitwise, dave, alice);
        System.out.println("\nALL SPLITWISE SCENARIOS COMPLETED");
    }

    private static void runConcurrencyScenario(SplitwiseFacade splitwise, User alice, User dave) throws InterruptedException {
        int threadCount = 2;
        int expensesPerThread = 25;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int thread = 0; thread < threadCount; thread++) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < expensesPerThread; i++) {
                        splitwise.addExpense("Concurrent coffee", money("10"), alice.getId(),
                            ids(alice, dave), SplitType.EQUAL, Map.of(), null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        start.countDown();
        executor.shutdown();
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrency scenario did not finish");
        }
    }

    private static void printUsers(User... users) {
        for (User user : users) {
            System.out.println("  " + user.getName() + " | " + user.getId() + " | " + user.getEmail());
        }
        System.out.println();
    }

    private static void printExpense(Expense expense) {
        System.out.println(expense);
        System.out.println("Splits:");
        for (Split split : expense.getSplits()) {
            System.out.println("  " + split);
        }
    }

    private static void printHistory(String title, List<Expense> expenses) {
        System.out.println(title + " (" + expenses.size() + "):");
        for (Expense expense : expenses) {
            System.out.println("  " + expense.getDescription() + " | " + expense.getAmount()
                + " | paid by " + expense.getPaidBy().getName());
        }
    }

    private static void printPairBalance(SplitwiseFacade splitwise, User userA, User userB) {
        BigDecimal balance = splitwise.getBalance(userA.getId(), userB.getId());
        if (balance.compareTo(ZERO) > 0) {
            System.out.println("  " + userA.getName() + " owes " + userB.getName() + " " + balance);
        } else if (balance.compareTo(ZERO) < 0) {
            System.out.println("  " + userB.getName() + " owes " + userA.getName() + " " + balance.abs());
        } else {
            System.out.println("  " + userA.getName() + " and " + userB.getName() + " are settled");
        }
    }

    private static void printSummary(SplitwiseFacade splitwise, User user, List<User> knownUsers) {
        Map<String, BigDecimal> summary = splitwise.getBalanceSummary(user.getId());
        BigDecimal net = ZERO;
        System.out.println("Balance summary for " + user.getName() + ":");
        for (User other : knownUsers) {
            BigDecimal amount = summary.get(other.getId());
            if (amount == null) {
                continue;
            }
            net = net.add(amount);
            if (amount.compareTo(ZERO) > 0) {
                System.out.println("  " + other.getName() + " owes " + user.getName() + " " + amount);
            } else {
                System.out.println("  " + user.getName() + " owes " + other.getName() + " " + amount.abs());
            }
        }
        System.out.println("  Net from " + user.getName() + "'s perspective: " + net);
    }

    private static void expectFailure(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  FAIL: expected error for " + label);
        } catch (SplitwiseException | IllegalArgumentException ex) {
            System.out.println("  OK: " + label + " -> " + ex.getMessage());
        }
    }

    private static List<String> ids(User... users) {
        List<String> ids = new ArrayList<>();
        for (User user : users) {
            ids.add(user.getId());
        }
        return ids;
    }

    private static String names(List<User> users) {
        return users.stream().map(User::getName).toList().toString();
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value).setScale(2);
    }

    private static void scenario(String title) {
        System.out.println("------------------------------------------------------------");
        System.out.println(title);
        System.out.println("------------------------------------------------------------");
    }

    private static class AuditListener implements SplitwiseEventListener {
        private final List<String> auditLog;

        private AuditListener(List<String> auditLog) {
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
}
