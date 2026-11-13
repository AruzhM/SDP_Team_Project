package com.throughtheparallels;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class RoomRenderer {

    //private static final int TILE = 16;
    private static final int WIDTH = 160;
    private static final int HEIGHT = 120;
    private static final int SCALE = 3;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Image tileset;

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


        player.render(gc);



    }

    public Canvas getCanvas() {
        return canvas;
    }


    public void movePlayer(double dx, double dy) {

        double newX = player.getX() + dx;
        double newY = player.getY() + dy;

        // Keep the character inside the room
        if (newX < 0 || newX > WIDTH - 16) {
            return;
        }

        if (newY < 32 || newY > HEIGHT - 32) {
            return;
        }

        player.move(dx, dy);
        render();
    }

}
