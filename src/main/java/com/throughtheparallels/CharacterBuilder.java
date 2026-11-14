package com.throughtheparallels;

public class CharacterBuilder {

    private String name = "Hikaru";
    private String gender = "Male";
    private String skinTone = "pale";
    private String hairstyle = "medium";
    private String hairColor = "purple";

    public CharacterBuilder name(String name) {
        this.name = name;
        return this;
    }

    public CharacterBuilder gender(String gender) {
        this.gender = gender;
        return this;
    }

    public CharacterBuilder skinTone(String skinTone) {
        this.skinTone = skinTone;
        return this;
    }

    public CharacterBuilder hairstyle(String hairstyle) {
        this.hairstyle = hairstyle;
        return this;
    }

    public CharacterBuilder hairColor(String hairColor) {
        this.hairColor = hairColor;
        return this;
    }

    public CharacterProfile build() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Give your character a name!"
            );
        }

        if (name.length() > 20) {
            throw new IllegalArgumentException(
                    "Sorry, but your character's name is too long! Please keep it under 20 characters."

            );
        }

        return new CharacterProfile(this);
    }

    String getName() {
        return name;
    }

    String getGender() {
        return gender;
    }

    String getSkinTone() {
        return skinTone;
    }

    String getHairstyle() {
        return hairstyle;
    }

    String getHairColor() {
        return hairColor;
    }
}
