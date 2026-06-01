package com.lldprep.systems.moviebooking.model;

import com.lldprep.systems.moviebooking.model.enums.SeatCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Show {
    private final String id;
    private final String movieName;
    private final Screen screen;
    private final LocalDateTime showTime;
    private final int durationMinutes;
    private final double basePrice;

    public Show(String movieName, Screen screen, LocalDateTime showTime, int durationMinutes, double basePrice) {
        this.id = "SHOW-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        this.movieName = movieName;
        this.screen = screen;
        this.showTime = showTime;
        this.durationMinutes = durationMinutes;
        this.basePrice = basePrice;
    }

    public String getId() {
        return id;
    }

    public String getMovieName() {
        return movieName;
    }

    public Screen getScreen() {
        return screen;
    }

    public LocalDateTime getShowTime() {
        return showTime;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public double getPriceForSeat(Seat seat) {
        return basePrice * seat.getCategory().getPriceMultiplier();
    }

    public double getTotalPrice(List<Seat> seats) {
        return seats.stream()
            .mapToDouble(this::getPriceForSeat)
            .sum();
    }

    @Override
    public String toString() {
        return movieName + " | " + showTime.toLocalTime() + " | " + screen.getName()
            + " | ₹" + basePrice + " (Reg) / ₹" + (int)(basePrice * 1.4) + " (Prem) / ₹" + (int)(basePrice * 2) + " (Rec)";
    }
}
