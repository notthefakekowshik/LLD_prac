package com.lldprep.foundations.structural.bridge.good;

/** Refined Abstraction — sends the body as-is with no special formatting. */
public class SimpleMessage extends Message {

    public SimpleMessage(MessageSender sender) {
        super(sender);
    }

    @Override
    public void send(String recipient, String body) {
        sender.sendMessage(recipient, body);
    }
}
