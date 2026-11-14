package com.throughtheparallels.progression;

/** Decorator component. Combat consumes these values, regardless of the wrapper chain. */
public interface Ability {
    String getName();
    int getDamage();
    double getRange();
    double getCooldown();
}
