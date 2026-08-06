# ROM-backed file selection runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Replace the title-screen Enter shortcut with the ROM-backed file-selection and New Game name-entry boundary, while preserving the existing configured gameplay bootstrap after a new file is committed.

**Architecture:** Add a small ROM data adapter for file-menu tables, a pure controller that owns selection/name-entry state and emits immutable render snapshots/actions, and feed those snapshots through the existing background/sprite renderer. Main remains responsible for screen transitions and continues to use the existing configured room as the post-creation gameplay bootstrap.

**Tech Stack:** Java records/classes, existing RomBank/BackgroundSceneLoader, LWJGL renderer, Gradle/JUnit tests.

---

### Task 1: Add ROM-backed file-menu data and background scene specifications

**Files:**
- Create: java/src/main/java/linksawakening/ui/FileMenuRomData.java
- Modify: java/src/main/java/linksawakening/scene/BackgroundSceneCatalog.java
- Create: java/src/test/java/linksawakening/ui/FileMenuRomDataTest.java
- Modify: java/src/test/java/linksawakening/scene/BackgroundSceneLoaderTest.java

- [x] Write failing adapter tests for the 0x40-byte name-entry table, 0x100-byte codepoint-to-tile table, selection cursor Y table, name cursor tables, and defensive copies.
- [x] Write failing catalog/loader tests for selection, command-row, and creation map/attrmap/palette specs.
- [x] Run:
  ~~~sh
  gradle test --tests linksawakening.ui.FileMenuRomDataTest --tests linksawakening.scene.BackgroundSceneLoaderTest
  ~~~
  Confirm the new tests fail before adding production behavior.
- [x] Implement FileMenuRomData using RomBank/from-bank-address ROM reads:
  - bank 1 $48E4, length 4: selected-slot cursor Y positions;
  - bank 1 $4BB5, length $40: name-entry character table;
  - bank 1 $4B30, length $40: name cursor Y positions;
  - bank 1 $4B70, length $40: name cursor X positions;
  - bank 1 $4BB0, length 5: name-position cursor X positions;
  - bank $1C, $4641, length $100: CodepointToTileMap.
  Return clones from all byte-array accessors.
- [x] Add exact BackgroundSceneCatalog specs:
  - selection: tilemap bank $20 address $6336, attrmap bank $24 address $5F80;
  - command row: tilemap bank $20 address $6328, attrmap bank $24 address $5F74;
  - creation: tilemap bank $20 address $644D, attrmap bank $24 address $6045;
  - all use menu palette block bank $21 address $7536.
- [x] Run the focused tests and commit:
  ~~~sh
  gradle test --tests linksawakening.ui.FileMenuRomDataTest --tests linksawakening.scene.BackgroundSceneLoaderTest
  git add java/src/main/java/linksawakening/ui/FileMenuRomData.java java/src/main/java/linksawakening/scene/BackgroundSceneCatalog.java java/src/test/java/linksawakening/ui/FileMenuRomDataTest.java java/src/test/java/linksawakening/scene/BackgroundSceneLoaderTest.java
  git commit -m "feat: load ROM-backed file menu data"
  ~~~

Task 1 is complete in commit 3a75533.

### Task 2: Implement pure selection and New Game name-entry state

**Files:**
- Create: java/src/main/java/linksawakening/ui/FileMenuAction.java
- Create: java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java
- Create: java/src/main/java/linksawakening/ui/FileMenuController.java
- Create: java/src/test/java/linksawakening/ui/FileMenuControllerTest.java

- [x] Write failing controller tests for:
  - empty-slot selection entering creation;
  - selection wrapping across slots 0..2 and command row 3 only when the save bitfield is nonzero;
  - command-row left/right toggle;
  - character movement by +/-1 and +/-0x10 with 0x40 wrap;
  - A adding a character, B backing up, and Start committing;
  - five-byte stored names converting through the ROM codepoint table;
  - dynamic map writes and cursor OAM positions;
  - immutable snapshot arrays.
- [x] Run the focused controller test and confirm it fails before implementation.
- [x] Implement FileMenuAction with NONE, START_NEW_GAME, and LOAD_GAME actions, including selected slot and cloned name bytes where relevant.
- [x] Implement FileMenuFrameSnapshot as an immutable render payload containing scene id, selected slot, command-row toggle, name-entry state, decoded tilemap/attrmap, BG/OBJ palettes, and file-menu sprites.
- [x] Implement FileMenuController with ROM-backed data and a background-scene provider:
  - use save bitfield bits 0..2 for initialized slots;
  - use slot 3 for the command row only when at least one save exists;
  - selection cursor Y values from ROM $48E4;
  - command arrow OAM y $88, tile $BE, x $2C/$64;
  - creation map destinations $9849, $984A, $982A;
  - name cursor positions from ROM $4B30, $4B70, $4BB0;
  - selected name destinations $98C5, $9925, $9985;
  - stored zero bytes render as tile $7E; nonzero bytes index CodepointToTileMap after decrement;
  - creation movement and editing follow the ROM’s 0x40-character table and five-byte name capacity.
