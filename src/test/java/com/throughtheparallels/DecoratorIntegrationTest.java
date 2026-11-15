package com.throughtheparallels;

import com.throughtheparallels.progression.SaveManager;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import com.throughtheparallels.story.DialogueView;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/** Run with mvn -Pheadless-tests test. Exercises the real room, facade and GUI. */
@EnabledIfSystemProperty(named = "sdp.fxTests", matches = "true")
class DecoratorIntegrationTest {
    @TempDir Path directory;

    @BeforeAll static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); });
        assertTrue(started.await(15, TimeUnit.SECONDS));
    }

    private void onFxThread(ThrowingAction action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> { action.run(); return null; });
        Platform.runLater(task);
        task.get(30, TimeUnit.SECONDS);
    }

    @FunctionalInterface private interface ThrowingAction { void run() throws Exception; }

    private GameFacade newGame() {
        GameFacade game = new GameFacade(new SaveManager(directory.resolve("save.properties")));
        game.startGame(new CharacterBuilder().name("Decorator demo").build());
        return game;
    }

    private void unlockAndEnterHallway(GameFacade game) {
        unlockAndEnterHallway(game, SaveManager.Ending.STAY_IN_ROOM);
    }

    private void unlockAndEnterHallway(GameFacade game, SaveManager.Ending ending) {
        // Move from (80,80) to the desk, then to the crystal. Use real interactions.
        game.update(20.0 / 65, 0, -1);
        game.interactWithDesk();
        assertEquals(0.52, game.getAbility().getCooldown(), 1e-9);
        game.update(72.0 / 65, -1, 0);
        game.update(4.0 / 65, 0, -1);
        game.interactWithCrystal();
        assertTrue(game.isWorldTransitioning());
        game.update(1.5, 0, 0);
        assertTrue(game.hasCrystal());
        assertEquals(20, game.getAbility().getDamage());
        game.update(56.0 / 65, 1, 0);
        game.update(24.0 / 65, 0, 1);
        game.interactWithHikari();
        assertTrue(game.isChoicePending());
        assertFalse(game.canUsePulse());
        game.chooseEnding(ending);
        assertEquals(52, game.getAbility().getRange());
        game.update(56.0 / 65, 1, 0);
        game.update(48.0 / 65, 0, -1);
        game.interactWithDoor();
        assertEquals(RoomType.HALLWAY, game.getCurrentRoom());
        assertTrue(game.canUsePulse());
    }

    @Test void completeStoryUnlocksRealPulseAndLoadingDoesNotStackUpgrades() throws Exception {
        onFxThread(() -> {
            GameFacade game = newGame();
            assertFalse(game.canUsePulse());
            assertThrows(IllegalStateException.class, () -> game.setTrainingBaseline(true));
            assertTrue(game.usePulse().contains("Collect"));
            unlockAndEnterHallway(game);
            assertTrue(game.usePulse().contains("20 damage"));
            assertEquals(40, game.getTrainingHealth());
            game.setPaused(true);
            game.update(10, 0, 0);
            assertEquals(0.52, game.getPulseCooldown(), 1e-9);
            assertTrue(game.usePulse().contains("paused"));
            game.setPaused(false);
            game.setTrainingBaseline(true);
            assertEquals(10, game.getAbility().getDamage());
            assertTrue(game.usePulse().contains("recharging"));
            game.update(0.8, 0, 0);
            assertTrue(game.usePulse().contains("10 damage"));
            game.saveGame();
            for (int i = 0; i < 3; i++) {
                game.loadGame();
                assertFalse(game.isTrainingBaseline());
                assertEquals(20, game.getAbility().getDamage());
                assertEquals(52, game.getAbility().getRange());
                assertEquals(0.52, game.getAbility().getCooldown(), 1e-9);
                assertEquals(60, game.getTrainingHealth());
            }
            game.startGame(game.getCurrentCharacter());
            assertEquals(10, game.getAbility().getDamage());
            assertEquals(36, game.getAbility().getRange());
            assertFalse(game.canUsePulse());
        });
    }

    @Test void reachChangesRealRoomHitDetection() throws Exception {
        onFxThread(() -> {
            GameFacade game = newGame();
            unlockAndEnterHallway(game);
            game.update(28.0 / 65, -1, 0); // Distance ~=45.6: outside 36, inside 52.
            assertTrue(game.getTrainingDistance() > 36 && game.getTrainingDistance() < 52);
            game.setTrainingBaseline(true);
            assertTrue(game.usePulse().contains("out of range"));
            game.update(0.8, 0, 0);
            game.setTrainingBaseline(false);
            assertTrue(game.usePulse().contains("20 damage"));
        });
    }

    @Test void guiControlsChangeActualTargetAndFitInTheWindow() throws Exception {
        onFxThread(() -> {
            GameFacade game = newGame();
            unlockAndEnterHallway(game);
            Stage stage = new Stage();
            try {
                new Main().showGame(stage, game);
                stage.show();
                var scene = stage.getScene();
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                Button pulse = (Button) scene.lookup("#pulse-button");
                Button reset = (Button) scene.lookup("#reset-target-button");
                CheckBox baseline = (CheckBox) scene.lookup("#baseline-toggle");
                pulse.fire();
                assertEquals(40, game.getTrainingHealth());
                assertTrue(((Label) scene.lookup("#ability-stats")).getText().contains("Damage 20"));
                assertTrue(pulse.isDisabled());
                reset.fire();
                assertEquals(60, game.getTrainingHealth());
                baseline.fire();
                assertEquals(10, game.getAbility().getDamage());
                baseline.fire();
                assertEquals(20, game.getAbility().getDamage());
                Node panel = scene.lookup("#ability-panel");
                Node story = scene.lookup("#story-panel");
                assertTrue(panel.localToScene(panel.getBoundsInLocal()).getMaxY()
                        <= story.localToScene(story.getBoundsInLocal()).getMinY(), "Training must not overlap dialogue");
                assertTrue(baseline.localToScene(baseline.getBoundsInLocal()).getMaxX() <= scene.getWidth());
                saveScreenshot(scene.snapshot(null), "decorator-training.png");
            } finally {
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                stage.hide();
            }
        });
    }

    @Test void hallwayDialogueDisablesTrainingControlsAndFitsAboveTheFooter() throws Exception {
        onFxThread(() -> {
            GameFacade game = newGame();
            unlockAndEnterHallway(game, SaveManager.Ending.FOLLOW_HIKARI);
            game.update(64.0 / 65, -1, 0);
            game.update(24.0 / 65, 0, -1);
            Stage stage = new Stage();
            try {
                new Main().showGame(stage, game);
                stage.show();
                var scene = stage.getScene();
                scene.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.E,
                        false, false, false, false));
                assertTrue(((DialogueView) scene.lookup("#hikari-dialogue")).isTrustDialogueActive());
                assertTrue(((Button) scene.lookup("#pulse-button")).isDisabled());
                assertTrue(((Button) scene.lookup("#reset-target-button")).isDisabled());
                assertTrue(((CheckBox) scene.lookup("#baseline-toggle")).isDisabled());
                scene.getRoot().applyCss();
                scene.getRoot().layout();
                Node panel = scene.lookup("#ability-panel");
                Node story = scene.lookup("#story-panel");
                assertFalse(panel.isVisible(), "Training card yields space to the active dialogue");
                assertFalse(panel.isManaged());
                assertTrue(story.localToScene(story.getBoundsInLocal()).getMinY() >= 384,
                        "Hikari dialogue must not cover the room");
                saveScreenshot(scene.snapshot(null), "decorator-dialogue.png");
                DialogueView dialogue = (DialogueView) scene.lookup("#hikari-dialogue");
                for (int i = 0; i < 20 && dialogue.isTrustDialogueActive(); i++) dialogue.advanceTrustDialogue();
                assertFalse(dialogue.isTrustDialogueActive());
                scene.getOnKeyPressed().handle(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.R,
                        false, false, false, false));
                assertTrue(panel.isVisible(), "Training card returns when dialogue ends");
            } finally {
                stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
                stage.hide();
            }
        });
    }

    private void saveScreenshot(WritableImage image, String name) throws Exception {
        String path = System.getProperty("sdp.screenshotDir");
        if (path == null) return;
        Path output = Path.of(path);
        Files.createDirectories(output);
        BufferedImage png = new BufferedImage((int) image.getWidth(), (int) image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < png.getHeight(); y++) {
            for (int x = 0; x < png.getWidth(); x++) png.setRGB(x, y, image.getPixelReader().getArgb(x, y));
        }
        ImageIO.write(png, "png", output.resolve(name).toFile());
    }
}
