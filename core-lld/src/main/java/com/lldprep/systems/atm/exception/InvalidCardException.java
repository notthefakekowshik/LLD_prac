package com.lldprep.systems.atm.exception;

public class InvalidCardException extends ATMException {
    private final String cardNumber;

    public InvalidCardException(String cardNumber) {
        super(String.format("Card %s is invalid or expired.", maskCard(cardNumber)));
        this.cardNumber = cardNumber;
    }

    private static String maskCard(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 4) {
            return "****";
        }
        return "****-****-****-" + cardNumber.substring(cardNumber.length() - 4);
    }

    public String getCardNumber() {
        return cardNumber;
    }
}
