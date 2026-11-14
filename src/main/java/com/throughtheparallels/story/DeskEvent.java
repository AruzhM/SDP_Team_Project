package com.throughtheparallels.story;

public class DeskEvent implements GameEvent {

    @Override
    public String getDescription() {
        return "Examine the desk";
    }

    @Override
    public void execute() {
        System.out.println(
                "I don't remember leaving this here..."
        );
    }
}
