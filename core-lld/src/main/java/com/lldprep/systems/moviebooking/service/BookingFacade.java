package com.lldprep.systems.moviebooking.service;

import com.lldprep.systems.moviebooking.exception.BookingException;
import com.lldprep.systems.moviebooking.exception.PaymentException;
import com.lldprep.systems.moviebooking.exception.SeatLockException;
import com.lldprep.systems.moviebooking.model.Booking;
import com.lldprep.systems.moviebooking.model.Payment;
import com.lldprep.systems.moviebooking.model.Seat;
import com.lldprep.systems.moviebooking.model.Show;
import com.lldprep.systems.moviebooking.model.enums.BookingStatus;
import com.lldprep.systems.moviebooking.model.enums.City;
import com.lldprep.systems.moviebooking.repository.BookingRepository;
import com.lldprep.systems.moviebooking.repository.ShowRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * FACADE pattern — single entry point for all booking operations.
 * Hides ShowRepository, BookingRepository, SeatLockService, PaymentService
 * behind a clean interface. Callers never deal with repositories directly.
 */
public class BookingFacade {

    private final ShowRepository showRepository;
    private final BookingRepository bookingRepository;
    private final SeatLockService lockService;
    private final PaymentService paymentService;
    private final List<BookingEventListener> listeners;

    public BookingFacade(ShowRepository showRepository,
                         BookingRepository bookingRepository,
                         SeatLockService lockService,
                         PaymentService paymentService) {
        this.showRepository = showRepository;
        this.bookingRepository = bookingRepository;
        this.lockService = lockService;
        this.paymentService = paymentService;
        this.listeners = new ArrayList<>();
    }

    public void addListener(BookingEventListener listener) {
        listeners.add(listener);
    }

    private void fireBookingConfirmed(Booking booking) {
        for (BookingEventListener listener : listeners) {
            listener.onBookingConfirmed(booking);
        }
    }

    private void fireBookingCancelled(Booking booking) {
        for (BookingEventListener listener : listeners) {
            listener.onBookingCancelled(booking);
        }
    }

    // ── Search ──

    public List<Show> searchShows(City city, String movieName, LocalDate date) {
        return showRepository.searchShows(city, movieName, date);
    }

    // ── Seat Availability ──

    public List<Seat> getAvailableSeats(String showId) {
        Show show = showRepository.getById(showId);
        if (show == null) {
            throw new BookingException("Show not found: " + showId);
        }

        List<String> bookedSeatIds = getBookedSeats(showId);

        List<Seat> available = new ArrayList<>();
        for (Seat seat : show.getScreen().getSeats()) {
            if (!lockService.isSeatLocked(showId, seat.getSeatId())
                && !bookedSeatIds.contains(seat.getSeatId())) {
                available.add(seat);
            }
        }
        return available;
    }

    public List<String> getBookedSeats(String showId) {
        Show show = showRepository.getById(showId);
        if (show == null) {
            throw new BookingException("Show not found: " + showId);
        }

        List<String> booked = new ArrayList<>();
        for (Booking b : bookingRepository.getByShow(showId)) {
            if (b.getStatus() == BookingStatus.CONFIRMED) {
                for (Seat s : b.getSeats()) {
                    booked.add(s.getSeatId());
                }
            }
        }
        return booked;
    }

    // ── Lock ──

    /**
     * Attempts to lock seats for a user. All-or-nothing: if any seat is unavailable,
     * zero seats are locked.
     *
     * @return true if all seats were successfully locked
     */
    public boolean lockSeats(String showId, String userId, List<String> seatIds) {
        validateShow(showId);
        return lockService.lockSeats(showId, userId, seatIds);
    }

    // ── Confirm Booking ──

    /**
     * Confirms a booking: validates that the user holds all requested seats,
     * creates a Booking entity, processes payment, and marks it CONFIRMED.
     *
     * If payment fails or any validation fails, locked seats are released.
     */
    public Booking confirmBooking(String showId, String userId) {
        Show show = showRepository.getById(showId);
        if (show == null) {
            throw new BookingException("Show not found: " + showId);
        }

        List<String> lockedSeatIds = lockService.getLockedSeats(showId, userId);
        if (lockedSeatIds.isEmpty()) {
            throw new BookingException("No seats locked for user " + userId);
        }

        List<Seat> seats = new ArrayList<>();
        for (String seatId : lockedSeatIds) {
            if (!lockService.isLockedByUser(showId, seatId, userId)) {
                lockService.unlockSeats(showId, userId);
                throw new SeatLockException("Lock expired for seat " + seatId);
            }
            for (Seat s : show.getScreen().getSeats()) {
                if (s.getSeatId().equals(seatId)) {
                    seats.add(s);
                    break;
                }
            }
        }

        double totalAmount = show.getTotalPrice(seats);
        Booking booking = new Booking(userId, show, seats, totalAmount);

        boolean paymentSuccess = paymentService.processPayment(booking, totalAmount);
        Payment payment = new Payment(totalAmount, paymentSuccess);
        if (!paymentSuccess) {
            lockService.unlockSeats(showId, userId);
            throw new PaymentException("Payment failed for booking. Seats released.");
        }

        booking.confirm(payment);
        bookingRepository.save(booking);
        fireBookingConfirmed(booking);
        return booking;
    }

    // ── Cancel ──

    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.getById(bookingId);
        if (booking == null) {
            throw new BookingException("Booking not found: " + bookingId);
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException("Cannot cancel booking in status: " + booking.getStatus());
        }

        booking.cancel();
        lockService.unlockSeats(booking.getShow().getId(), booking.getUserId());
        fireBookingCancelled(booking);
    }

    // ── Expiry ──

    public int releaseExpiredLocks(String showId) {
        return lockService.releaseExpiredLocks(showId);
    }

    // ── Helpers ──

    public Booking getBooking(String bookingId) {
        return bookingRepository.getById(bookingId);
    }

    public List<Booking> getUserBookings(String userId) {
        return bookingRepository.getByUser(userId);
    }

    public ShowRepository getShowRepository() {
        return showRepository;
    }

    public BookingRepository getBookingRepository() {
        return bookingRepository;
    }

    private void validateShow(String showId) {
        Show show = showRepository.getById(showId);
        if (show == null) {
            throw new BookingException("Show not found: " + showId);
        }
    }
}
