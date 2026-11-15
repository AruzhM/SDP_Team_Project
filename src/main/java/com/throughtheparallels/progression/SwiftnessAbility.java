package com.throughtheparallels.progression;

/** Reduces the delay between pulses to 65% of the wrapped cooldown. */
public final class SwiftnessAbility extends AbilityDecorator {
    public SwiftnessAbility(Ability wrapped) { super(wrapped); }

    @Override public String getName() { return super.getName() + " + SWIFTNESS"; }
    @Override public double getCooldown() { return super.getCooldown() * 0.65; }
}
