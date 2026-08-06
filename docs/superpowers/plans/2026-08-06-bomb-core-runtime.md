# Bomb core runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Implement the ordinary ROM-backed player bomb from placement through fuse, pre-explosion warning, explosion presentation, sound, and unload in the Java room runtime.

**Architecture:** Keep entity type `$02` in one room-entity slot. Decode the source bomb, warning-pair, and explosion-rectangle display lists through `EntitySpriteHandlerCatalog`; use a small pure `BombMotion` helper for countdown phase/variant decisions; bridge equipment placement through `RoomSession` into `RoomEntityRuntime`; and emit the explosion sound through the existing gameplay sound event path. Preserve the current room tick order and leave bombable-room mutations, bomb arrows, and enemy-bomb branches as explicit follow-up slices. The active lifecycle owns the phase presentation; lifted/throw status integration remains Task 5 because the source lifted handler has a bomb-specific render-only branch.

**Tech Stack:** Java 21 records/classes, JUnit 5, Gradle, shipped `azle.gbc` ROM, existing indexed framebuffer/OAM renderer and ROM bank helpers.

## File map

- Modify `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java` and its tests to decode the bank-$03 bomb display lists.
- Add `java/src/main/java/linksawakening/world/BombMotion.java` and focused tests for source countdown phases.
- Modify `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`, `RoomEntity.java`/selection plumbing as needed, and runtime/render tests for active spawn, lifecycle, source visual offsets, and unload.
- Modify `java/src/main/java/linksawakening/world/RoomSession.java` and its tests to expose placement and refresh the immutable room snapshot.
- Add `java/src/main/java/linksawakening/equipment/Bomb.java`; modify `ItemRegistry` consumers, `Main.java`, and equipment tests for inventory/edge wiring.
- Modify `java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java` and `GameplaySoundEffectMap.java`, plus sound-map tests, for explosion noise `$0C` (`$0B` remains hookshot).
- Modify `docs/reconstruction-roadmap.md` only after verification, recording the verified core scope and the deferred bomb branches.

## Task 1: Decode every bomb display list from the shipped ROM

**Files:** `EntitySpriteHandlerCatalog.java`, `EntitySpriteHandlerCatalogTest.java`

- [ ] **Step 1: Write the failing ROM-backed test.**

Add tests using the existing `loadRom()` helper and public catalog methods for:

  - `BombSprite` at bank `$03`, address `$652E`: a one-variant `SINGLE` definition whose tile is `$80` and attributes are `OAM_GBC_PAL_5 | OAMF_PAL1`;
  - `BombRightBeforeExplodingSprite` at bank `$03`, address `$5484`: a one-variant `PAIR` definition with two `$30` tiles and raw attributes `$01` and `$61`; and
  - `ExplosionSpriteRect` at bank `$03`, address `$6530`: four eight-entry rectangle variants, including signed offsets, `$32/$10/$12/$30` tile bytes, and exact `$01/$21/$02/$22/$42/$62` attributes.

Assert the bank/address metadata as well as representative entries from all
four variants, including the last variant's palette-zero bytes. The test must
fail because the bomb catalog accessors/mapping are absent.

- [ ] **Step 2: Run the focused test and confirm the expected failure.**

Run from `java/`:

    ./gradlew test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest

Expected: compilation failure for the missing bomb definitions or an
unsupported `$02` entity mapping; do not change production code before this
failure is observed.

- [ ] **Step 3: Implement ROM decoding and mapping.**

Add named catalog methods or a single source-shaped mapping that calls the
existing `decodeSingle`, `decodePair`, and `decodeRectangle` helpers. Add
`ENTITY_BOMB = 0x02` to the catalog mapping used by room entity selection.
Keep all OAM bytes ROM-derived; do not paste display data into Java.

- [ ] **Step 4: Run the focused test and inspect the diff.**

Run the focused catalog test and `git diff --check`. Confirm the test checks
the shipped ROM rather than a Java fixture, then commit:

    git add src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java
    git commit -m "feat: decode ROM bomb display lists"

## Task 2: Add the pure ROM countdown phase model

**Files:** `BombMotion.java`, `BombMotionTest.java`

- [ ] **Step 1: Write failing phase-boundary tests.**

Test a pure phase decision for unsigned transition countdown values:

  - `$A0` and `$22` select the normal bomb;
  - `$21` and `$18` select the warning pair, while `$22` remains normal;
  - `$17` selects explosion variant `$03`, `$14` selects `$03`, `$13` selects `$02`, `$10` selects `$02`, `$0F` selects `$01`, `$08` selects `$01`, `$07` selects `$00`, and `$00` selects `$00` before unload;
  - `$18` emits the one-shot explosion-sound transition; and
  - zero is an unload decision after the explosion presentation tick.

Also test that countdown arithmetic is unsigned and cannot index outside the
24-byte `ExplosionSpriteVariantFrames` table.

- [ ] **Step 2: Run the focused test and verify it fails.**

Run:

    ./gradlew test --tests linksawakening.world.BombMotionTest

