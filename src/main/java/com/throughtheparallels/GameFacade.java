package com.throughtheparallels;

import com.throughtheparallels.progression.*;
import com.throughtheparallels.story.CrystalEventFactory;
import com.throughtheparallels.story.DeskEventFactory;
import com.throughtheparallels.story.TrustStrategy;
import com.throughtheparallels.story.DoubtStrategy;
import javafx.scene.canvas.Canvas;

import java.io.IOException;
import java.util.Objects;

/** Connects quests, saves and pause controls to the team's existing room implementation. */
public class GameFacade {
    private final RoomRenderer room;
    private final SaveManager saves;
    private final GameEventPublisher events = new GameEventPublisher();
    private final QuestManager quests = new QuestManager(events);
    private CharacterProfile currentCharacter;
    private boolean gameStarted;
    private boolean paused;
    private SaveManager.Ending ending = SaveManager.Ending.UNDECIDED;
    private String storyMessage = "Explore the bedroom. Press E near the writing desk.";

    public GameFacade() { this(new SaveManager()); }

    public GameFacade(SaveManager saves) {
        this.saves = Objects.requireNonNull(saves);
        room = new RoomRenderer();
    }

    public void startGame(CharacterProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Character profile cannot be null");
        room.setCharacter(profile);
        currentCharacter = profile;
        quests.reset();
        ending = SaveManager.Ending.UNDECIDED;
        storyMessage = "Explore the bedroom. Press E near the writing desk.";
        room.restoreProgress(false, RoomType.BEDROOM, 80, 80, 0);
        gameStarted = true;
        paused = false;
    }

    public void update(double deltaTime, double dx, double dy) {
        if (!gameStarted || paused) return;
        if (!Double.isFinite(deltaTime) || deltaTime < 0) throw new IllegalArgumentException("Invalid frame duration");
        WorldState before = room.getWorldState();
        room.update(deltaTime, isChoicePending() ? 0 : dx, isChoicePending() ? 0 : dy);
        // Observe the team's transition; no separate pickup animation or timer lives here.
        if (before == WorldState.TRANSFORMING && room.getWorldState() == WorldState.PARALLEL) {
            events.publish(GameEventPublisher.Type.CRYSTAL_COLLECTED, "crystal");
            room.hideCrystal();
            events.publish(GameEventPublisher.Type.HIKARI_APPEARED, "hikari");
            storyMessage = finishAction("\"Why is it... warm?\" Hikari has appeared. Press E near her to talk.");
        }
    }

    public String interact() {
        if (paused) return "Game paused. Resume to continue.";
        if (!gameStarted) return "Start a game first.";
        if (isWorldTransitioning()) return "\"Why is it... warm?\"";
        if (isChoicePending()) return "Hikari: \"So, you found it too.\" Choose: follow Hikari or stay in the room.";
        if (room.isPlayerNearHikari()) return interactWithHikari();
        if (isPlayerNearCrystal() && !hasCrystal()) return interactWithCrystal();
        if (room.isPlayerNearDesk()) return interactWithDesk();
        if (isPlayerNearDoor()) return interactWithDoor();
        return "Move closer to an object or character and press E.";
    }

    /** Delegate to the existing team implementation after the first quest. */
    public boolean activateCrystal() {
        return gameStarted && !paused && !hasCrystal() && quests.isCompleted(QuestManager.READ_DESK)
                && room.startCrystalTransition();
    }

    public String interactWithCrystal() {
        if (paused) return "Game paused. Resume to continue.";
        if (!gameStarted || !isPlayerNearCrystal()) return "Move closer to the crystal.";
        if (hasCrystal()) return "You have already found the crystal.";
        if (!quests.isCompleted(QuestManager.READ_DESK)) return "Examine the writing desk first.";
        if (activateCrystal()) {
            new CrystalEventFactory().triggerEvent();
            storyMessage = "\"Why is it... warm?\"";
        }
        return storyMessage;
    }

    public String interactWithDesk() {
        if (paused) return "Game paused. Resume to continue.";
        if (isWorldTransitioning()) return "Wait for the room to settle.";
        if (!gameStarted || !room.isPlayerNearDesk()) return "Move closer to the desk.";
        if (quests.isCompleted(QuestManager.READ_DESK)) return "\"I don't remember leaving this here...\"";
        new DeskEventFactory().triggerEvent();
        events.publish(GameEventPublisher.Type.DESK_EXAMINED, "desk");
        return finishAction("\"I don't remember leaving this here...\" A crystal glows on the bedside table.");
    }

    public String interactWithHikari() {
        if (paused) return "Game paused. Resume to continue.";
        if (!gameStarted || !room.isPlayerNearHikari()) return "Move closer to Hikari.";
        if (hasEnding()) return getEndingDescription();
        if (!quests.isCompleted(QuestManager.TALK_HIKARI)) {
            events.publish(GameEventPublisher.Type.HIKARI_TALKED, "hikari");
            return finishAction("Hikari: \"So, you found it too.\" Will you follow me, or stay here?");
        }
        return "Choose: follow Hikari or stay in the room.";
    }

