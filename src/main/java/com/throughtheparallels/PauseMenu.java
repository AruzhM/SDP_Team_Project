package com.throughtheparallels;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Modal in-game overlay. The facade owns pause state; this view only exposes actions. */
public final class PauseMenu extends StackPane {
    private final Button resume = button("Resume", "pause-resume");
    private final Button save = button("Save", "pause-save");
    private final Button reload = button("Reload", "pause-reload");
    private final Button restart = button("Restart", "pause-restart");
    private final Button quit = button("Quit Game", "pause-quit");
    private final Label feedback = new Label();

    public PauseMenu() {
        setId("pause-menu");
        setStyle("-fx-background-color: rgba(12, 9, 22, 0.78);");
        setPickOnBounds(true);
        Label title = new Label("PAUSED");
        title.setStyle("-fx-text-fill: #e4d4f5; -fx-font-size: 25px; -fx-font-weight: bold;");
        Label hint = new Label("ESC to resume • Progress saves on quit");
        hint.setStyle("-fx-text-fill: #c5b7d8; -fx-font-size: 12px;");
        hint.setWrapText(true);
        feedback.setId("pause-feedback");
        feedback.setWrapText(true);
        feedback.setMaxWidth(Double.MAX_VALUE);
        feedback.setStyle("-fx-text-fill: #f4d59a; -fx-font-size: 13px;");
        VBox panel = new VBox(12, title, hint, resume, save, reload, restart, quit, feedback);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(28));
        panel.setMaxWidth(360);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setStyle("-fx-background-color: #332d47; -fx-background-radius: 16;"
                + " -fx-border-color: #66517e; -fx-border-radius: 16;");
        getChildren().add(panel);
        showFeedback("");
        setOpen(false);
    }

    public void configure(Runnable onResume, Runnable onSave, Runnable onReload, Runnable onRestart, Runnable onQuit) {
        resume.setOnAction(event -> onResume.run());
        save.setOnAction(event -> onSave.run());
        reload.setOnAction(event -> onReload.run());
        restart.setOnAction(event -> onRestart.run());
        quit.setOnAction(event -> onQuit.run());
    }

    public void setOpen(boolean open) {
        setVisible(open);
        setManaged(open);
        if (open) {
            showFeedback("");
            resume.requestFocus();
        }
    }

    public void showFeedback(String text) {
        feedback.setText(text);
        feedback.setVisible(!text.isEmpty());
        feedback.setManaged(!text.isEmpty());
    }

    private Button button(String title, String id) {
        Button button = new Button(title);
        button.setId(id);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle("-fx-background-color: #8064ae; -fx-text-fill: white;"
                + " -fx-font-size: 16px; -fx-padding: 11 18;");
        return button;
    }
}
