package com.throughtheparallels.story;

public class CrystalEvent implements GameEvent {

    @Override
    public String getDescription() {
        return "Pick up the mysterious crystal";
    }

    @Override
    public void execute() {
        System.out.println("Why is it... warm?");
    }
}
