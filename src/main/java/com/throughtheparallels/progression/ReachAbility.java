package com.throughtheparallels.progression;

/** Extends the pulse radius in logical room pixels, before the renderer's scale. */
public final class ReachAbility extends AbilityDecorator {
    public ReachAbility(Ability wrapped) { super(wrapped); }

    @Override public String getName() { return super.getName() + " + REACH"; }
    @Override public double getRange() { return super.getRange() + 16; }
}
