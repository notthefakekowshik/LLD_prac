package com.lldprep.systems.meetingroomscheduler.demo;

import com.lldprep.systems.meetingroomscheduler.exception.MeetingSchedulerException;
import com.lldprep.systems.meetingroomscheduler.model.Amenity;
import com.lldprep.systems.meetingroomscheduler.model.Booking;
import com.lldprep.systems.meetingroomscheduler.model.Room;
import com.lldprep.systems.meetingroomscheduler.model.TimeSlot;
import com.lldprep.systems.meetingroomscheduler.model.User;
import com.lldprep.systems.meetingroomscheduler.service.MeetingSchedulerFacade;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MeetingSchedulerDemo {
    private static MeetingSchedulerFacade scheduler;
    private static User alice;
    private static User bob;
    private static User charlie;
    private static Room confA;
    private static Room confB;
    private static Room boardRoom;
    private static Room tinyRoom;
    private static LocalDate today;

    public static void main(String[] args) throws InterruptedException {
        printHeader();
        setUp();

        registerUsers();        // 1
        addRooms();             // 2
        basicBooking();         // 3
        conflictDetection();    // 4
        roomSchedule();         // 5
        searchRoomsAllFilters();// 6
        upcomingBookings();     // 7
        cancelBooking();        // 8
        validationFailures();   // 9
        concurrency();          // 10
        successSummary();       // 11
    }

    private static void setUp() {
        scheduler = new MeetingSchedulerFacade();
        today = LocalDate.now();
    }

    // ======================================================================
    // 1. Register users
    // ======================================================================
    private static void registerUsers() {
        scenario("1. Register users");
        alice = scheduler.registerUser("Alice", "alice@example.com");
        bob = scheduler.registerUser("Bob", "bob@example.com");
        charlie = scheduler.registerUser("Charlie", "charlie@example.com");
        printUsers(alice, bob, charlie);
    }

    // ======================================================================
    // 2. Add meeting rooms with different capacities and amenities
    // ======================================================================
    private static void addRooms() {
        scenario("2. Add meeting rooms");
        confA = scheduler.addRoom("Conference Room A", 10, 1,
            Set.of(Amenity.PROJECTOR, Amenity.WHITEBOARD));
        confB = scheduler.addRoom("Conference Room B", 8, 2,
            Set.of(Amenity.PROJECTOR, Amenity.VIDEO_CONFERENCING));
        boardRoom = scheduler.addRoom("Board Room", 20, 3,
            Set.of(Amenity.PROJECTOR, Amenity.WHITEBOARD, Amenity.VIDEO_CONFERENCING, Amenity.CONFERENCE_PHONE));
        tinyRoom = scheduler.addRoom("Tiny Room", 4, 1,
            Set.of(Amenity.WHITEBOARD));

        System.out.println("Rooms added:");
        System.out.println("  " + confA);
        System.out.println("  " + confB);
        System.out.println("  " + boardRoom);
        System.out.println("  " + tinyRoom);
        System.out.println();
    }

    // ======================================================================
    // 3. Basic booking — Alice books Conference Room A for 1 hour
    // ======================================================================
    private static void basicBooking() {
        scenario("3. Basic booking — Alice books Conf A, tomorrow 10:00-11:00 (1h)");
        LocalDate date = today.plusDays(1);
        TimeSlot slot = TimeSlot.of(date, 10, 0, 60);

        Booking booking = scheduler.bookRoom(confA.getId(), alice.getId(), slot);
        System.out.println("Created: " + booking);

        System.out.println("\nConf A schedule after booking:");
        System.out.println(scheduler.getRoomSchedule(confA.getId(), date));
    }

    // ======================================================================
    // 4. Conflict detection — Bob tries to book same room, overlapping time
    // ======================================================================
    private static void conflictDetection() {
        scenario("4. Conflict detection — Bob tries Conf A, tomorrow 10:30-11:30");
        LocalDate date = today.plusDays(1);
        TimeSlot overlappingSlot = TimeSlot.of(date, 10, 30, 60);

        expectFailure("overlapping booking returns conflict",
            () -> scheduler.bookRoom(confA.getId(), bob.getId(), overlappingSlot));

        TimeSlot nonOverlappingSlot = TimeSlot.of(date, 11, 0, 60);
        Booking bobBooking = scheduler.bookRoom(confA.getId(), bob.getId(), nonOverlappingSlot);
        System.out.println("Bob successfully books non-overlapping slot: " + bobBooking);
        System.out.println();
    }

    // ======================================================================
    // 5. View a room's full schedule
    // ======================================================================
    private static void roomSchedule() {
        scenario("5. Room schedule — Conf A tomorrow");
        LocalDate date = today.plusDays(1);
        System.out.println(scheduler.getRoomSchedule(confA.getId(), date));

        System.out.println("Empty room schedule (Board Room tomorrow):");
        System.out.println(scheduler.getRoomSchedule(boardRoom.getId(), date));
    }

    // ======================================================================
    // 6. Search rooms — combine capacity + floor + amenities + availability
    // ======================================================================
    private static void searchRoomsAllFilters() {
        scenario("6. Search — cap >= 8, floor 1, projector required, tomorrow 09:00-10:00");
        LocalDate date = today.plusDays(1);
        TimeSlot slot = TimeSlot.of(date, 9, 0, 60);

        List<Room> results = scheduler.searchRooms(slot, 8, 1, true, Set.of(Amenity.PROJECTOR));

        System.out.println("Available rooms matching criteria:");
        for (Room room : results) {
            System.out.println("  " + room.getName() + " (floor " + room.getFloor()
                + ", cap " + room.getCapacity() + ", " + room.getAmenities() + ")");
        }
        System.out.println("Expected: only Conf A (floor 1, cap 10, has projector, free at 9:00-10:00)");
        System.out.println();

        scenario("6b. Search — any floor, cap >= 6, video conf, tomorrow 14:00-15:00");
        TimeSlot slot2 = TimeSlot.of(date, 14, 0, 60);

        List<Room> results2 = scheduler.searchRooms(slot2, 6, 0, false,
            Set.of(Amenity.VIDEO_CONFERENCING));

        System.out.println("Available rooms:");
        for (Room room : results2) {
            System.out.println("  " + room.getName() + " (floor " + room.getFloor()
                + ", cap " + room.getCapacity() + ")");
        }
        System.out.println("Expected: Conf B and Board Room (both have video conf, cap >= 6)");
        System.out.println();
    }

    // ======================================================================
    // 7. View user's upcoming bookings
    // ======================================================================
    private static void upcomingBookings() {
        scenario("7. Upcoming bookings for Alice and Bob");
        System.out.println("Alice's bookings:");
        for (Booking b : scheduler.getUpcomingBookings(alice.getId())) {
            System.out.println("  " + b);
        }
        System.out.println("Bob's bookings:");
        for (Booking b : scheduler.getUpcomingBookings(bob.getId())) {
            System.out.println("  " + b);
        }
        System.out.println();
    }

    // ======================================================================
    // 8. Cancel a booking and verify slot is freed
    // ======================================================================
    private static void cancelBooking() {
        scenario("8. Cancel Bob's booking — then re-book same slot for Charlie");
        LocalDate date = today.plusDays(1);

        List<Booking> bobBookings = scheduler.getUpcomingBookings(bob.getId());
        if (!bobBookings.isEmpty()) {
            String bookingId = bobBookings.get(0).getId();
            System.out.println("Cancelling: " + bobBookings.get(0));
            scheduler.cancelBooking(bookingId);

            System.out.println("Bob's bookings after cancel: " + scheduler.getUpcomingBookings(bob.getId()).size());

            TimeSlot freedSlot = TimeSlot.of(date, 11, 0, 60);
            Booking charlieBooking = scheduler.bookRoom(confA.getId(), charlie.getId(), freedSlot);
            System.out.println("Charlie re-booked the freed slot: " + charlieBooking);
        }
        System.out.println();
    }

    // ======================================================================
    // 9. Validation failures — try various invalid inputs
    // ======================================================================
    private static void validationFailures() {
        scenario("9. Validation failure scenarios");
        LocalDate date = today.plusDays(2);

        expectFailure("past date",
            () -> scheduler.bookRoom(confA.getId(), alice.getId(),
                TimeSlot.of(today.minusDays(1), 10, 0, 60)));
        expectFailure("too far in future",
            () -> scheduler.bookRoom(confA.getId(), alice.getId(),
                TimeSlot.of(today.plusDays(40), 10, 0, 60)));
        expectFailure("outside working hours (7:00 AM)",
            () -> scheduler.bookRoom(confA.getId(), alice.getId(),
                TimeSlot.of(date, 7, 0, 60)));
        expectFailure("outside working hours (7:00 PM)",
            () -> scheduler.bookRoom(confA.getId(), alice.getId(),
                TimeSlot.of(date, 19, 0, 60)));
        expectFailure("non-30-min duration",
            () -> TimeSlot.of(date, 10, 0, 45));
        expectFailure("meeting exceeds 6 PM boundary",
            () -> scheduler.bookRoom(confA.getId(), alice.getId(),
                TimeSlot.of(date, 17, 0, 90)));
        expectFailure("duplicate email", () -> scheduler.registerUser("Alice2", "alice@example.com"));
        expectFailure("invalid room capacity", () -> scheduler.addRoom("BadRoom", 1, 1, Set.of()));
        expectFailure("cancel non-existent booking", () -> scheduler.cancelBooking("BKG-DEADBEEF"));
        System.out.println();
    }

    // ======================================================================
    // 10. Concurrency — two threads race to book the same slot
    // ======================================================================
    private static void concurrency() throws InterruptedException {
        scenario("10. Concurrency — 4 threads race for Board Room, same slot");
        TimeSlot slot = TimeSlot.of(today.plusDays(3), 14, 0, 60);

        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        int threadCount = 4;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    scheduler.bookRoom(boardRoom.getId(), alice.getId(), slot);
                    successCount.incrementAndGet();
                } catch (MeetingSchedulerException e) {
                    if (e.getMessage().contains("already booked")) {
                        conflictCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        start.countDown();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        System.out.println("Threads: " + threadCount);
        System.out.println("Successful bookings: " + successCount.get() + " (expected: 1)");
        System.out.println("Conflicts caught: " + conflictCount.get() + " (expected: " + (threadCount - 1) + ")");
        System.out.println("Invariant held: " + (successCount.get() == 1 ? "YES" : "NO — DOUBLE BOOKING!"));
        System.out.println();
    }

    // ======================================================================
    // 11. Final summary
    // ======================================================================
    private static void successSummary() {
        scenario("11. Success summary");
        System.out.println("Total users: " + scheduler.totalUsers());
        System.out.println("Total rooms: " + scheduler.totalRooms());
        System.out.println("Total bookings: " + scheduler.totalBookings());
        System.out.println("\nALL MEETING ROOM SCHEDULER SCENARIOS COMPLETED");
    }

    // ======================================================================
    // Helpers
    // ======================================================================
    private static void printHeader() {
        System.out.println("============================================================");
        System.out.println("MEETING ROOM SCHEDULER LLD DEMO");
        System.out.println("Patterns: Strategy (Filter pipeline) | Repository | Facade");
        System.out.println("Core invariant: BitSet-based slot availability, O(1) lookup");
        System.out.println("============================================================\n");
    }

    private static void printUsers(User... users) {
        for (User user : users) {
            System.out.println("  " + user.getName() + " | " + user.getId() + " | " + user.getEmail());
        }
        System.out.println();
    }

    private static void expectFailure(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  FAIL: expected error for " + label);
        } catch (MeetingSchedulerException | IllegalArgumentException ex) {
            System.out.println("  OK: " + label + " -> " + ex.getMessage());
        }
    }

    private static void scenario(String title) {
        System.out.println("------------------------------------------------------------");
        System.out.println(title);
        System.out.println("------------------------------------------------------------");
    }
}
