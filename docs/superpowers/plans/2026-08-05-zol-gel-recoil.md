# Zol/Gel bank-$06 recoil Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Zol ($1B) and Gel ($1C) use the disassembly's shared bank-$06 sword-recoil path in the live Java room runtime.

**Architecture:** Reuse EnemyRecoilMotion and the existing RoomEntityRuntime combat/tick ordering. Admit both types through the existing bank-$06 predicate; do not duplicate recoil state or alter ZolGelMotion's movement, clinging, or split logic. Regression tests prove configuration, pre-movement application, blocked-step persistence, and split cleanup.

**Tech Stack:** Java, JUnit 5, Gradle, shipped ROM fixture, LADX disassembly.

---

### Task 1: Add failing Zol/Gel recoil regressions

**Files:**
- Modify: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java near the existing bank-$06 recoil tests and Zol split tests.

- [x] **Step 1: Extend the bank-$06 configuration matrix**

Change the existing type list in bankSixNormalEnemiesConfigureTheRomSharedSwordRecoil from:

    int[] types = {0x19, 0x0D, 0x15, 0x1A, 0x16, 0x17};

to:

    int[] types = {0x19, 0x0D, 0x15, 0x1A, 0x16, 0x17, 0x1B, 0x1C};

- [x] **Step 2: Add the pre-movement test**

Add a test that loops over 0x1B and 0x1C, creates an ACTIVE entity at (64,64) with the shipped ROM combat tables, resolves a sword collision from (72,72) using a level-zero non-damaging attack context, ticks once with sequence(0x01), and asserts position (61,61) and ignore-hits countdown 0x09. The level-zero context keeps the one-health entities active; the source configures recoil before its damage lookup. This proves the shared recoil runs before ZolGelMotion.advance.

- [x] **Step 3: Add the blocked-step test**

Add a test that loops over 0x1B and 0x1C, resolves the same hit, ticks with a background callback blocking directions 1 and 2, and asserts position (64,64), enemyRecoilActive(0) true, and countdown 0x09. This proves the bank-$06 policy does not clear recoil on a block.

- [x] **Step 4: Add split cleanup assertions**

At the end of zolSplittingTurnsTheOriginalSlotIntoAGelAndUsesTheLastFreeSlot, assert:

    assertFalse(runtime.enemyRecoilActive(0));
    assertFalse(runtime.enemyRecoilActive(15));
    assertEquals(0, runtime.enemyIgnoreHitsCountdown(0));

- [x] **Step 5: Run the focused tests and verify RED**

Run from java/:

    gradle test --tests linksawakening.world.RoomEntityRuntimeTest

Expected: an assertion failure for Zol or Gel because the production predicate still excludes both types, not a compilation or fixture error.

### Task 2: Admit Zol/Gel to the shared bank-$06 policy

**Files:**
- Modify: java/src/main/java/linksawakening/world/RoomEntityRuntime.java at usesBank6Recoil.

- [x] **Step 1: Extend the existing predicate only**

Add these two terms to the existing return expression:

    || type == ENTITY_ZOL || type == ENTITY_GEL;

Do not add a second recoil helper or change ZolGelMotion. The current combat resolver will configure the ROM $30 vector, and the current pre-motion applyEnemyRecoilIfNeeded call will invoke the non-roaming bank-$06 policy.

- [x] **Step 2: Run the focused tests and verify GREEN**

Run from java/:

    gradle test --tests linksawakening.world.RoomEntityRuntimeTest

Expected: BUILD SUCCESSFUL, including the matrix, ordering, blocked-step, and split-cleanup assertions.

### Task 3: Document and verify the increment

**Files:**
- Modify: docs/reconstruction-roadmap.md in the Zol/Gel bullet and immediately before ## Next entity increments.

- [x] **Step 1: Update the existing Zol/Gel status**

Replace the pending recoil wording with: shared bank-$06 sword recoil is verified below; joypad-driven clinging release, background interaction, and remaining damage-state branches remain pending.

- [x] **Step 2: Add the verified increment record**

Record that Zol and Gel configure the shared $30 vector and $0A window, apply one fixed-point step before ZolGelMotion.advance, preserve recoil on a blocked bank-$06 step, and clear recoil during the existing split reset. Keep clinging-input release, background interaction, recoil smoke, and remaining damage-state branches explicitly pending.

- [x] **Step 3: Run final verification**

Run gradle clean test from java/, then git diff --check and git status --short --branch from the worktree root. Expected: BUILD SUCCESSFUL, no whitespace errors, and only intended files changed.

- [x] **Step 4: Commit the implementation checkpoint**

    git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java docs/reconstruction-roadmap.md java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java docs/superpowers/plans/2026-08-05-zol-gel-recoil.md
    git commit -m "feat: add Zol Gel bank6 recoil"
