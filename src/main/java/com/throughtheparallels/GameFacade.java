package com.throughtheparallels;

import com.throughtheparallels.progression.*;
import com.throughtheparallels.story.CrystalEventFactory;
import com.throughtheparallels.story.DeskEventFactory;
import javafx.scene.canvas.Canvas;

import java.io.IOException;
import java.util.Objects;

/** Coordinates the room, progression, combat and persistence on the JavaFX thread. */
public class GameFacade {
    private final RoomRenderer room;
    private final SaveManager saves;
    private final GameEventPublisher events = new GameEventPublisher();
    private final QuestManager quests = new QuestManager(events);
    private CharacterProfile currentCharacter;
    private boolean gameStarted;
    private boolean paused;
    private Ability ability = new BasicAbility();
    private int sealHealth = SaveManager.MAX_SEAL_HEALTH;
    private double cooldownRemaining;
    private SaveManager.Ending ending = SaveManager.Ending.UNDECIDED;
    private String storyMessage = "Explore the bedroom. Press E near the writing desk.";

    public GameFacade() { this(new SaveManager()); }

    public GameFacade(SaveManager saves) {
        this.saves = Objects.requireNonNull(saves);
        room = new RoomRenderer();
        events.subscribe(event -> {
            if (event.type() == GameEventPublisher.Type.QUEST_COMPLETED) {
                ability = CrystalAbility.fromQuests(quests);
                refreshWorld();
            }
        });
    }

    public void startGame(CharacterProfile profile) {
        if (profile == null) throw new IllegalArgumentException("Character profile cannot be null");
        room.setCharacter(profile);
        currentCharacter = profile;
        quests.reset();
        ability = new BasicAbility();
        sealHealth = SaveManager.MAX_SEAL_HEALTH;
        cooldownRemaining = 0;
        ending = SaveManager.Ending.UNDECIDED;
        storyMessage = "Explore the bedroom. Press E near the writing desk.";
        room.restorePlayer(80, 80, 0);
        gameStarted = true;
        paused = false;
        refreshWorld();
    }

    public void update(double deltaTime, double dx, double dy) {
        if (!gameStarted || paused) return;
        if (!Double.isFinite(deltaTime) || deltaTime < 0) throw new IllegalArgumentException("Invalid frame duration");
        cooldownRemaining = Math.max(0, cooldownRemaining - deltaTime);
        boolean lifting = room.isLiftingCrystal();
        room.update(deltaTime, isChoicePending() || hasLeftRoom() ? 0 : dx, isChoicePending() || hasLeftRoom() ? 0 : dy);
        if (lifting && !room.isLiftingCrystal()) {
            events.publish(GameEventPublisher.Type.CRYSTAL_COLLECTED, "crystal");
            events.publish(GameEventPublisher.Type.HIKARI_APPEARED, "hikari");
            storyMessage = finishAction("\"Why is it... warm?\" Hikari has appeared near the door. Press E to talk to her.");
        }
    }

    public String interact() {
        if (paused) return "Game paused. Resume to continue.";
        if (!gameStarted) return "Start a game first.";
        if (room.isLiftingCrystal()) return "\"Why is it... warm?\"";
        if (hasLeftRoom()) return getEndingDescription();
        if (isChoicePending()) return "Hikari: \"So, you found it too.\" Choose: follow Hikari or stay in the room.";
        if (room.isPlayerNearHikari()) return interactWithHikari();
        if (isPlayerNearCrystal() && !hasCrystal()) return interactWithCrystal();
        if (room.isPlayerNearDesk()) return interactWithDesk();
        if (isPlayerNearDoor()) return interactWithDoor();
        return "Move closer to the desk, crystal or Hikari and press E.";
    }

    public String interactWithCrystal() {
        if (paused) return "Game paused. Resume to continue.";
        if (!gameStarted || !isPlayerNearCrystal()) return "Move closer to the crystal.";
        if (hasCrystal()) return "The crystal is already yours. Press SPACE to use it.";
        if (room.isLiftingCrystal()) return "The crystal is rising...";
        if (!quests.isCompleted(QuestManager.READ_DESK)) return "Examine the writing desk first.";
        new CrystalEventFactory().triggerEvent();
        room.beginCrystalLift();
        storyMessage = "\"Why is it... warm?\"";
        return storyMessage;
    }

