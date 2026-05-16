package com.lldprep.systems.parkinglot.demo;

import com.lldprep.systems.parkinglot.exception.NoSpotAvailableException;
import com.lldprep.systems.parkinglot.factory.VehicleFactory;
import com.lldprep.systems.parkinglot.model.*;
import com.lldprep.systems.parkinglot.policy.HourlyFeeStrategy;
import com.lldprep.systems.parkinglot.policy.NearestSpotStrategy;
import com.lldprep.systems.parkinglot.repository.InMemoryTicketRepository;
import com.lldprep.systems.parkinglot.service.ParkingLotService;
import com.lldprep.systems.parkinglot.service.ParkingLotServiceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ParkingLotDemo {

    public static void main(String[] args) throws InterruptedException {
        ParkingLotService service = buildParkingLot();
        System.out.println("=== Parking Lot System Demo ===");
        System.out.println("Lot: City Center Parking — 2 levels, 16 total spots\n");

        // ── FR1: Check-in — spot is assigned and ticket returned ──
        System.out.println("--- FR1: Check-in ---");
        Vehicle bike  = VehicleFactory.create(VehicleType.BIKE,  "MH-01-AA-1234");
        Vehicle car1  = VehicleFactory.create(VehicleType.CAR,   "MH-01-BB-5678");
        Vehicle car2  = VehicleFactory.create(VehicleType.CAR,   "MH-01-CC-9999");
        Vehicle truck = VehicleFactory.create(VehicleType.TRUCK, "MH-01-DD-0001");

        Ticket bikeTicket  = service.checkIn(bike);
        Ticket car1Ticket  = service.checkIn(car1);
        Ticket car2Ticket  = service.checkIn(car2);
        Ticket truckTicket = service.checkIn(truck);

        System.out.println("IN  " + bike  + "  → " + bikeTicket.getTicketId()  + " @ " + bikeTicket.getSpot());
        System.out.println("IN  " + car1  + "  → " + car1Ticket.getTicketId()  + " @ " + car1Ticket.getSpot());
        System.out.println("IN  " + car2  + "  → " + car2Ticket.getTicketId()  + " @ " + car2Ticket.getSpot());
        System.out.println("IN  " + truck + " → " + truckTicket.getTicketId() + " @ " + truckTicket.getSpot());

        // ── FR3: Available spots by type ──
        System.out.println("\n--- FR3: Available spots ---");
        System.out.println("BIKE-compatible:  " + service.getAvailableSpots(VehicleType.BIKE));
        System.out.println("CAR-compatible:   " + service.getAvailableSpots(VehicleType.CAR));
        System.out.println("TRUCK-compatible: " + service.getAvailableSpots(VehicleType.TRUCK));

        // ── FR2: Check-out — spot freed, fee computed ──
        System.out.println("\n--- FR2: Check-out ---");
        Thread.sleep(50); // simulate parking time
        double bikeFee = service.checkOut(bikeTicket.getTicketId());
        double car1Fee = service.checkOut(car1Ticket.getTicketId());
        System.out.printf("OUT %s  fee=₹%.2f%n", bike, bikeFee);
        System.out.printf("OUT %s  fee=₹%.2f%n", car1, car1Fee);
        System.out.println("Freed spot now available: " + bikeTicket.getSpot().isAvailable());

        // ── FR4: Spot re-use after checkout ──
        System.out.println("\n--- FR4: Spot reuse ---");
        Vehicle newBike = VehicleFactory.create(VehicleType.BIKE, "MH-02-ZZ-0001");
        Ticket newBikeTicket = service.checkIn(newBike);
        System.out.println("IN  " + newBike + " → " + newBikeTicket.getSpot());

        // ── FR5: Full lot — overflow rejected with exception ──
        System.out.println("\n--- FR5: Lot full scenario (fill all TRUCK spots) ---");
        List<Ticket> overflow = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                Vehicle v = VehicleFactory.create(VehicleType.TRUCK, "TRUCK-" + i);
                overflow.add(service.checkIn(v));
            }
        } catch (NoSpotAvailableException e) {
            System.out.println("Expected rejection: " + e.getMessage());
        }
        System.out.println("TRUCK spots left: " + service.getAvailableSpots(VehicleType.TRUCK));

        // ── FR6: Metrics ──
        System.out.println("\n--- FR6: Metrics ---");
        ParkingLotMetrics m = service.getMetrics();
        System.out.println("Total spots:     " + m.totalSpots());
        System.out.println("Available:       " + m.availableSpots());
        System.out.println("Occupied:        " + m.occupiedSpots());
        System.out.printf("Occupancy rate:  %.1f%%%n", m.occupancyRate());
        System.out.println("Tickets issued:  " + m.ticketsIssued());
        System.out.println("Checkouts done:  " + m.checkoutsProcessed());
        System.out.printf("Revenue:         ₹%.2f%n", m.totalRevenue());

        // ── FR7: Concurrent check-in (thread-safety) ──
        System.out.println("\n--- FR7: Concurrent check-in (10 threads) ---");
        ParkingLotService freshService = buildParkingLot();
        int threads = 10;
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<String> errors = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            final int idx = i;
            pool.submit(() -> {
                ready.countDown();
                try { start.await(); } catch (InterruptedException ignored) {}
                try {
                    Vehicle v = VehicleFactory.create(VehicleType.CAR, "CONCURRENT-" + idx);
                    freshService.checkIn(v);
                } catch (NoSpotAvailableException e) {
                    synchronized (errors) { errors.add("Rejected (lot full): CONCURRENT-" + idx); }
                }
            });
        }
        ready.await();
        start.countDown();
        pool.shutdown();
        pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);

        ParkingLotMetrics cm = freshService.getMetrics();
        System.out.println("Threads attempted: " + threads);
        System.out.println("Occupied spots:    " + cm.occupiedSpots());
        System.out.println("Rejections:        " + errors.size());
        System.out.println("No double-booking: " + (cm.occupiedSpots() + errors.size() == threads));

        // cleanup
        for (Ticket t : overflow) {
            try { service.checkOut(t.getTicketId()); } catch (Exception ignored) {}
        }
    }

    private static ParkingLotService buildParkingLot() {
        // Level 1: 3 COMPACT, 4 REGULAR, 2 LARGE  (9 spots)
        List<ParkingSpot> l1 = new ArrayList<>();
        for (int i = 1; i <= 3; i++) l1.add(new ParkingSpot("L1-C" + i, 1, i, SpotType.COMPACT));
        for (int i = 4; i <= 7; i++) l1.add(new ParkingSpot("L1-R" + i, 1, i, SpotType.REGULAR));
        for (int i = 8; i <= 9; i++) l1.add(new ParkingSpot("L1-L" + i, 1, i, SpotType.LARGE));

        // Level 2: 2 COMPACT, 3 REGULAR, 2 LARGE  (7 spots)
        List<ParkingSpot> l2 = new ArrayList<>();
        for (int i = 1; i <= 2; i++) l2.add(new ParkingSpot("L2-C" + i, 2, i, SpotType.COMPACT));
        for (int i = 3; i <= 5; i++) l2.add(new ParkingSpot("L2-R" + i, 2, i, SpotType.REGULAR));
        for (int i = 6; i <= 7; i++) l2.add(new ParkingSpot("L2-L" + i, 2, i, SpotType.LARGE));

        List<ParkingLevel> levels = List.of(
            new ParkingLevel(1, l1),
            new ParkingLevel(2, l2)
        );
        ParkingLot lot = new ParkingLot("City Center Parking", "123 Main St", levels);

        return new ParkingLotServiceImpl(
            lot,
            new NearestSpotStrategy(),
            new HourlyFeeStrategy(),
            new InMemoryTicketRepository()
        );
    }
}
