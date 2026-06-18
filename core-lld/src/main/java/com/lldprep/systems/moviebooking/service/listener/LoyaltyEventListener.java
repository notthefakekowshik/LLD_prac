package com.lldprep.systems.moviebooking.service.listener;

import com.lldprep.systems.moviebooking.model.Booking;
import com.lldprep.systems.moviebooking.service.BookingEventListener;

import java.util.Map;

public class LoyaltyEventListener implements BookingEventListener {

    private final Map<String, Integer> balances;
    private static final int POINTS_PER_RUPEE = 10;

    public LoyaltyEventListener(Map<String, Integer> balances) {
        this.balances = balances;
    }

    @Override
    public void onBookingConfirmed(Booking booking) {
        int points = (int) (booking.getTotalAmount() * POINTS_PER_RUPEE);
        balances.merge(booking.getUserId(), points, Integer::sum);
    }

    @Override
    public void onBookingCancelled(Booking booking) {
        // No-op
    }
}
