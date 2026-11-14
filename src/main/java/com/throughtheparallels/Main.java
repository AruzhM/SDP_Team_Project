package com.throughtheparallels;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.EnumSet;
import java.util.Set;

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

        StackPane root = new StackPane(
                game.getGameView()
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

            if (key == KeyCode.E) {

                // Crystal interaction has priority.
                if (!game.activateCrystal()) {
                    game.interactWithDoor();
                }
            }

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

                game.update(deltaTime, dx, dy);
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
    }

    public static void main(String[] args) {
        launch(args);
    }
}
