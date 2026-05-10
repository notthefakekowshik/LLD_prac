package com.lldprep.foundations.behavioral.chainofresponsibility.good;

import java.math.BigDecimal;

/**
 * Chain of Responsibility Pattern — Request Approval Workflow Demo
 *
 * <p><b>What problem does it solve?</b><br>
 * Approval workflows where different amounts require different levels of approval.
 * Without CoR, you'd need complex if-else logic to determine who should approve.
 * Adding a new approval level requires editing multiple classes — OCP violation.
 *
 * <p><b>How it works:</b><br>
 * - Each handler knows its approval limit and can decide to approve/reject.<br>
 * - Chain continues until: (a) someone rejects, or (b) all necessary approvals obtained.<br>
 * - If rejected: chain stops immediately, user knows who rejected and why.<br>
 * - If all approve: user gets list of all who approved.
 *
 * <p><b>Approval Rules in this demo:</b>
 * <ul>
 *   <li>≤ ₹2,000: Manager only</li>
 *   <li>₹2,001 - ₹10,000: Manager + Director</li>
 *   <li>₹10,001 - ₹50,000: Manager + Director + VP</li>
 *   <li>₹50,001+: Manager + Director + VP + CEO</li>
 * </ul>
 *
 * <p><b>When to use:</b>
 * <ul>
 *   <li>Multi-level approval workflows (expenses, purchases, leave requests).</li>
 *   <li>Request routing based on amount/urgency/type.</li>
 *   <li>Any workflow where multiple decision makers must approve/reject.</li>
 * </ul>
 *
 * <p><b>Key insight:</b> This is "Stop-on-first-reject" style where each handler
 * must approve for the chain to continue. Contrast with "Pass-and-process" where
 * all handlers process the same request (like the logging example).
 */
public class ChainOfResponsibilityDemo {

    public static void main(String[] args) {
        section("╔════════════════════════════════════════════════════════════╗");
        section("║    CHAIN OF RESPONSIBILITY — REQUEST APPROVAL WORKFLOW     ║");
        section("╚════════════════════════════════════════════════════════════╝");

        ApprovalChain chain = new ApprovalChain();

        demo1_SimpleApproval(chain);
        demo2_MultiLevelApproval(chain);
        demo3_RejectionStopsChain(chain);
        demo4_BorderlineAmounts(chain);
        demo5_EdgeCases(chain);

        section("╔════════════════════════════════════════════════════════════╗");
        section("║                      DEMO COMPLETE                         ║");
        section("╚════════════════════════════════════════════════════════════╝");
    }

    // -------------------------------------------------------------------------
    // DEMO 1: Simple approval - only Manager needed
    // -------------------------------------------------------------------------
    private static void demo1_SimpleApproval(ApprovalChain chain) {
        section("Demo 1: Simple Approval (₹1,500 — Manager only)");

        ApprovalRequest request = new ApprovalRequest(
            "Alice Chen",
            new BigDecimal("1500"),
            "Team lunch for Q2 celebration"
        );

        ApprovalResult result = chain.processRequest(request);
        assert result.isFullyApproved() : "Should be fully approved";
        assert result.getApprovedBy().size() == 1 : "Only Manager should approve";
        System.out.println("✓ PASS: Single-level approval works\n");
    }

    // -------------------------------------------------------------------------
    // DEMO 2: Multi-level approval - Manager + Director
    // -------------------------------------------------------------------------
    private static void demo2_MultiLevelApproval(ApprovalChain chain) {
        section("Demo 2: Multi-Level Approval (₹5,000 — Manager + Director)");

        ApprovalRequest request = new ApprovalRequest(
            "Bob Kumar",
            new BigDecimal("5000"),
            "New development workstation and monitors"
        );

        ApprovalResult result = chain.processRequest(request);
        assert result.isFullyApproved() : "Should be fully approved";
        assert result.getApprovedBy().size() == 2 : "Manager and Director should approve";
        System.out.println("✓ PASS: Multi-level approval works\n");
    }

