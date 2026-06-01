package com.lldprep.systems.moviebooking.model;

import com.lldprep.systems.moviebooking.model.enums.SeatCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Screen {
    private final String id;
    private final String name;
    private final int rows;
    private final int cols;
    private final List<Seat> seats;

    public Screen(String id, String name, int rows, int cols) {
        this.id = id;
        this.name = name;
        this.rows = rows;
        this.cols = cols;
        this.seats = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            SeatCategory category = categoryForRow(r, rows);
            for (int c = 0; c < cols; c++) {
                seats.add(new Seat(r, c, category));
            }
        }
    }

    private static SeatCategory categoryForRow(int row, int totalRows) {
        if (totalRows <= 4) {
            return SeatCategory.REGULAR;
        }
        if (row >= totalRows - 1) {
            return SeatCategory.RECLINER;
        }
        if (row >= totalRows - 3) {
            return SeatCategory.PREMIUM;
        }
        return SeatCategory.REGULAR;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public int getTotalSeats() {
        return rows * cols;
    }

    public List<Seat> getSeats() {
        return Collections.unmodifiableList(seats);
    }

    @Override
    public String toString() {
        return name + " (" + rows + "x" + cols + ")";
    }
}
