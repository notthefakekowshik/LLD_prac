// SRP GOOD: Single responsibility — sends notifications via email only.
// Only changes if email delivery logic changes. Fetching and formatting are not its concern.
package com.lldprep.foundations.solid.srp.good;

public class EmailNotificationSender implements NotificationSender {

    @Override
    public void send(String recipient, String content) {
        System.out.println("Sending email to " + recipient + ": " + content);
    }
}
