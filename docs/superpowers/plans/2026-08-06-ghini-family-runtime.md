# Ghini Family Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ROM-backed hiding, giant, and ordinary Ghini behavior to the live Java room-entity runtime, including the source Link-collision wake signal.

**Architecture:** Decode the two bank-$04 display-list forms through `EntitySpriteHandlerCatalog`, extend the existing per-slot `GhiniMotion` state machine for the shared hidden/visible handler, and pass a source-shaped `wCollisionType` byte from `Link` through `Main`/`RoomSession` into `RoomEntityRuntime`. Reuse the existing combat, recoil, renderer, and room-loader boundaries; do not add bomb or emulator code.

**Tech Stack:** Java 17 records/classes, JUnit 5, Gradle, shipped LADX DX ROM resource, existing indexed/OAM renderer.

---

## File map

- Modify `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java` to map entity IDs `$10` and `$11` to the ROM pair and rectangle lists.
- Modify `java/src/main/java/linksawakening/world/EntityRoomLoader.java` to apply
  `EntityInitGhini`'s exact `$12` initial Z value (`$10`) when a room stream is
  decoded.
- Modify `java/src/main/java/linksawakening/entity/Link.java` to expose the current source collision-type bits produced by blocked leading-edge probes.
- Modify `java/src/main/java/linksawakening/world/GhiniMotion.java` to retain hidden state, reveal countdown, and giant-list orientation selection.
- Modify `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java` to add the three Ghini hitbox/health/contact-damage entries.
- Modify `java/src/main/java/linksawakening/world/RoomEntityRuntime.java` to initialize/advance all three types, pass Link collision bits, exclude hidden combat, honor no-ground behavior, and include bank-$04 recoil.
- Modify `java/src/main/java/linksawakening/world/RoomSession.java` and `java/src/main/java/linksawakening/Main.java` to carry the collision-type byte without breaking existing callers.
- Modify focused tests under `java/src/test/java/linksawakening/entity/`, `java/src/test/java/linksawakening/world/`, and `java/src/test/java/linksawakening/` for ROM bytes, state transitions, collision bits, and live wiring.
- Modify `docs/reconstruction-roadmap.md` only after verification to record the completed Ghini slice and its explicit deferrals.

### Task 1: Lock the ROM display definitions

**Files:**
- Test: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`
- Test: `java/src/test/java/linksawakening/world/EntityRoomLoaderTest.java`
- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`
- Modify: `java/src/main/java/linksawakening/world/EntityRoomLoader.java`

- [ ] **Step 1: Write failing shipped-ROM assertions.** The catalog assertions are backed by `loadRom()` and call `forEntityType(0x10, OVERWORLD)`, `forEntityType(0x11, OVERWORLD)`, and `forEntityType(0x12, OVERWORLD)`. Assert:
  - `$10` and `$12` are supported `PAIR` definitions with bank `$04`, address `$5BFC`, two variants, and source first/second bytes `$58,$02` and `$5A,$02` for variant 0 (variant 1 is `$5C,$02` and `$5E,$02`);
  - `$11` is a supported `RECTANGLE` definition with bank `$04`, address `$5D26`, four variants, eight sprites per variant, and the first rectangle tuple `(-8,-8,$60,$02)`; and
  - the `$11` fourth variant preserves the source flipped bottom-row tile sequence `$6A,$6E,$6C,$64`.

  The rectangle assertions use the complete shipped-ROM 4x8 matrix, including `$02`
  for unflipped entries and `$22` for XFLIP entries. The shipped-room loader test also
  loads overworld room `$67`, whose source order contains `$10,$10,$12,$11`, and asserts
  Z values `$00,$00,$10,$00`.

  Example assertions:

  ```java
  EntitySpriteDefinition hiding = catalog.forEntityType(
      0x10, EntityRoomLoader.RoomTable.OVERWORLD);
  assertEquals(EntitySpriteDefinition.Shape.PAIR, hiding.shape());
  assertEquals(0x04, hiding.bank());
  assertEquals(0x5BFC, hiding.address());
  assertEquals(0x58, hiding.variant(0).first().tile());
  assertEquals(0x02, hiding.variant(0).first().attributes());

  EntitySpriteDefinition giant = catalog.forEntityType(
      0x11, EntityRoomLoader.RoomTable.OVERWORLD);
  assertEquals(EntitySpriteDefinition.Shape.RECTANGLE, giant.shape());
  assertEquals(8, giant.rectangleVariant(0).size());
  assertEquals(0x60, giant.rectangleVariant(0).get(0).oam().tile());
  assertEquals(0x6A, giant.rectangleVariant(3).get(4).oam().tile());
  ```

