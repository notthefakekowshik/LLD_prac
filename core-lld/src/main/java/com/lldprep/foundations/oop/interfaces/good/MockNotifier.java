package com.lldprep.foundations.oop.interfaces.good;

import java.util.ArrayList;
import java.util.List;

/**
 * GOOD CODE: Mock implementation for testing.
 * 
 * This demonstrates how interfaces enable TESTABILITY:
 * - In tests, we can use MockNotifier instead of real notifiers
 * - No actual emails/SMS/push notifications are sent
 * - We can verify that send() was called with correct parameters
 * 
 * In the BAD version, testing would require:
 * - Mocking three different concrete classes
 * - Complex test setup
 * - Or actually sending real notifications during tests (bad!)
 */
public class MockNotifier implements Notifier {
    
    private List<String> sentNotifications = new ArrayList<>();
    private int sendCount = 0;
    
    @Override
    public void send(String recipient, String subject, String message) {
        sendCount++;
        String notification = String.format("To: %s, Subject: %s, Message: %s", 
                                           recipient, subject, message);
        sentNotifications.add(notification);
        System.out.println("[MOCK] Notification captured (not actually sent): " + notification);
    }
    
    @Override
    public String getChannelType() {
        return "MOCK";
    }
    
    public int getSendCount() {
        return sendCount;
    }
    
    public List<String> getSentNotifications() {
        return new ArrayList<>(sentNotifications);
    }
    
    public void reset() {
        sendCount = 0;
        sentNotifications.clear();
    }
}
