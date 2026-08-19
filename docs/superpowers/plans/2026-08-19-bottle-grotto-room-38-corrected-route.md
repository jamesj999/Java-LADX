# Bottle Grotto Room `$38` Corrected Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue ordered Bottle Grotto play from the Stone Beak through the real crystal-switch route and collect room `$38`'s Small Key.

**Architecture:** Extend the single uninterrupted `RoomTransitionCoordinatorTest` trace using existing collision, transition, combat, chest, and crystal-switch APIs. Preserve ROM-backed room data and add production code only if a failing test demonstrates a shared runtime defect.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed room/entity/chest data.

---

### Task 1: Prove the collision-valid route to room `$38`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Continue `freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder` after the Stone Beak chest teardown with the same session, Link, player state, and frame counter.
- [ ] Cross `$2E -> $30 -> $31 -> $32 -> $33` using `walkToAndCrossIndoorBoundary`, asserting every room id and retaining zero Small Keys.
- [ ] Reach room `$33`'s live crystal switch through collision, hit it with the ordinary sword/entity path, and tick its nine-stage animation until the shared switch state makes the required `$34` partition route passable.
- [ ] Cross `$33 -> $34 -> $39 -> $38` using collision-aware movement; assert room `$38` has event `$00`, chest `$A0` at `$43`, Moblin Sword `$14` at `$62`, and crystal switch at `$45`.
- [ ] Run `cd java && gradle test --tests linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder --rerun-tasks` and retain any source-backed RED before changing production code.

### Task 2: Complete room `$38` through shared runtime paths

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only if a shared RED requires it: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify only if a shared RED requires it: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

- [ ] Reach the Moblin through the collision map, initialize it, strike it with live Sword collision boxes, and use bounded recovery/death ticks until disabled.
- [ ] Assert the optional Moblin's death leaves room event `$00` and the chest unchanged.
- [ ] Reach chest `$43`, open it through `tryOpenChest`, and assert `CHEST_SMALL_KEY`, room completion persistence, reward event, ROM-selected dialog, and bounded chest-entity teardown.
- [ ] Apply the emitted reward and assert the dungeon Small Key count changes from zero to one.
- [ ] Reach and strike room `$38`'s crystal switch through normal combat interaction; tick the full staged animation and assert representative `$DB` collision changes with the shared switch state.
- [ ] If a production defect appears, first add the narrowest focused failing regression in the owning test class, run it RED, implement the smallest shared correction, then rerun it GREEN.
- [ ] Re-run the ordered test and relevant focused `RoomSession`, entity-motion/combat, chest, crystal-switch, and transition tests.

### Task 3: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-room-38-corrected-route.md`

- [ ] Record the corrected route, source labels, observed runtime behavior, exact fresh test totals, and the next accessible gameplay frontier in the roadmap.
- [ ] Run the focused subsystem matrix with `--rerun-tasks`, then `cd java && gradle clean test` and count failures/errors/skips from `build/test-results/test/*.xml`.
- [ ] Run `git diff --check` and inspect `git diff --stat` plus the full relevant diff.
- [ ] Obtain independent source-fidelity review, fix every Critical or Important issue, and rerun affected checks.
- [ ] Obtain independent code-quality review only after source compliance passes, fix every Critical or Important issue, and rerun affected checks.
- [ ] Mark each checkbox complete only after fresh evidence exists, then commit the implementation, tests, plan, and roadmap on the current branch.
