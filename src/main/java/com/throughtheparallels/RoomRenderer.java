package com.throughtheparallels;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
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
    private final Image hikari = new Image(Objects.requireNonNull(
            getClass().getResource("/assets/characters/hikari_sprite.png")).toExternalForm());
    private static final double HIKARI_X = 112;
    private static final double HIKARI_Y = 56;
    private boolean hikariVisible;

    private final Player player = new Player();
    private boolean doorLocked = true;
    private boolean doorOpen;
    private double pulseRemaining;
    private double pulseX;
    private double pulseY;
    private double pulseRange;
    private int pulseDirection;


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

        if (doorOpen) {
            gc.setFill(Color.web("#151225"));
            gc.fillRect(120, 24, 16, 24);
        }

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
        if (crystalVisible && !player.isLiftingCrystal()) {
            gc.drawImage(crystal, crystalX, crystalY, 16, 16);
        }



        if (hikariVisible) {
            gc.drawImage(hikari, HIKARI_X, HIKARI_Y, 16, 32);
            gc.setFill(Color.web("#dcd0ff"));
            gc.setFont(javafx.scene.text.Font.font(5));
            gc.fillText("Hikari", HIKARI_X, HIKARI_Y - 3);
        }
        player.render(gc);
        if (player.isLiftingCrystal()) {
            // Rise for 0.35s, then hold above the head with the fifth-row pose.
            double rise = Math.min(1, player.getCrystalLiftProgress() * Player.CRYSTAL_LIFT_DURATION / 0.35);
            double liftY = player.getY() + 1 - 15 * rise;
            gc.setFill(Color.rgb(160, 220, 255, 0.18));
            gc.fillOval(player.getX() - 4, liftY - 4, 24, 24);
            gc.drawImage(crystal, player.getX(), liftY, 16, 16);
        }
        if (pulseRemaining > 0) {
            gc.setStroke(Color.rgb(139, 226, 255, Math.min(1, pulseRemaining / 0.22)));
            gc.setLineWidth(2);
            double angle = switch (pulseDirection) {
                case 1 -> 180;
                case 2 -> 0;
                case 3 -> 90;
                default -> 270;
            };
            gc.strokeArc(pulseX - pulseRange, pulseY - pulseRange, pulseRange * 2, pulseRange * 2,
                    angle - 45, 90, javafx.scene.shape.ArcType.OPEN);
        }
    }

    public Canvas getCanvas() {
        return canvas;
    }



    public void update(
            double deltaTime,
            double dx,
            double dy
    ) {
        pulseRemaining = Math.max(0, pulseRemaining - deltaTime);
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

    public boolean isPlayerNearDesk() {
        return Math.abs(player.getX() - 72) < 24 && Math.abs(player.getY() - 48) < 20;
    }

    public boolean isPlayerNearHikari() {
        return hikariVisible && Math.hypot(player.getX() - HIKARI_X, player.getY() - HIKARI_Y) < 26;
    }

    public boolean isHikariVisible() { return hikariVisible; }
    public void beginCrystalLift() { player.beginCrystalLift(); render(); }
    public boolean isLiftingCrystal() { return player.isLiftingCrystal(); }

    public double getPlayerX() { return player.getX(); }
    public double getPlayerY() { return player.getY(); }
    public int getPlayerDirection() { return player.getDirection(); }

    public void restorePlayer(double x, double y, int direction) {
        player.restorePosition(x, y, direction);
        pulseRemaining = 0;
    }

    public void setWorldState(boolean hasCrystal, boolean sealBroken, boolean doorOpen, boolean hikariVisible) {
        this.hikariVisible = hikariVisible;
        crystalVisible = !hasCrystal;
        doorLocked = !sealBroken;
        this.doorOpen = doorOpen;
        render();
    }

    /** A 90-degree forward cone centered on the character; the seal is at (128, 40). */
    public boolean canHitSeal(double range) {
        double dx = 128 - (player.getX() + 8);
        double dy = 40 - (player.getY() + 16);
        double distance = Math.hypot(dx, dy);
        double forward = switch (player.getDirection()) {
            case 1 -> -dx;
            case 2 -> dx;
            case 3 -> -dy;
            default -> dy;
        };
        return doorLocked && distance <= range && (distance == 0 || forward / distance >= Math.sqrt(0.5));
    }

    public void showPulse(double range) {
        pulseRemaining = 0.22;
        pulseX = player.getX() + 8;
        pulseY = player.getY() + 16;
        pulseRange = range;
        pulseDirection = player.getDirection();
        render();
    }

}