- [ ] **Step 2: Run the focused test and verify it fails.**

  Run: `gradle -p java test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest`

  Expected: FAIL because `$10`/`$11` currently return unsupported definitions.

- [ ] **Step 3: Implement the catalog mappings and exact room initialization.** Add constants for `$10` and `$11`. Map `$10` to `decodePair(entityType, 0x04, 0x5BFC, 2, 0)`, map `$11` to `decodeRectangle(entityType, 0x04, 0x5D26, 4, 8, 0)`, and leave the existing `$12` mapping unchanged. In `EntityRoomLoader`, make the ROM room-load path assign Z `$10` for `$12`, preserving the existing floating-item initialization for other types; do not initialize `$10`/`$11` Z from a guessed value.

- [ ] **Step 4: Run the focused test and verify it passes.**

  Run: `gradle -p java test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest`

  Expected: PASS with the exact shipped-ROM bytes.

- [ ] **Step 5: Commit the isolated display-data slice.**

  ```bash
  git add java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java \
      java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java
  git commit -m "feat: decode ROM Ghini display lists"
  ```

### Task 2: Add the source collision-type signal to Link

**Files:**
- Test: `java/src/test/java/linksawakening/entity/LinkTest.java`
- Modify: `java/src/main/java/linksawakening/entity/Link.java`

- [ ] **Step 1: Write failing collision-bit tests.** Add tests using `OverworldCollision.setRoom(...)` and a ROM blocking object at the appropriate leading-edge cell, then call `update()` and assert `romCollisionType()` contains the source bit for the attempted direction (`UP=$01`, `DOWN=$02`, `LEFT=$04`, `RIGHT=$08`). Add a second assertion that a new `update()` with no blocked probe clears the previous byte; do not invent a `setPointBlocked` API solely for the test.

  ```java
  link.setDirection(Link.DIRECTION_RIGHT);
  link.setPixelPosition(32, 32);
  // Place a ROM-solid object in the cell sampled by Link's right edge.
  link.update();
  assertEquals(0x08, link.romCollisionType());

  // Clear that room cell before the next update.
  link.update();
  assertEquals(0, link.romCollisionType());
  ```

- [ ] **Step 2: Run the focused test and verify it fails.**

  Run: `gradle -p java test --tests linksawakening.entity.LinkTest`

  Expected: FAIL because `romCollisionType()` and the accumulation do not exist.

- [ ] **Step 3: Implement the minimal source-shaped byte.** Add a private `romCollisionType` field, reset it at the beginning of `update()`, set the direction bit in `tryMoveAxis` immediately before returning for a blocked leading edge, and expose a read-only `romCollisionType()` accessor. Use the source `AddedCollisionType` mapping (`UP 0x01`, `DOWN 0x02`, `LEFT 0x04`, `RIGHT 0x08`); do not change movement acceptance.

- [ ] **Step 4: Run the focused test and verify it passes.**

  Run: `gradle -p java test --tests linksawakening.entity.LinkTest`

  Expected: PASS, including the existing Link movement tests.

- [ ] **Step 5: Commit the Link signal.**

  ```bash
  git add java/src/main/java/linksawakening/entity/Link.java \
      java/src/test/java/linksawakening/entity/LinkTest.java
  git commit -m "feat: expose ROM Link collision type"
  ```

### Task 3: Port the Ghini hidden/visible state machine in isolation

**Files:**
- Test: `java/src/test/java/linksawakening/world/GhiniMotionTest.java`
- Modify: `java/src/main/java/linksawakening/world/GhiniMotion.java`