Expected: the new type or methods are missing.

- [ ] **Step 3: Implement only pure phase/variant logic.**

Represent normal, warning, and explosion phases without ROM coordinates or
renderer dependencies. Keep the source table explicit and document that the
entity countdown is decremented by the runtime, not by this helper.

- [ ] **Step 4: Run the focused test and commit.**

Run the focused test and `git diff --check`, then commit:

    git add src/main/java/linksawakening/world/BombMotion.java src/test/java/linksawakening/world/BombMotionTest.java
    git commit -m "feat: model ROM bomb countdown phases"

## Task 3: Add bomb lifecycle to the room entity runtime

**Files:** `RoomEntityRuntime.java`, `RoomEntity.java` or sprite-selection
plumbing if required, `RoomEntityRuntimeTest.java`, and
`EntityRenderLayer.java`/`EntityRenderLayerTest.java` for the source normal-
bomb visual offset.

- [ ] **Step 1: Write failing spawn/lifecycle tests.**

Add tests that spawn a bomb through the runtime and assert:

  - the free slot contains type `$02`, source position/Z/direction, the ROM bomb definition, and transition countdown `$A0`;
  - the pre-tick `$22..$18` snapshots use the warning pair while retaining the same slot;
  - the `$18` tick emits exactly one explosion sound event and advances into the explosion phase;
  - countdown values `$17..$00` select the exact ROM rectangle variants and zero clears the slot;
  - the normal single phase applies the source `RenderBomb` Y increment of two pixels, while warning/explosion phases do not; and
  - `wHasPlacedBomb`-equivalent state is cleared when the bomb unloads.

Use a fake sound sink/event collector and the real catalog where the test
needs OAM metadata. Verify no transient second entity is created.

- [ ] **Step 2: Run the focused tests and confirm the missing lifecycle.**

Run:

    ./gradlew test --tests linksawakening.world.RoomEntityRuntimeTest --tests linksawakening.render.EntityRenderLayerTest

Expected: compilation or assertion failure because there is no bomb spawn or
phase-specific runtime state.

- [ ] **Step 3: Implement the smallest runtime bridge.**

Add a bomb spawn method and per-slot bomb state/countdown initialization. Make
the active entity handler run the bomb phase before generic enemy-only
handling. Leave lifted/throw status dispatch to Task 5, matching the source's
lifted render-only bomb branch. Use the existing entity countdown decrement
boundary and existing `clearEntity` path. Do not mark a pre-tick player bomb
as a dynamic spawn that would skip its first handler tick.

Select the catalog definition for the current phase while preserving the
same `RoomEntity` slot/type. Preserve the source normal single-sprite visual
Y increment of two pixels in the rendering contract. Keep normal bomb physics
and source wall-bounce tables behind the existing collision resolver; do not
add guessed movement constants.

- [ ] **Step 4: Run the focused runtime/render tests and inspect.**

Run both focused test commands, then `git diff --check`. Commit:

    git add src/main/java/linksawakening/world/RoomEntityRuntime.java src/main/java/linksawakening/world/RoomEntity.java src/test/java/linksawakening/world/RoomEntityRuntimeTest.java src/test/java/linksawakening/render/EntityRenderLayerTest.java
    git commit -m "feat: run ROM bomb entity lifecycle"

## Task 4: Wire bomb placement and inventory/audio behavior

**Files:** `Bomb.java`, `RoomSession.java`, `Main.java`,
`GameplaySoundEvent.java`, `GameplaySoundEffectMap.java`, and tests under
`equipment`, `world`, `gameplay`, and `linksawakening`.

- [ ] **Step 1: Write failing equipment and sound tests.**

Test that:

  - pressing the registered bomb item calls a placement target once per edge;
  - zero bombs leaves the count unchanged and emits `WRONG_ANSWER`;
  - a successful placement decrements the count once, rejects a second active bomb, and forwards Link's source coordinates/direction to `RoomSession`; and
  - `BOMB_EXPLOSION` resolves through `SoundEffectCatalog` to noise `$0C`, without changing hookshot noise `$0B`.

Use the existing `EquipmentController`/`InputState` test style and a fake
placement target so item tests do not require a GLFW window or full main loop.

- [ ] **Step 2: Run the focused tests and verify they fail.**

Run:

    ./gradlew test --tests linksawakening.equipment.EquipmentControllerTest --tests linksawakening.gameplay.GameplaySoundEffectMapTest

Expected: the bomb handler/event and placement target are absent.

- [ ] **Step 3: Implement the item and session bridge.**

Add the bomb equipment handler with a narrow target interface, register it for
`PlayerState.INVENTORY_BOMBS`, and route the target through `RoomSession` into
the runtime. Reuse `PlayerState` bomb-count methods and the existing gameplay
sound sink; preserve source decrement-before-spawn ordering. Wire the new
sound event to `SoundEffectNamespace.NOISE, 0x0C`. Keep the existing hookshot
mapping at `SoundEffectNamespace.NOISE, 0x0B`.

