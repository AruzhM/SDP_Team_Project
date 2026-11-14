package com.throughtheparallels.progression;

public final class BasicAbility implements Ability {
    @Override public String getName() { return "Crystal pulse"; }
    @Override public int getDamage() { return 10; }
    @Override public double getRange() { return 36; }
    @Override public double getCooldown() { return 0.8; }
}
