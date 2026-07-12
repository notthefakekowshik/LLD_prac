package com.lldprep.systems.meetingroomscheduler.model;

import com.lldprep.systems.meetingroomscheduler.exception.InvalidBookingException;

import java.time.LocalDate;

public record TimeSlot(LocalDate date, int startSlot, int endSlot) {

    private static final int WORK_START_HOUR = 9;
    private static final int WORK_END_HOUR = 18;
    private static final int TOTAL_SLOTS = 18;

    public TimeSlot {
        if (date == null) {
            throw new InvalidBookingException("Date cannot be null");
        }
        if (startSlot < 0 || startSlot >= TOTAL_SLOTS) {
            throw new InvalidBookingException("Start slot must be between 0 and " + (TOTAL_SLOTS - 1)
                + ", got: " + startSlot);
        }
        if (endSlot <= startSlot || endSlot > TOTAL_SLOTS) {
            throw new InvalidBookingException("End slot must be > start slot and <= " + TOTAL_SLOTS
                + ", got start=" + startSlot + " end=" + endSlot);
        }
    }

    public static TimeSlot of(LocalDate date, int hour, int minute, int durationMinutes) {
        if (durationMinutes < 30 || durationMinutes % 30 != 0) {
            throw new InvalidBookingException("Duration must be in 30-minute increments, got: " + durationMinutes);
        }
        int startSlot = hourMinuteToSlot(hour, minute);
        int endSlot = startSlot + (durationMinutes / 30);
        if (endSlot > TOTAL_SLOTS) {
            throw new InvalidBookingException("Meeting exceeds working hours: start slot " + startSlot
                + " + " + durationMinutes + "min = end slot " + endSlot + " (max " + TOTAL_SLOTS + ")");
        }
        return new TimeSlot(date, startSlot, endSlot);
    }

    public static int hourMinuteToSlot(int hour, int minute) {
        if (hour < WORK_START_HOUR || hour >= WORK_END_HOUR) {
            throw new InvalidBookingException("Time " + formatTime(hour, minute)
                + " is outside working hours (" + WORK_START_HOUR + ":00 - " + WORK_END_HOUR + ":00)");
        }
        return (hour - WORK_START_HOUR) * 2 + (minute / 30);
    }

    public int slotCount() {
        return endSlot - startSlot;
    }

    public int durationMinutes() {
        return slotCount() * 30;
    }

    public String display() {
        int startHour = WORK_START_HOUR + startSlot / 2;
        int startMin = (startSlot % 2) * 30;
        int endHour = WORK_START_HOUR + endSlot / 2;
        int endMin = (endSlot % 2) * 30;
        return formatTime(startHour, startMin) + " - " + formatTime(endHour, endMin);
    }

    private static String formatTime(int hour, int minute) {
        return String.format("%02d:%02d", hour, minute);
    }
}
