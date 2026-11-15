package com.throughtheparallels.progression;

/** Adds damage while preserving all other behavior of the wrapped ability. */
public final class PowerAbility extends AbilityDecorator {
    public PowerAbility(Ability wrapped) { super(wrapped); }

    @Override public String getName() { return super.getName() + " + POWER"; }
    @Override public int getDamage() { return super.getDamage() + 10; }
}
