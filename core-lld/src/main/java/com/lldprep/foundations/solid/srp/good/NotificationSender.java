// SRP GOOD: Single responsibility — defines the contract for sending notifications only.
// Only changes if the concept of "notification" changes. Fetching and formatting are not its concern.
package com.lldprep.foundations.solid.srp.good;

public interface NotificationSender {
    void send(String recipient, String content);
}