    public String interactWithDesk() {
        if (paused) return "Game paused. Resume to continue.";
        if (room.isLiftingCrystal()) return "The crystal is rising...";
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
            sealHealth = 0;
            events.publish(GameEventPublisher.Type.DOOR_OPENED, "bedroom_door");
        }
        events.publish(GameEventPublisher.Type.ENDING_CHOSEN, choice.name());
        storyMessage = finishAction(getEndingDescription());
        return storyMessage;
    }

    public String interactWithDoor() {
        if (paused) return "Game paused. Resume to continue.";
        if (!gameStarted || !isPlayerNearDoor()) return "Move closer to the door.";
        return hasLeftRoom() ? getEndingDescription() : getDoorHint();
    }

    /** Context hint is independent of dialogue, so approaching the door never erases a story line. */
    public String getDoorHint() {
        if (!gameStarted || !isPlayerNearDoor() || room.isLiftingCrystal() || hasLeftRoom()) return "";
        if (!hasCrystal()) return "The door is locked. Find the crystal to unlock it.";
        if (sealHealth > 0) return "The door is locked. Use the crystal (SPACE) to unlock it.";
        return ending == SaveManager.Ending.STAY_IN_ROOM
                ? "The door is unlocked. You chose to stay and explore the room."
                : "The door is unlocked. Talk to Hikari before leaving.";
    }

    public boolean isPaused() { return paused; }
    public void setPaused(boolean paused) { this.paused = gameStarted && paused; }
    private boolean hasLeftRoom() { return ending == SaveManager.Ending.FOLLOW_HIKARI; }

    public boolean isChoicePending() { return quests.isCompleted(QuestManager.TALK_HIKARI) && !hasEnding(); }
    public boolean hasEnding() { return ending != SaveManager.Ending.UNDECIDED; }
    public SaveManager.Ending getEnding() { return ending; }
    public boolean isHikariVisible() { return room.isHikariVisible(); }
    public boolean isLiftingCrystal() { return room.isLiftingCrystal(); }
    public String getStoryMessage() { return storyMessage; }
    public String getEndingDescription() {
        return switch (ending) {
            case FOLLOW_HIKARI -> "Ending — Beyond the Boundary: You follow Hikari into the light, leaving the familiar room behind.";
            case STAY_IN_ROOM -> "Ending — The Familiar Room: You choose to stay. The light fades, but the warm crystal remains in your inventory. You can keep exploring.";
            case UNDECIDED -> "Your story is not over yet.";
        };
    }

    public String useCrystalAbility() {
        if (paused) return "Game paused. Resume to continue.";
        if (isChoicePending() || hasLeftRoom()) return "The crystal is quiet. Your decision matters now.";
        if (!gameStarted || !hasCrystal()) return "Find the crystal before using its ability.";
        if (cooldownRemaining > 0) return "The crystal is recharging.";
        cooldownRemaining = ability.getCooldown();
        room.showPulse(ability.getRange());
        events.publish(GameEventPublisher.Type.ABILITY_USED, "crystal_pulse");
        if (sealHealth == 0 || !room.canHitSeal(ability.getRange())) {
            return "Crystal pulse! Move closer and face the door to unlock it.";
        }
        sealHealth = Math.max(0, sealHealth - ability.getDamage());
        events.publish(GameEventPublisher.Type.SEAL_DAMAGED, "door_seal");
        if (sealHealth == 0) {
            events.publish(GameEventPublisher.Type.SEAL_BROKEN, "door_seal");
            return finishAction(ending == SaveManager.Ending.STAY_IN_ROOM
                    ? "The door is unlocked. You can keep exploring the room."
                    : "The door is unlocked. Hikari is waiting to speak with you.");
        }
        return finishAction("The pulse deals " + ability.getDamage() + " damage to the lock. Its strength is now " + sealHealth + ".");
    }

    public void saveGame() throws IOException {
        if (!gameStarted) throw new IllegalStateException("No active game to save");
        saves.save(new SaveManager.SaveData(currentCharacter, room.getPlayerX(), room.getPlayerY(),
                room.getPlayerDirection(), sealHealth, cooldownRemaining, quests.snapshot(), ending));
        events.publish(GameEventPublisher.Type.GAME_SAVED, "save_slot_1");
    }

    public void loadGame() throws IOException {
        // Parse and validate the entire file before mutating the active game.
        SaveManager.SaveData data = saves.load();
        room.setCharacter(data.profile());
        quests.restore(data.quests());
        currentCharacter = data.profile();
        room.restorePlayer(data.x(), data.y(), data.direction());
        ending = data.ending();
        sealHealth = data.sealHealth();
        cooldownRemaining = data.cooldownRemaining();
        ability = CrystalAbility.fromQuests(quests);
        gameStarted = true;
        refreshWorld();
        storyMessage = hasEnding() ? getEndingDescription() : isChoicePending()
                ? "Hikari: \"So, you found it too.\" Choose what to do next."
                : hasCrystal() ? "Hikari is waiting near the door. Press E to talk to her."
                : quests.isCompleted(QuestManager.READ_DESK) ? "A crystal glows on the bedside table. Press E to pick it up."
                : "Explore the bedroom. Press E near the writing desk.";
        events.publish(GameEventPublisher.Type.GAME_LOADED, "save_slot_1");
    }

    private String finishAction(String message) {
        refreshWorld();
        try {
            saveGame();
            return message;
        } catch (IOException ex) {
            return message + " Autosave failed: " + ex.getMessage() + ". Press F5 to retry.";
        }
    }

    private void refreshWorld() {
        room.setWorldState(hasCrystal(), sealHealth == 0, ending == SaveManager.Ending.FOLLOW_HIKARI,
                hasCrystal() && !hasEnding());
    }

    public Canvas getGameView() { return room.getCanvas(); }
    public CharacterProfile getCurrentCharacter() { return currentCharacter; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean isPlayerNearCrystal() { return room.isPlayerNearCrystal(); }
    public boolean isPlayerNearDoor() { return room.isPlayerNearDoor(); }
    public boolean hasCrystal() { return quests.isCompleted(QuestManager.FIND_CRYSTAL); }
    public QuestManager getQuestManager() { return quests; }
    public GameEventPublisher getEventPublisher() { return events; }
    public Ability getAbility() { return ability; }
    public int getSealHealth() { return sealHealth; }
    public double getCooldownRemaining() { return cooldownRemaining; }
}
