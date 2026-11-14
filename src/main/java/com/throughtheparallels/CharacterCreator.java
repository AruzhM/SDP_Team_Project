package com.throughtheparallels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Consumer;

public class CharacterCreator {

    private final Player previewPlayer = new Player();

    private final Canvas preview = new Canvas(192, 192);
    private final GraphicsContext gc =
            preview.getGraphicsContext2D();

    private final Label errorLabel = new Label();

    private final TextField nameField =
            new TextField("Hikaru");

    private final ComboBox<String> genderBox =
            new ComboBox<>();

    private final ComboBox<String> skinBox =
            new ComboBox<>();

    private final ComboBox<String> hairBox =
            new ComboBox<>();

    private final ComboBox<String> colorBox =
            new ComboBox<>();

    private final Consumer<CharacterProfile> onStart;

    public CharacterCreator(
            Consumer<CharacterProfile> onStart
    ) {
        this.onStart = onStart;

        configureOptions();
    }

    private void configureOptions() {

        genderBox.getItems().addAll(
                "Male", "Female", "Other"
        );
        genderBox.setValue("Male");

        skinBox.getItems().addAll(
                "pale", "light", "medium", "dark", "darkest"
        );
        skinBox.setValue("pale");

        hairBox.getItems().addAll(
                "short", "medium", "long"
        );
        hairBox.setValue("medium");

        colorBox.getItems().addAll(
                "blonde", "red", "brown",
                "black", "purple"
        );
        colorBox.setValue("purple");

        // Automatically update the preview
        skinBox.setOnAction(e -> updatePreview());
        hairBox.setOnAction(e -> updatePreview());
        colorBox.setOnAction(e -> updatePreview());
    }

    private CharacterProfile buildProfile() {

        return new CharacterBuilder()
                .name(nameField.getText().trim())
                .gender(genderBox.getValue())
                .skinTone(skinBox.getValue())
                .hairstyle(hairBox.getValue())
                .hairColor(colorBox.getValue())
                .build();
    }

    private void updatePreview() {

        try {
            CharacterProfile profile = buildProfile();

            previewPlayer.applyProfile(profile);
            drawPreview();

            errorLabel.setText("");

        } catch (RuntimeException ex) {
            errorLabel.setText(
                    "Preview error: " + ex.getMessage()
            );
        }
    }

    private void drawPreview() {

        gc.clearRect(0, 0, 192, 192);

        gc.save();

        gc.setImageSmoothing(false);

        // 16x32 character becomes 64x128
        gc.scale(4, 4);

        // Center character inside preview
        previewPlayer.renderAt(gc, 16, 8);

        gc.restore();
    }

    public Scene createScene() {

        Label title = new Label(
                "THROUGH THE PARALLELS"
        );

        title.setStyle(
                "-fx-font-size: 30px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: #e4d4f5;"
        );

        Label subtitle = new Label(
                "Create Your Traveler"
        );

        subtitle.setStyle(
                "-fx-font-size: 18px;" +
                        "-fx-text-fill: #c5b7d8;"
        );

        VBox previewPanel = new VBox(
                15, preview
        );

        previewPanel.setAlignment(Pos.CENTER);
        previewPanel.setPrefWidth(320);

        previewPanel.setStyle(
                "-fx-background-color: #363048;" +
                        "-fx-background-radius: 12;"
        );

        VBox form = new VBox(12);

        form.getChildren().addAll(
                field("Name", nameField),
                field("Gender", genderBox),
                field("Skin Tone", skinBox),
                field("Hairstyle", hairBox),
                field("Hair Color", colorBox)
        );

        nameField.setMaxWidth(Double.MAX_VALUE);

        genderBox.setMaxWidth(Double.MAX_VALUE);
        skinBox.setMaxWidth(Double.MAX_VALUE);
        hairBox.setMaxWidth(Double.MAX_VALUE);
        colorBox.setMaxWidth(Double.MAX_VALUE);

        errorLabel.setStyle(
                "-fx-text-fill: #ff9b9b;"
        );
        errorLabel.setWrapText(true);

        Button startButton = new Button(
                "START GAME"
        );

        startButton.setMaxWidth(Double.MAX_VALUE);

        startButton.setStyle(
                "-fx-background-color: #8064ae;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 16px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 12px;"
        );

        startButton.setOnAction(e -> {
            try {
                CharacterProfile profile = buildProfile();

                // Check that the selected sprites exist
                previewPlayer.applyProfile(profile);

                onStart.accept(profile);

            } catch (RuntimeException ex) {
                errorLabel.setText(ex.getMessage());
            }
        });

        form.getChildren().addAll(
                errorLabel,
                startButton
        );

        HBox content = new HBox(
                30, previewPanel, form
        );

        content.setAlignment(Pos.CENTER);
        HBox.setHgrow(form, Priority.ALWAYS);

        VBox root = new VBox(
                25, title, subtitle, content
        );

        root.setPadding(new Insets(45));
        root.setAlignment(Pos.TOP_CENTER);

        root.setStyle(
                "-fx-background-color: #252238;"
        );

        drawPreview();

        return new Scene(root, 960, 720);
    }

    private VBox field(
            String labelText,
            Control control
    ) {

        Label label = new Label(labelText);

        label.setStyle(
                "-fx-text-fill: #e4d4f5;" +
                        "-fx-font-size: 14px;"
        );

        return new VBox(5, label, control);
    }
}
