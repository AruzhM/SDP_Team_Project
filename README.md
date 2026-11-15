# Through The Parallels

**Software Design Patterns — Final Project | Game Lab**

A small, story-driven, top-down pixel-art RPG demo built with **Java 21**, **JavaFX**, and **Maven**. Create a traveler, explore a bedroom, investigate a crystal, meet Hikari in a parallel world, make a dialogue choice, travel between the bedroom and hallway, and try a crystal pulse against a training dummy.

**Repository:** https://github.com/AruzhM/SDP_Team_Project

> This is a short playable demonstration, not a full-length game. Achievement badges are **not** part of the final scope. The repository's original README described a platformer; the actual game uses top-down movement.

## Requirements

- **JDK 21** (the Maven compiler is configured with `release=21`)
- **Maven 3.x** (Maven 3.9.x recommended)
- A desktop operating system with a graphical session, keyboard, and JavaFX-compatible display
- Internet connection on the first build to resolve Maven dependencies

The `pom.xml` specifies **JavaFX Controls 21.0.9**, **JUnit Jupiter 5.10.2**, and the **OpenJFX Maven Plugin 0.0.8**. JavaFX dependencies are handled through Maven; no separate SDK download should be necessary for the standard Maven launch.

## Getting started

```bash
git clone https://github.com/AruzhM/SDP_Team_Project.git
cd SDP_Team_Project
mvn clean javafx:run
```

Or open the project folder in IntelliJ IDEA, select JDK 21, reload Maven dependencies, and run the `javafx:run` Maven goal. The `pom.xml` launches `com.throughtheparallels.Launcher` (do not directly replace it with the `Main` JavaFX subclass).

## How to play

1. **New Game**: enter a name and choose gender, skin tone, hairstyle, and hair color. A sample character is prefilled in the creator.
2. Move with **W/A/S/D**. Use **E** to examine the desk and crystal, talk to Hikari, and use doors.
3. Follow quest objectives. After the crystal sequence, the room changes to its parallel-world form and Hikari appears.
4. Choose a dialogue outcome: follow/trust Hikari or stay/doubt Hikari. The Trust route can lead toward the hallway conversation.
5. In the hallway, use **Q** to fire a crystal pulse toward the training dummy. Direction, distance, and cooldown matter. Use **R** to reset the dummy. The optional **Basic pulse comparison** toggle demonstrates Decorator effects.
6. Use **F5** to save, **F9** to reload, and **Esc** to access pause/menu controls. A Continue option is available when a save exists.

Some interactions depend on completing earlier quests. The game displays messages for incorrect timing, distance, and other invalid actions.

### Controls

| Key / action | Effect |
|---|---|
| W, A, S, D | Move in four directions |
| E | Interact with nearby object, doorway, or NPC |
| Q | Fire crystal pulse during hallway training |
| R | Reset training target when training is available |
| 1 / 2 | Select dialogue choice when offered |
| Enter / on-screen Continue | Advance applicable Hikari dialogue |
| F5 / F9 | Save / load |
| Esc | Pause menu |

## Character customization and sample data

The `CharacterBuilder` constructs immutable `CharacterProfile` values. Preset values include a traveler named **Hikaru**, **medium purple hair**, and a predefined skin tone. The available art includes **five skin-tone sprite sheets**, **three hair lengths**, and **five hair colors** (blonde, red, brown, black, purple) with directional hair sprites. Gender is stored as a profile option and does not require unique art for every selection.

Sample world content is bundled under `src/main/resources/assets/`: the bedroom/hallway tileset, layered player sprites, Hikari's sprite and portrait, crystal images and six-frame effects, and a training dummy. No external asset download is required after cloning.

## Save data

The game uses a **single local save slot in Java Properties format**, not JSON. By default it is stored at:

```text
~/.through-the-parallels/save.properties
```

`SaveManager` stores character appearance, player coordinates/direction, current room, quest progress, and the chosen dialogue ending. Starting a new game asks before replacing existing progress. Temporary dummy health/cooldown are reset on a new session/load; they are not durable story progression.

## Design patterns (six)

