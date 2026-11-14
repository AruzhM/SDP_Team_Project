package com.throughtheparallels.story;

public class TrustStrategy implements InteractionStrategy {

    @Override
    public String getDialogue() {
        return "I'm glad you trust me. Follow me.";
    }

    @Override
    public String getOutcome() {
        return "Hikari leads the player into the corridor.";
    }
}
