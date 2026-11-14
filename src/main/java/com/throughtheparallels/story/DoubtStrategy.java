package com.throughtheparallels.story;

public class DoubtStrategy implements InteractionStrategy {

    @Override
    public String getDialogue() {
        return "Hikari: I understand your doubts. I'll wait here.";
    }

    @Override
    public String getOutcome() {
        return "The player stays in the room to investigate alone.";
    }
}
