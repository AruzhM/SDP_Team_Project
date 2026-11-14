package com.throughtheparallels.progression;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/** Observer subject. Used on the game thread; nested events are delivered in FIFO order. */
public final class GameEventPublisher {
    public enum Type {
        CRYSTAL_COLLECTED, DESK_EXAMINED, ABILITY_USED, SEAL_DAMAGED,
        SEAL_BROKEN, DOOR_OPENED, HIKARI_APPEARED, HIKARI_TALKED, ENDING_CHOSEN,
        QUEST_COMPLETED, GAME_SAVED, GAME_LOADED
    }

    public record Event(Type type, String subject) {
        public Event {
            Objects.requireNonNull(type, "Event type");
            Objects.requireNonNull(subject, "Event subject");
        }
    }

    private final Set<GameEventListener> listeners = new LinkedHashSet<>();
    private final Queue<Event> pending = new ArrayDeque<>();
    private boolean dispatching;

    public void subscribe(GameEventListener listener) {
        listeners.add(Objects.requireNonNull(listener));
    }

    public void unsubscribe(GameEventListener listener) {
        listeners.remove(listener);
    }

    public void publish(Type type, String subject) {
        publish(new Event(type, subject));
    }

    public void publish(Event event) {
        pending.add(Objects.requireNonNull(event));
        if (dispatching) return;
        dispatching = true;
        try {
            while (!pending.isEmpty()) {
                Event next = pending.remove();
                for (GameEventListener listener : List.copyOf(listeners)) {
                    listener.onGameEvent(next);
                }
            }
        } finally {
            pending.clear();
            dispatching = false;
        }
    }
}
