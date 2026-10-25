# LinguaCraft (NeoForge 1.21.1)

Say the item's name in **Spanish or Japanese** or it gets **deleted**. Random phrase challenges teach you real sentences too.

## Build
1. Install JDK 21.
2. In this folder run `gradle wrapper` once (or copy `gradlew`, `gradlew.bat` and `gradle/` from the official NeoForge 1.21.1 MDK), then:
   - `./gradlew runClient` to test in a dev client
   - `./gradlew build` to make `build/libs/linguacraft-1.0.0.jar`
3. If Gradle can't resolve `net.neoforged.moddev` `2.0.78` or `neo_version` `21.1.172`, open the official 1.21.1 MDK and copy its current plugin version / NeoForge version into `build.gradle` and `gradle.properties`.

## First run
The first challenge in a language downloads a ~40-50 MB offline speech model (Vosk) into `config/linguacraft/models/`. After that everything works offline. Allow microphone access for Java/Minecraft in your OS.

## How it plays
- **Item words:** craft, smelt or pick up an item for the first time -> a panel appears with the word, pronunciation and a live accuracy bar. Reach the threshold (default 90%) = "Unlocked!", otherwise every copy of that item is deleted from your inventory and crafting grid.
- **Phrases:** every ~5 minutes (random), plus after eating, killing a hostile mob, getting hurt, waking up, or changing dimension. Fail = Slowness by default (configurable).
- **Review:** ~30% of random challenges re-quiz item words you already unlocked.
- **Practice mode:** nothing is ever deleted.

## Commands
- `/lingua language <spanish|japanese>`
- `/lingua practice <true|false>`
- `/lingua test` (start a challenge now)
- `/lingua skip`
- `/lingua status`

## Config
`config/linguacraft-common.toml` (or Mods -> LinguaCraft -> Config in game): default language, threshold, time limits, phrase frequency, trigger chance, penalties.

## Adding words
Item words (191 per language): `core/Vocab.java` (key = item registry path like `wooden_pickaxe`). Phrases: `core/Phrases.java`.

## Notes
- Scoring runs on the client; the server trusts the reported accuracy. Fine for solo play and streams, not for competitive servers.
- Items with no entry in `Vocab.java` are never challenged.
