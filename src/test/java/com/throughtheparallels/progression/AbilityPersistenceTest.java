package com.throughtheparallels.progression;

import com.throughtheparallels.CharacterBuilder;
import com.throughtheparallels.RoomType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class AbilityPersistenceTest {
    @TempDir Path directory;

    @Test void reloadingQuestSaveRebuildsExactlyOneOfEachEarnedUpgrade() throws Exception {
        GameEventPublisher events = new GameEventPublisher();
        try (QuestManager quests = new QuestManager(events)) {
            events.publish(GameEventPublisher.Type.DESK_EXAMINED, "desk");
            events.publish(GameEventPublisher.Type.CRYSTAL_COLLECTED, "crystal");
            events.publish(GameEventPublisher.Type.HIKARI_TALKED, "hikari");
            SaveManager saves = new SaveManager(directory.resolve("save.properties"));
            saves.save(new SaveManager.SaveData(new CharacterBuilder().name("Decorator demo").build(),
                    80, 80, 0, quests.snapshot(), SaveManager.Ending.UNDECIDED, RoomType.BEDROOM));
            for (int i = 0; i < 3; i++) {
                quests.restore(saves.load().quests());
                Ability ability = CrystalAbility.fromQuests(quests);
                assertEquals(20, ability.getDamage());
                assertEquals(52, ability.getRange());
                assertEquals(0.52, ability.getCooldown(), 1e-9);
            }
        }
    }
}
