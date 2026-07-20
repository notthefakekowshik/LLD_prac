package com.lldprep.systems.meetingroomscheduler.service;

import com.lldprep.systems.meetingroomscheduler.exception.BookingConflictException;
import com.lldprep.systems.meetingroomscheduler.exception.BookingNotFoundException;
import com.lldprep.systems.meetingroomscheduler.exception.DuplicateEmailException;
import com.lldprep.systems.meetingroomscheduler.exception.InvalidBookingException;
import com.lldprep.systems.meetingroomscheduler.exception.RoomNotFoundException;
import com.lldprep.systems.meetingroomscheduler.exception.UserNotFoundException;
import com.lldprep.systems.meetingroomscheduler.model.Amenity;
import com.lldprep.systems.meetingroomscheduler.model.Booking;
import com.lldprep.systems.meetingroomscheduler.model.Room;
import com.lldprep.systems.meetingroomscheduler.model.TimeSlot;
import com.lldprep.systems.meetingroomscheduler.model.User;
import com.lldprep.systems.meetingroomscheduler.policy.AmenityFilter;
import com.lldprep.systems.meetingroomscheduler.policy.AvailabilityFilter;
import com.lldprep.systems.meetingroomscheduler.policy.CapacityFilter;
import com.lldprep.systems.meetingroomscheduler.policy.FloorFilter;
import com.lldprep.systems.meetingroomscheduler.policy.RoomFilter;
import com.lldprep.systems.meetingroomscheduler.policy.SearchCriteria;
import com.lldprep.systems.meetingroomscheduler.repository.BookingRepository;
import com.lldprep.systems.meetingroomscheduler.repository.RoomRepository;
import com.lldprep.systems.meetingroomscheduler.repository.UserRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class MeetingSchedulerFacade {
    private static final int MAX_ADVANCE_DAYS = 30;

    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final List<RoomFilter> filters;

    public MeetingSchedulerFacade() {
        this.userRepository = new UserRepository();
        this.roomRepository = new RoomRepository();
        this.bookingRepository = new BookingRepository();
        // Order matters only for early-exit efficiency: cheap field checks before availability.
        this.filters = List.of(new CapacityFilter(), new FloorFilter(),
            new AmenityFilter(), new AvailabilityFilter());
    }

    public User registerUser(String name, String email) {
        validateName(name);
        validateEmail(email);
        User user = new User(name, email);
        if (!userRepository.saveIfEmailAbsent(user)) {
            throw new DuplicateEmailException(email);
        }
        return user;
    }

    public Room addRoom(String name, int capacity, int floor, Set<Amenity> amenities) {
        validateRoom(name, capacity, floor);
        Room room = new Room(name, capacity, floor, amenities);
        roomRepository.save(room);
        return room;
    }

    public List<Room> searchRooms(TimeSlot timeSlot, int minCapacity,
                                   Optional<Integer> floor, Set<Amenity> requiredAmenities) {
        if (minCapacity < 1) {
            throw new InvalidBookingException("Minimum capacity must be at least 1, got: " + minCapacity);
        }
        SearchCriteria criteria = new SearchCriteria(timeSlot, minCapacity, floor, requiredAmenities);

        List<Room> rooms = new ArrayList<>(roomRepository.getAll());
        for (RoomFilter filter : filters) {
            rooms = filter.filter(rooms, criteria);
            if (rooms.isEmpty()) {
                break;
            }
        }
        return rooms;
    }

    public Booking bookRoom(String roomId, String userId, TimeSlot timeSlot) {
        Room room = getRoomOrThrow(roomId);
        User user = getUserOrThrow(userId);
        validateBookingDateRange(timeSlot.date());

        if (!room.bookSlot(timeSlot)) {
            throw new BookingConflictException(room.getName());
        }

        Booking booking = new Booking(room, user, timeSlot);
        bookingRepository.save(booking);
        return booking;
    }

    public void cancelBooking(String bookingId) {
        Booking booking = bookingRepository.getById(bookingId);
        if (booking == null) {
            throw new BookingNotFoundException(bookingId);
        }
        booking.getRoom().freeSlot(booking.getTimeSlot());
        bookingRepository.removeById(bookingId);
    }

    public List<Booking> getUpcomingBookings(String userId) {
        getUserOrThrow(userId);
        return bookingRepository.findByUser(userId);
    }

    public String getRoomSchedule(String roomId, LocalDate date) {
        Room room = getRoomOrThrow(roomId);
        return room.getScheduleForDate(date);
    }

    public int totalUsers() {
        return userRepository.count();
    }

    public int totalRooms() {
        return roomRepository.count();
    }

    public int totalBookings() {
        return bookingRepository.count();
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidBookingException("User name cannot be empty");
        }
    }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@")) {
            throw new InvalidBookingException("Invalid email: " + email);
        }
    }

    private void validateRoom(String name, int capacity, int floor) {
        if (name == null || name.isBlank()) {
            throw new InvalidBookingException("Room name cannot be empty");
        }
        if (capacity < 2 || capacity > 50) {
            throw new InvalidBookingException("Room capacity must be between 2 and 50, got: " + capacity);
        }
        if (floor < 0) {
            throw new InvalidBookingException("Floor cannot be negative, got: " + floor);
        }
    }

    private void validateBookingDateRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today)) {
            throw new InvalidBookingException("Cannot book for a past date: " + date);
        }
        if (date.isAfter(today.plusDays(MAX_ADVANCE_DAYS))) {
            throw new InvalidBookingException("Cannot book more than " + MAX_ADVANCE_DAYS + " days in advance: " + date);
        }
    }

    private User getUserOrThrow(String userId) {
        User user = userRepository.getById(userId);
        if (user == null) {
            throw new UserNotFoundException(userId);
        }
        return user;
    }

    private Room getRoomOrThrow(String roomId) {
        Room room = roomRepository.getById(roomId);
        if (room == null) {
            throw new RoomNotFoundException(roomId);
        }
        return room;
    }
}
