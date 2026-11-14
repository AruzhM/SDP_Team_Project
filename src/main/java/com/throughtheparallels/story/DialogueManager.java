package com.throughtheparallels.story;
import java.util.Objects;

public class DialogueManager {

    private InteractionStrategy strategy;

    public DialogueManager(InteractionStrategy strategy) {
        setStrategy(strategy);
    }

    public void setStrategy(InteractionStrategy strategy) {
        this.strategy = Objects.requireNonNull(
                strategy,
                "Interaction strategy cannot be null"
        );
    }

    public String getDialogue() {
        return strategy.getDialogue();
    }

    public String getOutcome() {
        return strategy.getOutcome();
    }
}
