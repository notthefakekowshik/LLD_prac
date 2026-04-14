package com.lldprep.foundations.creational.factory.good;

/**
 * Concrete Product - Email notification implementation.
 */
public class EmailNotification implements Notification {
    private final String smtpServer;
    private final int port;
    
    public EmailNotification(String smtpServer, int port) {
        this.smtpServer = smtpServer;
        this.port = port;
    }
    
    @Override
    public void send(String message) {
        System.out.println("[EMAIL via " + smtpServer + ":" + port + "] " + message);
    }
    
    @Override
    public String getChannel() {
        return "EMAIL";
    }
}
