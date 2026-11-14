package com.throughtheparallels.progression;

/** Observer: receives notifications without coupling the publisher to a quest or UI. */
@FunctionalInterface
public interface GameEventListener {
    void onGameEvent(GameEventPublisher.Event event);
}
