package com.throughtheparallels.progression;

import java.util.Objects;

/**
 * Legacy entry point kept for callers of Upgrade. New chains use focused decorators.
 * The quest recipe below is the composition root, not a concrete decorator's behavior.
 */
public final class CrystalAbility extends AbilityDecorator {
    public enum Upgrade { POWER, REACH, SWIFTNESS }
    public CrystalAbility(Ability wrapped, Upgrade upgrade) {
        super(switch (Objects.requireNonNull(upgrade, "Upgrade")) {
            case POWER -> new PowerAbility(wrapped);
            case REACH -> new ReachAbility(wrapped);
            case SWIFTNESS -> new SwiftnessAbility(wrapped);
        });
    }

    /** Rebuild once from durable quest state, so loading cannot stack rewards repeatedly. */
    public static Ability fromQuests(QuestManager quests) {
        Objects.requireNonNull(quests, "Quests");
        Ability ability = new BasicAbility();
        if (quests.isCompleted(QuestManager.FIND_CRYSTAL)) ability = new PowerAbility(ability);
        if (quests.isCompleted(QuestManager.READ_DESK)) ability = new SwiftnessAbility(ability);
        if (quests.isCompleted(QuestManager.TALK_HIKARI)) ability = new ReachAbility(ability);
        return ability;
    }
}
