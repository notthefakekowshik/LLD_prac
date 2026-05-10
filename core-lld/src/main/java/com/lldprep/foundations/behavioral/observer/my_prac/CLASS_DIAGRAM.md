# Observer Pattern Class Diagram

## Mermaid.js Code

```mermaid
classDiagram
    %% Observer Pattern Core
    class Observer {
        <<interface>>
        +update(AuctionItem item, int newBid, Bidder whoPlacedBid)
    }

    class BidderObserver {
        -Bidder bidder
        -NotificationService notificationService
        +update(AuctionItem, int, Bidder)
    }

    %% Data Models
    class Bidder {
        -String name
        -String email
        -String phone
        -String deviceId
        +getName() String
        +getEmail() String
        +getPhone() String
        +getDeviceId() String
    }

    class AuctionItem {
        -int id
        -String name
        -int currentBid
        -Bidder currentBidder
        +getName() String
        +getCurrentBid() int
        +setCurrentBid(int)
        +getCurrentBidder() Bidder
        +setCurrentBidder(Bidder)
    }

    %% Service Layer (Subject)
    class AuctionService {
        -AuctionItem item
        -List~Observer~ observers
        +addObserver(Observer)
        +removeObserver(Observer)
        +placeBid(Bidder, int) boolean
        -notifyObservers()
    }

    %% Notification System
    class NotificationService {
        -EmailService emailService
        -SMSService smsService
        -PushService pushService
        +notify(Bidder, String)
    }

    class EmailService {
        +sendEmail(String email, String message)
    }

    class SMSService {
        +sendSMS(String phone, String message)
    }

    class PushService {
        +sendPush(String deviceId, String message)
    }

    %% Relationships
    Observer <|.. BidderObserver : implements
    BidderObserver --> Bidder : has-a
    BidderObserver --> NotificationService : uses

    AuctionService --> AuctionItem : manages
    AuctionService --> Observer : notifies

    NotificationService *-- EmailService : composition (creates via new)
    NotificationService *-- SMSService : composition (creates via new)
    NotificationService *-- PushService : composition (creates via new)
    NotificationService --> Bidder : reads

    AuctionItem --> Bidder : tracks current bidder
```

## Diagram Explanation

### Core Observer Pattern
- **Observer** (interface) — Contract for all observers
- **BidderObserver** — Concrete observer, decides whether to react
- **AuctionService** — Subject, manages observers and notifies them

### Data Models (Pure Data)
- **Bidder** — User contact info, no behavior
- **AuctionItem** — Auction state, getters/setters only

### Notification System (Strategy-like)
- **NotificationService** — Orchestrates multi-channel notifications
- **EmailService / SMSService / PushService** — Channel implementations

### Key Design Decisions
1. **Separation of Concerns**: Data models (Bidder, AuctionItem) have no logic
2. **Observer owns decision**: BidderObserver decides to skip itself
3. **Subject in Service layer**: AuctionService handles both business logic and notifications
4. **Composition over injection**: NotificationService creates Email/SMS/Push via `new` (strong ownership)
5. **Multi-channel notifications**: NotificationService picks channels based on Bidder preferences
