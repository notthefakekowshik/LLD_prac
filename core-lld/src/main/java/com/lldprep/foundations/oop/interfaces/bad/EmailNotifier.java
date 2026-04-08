package com.lldprep.foundations.oop.interfaces.bad;

/**
 * BAD CODE: Concrete class with no interface abstraction.
 * 
 * This class directly implements email notification logic.
 * Any code that uses this is TIGHTLY COUPLED to email-specific implementation.
 */
public class EmailNotifier {
    
    private String smtpServer;
    private int port;
    
    public EmailNotifier(String smtpServer, int port) {
        this.smtpServer = smtpServer;
        this.port = port;
    }
    
    public void sendEmail(String recipient, String subject, String body) {
        System.out.println("[EMAIL] Connecting to SMTP server: " + smtpServer + ":" + port);
        System.out.println("[EMAIL] To: " + recipient);
        System.out.println("[EMAIL] Subject: " + subject);
        System.out.println("[EMAIL] Body: " + body);
        System.out.println("[EMAIL] Email sent successfully!");
    }
    
    public String getSmtpServer() {
        return smtpServer;
    }
}
