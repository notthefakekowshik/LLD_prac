package com.lldprep.systems.moviebooking.service.listener;

import com.lldprep.systems.moviebooking.model.Booking;
import com.lldprep.systems.moviebooking.service.BookingEventListener;

import java.util.List;

public class AuditEventListener implements BookingEventListener {

    private final List<String> auditLog;

    public AuditEventListener(List<String> auditLog) {
        this.auditLog = auditLog;
    }

    @Override
    public void onBookingConfirmed(Booking booking) {
        auditLog.add("CONFIRMED | %s | %s | %s | %s seats | ₹%s".formatted(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getShow().getMovieName(),
                booking.getSeats().size(),
                booking.getTotalAmount()
        ));
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        auditLog.add("CANCELLED | %s | %s | %s".formatted(
                booking.getBookingId(),
                booking.getUserId(),
                booking.getShow().getMovieName()
        ));
    }
}
