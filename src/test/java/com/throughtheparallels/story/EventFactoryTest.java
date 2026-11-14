package com.throughtheparallels.story;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EventFactoryTest {

    @Test
    void testDeskEventFactory() {
        EventFactory factory = new DeskEventFactory();

        GameEvent event = factory.createEvent();

        assertInstanceOf(DeskEvent.class, event);
        assertEquals("Examine the desk",
                event.getDescription());
    }

    @Test
    void testCrystalEventFactory() {
        EventFactory factory = new CrystalEventFactory();

        GameEvent event = factory.createEvent();

        assertInstanceOf(CrystalEvent.class, event);
        assertEquals("Pick up the mysterious crystal",
                event.getDescription());
    }
}
