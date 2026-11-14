package com.throughtheparallels.story;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DialoguePreview extends Application {

    @Override
    public void start(Stage stage) {

        DialogueView dialogue = new DialogueView();
        dialogue.showHikariChoice();

        StackPane root = new StackPane(dialogue);
        root.setAlignment(Pos.BOTTOM_CENTER);
        root.setStyle("-fx-background-color: #252238;");

        Scene scene = new Scene(root, 800, 500);

        stage.setTitle("Hikari Dialogue Preview");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
