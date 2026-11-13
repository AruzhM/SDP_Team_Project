package com.throughtheparallels;

public class CharacterProfile {

    private final String name;
    private final String gender;
    private final String skinTone;
    private final String hairstyle;
    private final String hairColor;

    CharacterProfile(CharacterBuilder builder) {
        this.name = builder.getName();
        this.gender = builder.getGender();
        this.skinTone = builder.getSkinTone();
        this.hairstyle = builder.getHairstyle();
        this.hairColor = builder.getHairColor();
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getSkinTone() {
        return skinTone;
    }

    public String getHairstyle() {
        return hairstyle;
    }

    public String getHairColor() {
        return hairColor;
    }
}
