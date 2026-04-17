package com.lldprep.foundations.structural.bridge.good;

/** Refined Abstraction — uppercases the body and prepends an URGENT prefix. */
public class UrgentMessage extends Message {

    public UrgentMessage(MessageSender sender) {
        super(sender);
    }

    @Override
    public void send(String recipient, String body) {
        String formatted = "🚨 URGENT: " + body.toUpperCase();
        sender.sendMessage(recipient, formatted);
    }
}
