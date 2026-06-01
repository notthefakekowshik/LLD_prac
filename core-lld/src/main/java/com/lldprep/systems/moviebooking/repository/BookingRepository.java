package com.lldprep.systems.moviebooking.repository;

import com.lldprep.systems.moviebooking.model.Booking;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BookingRepository {
    private final Map<String, Booking> bookingMap;

    public BookingRepository() {
        this.bookingMap = new LinkedHashMap<>();
    }

    public void save(Booking booking) {
        bookingMap.put(booking.getBookingId(), booking);
    }

    public Booking getById(String bookingId) {
        return bookingMap.get(bookingId);
    }

    public List<Booking> getByUser(String userId) {
        List<Booking> results = new ArrayList<>();
        for (Booking booking : bookingMap.values()) {
            if (booking.getUserId().equals(userId)) {
                results.add(booking);
            }
        }
        return results;
    }

    public List<Booking> getByShow(String showId) {
        List<Booking> results = new ArrayList<>();
        for (Booking booking : bookingMap.values()) {
            if (booking.getShow().getId().equals(showId)) {
                results.add(booking);
            }
        }
        return results;
    }
}
