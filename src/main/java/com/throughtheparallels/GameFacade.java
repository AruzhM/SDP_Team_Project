package com.throughtheparallels;

import com.throughtheparallels.progression.*;
import com.throughtheparallels.story.CrystalEventFactory;
import com.throughtheparallels.story.DeskEventFactory;
import com.throughtheparallels.story.TrustStrategy;
import com.throughtheparallels.story.DoubtStrategy;
import javafx.scene.canvas.Canvas;

import java.io.IOException;
import java.util.Objects;
import java.util.Locale;

/** Connects quests, saves and pause controls to the team's existing room implementation. */
public class GameFacade {
    private final RoomRenderer room;
    private final SaveManager saves;
    private final GameEventPublisher events = new GameEventPublisher();
    private final QuestManager quests = new QuestManager(events);
    private Ability ability = new BasicAbility();
    private boolean trainingBaseline;
    private final PulseTraining training = new PulseTraining();
    private CharacterProfile currentCharacter;
    private boolean gameStarted;
    private boolean paused;
    private SaveManager.Ending ending = SaveManager.Ending.UNDECIDED;
    private String storyMessage = "Explore the bedroom. Press E near the writing desk.";

    public GameFacade() { this(new SaveManager()); }

    public GameFacade(SaveManager saves) {
        this.saves = Objects.requireNonNull(saves);
        room = new RoomRenderer();
        events.subscribe(event -> {
            if (event.type() == GameEventPublisher.Type.QUEST_COMPLETED) {
                rebuildAbility();
            }
        });
    }

    public void startGame(CharacterProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Character profile cannot be null");
        room.setCharacter(profile);
        currentCharacter = profile;
        quests.reset();
        resetAbilities();
        ending = SaveManager.Ending.UNDECIDED;
        storyMessage = "Explore the bedroom. Press E near the writing desk.";
        room.restoreProgress(false, RoomType.BEDROOM, 80, 80, 0);
        gameStarted = true;
        paused = false;
    }

    public void update(double deltaTime, double dx, double dy) {
        if (!gameStarted || paused) return;
        if (!Double.isFinite(deltaTime) || deltaTime < 0) throw new IllegalArgumentException("Invalid frame duration");
        if (!isChoicePending() && !isWorldTransitioning()) training.update(deltaTime);
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
    public boolean isPlayerNearHikari() {
        return gameStarted && room.isPlayerNearHikari();
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

        if (ending == SaveManager.Ending.STAY_IN_ROOM) {
            return "Hikari: \"Still not convinced? That's alright. "
                    + "You don't have to trust me yet. "
                    + "Ask me when you're ready.\"";
        }

        if (ending == SaveManager.Ending.FOLLOW_HIKARI) {
            return "Hikari: \"There is something I want to show you.\"";
        }

        if (!gameStarted || !isChoicePending() || choice == SaveManager.Ending.UNDECIDED) {
            return "Talk to Hikari before making your choice.";
        }
        ending = choice;

        if (choice == SaveManager.Ending.FOLLOW_HIKARI) {

            // Hikari leaves, but the player stays here
            room.moveHikariToHallway();

            System.out.println(
                    "Hikari moved to the hallway."
            );
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
        resetAbilities();
        currentCharacter = data.profile();
        ending = data.ending();
        room.restoreProgress(hasCrystal(), data.room(), data.x(), data.y(), data.direction());

        if (ending == SaveManager.Ending.FOLLOW_HIKARI) {
            room.moveHikariToHallway();
        }

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
            case FOLLOW_HIKARI ->
                    "Hikari: \"" +
                            new TrustStrategy().getDialogue() +
                            "\"\nNew objective: Follow Hikari into the hallway.";

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

    private void resetAbilities() {
        trainingBaseline = false;
        rebuildAbility();
        training.resetSession();
        room.setTrainingHealth(training.getHealth());
    }

    public Ability getAbility() { return ability; }
    public int getTrainingHealth() { return training.getHealth(); }
    public double getPulseCooldown() { return training.getRemainingCooldown(); }
    public double getTrainingDistance() { return room.getTrainingDistance(); }
    public boolean isTrainingBaseline() { return trainingBaseline; }

    private void rebuildAbility() {
        ability = trainingBaseline ? new BasicAbility() : CrystalAbility.fromQuests(quests);
    }

    /** Temporary A/B comparison for training; never changes earned quest progress. */
    public void setTrainingBaseline(boolean baseline) {
        if (!canUsePulse()) throw new IllegalStateException("Training is not available now");
        trainingBaseline = baseline;
        rebuildAbility();
    }

    /** UI and keyboard share the same guards and business logic. */
    public boolean canUsePulse() {
        return gameStarted && !paused && hasCrystal() && !isWorldTransitioning()
                && !isChoicePending() && room.getCurrentRoom() == RoomType.HALLWAY;
    }

    public String usePulse() {
        if (!gameStarted) return "Start a game first.";
        if (paused) return "Game paused. Resume to continue.";
        if (isWorldTransitioning() || isChoicePending()) return "Finish the current interaction first.";
        if (!hasCrystal()) return "Collect the crystal to unlock the pulse.";
        if (room.getCurrentRoom() != RoomType.HALLWAY) return "Enter the hallway to practice with the target.";
        PulseTraining.Result result = training.fire(ability, room.getTrainingDistance());
        room.setTrainingHealth(training.getHealth());
        if (result.fired()) room.showPulse(ability.getRange());
        return switch (result.status()) {
            case HIT -> "Pulse hit: " + result.damage() + " damage. Target: " + result.remainingHealth() + "/60 HP.";
            case DESTROYED -> "Practice target cleared! Press R to reset it.";
            case OUT_OF_RANGE -> "Target out of range. Move closer; REACH extends the pulse radius.";
            case COOLDOWN -> String.format(Locale.ROOT, "Pulse recharging: %.2f s.", training.getRemainingCooldown());
            case ALREADY_DESTROYED -> "Target already cleared. Press R to reset it.";
        };
    }

    public String resetTrainingTarget() {
        if (!canUsePulse()) return "Enter the hallway with the crystal and finish any dialogue to practice.";
        training.resetTarget();
        room.setTrainingHealth(training.getHealth());
        return "Practice target reset: 60 HP. Your unlocked upgrades are unchanged.";
    }

    public void moveHikariToHallway() {
        room.moveHikariToHallway();
    }

}
