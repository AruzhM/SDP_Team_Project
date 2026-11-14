package com.throughtheparallels.progression;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Concrete observer: gameplay events advance quests and emit completion notifications. */
public final class QuestManager implements GameEventListener, AutoCloseable {
    public static final String FIND_CRYSTAL = "find_crystal";
    public static final String READ_DESK = "read_desk";
    public static final String TALK_HIKARI = "talk_hikari";
    public static final String CROSS_BOUNDARY = "cross_boundary";

    private final GameEventPublisher publisher;
    private final Map<String, Quest> quests = new LinkedHashMap<>();

    public QuestManager(GameEventPublisher publisher) {
        this.publisher = publisher;
        addQuest(new Quest(READ_DESK, "Quest 1 — A Strange Morning", "Examine the writing desk.", GameEventPublisher.Type.DESK_EXAMINED, 1));
        addQuest(new Quest(FIND_CRYSTAL, "Quest 2 — The Crystal", "Pick up the crystal.", GameEventPublisher.Type.CRYSTAL_COLLECTED, 1));
        addQuest(new Quest(TALK_HIKARI, "Quest 3 — The Other Me", "Talk to Hikari.", GameEventPublisher.Type.HIKARI_TALKED, 1));
        addQuest(new Quest(CROSS_BOUNDARY, "Quest 4 — Cross the Boundary", "Decide what to do next.", GameEventPublisher.Type.ENDING_CHOSEN, 1));
        publisher.subscribe(this);
    }

    public void addQuest(Quest quest) {
        if (quests.putIfAbsent(quest.getId(), quest) != null) {
            throw new IllegalArgumentException("Duplicate quest: " + quest.getId());
        }
    }

    @Override
    public void onGameEvent(GameEventPublisher.Event event) {
        // Only the first unfinished quest is active: later story beats cannot be skipped.
        for (Quest quest : List.copyOf(quests.values())) {
            if (!quest.isCompleted()) {
                if (quest.accept(event)) publisher.publish(GameEventPublisher.Type.QUEST_COMPLETED, quest.getId());
                break;
            }
        }
    }


    public List<Quest> getQuests() { return List.copyOf(quests.values()); }

    public boolean isCompleted(String id) {
        Quest quest = quests.get(id);
        return quest != null && quest.isCompleted();
    }

    public Map<String, Integer> snapshot() {
        Map<String, Integer> result = new LinkedHashMap<>();
        quests.forEach((id, quest) -> result.put(id, quest.getProgress()));
        return Map.copyOf(result);
    }

    /** Validate everything before changing live progress; loading never re-emits rewards. */
    public void restore(Map<String, Integer> progress) {
        if (!progress.keySet().equals(quests.keySet())) throw new IllegalArgumentException("Unknown or missing quests");
        quests.forEach((id, quest) -> {
            Integer value = progress.get(id);
            if (value == null || value < 0 || value > quest.getTarget()) {
                throw new IllegalArgumentException("Invalid progress for " + id);
            }
        });
        boolean incomplete = false;
        for (Quest quest : quests.values()) {
            int value = progress.get(quest.getId());
            if (incomplete && value != 0) throw new IllegalArgumentException("Quest order is invalid");
            if (value < quest.getTarget()) incomplete = true;
        }
        quests.forEach((id, quest) -> quest.restoreProgress(progress.get(id)));
    }

    public void reset() { quests.values().forEach(quest -> quest.restoreProgress(0)); }

    @Override
    public void close() { publisher.unsubscribe(this); }
}
