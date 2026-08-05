# Water Tektite water collision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> (recommended) to implement this plan task-by-task.

**Goal:** Preserve Water Tektite's ROM shallow/deep-water collision exception
through the live room-session boundary.

**Architecture:** Expose the physics byte at the already-computed background
collision point, then add a type-specific pass-through only for Water Tektite
and physics flags `$05/$07`. Keep ordinary `pointBlocked` behavior for every
other entity and terrain type.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly and shipped ROM.

---

### Task 1: Add failing collision regressions

**Files:**

- Modify: `java/src/test/java/linksawakening/physics/OverworldCollisionTest.java`.
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`.

- [x] **Step 1: Test the padded point physics lookup.** Place a known object
  in the padded room array, select the relevant physics table, and assert the
  new lookup returns its ROM flag at the same coordinate used by background
  collision.

- [x] **Step 2: Test a real Water Tektite room on deep water.** Load indoor
  map `$00`, room `$65`, replace the active 10x8 object cells with deep-water
  object `$0E`, advance through the resident Water Tektite's initial state
  frames, and assert it moves from its original position.

- [x] **Step 3: Run focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.physics.OverworldCollisionTest \
  --tests linksawakening.world.RoomSessionTest
```

Expected: the new lookup does not compile until implemented, and after the
minimal test shape is corrected the Water Tektite session assertion fails
because deep water is still treated as blocked.

### Task 2: Restore the bank-$07 water exception

**Files:**

- Modify: `java/src/main/java/linksawakening/physics/OverworldCollision.java`.
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`.

- [x] **Step 1: Add the coordinate physics lookup.** Reuse the existing cell
  conversion and padded-buffer indexing; return the active table's ROM flag.

- [x] **Step 2: Apply the narrow Water Tektite rule.** In the session
  collision callback, pass only shallow/deep water for entity `$99`; delegate
  all other cases to `pointBlocked`.

- [x] **Step 3: Run focused tests and verify GREEN.** Run the two focused test
  classes, then confirm no unrelated collision or room-session tests regress.

### Task 3: Document and checkpoint

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the Water Tektite bullet and
  before `## Next entity increments`.
- Modify: this plan to mark completed steps.

- [x] **Step 1: Record the verified exception.** State that Water Tektite now
  passes `$05/$07` and retains ordinary blocking elsewhere; leave broader
  ground/pit/conveyor behavior pending.

- [x] **Step 2: Run final verification.** From `java/`, run
  `gradle clean test`; from the worktree root run `git diff --check` and
  `git status --short --branch`.

- [x] **Step 3: Commit the checkpoint.** Commit the intended files with:

```bash
git add java/src/main/java/linksawakening/physics/OverworldCollision.java \
  java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/test/java/linksawakening/physics/OverworldCollisionTest.java \
  java/src/test/java/linksawakening/world/RoomSessionTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-water-tektite-water-collision.md \
  docs/superpowers/specs/2026-08-05-water-tektite-water-collision-design.md
git commit -m "fix: let Water Tektites cross water"
```