Register the item at the same construction point as Sword, Roc's Feather,
and Hookshot. Keep existing callers and tests that do not provide a bomb
target source-compatible.

- [ ] **Step 4: Run focused tests and commit.**

Run equipment, sound-map, session, and main architecture tests. Then:

    git diff --check
    git add src/main/java/linksawakening/equipment/Bomb.java src/main/java/linksawakening/world/RoomSession.java src/main/java/linksawakening/gameplay/GameplaySoundEvent.java src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java src/main/java/linksawakening/Main.java src/test/java/linksawakening/equipment src/test/java/linksawakening/world src/test/java/linksawakening/gameplay src/test/java/linksawakening
    git commit -m "feat: wire player bomb placement and audio"

## Task 5: Preserve lifted/thrown bomb motion and source countdown reset

**Files:** `RoomEntityRuntime.java`, `ThrownEntityMotion.java` only if the
existing API needs a bomb-specific assertion, and focused runtime tests.

- [ ] **Step 1: Write failing lifted/thrown tests.**

Cover ROM behavior that a bomb remains the same entity while lifted/thrown:

  - lifting continues to render the bomb and does not create another slot;
  - throwing uses the existing bomb offset in `ThrownEntityMotion` and writes
    countdown `$A0` at the source throw transition;
  - the bomb continues through wall reversal/ground handling and eventually
    enters the same explosion phase; and
  - clearing/unloading a thrown bomb releases the one-active-bomb state.

- [ ] **Step 2: Run the focused test and observe the pre-fix failure.**

Run:

    ./gradlew test --tests linksawakening.world.RoomEntityRuntimeTest --tests linksawakening.world.ThrownEntityMotionTest

- [ ] **Step 3: Implement source-shaped status integration.**

Run bomb handling for lifted/thrown statuses at the same point as the source
handler wrapper, preserve the fuse, and call the existing ROM-derived throw
motion rather than introducing a second bomb trajectory table. Keep enemy
bomb/private-state conversion out of this task.

- [ ] **Step 4: Run focused tests and commit.**

Run the focused runtime suite and `git diff --check`, then commit:

    git add src/main/java/linksawakening/world/RoomEntityRuntime.java src/test/java/linksawakening/world/RoomEntityRuntimeTest.java src/test/java/linksawakening/world/ThrownEntityMotionTest.java
    git commit -m "feat: preserve bomb lift and throw lifecycle"

## Task 6: Add the bounded explosion interaction seam

**Files:** `RoomEntityRuntime.java`, `RoomSession.java`, existing combat-event
types/consumer as needed, and focused world/combat tests.

- [ ] **Step 1: Write failing event-boundary tests.**

Assert that the explosion interaction window is reached only for transition
counts `$16..$0E`, that a bomb cannot interact before the source window, and
that the event identifies the bomb slot/position/damage type without being
misclassified as a normal sword/projectile hit. Assert that unsupported
destroyable-object mutations are not fabricated.

- [ ] **Step 2: Run the focused test and confirm the seam is absent.**

Run the relevant `RoomEntityRuntimeTest`/`RoomSessionTest` selection and verify
the expected missing event or boundary failure.

- [ ] **Step 3: Implement the narrow seam.**

Expose a source-shaped bomb explosion event through the existing room tick
result. If the current combat table can represent the ROM bomb damage type,
apply that result using the existing enemy-state path; otherwise retain the
event for the next interaction slice and consume only the already-supported
sound/Link effects. Do not write guessed room-object persistence or generic
enemy damage as a substitute for `CheckForBombDestroyableObjectBasic`.

- [ ] **Step 4: Run focused tests and commit.**

Run all affected world/combat tests and `git diff --check`, then commit:

    git add src/main/java/linksawakening/world src/test/java/linksawakening/world
    git commit -m "feat: expose ROM bomb explosion interaction window"

## Task 7: Full verification and roadmap handoff

**Files:** `docs/reconstruction-roadmap.md` and any test fixtures required by
the preceding tasks.

- [ ] **Step 1: Run the complete test suite.**

From `java/` run:

    ./gradlew test

Expected: all existing and new tests pass.

- [ ] **Step 2: Run repository hygiene checks.**

Run:

    git diff --check
    git status --short --branch

Resolve only issues caused by this slice; preserve unrelated worktree edits.

- [ ] **Step 3: Document the verified boundary.**

Add a dated roadmap section naming placement, ROM display decoding, fuse
timing, warning/explosion presentation, audio, and lifted/thrown lifecycle as
verified. Explicitly list bombable-object/puzzle effects, bomb arrows, enemy
bombs, and any unimplemented palette flash as remaining parity work.

- [ ] **Step 4: Commit documentation and report evidence.**

Run the complete suite once more after the documentation change, then commit:

    git add docs/reconstruction-roadmap.md
    git commit -m "docs: record ROM bomb core runtime"

Report the worktree/branch and verification commands; do not describe the
entire LADX reimplementation as complete.