| Category | Pattern | Core implementation | Visible usage |
|---|---|---|---|
| Creational | **Builder** | `CharacterBuilder`, `CharacterProfile` | Builds validated customizable traveler |
| Creational | **Factory Method** | `EventFactory`, `DeskEventFactory`, `CrystalEventFactory` | Desk and crystal interactions create/execute event objects |
| Structural | **Facade** | `GameFacade` | Central interface for starting, movement, dialogue, quest, save/load, and training |
| Structural | **Decorator** | `Ability`, `AbilityDecorator`, `PowerAbility`, `ReachAbility`, `SwiftnessAbility` | Changes real dummy training damage, range, and cooldown |
| Behavioral | **Strategy** | `InteractionStrategy`, `TrustStrategy`, `DoubtStrategy`, `DialogueManager` | Selects different Hikari dialogue outcomes |
| Behavioral | **Observer** | `GameEventPublisher`, `GameEventListener`, `QuestManager` | Event-driven quest progress and notifications |

The six patterns are integrated in the running application. For defense, demonstrate an actual scenario for each; class names alone are insufficient.

## Architecture

```text
src/main/java/com/throughtheparallels/
  Main.java, Launcher.java, CharacterCreator.java
  CharacterBuilder.java, CharacterProfile.java
  GameFacade.java, Player.java, RoomRenderer.java
  RoomType.java, WorldState.java, StoryStage.java, PauseMenu.java
  story/          # dialogue strategy, events and factories
  progression/    # observer quests, decorators, pulse training, save data
src/main/resources/assets/  # original pixel art and effect frames
src/test/java/               # JUnit test classes

docs/                        # pattern UML and test documentation
```

JavaFX (`Main`, `CharacterCreator`, `DialogueView`) handles presentation; `GameFacade` coordinates subsystems; `QuestManager`, dialogue strategies, event factories and `PulseTraining` handle business logic; `SaveManager` provides file persistence.

## Testing

Run the default unit tests:

```bash
mvn clean test
```

Optional headless JavaFX integration tests:

```bash
mvn -Pheadless-tests clean test
```

To generate GUI screenshots during those tests:

```bash
mvn -Pheadless-tests -Dsdp.screenshotDir=target/screenshots test
```

The repository's `docs/DECORATOR_TESTS.md` records a Linux/JDK 21/Maven 3.9.9/Monocle headless test run on **15 November 2026: 34 passed, 0 failed, 0 skipped**. These results are reported from that documented run, not re-executed as part of this README's creation. Integration tests tagged `sdp.fxTests` are excluded unless the headless profile is enabled.

## UML and documentation

Existing documentation includes `docs/story-patterns.puml`, `docs/decorator-class.puml`, `docs/decorator-sequence.puml`, and `docs/DECORATOR_TESTS.md`. The course report should additionally include screenshots, a combined class diagram, a main-journey sequence diagram, and team contributions.

## Team contributions

- **Member 1 - Sara Yermaganbetova**: character creation, Builder, rendering/movement, Facade, and pixel art.
- **Member 2 - Aruzhan Marat**: story interactions, event factories, dialogue Strategy.
- **Member 3 - Damir Satimov**: progression/quests, Observer, crystal ability Decorator, testing/persistence integration.

Replace bracketed fields with verified names and confirm the actual contributions against the Git history. Each student must independently explain at least two assigned patterns.

## Scope and limitations

This is a short two-room demo with branching dialogue and a temporary target-practice encounter. It does not include achievements or a full RPG combat/world system. The saved result is a persistent story-progress/ending state, not a separately exported PDF. On-screen presentation and game behavior require JavaFX; headless testing uses an optional Maven profile.

## AI and asset disclosure

No AI was used to generate code or artwork. All pixel art, sprite sheets, and effect frames were created by the team members. The repository does not include any third-party assets that require attribution or licensing beyond the standard JavaFX and Maven dependencies. However, AI tools were used for fixing the merging problems in GitHub and for generating the initial README template. The team members are responsible for all code and artwork in the repository.

## License

**No license is specified in the repository.** Do not assume the code or artwork may be reused outside the terms allowed by the authors.
