# Stalfos Evasive Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans (recommended) to implement this plan task-by-task.

**Goal:** Implement the ROM-backed rendering and normal/airborne movement of
Stalfos Evasive `$1E`, including held A/B input in the live entity tick.

**Architecture:** Decode bank `$15`'s two Evasive display-list pairs in the
existing sprite catalog. Keep private state, inertia, countdown, speeds, and
fixed-point accumulators in a focused `StalfosEvasiveMotion` class; let
`RoomEntityRuntime` preserve shared status/combat ordering and select the
fleeing display definition. `RoomSession` passes a held action-button flag from
`Main` without turning the host into a Game Boy input emulator.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly and shipped ROM.

---

### Task 1: Add source-boundary regression tests

**Files:**

- Modify: `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java`.
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`.

- [x] **Step 1: Add the Evasive sprite mapping assertion.** In the existing
  handler-mapping test, call `forEntityType(0x1E, INDOORS_A)` and assert
  `Shape.PAIR`, bank `$15`, address `$4E7D`, three variants, and initial
  variant zero. Add a separate catalog method assertion for the fleeing pair
  at bank `$15:$4E8E`, with two variants and initial variant zero.

- [x] **Step 2: Add the random-walk test.** Create an active or initialized
  type `$1E` entity with a three-variant pair definition, tick with deterministic
  random bytes, and assert the ROM speed choice `$06`, fixed-point position
  movement after four frames, and `(frame >> 3) & 1` normal animation.

- [x] **Step 3: Add the held-action jump test.** Set the runtime's action flag,
  tick an Evasive entity inside the ROM `[-$24,$23]` X/Y window, and assert
  inertia one, transition countdown `$08`, speed Z `$15`, variant `$02`, and
  the first airborne Z update on the next frame. Tick until landing and assert
  X/Y speeds `$08`, inertia zero, and private countdown `$10`.

- [x] **Step 4: Run the focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest \
  --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the new Evasive mapping assertion fails because type `$1E` is
currently unsupported, and the motion assertions fail because no Evasive
handler is dispatched.

### Task 2: Implement Evasive display lists and motion

**Files:**

- Modify: `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`.
- Add: `java/src/main/java/linksawakening/world/StalfosEvasiveMotion.java`.
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`.
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`.

- [x] **Step 1: Decode the two ROM pairs.** Map type `$1E` to
  `decodePair(0x1E, 0x15, 0x4E7D, 3, 0)`. Add
  `forStalfosEvasiveState(int privateState1)` that returns the normal pair
  for zero and `decodePair(0x1E, 0x15, 0x4E8E, 2, 0)` for nonzero, validating
  the state argument.

- [x] **Step 2: Implement the motion state.** Store `privateState1`,
  `privateCountdown1`, `inertia`, X/Y/Z speeds, three accumulators, and an
  initialization bit per slot. Implement signed fixed-point addition, the
  ROM five-byte direction table, collision-axis reversal, the held-action
  jump window, vector-length `$12` jump-away speeds, airborne Z update, and
  landing writes `$08/$08/$10`.

- [x] **Step 3: Dispatch the handler in runtime order.** Initialize the motion
  during the existing INIT branch; advance it after shared recoil and before
  the generic ground callback. While private state 1 is nonzero, select the
  fleeing definition and its two-frame variant. Clear motion state from both
  entity-clear paths. Add type `$1E` to the normal enemy collision family and
  its ROM health/contact defaults where the fallback table is used.

- [x] **Step 4: Run focused tests and verify GREEN.** Re-run the two focused
  test classes. Expected: all Evasive sprite and movement assertions pass,
  while the existing burn-expiry test also sees a supported normal Evasive
  definition.

### Task 3: Wire held A/B into the live entity boundary

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`.
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`.
- Modify: `java/src/main/java/linksawakening/Main.java`.
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`.

- [x] **Step 1: Add the runtime/session flag.** Add a package-visible runtime
  setter and a session setter/default for `actionButtonsHeld`; copy the flag
  into the runtime before every entity tick and preserve it when the follower
  synchronization recreates the runtime.

- [x] **Step 2: Pass live input.** At the existing `Main` AnimateEntities call,
  compute `inputState.isDown(inputConfig.aKey()) ||
  inputState.isDown(inputConfig.bKey())` and set that flag before the session
  tick. Existing callers that do not set it retain false.

- [x] **Step 3: Add a session-boundary assertion.** Use a real room/session
  fixture or the existing test injection path to hold the flag and verify the
  Evasive entity enters the jump state through `tickEntitiesWithProjectileEvents`,
  not only through direct runtime calls.

- [x] **Step 4: Run focused integration tests.** Run:

```bash
gradle test --tests linksawakening.world.RoomSessionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: the session-held-action test passes and previous entity tests remain
green.

### Task 4: Verify, document, and checkpoint

**Files:**

- Modify: `docs/reconstruction-roadmap.md`.
- Modify: `docs/superpowers/specs/2026-08-05-stalfos-evasive-design.md`.
- Modify: `docs/superpowers/plans/2026-08-05-stalfos-evasive.md`.

- [x] **Step 1: Record the verified scope.** Add a dated roadmap entry for
  bank `$15` Evasive display selection, random walk, jump/landing, and live
  A/B input. Explicitly retain clone, fleeing side effects, audio/VFX, and
  generic ground-status gaps as pending.

- [x] **Step 2: Run final verification.** From `java/`, run `gradle clean test`.
  From the worktree root, run `git diff --check` and
  `git status --short --branch`.

- [x] **Step 3: Commit the checkpoint.** Commit the intended source, tests,
  spec, plan, and roadmap files with:

```bash
git add java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java \
  java/src/main/java/linksawakening/world/StalfosEvasiveMotion.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/RoomEntityCombatRules.java \
  java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/main/java/linksawakening/Main.java \
  java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  java/src/test/java/linksawakening/world/RoomSessionTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/specs/2026-08-05-stalfos-evasive-design.md \
  docs/superpowers/plans/2026-08-05-stalfos-evasive.md
git commit -m "feat: add Evasive Stalfos runtime"
```
