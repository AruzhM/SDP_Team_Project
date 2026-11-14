package com.throughtheparallels.progression;

import java.util.Objects;

public final class Quest {
    private final String id;
    private final String description;
    private final String objective;
    private final GameEventPublisher.Type eventType;
    private final int target;
    private int progress;

    public Quest(String id, String description, GameEventPublisher.Type eventType, int target) {
        this(id, description, description, eventType, target);
    }

    public Quest(String id, String description, String objective, GameEventPublisher.Type eventType, int target) {
        this.objective = Objects.requireNonNull(objective);
        if (id == null || id.isBlank() || description == null || description.isBlank() || target < 1) {
            throw new IllegalArgumentException("Quest needs an id, description and positive target");
        }
        this.id = id;
        this.description = description;
        this.eventType = Objects.requireNonNull(eventType);
        this.target = target;
    }

    /** Returns true only on the transition to completed. */
    boolean accept(GameEventPublisher.Event event) {
        if (isCompleted() || event.type() != eventType) return false;
        progress++;
        return isCompleted();
    }

    void restoreProgress(int value) {
        if (value < 0 || value > target) throw new IllegalArgumentException("Invalid quest progress: " + id);
        progress = value;
    }

    public String getId() { return id; }
    public String getDescription() { return description; }
    public String getObjective() { return objective; }
    public int getTarget() { return target; }
    public int getProgress() { return progress; }
    public boolean isCompleted() { return progress == target; }
}
