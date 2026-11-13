package com.throughtheparallels;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import java.util.Objects;

public class RoomRenderer {

    private static final int WIDTH = 176;
    private static final int HEIGHT = 120;
    private static final int SCALE = 3;

    private static final int crystalX = 0;
    private static final int crystalY = 48;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Image tileset;
    private final Image crystal;

    private final Player player = new Player();


    public RoomRenderer() {

        canvas = new Canvas(
                WIDTH * SCALE,
                HEIGHT * SCALE
        );

        gc = canvas.getGraphicsContext2D();

        gc.setImageSmoothing(false);
        gc.scale(SCALE, SCALE);


        var imageUrl = getClass().getResource(
                "/assets/tiles/tileset.png"
        );

        if (imageUrl == null) {
            throw new IllegalStateException(
                    "ERROR: Cannot find tileset.png! " +
                            "Check src/main/resources/assets/tiles/"
            );
        }

        tileset = new Image(imageUrl.toExternalForm());

        if (tileset.isError()) {
            throw new IllegalStateException(
                    "ERROR: Could not load the tileset image",
                    tileset.getException()
            );
        }

        crystal = new Image(
                Objects.requireNonNull(
                        getClass().getResource(
                                "/assets/objects/crystal_object.png"
                        ),
                        "Missing crystal image"
                ).toExternalForm()
        );

    }

    private void drawSprite(
            int sourceX, int sourceY,
            int sourceWidth, int sourceHeight,
            int x, int y) {

        gc.drawImage(
                tileset,
                sourceX, sourceY,
                sourceWidth, sourceHeight,
                x, y,
                sourceWidth, sourceHeight
        );
    }


    public void render() {
        player.render(gc);

        // Clear the previous frame
        gc.clearRect(0, 0, WIDTH, HEIGHT);

        // FLOOR
        for (int y = 32; y < HEIGHT; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {
                drawSprite(0, 0, 16, 16, x, y);
            }
        }

        // BACK WALL
        for (int y = 0; y < 48; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {
                drawSprite(32, 0, 16, 16, x, y);
            }
        }

        // Rug first, underneath furniture
        drawSprite(32, 32, 32, 32, 16, 64);

        // Bed
        drawSprite(0, 32, 32, 16, 16, 48);

        // Desk
        drawSprite(32, 16, 32, 16, 64, 48);

        // Door
        drawSprite(0, 16, 16, 16, 120, 32);
        drawSprite(16, 16, 16, 16, 120, 16);

        //bedroom window
        drawSprite(32, 64, 16, 16, 48, 16);

        //bedside table
        drawSprite(48, 64, 16, 16, 0, 48);

        //drawer
        drawSprite(0, 64, 32, 32, 136, 32);

        //pillow
        drawSprite(32, 80, 16, 16, 64, 64);

        //little rug
        drawSprite(48, 80, 16, 16, 120, 48);

        // Crystal on top of the desk
        if (crystalVisible) {
            gc.drawImage(crystal, crystalX, crystalY, 16, 16);
        }



        player.render(gc);



    }

    public Canvas getCanvas() {
        return canvas;
    }



    public void update(
            double deltaTime,
            double dx,
            double dy
    ) {
        player.update(
                deltaTime,
                dx,
                dy,
                WIDTH,
                HEIGHT
        );

        render();
    }

    public void setCharacter(CharacterProfile profile) {
        player.applyProfile(profile);
        render();
    }

    public boolean isPlayerNearCrystal() {
        double playerX = player.getX();
        double playerY = player.getY();

        double dx = Math.abs(playerX - crystalX);
        double dy = Math.abs(playerY - crystalY);

        return dx < 20 && dy < 20;
    }

    public boolean isPlayerNearDoor() {
        double playerX = player.getX();
        double playerY = player.getY();

        double doorX = 120;
        double doorY = 32;

        double dx = Math.abs(playerX - doorX);
        double dy = Math.abs(playerY - doorY);

        return dx < 20 && dy < 20;
    }

    private boolean crystalVisible = true;

    public void hideCrystal() {
        crystalVisible = false;
        render();
    }

}
