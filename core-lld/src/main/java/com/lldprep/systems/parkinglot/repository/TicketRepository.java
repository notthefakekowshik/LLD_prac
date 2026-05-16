package com.lldprep.systems.parkinglot.repository;

import com.lldprep.systems.parkinglot.model.Ticket;

import java.util.Optional;

public interface TicketRepository {
    void save(Ticket ticket);
    Optional<Ticket> findById(String ticketId);
    // Atomically removes and returns the ticket — used by checkOut to prevent double-checkout.
    Optional<Ticket> removeAndGet(String ticketId);
    long count();
    long totalIssued();
}
