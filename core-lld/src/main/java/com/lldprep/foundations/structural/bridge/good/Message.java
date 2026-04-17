package com.lldprep.foundations.structural.bridge.good;

/**
 * Abstraction — the "message type" side of the bridge.
 * Holds a reference to a {@link MessageSender} (the implementor) rather than inheriting it.
 * Subclasses control formatting; the sender controls delivery.
 */
public abstract class Message {

    protected final MessageSender sender;

    protected Message(MessageSender sender) {
        this.sender = sender;
    }

    /** Format the raw body according to message type rules, then delegate delivery to sender. */
    public abstract void send(String recipient, String body);
}
