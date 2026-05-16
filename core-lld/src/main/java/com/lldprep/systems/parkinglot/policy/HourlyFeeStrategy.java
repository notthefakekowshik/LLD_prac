package com.lldprep.systems.parkinglot.policy;

import com.lldprep.systems.parkinglot.model.Ticket;
import com.lldprep.systems.parkinglot.model.VehicleType;

import java.time.Duration;
import java.time.LocalDateTime;

// Why: different vehicle types occupy different spot sizes, justifying tiered rates.
//      Ceiling on partial hours is standard billing practice; minimum fee covers admin cost.
public class HourlyFeeStrategy implements FeeCalculationStrategy {

    private static final double BIKE_RATE  = 20.0;
    private static final double CAR_RATE   = 50.0;
    private static final double TRUCK_RATE = 100.0;
    private static final double MIN_FEE    = 10.0;

    @Override
    public double calculateFee(Ticket ticket, LocalDateTime exitTime) {
        Duration duration = Duration.between(ticket.getEntryTime(), exitTime);
        long hours = Math.max(1L, (long) Math.ceil(duration.toMinutes() / 60.0));
        double rate = rateFor(ticket.getVehicle().getVehicleType());
        return Math.max(MIN_FEE, hours * rate);
    }

    private double rateFor(VehicleType type) {
        return switch (type) {
            case BIKE  -> BIKE_RATE;
            case CAR   -> CAR_RATE;
            case TRUCK -> TRUCK_RATE;
        };
    }
}