- [x] Run focused tests and commit:
  ~~~sh
  gradle test --tests linksawakening.ui.FileMenuControllerTest
  git add java/src/main/java/linksawakening/ui/FileMenuAction.java java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java java/src/main/java/linksawakening/ui/FileMenuController.java java/src/test/java/linksawakening/ui/FileMenuControllerTest.java
  git commit -m "feat: model ROM file selection and new game entry"
  ~~~

Task 2 is complete in commit 11ccc55.

### Task 3: Load exact menu tiles and render snapshots

**Files:**
- Modify: java/src/main/java/linksawakening/gpu/GPU.java
- Modify: java/src/main/java/linksawakening/render/RenderScreen.java
- Modify: java/src/main/java/linksawakening/render/GameFrameState.java
- Modify: java/src/main/java/linksawakening/render/GameFrameSceneBuilder.java
- Create/modify: java/src/test/java/linksawakening/gpu/GPUFileMenuTilesTest.java
- Create/modify: java/src/test/java/linksawakening/render/GameFrameSceneBuilderFileMenuTest.java

- [x] Write failing GPU and framebuffer tests for the ROM-backed menu tile destinations and for a file-menu snapshot producing a background plus object-sprite render layer.
- [x] Run the focused tests and confirm they fail before implementation.
- [x] Add GPU.loadMenuTiles mirroring LoadMenuTiles/LoadBaseTiles:
  - copy the contiguous 0x100-tile base block from adjusted bank $2C, $4800 to VRAM tile $080;
  - copy MenuTiles bank $2F, $4000, 0x40 tiles to tile $080;
  - copy FontTiles bank $2F, $5000, 0x80 tiles to tile $100;
  - copy Items1Tiles adjusted bank $2C, $47A0, 2 tiles to tile $0E0.
- [x] Add FILE_MENU to RenderScreen, carry FileMenuFrameSnapshot in GameFrameState, and preserve all existing empty/with-method behavior.
- [x] Make GameFrameSceneBuilder prefer the file-menu snapshot for the file-menu screen while reusing the existing background and CutsceneSpriteRenderLayer infrastructure.
- [x] Run focused tests and commit:
  ~~~sh
  gradle test --tests linksawakening.render.GPUFileMenuTilesTest --tests linksawakening.render.GameFrameSceneBuilderFileMenuTest
  git add java/src/main/java/linksawakening/gpu/GPU.java java/src/main/java/linksawakening/render/RenderScreen.java java/src/main/java/linksawakening/render/GameFrameState.java java/src/main/java/linksawakening/render/GameFrameSceneBuilder.java java/src/test/java/linksawakening/gpu/GPUFileMenuTilesTest.java java/src/test/java/linksawakening/render/GameFrameSceneBuilderFileMenuTest.java
  git commit -m "feat: render ROM-backed file menu frames"
  ~~~

Task 3 is complete in commit 7649c92.

### Task 4: Wire title Enter to file selection and New Game transition

**Files:**
- Modify: java/src/main/java/linksawakening/Main.java
- Modify: docs/reconstruction-roadmap.md
- Create/modify: java/src/test/java/linksawakening/MainFileMenuFlowTest.java

- [ ] Write failing flow tests for title Enter entering file selection and a committed empty-slot name returning START_NEW_GAME to the existing configured gameplay bootstrap.
- [ ] Run the focused flow test and confirm it fails before implementation.
- [ ] Add a file-menu screen state and controller initialization after ROM/background setup.
- [ ] On title Enter, load menu tiles, initialize the controller, select the correct selection map based on save state, and switch to FILE_MENU.
- [ ] Tick the controller while on the file-menu screen; on START_NEW_GAME, call the existing configured gameplay bootstrap. Keep initialized-slot load and SRAM/copy/erase behavior explicit as unsupported follow-ups rather than fabricating persistence.
- [ ] Carry file-menu snapshots into GameFrameState and map FILE_MENU through currentRenderScreen().
- [ ] Run focused flow tests and update the roadmap to record the verified boundary and remaining save-system gaps.
- [ ] Commit:
  ~~~sh
  gradle test --tests linksawakening.MainFileMenuFlowTest
  git add java/src/main/java/linksawakening/Main.java docs/reconstruction-roadmap.md java/src/test/java/linksawakening/MainFileMenuFlowTest.java
  git commit -m "feat: enter ROM-backed file menu from title"
  ~~~

### Task 5: Full verification and handoff

**Files:** no planned source changes.

- [ ] Run gradle clean test.
- [ ] Run git diff --check and inspect git status --short.
- [ ] Review the final diff for accidental changes outside the isolated worktree and confirm the root checkout remains untouched.
- [ ] Report exact verification results and the intentionally deferred file-menu behaviors: copy, erase, load existing save, SRAM persistence, fades, and exact audio sequencing.
