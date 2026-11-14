package com.throughtheparallels;

import javafx.scene.canvas.Canvas;

public class GameFacade {

    private final RoomRenderer room;
    private CharacterProfile currentCharacter;
    private boolean gameStarted = false;

    public GameFacade() {
        room = new RoomRenderer();
    }

    public void startGame(CharacterProfile profile) {

        if (profile == null) {
            throw new IllegalArgumentException(
                    "Character profile cannot be null"
            );
        }

        currentCharacter = profile;

        room.setCharacter(profile);

        gameStarted = true;

        System.out.println(
                "Game started for " + profile.getName()
        );
    }

    public void update(
            double deltaTime,
            double dx,
            double dy
    ) {
        if (!gameStarted) {
            return;
        }

        room.update(deltaTime, dx, dy);
    }

    public Canvas getGameView() {
        return room.getCanvas();
    }

    public CharacterProfile getCurrentCharacter() {
        return currentCharacter;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public boolean isPlayerNearCrystal() {
        return room.isPlayerNearCrystal();
    }

    public boolean isPlayerNearDoor() {
        return room.isPlayerNearDoor();
    }

    //demo interactions
    private boolean hasCrystal = false;


    public String interactWithCrystal() {

        if (hasCrystal) {
            return "The crystal was there when I woke up. I don't remember putting it there...";
        }

        hasCrystal = true;

        return "I found some glowing crystal. I wonder who left it here...";
    }

    public boolean hasCrystal() {
        return hasCrystal;
    }


    public boolean interactWithDoor() {

        if (!gameStarted) {
            return false;
        }

        if (room.getCurrentRoom() == RoomType.BEDROOM) {


            if (room.isPlayerNearDoor()) {
                room.changeRoom(RoomType.HALLWAY);
                return true;
            }

        } else {

            if (room.isPlayerNearHallwayExit()) {
                room.changeRoom(RoomType.BEDROOM);
                return true;
            }
        }

        return false;
    }

    public boolean activateCrystal() {

        if (!gameStarted) {
            return false;
        }

        boolean activated = room.startCrystalTransition();

        if (activated) {
            System.out.println(
                    "Crystal activated! The room seems to change..."
            );
        }

        return activated;
    }

    public boolean canTalkToHikari() {

        if (!gameStarted) {
            return false;
        }

        return room.isPlayerNearHikari();
    }




}
