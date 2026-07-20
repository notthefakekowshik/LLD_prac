package com.lldprep.systems.meetingroomscheduler.repository;

import com.lldprep.systems.meetingroomscheduler.model.Booking;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BookingRepository {
    // Keyed by id so getById/removeById are O(1), matching Room/User repos.
    private final Map<String, Booking> bookings = new ConcurrentHashMap<>();

    public void save(Booking booking) {
        bookings.put(booking.getId(), booking);
    }

    public void removeById(String bookingId) {
        bookings.remove(bookingId);
    }

    public Booking getById(String bookingId) {
        return bookings.get(bookingId);
    }

    public List<Booking> findByUser(String userId) {
        return bookings.values().stream()
            .filter(b -> b.getUser().getId().equals(userId)
                && !b.getTimeSlot().date().isBefore(LocalDate.now()))
            .sorted(Comparator.comparing((Booking b) -> b.getTimeSlot().date())
                .thenComparing(b -> b.getTimeSlot().startSlot()))
            .toList();
    }

    public int count() {
        return bookings.size();
    }
}
