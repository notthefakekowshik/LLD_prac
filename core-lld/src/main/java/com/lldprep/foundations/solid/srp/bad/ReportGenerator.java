// SRP VIOLATION: This class has 3 reasons to change
// 1. If data fetching logic changes (e.g., switch from CSV to DB)
// 2. If report formatting changes (e.g., switch from text to HTML)
// 3. If notification mechanism changes (e.g., switch from email to SMS)
package com.lldprep.foundations.solid.srp.bad;

import java.util.List;

public class ReportGenerator {

    // Responsibility 1: Data Fetching
    public List<String> fetchData(String source) {
        System.out.println("[BAD-SRP] Fetching data from: " + source);
        // Hardcoded data simulating CSV rows
        return List.of("Alice,30,Engineering", "Bob,25,Marketing", "Charlie,35,Finance");
    }

    // Responsibility 2: Report Formatting
    public String formatReport(List<String> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== REPORT ===\n");
        for (String row : data) {
            sb.append(row).append("\n");
        }
        sb.append("==============");
        return sb.toString();
    }

    // Responsibility 3: Sending Notification (simulating email via System.out)
    public void sendEmail(String recipient, String content) {
        System.out.println("[BAD-SRP] Sending email to " + recipient + ":\n" + content);
    }

    // Orchestration — tightly couples all three responsibilities
    public void generateAndSend(String source, String recipient) {
        List<String> data = fetchData(source);
        String report = formatReport(data);
        sendEmail(recipient, report);
    }
}
