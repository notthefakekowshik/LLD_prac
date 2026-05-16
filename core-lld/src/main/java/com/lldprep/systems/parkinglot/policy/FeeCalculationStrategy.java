package com.lldprep.systems.parkinglot.policy;

import com.lldprep.systems.parkinglot.model.Ticket;

import java.time.LocalDateTime;

public interface FeeCalculationStrategy {
    double calculateFee(Ticket ticket, LocalDateTime exitTime);
}
