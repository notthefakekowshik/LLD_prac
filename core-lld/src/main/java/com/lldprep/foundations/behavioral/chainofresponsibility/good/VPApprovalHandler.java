package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;

public class VPApprovalHandler extends ApprovalHandler {
    private static final BigDecimal LIMIT = new BigDecimal("100000");
    private static final BigDecimal NEEDS_HIGHER = new BigDecimal("50000");

    public VPApprovalHandler() {
        super("VP (Vice President)", LIMIT);
    }

    @Override
    protected boolean needsHigherApproval(BigDecimal amount) {
        // If amount > 50000, needs CEO approval even though VP can approve up to 100000
        return amount.compareTo(NEEDS_HIGHER) > 0;
    }

    @Override
    protected boolean processApproval(ApprovalRequest request) {
        // VPs require business justification for high amounts
        String purpose = request.getPurpose().toLowerCase();
        
        // Must contain business keywords
        boolean hasBusinessJustification = 
            purpose.contains("revenue") || 
            purpose.contains("client") || 
            purpose.contains("critical") ||
            purpose.contains("contract") ||
            purpose.contains("equipment") ||
            purpose.contains("software") ||
            purpose.contains("infrastructure");
        
        if (!hasBusinessJustification && request.getAmount().compareTo(new BigDecimal("20000")) > 0) {
            return false;
        }
        
        // Check if requester is known (not empty/null)
        if (request.getRequesterName() == null || request.getRequesterName().trim().isEmpty()) {
            return false;
        }
        
        return true;
    }

    @Override
    protected String getRejectionReason(ApprovalRequest request) {
        String purpose = request.getPurpose().toLowerCase();
        boolean hasBusinessJustification = 
            purpose.contains("revenue") || 
            purpose.contains("client") || 
            purpose.contains("critical") ||
            purpose.contains("contract") ||
            purpose.contains("equipment") ||
            purpose.contains("software") ||
            purpose.contains("infrastructure");
        
        if (!hasBusinessJustification && request.getAmount().compareTo(new BigDecimal("20000")) > 0) {
            return "Business justification required for amounts above ₹20K (include: revenue/client/critical/contract/equipment/software/infrastructure)";
        }
        
        if (request.getRequesterName() == null || request.getRequesterName().trim().isEmpty()) {
            return "Requester identification required";
        }
        
        return "VP discretion - request denied";
    }
}
