# Entity conveyor interaction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> (recommended) to implement this plan task-by-task.

**Goal:** Restore the ROM's generic entity conveyor nudge through the live
room-session boundary, including the ROM options exclusion.

**Architecture:** Load `Options1ForEntity` into `RomTables`, expose the exact
`func_003_7E0E` padded-coordinate physics lookup from `OverworldCollision`,
and inject a ground-interaction callback into `RoomEntityRuntime`. Keep the
callback responsible for room physics and source timing while the runtime
owns handler ordering.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly and shipped ROM.

---

### Task 1: Add failing source-boundary regressions

**Files:**

- Modify: `java/src/test/java/linksawakening/physics/OverworldCollisionTest.java`.
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`.
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`.

- [x] **Step 1: Test the exact padded ground coordinate.** Put a known object
  at the cell selected by `entityX - 1`, `entityY - 7`, select an indoor ROM
  physics table, and assert the new lookup returns its conveyor flag.

- [x] **Step 2: Test the runtime ground-interaction boundary.** Inject a
  deterministic callback into an active static entity and assert it is called
  after the runtime accepts the frame update.

- [x] **Step 3: Test an ordinary entity on a real ROM conveyor.** Load
  overworld room `$2F`, compare an all-`$01` passable floor with an all-`$CF`
  conveyor floor, and assert its Octorok receives the `$F4` movement-table
  nudge at frame `$04`.

- [x] **Step 4: Test a no-ground entity on a real ROM conveyor.** Load indoor
  room `$05`, compare the same floors for a resident Spark, and assert its
  positions remain identical because its ROM options byte has bit `$10` set.

- [x] **Step 5: Run focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.physics.OverworldCollisionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.world.RoomSessionTest
```

Expected: the new API/tests fail to compile or the session comparisons fail
until the source-backed implementation exists.

### Task 2: Implement the ROM-backed conveyor boundary

**Files:**

- Modify: `java/src/main/java/linksawakening/rom/RomTables.java`.
- Modify: `java/src/main/java/linksawakening/physics/OverworldCollision.java`.
- Add: `java/src/main/java/linksawakening/world/RoomEntityGroundInteraction.java`.
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`.
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`.

- [x] **Step 1: Load the ROM options table.** Read bank `$03` address `$42F1`
  for 256 bytes and expose the unsigned `Options1ForEntity` value.

- [x] **Step 2: Add the exact entity-ground physics lookup.** Reuse the
  active physics table and padded room stride, but use `entityX - 1` and
  `entityY - 7` as `func_003_7E0E` does.

- [x] **Step 3: Add the runtime callback boundary.** Invoke the callback only
  for active, non-initializing entities after their family motion update;
  preserve the existing behavior when no callback is installed.

- [x] **Step 4: Implement conveyor movement in `RoomSession`.** Honor the
  options bit, Z gate, four-frame cadence, `$F0-$F7` table index, signed
  movement, and byte wrapping.

- [x] **Step 5: Run focused tests and verify GREEN.** Re-run the three focused
  test classes and confirm the new ordinary/no-ground comparisons pass.

### Task 3: Document and checkpoint

**Files:**

- Modify: `docs/reconstruction-roadmap.md`.
- Modify: this plan to mark completed steps.

- [x] **Step 1: Record the verified conveyor behavior.** Add a dated roadmap
  entry that identifies the exact ROM tables, cadence, and options exclusion;
  leave the rest of ground interaction explicitly pending.

- [x] **Step 2: Run final verification.** From `java/`, run
  `gradle clean test`; from the worktree root run `git diff --check` and
  `git status --short --branch`.

- [x] **Step 3: Commit the checkpoint.** Commit the intended files with:

```bash
git add java/src/main/java/linksawakening/rom/RomTables.java \
  java/src/main/java/linksawakening/physics/OverworldCollision.java \
  java/src/main/java/linksawakening/world/RoomEntityGroundInteraction.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/test/java/linksawakening/physics/OverworldCollisionTest.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  java/src/test/java/linksawakening/world/RoomSessionTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-entity-conveyor-interaction.md \
  docs/superpowers/specs/2026-08-05-entity-conveyor-interaction-design.md
git commit -m "feat: apply ROM entity conveyor movement"
```
