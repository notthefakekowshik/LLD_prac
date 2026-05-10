package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;

public class CEOApprovalHandler extends ApprovalHandler {
    private static final BigDecimal LIMIT = new BigDecimal("1000000"); // 1 Crore

    public CEOApprovalHandler() {
        super("CEO (Chief Executive Officer)", LIMIT);
    }

    @Override
    protected boolean needsHigherApproval(BigDecimal amount) {
        // CEO is the final authority - no one above
        return false;
    }

    @Override
    protected boolean processApproval(ApprovalRequest request) {
        // CEO auto-approves anything within limit (CEO trusts the chain below)
        // But has some absolute veto powers
        
        String purpose = request.getPurpose().toLowerCase();
        
        // CEO vetoes anything suspicious
        if (purpose.contains("kickback") || purpose.contains("bribe") || purpose.contains("personal gift")) {
            return false;
        }
        
        // Auto-approve everything else
        return true;
    }

    @Override
    protected String getRejectionReason(ApprovalRequest request) {
        return "CEO veto - request violates company ethics policy";
    }
}