    // -------------------------------------------------------------------------
    // DEMO 3: Rejection stops chain immediately
    // -------------------------------------------------------------------------
    private static void demo3_RejectionStopsChain(ApprovalChain chain) {
        section("Demo 3: Rejection Stops Chain (₹666 — Manager rejects)");

        // ₹666 is "unlucky number" in our demo - Manager auto-rejects
        ApprovalRequest request = new ApprovalRequest(
            "Charlie Smith",
            new BigDecimal("666"),
            "Office supplies"
        );

        ApprovalResult result = chain.processRequest(request);
        assert result.isRejected() : "Should be rejected";
        assert "Manager (Team Lead)".equals(result.getRejectedBy()) : "Manager should reject";
        assert result.getApprovedBy().isEmpty() : "No approvals before rejection";
        System.out.println("✓ PASS: Rejection stops chain immediately\n");

        // Another rejection: personal expense
        section("Demo 3b: Rejection for Policy Violation (Personal expense)");

        ApprovalRequest request2 = new ApprovalRequest(
            "Diana Prince",
            new BigDecimal("3000"),
            "Personal gym membership"
        );

        ApprovalResult result2 = chain.processRequest(request2);
        assert result2.isRejected() : "Should be rejected for personal expense";
        System.out.println("✓ PASS: Policy violation detected and rejected\n");
    }

    // -------------------------------------------------------------------------
    // DEMO 4: Borderline amounts - test threshold logic
    // -------------------------------------------------------------------------
    private static void demo4_BorderlineAmounts(ApprovalChain chain) {
        section("Demo 4: Borderline Amounts");

        // Exactly ₹2,000 - Manager only (at threshold)
        System.out.println("--- ₹2,000 (at Manager threshold) ---");
        ApprovalResult r1 = chain.processRequest(new ApprovalRequest(
            "Eve Adams", new BigDecimal("2000"), "Training materials"
        ));
        System.out.println("Approved by: " + r1.getApprovedBy());
        assert r1.getApprovedBy().size() == 1 : "Exactly at threshold = Manager only";

        // ₹2,001 - triggers Director approval (just over threshold)
        System.out.println("\n--- ₹2,001 (just over Manager single-level limit) ---");
        ApprovalResult r2 = chain.processRequest(new ApprovalRequest(
            "Frank Lee", new BigDecimal("2001"), "Software license renewal"
        ));
        System.out.println("Approved by: " + r2.getApprovedBy());
        assert r2.getApprovedBy().size() == 2 : "Over threshold = Manager + Director";

        // ₹10,000 - at Director threshold, needs VP
        System.out.println("\n--- ₹10,001 (triggers VP approval) ---");
        ApprovalResult r3 = chain.processRequest(new ApprovalRequest(
            "Grace Ho", new BigDecimal("10001"), "Q3 client event sponsorship"
        ));
        System.out.println("Approved by: " + r3.getApprovedBy());
        assert r3.getApprovedBy().size() == 3 : "Needs Manager + Director + VP";

        System.out.println("\n✓ PASS: Borderline amounts handled correctly\n");
    }

    // -------------------------------------------------------------------------
    // DEMO 5: Edge cases - high amounts, rejections at different levels
    // -------------------------------------------------------------------------
    private static void demo5_EdgeCases(ApprovalChain chain) {
        section("Demo 5: Edge Cases");

        // High amount requiring CEO approval
        System.out.println("--- ₹75,000 (requires CEO approval) ---");
        ApprovalResult r1 = chain.processRequest(new ApprovalRequest(
            "Henry Ford", new BigDecimal("75000"), "Critical infrastructure upgrade for revenue system"
        ));
        System.out.println("Approved by: " + r1.getApprovedBy());
        assert r1.isFullyApproved() : "Should get all approvals including CEO";
        assert r1.getApprovedBy().size() == 4 : "Manager + Director + VP + CEO";

        // Director rejection - insufficient purpose description
        System.out.println("\n--- ₹8,000 (Director rejects - short description) ---");
        ApprovalResult r2 = chain.processRequest(new ApprovalRequest(
            "Ivy Wong", new BigDecimal("8000"), "Stuff"  // Too short!
        ));
        assert r2.isRejected() : "Should be rejected for short description";
        assert "Director (Department Head)".equals(r2.getRejectedBy()) : "Director should reject";
        assert r2.getApprovedBy().size() == 1 : "Only Manager approved before rejection";

        // VP rejection - no business justification
        System.out.println("\n--- ₹30,000 (VP rejects - no business justification) ---");
        ApprovalResult r3 = chain.processRequest(new ApprovalRequest(
            "Jack Ma", new BigDecimal("30000"), "Team morale activities and fun events"  // No biz keywords
        ));
        assert r3.isRejected() : "Should be rejected for lack of business justification";
        assert "VP (Vice President)".equals(r3.getRejectedBy()) : "VP should reject";

        System.out.println("\n✓ PASS: Edge cases handled correctly\n");
    }

    private static void section(String title) {
        System.out.println(title);
    }
}

