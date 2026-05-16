package com.lldprep.systems.parkinglot.service;

import com.lldprep.systems.parkinglot.exception.InvalidTicketException;
import com.lldprep.systems.parkinglot.exception.NoSpotAvailableException;
import com.lldprep.systems.parkinglot.model.*;
import com.lldprep.systems.parkinglot.policy.FeeCalculationStrategy;
import com.lldprep.systems.parkinglot.policy.SpotAllocationStrategy;
import com.lldprep.systems.parkinglot.repository.TicketRepository;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class ParkingLotServiceImpl implements ParkingLotService {

    private final ParkingLot parkingLot;
    private final SpotAllocationStrategy allocationStrategy;
    private final FeeCalculationStrategy feeStrategy;
    private final TicketRepository ticketRepository;

    // CRITICAL SECTION — allocate + park must be atomic; otherwise two threads can see
    // the same spot as AVAILABLE and both try to park in it.
    private final ReentrantLock checkInLock = new ReentrantLock();

    private final AtomicLong totalRevenueCents = new AtomicLong();
    private final AtomicLong checkoutsProcessed = new AtomicLong();

    public ParkingLotServiceImpl(
            ParkingLot parkingLot,
            SpotAllocationStrategy allocationStrategy,
            FeeCalculationStrategy feeStrategy,
            TicketRepository ticketRepository) {
        this.parkingLot = parkingLot;
        this.allocationStrategy = allocationStrategy;
        this.feeStrategy = feeStrategy;
        this.ticketRepository = ticketRepository;
    }

    @Override
    public Ticket checkIn(Vehicle vehicle) {
        checkInLock.lock(); // CRITICAL SECTION — shared mutable state
        try {
            ParkingSpot spot = allocationStrategy
                .allocate(vehicle, parkingLot.getLevels())
                .orElseThrow(() -> new NoSpotAvailableException(vehicle.getVehicleType()));

            spot.park(vehicle);
            Ticket ticket = new Ticket(generateTicketId(), vehicle, spot, LocalDateTime.now());
            ticketRepository.save(ticket);
            return ticket;
        } finally {
            checkInLock.unlock();
        }
    }

    @Override
    public double checkOut(String ticketId) {
        // removeAndGet is atomic — concurrent double-checkout attempts both get the same
        // ticket object but only one can remove it; the loser gets InvalidTicketException.
        Ticket ticket = ticketRepository.removeAndGet(ticketId)
            .orElseThrow(() -> new InvalidTicketException(ticketId));

        LocalDateTime exitTime = LocalDateTime.now();
        double fee = feeStrategy.calculateFee(ticket, exitTime);
        ticket.checkout(exitTime, fee);
        ticket.getSpot().vacate();

        totalRevenueCents.addAndGet(Math.round(fee * 100));
        checkoutsProcessed.incrementAndGet();
        return fee;
    }

    @Override
    public long getAvailableSpots(VehicleType vehicleType) {
        return parkingLot.getLevels().stream()
            .flatMap(l -> l.getSpots().stream())
            .filter(s -> s.isAvailable() && s.getSpotType().canFit(vehicleType))
            .count();
    }

    @Override
    public ParkingLotMetrics getMetrics() {
        return new ParkingLotMetrics(
            parkingLot.getTotalSpots(),
            parkingLot.getAvailableSpots(),
            parkingLot.getOccupiedSpots(),
            ticketRepository.totalIssued(),
            checkoutsProcessed.get(),
            totalRevenueCents.get() / 100.0
        );
    }

    private String generateTicketId() {
        return "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
