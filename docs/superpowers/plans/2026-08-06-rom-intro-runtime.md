# ROM-backed opening intro runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the approximated Java opening sequence with a ROM-backed, frame-accurate domain runtime for the shipped GBC intro, including its scene substates, entity OAM, scrolling, dynamic palettes, title-map writes, and title handoff.

**Architecture:** Add an `IntroRomData` adapter for fixed intro tables and an immutable `IntroFrameSnapshot` for render-time state. Refactor `IntroSequence` into a source-shaped state machine that owns its private intro RNG and current dynamic maps/palettes. `CutsceneManager` forwards snapshots, while `GameFrameSceneBuilder` consumes them without changing the room renderer. `Main` supplies ROM-backed background scenes and keeps the existing scene-load/music/skip boundaries.

**Tech Stack:** Java 17 records and collections, existing `RomBank`, `RomRandomByteSource`, `BackgroundSceneLoader`, JUnit 5, Gradle.

---

## Task 1: Add the ROM intro data adapter

Files:

- Add `java/src/main/java/linksawakening/cutscene/IntroRomData.java`.
- Add `java/src/test/java/linksawakening/cutscene/IntroRomDataTest.java`.

- [ ] Write failing adapter tests first. Use a synthetic byte array sized from `RomBank.romOffset(0x01, 0x7AE4) + 0x180` and write sentinel bytes at the shipped labels. Cover:
  - banked reads through `RomBank.romOffset`, including a bank/address pair whose correct file offset differs from `bank * 0x4000 + address`;
  - truncation rejection for every public fixed-table reader;
  - the six four-byte `IntroShipTiles` records at `$01:$7538`, including `$F8` as a signed X offset;
  - the four four-byte `Data_001_7550` records and eight-byte `ShipHeaveTable`;
  - four six-record lightning rectangles at `$01:$75CB`, the four Marin two-record variants at `$01:$764F`, the two-record inert-Link variants at `$01:$7A27`, and eight sparkle variants at `$01:$77BD`;
  - the seven pointer-selected title rows from `TitleTileMap`/`TitleAttrMap`, with the three-byte draw-command header removed and all 16 row bytes preserved as unsigned values;
  - the 20-by-19 `TitleScreenPostBeachTilemap` bytes at `$01:$7AE4`;
  - RGB555 decoding for the shipped GBC `DXFadeInPalette` block at `$01:$79A0`.

  Run the red test before production code:

```sh
cd java
gradle test --tests linksawakening.cutscene.IntroRomDataTest
```

- [ ] Implement `IntroRomData` with named bank/address constants matching the disassembly labels. Its public methods return immutable value records (`OamEntry`, `SpritePair`, `TitleRow`, and `PaletteBlock`) rather than raw ROM offsets. Each reader validates `RomBank.romOffset(bank, address) + length <= romData.length` and reports the label, bank, address, and requested length in the exception.

  Decode bytes using `Byte.toUnsignedInt`, sign-extend only the relative OAM X/Y offsets used by the ship/lightning records, preserve OAM order, and use `RomBank.decodeRgb555` for palette colors. Read title pointer words little-endian from the pointer table and validate both the pointer table and each pointed row. Do not duplicate any intro table as a Java literal.

- [ ] Run the focused test green, then run the existing ROM utility tests to catch regressions:

```sh
gradle test --tests linksawakening.cutscene.IntroRomDataTest --tests linksawakening.rom.RomBankTest
```

## Task 2: Define immutable per-frame intro state and migrate state-machine tests

Files:

- Add `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java`.
- Modify `java/src/main/java/linksawakening/cutscene/IntroSequence.java`.
- Replace the approximation assertions in `java/src/test/java/linksawakening/cutscene/IntroSequenceTest.java` with ROM-backed tests.

