package com.lldprep.foundations.behavioral.chainofresponsibility.good.notification;

public class NotificationRequest {
    private final String userId;
    private final String message;
    private final String priority; // HIGH, NORMAL, LOW

    public NotificationRequest(String userId, String message, String priority) {
        this.userId = userId;
        this.message = message;
        this.priority = priority;
    }

    public String getUserId() { return userId; }
    public String getMessage() { return message; }
    public String getPriority() { return priority; }
}
