package com.throughtheparallels;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {

        RoomRenderer room = new RoomRenderer();

        room.render();

        StackPane root = new StackPane(
                room.getCanvas()
        );

        root.setStyle("-fx-background-color: #252238;");

        Scene scene = new Scene(
                root, 960, 720
        );


        scene.setOnKeyPressed(event -> {

            switch (event.getCode()) {

                case W -> room.movePlayer(0, -2);
                case S -> room.movePlayer(0, 2);
                case A -> room.movePlayer(-2, 0);
                case D -> room.movePlayer(2, 0);

                default -> {
                    //ignoring other keys
                }
            }
        });



        stage.setTitle(
                "Through The Parallels - Hikaru's Room"
        );

        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

}
