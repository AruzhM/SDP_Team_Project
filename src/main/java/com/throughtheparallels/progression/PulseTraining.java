package com.throughtheparallels.progression;

import java.util.Objects;

/** GUI-independent consumer of Ability. It knows nothing about concrete decorators. */
public final class PulseTraining {
    public static final int MAX_HEALTH = 60;
    public enum Status { HIT, DESTROYED, OUT_OF_RANGE, MISSED, COOLDOWN, ALREADY_DESTROYED }
    public record Result(Status status, int damage, int remainingHealth) {
        public boolean fired() {
            return status == Status.HIT || status == Status.DESTROYED
                    || status == Status.OUT_OF_RANGE || status == Status.MISSED;
        }
    }

    private int health = MAX_HEALTH;
    private double remainingCooldown;

    public Result fire(Ability ability, double distance) {
        return fire(ability, distance, true);
    }

    public Result fire(Ability ability, double distance, boolean targetInWavePath) {
        Objects.requireNonNull(ability, "Ability");
        requireNonNegativeFinite(distance, "Distance");
        if (health == 0) return new Result(Status.ALREADY_DESTROYED, 0, health);
        if (remainingCooldown > 0) return new Result(Status.COOLDOWN, 0, health);

        // Snapshot and validate before changing state; also supports future decorators.
        int damage = ability.getDamage();
        double range = ability.getRange();
        double cooldown = ability.getCooldown();
        if (damage <= 0) throw new IllegalArgumentException("Damage must be positive");
        requireNonNegativeFinite(range, "Range");
        requireNonNegativeFinite(cooldown, "Cooldown");
        remainingCooldown = cooldown;
        if (distance > range) return new Result(Status.OUT_OF_RANGE, 0, health);
        if (!targetInWavePath) return new Result(Status.MISSED, 0, health);
        int appliedDamage = Math.min(health, damage);
        health -= appliedDamage;
        return new Result(health == 0 ? Status.DESTROYED : Status.HIT, appliedDamage, health);
    }

    public void update(double deltaTime) {
        requireNonNegativeFinite(deltaTime, "Frame duration");
        remainingCooldown = Math.max(0, remainingCooldown - deltaTime);
    }

    /** A practice reset must not let the player bypass the current cooldown. */
    public void resetTarget() { health = MAX_HEALTH; }

    /** New games and loads start a fresh, temporary training session. */
    public void resetSession() { health = MAX_HEALTH; remainingCooldown = 0; }

    public int getHealth() { return health; }
    public double getRemainingCooldown() { return remainingCooldown; }

    private static void requireNonNegativeFinite(double value, String name) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(name + " must be finite and non-negative");
        }
    }
}
