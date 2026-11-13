package com.throughtheparallels;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Player {

    private double x = 80;
    private double y = 80;

    private final Image sprite;

    public Player() {
        var url = getClass().getResource(
                "/assets/characters/base/player.png"
        );

        if (url == null) {
            throw new IllegalStateException(
                    "Player sprite not found!"
            );
        }

        System.out.println("Sprite found: " + url);

        sprite = new Image(url.toExternalForm());

        System.out.println(
                "Sprite size: " +
                        sprite.getWidth() + "x" +
                        sprite.getHeight()
        );

        if (sprite.isError()) {
            throw new IllegalStateException(
                    "Player sprite failed to load",
                    sprite.getException()
            );
        }
    }


    public void move(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public void render(GraphicsContext gc) {
        gc.drawImage(sprite, x, y, 16, 32);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
