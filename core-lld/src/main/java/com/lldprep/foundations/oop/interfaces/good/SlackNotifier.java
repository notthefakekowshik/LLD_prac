package com.lldprep.foundations.oop.interfaces.good;

/**
 * GOOD CODE: New notification channel added WITHOUT modifying existing code!
 * 
 * This demonstrates the Open/Closed Principle:
 * - System is OPEN for extension (we added SlackNotifier)
 * - System is CLOSED for modification (didn't touch OrderService, EmailNotifier, etc.)
 * 
 * In the BAD version, adding Slack would require:
 * 1. Modifying OrderService constructor
 * 2. Adding a new field to OrderService
 * 3. Adding new method calls in placeOrder()
 * 
 * In the GOOD version, we just:
 * 1. Create this new class implementing Notifier
 * 2. Pass it to OrderService (which already accepts List<Notifier>)
 * 
 * Zero modifications to existing code!
 */
public class SlackNotifier implements Notifier {
    
    private String webhookUrl;
    private String channel;
    
    public SlackNotifier(String webhookUrl, String channel) {
        this.webhookUrl = webhookUrl;
        this.channel = channel;
    }
    
    @Override
    public void send(String recipient, String subject, String message) {
        System.out.println("[SLACK] Webhook URL: " + webhookUrl);
        System.out.println("[SLACK] Channel: " + channel);
        System.out.println("[SLACK] Recipient: " + recipient);
        System.out.println("[SLACK] Subject: " + subject);
        System.out.println("[SLACK] Message: " + message);
        System.out.println("[SLACK] Slack message sent successfully!");
    }
    
    @Override
    public String getChannelType() {
        return "SLACK";
    }
    
    public String getChannel() {
        return channel;
    }
}
