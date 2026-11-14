package com.throughtheparallels.story;

public class DialogueManager {

    private InteractionStrategy strategy;

    public DialogueManager(InteractionStrategy strategy) {
        this.strategy = strategy;
    }

    public void setStrategy(InteractionStrategy strategy) {
        this.strategy = strategy;
    }

    public String getDialogue() {
        return strategy.getDialogue();
    }

    public String getOutcome() {
        return strategy.getOutcome();
    }
}
