package com.lldprep.systems.atm.service;

import com.lldprep.systems.atm.model.Card;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CardManager {
    private final Map<String, Card> cards = new ConcurrentHashMap<>();

    public CardManager() {
        initializeSampleCards();
    }

    private void initializeSampleCards() {
        // Card 1: Valid card with PIN "1234", expires in 2027
        Card card1 = new Card("1234-5678-9012-3456", "1234", LocalDate.of(2027, 12, 31));
        card1.linkAccount("ACC001"); // Checking
        card1.linkAccount("ACC002"); // Savings
        cards.put(card1.getCardNumber(), card1);

        // Card 2: Valid card with PIN "4321", expires in 2026
        Card card2 = new Card("9876-5432-1098-7654", "4321", LocalDate.of(2026, 6, 30));
        card2.linkAccount("ACC003");
        cards.put(card2.getCardNumber(), card2);

        // Card 3: Expired card
        Card card3 = new Card("1111-2222-3333-4444", "1111", LocalDate.of(2023, 1, 1));
        card3.linkAccount("ACC004");
        cards.put(card3.getCardNumber(), card3);

        // Card 4: Blocked card
        Card card4 = new Card("5555-6666-7777-8888", "5555", LocalDate.of(2028, 12, 31));
        card4.linkAccount("ACC005");
        card4.block();
        cards.put(card4.getCardNumber(), card4);
    }

    public Card getCard(String cardNumber) {
        return cards.get(cardNumber);
    }

    public boolean validateCard(String cardNumber) {
        Card card = cards.get(cardNumber);
        if (card == null) {
            return false;
        }
        return !card.isExpired();
    }

    public boolean isBlocked(String cardNumber) {
        Card card = cards.get(cardNumber);
        return card != null && card.isBlocked();
    }

    public boolean verifyPIN(Card card, String pin) {
        return card.validatePIN(pin);
    }

    public void blockCard(String cardNumber) {
        Card card = cards.get(cardNumber);
        if (card != null) {
            card.block();
        }
    }

    public void unblockCard(String cardNumber) {
        Card card = cards.get(cardNumber);
        if (card != null) {
            card.unblock();
        }
    }
}
