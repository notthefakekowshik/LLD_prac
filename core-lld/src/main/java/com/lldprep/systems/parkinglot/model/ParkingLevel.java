package com.lldprep.systems.parkinglot.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ParkingLevel {
    private final int levelNumber;
    private final List<ParkingSpot> spots;

    public ParkingLevel(int levelNumber, List<ParkingSpot> spots) {
        this.levelNumber = levelNumber;
        this.spots = new ArrayList<>(spots);
    }

    public int getLevelNumber() { return levelNumber; }

    public List<ParkingSpot> getSpots() {
        return Collections.unmodifiableList(spots);
    }

    public long availableCount() {
        return spots.stream().filter(ParkingSpot::isAvailable).count();
    }
}
