package com.throughtheparallels.story;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DialogueManagerTest {

    @Test
    void testTrustStrategy() {
        DialogueManager manager =
                new DialogueManager(new TrustStrategy());

        assertEquals(
                "I'm glad you trust me. Follow me.",
                manager.getDialogue()
        );

        assertEquals(
                "Hikari leads the player into the corridor.",
                manager.getOutcome()
        );
    }

    @Test
    void testDoubtStrategy() {
        DialogueManager manager =
                new DialogueManager(new DoubtStrategy());

        assertEquals(
                "I understand your doubts. I'll wait here.",
                manager.getDialogue()
        );

        assertEquals(
                "The player stays in the room to investigate alone.",
                manager.getOutcome()
        );
    }

    @Test
    void testStrategySwitching() {
        DialogueManager manager =
                new DialogueManager(new TrustStrategy());

        assertEquals(
                "Hikari leads the player into the corridor.",
                manager.getOutcome()
        );

        manager.setStrategy(new DoubtStrategy());

        assertEquals(
                "The player stays in the room to investigate alone.",
                manager.getOutcome()
        );
    }

    @Test
    void testNullStrategy() {
        DialogueManager manager =
                new DialogueManager(new TrustStrategy());

        assertThrows(
                NullPointerException.class,
                () -> manager.setStrategy(null)
        );
    }

}
