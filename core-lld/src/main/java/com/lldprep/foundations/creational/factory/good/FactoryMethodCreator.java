package com.lldprep.foundations.creational.factory.good;

/**
 * Factory Method Pattern - Creator hierarchy.
 * 
 * USE CASE: When different creators need to produce different products,
 * or when creator itself has logic that works with the product.
 * 
 * EXAMPLE: Cross-platform UI - WindowsDialog creates WindowsButton,
 * MacDialog creates MacButton.
 */
public abstract class FactoryMethodCreator {
    
    // Business logic that uses the product
    public void notifyUser(String message) {
        Notification notification = createNotification();
        notification.send(message);
    }
    
    // Factory Method - subclasses decide which Notification to create
    protected abstract Notification createNotification();
}

// Concrete Creators
class EmailNotificationCreator extends FactoryMethodCreator {
    @Override
    protected Notification createNotification() {
        return new EmailNotification("smtp.company.com", 587);
    }
}

class SMSNotificationCreator extends FactoryMethodCreator {
    @Override
    protected Notification createNotification() {
        return new SMSNotification("AWS SNS", "aws-api-key");
    }
}

class PushNotificationCreator extends FactoryMethodCreator {
    @Override
    protected Notification createNotification() {
        return new PushNotification("user-fcm-token");
    }
}
