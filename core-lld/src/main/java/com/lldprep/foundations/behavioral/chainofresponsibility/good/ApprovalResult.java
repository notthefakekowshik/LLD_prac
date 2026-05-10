package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ApprovalResult {
    private final ApprovalRequest request;
    private final List<String> approvedBy;
    private String rejectedBy;
    private String rejectionReason;
    private Status status;

    public enum Status {
        PENDING,
        FULLY_APPROVED,
        REJECTED
    }

    public ApprovalResult(ApprovalRequest request) {
        this.request = request;
        this.approvedBy = new ArrayList<>();
        this.status = Status.PENDING;
    }

    public void addApproval(String approverName) {
        approvedBy.add(approverName);
    }

    public void markRejected(String approverName, String reason) {
        this.rejectedBy = approverName;
        this.rejectionReason = reason;
        this.status = Status.REJECTED;
    }

    public void markFullyApproved() {
        this.status = Status.FULLY_APPROVED;
    }

    public boolean isRejected() {
        return status == Status.REJECTED;
    }

    public boolean isFullyApproved() {
        return status == Status.FULLY_APPROVED;
    }

    public ApprovalRequest getRequest() {
        return request;
    }

    public List<String> getApprovedBy() {
        return Collections.unmodifiableList(approvedBy);
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Status getStatus() {
        return status;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔═══════════════════════════════════════════════════╗\n");
        sb.append("║           APPROVAL RESULT                         ║\n");
        sb.append("╠═══════════════════════════════════════════════════╣\n");
        sb.append(String.format("║ Request:    %s\n", request));
        sb.append(String.format("║ Status:     %s\n", status));
        
        if (status == Status.REJECTED) {
            sb.append(String.format("║ Rejected By: %s\n", rejectedBy));
            sb.append(String.format("║ Reason:     %s\n", rejectionReason));
        } else if (status == Status.FULLY_APPROVED) {
            sb.append("║ Approved By:\n");
            for (String approver : approvedBy) {
                sb.append(String.format("║   • %s\n", approver));
            }
            sb.append("║\n║ ✓ FULLY APPROVED - Ready to proceed\n");
        } else {
            sb.append("║ Pending approvals...\n");
            if (!approvedBy.isEmpty()) {
                sb.append("║ So far approved by:\n");
                for (String approver : approvedBy) {
                    sb.append(String.format("║   • %s\n", approver));
                }
            }
        }
        
        sb.append("╚═══════════════════════════════════════════════════╝");
        return sb.toString();
    }
}
