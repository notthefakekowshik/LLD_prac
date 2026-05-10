package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;

public abstract class ApprovalHandler {
    protected ApprovalHandler nextHandler;
    protected final String handlerName;
    protected final BigDecimal approvalLimit;

    public ApprovalHandler(String handlerName, BigDecimal approvalLimit) {
        this.handlerName = handlerName;
        this.approvalLimit = approvalLimit;
    }

    public void setNext(ApprovalHandler next) {
        this.nextHandler = next;
    }

    public void handle(ApprovalRequest request, ApprovalResult result) {
        if (canHandle(request.getAmount())) {
            // This handler can process the request
            boolean approved = processApproval(request);
            
            if (approved) {
                result.addApproval(handlerName);
                System.out.printf("✓ %s APPROVED request for ₹%,.2f%n", handlerName, request.getAmount());
                
                // Check if we need higher approval or if this is sufficient
                if (needsHigherApproval(request.getAmount())) {
                    System.out.printf("  → Forwarding to next level (amount exceeds single-level limit)%n");
                    forwardToNext(request, result);
                } else {
                    // No higher approval needed, mark as fully approved
                    result.markFullyApproved();
                    System.out.printf("  → All required approvals obtained%n");
                }
            } else {
                // Rejected - stop the chain immediately
                result.markRejected(handlerName, getRejectionReason(request));
                System.out.printf("✗ %s REJECTED request for ₹%,.2f - %s%n", 
                    handlerName, request.getAmount(), getRejectionReason(request));
                // Do NOT forward - rejection stops the chain
            }
        } else {
            // Cannot handle, forward to next
            forwardToNext(request, result);
        }
    }

    private void forwardToNext(ApprovalRequest request, ApprovalResult result) {
        if (nextHandler != null) {
            nextHandler.handle(request, result);
        } else {
            // No more handlers in chain
            if (!result.isRejected()) {
                // If not rejected and no one else to approve, consider it fully approved
                // (this would be the CEO case where no one is above)
                if (result.getApprovedBy().isEmpty()) {
                    // No one could handle this - odd case
                    result.markRejected("System", "No approver available for this amount");
                } else {
                    result.markFullyApproved();
                }
            }
        }
    }

    protected boolean canHandle(BigDecimal amount) {
        return amount.compareTo(approvalLimit) <= 0;
    }

    protected abstract boolean needsHigherApproval(BigDecimal amount);

    protected abstract boolean processApproval(ApprovalRequest request);

    protected abstract String getRejectionReason(ApprovalRequest request);

    public String getHandlerName() {
        return handlerName;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }
}
