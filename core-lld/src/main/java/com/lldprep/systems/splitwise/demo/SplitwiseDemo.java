package com.lldprep.systems.splitwise.demo;

import com.lldprep.systems.splitwise.exception.SplitwiseException;
import com.lldprep.systems.splitwise.listener.AuditListener;
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

    // ponytail: static fields hold the demo's shared actors, wired once in setUp() and reused by
    // every scenario below. Fine for a single-threaded demo runner; not a pattern for real state.
    private static SplitwiseFacade splitwise;
    private static List<String> auditLog;
    private static User alice;
    private static User bob;
    private static User charlie;
    private static User dave;
    private static Group goaTrip;

    // Each scenario is its own method, in the order they run. Read top-to-bottom like a table of contents.
    public static void main(String[] args) throws InterruptedException {
        printHeader();
        setUp();

        registerUsers();       // 1
        createGroup();         // 2
        equalSplit();          // 3
//        exactSplit();          // 4
//        percentageSplit();     // 5
//        balanceSummary();      // 6
//        settleUp();            // 7
//        expenseHistory();      // 8
//        validationFailures();  // 9
//        debtSimplification();  // 10
//        concurrency();         // 11
//        observerAuditLog();    // 12
        successSummary();      // 13
    }

    // Build the facade and attach an audit listener (Observer) that records every event.
    private static void setUp() {
        splitwise = new SplitwiseFacade();
        auditLog = Collections.synchronizedList(new ArrayList<>());
        splitwise.addListener(new AuditListener(auditLog));
    }

    // 1. Register the four users that the rest of the scenarios reuse.
    private static void registerUsers() {
        scenario("1. Register users");
        alice = splitwise.registerUser("Alice", "alice@example.com");
        bob = splitwise.registerUser("Bob", "bob@example.com");
        charlie = splitwise.registerUser("Charlie", "charlie@example.com");
        dave = splitwise.registerUser("Dave", "dave@example.com");
        printUsers(alice, bob, charlie, dave);
    }

    // 2. Create a group; only its members may share group-scoped expenses.
    private static void createGroup() {
        scenario("2. Create group");
        goaTrip = splitwise.createGroup("Goa Trip", ids(alice, bob, charlie));
        System.out.println("Group: " + goaTrip);
        System.out.println("Members: " + names(goaTrip.getMembers()));
        System.out.println();
    }

    // 3. Equal split: total divided evenly across all participants.
    private static void equalSplit() {
        scenario("3. Equal split - Alice pays 900 dinner for Alice/Bob/Charlie");
        /*
        Here, I thought we need not participants list, we can have group ID and fetch the group members with ID.
            What if all the users do not participate in this expense? If the group has 10 members, not everyone can go to lunch/dinner.
            there's group validation down the line which check if these participants are part of the group.
            com.lldprep.systems.splitwise.service.ExpenseService.validateGroupMembership
         */
        Expense dinner = splitwise.addExpense("Dinner", money("900"), alice.getId(),
            ids(alice, bob, charlie), SplitType.EQUAL, Map.of(), goaTrip.getId());
        printExpense(dinner);
        System.out.println("Expected: Bob owes Alice 300.00, Charlie owes Alice 300.00");
        printPairBalance(bob, alice);
        printPairBalance(charlie, alice);
        System.out.println();
    }

    // 4. Exact split: each participant's amount is given and must sum to the total.
    private static void exactSplit() {
        scenario("4. Exact split - Bob pays 600 hotel: Alice 250, Bob 200, Charlie 150");
        Expense hotel = splitwise.addExpense("Hotel", money("600"), bob.getId(),
            ids(alice, bob, charlie), SplitType.EXACT,
            Map.of(alice.getId(), money("250"), bob.getId(), money("200"), charlie.getId(), money("150")),
            goaTrip.getId());
        printExpense(hotel);
        System.out.println("Expected net: Bob now owes Alice 50.00; Charlie owes Bob 150.00; Charlie still owes Alice 300.00");
        printPairBalance(bob, alice);
        printPairBalance(charlie, bob);
        printPairBalance(charlie, alice);
        System.out.println();
    }

    // 5. Percentage split: shares given as percentages that must total 100.
    private static void percentageSplit() {
        scenario("5. Percentage split - Charlie pays 800 cab: Alice 50%, Bob 25%, Charlie 25%");
        Expense cab = splitwise.addExpense("Cab", money("800"), charlie.getId(),
            ids(alice, bob, charlie), SplitType.PERCENTAGE,
            Map.of(alice.getId(), money("50"), bob.getId(), money("25"), charlie.getId(), money("25")),
            goaTrip.getId());
        printExpense(cab);
        System.out.println("Expected net: Bob owes Alice 50.00; Alice owes Charlie 100.00; Bob owes Charlie 50.00");
        printPairBalance(bob, alice);
        printPairBalance(alice, charlie);
        printPairBalance(bob, charlie);
        System.out.println();
    }

    // 6. Net balance summary from a single user's perspective.
    private static void balanceSummary() {
        scenario("6. Balance summary for Alice");
        printSummary(alice, List.of(bob, charlie, dave));
        System.out.println("Expected: Alice is owed 50.00 by Bob, owes 100.00 to Charlie, net -50.00");
        System.out.println();
    }

    // 7. Settle up: a direct payment that reduces an outstanding balance to zero.
    private static void settleUp() {
        scenario("7. Settlement - Bob pays Alice 50");
        Settlement settlement = splitwise.settle(bob.getId(), alice.getId(), money("50"));
        System.out.println("Settlement recorded: " + settlement);
        System.out.println("Expected: Bob/Alice pair is now zero");
        printPairBalance(bob, alice);
        printSummary(alice, List.of(bob, charlie, dave));
        System.out.println();
    }

    // 8. Expense history, per user and per group, in chronological order.
    private static void expenseHistory() {
        scenario("8. Expense history");
        printHistory("Alice expense history", splitwise.getExpensesForUser(alice.getId()));
        printHistory("Goa Trip expense history", splitwise.getExpensesForGroup(goaTrip.getId()));
        System.out.println();
    }

    // 9. Validation guards: non-member, bad exact sum, bad percentage sum, over-settlement.
    private static void validationFailures() {
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
    }

    // 10. Debt simplification is read-only: it suggests fewer transfers without mutating balances.
    private static void debtSimplification() {
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
    }

    // 11. Concurrency: two threads add same-pair expenses; per-pair locking keeps the balance correct.
    private static void concurrency() throws InterruptedException {
        scenario("11. Concurrency - two threads add same-pair expenses");
        splitwise.addMember(goaTrip.getId(), dave.getId());
        runConcurrentExpenses();
        System.out.println("Expected: 50 expenses * 5.00 Dave share = Dave owes Alice 250.00");
        printPairBalance(dave, alice);
        System.out.println();
    }

    // 12. Observer: the audit listener captured every expense and settlement event.
    private static void observerAuditLog() {
        scenario("12. Observer audit log");
        System.out.println("Audit events captured: " + auditLog.size());
        auditLog.stream().limit(10).forEach(entry -> System.out.println("  " + entry));
        if (auditLog.size() > 10) {
            System.out.println("  ... " + (auditLog.size() - 10) + " more events");
        }
        System.out.println();
    }

    // 13. Final totals and key balances.
    private static void successSummary() {
        scenario("13. Success summary");
        System.out.println("Total users: " + splitwise.totalUsers());
        System.out.println("Total groups: " + splitwise.totalGroups());
        System.out.println("Total expenses: " + splitwise.totalExpenses());
        System.out.println("Total settlements: " + splitwise.totalSettlements());
        System.out.println("Final key balances:");
        printPairBalance(bob, alice);
        printPairBalance(alice, charlie);
        printPairBalance(bob, charlie);
        printPairBalance(dave, alice);
        System.out.println("\nALL SPLITWISE SCENARIOS COMPLETED");
    }

    // Fires 2 threads x 25 equal-split expenses on the Alice/Dave pair, all released together.
    private static void runConcurrentExpenses() throws InterruptedException {
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

    private static void printHeader() {
        System.out.println("============================================================");
        System.out.println("SPLITWISE LLD DEMO");
        System.out.println("Patterns: Strategy | Factory | Repository | Facade | Observer");
        System.out.println("============================================================\n");
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

    private static void printPairBalance(User userA, User userB) {
        BigDecimal balance = splitwise.getBalance(userA.getId(), userB.getId());
        if (balance.compareTo(ZERO) > 0) {
            System.out.println("  " + userA.getName() + " owes " + userB.getName() + " " + balance);
        } else if (balance.compareTo(ZERO) < 0) {
            System.out.println("  " + userB.getName() + " owes " + userA.getName() + " " + balance.abs());
        } else {
            System.out.println("  " + userA.getName() + " and " + userB.getName() + " are settled");
        }
    }

    private static void printSummary(User user, List<User> knownUsers) {
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

}
