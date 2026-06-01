package com.lldprep.systems.moviebooking.service;

import com.lldprep.systems.moviebooking.model.Booking;

public class PaymentService {

    /**
     * Mocks payment processing. Returns true with 90% success rate.
     * In production, this would integrate with a real payment gateway.
     */
    public boolean processPayment(Booking booking, double amount) {
        if (amount <= 0) {
            return false;
        }
        return true;
    }
}
