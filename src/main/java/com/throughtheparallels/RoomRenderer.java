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

    private static final int EXIT_X = 120;
    private static final int EXIT_Y = HEIGHT - 16;
    private static final int EXIT_SIZE = 16;
    private static final int EXIT_HEIGHT = 16;
    private static final int EXIT_WIDTH = 32;


    private WorldState worldState = WorldState.NORMAL;

    private double transitionTime = 0;

    private static final double TRANSITION_DURATION = 1.5;

    private final Image[] crystalFrames = new Image[6];



    private final Canvas canvas;
    private final GraphicsContext gc;
    private final Image tileset;
    private final Image crystal;

    private RoomType currentRoom = RoomType.BEDROOM;

    private final Image hallwayTileset;


    private final Player player = new Player();


    public RoomRenderer() {

        for (int i = 0; i < crystalFrames.length; i++) {

            String path = "/assets/effects/crystal - "
                    + (i + 1) + ".png";

            var url = getClass().getResource(path);

            if (url == null) {
                throw new IllegalStateException(
                        "Missing crystal frame: " + path
                );
            }

            crystalFrames[i] = new Image(url.toExternalForm());

            if (crystalFrames[i].isError()) {
                throw new IllegalStateException(
                        "Cannot load crystal frame: " + path,
                        crystalFrames[i].getException()
                );
            }
        }


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


        var hallwayUrl = getClass().getResource(
                "/assets/tiles/tileset.png"
        );

        if (hallwayUrl == null) {
            throw new IllegalStateException(
                    "Missing tileset.png"
            );
        }

        hallwayTileset = new Image(
                hallwayUrl.toExternalForm()
        );

        if (hallwayTileset.isError()) {
            throw new IllegalStateException(
                    "Cannot load hallway tileset",
                    hallwayTileset.getException()
            );
        }


    }

    private void renderCrystalEffect() {

        if (worldState != WorldState.TRANSFORMING) {
            return;
        }

        // Six frames over 1.5 seconds
        int frame = Math.min(
                5,
                (int) (transitionTime / 0.25)
        );

        // Center the crystal above the player's head
        double effectX = player.getX();
        double effectY = player.getY() - 12;

        gc.drawImage(
                crystalFrames[frame],
                effectX,
                effectY,
                16,
                16
        );
    }



    private boolean isParallelWorld() {
        return worldState == WorldState.PARALLEL;
    }

    private void drawWorldTile(
            int normalX, int normalY,
            int parallelX, int parallelY,
            int x, int y
    ) {
        int sourceX = isParallelWorld()
                ? parallelX : normalX;

        int sourceY = isParallelWorld()
                ? parallelY : normalY;

        drawSprite(
                sourceX, sourceY,
                16, 16,
                x, y
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


    private final Image hikariSprite = new Image(
            java.util.Objects.requireNonNull(
                    getClass().getResource(
                            "/assets/characters/npc/hikari_sprite.png"
                    ),
                    "Missing Hikari sprite"
            ).toExternalForm()
    );

    private static final int HIKARI_X = 64;
    private static final int HIKARI_Y = 80;



    public void renderBedroom(){


        for (int y = 32; y < HEIGHT; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {

                drawWorldTile(
                        0, 0,       // Normal floor
                        16, 0,     // Parallel floor
                        x, y
                );
            }
        }


        // BACK WALL
        for (int y = 0; y < 48; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {
                drawWorldTile(
                        32, 0,       // Normal wallpaper
                        48, 0,     // Parallel wallpaper
                        x, y
                );
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

        if (worldState == WorldState.PARALLEL) {

            gc.drawImage(
                    hikariSprite,
                    HIKARI_X,
                    HIKARI_Y,
                    16, 32
            );
        }

        if (worldState == WorldState.NORMAL) {
            gc.drawImage(
                    crystal,
                    crystalX,
                    crystalY,
                    16, 16
            );
        }


    }



    private void renderHallway() {

        gc.setFill(
                javafx.scene.paint.Color.web("#252238")
        );

        gc.fillRect(
                0, 0, WIDTH, HEIGHT
        );


        // 2. Hallway floor
        // Stop 16 pixels before the bottom of the canvas
        for (int y = 64; y < HEIGHT - EXIT_HEIGHT; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {

                drawSprite(
                        0, 0,       // Floor tile in tileset
                        16, 16,     // Tile dimensions
                        x, y        // Position in room
                );
            }
        }

        // Wallpaper
        for (int y = 0; y < 32; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {
                drawSprite(0, 96, 16, 16, x, y);
            }
        }

        // Wood border at the top of paneling
        for (int x = 0; x < WIDTH; x += 16) {
            drawSprite(32, 96, 16, 16, x, 32);
        }

        // Wooden wall paneling
        for (int y = 48; y < 64; y += 16) {
            for (int x = 0; x < WIDTH; x += 16) {
                drawSprite(16, 96, 16, 16, x, y);
            }
        }

        // Painting
        drawSprite(
                48, 96, 16, 16,
                WIDTH / 2 - 8, 16
        );

        //window 1
        drawSprite(32, 64, 16, 16, 32, 16);

        //window 2
        drawSprite(32, 64, 16, 16, 144, 16);



        gc.setFill(
                javafx.scene.paint.Color.web("#0e0b13")
        );

        // Black doorway, extending downward
        gc.fillRect(
                EXIT_X,
                EXIT_Y - 8,
                EXIT_WIDTH,
                EXIT_HEIGHT + 16
        );


    }


    public void render() {

        gc.clearRect(0, 0, WIDTH, HEIGHT);

        if (currentRoom == RoomType.BEDROOM) {
            renderBedroom();
        } else {
            renderHallway();
        }

        // Draw the player
        player.render(gc);

        // Draw the crystal effect over the player
        renderCrystalEffect();
    }


    public Canvas getCanvas() {
        return canvas;
    }




    public void update(
            double deltaTime,
            double dx,
            double dy
    ) {

        if (worldState == WorldState.TRANSFORMING) {

            transitionTime += deltaTime;

            // Four frames across the transition
            int animationFrame = Math.min(
                    3,
                    (int) (transitionTime / 0.25)
            );

            player.setSpecialFrame(animationFrame);

            if (transitionTime >= TRANSITION_DURATION) {

                worldState = WorldState.PARALLEL;

                player.stopSpecialAnimation();

                // Hikari will now be drawn
            }

            // Do not move while transforming
            render();
            return;
        }

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

    private void drawTileFrom(
            Image image,
            int sourceX,
            int sourceY,
            int x,
            int y
    ) {
        gc.drawImage(
                image,
                sourceX, sourceY, 16, 16,
                x, y, 16, 16
        );
    }


    public RoomType getCurrentRoom() {
        return currentRoom;
    }



    public void changeRoom(RoomType newRoom) {

        currentRoom = newRoom;

        if (newRoom == RoomType.HALLWAY) {

            // Just inside the hallway's bottom entrance
            player.setPosition(
                    EXIT_X + (EXIT_WIDTH - 16) / 2.0,
                    EXIT_Y - 32
            );

            // Face up, into the hallway
            player.setDirection(3);

        } else if (newRoom == RoomType.BEDROOM) {

            // Return to bedroom, just below the door
            player.setPosition(
                    120,
                    32
            );

            // Face down, into the room
            player.setDirection(0);
        }

        render();
    }

    public boolean isPlayerNearHallwayExit() {

        double playerCenterX = player.getX() + 8;
        double playerFeetY = player.getY() + 32;

        boolean insideEntranceX =
                playerCenterX >= EXIT_X &&
                        playerCenterX <= EXIT_X + EXIT_WIDTH;

        boolean nearEntranceY =
                playerFeetY >= EXIT_Y - 12;

        return insideEntranceX && nearEntranceY;
    }


    public boolean startCrystalTransition() {

        if (currentRoom != RoomType.BEDROOM ||
                worldState != WorldState.NORMAL ||
                !isPlayerNearCrystal()) {
            return false;
        }

        worldState = WorldState.TRANSFORMING;
        transitionTime = 0;

        player.setDirection(0);
        player.setSpecialFrame(0);

        return true;
    }

    // Progression integration only; the team's transition/animation above is unchanged.
    public WorldState getWorldState() { return worldState; }
    public double getPlayerX() { return player.getX(); }
    public double getPlayerY() { return player.getY(); }
    public int getPlayerDirection() { return player.getDirection(); }

    public boolean isPlayerNearDesk() {
        return currentRoom == RoomType.BEDROOM
                && Math.abs(player.getX() - 72) < 24 && Math.abs(player.getY() - 48) < 20;
    }

    public boolean isHikariVisible() {
        return currentRoom == RoomType.BEDROOM && worldState == WorldState.PARALLEL;
    }

    public boolean isPlayerNearHikari() {
        return isHikariVisible() && Math.hypot(player.getX() - HIKARI_X, player.getY() - HIKARI_Y) < 26;
    }

    /** Loading restores a stable world; an interrupted transition can be activated again. */
    public void restoreProgress(boolean crystalCollected, RoomType savedRoom, double x, double y, int direction) {
        currentRoom = savedRoom;
        worldState = crystalCollected ? WorldState.PARALLEL : WorldState.NORMAL;
        crystalVisible = !crystalCollected;
        transitionTime = 0;
        player.stopSpecialAnimation();
        player.setPosition(x, y);
        player.setDirection(direction);
        render();
    }

}
