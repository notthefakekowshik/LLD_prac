package com.lldprep.systems.parkinglot.repository;

import com.lldprep.systems.parkinglot.model.Ticket;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryTicketRepository implements TicketRepository {

    private final ConcurrentHashMap<String, Ticket> store = new ConcurrentHashMap<>();
    private final AtomicLong issuedCount = new AtomicLong();

    @Override
    public void save(Ticket ticket) {
        store.put(ticket.getTicketId(), ticket);
        issuedCount.incrementAndGet();
    }

    @Override
    public Optional<Ticket> findById(String ticketId) {
        return Optional.ofNullable(store.get(ticketId));
    }

    @Override
    public Optional<Ticket> removeAndGet(String ticketId) {
        // ConcurrentHashMap.remove() is atomic — only one caller wins; subsequent calls get empty.
        return Optional.ofNullable(store.remove(ticketId));
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public long totalIssued() {
        return issuedCount.get();
    }
}
