package com.lldprep.foundations.structural.bridge.good;

/** Concrete Implementor — delivers via email. */
public class EmailSender implements MessageSender {

    @Override
    public void sendMessage(String recipient, String formattedBody) {
        System.out.printf("  [Email] To: %s | %s%n", recipient, formattedBody);
    }

    @Override
    public String channelName() {
        return "Email";
    }
}