- [ ] **Step 1: Write failing state-machine tests.** Create package-private tests using a supported pair or rectangle definition and a deterministic random-byte supplier. Cover:
  - `$10`/`$11` initialize hidden while `$12` initializes visible;
  - hidden entities keep variant `-1` when far away or when collision type is zero;
  - a close hidden entity with nonzero collision type changes internal state to visible but returns variant `-1` on that tick;
  - the next visible tick has the ROM animation variant and advances Z/position while private countdown `$30` suppresses target refresh; and
  - speed `$F4` leaves the normal pair unflipped while speed `$0C` selects X flip, with the source XOR against the base attribute.

  Example state assertions:

  ```java
  GhiniMotion motion = new GhiniMotion();
  motion.initialize(0, 0x10);
  RoomEntity hidden = motion.advance(entity(0x10, 0x50, 0x50), 0,
      0x10, 0x50, 0x50, 0, () -> 0);
  assertEquals(-1, hidden.spriteVariant());
  assertTrue(motion.hidden(0));

  RoomEntity revealFrame = motion.advance(hidden, 1,
      0x58, 0x58, 0x50, 0x01, () -> 0);
  assertEquals(-1, revealFrame.spriteVariant());
  assertFalse(motion.hidden(0));
  assertEquals(0x30, motion.privateCountdown2(0));
  ```

- [ ] **Step 2: Run the new focused test and verify it fails.**

  Run: `gradle -p java test --tests linksawakening.world.GhiniMotionTest`

  Expected: FAIL because the new state-aware overload and accessors do not exist.

- [ ] **Step 3: Implement the smallest state extension.** Preserve the existing ordinary-flight math. Add per-slot `state` and source hidden initialization, a state-aware `advance` accepting entity type, Link X/Y, collision type, and random bytes, and accessors for hidden state/private countdown used by runtime tests. Use unsigned-byte absolute distance for the `$10` windows, preserve render-before-state-update by returning hidden presentation on the reveal tick, and select the giant rectangle orientation from source X-speed sign without applying a second renderer flip. Keep the ordinary compatibility overload for existing tests, and make the runtime's entity-init path carry the loader-provided `$12` Z `$10`.

- [ ] **Step 4: Run the focused test and verify it passes.**

  Run: `gradle -p java test --tests linksawakening.world.GhiniMotionTest`

  Expected: PASS with deterministic ROM timing and no changes to unrelated motion classes.

- [ ] **Step 5: Commit the isolated motion state.**

  ```bash
  git add java/src/main/java/linksawakening/world/GhiniMotion.java \
      java/src/test/java/linksawakening/world/GhiniMotionTest.java
  git commit -m "feat: model Ghini hidden state"
  ```

### Task 4: Add Ghini collision table entries and runtime dispatch

**Files:**
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

- [ ] **Step 1: Write failing combat/runtime tests.** Add shipped-ROM-backed runtime cases for `$10`, `$11`, and `$12` that assert:
  - health is `$08` and contact damage is `$08`;
  - `$11` uses the big hitbox while `$10`/`$12` use the normal hitbox;
  - a hidden `$10` cannot damage Link or receive sword damage before reveal;
  - `$11` enters the existing DYING path after a lethal sword hit; and
  - all three skip the ground-interaction callback as source no-ground entities.

  ```java
  assertTrue(RoomEntityCombatRules.supportsEnemyCollision(0x11));
  assertEquals(0x08, RoomEntityCombatRules.initialHealth(0x10));
  assertEquals(0x08, RoomEntityCombatRules.contactDamage(0x11));
  assertTrue(RoomEntityCombatRules.overlapsLink(giant, 0x50, 0x50));
  ```

- [ ] **Step 2: Run the focused tests and verify they fail.**

  Run: `gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest`

  Expected: FAIL because the type table and runtime dispatch do not recognize `$10`/`$11`, and the hidden gate is absent.

- [ ] **Step 3: Implement the source table and dispatch changes.** Add `$10`/`$11` constants and entries to `RoomEntityCombatRules`, use the big dimensions only for `$11`, add all three to bank-$04 shared recoil/no-ground handling, initialize `GhiniMotion` for all three, pass the stored Link collision type to its state-aware `advance`, and suppress combat for hidden slots. Honor the static no-ground option in addition to dynamic overrides. Preserve the existing status/variant reconstruction so `-1` is rendered as hidden and rectangle variants remain valid; select the giant's pre-oriented rectangle list while leaving the generic entity flip bits clear so `EntityRenderLayer` does not flip the list a second time.

- [ ] **Step 4: Run the focused tests and verify they pass.**

  Run: `gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest`

  Expected: PASS, including existing enemy, recoil, death, and pickup cases.

- [ ] **Step 5: Commit the runtime slice.**

  ```bash
  git add java/src/main/java/linksawakening/world/RoomEntityCombatRules.java \
      java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
      java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
  git commit -m "feat: run ROM Ghini entities"
  ```

