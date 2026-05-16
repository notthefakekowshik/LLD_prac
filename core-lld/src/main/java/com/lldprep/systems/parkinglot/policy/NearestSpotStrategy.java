package com.lldprep.systems.parkinglot.policy;

import com.lldprep.systems.parkinglot.model.ParkingLevel;
import com.lldprep.systems.parkinglot.model.ParkingSpot;
import com.lldprep.systems.parkinglot.model.SpotType;
import com.lldprep.systems.parkinglot.model.Vehicle;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// Why: first pass finds the preferred (smallest) spot type to avoid oversized-spot waste;
//      second pass overflows to the next-smallest compatible type when preferred is full.
//      Levels are scanned in order so lower levels are always preferred (nearest to entry).
public class NearestSpotStrategy implements SpotAllocationStrategy {

    @Override
    public Optional<ParkingSpot> allocate(Vehicle vehicle, List<ParkingLevel> levels) {
        SpotType preferred = SpotType.preferredFor(vehicle.getVehicleType());

        // First pass: exact preferred type, level-order
        for (ParkingLevel level : levels) {
            Optional<ParkingSpot> spot = level.getSpots().stream()
                .filter(s -> s.getSpotType() == preferred && s.isAvailable())
                .findFirst();
            if (spot.isPresent()) return spot;
        }

        // Second pass: any compatible type — pick smallest available across all levels
        return levels.stream()
            .flatMap(l -> l.getSpots().stream())
            .filter(s -> s.getSpotType().canFit(vehicle.getVehicleType()) && s.isAvailable())
            .min(Comparator.comparingInt(s -> s.getSpotType().ordinal()));
    }
}
