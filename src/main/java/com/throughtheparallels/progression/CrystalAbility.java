package com.throughtheparallels.progression;

import java.util.Objects;

/** Concrete decorator: different upgrades can wrap the same ability at runtime. */
public final class CrystalAbility extends AbilityDecorator {
    public enum Upgrade { POWER, REACH, SWIFTNESS }
    private final Upgrade upgrade;

    public CrystalAbility(Ability wrapped, Upgrade upgrade) {
        super(wrapped);
        this.upgrade = Objects.requireNonNull(upgrade);
    }

    @Override public String getName() { return super.getName() + " + " + upgrade; }
    @Override public int getDamage() { return super.getDamage() + (upgrade == Upgrade.POWER ? 10 : 0); }
    @Override public double getRange() { return super.getRange() + (upgrade == Upgrade.REACH ? 16 : 0); }
    @Override public double getCooldown() {
        return upgrade == Upgrade.SWIFTNESS ? super.getCooldown() * 0.65 : super.getCooldown();
    }

    /** Rebuild once from durable quest state, so loading cannot stack rewards repeatedly. */
    public static Ability fromQuests(QuestManager quests) {
        Ability ability = new BasicAbility();
        if (quests.isCompleted(QuestManager.FIND_CRYSTAL)) ability = new CrystalAbility(ability, Upgrade.POWER);
        if (quests.isCompleted(QuestManager.READ_DESK)) ability = new CrystalAbility(ability, Upgrade.SWIFTNESS);
        if (quests.isCompleted(QuestManager.TALK_HIKARI)) ability = new CrystalAbility(ability, Upgrade.REACH);
        return ability;
    }
}
