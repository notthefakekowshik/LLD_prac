package com.lldprep.systems.moviebooking.model;

import com.lldprep.systems.moviebooking.model.enums.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class Booking {
    private final String bookingId;
    private final String userId;
    private final Show show;
    private final List<Seat> seats;
    private final double totalAmount;
    private BookingStatus status;
    private Payment payment;
    private final LocalDateTime createdAt;

    public Booking(String userId, Show show, List<Seat> seats, double totalAmount) {
        this.bookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.userId = userId;
        this.show = show;
        this.seats = List.copyOf(seats);
        this.totalAmount = totalAmount;
        this.status = BookingStatus.PENDING;
        this.payment = null;
        this.createdAt = LocalDateTime.now();
    }

    public void confirm(Payment payment) {
        this.status = BookingStatus.CONFIRMED;
        this.payment = payment;
    }

    public void cancel() {
        this.status = BookingStatus.CANCELLED;
    }

    public void expire() {
        this.status = BookingStatus.EXPIRED;
    }

    public String getBookingId() {
        return bookingId;
    }

    public String getUserId() {
        return userId;
    }

    public Show getShow() {
        return show;
    }

    public List<Seat> getSeats() {
        return seats;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Payment getPayment() {
        return payment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return bookingId + " | " + show.getMovieName() + " | " + seats.size() + " seats | ₹" + totalAmount
            + " | " + status + (payment != null ? " | " + payment : "");
    }
}