- [ ] Write failing tests for snapshot immutability and source-shaped state transitions. Build a synthetic ROM containing the adapter tables and a background provider returning 32x32 maps. Assert that:
  - the initial snapshot is sea setup with ship entity data from the ROM, not a Java ship literal;
  - the ship moves and base scroll advances only on the source's `$08` cadence;
  - sea lightning triggers only at the source scroll thresholds `$10`, `$30`, `$38`, `$58`, `$5A`, and `$69`, then runs the ROM palette countdown before the Link-face scene;
  - Link-face rain consumes exactly the source random calls, reaches scream at frame `$80`, lightning at `$90`, and returns to the sea/next stage at `$A0` as the source handler does;
  - the beach enters Marin at `$B0/$68`, waits at the source countdowns, spawns inert Link at `$FE/$6E`, pauses at scroll `$3A` and `$40`, jumps to `$A0` at `$56`, and holds through `$E0` before title reveal;
  - title rows appear in the pointer-table order from ROM, one row per source handler frame, with no synthetic mask substituting for the draw-command bytes;
  - `skipToTitle()` publishes the stable title snapshot with all title rows and no stale beach sprites.

  Run the red tests before changing `IntroSequence`:

```sh
cd java
gradle test --tests linksawakening.cutscene.IntroSequenceTest
```

- [ ] Implement `IntroFrameSnapshot` as a record containing `sceneId`, source substate name, frame counter, base `scrollX`/`scrollY`, optional per-scanline `lineScrollX`, vertical wave offset, full 32x32 tilemap and attrmap, BG/OBJ palette rows, ordered `List<IntroSprite>`, and revealed-title row count. Clone array data at construction/access and use `List.copyOf` so a published frame cannot be mutated by the next tick.

- [ ] Refactor `IntroSequence` around the meaningful disassembly substates: setup, sea, sea-lightning, Link-face, beach transition, Marin state 0/1/2/3/4, title-row reveal, title SFX, copyright/DX fade, stable title, and complete. Advance in source order (timer decrement/update, entity/OAM update, scroll/palette update, then snapshot publication). Construct it from `byte[] romData` plus `Function<String, BackgroundScene> backgroundProvider`; initialize a private `RomRandomByteSource(0xA2)` and never share it with gameplay.

  Read ship, lightning, Marin, inert-Link, sparkle, DX, title-row, post-beach, palette, and wave data through `IntroRomData`. Reproduce `RenderRain`'s two seed calls, per-row tile-selection calls, row count (`$10` sea or `$15` Link-face), `$1C` X step, `$25` Y step, `$A0`/`$98` wrap, and exact sprite ordering. Use source OAM coordinates and attributes to create `IntroSprite` values, including the ship's `$F8` relative offset and lightning flip flags. Keep the current observation helpers (`sceneId`, `scrollX`, `scrollY`, `lineScrollX`, `sprites`, `titleRevealRows`) as views over the current snapshot so callers remain simple.

- [ ] Run the focused state tests and the ROM adapter tests green. Record the exact tested frame boundaries in the test names so later entity work can distinguish a source-timing regression from a rendering regression:

```sh
gradle test --tests linksawakening.cutscene.IntroRomDataTest --tests linksawakening.cutscene.IntroSequenceTest
```

## Task 3: Make CutsceneManager and the frame builder consume dynamic intro state

Files:

- Modify `java/src/main/java/linksawakening/cutscene/CutsceneManager.java`.
- Modify `java/src/main/java/linksawakening/render/GameFrameState.java`.
- Modify `java/src/main/java/linksawakening/render/GameFrameSceneBuilder.java`.
- Add or extend `java/src/test/java/linksawakening/cutscene/CutsceneManagerTest.java`.
- Add `java/src/test/java/linksawakening/render/GameFrameSceneBuilderTest.java` if the existing render test fixtures do not cover cutscenes.

- [ ] Add failing integration tests that start a manager with a synthetic ROM/background provider and assert that:
  - `startIntro` publishes the initial `IntroFrameSnapshot` and still invokes the scene loader once for sea;
  - a tick that changes only a dynamic palette, title row, line-scroll value, or sprite list changes the built background/sprite layers without changing the room path;
  - scene changes forward the new snapshot and load the matching static background exactly once;
  - skip publishes the stable-title maps/palettes/sprites and keeps the existing dialog and scene-loading behavior.

  Run the red tests:

```sh
cd java
gradle test --tests linksawakening.cutscene.CutsceneManagerTest --tests linksawakening.render.GameFrameSceneBuilderTest
```

- [ ] Extend `CutsceneManager` with a ROM/provider-aware `startIntro(byte[] romData, Function<String, BackgroundScene> backgroundProvider)` path and a `frameSnapshot()` accessor. Preserve a small compatibility constructor only if existing non-intro callers require it; it must not construct a non-ROM intro. When the sequence changes scene, call the existing loader and expose the already-updated snapshot. Keep dialog ticking, `skipIntroToTitle`, title detection, and input behavior unchanged.

