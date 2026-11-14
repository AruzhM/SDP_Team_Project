package com.throughtheparallels.progression;

import com.throughtheparallels.CharacterBuilder;
import com.throughtheparallels.CharacterProfile;
import com.throughtheparallels.RoomType;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** Versioned single-slot save. No Java object deserialization or external libraries. */
public final class SaveManager {
    public enum Ending { UNDECIDED, FOLLOW_HIKARI, STAY_IN_ROOM }
    private final Path path;

    public record SaveData(CharacterProfile profile, double x, double y, int direction,
                           Map<String, Integer> quests, Ending ending, RoomType room) {
        public SaveData {
            Objects.requireNonNull(profile, "Character profile");
            quests = Map.copyOf(quests);
            Objects.requireNonNull(ending, "Ending");
            Objects.requireNonNull(room, "Room");
            if (!Double.isFinite(x) || !Double.isFinite(y) || x < 0 || x > 160 || y < 32 || y > 88
                    || direction < 0 || direction > 3) {
                throw new IllegalArgumentException("Invalid player state");
            }
            validateProfile(profile);
            try (QuestManager manager = new QuestManager(new GameEventPublisher())) {
                manager.restore(quests);
                boolean decided = manager.isCompleted(QuestManager.CROSS_BOUNDARY);
                if (decided != (ending != Ending.UNDECIDED)) {
                    throw new IllegalArgumentException("Inconsistent quest and world state");
                }
            }
        }
    }

    public SaveManager() {
        this(Path.of(System.getProperty("user.home"), ".through-the-parallels", "save.properties"));
    }

    public SaveManager(Path path) { this.path = Objects.requireNonNull(path).toAbsolutePath(); }
    public Path getPath() { return path; }
    public boolean hasSave() { return Files.isRegularFile(path); }

    public void save(SaveData data) throws IOException {
        Properties values = new Properties();
        CharacterProfile profile = data.profile();
        values.setProperty("version", "3");
        values.setProperty("world.room", data.room().name());
        values.setProperty("story.ending", data.ending().name());
        values.setProperty("character.name", profile.getName());
        values.setProperty("character.gender", profile.getGender());
        values.setProperty("character.skin", profile.getSkinTone());
        values.setProperty("character.hair", profile.getHairstyle());
        values.setProperty("character.color", profile.getHairColor());
        values.setProperty("player.x", Double.toString(data.x()));
        values.setProperty("player.y", Double.toString(data.y()));
        values.setProperty("player.direction", Integer.toString(data.direction()));
        data.quests().forEach((id, progress) -> values.setProperty("quest." + id, progress.toString()));

        Files.createDirectories(path.getParent());
        // Keep the original pre-story save when it is first upgraded to version 3.
        if (Files.isRegularFile(path)) {
            Properties previous = new Properties();
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                previous.load(reader);
            } catch (IllegalArgumentException ignored) {
                // Starting a new game may explicitly replace an invalid slot.
            }
            String previousVersion = previous.getProperty("version", "");
            if (Set.of("1", "2").contains(previousVersion)) {
                Path backup = path.resolveSibling(path.getFileName() + ".v" + previousVersion + ".bak");
                if (!Files.exists(backup)) Files.copy(path, backup);
            }
        }
        Path temporary = Files.createTempFile(path.getParent(), "save-", ".tmp");
        try {
            try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                values.store(writer, "Through The Parallels - save version 3");
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public SaveData load() throws IOException {
        Properties values = new Properties();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            values.load(reader);
            String version = required(values, "version");
            if (!Set.of("1", "2", "3").contains(version)) throw new IllegalArgumentException("Unsupported save version");
            CharacterProfile profile = new CharacterBuilder()
                    .name(required(values, "character.name"))
                    .gender(required(values, "character.gender"))
                    .skinTone(required(values, "character.skin"))
                    .hairstyle(required(values, "character.hair"))
                    .hairColor(required(values, "character.color")).build();
            Map<String, Integer> quests = new LinkedHashMap<>();
            for (String key : values.stringPropertyNames()) {
                if (key.startsWith("quest.")) quests.put(key.substring(6), Integer.parseInt(values.getProperty(key)));
            }
            Ending ending = "1".equals(version) ? Ending.UNDECIDED : Ending.valueOf(required(values, "story.ending"));
            if ("1".equals(version)) quests = migrateLegacyQuests(quests);
            return new SaveData(profile,
                    Double.parseDouble(required(values, "player.x")),
                    Double.parseDouble(required(values, "player.y")),
                    Integer.parseInt(required(values, "player.direction")),
                    quests, ending, "3".equals(version) ? RoomType.valueOf(required(values, "world.room")) : RoomType.BEDROOM);
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IOException("Invalid save: " + ex.getMessage(), ex);
        }
    }

    private static Map<String, Integer> migrateLegacyQuests(Map<String, Integer> old) {
        if (!old.keySet().equals(Set.of("read_desk", "find_crystal", "break_seal", "open_door"))
                || old.values().stream().anyMatch(value -> value < 0 || value > 1)) {
            throw new IllegalArgumentException("Invalid legacy quests");
        }
        // Previously the crystal could be collected before reading the desk.
        return Map.of(QuestManager.READ_DESK, Math.max(old.get("read_desk"), old.get("find_crystal")),
                QuestManager.FIND_CRYSTAL, old.get("find_crystal"),
                QuestManager.TALK_HIKARI, 0, QuestManager.CROSS_BOUNDARY, 0);
    }

    private static String required(Properties values, String key) {
        String value = values.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static void validateProfile(CharacterProfile profile) {
        if (profile.getName() == null || profile.getName().isBlank() || profile.getName().length() > 20
                || !Set.of("Male", "Female", "Other").contains(profile.getGender())
                || !Set.of("pale", "light", "medium", "dark", "darkest").contains(profile.getSkinTone())
                || !Set.of("short", "medium", "long").contains(profile.getHairstyle())
                || !Set.of("blonde", "red", "brown", "black", "purple").contains(profile.getHairColor())) {
            throw new IllegalArgumentException("Unknown character appearance");
        }
    }
}
