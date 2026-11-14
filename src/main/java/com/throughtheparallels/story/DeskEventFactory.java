package com.throughtheparallels.story;

public class DeskEventFactory extends EventFactory {

    @Override
    public GameEvent createEvent() {
        return new DeskEvent();
    }
}
