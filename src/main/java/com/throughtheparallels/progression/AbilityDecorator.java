package com.throughtheparallels.progression;

import java.util.Objects;

public abstract class AbilityDecorator implements Ability {
    protected final Ability wrapped;

    protected AbilityDecorator(Ability wrapped) { this.wrapped = Objects.requireNonNull(wrapped); }

    @Override public String getName() { return wrapped.getName(); }
    @Override public int getDamage() { return wrapped.getDamage(); }
    @Override public double getRange() { return wrapped.getRange(); }
    @Override public double getCooldown() { return wrapped.getCooldown(); }
}