### Task 5: Wire collision type through RoomSession and Main

**Files:**
- Test: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Test: `java/src/test/java/linksawakening/MainArchitectureTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/Main.java`

- [ ] **Step 1: Write failing boundary tests.** Add a RoomSession test that calls the new tick overload with a nonzero collision type and verifies a nearby hidden Ghini becomes visible after the handler pass while its current snapshot remains hidden for that frame. Add a Main architecture assertion that the entity tick call includes `link.romCollisionType()` after the Link update and before the entity runtime call.

- [ ] **Step 2: Run the boundary tests and verify they fail.**

  Run: `gradle -p java test --tests linksawakening.world.RoomSessionTest --tests linksawakening.MainArchitectureTest`

  Expected: FAIL because the overload and Main wiring are absent.

- [ ] **Step 3: Implement compatible overloads.** Keep all existing `RoomSession.tickEntitiesWithProjectileEvents` overloads defaulting collision type to zero. Add the explicit collision-type overload, store it in the runtime before ticking, and pass `link.romCollisionType()` from `Main`. Mask and validate it as an unsigned byte. Do not move the existing combat/projectile order. The new argument must be carried into `RoomEntityRuntime.tickInternal` rather than held as stale session state across frames.

- [ ] **Step 4: Run the boundary tests and verify they pass.**

  Run: `gradle -p java test --tests linksawakening.world.RoomSessionTest --tests linksawakening.MainArchitectureTest`

  Expected: PASS with the existing hookshot/projectile boundary assertions unchanged.

- [ ] **Step 5: Commit the live wiring.**

  ```bash
  git add java/src/main/java/linksawakening/world/RoomSession.java \
      java/src/main/java/linksawakening/Main.java \
      java/src/test/java/linksawakening/world/RoomSessionTest.java \
      java/src/test/java/linksawakening/MainArchitectureTest.java
  git commit -m "feat: wire Ghini Link collision state"
  ```

### Task 6: Fresh verification and roadmap handoff

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Run the full verification before documentation.**

  ```bash
  gradle -p java clean test
  git diff --check
  git status --short --branch
  ```

  Expected: `BUILD SUCCESSFUL`, no whitespace errors, and only the intended spec/plan files plus any implementation changes awaiting the roadmap edit.

- [ ] **Step 2: Append the verified roadmap entry.** Record the exact bank-$04 labels, `$10/$11/$12` behavior, source collision-type wiring, combat/no-ground/recoil coverage, and explicit bomb/object-destruction and other Ghini special deferrals. Do not claim the full reconstruction complete.

- [ ] **Step 3: Re-run verification after the documentation edit.**

  ```bash
  gradle -p java test
  git diff --check
  ```

  Expected: `BUILD SUCCESSFUL` and a clean whitespace check.

- [ ] **Step 4: Commit the verified slice.**

  ```bash
  git add docs/reconstruction-roadmap.md \
      docs/superpowers/specs/2026-08-06-ghini-family-runtime-design.md \
      docs/superpowers/plans/2026-08-06-ghini-family-runtime.md
  git commit -m "feat: complete ROM Ghini family slice"
  ```

- [ ] **Step 5: Inspect the final commit and report evidence.** Run `git show --stat --oneline HEAD` and report the commit ID, fresh test result, and the remaining bomb/room-script/entity-handler gaps.

## Plan self-review

- **Spec coverage:** Display decoding is Task 1; Link collision bits are Task 2; hidden/reveal/visible motion and giant orientation are Task 3; combat, no-ground, recoil, and hidden collision gates are Task 4; live RoomSession/Main ordering is Task 5; verification and explicit deferrals are Task 6.
- **Initialization coverage:** Task 1 covers `EntityInitGhini`'s `$12` Z `$10` loader effect; the private state-3 `$01` write is documented as source metadata because it has no observable effect in the ordinary handler's disabled flash branch.
- **Placeholder scan:** The plan contains no TBD/TODO steps or unspecified “handle edge cases” work. Every production change names a file, method boundary, and expected test command.
- **Type consistency:** The plan consistently uses `romCollisionType`, the three entity IDs `$10/$11/$12`, `GhiniMotion` state-aware `advance`, and the existing `RoomEntityRuntime`/`RoomSession` tick boundary.
