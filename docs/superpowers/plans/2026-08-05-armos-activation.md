# Armos Statue activation and active combat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Armos Statue's ROM harmless-to-active transition and active combat behavior observable through the live Java room runtime.

**Architecture:** Keep Armos state and transition detection in `ArmosMotion.Update`; project the source physics/flash transitions into the existing per-slot arrays in `RoomEntityRuntime`. Admit Armos to the shared combat resolver only after state 2, while ROM-backed health/contact values continue to come from `RomEnemyCombatTables`.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX assembly source.

---

### Task 1: Add failing Armos activation/combat tests

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Reference: `LADX-Disassembly/src/code/entities/06_armos_statue.asm:10-80`
- Reference: `LADX-Disassembly/src/data/entities/physics_flags.asm:24-28`
- Reference: `LADX-Disassembly/src/data/entities/hitbox_flags.asm:12-18`
- Reference: `LADX-Disassembly/src/data/entities/options1.asm:12-18`
- Reference: `LADX-Disassembly/src/data/entities/health_groups.asm:18-20`
- Reference: `LADX-Disassembly/src/data/entities/damages.asm:8-16`

- [x] **Step 1: Add initial harmless/flash tests**

Extend the existing Armos test to assert `physicsFlags(0) == 0x92` before
wake. After a wake tick, assert state 1, countdown `$30`, flash countdown
`$18`, and unchanged `$92` flags. Call `resolveCombat` with an overlapping
sword and Link position and assert it returns no event while state is below 2.

- [x] **Step 2: Add active transition and combat tests**

After the existing countdown loop, assert `physicsFlags(0) == 0x12` and then
call `resolveCombat` with an overlapping sword. Assert one sword-hit event,
positive enemy damage, health reduced from the fallback four points to three,
and the normal `$0A` ignore-hit countdown. Use an odd collision frame and the
entity's current coordinates to assert active Link contact reports fallback
damage `$10`.

- [x] **Step 3: Add active recoil test**

After an active Armos sword hit, tick one frame with no background collision
and assert the entity moves through the existing recoil path and its ignore
countdown decrements. This proves Armos is included in `ApplyRecoilIfNeeded_06`
without adding background behavior.

- [x] **Step 4: Run the focused tests and confirm RED**

Run from `java/`:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected result: the new flag assertion fails because Armos flags are not
initialized, and the pre-activation combat assertion fails because Armos is
not currently admitted to the resolver.

### Task 2: Carry transition events from ArmosMotion

**Files:**
- Modify: `java/src/main/java/linksawakening/world/ArmosMotion.java`
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [x] **Step 1: Add an `Update` record and transition booleans**

Change `advance` to return `Update(RoomEntity entity, boolean woke,
boolean activated)`. Capture the previous state before handler logic, set
`woke` when state changes `0 -> 1`, and set `activated` when it changes `1 -> 2`.
Preserve all existing countdown, position, speed, random-table, and sprite
behavior inside the returned entity.

- [x] **Step 2: Add `isActive`**

Expose `boolean isActive(int slot)` returning `initialized[slot] && state[slot] >= 2`.
Keep `clear` resetting state and initialization exactly as before.

- [x] **Step 3: Run focused tests and confirm only integration failures remain**

Run `gradle test --tests linksawakening.world.RoomEntityRuntimeTest`.
Expected result: ArmosMotion compiles, while runtime flag/combat assertions
remain red until Task 3.

### Task 3: Project Armos flags and admit active combat

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [x] **Step 1: Initialize and transition physics flags**

Set Armos' runtime physics flags to `$92` when its slot is initialized. When
the Armos update reports `woke`, set `enemyFlashCountdown[slot] = 0x18`.
When it reports `activated`, clear bit `$80` with
`enemyPhysicsFlags[slot] &= 0x7F`, clear any runtime sword-ignore gate, and
leave the active flags at `$12`. Do not change the existing background
collision arguments.

- [x] **Step 2: Add Armos to shared combat only when active**

Include `$0F` in `RoomEntityCombatRules.supportsEnemyCollision`, fallback
contact damage `$10`, and fallback initial health `$04`. In
`resolveCombat`, skip Armos unless `armosMotion.isActive(slot)` is true. This
keeps state 0/1 out of both sword and Link combat while letting state 2 use the
existing sword damage, contact event, and health paths.

- [x] **Step 3: Include Armos in bank-$06 recoil**

Add `$0F` to `usesBank6Recoil`. The existing `applyEnemyRecoilIfNeeded` already
uses the non-roaming bank-$06 collision policy, so blocked recoil must not
clear the countdown.

- [x] **Step 4: Run focused tests and confirm GREEN**

Run `gradle test --tests linksawakening.world.RoomEntityRuntimeTest`.
Expected result: all runtime tests pass, including the new Armos assertions.

### Task 4: Verify and checkpoint

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Review: all files changed by Tasks 1-3

- [x] **Step 1: Run full verification**

From `java/`, run `gradle clean test`. Expected result: BUILD SUCCESSFUL.

- [x] **Step 2: Run repository checks**

From the worktree root, run `git diff --check` and `git status --short`.
Expected result: no whitespace errors and only intended files changed.

- [x] **Step 3: Update the roadmap**

Record that Armos now wakes with the source flash/countdown, becomes active
with source flags, reaches active sword/contact combat and bank-$06 recoil,
and that background interaction/final-Link-position details remain pending.

- [x] **Step 4: Commit the checkpoint**

Commit the documents and implementation with:
`git commit -m "feat: complete Armos activation flags"`.
