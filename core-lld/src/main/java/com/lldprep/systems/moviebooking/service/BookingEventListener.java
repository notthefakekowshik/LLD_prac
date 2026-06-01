package com.lldprep.systems.moviebooking.service;

import com.lldprep.systems.moviebooking.model.Booking;

public interface BookingEventListener {
    void onBookingConfirmed(Booking booking);
    void onBookingCancelled(Booking booking);
}
