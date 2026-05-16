package com.lldprep.systems.parkinglot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParkingLot {
    private final String name;
    private final String address;
    private final List<ParkingLevel> levels;

    public ParkingLot(String name, String address, List<ParkingLevel> levels) {
        this.name = name;
        this.address = address;
        this.levels = new ArrayList<>(levels);
    }

    public String getName()    { return name; }
    public String getAddress() { return address; }

    public List<ParkingLevel> getLevels() {
        return Collections.unmodifiableList(levels);
    }

    public long getTotalSpots() {
        return levels.stream().mapToLong(l -> l.getSpots().size()).sum();
    }

    public long getAvailableSpots() {
        return levels.stream().mapToLong(ParkingLevel::availableCount).sum();
    }

    public long getOccupiedSpots() {
        return getTotalSpots() - getAvailableSpots();
    }
}
