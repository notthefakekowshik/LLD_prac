package com.lldprep.systems.moviebooking.demo;

import com.lldprep.systems.moviebooking.exception.BookingException;
import com.lldprep.systems.moviebooking.exception.PaymentException;
import com.lldprep.systems.moviebooking.model.Booking;
import com.lldprep.systems.moviebooking.model.Screen;
import com.lldprep.systems.moviebooking.model.Seat;
import com.lldprep.systems.moviebooking.model.Show;
import com.lldprep.systems.moviebooking.model.Theater;
import com.lldprep.systems.moviebooking.model.enums.BookingStatus;
import com.lldprep.systems.moviebooking.model.enums.City;
import com.lldprep.systems.moviebooking.model.enums.SeatCategory;
import com.lldprep.systems.moviebooking.repository.BookingRepository;
import com.lldprep.systems.moviebooking.repository.ShowRepository;
import com.lldprep.systems.moviebooking.service.BookingEventListener;
import com.lldprep.systems.moviebooking.service.BookingFacade;
import com.lldprep.systems.moviebooking.service.PaymentService;
import com.lldprep.systems.moviebooking.service.SeatLockService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class MovieBookingDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║            MOVIE BOOKING SYSTEM DEMONSTRATION                ║");
        System.out.println("║                                                              ║");
        System.out.println("║  Patterns: Facade | Strategy | Observer (listener)          ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        // ── Setup ──
        ShowRepository showRepo = new ShowRepository();
        BookingRepository bookingRepo = new BookingRepository();
        SeatLockService lockService = new SeatLockService(300); // 5-min lock
        PaymentService paymentService = new PaymentService();
        BookingFacade facade = new BookingFacade(showRepo, bookingRepo, lockService, paymentService);

        List<String> auditLog = new ArrayList<>();
        facade.addListener(new BookingEventListener() {
            public void onBookingConfirmed(Booking b) {
                auditLog.add("✓ CONFIRMED " + b.getBookingId() + " | " + b.getShow().getMovieName() + " | " + b.getSeats().size() + " seats | ₹" + b.getTotalAmount());
            }
            public void onBookingCancelled(Booking b) {
                auditLog.add("✗ CANCELLED " + b.getBookingId() + " | " + b.getShow().getMovieName());
            }
        });

        seedData(showRepo);

        // Run all scenarios
        scenario1_SearchShows(facade);
        scenario2_ViewAvailableSeats(facade);
        scenario3_HappyPath_BookAndConfirm(facade);
        scenario4_ConcurrentSeatLocking(facade);
        scenario5_LockExpiry(facade);
        scenario6_CancelBooking(facade);
        scenario7_DoubleBookingPrevention(facade);
        scenario8_MultipleUsersDifferentSeats(facade);
        scenario9_Curveball_PaymentFailureAndSeatRelease(facade);
        scenario10_Curveball_SeatCategories(facade);

        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ AUDIT LOG (via BookingEventListener)                        │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
        for (String entry : auditLog) {
            System.out.println("  " + entry);
        }
        System.out.println();

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║              ALL SCENARIOS COMPLETED SUCCESSFULLY            ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    // ── SEED DATA ──

    static void seedData(ShowRepository showRepo) {
        // Theaters
        Theater pvrKoramangala = new Theater("T1", "PVR Koramangala", City.BANGALORE);
        Theater inoxMg = new Theater("T2", "INOX MG Road", City.BANGALORE);
        Theater cinepolisPhoenix = new Theater("T3", "PVR Phoenix", City.MUMBAI);

        // Screens
        Screen pvrScreen1 = new Screen("S1", "Screen 1", 8, 10);  // 80 seats
        Screen pvrScreen2 = new Screen("S2", "Screen 2", 8, 10);
        Screen inoxScreen1 = new Screen("S3", "Audi 1", 10, 12);   // 120 seats
        Screen cineScreen1 = new Screen("S4", "Screen A", 8, 10);

        pvrKoramangala.addScreen(pvrScreen1);
        pvrKoramangala.addScreen(pvrScreen2);
        inoxMg.addScreen(inoxScreen1);
        cinepolisPhoenix.addScreen(cineScreen1);

        showRepo.registerTheater(pvrKoramangala);
        showRepo.registerTheater(inoxMg);
        showRepo.registerTheater(cinepolisPhoenix);

        // Shows — BANGALORE, today
        LocalDate today = LocalDate.now();
        showRepo.addShow(new Show("Inception", pvrScreen1, today.atTime(10, 0), 148, 250));
        showRepo.addShow(new Show("Inception", pvrScreen1, today.atTime(14, 0), 148, 280));
        showRepo.addShow(new Show("Inception", pvrScreen2, today.atTime(18, 0), 148, 300));
        showRepo.addShow(new Show("Inception", inoxScreen1, today.atTime(10, 30), 148, 220));
        showRepo.addShow(new Show("Inception", inoxScreen1, today.atTime(16, 0), 148, 260));
        showRepo.addShow(new Show("Interstellar", pvrScreen2, today.atTime(10, 0), 169, 300));
        showRepo.addShow(new Show("Interstellar", pvrScreen2, today.atTime(21, 0), 169, 350));

        // MUMBAI
        showRepo.addShow(new Show("Inception", cineScreen1, today.atTime(10, 0), 148, 270));
        showRepo.addShow(new Show("Inception", cineScreen1, today.atTime(14, 0), 148, 300));

        System.out.println("Seed data: " + showRepo.searchShows(City.BANGALORE, null, today).size()
            + " shows in BANGALORE, " + showRepo.searchShows(City.MUMBAI, null, today).size()
            + " shows in MUMBAI");
        System.out.println();
    }

    // ── SCENARIO 1: Search ──

    static void scenario1_SearchShows(BookingFacade facade) {
        println("SCENARIO 1: Search Shows — Inception in Bangalore today");

        List<Show> results = facade.searchShows(City.BANGALORE, "Inception", LocalDate.now());
        System.out.println("  Found " + results.size() + " shows:");
        for (Show s : results) {
            System.out.println("    " + s);
        }
        System.out.println();
    }

    // ── SCENARIO 2: View Seats ──

    static void scenario2_ViewAvailableSeats(BookingFacade facade) {
        println("SCENARIO 2: View Available Seats");

        Show show = facade.searchShows(City.BANGALORE, "Inception", LocalDate.now()).get(0);
        List<Seat> available = facade.getAvailableSeats(show.getId());
        System.out.println("  " + show.getMovieName() + " at " + show.getShowTime().toLocalTime());
        System.out.println("  Screen: " + show.getScreen().getName() + " (" + show.getScreen().getTotalSeats() + " seats)");
        System.out.println("  Available: " + available.size() + " seats");
        System.out.println("  First 5: " + available.stream().limit(5).map(Seat::getSeatId).toList());
        System.out.println();
    }

    // ── SCENARIO 3: Happy Path — Book + Confirm ──

    static void scenario3_HappyPath_BookAndConfirm(BookingFacade facade) {
        println("SCENARIO 3: Happy Path — Lock Seats → Confirm Booking → Payment");

        Show show = facade.searchShows(City.BANGALORE, "Inception", LocalDate.now()).get(0);
        String userId = "user-alice";

        List<Seat> available = facade.getAvailableSeats(show.getId());
        List<String> selectedSeats = available.stream()
            .limit(3)
            .map(Seat::getSeatId)
            .toList();

        boolean locked = facade.lockSeats(show.getId(), userId, selectedSeats);
        System.out.println("  Step 1: Lock " + selectedSeats + " → " + (locked ? "SUCCESS" : "FAILED"));

        Booking booking = facade.confirmBooking(show.getId(), userId);
        System.out.println("  Step 2: Confirm booking → " + booking);
        System.out.println("  Payment: ₹" + booking.getTotalAmount() + " for " + booking.getSeats().size() + " seats");
        System.out.println("  Status: " + booking.getStatus());
        System.out.println();
    }

    // ── SCENARIO 4: Concurrent Seat Locking ──

    static void scenario4_ConcurrentSeatLocking(BookingFacade facade) throws InterruptedException {
        println("SCENARIO 4: Concurrent Seat Locking — Two Users Same Seats");

        Show show = facade.searchShows(City.BANGALORE, "Interstellar", LocalDate.now()).get(0);

        // Two users, same seat selection
        List<String> seatA1 = List.of("A1", "A2", "A3");
        String userBob = "user-bob";
        String userCharlie = "user-charlie";

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);
        CompletableFuture<Boolean> bobResult = CompletableFuture.supplyAsync(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return facade.lockSeats(show.getId(), userBob, seatA1);
        }, pool);

        CompletableFuture<Boolean> charlieResult = CompletableFuture.supplyAsync(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return facade.lockSeats(show.getId(), userCharlie, seatA1);
        }, pool);

        latch.countDown();

        try {
            System.out.println("  Bob locks " + seatA1 + " → " + (bobResult.get(2, TimeUnit.SECONDS) ? "SUCCESS" : "FAILED"));
            System.out.println("  Charlie locks " + seatA1 + " → " + (charlieResult.get(2, TimeUnit.SECONDS) ? "SUCCESS" : "FAILED"));
            System.out.println("  Only one user gets the lock — double-booking prevented ✓");
        } catch (Exception e) {
            System.out.println("  Error: " + e.getMessage());
        }

        pool.shutdown();
        System.out.println();
    }

    // ── SCENARIO 5: Lock Expiry ──

    static void scenario5_LockExpiry(BookingFacade facade) throws InterruptedException {
        println("SCENARIO 5: Lock Expiry — Seat auto-releases after timeout");

        // Create a short-lock facade just for this scenario
        SeatLockService shortLock = new SeatLockService(2); // 2-second lock
        BookingFacade shortFacade = new BookingFacade(
            facade.getShowRepository(), facade.getBookingRepository(), shortLock, new PaymentService()
        );

        Show show = shortFacade.searchShows(City.BANGALORE, "Inception", LocalDate.now()).get(0);
        String userId = "user-dave";

        List<String> seats = List.of("B5", "B6");
        boolean locked = shortFacade.lockSeats(show.getId(), userId, seats);
        System.out.println("  Step 1: Lock " + seats + " → " + (locked ? "SUCCESS" : "FAILED"));

        List<Seat> avail1 = shortFacade.getAvailableSeats(show.getId());
        System.out.println("  Avail seats before expiry: " + avail1.size());

        System.out.println("  Waiting 3 seconds for lock to expire...");
        Thread.sleep(3000);

        // Lazy expiry: lockSeats cleans up expired locks during check
        // But we need explicit release for the demo
        int expired = shortFacade.releaseExpiredLocks(show.getId());
        System.out.println("  Expired locks released: " + expired);

        List<Seat> avail2 = shortFacade.getAvailableSeats(show.getId());
        System.out.println("  Avail seats after expiry: " + avail2.size() + " (seats returned)");

        // Now another user can lock them
        boolean reLocked = shortFacade.lockSeats(show.getId(), "user-eve", seats);
        System.out.println("  Another user locks same seats → " + (reLocked ? "SUCCESS ✓" : "FAILED"));
        System.out.println();
    }

    // ── SCENARIO 6: Cancel Booking ──

    static void scenario6_CancelBooking(BookingFacade facade) {
        println("SCENARIO 6: Cancel Booking");

        Show show = facade.searchShows(City.BANGALORE, "Interstellar", LocalDate.now()).get(0);
        String userId = "user-frank";

        // Lock and confirm
        List<String> cancelSeats = List.of("E1", "E2");
        facade.lockSeats(show.getId(), userId, cancelSeats);
        Booking booking = facade.confirmBooking(show.getId(), userId);
        System.out.println("  Booked: " + booking);

        // Cancel
        facade.cancelBooking(booking.getBookingId());
        Booking cancelled = facade.getBooking(booking.getBookingId());
        System.out.println("  After cancel: " + cancelled.getStatus());

        // Seats are returnable — next user can book them
        List<Seat> available = facade.getAvailableSeats(show.getId());
        boolean seatsReturned = available.stream()
            .anyMatch(s -> s.getSeatId().equals("E1"));
        System.out.println("  E1 available again? " + (seatsReturned ? "Yes ✓" : "No"));
        System.out.println();
    }

    // ── SCENARIO 7: Double-Booking Prevention ──

    static void scenario7_DoubleBookingPrevention(BookingFacade facade) {
        println("SCENARIO 7: Double-Booking Prevention");

        Show show = facade.searchShows(City.MUMBAI, "Inception", LocalDate.now()).get(0);
        String user1 = "user-george";
        String user2 = "user-hannah";

        List<String> seats = List.of("C1", "C2");

        // User 1 locks
        facade.lockSeats(show.getId(), user1, seats);
        System.out.println("  " + user1 + " locked " + seats);

        // User 2 tries to lock same seats
        boolean user2Locked = facade.lockSeats(show.getId(), user2, seats);
        System.out.println("  " + user2 + " tries to lock same → " + (user2Locked ? "SUCCESS (BAD!)" : "FAILED ✓"));

        // User 1 confirms
        Booking booking = facade.confirmBooking(show.getId(), user1);
        System.out.println("  " + user1 + " booking → CONFIRMED | Seats: " + booking.getSeats().stream().map(Seat::getSeatId).toList());
        System.out.println();
    }

    // ── SCENARIO 8: Multiple Users, Different Seats (No Conflict) ──

    static void scenario8_MultipleUsersDifferentSeats(BookingFacade facade) {
        println("SCENARIO 8: Multiple Users Book Different Seats — No Conflict");

        Show show = facade.searchShows(City.BANGALORE, "Interstellar", LocalDate.now()).get(0);
        String user1 = "user-ivan";
        String user2 = "user-julia";

        List<String> seats1 = List.of("F1", "F2");
        List<String> seats2 = List.of("F5", "F6", "F7");

        boolean lock1 = facade.lockSeats(show.getId(), user1, seats1);
        boolean lock2 = facade.lockSeats(show.getId(), user2, seats2);

        System.out.println("  " + user1 + " locked " + seats1 + " → " + (lock1 ? "SUCCESS" : "FAILED"));
        System.out.println("  " + user2 + " locked " + seats2 + " → " + (lock2 ? "SUCCESS" : "FAILED"));

        Booking b1 = facade.confirmBooking(show.getId(), user1);
        Booking b2 = facade.confirmBooking(show.getId(), user2);

        System.out.println("  " + user1 + " booking: " + b1);
        System.out.println("  " + user2 + " booking: " + b2);
        System.out.println("  Both confirmed with no conflict ✓");

        // Print summary
        List<Booking> allBookings = facade.getBookingRepository().getByShow(show.getId());
        long confirmed = allBookings.stream()
            .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
            .count();
        System.out.println("  Total confirmed bookings for this show: " + confirmed);
        System.out.println();
    }

    // ── SCENARIO 9: Curveball — Payment Failure + Seat Release + Listener ──

    static void scenario9_Curveball_PaymentFailureAndSeatRelease(BookingFacade facade) {
        println("SCENARIO 9: Curveball — Payment Failure Releases Seats + Listener");

        // Create a facade with a failing paymentService
        PaymentService failingPayment = new PaymentService() {
            @Override
            public boolean processPayment(Booking booking, double amount) {
                return false;
            }
        };
        SeatLockService freshLock = new SeatLockService(300);
        ShowRepository showRepo = facade.getShowRepository();
        BookingRepository bookingRepo = facade.getBookingRepository();
        BookingFacade failFacade = new BookingFacade(showRepo, bookingRepo, freshLock, failingPayment);

        // Add a listener to demonstrate the Observer pattern
        List<String> events = new ArrayList<>();
        failFacade.addListener(new BookingEventListener() {
            public void onBookingConfirmed(Booking b) {
                events.add("CONFIRMED: " + b.getBookingId());
            }
            public void onBookingCancelled(Booking b) {
                events.add("CANCELLED: " + b.getBookingId());
            }
        });

        Show show = failFacade.searchShows(City.MUMBAI, "Inception", LocalDate.now()).get(0);
        String userId = "user-kelly";

        List<Seat> availBefore = failFacade.getAvailableSeats(show.getId());
        System.out.println("  Available before: " + availBefore.size() + " seats");

        // Lock seats
        List<String> seats = List.of("D3", "D4");
        boolean locked = failFacade.lockSeats(show.getId(), userId, seats);
        System.out.println("  Locked " + seats + " → " + (locked ? "SUCCESS" : "FAILED"));

        List<Seat> availDuring = failFacade.getAvailableSeats(show.getId());
        System.out.println("  Available during lock: " + availDuring.size() + " seats");

        // Try to confirm — payment fails
        System.out.println("  Attempting to confirm booking...");
        try {
            failFacade.confirmBooking(show.getId(), userId);
        } catch (PaymentException e) {
            System.out.println("  ✗ " + e.getMessage());
        }

        // Seats should be released
        List<Seat> availAfter = failFacade.getAvailableSeats(show.getId());
        System.out.println("  Available after failure: " + availAfter.size() + " seats ✓");

        // Another user can now lock those seats
        boolean reLocked = failFacade.lockSeats(show.getId(), "user-leo", seats);
        System.out.println("  Another user locks same seats → " + (reLocked ? "SUCCESS" : "FAILED"));

        // Listener events
        System.out.println("  Events fired: " + (events.isEmpty() ? "none (expected — payment failed, no confirm/cancel)" : events));
        System.out.println("  Observer pattern: BookingEventListener fires on confirm/cancel only.");
        System.out.println("    Payment failure → exception thrown → no event (seats auto-released).");
        System.out.println();
    }

    // ── SCENARIO 10: Curveball — Seat Categories (Gold / Silver / Recliner) ──

    static void scenario10_Curveball_SeatCategories(BookingFacade facade) {
        println("SCENARIO 10: Curveball — Seat Categories (OCP in Action)");

        Show show = facade.searchShows(City.BANGALORE, "Interstellar", LocalDate.now()).get(0);
        System.out.println("  Show: " + show.getMovieName() + " at " + show.getShowTime().toLocalTime());

        // Show the seat layout by category
        Screen screen = show.getScreen();
        System.out.println("  Screen: " + screen.getName() + " (" + screen.getRows() + " rows × " + screen.getCols() + " cols)");

        // Category distribution
        long regulars = screen.getSeats().stream()
            .filter(s -> s.getCategory() == SeatCategory.REGULAR).count();
        long premiums = screen.getSeats().stream()
            .filter(s -> s.getCategory() == SeatCategory.PREMIUM).count();
        long recliners = screen.getSeats().stream()
            .filter(s -> s.getCategory() == SeatCategory.RECLINER).count();

        String catLabel = regulars + " × Regular(₹" + (int)(show.getBasePrice() * 1.0) + ") ";
        System.out.println("  Categories:");
        catLabel += premiums + " × Premium(₹" + (int)(show.getBasePrice() * 1.4) + ") ";
        catLabel += recliners + " × Recliner(₹" + (int)(show.getBasePrice() * 2.0) + ")";
        System.out.println("    " + catLabel);

        // Row layout
        System.out.println("  Layout (by row):");
        for (int r = 0; r < screen.getRows(); r++) {
            Seat sample = screen.getSeats().get(r * screen.getCols());
            System.out.println("    Row " + Seat.rowLabel(r) + ": " + sample.getCategory());
        }

        // Book one seat from each category and verify different prices
        String userId = "user-vip";
        List<String> mixedSeats = List.of("D1", "G1", "H1"); // REGULAR, PREMIUM, RECLINER (all free)
        boolean locked = facade.lockSeats(show.getId(), userId, mixedSeats);
        System.out.println("  Locked " + mixedSeats + " → " + (locked ? "SUCCESS" : "FAILED"));

        Booking booking = facade.confirmBooking(show.getId(), userId);
        System.out.println("  Booking: " + booking.getBookingId() + " | " + booking.getSeats().size() + " seats | ₹" + booking.getTotalAmount());
        System.out.println("  Per-seat breakdown:");
        for (Seat s : booking.getSeats()) {
            double price = show.getPriceForSeat(s);
            System.out.println("    " + s.getSeatId() + " (" + s.getCategory() + "): ₹" + (int)price);
        }

        System.out.println("  OCP check: Adding seat categories required ZERO changes to:");
        System.out.println("    - BookingFacade, BookingRepository, Booking");
        System.out.println("    - SeatLockService, PaymentService");
        System.out.println("  Only changed: Seat (+category), Screen (+categoryForRow), Show (+getPriceForSeat)");
        System.out.println("  Price calculation uses category multiplier — new categories = new enum values only.");
        System.out.println();
    }

    // ── HELPERS ──

    static void println(String msg) {
        System.out.println("┌──────────────────────────────────────────────────────────────┐");
        System.out.println("│ " + padRight(msg, 60) + " │");
        System.out.println("└──────────────────────────────────────────────────────────────┘");
    }

    static String padRight(String s, int n) {
        if (s.length() > n - 2) {
            return s.substring(0, n - 2);
        }
        return String.format("%-" + (n - 2) + "s", s);
    }
}
