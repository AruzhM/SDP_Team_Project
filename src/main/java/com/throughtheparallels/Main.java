package com.throughtheparallels;

import com.throughtheparallels.progression.GameEventPublisher;
import com.throughtheparallels.progression.Quest;
import com.throughtheparallels.progression.SaveManager;
import com.throughtheparallels.story.DialogueView;
import com.throughtheparallels.story.TrustStrategy;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.WindowEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Main extends Application {
    private final SaveManager saves = new SaveManager();

    @Override
    public void start(Stage stage) {
        stage.setTitle("Through The Parallels");
        stage.setResizable(false);
        CharacterCreator creator = new CharacterCreator(profile -> {
            if (saves.hasSave()) {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                        "Starting a new game replaces your saved progress. Continue?", ButtonType.OK, ButtonType.CANCEL);
                confirm.initOwner(stage);
                if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            }
            GameFacade game = new GameFacade(saves);
            game.startGame(profile);
            showGame(stage, game);
        }, () -> {
            try {
                GameFacade game = new GameFacade(saves);
                game.loadGame();
                showGame(stage, game);
            } catch (IOException | RuntimeException ex) {
                showError(stage, "Could not load the saved game", ex.getMessage());
            }
        }, saves.hasSave());
        stage.setScene(creator.createScene());
        stage.show();
    }

    void showGame(Stage stage, GameFacade game) {
        Label journal = label(14);
        Label message = label(14);
        Label eventNotice = label(12);
        Label doorHint = label(14);
        doorHint.setId("door-hint");
        doorHint.setStyle("-fx-text-fill: #f4d59a; -fx-font-size: 14px;");
        Label controls = label(13);
        controls.setText("WASD: move  |  E: interact  |  F5: save  |  F9: load  |  ESC: pause");
        message.setText(game.getStoryMessage());
        journal.setId("quest-journal");
        journal.setMinWidth(330);
        journal.setPrefWidth(330);
        journal.setMaxWidth(330);
        journal.setPadding(new Insets(18));
        journal.setStyle("-fx-text-fill: #e4d4f5; -fx-font-size: 14px; -fx-background-color: #332d47; -fx-background-radius: 12;");
        DialogueView hikariDialogue = new DialogueView();
        hikariDialogue.setId("hikari-dialogue");
        try {
            game.saveGame();
        } catch (IOException ex) {
            message.setText("Save failed: " + ex.getMessage() + ". Press F5 to retry.");
        }

        Runnable refreshHud = () -> {
            journal.setText("QUESTS\n" + game.getQuestManager().getQuests().stream()
                    .map(q -> (q.isCompleted() ? "✓ " : "✗ ") + q.getDescription()
                            + "\nObjective: " + q.getObjective())
                    .collect(Collectors.joining("\n\n")));
            String hint = game.getDoorHint();
            doorHint.setText(hint);
            doorHint.setVisible(!hint.isEmpty());
            doorHint.setManaged(!hint.isEmpty());
            message.setVisible(!game.isChoicePending());
            message.setManaged(!game.isChoicePending());
            hikariDialogue.setDisable(game.isPaused());
            if (game.isChoicePending()) {
                if (!hikariDialogue.isVisible()) hikariDialogue.showHikariChoice();
            } else {
                hikariDialogue.hideDialogue();
            }

        };
        game.getEventPublisher().subscribe(event -> {
            if (event.type() == GameEventPublisher.Type.QUEST_COMPLETED) {
                String description = game.getQuestManager().getQuests().stream()
                        .filter(q -> q.getId().equals(event.subject())).map(Quest::getDescription).findFirst().orElse(event.subject());
                eventNotice.setText("Quest completed: " + description);
            } else if (event.type() == GameEventPublisher.Type.GAME_LOADED) {
                eventNotice.setText("Saved progress restored.");
            }
        });

        hikariDialogue.setOnChoice(strategy -> {
            if (game.isPaused() || !game.isChoicePending()) return;
            SaveManager.Ending choice = strategy instanceof TrustStrategy
                    ? SaveManager.Ending.FOLLOW_HIKARI : SaveManager.Ending.STAY_IN_ROOM;
            message.setText(game.chooseEnding(choice));
            refreshHud.run();
        });
        
        StackPane roomPanel = new StackPane(game.getGameView());
        roomPanel.setMaxSize(528, 360);
        VBox dialogue = new VBox(10, doorHint, eventNotice, message, hikariDialogue, controls);
        dialogue.setMaxWidth(912);
        dialogue.setPadding(new Insets(18));
        dialogue.setStyle("-fx-background-color: #332d47; -fx-background-radius: 12;");
        StackPane root = new StackPane(roomPanel, journal, dialogue);
        StackPane.setAlignment(journal, Pos.TOP_LEFT);
        StackPane.setMargin(journal, new Insets(24, 0, 0, 24));
        StackPane.setAlignment(roomPanel, Pos.TOP_RIGHT);
        StackPane.setMargin(roomPanel, new Insets(24, 24, 0, 0));
        StackPane.setAlignment(dialogue, Pos.BOTTOM_CENTER);
        StackPane.setMargin(dialogue, new Insets(0, 24, 24, 24));
        // Overlay panels use their content height instead of filling the play area.
        journal.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        dialogue.setMaxHeight(javafx.scene.layout.Region.USE_PREF_SIZE);
        root.setStyle("-fx-background-color: #252238;");
        PauseMenu pauseMenu = new PauseMenu();
        root.getChildren().add(pauseMenu);
        Scene scene = new Scene(root, 960, 720);
        Set<KeyCode> pressedKeys = EnumSet.noneOf(KeyCode.class);
        Runnable resume = () -> {
            pressedKeys.clear();
            game.setPaused(false);
            pauseMenu.setOpen(false);
            refreshHud.run();
            root.requestFocus();
        };
        Runnable save = () -> {
            try {
                game.saveGame();
                message.setText("Progress saved.");
                pauseMenu.showFeedback("Progress saved.");
            } catch (IOException ex) {
                String error = "Save failed: " + ex.getMessage();
                message.setText(error);
                pauseMenu.showFeedback(error);
            }
        };
        Runnable reload = () -> {
            try {
                game.loadGame();
                pressedKeys.removeAll(EnumSet.of(KeyCode.W, KeyCode.A, KeyCode.S, KeyCode.D));
                updateTitle(stage, game);
                message.setText(game.getStoryMessage());
                pauseMenu.showFeedback("Saved progress reloaded. Select Resume to continue.");
                refreshHud.run();
            } catch (IOException ex) {
                String error = "Load failed: " + ex.getMessage() + ". Current progress kept.";
                message.setText(error);
                pauseMenu.showFeedback(error);
            }
        };
        Runnable restart = () -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Restart the story with the same character? This replaces your saved progress.",
                    ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText("Restart game?");
            confirm.initOwner(stage);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            pressedKeys.clear();
            game.startGame(game.getCurrentCharacter());
            game.setPaused(true);
            eventNotice.setText("");
            message.setText(game.getStoryMessage());
            refreshHud.run();
            try {
                game.saveGame();
                resume.run();
            } catch (IOException ex) {
                pauseMenu.showFeedback("Story restarted, but saving failed: " + ex.getMessage() + ". Use Save to retry.");
            }
        };
        pauseMenu.configure(resume, save, reload, restart, () -> {
            // Use the same save/error/cancel path as closing the window normally.
            WindowEvent close = new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST);
            stage.fireEvent(close);
            if (!close.isConsumed()) stage.close();
        });
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                if (!pressedKeys.contains(KeyCode.ESCAPE)) {
                    if (game.isPaused()) resume.run();
                    else {
                        pressedKeys.clear();
                        game.setPaused(true);
                        pauseMenu.setOpen(true);
                        refreshHud.run();
                    }
                    // Keep ESC until release, preventing auto-repeat from toggling the menu.
                    pressedKeys.add(KeyCode.ESCAPE);
                }
                event.consume();
            } else if (game.isPaused() && switch (event.getCode()) {
                case TAB, ENTER, SPACE, UP, DOWN, LEFT, RIGHT -> false;
                default -> true;
            }) {
                event.consume();
            }
        });
        scene.addEventFilter(KeyEvent.KEY_RELEASED, event -> pressedKeys.remove(event.getCode()));
        scene.setOnKeyPressed(event -> {
            if (game.isPaused()) return;
            // Ignore OS key repeats: each press triggers an action only once.
            if (!pressedKeys.add(event.getCode())) return;
            switch (event.getCode()) {
                case E -> message.setText(game.interact());
                case DIGIT1, NUMPAD1 -> {
                    if (game.isChoicePending()) hikariDialogue.chooseTrust();
                }
                case DIGIT2, NUMPAD2 -> {
                    if (game.isChoicePending()) hikariDialogue.chooseDoubt();
                }
                case F5 -> save.run();
                case F9 -> reload.run();
                default -> { }
            }
            refreshHud.run();
            event.consume();
        });
        scene.setOnKeyReleased(event -> pressedKeys.remove(event.getCode()));
        stage.focusedProperty().addListener((obs, oldValue, focused) -> {
            if (!focused) pressedKeys.clear();
        });

        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastTime;
            private String lastStoryMessage = game.getStoryMessage();

            @Override
            public void handle(long now) {
                if (lastTime == 0) { lastTime = now; return; }
                double deltaTime = Math.min((now - lastTime) / 1_000_000_000.0, 0.05);
                lastTime = now;
                double dx = (pressedKeys.contains(KeyCode.D) ? 1 : 0) - (pressedKeys.contains(KeyCode.A) ? 1 : 0);
                double dy = (pressedKeys.contains(KeyCode.S) ? 1 : 0) - (pressedKeys.contains(KeyCode.W) ? 1 : 0);
                game.update(deltaTime, dx, dy);
                if (!lastStoryMessage.equals(game.getStoryMessage())) {
                    lastStoryMessage = game.getStoryMessage();
                    message.setText(lastStoryMessage);
                }
                refreshHud.run();
            }
        };
        stage.setScene(scene);
        updateTitle(stage, game);
        stage.setOnCloseRequest(event -> {
            pressedKeys.clear();
            gameLoop.stop();
            try {
                game.saveGame();
            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                        "Progress could not be saved: " + ex.getMessage() + "\nClose without saving?",
                        ButtonType.YES, ButtonType.CANCEL);
                alert.initOwner(stage);
                if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.YES) {
                    event.consume();
                    gameLoop.start();
                }
            }
        });
        refreshHud.run();
        gameLoop.start();
        root.requestFocus();
    }

    private Label label(int size) {
        Label label = new Label();
        label.setStyle("-fx-text-fill: #e4d4f5; -fx-font-size: " + size + "px;");
        label.setWrapText(true);
        label.setMaxWidth(880);
        return label;
    }

    private void updateTitle(Stage stage, GameFacade game) {
        stage.setTitle("Through The Parallels - " + game.getCurrentCharacter().getName());
    }

    private void showError(Stage stage, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(title);
        alert.initOwner(stage);
        alert.showAndWait();
    }

    public static void main(String[] args) { launch(args); }
}
