package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;

public class ManagerApprovalHandler extends ApprovalHandler {
    private static final BigDecimal LIMIT = new BigDecimal("5000");
    private static final BigDecimal NEEDS_HIGHER = new BigDecimal("2000");

    public ManagerApprovalHandler() {
        super("Manager (Team Lead)", LIMIT);
    }

    @Override
    protected boolean needsHigherApproval(BigDecimal amount) {
        // If amount > 2000, needs Director approval even though Manager can approve up to 5000
        return amount.compareTo(NEEDS_HIGHER) > 0;
    }

    @Override
    protected boolean processApproval(ApprovalRequest request) {
        // Simulate approval logic
        // In real system: check budget, check requester's department, etc.
        
        // Auto-reject suspicious purposes for demo
        if (request.getPurpose().toLowerCase().contains("personal")) {
            return false;
        }
        
        // Auto-reject if amount is exactly 666 (unlucky number demo)
        if (request.getAmount().intValue() == 666) {
            return false;
        }
        
        return true;
    }

    @Override
    protected String getRejectionReason(ApprovalRequest request) {
        if (request.getPurpose().toLowerCase().contains("personal")) {
            return "Personal expenses not allowed";
        }
        if (request.getAmount().intValue() == 666) {
            return "Amount flagged for review";
        }
        return "Manager discretion - request denied";
    }
}
