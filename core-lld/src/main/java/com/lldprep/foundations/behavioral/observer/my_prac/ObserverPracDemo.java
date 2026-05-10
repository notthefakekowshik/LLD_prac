package com.lldprep.foundations.behavioral.observer.my_prac;

import java.util.ArrayList;
import java.util.List;

// ─── Notification channels ───────────────────────────────────────────────────

class EmailService {
    public void sendEmail(String email, String message) {
        System.out.println("[EMAIL → " + email + "] " + message);
    }
}

class SMSService {
    public void sendSMS(String phone, String message) {
        System.out.println("[SMS → " + phone + "] " + message);
    }
}

class PushService {
    public void sendPush(String deviceId, String message) {
        System.out.println("[PUSH → " + deviceId + "] " + message);
    }
}

// ─── NotificationService — knows all channels, picks right one ───────────────

class NotificationService {
    private EmailService emailService = new EmailService();
    private SMSService   smsService   = new SMSService();
    private PushService  pushService  = new PushService();

    public void notify(Bidder bidder, String message) {
        // in real system — bidder's preferences drive which channel fires
        if (bidder.getEmail() != null) {
            emailService.sendEmail(bidder.getEmail(), message);
        }
        if (bidder.getPhone() != null) {
            smsService.sendSMS(bidder.getPhone(), message);
        }
        if (bidder.getDeviceId() != null) {
            pushService.sendPush(bidder.getDeviceId(), message);
        }
    }
}

// ─── Observer contract ────────────────────────────────────────────────────────

interface Observer {
    void update(AuctionItem item, int newBid, Bidder whoPlacedBid);
}

// ─── Bidder — Observer, delegates sending to NotificationService ──────────────

// ─── Bidder — pure data model, no behavior ───────────────────────────────────

class Bidder {
    private String name;
    private String email;
    private String phone;
    private String deviceId;

    public Bidder(String name, String email,
        String phone, String deviceId) {
        this.name     = name;
        this.email    = email;
        this.phone    = phone;
        this.deviceId = deviceId;
    }

    public String getName()     { return name; }
    public String getEmail()    { return email; }
    public String getPhone()    { return phone; }
    public String getDeviceId() { return deviceId; }
}

// ─── BidderObserver — observer concern, holds reference to Bidder ─────────────
class BidderObserver implements Observer {
    private Bidder              bidder;               // has-a, not is-a
    private NotificationService notificationService;

    public BidderObserver(Bidder bidder,
        NotificationService notificationService) {
        this.bidder              = bidder;
        this.notificationService = notificationService;
    }

    @Override
    public void update(AuctionItem item, int newBid, Bidder whoPlacedBid) {
        // Observer decides for itself whether to react
        if (this.bidder == whoPlacedBid) {
            System.out.println("[Skipping " + bidder.getName() + " — they placed this bid]");
            return;
        }

        String message = "New bid on [" + item.getName() + "]: ₹" + newBid
            + " — want to outbid?";
        notificationService.notify(bidder, message);
    }
}

// ─── AuctionItem — Subject ────────────────────────────────────────────────────

// ─── AuctionItem — pure data model ────────────────────────────────────────────

class AuctionItem {
    private final int    id;
    private final String name;
    private int    currentBid = 0;
    private Bidder currentBidder = null;

    public AuctionItem(int id, String name) {
        this.id   = id;
        this.name = name;
    }

    // Getters and setters for data access
    public String getName()      { return name; }
    public int    getCurrentBid()   { return currentBid; }
    public void   setCurrentBid(int bid) { this.currentBid = bid; }
    public Bidder getCurrentBidder() { return currentBidder; }
    public void   setCurrentBidder(Bidder bidder) { this.currentBidder = bidder; }
}

// ─── AuctionService — business logic and Subject behavior ─────────────────────

class AuctionService {
    private final AuctionItem item;
    private final List<Observer> observers = new ArrayList<>();

    public AuctionService(AuctionItem item) {
        this.item = item;
    }

    public void addObserver(Observer o)    { observers.add(o); }
    public void removeObserver(Observer o) { observers.remove(o); }

    // Business logic: validate and process bid
    public boolean placeBid(Bidder bidder, int amount) {
        if (amount <= item.getCurrentBid()) {
            System.out.println("Bid too low. Current: ₹" + item.getCurrentBid());
            return false;
        }

        // Update data model
        item.setCurrentBid(amount);
        item.setCurrentBidder(bidder);

        // Notify observers (Subject behavior)
        notifyObservers();
        return true;
    }

    // Subject behavior: push event to all observers
    private void notifyObservers() {
        for (Observer o : observers) {
            o.update(item, item.getCurrentBid(), item.getCurrentBidder());
        }
    }
}

// ─── Main ─────────────────────────────────────────────────────────────────────

public class ObserverPracDemo {
    public static void main(String[] args) {
        NotificationService notificationService = new NotificationService();

        // pure data
        Bidder alice = new Bidder("Alice", "alice@gmail.com", "+91-9000000001", "device-alice-01");
        Bidder bob   = new Bidder("Bob",   "bob@gmail.com",   null,             null);

        // observer wraps data model
        BidderObserver aliceObserver = new BidderObserver(alice, notificationService);
        BidderObserver bobObserver   = new BidderObserver(bob,   notificationService);

        // Data model (pure data)
        AuctionItem watch = new AuctionItem(1, "Vintage Watch");

        // Service layer handles bidding logic and observer notifications
        AuctionService auctionService = new AuctionService(watch);

        auctionService.addObserver(aliceObserver);
        auctionService.addObserver(bobObserver);

        // Alice places a bid - she should NOT be notified, only Bob should be
        System.out.println("\n=== [1] Alice places bid of ₹1000 ===\n");
        System.out.println("Expected: Alice skipped, Bob gets email\n");
        auctionService.placeBid(alice, 1000);

        // Bob places a higher bid - he should NOT be notified, Alice should be
        System.out.println("\n=== [2] Bob places bid of ₹1200 ===\n");
        System.out.println("Expected: Bob skipped, Alice gets email + SMS + push\n");
        auctionService.placeBid(bob, 1200);

        // Alice places higher bid again
        System.out.println("\n=== [3] Alice places bid of ₹1500 ===\n");
        System.out.println("Expected: Alice skipped, Bob gets email\n");
        auctionService.placeBid(alice, 1500);

        // Bob tries lower bid - should be rejected
        System.out.println("\n=== [4] Bob tries bid of ₹1100 (too low) ===\n");
        System.out.println("Expected: Rejected, no notifications\n");
        auctionService.placeBid(bob, 1100);

        System.out.println("\n=== Auction complete! Final bid: ₹" + watch.getCurrentBid() + " ===");
    }
}