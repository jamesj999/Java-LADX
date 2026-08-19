# Bottle Grotto Crystal-Switch Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue ordered play through Bottle Grotto room `$33`'s ROM-authored crystal switch and newly passable route.

**Architecture:** Extend the existing uninterrupted integration test and use live room transition, combat, VBlank animation, and collision paths. Add production behavior only where a failing test proves the Java port diverges from the disassembly.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed room and entity tables.

---

### Task 1: Establish the room `$33` ordered-play regression

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [x] Extend `freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder` after the key pickup to walk east into room `$33` through collision and the transition coordinator.
- [x] Assert the room id, ROM-loaded crystal switch `$66`, owl statue, Spark, switch-block objects, and initial global switch state `$00`.
- [x] Run `gradle test --tests linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder --rerun-tasks`; the integration exposed the required source initialization tick, then passed through the live path without a production divergence.

### Task 2: Exercise the live crystal-switch path

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only if the red test requires it: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify only if the red test requires it: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

- [x] Move Link to a collision-valid sword-contact position and hit entity `$66` through `resolveEntityCombat`.
- [x] Tick the entity handler and assert wave SFX `$0E` plus animation stage `$01`.
- [x] Advance `tickGameplayVBlank` through the source stages and assert final global state `$02` after nine VBlank calls from stage `$01` to idle.
- [x] Assert a ROM switch-block collision point changes passability with the synchronized state and use that route to reach room `$34`.
- [x] Preserve the existing shared implementation after the integration test found no production divergence.
- [x] Re-run the focused regression and require `BUILD SUCCESSFUL`.

### Task 3: Verify and record the gameplay frontier

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-crystal-switch.md`

- [x] Record the exact verified room route, source labels, switch behavior, and next gameplay gap.
- [x] Mark completed plan steps only after their verification commands pass.
- [x] Run `gradle test --tests linksawakening.world.SwitchBlockAnimationTest --tests linksawakening.world.SwitchBlockLinkInteractionTest --tests linksawakening.world.RoomSessionTest --tests linksawakening.world.RoomTransitionCoordinatorTest --rerun-tasks`.
- [x] Run `gradle clean test`, `git diff --check`, and inspect the complete diff.
- [x] Request source-fidelity and code-quality review; both approved with no findings.
