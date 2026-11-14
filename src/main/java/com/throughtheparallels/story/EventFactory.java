package com.throughtheparallels.story;

public abstract class EventFactory {

    public abstract GameEvent createEvent();

    public void triggerEvent() {
        GameEvent event = createEvent();
        event.execute();
    }
}
