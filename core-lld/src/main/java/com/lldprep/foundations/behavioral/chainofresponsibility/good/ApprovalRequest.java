package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;
import java.util.UUID;

public class ApprovalRequest {
    private final String requestId;
    private final String requesterName;
    private final BigDecimal amount;
    private final String purpose;

    public ApprovalRequest(String requesterName, BigDecimal amount, String purpose) {
        this.requestId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.requesterName = requesterName;
        this.amount = amount;
        this.purpose = purpose;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getPurpose() {
        return purpose;
    }

    @Override
    public String toString() {
        return String.format("Request[%s] by %s for ₹%,.2f - %s", 
            requestId, requesterName, amount, purpose);
    }
}
