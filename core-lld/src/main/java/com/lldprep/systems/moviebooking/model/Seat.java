package com.lldprep.systems.moviebooking.model;

import com.lldprep.systems.moviebooking.model.enums.SeatCategory;

import java.util.Objects;

public class Seat {
    private final String seatId;
    private final int row;
    private final int col;
    private final SeatCategory category;

    public Seat(int row, int col, SeatCategory category) {
        this.row = row;
        this.col = col;
        this.category = category;
        this.seatId = rowLabel(row) + (col + 1);
    }

    public static String rowLabel(int row) {
        return String.valueOf((char) ('A' + row));
    }

    public String getSeatId() {
        return seatId;
    }

    public int getRow() {
        return row;
    }

    public int getCol() {
        return col;
    }

    public SeatCategory getCategory() {
        return category;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Seat other)) {
            return false;
        }
        return Objects.equals(seatId, other.seatId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seatId);
    }

    @Override
    public String toString() {
        return seatId + "(" + category + ")";
    }
}
