# ROM-Backed New Game Bootstrap Implementation Plan

> **For agentic workers:** Execute this plan task-by-task with test-first
> checkpoints. Keep the source of truth in the disassembly and shipped ROM;
> do not replace the runtime with a CPU/PPU emulator.

**Goal:** Make a committed empty file enter the disassembly's fresh-game
state in Marin's House instead of the configured debug overworld start.

**Architecture:** Add an immutable `NewGameStartProfile` in the startup layer
for the source-derived map, entry, capacity, and initialization values. Add an
explicit reset operation to `PlayerState` for the fields currently modeled by
the Java runtime. Give `Main` a dedicated new-game transition that loads the
indoor room through `RoomSession` and applies the profile. Leave existing-save
loading, SRAM persistence, copy, erase, and file-menu transition effects
explicitly deferred.

**Tech Stack:** Java records/classes, existing `RoomSession`/`RoomLoader`,
JUnit 5, Gradle.

---

### Task 1: Add failing source-derived profile and player-reset tests

**Files:**
- Create: `java/src/test/java/linksawakening/startup/NewGameStartProfileTest.java`
- Modify: `java/src/test/java/linksawakening/state/PlayerStateTest.java`
- Modify: `java/src/test/java/linksawakening/MainFileMenuFlowTest.java`

- [ ] Write profile tests for map `$10`, room `$A3`, entry `(0x50,0x60)`,
  max arrows/bombs `$30`, max powder `$20`, ROM down direction `$03`,
  standing animation `$00`, and the source wrecking-ball values.
- [ ] Write a player-reset test that mutates the debug constructor's inventory,
  equipment, resources, damage buffers, and pickup state, then asserts the
  profile produces three full hearts, blank inventory, zero counts, and the
  exact new-game capacities.
- [ ] Add a flow architecture assertion that the file-menu branch invokes
  `startNewGame()` and does not invoke `startConfiguredGameplay()` for
  `START_NEW_GAME`.
- [ ] Run the focused tests and confirm they fail because the profile/reset
  API and dedicated branch do not yet exist.

### Task 2: Implement the source-derived profile and modeled fresh-game state

**Files:**
- Create: `java/src/main/java/linksawakening/startup/NewGameStartProfile.java`
- Modify: `java/src/main/java/linksawakening/state/PlayerState.java`

- [ ] Add an immutable profile with the exact values from `bank1.asm` and a
  method that applies the modeled player-state portion to a supplied
  `PlayerState`.
- [ ] Add `PlayerState.initializeNewGame(maxArrows, maxBombs,
  maxMagicPowder)` that clears all currently modeled mutable state, fills the
  three-heart health value, sets the three source capacities, clears A/B and
  all ten subscreen slots, and removes the constructor's debug inventory.
- [ ] Keep the existing no-argument constructor behavior unchanged for
  existing debug-oriented tests and callers; only the real New Game boundary
  invokes the reset.
- [ ] Run the profile and player-state tests and commit:
  `feat: model ROM-backed new game defaults`.

### Task 3: Wire the file-menu action to the actual indoor start

**Files:**
- Modify: `java/src/main/java/linksawakening/Main.java`
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify: `java/src/test/java/linksawakening/MainFileMenuFlowTest.java`

- [ ] Add `startNewGame()` alongside `startConfiguredGameplay()`.
- [ ] Clear the file-menu controller, select the gameplay screen, apply the
  profile to `playerState`, call `roomSession.loadIndoor(0x10,0xA3)`, and set
  Link's room-entry position to `(0x50,0x60)`.
- [ ] Change only the `START_NEW_GAME` action branch to call `startNewGame()`;
  keep initialized-slot `LOAD_GAME` explicit until SRAM is implemented.
- [ ] Add a shipped-ROM room-session regression proving map `$10`, room `$A3`
  loads through the indoor loader and reports `Warp.CATEGORY_INDOOR`.
- [ ] Run the focused tests and commit:
  `feat: enter the ROM-backed new game room`.

### Task 4: Update the reconstruction record and verify the slice

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] Record the verified new-game bootstrap values and identify SRAM,
  existing-save load, copy/erase, wrecking-ball runtime state, and exact
  file-menu transition effects as remaining work where applicable.
- [ ] Run focused tests, then `gradle clean test`.
- [ ] Run `git diff --check`, inspect `git status --short`, and confirm the
  root checkout remains unaffected.
- [ ] Review the final diff for accidental coupling to configured/debug
  startup and report the exact verification results.

