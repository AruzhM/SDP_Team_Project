package com.throughtheparallels.story;
import java.util.List;

public class TrustDialogue {

    public record DialogueLine(String speaker, String text) {}

    private final List<DialogueLine> lines = List.of(
            new DialogueLine(
                    "Hikari",
                    "I'm glad you trust me. There's something you need to know about that crystal."
            ),
            new DialogueLine(
                    "Player",
                    "What is it? Why did everything change when I touched it?"
            ),
            new DialogueLine(
                    "Hikari",
                    "It's not an ordinary crystal. It connects places that were never meant to meet."
            ),
            new DialogueLine(
                    "Player",
                    "Then why did it appear in my room?"
            ),
            new DialogueLine(
                    "Hikari",
                    "I don't know everything yet. But I think it chose you."
            ),
            new DialogueLine(
                    "Hikari",
                    "Come with me. There's something I need to show you."
            )
    );

    private int currentIndex = 0;

    public DialogueLine getCurrentLine() {
        return lines.get(currentIndex);
    }

    public boolean hasNext() {
        return currentIndex < lines.size() - 1;
    }

    public void next() {
        if (hasNext()) {
            currentIndex++;
        }
    }

    public void reset() {
        currentIndex = 0;
    }
}
