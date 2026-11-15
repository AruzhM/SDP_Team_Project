package com.throughtheparallels.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.throughtheparallels.progression.PulseTraining.Status.*;

class PulseTrainingTest {
    @Test void powerChangesHitsRequiredFromSixToThree() {
        assertEquals(6, hitsToClear(new BasicAbility()));
        assertEquals(3, hitsToClear(new PowerAbility(new BasicAbility())));
    }

    private int hitsToClear(Ability ability) {
        PulseTraining training = new PulseTraining();
        int hits = 0;
        while (training.getHealth() > 0) {
            assertTrue(training.fire(ability, 0).fired());
            hits++;
            training.update(ability.getCooldown());
        }
        return hits;
    }

    @Test void reachHitsWhereBasicPulseMisses() {
        PulseTraining training = new PulseTraining();
        assertEquals(OUT_OF_RANGE, training.fire(new BasicAbility(), 45).status());
        assertEquals(60, training.getHealth());
        training.update(0.8);
        assertEquals(HIT, training.fire(new ReachAbility(new BasicAbility()), 45).status());
        assertEquals(50, training.getHealth());
    }

    @Test void exactRangeBoundaryIsIncluded() {
        assertEquals(HIT, new PulseTraining().fire(new BasicAbility(), 36).status());
        assertEquals(OUT_OF_RANGE, new PulseTraining().fire(new BasicAbility(), 36.00001).status());
    }

    @Test void swiftnessAllowsAnotherPulseAtPoint52Seconds() {
        Ability fast = new SwiftnessAbility(new BasicAbility());
        PulseTraining training = new PulseTraining();
        training.fire(fast, 0);
        training.update(0.51);
        assertEquals(COOLDOWN, training.fire(fast, 0).status());
        training.update(0.010001);
        assertEquals(HIT, training.fire(fast, 0).status());
        PulseTraining basic = new PulseTraining();
        basic.fire(new BasicAbility(), 0);
        basic.update(0.52);
        assertEquals(COOLDOWN, basic.fire(new BasicAbility(), 0).status());
    }

    @Test void missedPulseStillConsumesCooldown() {
        PulseTraining training = new PulseTraining();
        training.fire(new BasicAbility(), 100);
        assertEquals(COOLDOWN, training.fire(new BasicAbility(), 0).status());
        assertEquals(60, training.getHealth());
    }

    @Test void resetOrSwitchingAbilityDoesNotBypassCooldown() {
        PulseTraining training = new PulseTraining();
        training.fire(new BasicAbility(), 0);
        training.resetTarget();
        assertEquals(60, training.getHealth());
        assertEquals(COOLDOWN, training.fire(new SwiftnessAbility(new BasicAbility()), 0).status());
        training.update(5);
        assertEquals(0, training.getRemainingCooldown());
        assertEquals(HIT, training.fire(new BasicAbility(), 0).status());
    }

    @Test void healthCannotGoBelowZeroAndDestroyedTargetRejectsHits() {
        Ability strong = new BasicAbility();
        for (int i = 0; i < 6; i++) strong = new PowerAbility(strong);
        PulseTraining training = new PulseTraining();
        var result = training.fire(strong, 0);
        assertEquals(DESTROYED, result.status());
        assertEquals(60, result.damage());
        assertEquals(0, result.remainingHealth());
        assertEquals(ALREADY_DESTROYED, training.fire(strong, 0).status());
        training.resetSession();
        assertEquals(60, training.getHealth());
        assertEquals(0, training.getRemainingCooldown());
    }

    @Test void invalidInputsDoNotChangeState() {
        PulseTraining training = new PulseTraining();
        for (double invalid : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> training.fire(new BasicAbility(), invalid));
            assertThrows(IllegalArgumentException.class, () -> training.update(invalid));
        }
        assertThrows(NullPointerException.class, () -> training.fire(null, 0));
        assertEquals(60, training.getHealth());
        assertEquals(0, training.getRemainingCooldown());
    }

    @Test void malformedFutureDecoratorCannotCorruptTrainingState() {
        Ability invalid = new AbilityDecorator(new BasicAbility()) {
            @Override public double getCooldown() { return Double.NaN; }
        };
        PulseTraining training = new PulseTraining();
        assertThrows(IllegalArgumentException.class, () -> training.fire(invalid, 0));
        assertEquals(60, training.getHealth());
        assertEquals(0, training.getRemainingCooldown());
    }
}
