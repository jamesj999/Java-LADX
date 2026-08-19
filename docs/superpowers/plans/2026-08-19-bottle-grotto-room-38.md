# Bottle Grotto Room `$38` Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue uninterrupted Bottle Grotto play from the room `$37` Compass through all source-authored room `$38` interactions.

**Architecture:** Extend the existing fresh-game ordered regression with collision-valid traversal and the generic room/entity/chest/switch APIs. Add production behavior only when a source-backed failing test proves a shared runtime gap; never add a room-id special case.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed room/entity/event/chest tables.

---

### Task 1: Enter and validate room `$38`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Extend `freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder` after the Compass assertions by calling the existing collision-aware `walkToAndCrossIndoorBoundary` helper with `Link.DIRECTION_RIGHT`.
- [ ] Run the ordered test with `--rerun-tasks` and require a route assertion to fail before adding any production code; use the rendered collision grid and ROM room objects to correct test assumptions.
- [ ] Assert room id `$38`, `DungeonEventsTable` value `$00`, one loaded Moblin Sword `$14` at location `$62`, one crystal switch `$66` at `$45`, chest object `$A0` at `$43`, and the source `$DB` switch blocks.
- [ ] Initialize the room through `tickEntities` and keep all waits bounded with diagnostic failure messages.

### Task 2: Resolve the Moblin and collect the Small Key

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only after a genuine RED: shared runtime files under `java/src/main/java/linksawakening/world/`

- [ ] Use collision-aware movement to place Link at a live sword-contact position for the Moblin Sword; do not teleport between semantic interaction points.
- [ ] Apply ROM-table sword damage through `resolveEntityCombat`, respecting invulnerability/recovery, and use a bounded wait for the ordinary death lifecycle.
- [ ] Assert the Moblin becomes disabled while room event `$00` remains unchanged.
- [ ] Find a collision-valid path to the last passable position below chest `$43`, perform exactly one upward contact step, and assert the production interaction location is `$43`.
- [ ] Open the chest through `tryOpenChest(..., Link.DIRECTION_UP, true, swordLevel)` and assert `CHEST_SMALL_KEY`, location `$43`, and the room chest-open status bit.
- [ ] Tick at most `$40` chest frames, consume and apply reward events, observe the source dialog, and wait for the chest entity to disable or unload; assert Small Keys increase from one to two.
- [ ] If production behavior fails, preserve the RED, trace the corresponding disassembly handler, implement the smallest shared fix, and rerun the failing test to GREEN.

### Task 3: Toggle room `$38` and verify switch-block collision

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only after a genuine RED: shared switch/runtime files under `java/src/main/java/linksawakening/world/`

- [ ] Record representative `$DB` cells and their collision while the carried switch state is `$00`.
- [ ] Reach crystal switch `$45` through collision-aware movement and strike its live hitbox with the ordinary `Sword` collision plus `resolveEntityCombat` path.
- [ ] Assert animation stage `$01` and wave SFX `$0E`, then advance exactly nine gameplay VBlanks.
- [ ] Refresh the switch entity snapshot and assert animation stage `$00`, global switch state `$02`, and synchronized Link collision at the recorded switch-block cells.
- [ ] Keep room `$39` as the explicitly documented next frontier; do not cross it in this task.

### Task 4: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-room-38.md`

- [ ] Run the ordered test and focused transition, Moblin Sword, chest, switch-block, and room-runtime tests with `--rerun-tasks`.
- [ ] Request source-fidelity review and address every Critical and Important finding before requesting code-quality review.
- [ ] Run `gradle clean test`, count JUnit XML totals, and run `git diff --check`.
- [ ] Record exact verified behavior, test totals, and room `$39` as the next frontier in the roadmap.
- [ ] Mark checkboxes complete only after fresh evidence exists, then commit the implementation and records on the current branch.
