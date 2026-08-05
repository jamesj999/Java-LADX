# Water Tektite shared recoil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans
> (recommended) to implement this plan task-by-task.

**Goal:** Mirror Water Tektite's bank-$07 shared sword-recoil path through the
live Java room entity runtime.

**Architecture:** Admit entity `$99` to `RoomEntityRuntime`'s existing shared
recoil family. Reuse `EnemyRecoilMotion` and the existing non-roaming blocked
step semantics; leave `WaterTektiteMotion` unchanged.

**Tech Stack:** Java 21, Gradle, JUnit 5, LADX disassembly.

---

### Task 1: Add the failing recoil regressions

**Files:**

- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
  beside the existing Water Tektite tests.

- [x] **Step 1: Add the active-hit test.** Use a level-zero
  `EnemyAttackContext` and ROM combat tables so the one-health entity remains
  active. Submit a sword hit, assert `$D0/$D0` recoil and `$0A` countdown,
  tick once, then assert `(64,64) -> (61,61)` and `$09`.

- [x] **Step 2: Add the blocked-step test.** Configure the same hit, provide a
  background callback that blocks directions left and up, tick once, and
  assert that position remains `(64,64)`, recoil remains active, and the
  countdown is `$09`.

- [x] **Step 3: Run the focused tests and verify RED.** From `java/`, run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: only the new Water Tektite recoil assertions fail because `$99` is
not admitted to `usesSharedRecoil`; existing Water Tektite motion/combat and
other recoil tests continue to pass.

### Task 2: Admit Water Tektite to shared recoil

**Files:**

- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
  in `usesSharedRecoil`.

- [x] **Step 1: Add the source-backed family admission.** Include
  `ENTITY_WATER_TEKTITE` beside the existing bank-$07 shared-recoil entity
  entries. Do not alter `EnemyRecoilMotion` or Water Tektite state movement.

- [x] **Step 2: Run the focused tests and verify GREEN.** Run the same focused
  Gradle test command and confirm both new regressions and the full runtime
  class pass.

### Task 3: Document and checkpoint the increment

**Files:**

- Modify: `docs/reconstruction-roadmap.md` in the Water Tektite status bullet
  and before `## Next entity increments`.
- Modify: this plan to mark completed steps.

- [x] **Step 1: Update the roadmap.** State that shared bank-$07 recoil is
  verified while water/pit/conveyor side effects and remaining damage states
  remain pending.

- [x] **Step 2: Add a dated verification entry.** Record the pre-motion
  recoil ordering, fixed-point movement, and blocked-step countdown behavior.

- [x] **Step 3: Run final verification.** From `java/`, run
  `gradle clean test`. From the worktree root, run `git diff --check` and
  `git status --short --branch`.

- [x] **Step 4: Commit the checkpoint.** Commit the intended files with:

```bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  docs/reconstruction-roadmap.md \
  docs/superpowers/plans/2026-08-05-water-tektite-recoil.md \
  docs/superpowers/specs/2026-08-05-water-tektite-recoil-design.md
git commit -m "feat: add Water Tektite shared recoil"
```
