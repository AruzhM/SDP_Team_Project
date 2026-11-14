package com.throughtheparallels.story;
import java.util.function.Consumer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;


public class DialogueView extends VBox {

    private final ImageView hikariPortrait;
    private final Label speakerLabel;
    private final Label dialogueLabel;
    private final HBox choices;


    private Consumer<InteractionStrategy> onChoice = strategy -> {};

    /** A typed choice lets gameplay update quests and saves without parsing display text. */
    public void setOnChoice(Consumer<InteractionStrategy> callback) {
        onChoice = Objects.requireNonNull(callback);
    }

    private Consumer<String> onOutcome = outcome -> {};
    public void setOnOutcome(Consumer<String> callback) {
        this.onOutcome = Objects.requireNonNull(callback);
    }




    private final DialogueManager manager =
            new DialogueManager(new TrustStrategy());

    public DialogueView() {

        setSpacing(10);
        setPadding(new Insets(15));

        setPrefWidth(600);
        setMaxWidth(600);

        setMinHeight(Region.USE_PREF_SIZE);
        setMaxHeight(Region.USE_PREF_SIZE);

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

        hikariPortrait = new ImageView(
                new Image(
                        Objects.requireNonNull(
                                getClass().getResource(
                                        "/assets/characters/hikari_portrait.png"
                                )
                        ).toExternalForm()
                )
        );

        hikariPortrait.setFitWidth(110);
        hikariPortrait.setFitHeight(110);
        hikariPortrait.setPreserveRatio(true);
        hikariPortrait.setSmooth(false);  

        Button trustButton = new Button("1 — Follow Hikari");
        Button doubtButton = new Button("2 — Stay in the room");

        trustButton.setId("follow-hikari");
        doubtButton.setId("stay-in-room");
        trustButton.setFocusTraversable(false);
        doubtButton.setFocusTraversable(false);
        trustButton.setOnAction(event -> chooseTrust());

        doubtButton.setOnAction(event -> chooseDoubt());

        choices = new HBox(10, trustButton, doubtButton);


        VBox dialogueText = new VBox(
                8,
                speakerLabel,
                dialogueLabel
        );

        HBox dialogueContent = new HBox(
                15,
                hikariPortrait,
                dialogueText
        );

        getChildren().addAll(
                dialogueContent,
                choices
        );

        choices.setVisible(false);
        choices.setManaged(false);
        hideDialogue();
    }

    public void showDialogue(String speaker, String text) {
        speakerLabel.setText(speaker);
        dialogueLabel.setText(text);
        choices.setVisible(false);
        choices.setManaged(false);
        setVisible(true);
        setManaged(true);
    }

    public void showHikariChoice() {
        showDialogue("Hikari", "So, you found it too.");
        choices.setVisible(true);
        choices.setManaged(true);
    }


    public void chooseTrust() { selectStrategy(new TrustStrategy()); }

    public void chooseDoubt() { selectStrategy(new DoubtStrategy()); }

    private void selectStrategy(
            InteractionStrategy strategy
    ) {
        if (!isVisible() || !choices.isVisible() || isDisabled()) return;
        manager.setStrategy(strategy);

        // Display the selected dialogue
        dialogueLabel.setText(manager.getDialogue());

        choices.setVisible(false);
        choices.setManaged(false);

        // Notify the game of the selected outcome
        onOutcome.accept(manager.getOutcome());
        onChoice.accept(strategy);
    }


    public void hideDialogue() {
        setVisible(false);
        setManaged(false);
    }
}
