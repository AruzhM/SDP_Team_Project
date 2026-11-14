package com.throughtheparallels;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

import java.util.Objects;

public class Player {

    private static final int FRAME_WIDTH = 16;
    private static final int FRAME_HEIGHT = 32;

    private static final double SPEED = 65.0;
    private static final double FRAME_DURATION = 0.15;

    // 0 = Down, 1 = Left,
    // 2 = Right, 3 = Up
    private int direction = 0;
    private int frame = 0;

    private double x = 80;
    private double y = 80;

    private double animationTimer = 0;
    private boolean moving = false;
    public static final double CRYSTAL_LIFT_DURATION = 1.6;
    private double crystalLiftRemaining;

    public void beginCrystalLift() {
        crystalLiftRemaining = CRYSTAL_LIFT_DURATION;
        direction = 0;
        frame = 0;
        animationTimer = 0;
        moving = false;
    }

    public boolean isLiftingCrystal() { return crystalLiftRemaining > 0; }
    public double getCrystalLiftProgress() { return 1 - crystalLiftRemaining / CRYSTAL_LIFT_DURATION; }
    public int getSpriteSourceX() { return isLiftingCrystal() ? 0 : frame * FRAME_WIDTH; }
    public int getSpriteSourceY() { return isLiftingCrystal() ? 128 : direction * FRAME_HEIGHT; }

    // Head shifts down on walking frames 1 and 3
    private static final int[] HAIR_OFFSETS = {
            0, 1, 0, 1
    };

    private Image bodySheet;
    private final Image[] hairSprites = new Image[4];

    public Player() {

        CharacterProfile profile = new CharacterBuilder()
                .name("Hikaru")
                .gender("Male")
                .skinTone("pale")
                .hairstyle("medium")
                .hairColor("purple")
                .build();

        applyProfile(profile);
    }


    private Image loadImage(String path) {
        var url = Objects.requireNonNull(
                getClass().getResource(path),
                "Missing image: " + path
        );

        Image image = new Image(url.toExternalForm());

        if (image.isError()) {
            throw new IllegalStateException(
                    "Cannot load image: " + path,
                    image.getException()
            );
        }

        return image;
    }


    public void setHair(String hairName) {

        String[] directions = {
                "down", "left", "right", "up"
        };

        for (int i = 0; i < directions.length; i++) {

            String path =
                    "/assets/characters/hair/" +
                            hairName +
                            "_hair-" +
                            directions[i] +
                            ".png";

            hairSprites[i] = loadImage(path);
        }
    }


    public void update(
            double deltaTime,
            double dx,
            double dy,
            double roomWidth,
            double roomHeight
    ) {

        if (isLiftingCrystal()) {
            crystalLiftRemaining = Math.max(0, crystalLiftRemaining - deltaTime);
            return;
        }
        moving = dx != 0 || dy != 0;

        if (!moving) {
            frame = 0;
            animationTimer = 0;
            return;
        }

        // Face the direction being walked
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx < 0 ? 1 : 2;
        } else {
            direction = dy < 0 ? 3 : 0;
        }

        // Normalize diagonal movement
        double length = Math.sqrt(dx * dx + dy * dy);

        dx /= length;
        dy /= length;

        double newX = x + dx * SPEED * deltaTime;
        double newY = y + dy * SPEED * deltaTime;

        // Keep the character inside the room
        x = Math.max(
                0,
                Math.min(newX, roomWidth - FRAME_WIDTH)
        );

        y = Math.max(
                32,
                Math.min(newY, roomHeight - FRAME_HEIGHT)
        );

        animationTimer += deltaTime;

        while (animationTimer >= FRAME_DURATION) {
            frame = (frame + 1) % 4;
            animationTimer -= FRAME_DURATION;
        }
    }


    public void render(GraphicsContext gc) {
        renderAt(gc, x, y);
    }

    public void renderAt(
            GraphicsContext gc,
            double drawX,
            double drawY
    ) {
        int sourceX = getSpriteSourceX();
        int sourceY = getSpriteSourceY();

        // Body
        gc.drawImage(
                bodySheet,
                sourceX, sourceY,
                FRAME_WIDTH, FRAME_HEIGHT,
                drawX, drawY,
                FRAME_WIDTH, FRAME_HEIGHT
        );

        // Hair moves with the head
        int hairOffsetY = moving
                ? HAIR_OFFSETS[frame]
                : 0;

        gc.drawImage(
                hairSprites[direction],
                drawX,
                drawY + hairOffsetY,
                FRAME_WIDTH,
                FRAME_HEIGHT
        );
    }


    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getDirection() { return direction; }

    public void restorePosition(double x, double y, int direction) {
        this.x = x;
        this.y = y;
        this.direction = direction;
        crystalLiftRemaining = 0;
        frame = 0;
        animationTimer = 0;
        moving = false;
    }


    public void applyProfile(CharacterProfile profile) {

        String skinPath =
                "/assets/characters/base/" +
                        "player_" +
                        profile.getSkinTone() +
                        ".png";

        bodySheet = loadImage(skinPath);

        String hairName =
                profile.getHairstyle() +
                        "_" +
                        profile.getHairColor();

        setHair(hairName);
    }

}
