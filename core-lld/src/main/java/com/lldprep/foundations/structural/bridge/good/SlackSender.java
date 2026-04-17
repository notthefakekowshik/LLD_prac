package com.lldprep.foundations.structural.bridge.good;

/** Concrete Implementor — delivers via Slack. Adding a new channel = one new class, nothing else changes. */
public class SlackSender implements MessageSender {

    private final String channel;

    public SlackSender(String channel) {
        this.channel = channel;
    }

    @Override
    public void sendMessage(String recipient, String formattedBody) {
        System.out.printf("  [Slack] #%s @%s | %s%n", channel, recipient, formattedBody);
    }

    @Override
    public String channelName() {
        return "Slack#" + channel;
    }
}
