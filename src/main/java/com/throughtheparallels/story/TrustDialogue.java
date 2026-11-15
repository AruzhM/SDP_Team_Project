package com.throughtheparallels.story;
import java.util.List;

public class TrustDialogue {

    public record DialogueLine(String speaker, String text) {}

    private final List<DialogueLine> lines = List.of(
            new DialogueLine(
                    "Hikari",
                    "I'm glad you trust me. I'm Hikari, by the way! There's something you need to know about that crystal."
            ),
            new DialogueLine(
                    "You",
                    "What is it? Why did everything change when I touched it?"
            ),
            new DialogueLine(
                    "Hikari",
                    "It's not an ordinary crystal. It connects places that were never meant to meet."
            ),
            new DialogueLine(
                    "You",
                    "Then why did it appear in my room?"
            ),
            new DialogueLine(
                    "Hikari",
                    "I don't know everything yet. But I think it chose you. Come with me, {playerName}"
            ),
            new DialogueLine(
                    "Hikari",
                    "You brought the crystal with you, didn't you?"
            ),
            new DialogueLine(
                    "You",
                    "Yeah. But I still don't know what it does."
            ),
            new DialogueLine(
                    "Hikari",
                    "It doesn't just open paths between worlds. It can protect you, too."
            ),
            new DialogueLine(
                    "Hikari",
                    "See that training dummy? Try focusing your energy on it."
            ),
            new DialogueLine(
                    "Hikari",
                    "Press Q to release a crystal pulse. Make sure you're close enough!"
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
