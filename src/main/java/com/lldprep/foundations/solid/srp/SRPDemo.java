package com.lldprep.foundations.solid.srp;

import com.lldprep.foundations.solid.srp.bad.ReportGenerator;
import com.lldprep.foundations.solid.srp.good.*;

public class SRPDemo {

    public static void main(String[] args) {
        System.out.println("===== SRP: SINGLE RESPONSIBILITY PRINCIPLE =====\n");

        // --- BAD VERSION ---
        System.out.println("--- BAD: One class doing everything ---");
        System.out.println("Problem: ReportGenerator has 3 reasons to change:");
        System.out.println("  1. Data source changes (CSV -> DB)");
        System.out.println("  2. Format changes (text -> HTML)");
        System.out.println("  3. Notification channel changes (email -> SMS)");
        System.out.println();

        ReportGenerator badGenerator = new ReportGenerator();
        badGenerator.generateAndSend("employees.csv", "manager@company.com");

        System.out.println();

        // --- GOOD VERSION ---
        System.out.println("--- GOOD: Each class has one reason to change ---");
        System.out.println("Swap CSVDataFetcher -> DBDataFetcher without touching formatter or sender.");
        System.out.println("Swap TextReportFormatter -> HTMLReportFormatter without touching fetcher or sender.");
        System.out.println("Swap EmailNotificationSender -> SMSNotificationSender without touching fetcher or formatter.");
        System.out.println();

        DataFetcher fetcher = new CSVDataFetcher();
        ReportFormatter formatter = new TextReportFormatter();
        NotificationSender sender = new EmailNotificationSender();

        ReportService service = new ReportService(fetcher, formatter, sender);
        service.generateAndSend("employees.csv", "manager@company.com");

        System.out.println("\n===== END SRP DEMO =====");
    }
}