    public String chooseEnding(SaveManager.Ending choice) {
        if (paused) return "Game paused. Resume to continue.";
        Objects.requireNonNull(choice);
        if (hasEnding()) return getEndingDescription();
        if (!gameStarted || !isChoicePending() || choice == SaveManager.Ending.UNDECIDED) {
            return "Talk to Hikari before making your choice.";
        }
        ending = choice;
        if (choice == SaveManager.Ending.FOLLOW_HIKARI) {
            room.changeRoom(RoomType.HALLWAY);
            events.publish(GameEventPublisher.Type.DOOR_OPENED, "bedroom_door");
        }
        events.publish(GameEventPublisher.Type.ENDING_CHOSEN, choice.name());
        storyMessage = finishAction(getEndingDescription());
        return storyMessage;
    }

    public String interactWithDoor() {
        if (paused) return "Game paused. Resume to continue.";
        if (isWorldTransitioning() || isChoicePending()) return "Finish the current interaction first.";
        if (!gameStarted || !isPlayerNearDoor()) return "Move closer to the door.";
        room.changeRoom(room.getCurrentRoom() == RoomType.BEDROOM ? RoomType.HALLWAY : RoomType.BEDROOM);
        events.publish(GameEventPublisher.Type.DOOR_OPENED, room.getCurrentRoom().name());
        return finishAction(room.getCurrentRoom() == RoomType.HALLWAY ? "You enter the hallway." : "You return to the bedroom.");
    }

    public String getDoorHint() {
        if (!gameStarted || !isPlayerNearDoor() || isWorldTransitioning() || isChoicePending()) return "";
        return room.getCurrentRoom() == RoomType.BEDROOM ? "E — Enter the hallway." : "E — Return to the bedroom.";
    }

    public void saveGame() throws IOException {
        if (!gameStarted) throw new IllegalStateException("No active game to save");
        saves.save(new SaveManager.SaveData(currentCharacter, room.getPlayerX(), room.getPlayerY(),
                room.getPlayerDirection(), quests.snapshot(), ending, room.getCurrentRoom()));
        events.publish(GameEventPublisher.Type.GAME_SAVED, "save_slot_1");
    }

    public void loadGame() throws IOException {
        SaveManager.SaveData data = saves.load();
        room.setCharacter(data.profile());
        quests.restore(data.quests());
        currentCharacter = data.profile();
        ending = data.ending();
        room.restoreProgress(hasCrystal(), data.room(), data.x(), data.y(), data.direction());
        gameStarted = true;
        storyMessage = hasEnding() ? getEndingDescription() : isChoicePending()
                ? "Hikari: \"So, you found it too.\" Choose what to do next."
                : hasCrystal() ? "Hikari is waiting in the bedroom. Press E near her to talk."
                : quests.isCompleted(QuestManager.READ_DESK) ? "A crystal glows on the bedside table. Press E to examine it."
                : "Explore the bedroom. Press E near the writing desk.";
        events.publish(GameEventPublisher.Type.GAME_LOADED, "save_slot_1");
    }

    private String finishAction(String message) {
        try {
            saveGame();
            storyMessage = message;
        } catch (IOException ex) {
            storyMessage = message + " Autosave failed: " + ex.getMessage() + ". Press F5 to retry.";
        }
        return storyMessage;
    }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = gameStarted && paused; }
    public boolean isChoicePending() { return quests.isCompleted(QuestManager.TALK_HIKARI) && !hasEnding(); }
    public boolean hasEnding() { return ending != SaveManager.Ending.UNDECIDED; }
    public SaveManager.Ending getEnding() { return ending; }
    public boolean isHikariVisible() { return room.isHikariVisible(); }
    public boolean isWorldTransitioning() { return room.getWorldState() == WorldState.TRANSFORMING; }
    public RoomType getCurrentRoom() { return room.getCurrentRoom(); }
    public String getStoryMessage() { return storyMessage; }
    public String getEndingDescription() {
        return switch (ending) {
            case FOLLOW_HIKARI -> "Hikari: \"" + new TrustStrategy().getDialogue()
                    + "\"\nEnding — Beyond the Boundary: You follow Hikari into the hallway.";
            case STAY_IN_ROOM -> "Hikari: \"" + new DoubtStrategy().getDialogue()
                    + "\"\nEnding — The Familiar Room: You choose to stay. You can keep exploring.";
            case UNDECIDED -> "Your story is not over yet.";
        };
    }

    public Canvas getGameView() { return room.getCanvas(); }
    public CharacterProfile getCurrentCharacter() { return currentCharacter; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean isPlayerNearCrystal() { return room.getCurrentRoom() == RoomType.BEDROOM && room.isPlayerNearCrystal(); }
    public boolean isPlayerNearDoor() {
        return room.getCurrentRoom() == RoomType.BEDROOM ? room.isPlayerNearDoor() : room.isPlayerNearHallwayExit();
    }
    public boolean hasCrystal() { return quests.isCompleted(QuestManager.FIND_CRYSTAL); }
    public QuestManager getQuestManager() { return quests; }
    public GameEventPublisher getEventPublisher() { return events; }

    public void moveHikariToHallway() {
        room.moveHikariToHallway();
    }

}
