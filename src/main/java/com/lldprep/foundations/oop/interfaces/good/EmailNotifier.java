package com.lldprep.foundations.oop.interfaces.good;

/**
 * GOOD CODE: Concrete implementation of Notifier interface.
 * 
 * This class:
 * - Implements the Notifier contract
 * - Can be used anywhere a Notifier is expected
 * - Can be swapped with any other Notifier implementation
 */
public class EmailNotifier implements Notifier {
    
    private String smtpServer;
    private int port;
    
    public EmailNotifier(String smtpServer, int port) {
        this.smtpServer = smtpServer;
        this.port = port;
    }
    
    @Override
    public void send(String recipient, String subject, String message) {
        System.out.println("[EMAIL] Connecting to SMTP server: " + smtpServer + ":" + port);
        System.out.println("[EMAIL] To: " + recipient);
        System.out.println("[EMAIL] Subject: " + subject);
        System.out.println("[EMAIL] Body: " + message);
        System.out.println("[EMAIL] Email sent successfully!");
    }
    
    @Override
    public String getChannelType() {
        return "EMAIL";
    }
    
    public String getSmtpServer() {
        return smtpServer;
    }
}
