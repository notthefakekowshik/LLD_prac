package com.lldprep.foundations.structural.bridge.good;

/** Concrete Implementor — delivers via SMS. */
public class SmsSender implements MessageSender {

    @Override
    public void sendMessage(String recipient, String formattedBody) {
        System.out.printf("  [SMS]   To: %s | %s%n", recipient, formattedBody);
    }

    @Override
    public String channelName() {
        return "SMS";
    }
}
