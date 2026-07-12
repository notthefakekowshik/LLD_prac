package com.lldprep.systems.meetingroomscheduler.repository;

import com.lldprep.systems.meetingroomscheduler.model.Booking;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class BookingRepository {
    private final List<Booking> bookings = new CopyOnWriteArrayList<>();

    public void save(Booking booking) {
        bookings.add(booking);
    }

    public void removeById(String bookingId) {
        bookings.removeIf(b -> b.getId().equals(bookingId));
    }

    public Booking getById(String bookingId) {
        return bookings.stream()
            .filter(b -> b.getId().equals(bookingId))
            .findFirst()
            .orElse(null);
    }

    public List<Booking> findByUser(String userId) {
        return bookings.stream()
            .filter(b -> b.getUser().getId().equals(userId)
                && !b.getTimeSlot().date().isBefore(LocalDate.now()))
            .sorted(Comparator.comparing((Booking b) -> b.getTimeSlot().date())
                .thenComparing(b -> b.getTimeSlot().startSlot()))
            .toList();
    }

    public List<Booking> findByRoomAndDate(String roomId, LocalDate date) {
        return bookings.stream()
            .filter(b -> b.getRoom().getId().equals(roomId) && b.getTimeSlot().date().equals(date))
            .sorted(Comparator.comparing(b -> b.getTimeSlot().startSlot()))
            .toList();
    }

    public int count() {
        return bookings.size();
    }
}