- [ ] Extend `GameFrameState` with an optional `IntroFrameSnapshot` and update every `with...` method/`empty()` constructor consistently. In `GameFrameSceneBuilder`, use the snapshot's maps, palettes, base scroll, line-scroll array, title reveal count, and ordered OAM sprites for background scenes. Apply the snapshot's vertical wave offset to the background scroll calculation. Keep `RoomRenderLayer`, Link, entity, VFX, inventory, and dialog composition byte-for-byte on the existing room path.

- [ ] Run the focused integration tests plus the existing scene and rendering tests:

```sh
gradle test --tests linksawakening.cutscene.CutsceneManagerTest --tests linksawakening.render.GameFrameSceneBuilderTest --tests linksawakening.scene.BackgroundSceneLoaderTest
```

## Task 4: Wire the shipped ROM into startup and validate visible intro frames

Files:

- Modify `java/src/main/java/linksawakening/Main.java`.
- Extend `java/src/test/java/linksawakening/gpu/GPUIntroTilesTest.java` or add `java/src/test/java/linksawakening/cutscene/IntroShippedRomTest.java`.
- Add `java/src/test/java/linksawakening/render/IntroFrameRegressionTest.java` if a framebuffer fixture is available.
- Update `docs/ROADMAP.md` (or the current roadmap file identified with `rg --files docs`) to mark only the implemented opening-intro slice complete and list file-select/save/new-game as remaining.

- [ ] Write failing shipped-ROM tests before wiring Main. Load `src/main/resources/rom/azle.gbc` through the same resource path as runtime and assert:
  - adapter records at `$01:$7538`, `$01:$75CB`, `$01:$764F`, `$01:$77BD`, `$01:$7AE4`, and `$01:$79A0` match the ROM bytes and expected record counts;
  - the first sea snapshot contains ROM ship tiles and the source initial rain count;
  - representative frames around sea scroll `$10`, Link-face frame `$80/$90`, beach scroll `$3A/$40/$56`, first title row, and stable title use the expected scene, scroll, map write, palette row, and sprite-order values;
  - changing the frame changes only the expected animated pixels/OAM entries while the decoded static background remains unchanged.

  Run the red shipped-ROM test:

```sh
cd java
gradle test --tests linksawakening.cutscene.IntroShippedRomTest --tests linksawakening.render.IntroFrameRegressionTest
```

- [ ] In `Main.initMenuSystem`, construct the manager with the loaded ROM and the existing `BackgroundSceneLoader`. In `startIntroCutscene`, load intro tiles before starting the ROM-backed sequence. Make the scene provider use `BackgroundSceneCatalog.forCutsceneScene` and reject a missing spec. Keep `setCutsceneScene` responsible for GPU title tiles, static scene loading, and natural title music; the snapshot supplies only the dynamic per-frame overrides.

- [ ] Implement the shipped-ROM assertions and a deterministic framebuffer fixture using the existing indexed renderer. Verify rain, ship, lightning, Marin, inert Link, title rows, DX OAM, and palette transitions are rendered in source order. Do not add PNG assets or hardcoded table replacements. Preserve the title Enter handoff to the existing configured gameplay path; do not add file-menu/save behavior in this slice.

- [ ] Run the focused startup/ROM/render tests green:

```sh
gradle test --tests linksawakening.cutscene.IntroShippedRomTest --tests linksawakening.render.IntroFrameRegressionTest --tests linksawakening.gpu.GPUIntroTilesTest
```

## Task 5: Full verification and handoff

- [ ] Run the entire Java suite from a clean build and inspect the output for failures:

```sh
cd java
gradle clean test
```

- [ ] Run repository whitespace validation:

```sh
git diff --check
```

- [ ] Inspect the final diff for accidental Java literals that duplicate intro ROM tables, changes outside the intro/render integration, and regressions to overworld, indoor, Color Dungeon, title-without-intro, and skip paths. Confirm the roadmap and tests describe exactly what is implemented.

- [ ] Commit the implementation only after the fresh verification commands pass, using a focused message such as `feat: render intro sequence from ROM`.
