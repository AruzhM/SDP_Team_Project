package com.throughtheparallels;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.EnumSet;
import java.util.Set;


import com.throughtheparallels.story.DialogueView;
import javafx.geometry.Pos;


public class Main extends Application {

    @Override
    public void start(Stage stage) {

        stage.setTitle("Through The Parallels");
        stage.setResizable(false);

        CharacterCreator creator =
                new CharacterCreator(
                        profile -> startGame(stage, profile)
                );

        stage.setScene(creator.createScene());
        stage.show();
    }

    private void startGame(
            Stage stage,
            CharacterProfile profile
    ) {


        GameFacade game = new GameFacade();
        game.startGame(profile);

        // Create your teammate's dialogue interface
        DialogueView dialogue = new DialogueView();

        // Place the dialogue on top of the game canvas
        StackPane root = new StackPane(
                game.getGameView(),
                dialogue
        );

        // Dialogue appears near the bottom of the window
        StackPane.setAlignment(
                dialogue,
                Pos.BOTTOM_CENTER
        );

        StackPane.setMargin(
                dialogue,
                new javafx.geometry.Insets(0, 0, 25, 0)
        );


        root.setStyle(
                "-fx-background-color: #252238;"
        );

        Scene scene = new Scene(root, 960, 720);

        Set<KeyCode> pressedKeys =
                EnumSet.noneOf(KeyCode.class);


        scene.setOnKeyPressed(event -> {

            KeyCode key = event.getCode();

            // Ignore repeated key events while held
            if (!pressedKeys.add(key)) {
                return;
            }


            if (key == KeyCode.E && !dialogue.isVisible()) {

                if (game.canTalkToHikari()) {

                    dialogue.showHikariChoice();

                } else if (!game.activateCrystal()) {

                    game.interactWithDoor();
                }
            }

            // Escape closes the dialogue
            if (key == KeyCode.ESCAPE &&
                    dialogue.isVisible()) {

                dialogue.hideDialogue();
            }


            dialogue.setOnOutcome(outcome -> {

                System.out.println(
                        "Hikari dialogue outcome: " + outcome
                );

                // Later: forward to the quest/save system.
            });



        });


        scene.setOnKeyReleased(event ->
                pressedKeys.remove(event.getCode())
        );

        stage.focusedProperty().addListener(
                (obs, oldValue, focused) -> {
                    if (!focused) {
                        pressedKeys.clear();
                    }
                }
        );

        AnimationTimer gameLoop = new AnimationTimer() {

            private long lastTime = 0;

            @Override
            public void handle(long now) {

                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double deltaTime =
                        (now - lastTime)
                                / 1_000_000_000.0;

                lastTime = now;

                deltaTime = Math.min(
                        deltaTime, 0.05
                );

                double dx = 0;
                double dy = 0;

                if (pressedKeys.contains(KeyCode.W))
                    dy--;

                if (pressedKeys.contains(KeyCode.S))
                    dy++;

                if (pressedKeys.contains(KeyCode.A))
                    dx--;

                if (pressedKeys.contains(KeyCode.D))
                    dx++;


                if (dialogue.isVisible()) {

                    // Stop movement while talking
                    game.update(deltaTime, 0, 0);

                } else {

                    game.update(deltaTime, dx, dy);
                }

            }
        };

        stage.setScene(scene);
        stage.setTitle(
                "Through The Parallels - " +
                        profile.getName()
        );

        stage.setOnCloseRequest(event ->
                gameLoop.stop()
        );

        gameLoop.start();

        game.canTalkToHikari();
    }



    public static void main(String[] args) {
        launch(args);
    }
}
