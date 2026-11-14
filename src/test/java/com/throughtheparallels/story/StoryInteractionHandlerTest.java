package com.throughtheparallels.story;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import static org.junit.jupiter.api.Assertions.*;

public class StoryInteractionHandlerTest {

    @Test
    void testDeskInteraction() {
        StoryInteractionHandler handler =
                new StoryInteractionHandler();

        PrintStream originalOut = System.out;
        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));

            handler.interactWithDesk();

            assertTrue(output.toString().contains(
                    "I don't remember leaving this here..."
            ));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testCrystalInteraction() {
        StoryInteractionHandler handler =
                new StoryInteractionHandler();

        PrintStream originalOut = System.out;
        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        try {
            System.setOut(new PrintStream(output));

            handler.interactWithCrystal();

            assertTrue(output.toString().contains(
                    "Why is it... warm?"
            ));
        } finally {
            System.setOut(originalOut);
        }
    }
}
