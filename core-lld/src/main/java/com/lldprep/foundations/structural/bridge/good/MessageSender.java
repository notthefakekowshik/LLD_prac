package com.lldprep.foundations.structural.bridge.good;

/**
 * Implementor — the "channel" side of the bridge.
 * Message types delegate actual delivery to this interface.
 */
public interface MessageSender {
    void sendMessage(String recipient, String formattedBody);
    String channelName();
}
