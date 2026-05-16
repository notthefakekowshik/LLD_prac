package com.lldprep.systems.parkinglot.policy;

import com.lldprep.systems.parkinglot.model.ParkingLevel;
import com.lldprep.systems.parkinglot.model.ParkingSpot;
import com.lldprep.systems.parkinglot.model.Vehicle;

import java.util.List;
import java.util.Optional;

public interface SpotAllocationStrategy {
    Optional<ParkingSpot> allocate(Vehicle vehicle, List<ParkingLevel> levels);
}
