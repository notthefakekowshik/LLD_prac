package com.lldprep.foundations.behavioral.chainofresponsibility.good;

public class ApprovalChain {
    private final ApprovalHandler head;

    public ApprovalChain() {
        // Build the chain: Manager -> Director -> VP -> CEO
        ApprovalHandler manager = new ManagerApprovalHandler();
        ApprovalHandler director = new DirectorApprovalHandler();
        ApprovalHandler vp = new VPApprovalHandler();
        ApprovalHandler ceo = new CEOApprovalHandler();

        manager.setNext(director);
        director.setNext(vp);
        vp.setNext(ceo);

        this.head = manager;
    }

    public ApprovalResult processRequest(ApprovalRequest request) {
        System.out.println("\n" + "═".repeat(60));
        System.out.println("PROCESSING: " + request);
        System.out.println("═".repeat(60));

        ApprovalResult result = new ApprovalResult(request);
        head.handle(request, result);

        System.out.println("\n" + result);
        
        return result;
    }

    // Convenience method for quick approval check
    public boolean isApproved(ApprovalRequest request) {
        ApprovalResult result = processRequest(request);
        return result.isFullyApproved();
    }
}
