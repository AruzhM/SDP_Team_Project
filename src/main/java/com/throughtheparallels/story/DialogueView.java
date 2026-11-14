package com.throughtheparallels.story;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class DialogueView extends VBox {

    private final Label speakerLabel;
    private final Label dialogueLabel;
    private final HBox choices;

    private final DialogueManager manager =
            new DialogueManager(new TrustStrategy());

    public DialogueView() {

        setSpacing(10);
        setPadding(new Insets(15));
        setMaxWidth(600);

        setStyle(
                "-fx-background-color: #252238;" +
                        "-fx-border-color: #b9a3e3;" +
                        "-fx-border-width: 3;"
        );

        speakerLabel = new Label("Hikari");
        speakerLabel.setStyle(
                "-fx-text-fill: #e9c8ff;" +
                        "-fx-font-size: 18px;" +
                        "-fx-font-weight: bold;"
        );

        dialogueLabel = new Label();
        dialogueLabel.setWrapText(true);
        dialogueLabel.setStyle(
                "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;"
        );

        Button trustButton = new Button("Trust Hikari");
        Button doubtButton = new Button("Doubt Hikari");

        trustButton.setOnAction(event ->
                selectStrategy(new TrustStrategy())
        );

        doubtButton.setOnAction(event ->
                selectStrategy(new DoubtStrategy())
        );

        choices = new HBox(10, trustButton, doubtButton);

        getChildren().addAll(
                speakerLabel,
                dialogueLabel,
                choices
        );

        choices.setVisible(false);
        choices.setManaged(false);
        setVisible(false);
    }

    public void showDialogue(String speaker, String text) {
        speakerLabel.setText(speaker);
        dialogueLabel.setText(text);
        choices.setVisible(false);
        choices.setManaged(false);
        setVisible(true);
    }

    public void showHikariChoice() {
        showDialogue("Hikari", "So, you found it too.");
        choices.setVisible(true);
        choices.setManaged(true);
    }

    private void selectStrategy(InteractionStrategy strategy) {
        manager.setStrategy(strategy);
        dialogueLabel.setText(manager.getDialogue());

        choices.setVisible(false);
        choices.setManaged(false);
    }

    public void hideDialogue() {
        setVisible(false);
    }
}
