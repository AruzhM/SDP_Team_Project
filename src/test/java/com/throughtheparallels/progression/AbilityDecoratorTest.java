package com.throughtheparallels.progression;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AbilityDecoratorTest {
    private void assertStats(Ability ability, int damage, double range, double cooldown) {
        assertEquals(damage, ability.getDamage());
        assertEquals(range, ability.getRange(), 1e-9);
        assertEquals(cooldown, ability.getCooldown(), 1e-9);
    }

    @Test void basicPulseHasNoBonuses() { assertStats(new BasicAbility(), 10, 36, 0.8); }
    @Test void powerOnlyChangesDamage() { assertStats(new PowerAbility(new BasicAbility()), 20, 36, 0.8); }
    @Test void reachOnlyChangesRadius() { assertStats(new ReachAbility(new BasicAbility()), 10, 52, 0.8); }
    @Test void swiftnessOnlyChangesCooldown() { assertStats(new SwiftnessAbility(new BasicAbility()), 10, 36, 0.52); }

    @Test void wrappersComposeWithoutMutatingTheBase() {
        Ability base = new BasicAbility();
        Ability upgraded = new ReachAbility(new SwiftnessAbility(new PowerAbility(base)));
        assertStats(upgraded, 20, 52, 0.52);
        assertStats(base, 10, 36, 0.8);
        assertEquals("Crystal pulse + POWER + SWIFTNESS + REACH", upgraded.getName());
    }

    @Test void currentIndependentBonusesCanBeReordered() {
        assertStats(new PowerAbility(new ReachAbility(new SwiftnessAbility(new BasicAbility()))), 20, 52, 0.52);
    }

    @Test void rejectsNullComponents() {
        assertThrows(NullPointerException.class, () -> new PowerAbility(null));
        assertThrows(NullPointerException.class, () -> new ReachAbility(null));
        assertThrows(NullPointerException.class, () -> new SwiftnessAbility(null));
        assertThrows(NullPointerException.class, () -> CrystalAbility.fromQuests(null));
    }

    @Test void legacyApiDelegatesToTheSameBonuses() {
        Ability legacy = new CrystalAbility(new CrystalAbility(new CrystalAbility(new BasicAbility(),
                CrystalAbility.Upgrade.POWER), CrystalAbility.Upgrade.REACH), CrystalAbility.Upgrade.SWIFTNESS);
        assertStats(legacy, 20, 52, 0.52);
        assertThrows(NullPointerException.class, () -> new CrystalAbility(new BasicAbility(), null));
    }

    @Test void questEventsUnlockBonusesInStoryOrderWithoutStacking() {
        GameEventPublisher events = new GameEventPublisher();
        try (QuestManager quests = new QuestManager(events)) {
            assertStats(CrystalAbility.fromQuests(quests), 10, 36, 0.8);
            events.publish(GameEventPublisher.Type.HIKARI_TALKED, "hikari");
            assertStats(CrystalAbility.fromQuests(quests), 10, 36, 0.8);
            events.publish(GameEventPublisher.Type.DESK_EXAMINED, "desk");
            assertStats(CrystalAbility.fromQuests(quests), 10, 36, 0.52);
            events.publish(GameEventPublisher.Type.CRYSTAL_COLLECTED, "crystal");
            assertStats(CrystalAbility.fromQuests(quests), 20, 36, 0.52);
            events.publish(GameEventPublisher.Type.HIKARI_TALKED, "hikari");
            events.publish(GameEventPublisher.Type.HIKARI_TALKED, "hikari");
            for (int i = 0; i < 5; i++) assertStats(CrystalAbility.fromQuests(quests), 20, 52, 0.52);
            quests.reset();
            assertStats(CrystalAbility.fromQuests(quests), 10, 36, 0.8);
        }
    }
}
