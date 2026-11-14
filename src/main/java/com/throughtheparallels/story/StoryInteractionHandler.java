package com.throughtheparallels.story;

public class StoryInteractionHandler {

    private final EventFactory deskFactory =
            new DeskEventFactory();

    private final EventFactory crystalFactory =
            new CrystalEventFactory();

    public void interactWithDesk() {
        deskFactory.triggerEvent();
    }

    public void interactWithCrystal() {
        crystalFactory.triggerEvent();
    }
}
