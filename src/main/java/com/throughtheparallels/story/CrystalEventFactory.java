package com.throughtheparallels.story;

public class CrystalEventFactory extends EventFactory {

    @Override
    public GameEvent createEvent() {
        return new CrystalEvent();
    }
}
