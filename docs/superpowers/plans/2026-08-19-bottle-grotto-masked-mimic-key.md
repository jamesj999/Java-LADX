# Bottle Grotto Masked-Mimic Key Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue ordered play through room `$34`'s Masked-Mimic event and collect Bottle Grotto's second Small Key.

**Architecture:** Extend the existing uninterrupted regression using live room initialization, Mimic input/options, combat, death/event, key-drop motion, and pickup paths. Correct only shared source-backed behavior exposed by the failing test.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed tables.

---

### Task 1: Establish the room `$34` encounter state

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] After entering `$34`, assert event `$81`, exactly two Masked Mimics `$8F`, one droppable rupee `$2E`, and carried switch state `$02`.
- [ ] Tick the room through normal initialization and assert the Mimics remain kill-all participants while the rupee does not block event completion.
- [ ] Run the focused ordered-play test and capture the first genuine RED.

### Task 2: Defeat both Mimics and collect the key

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only as a failing test requires: `java/src/main/java/linksawakening/world/MaskedMimicMotion.java`
- Modify only as a failing test requires: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify only as a failing test requires: `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`
- Modify only as a failing test requires: `java/src/main/java/linksawakening/world/RoomSession.java`

- [ ] Drive each Mimic's live direction/options and strike its vulnerable side through `resolveEntityCombat` until the normal death state completes.
- [ ] Assert event `$81` resolves only after both Mimics die despite the loaded droppable rupee.
- [ ] Wait for the ordinary `$30` key to land, collect it through `collectEntityIfNeeded`, and assert exactly two Small Keys.
- [ ] Implement the smallest shared ROM-shaped fix for any failed behavior and re-run the focused regression to GREEN.

### Task 3: Verify, review, and record the next frontier

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-masked-mimic-key.md`

- [ ] Record exact source labels, verified behavior, and next accessible gameplay gap.
- [ ] Run focused Masked-Mimic, room-session, transition, and event tests with `--rerun-tasks`.
- [ ] Run `gradle clean test`, count XML test results, and run `git diff --check`.
- [ ] Request source-fidelity and code-quality review and address all Critical and Important findings.
- [ ] Mark plan steps complete only after fresh evidence exists.
