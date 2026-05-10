package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;

public class DirectorApprovalHandler extends ApprovalHandler {
    private static final BigDecimal LIMIT = new BigDecimal("25000");
    private static final BigDecimal NEEDS_HIGHER = new BigDecimal("10000");

    public DirectorApprovalHandler() {
        super("Director (Department Head)", LIMIT);
    }

    @Override
    protected boolean needsHigherApproval(BigDecimal amount) {
        // If amount > 10000, needs VP approval even though Director can approve up to 25000
        return amount.compareTo(NEEDS_HIGHER) > 0;
    }

    @Override
    protected boolean processApproval(ApprovalRequest request) {
        // Directors are stricter about documentation
        if (request.getPurpose().length() < 10) {
            return false; // Insufficient detail
        }
        
        // Auto-reject very round numbers above 15000 (might be estimate without research)
        if (request.getAmount().compareTo(new BigDecimal("15000")) > 0 
            && request.getAmount().remainder(new BigDecimal("1000")).equals(BigDecimal.ZERO)) {
            return false; // Round number - need detailed breakdown
        }
        
        return true;
    }

    @Override
    protected String getRejectionReason(ApprovalRequest request) {
        if (request.getPurpose().length() < 10) {
            return "Insufficient purpose description (min 10 chars)";
        }
        if (request.getAmount().compareTo(new BigDecimal("15000")) > 0 
            && request.getAmount().remainder(new BigDecimal("1000")).equals(BigDecimal.ZERO)) {
            return "Round numbers above ₹15K need detailed cost breakdown";
        }
        return "Director discretion - request denied";
    }
}
