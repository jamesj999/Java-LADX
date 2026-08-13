# Bottle Grotto Entry Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend fresh ordered play from BowWow's rescue through the swamp owl,
Goponga Swamp clearance, and Bottle Grotto's ROM-authored entrance.

**Architecture:** Keep the existing uninterrupted `RoomSession` and extend its
ordered integration regression. Use real collision paths, entity ticks,
persistence harvesting, and warps; alter production behavior only when the red
test demonstrates a mismatch with the disassembly.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed tables.

---

### Task 1: Prove the post-rescue route and owl event

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Source: `LADX-Disassembly/src/code/entities/06_owl_event.asm`
- Source: `LADX-Disassembly/src/data/entities/overworld.asm`

- [x] Walk `$E1 -> $E0 -> $F0`, use the active ROM warp, and assert return to
  overworld room `$35`.
- [x] Cross east to room `$36` using the collision-connectivity helper.
- [x] Tick with an interactive Link until ROM dialog `$0C3` opens and room
  status bit `$20` persists.
- [x] Run the focused ordered test and confirm it fails for the first missing
  source behavior rather than a test setup error.

Run:
```bash
gradle -p java test --tests linksawakening.world.RoomTransitionCoordinatorTest.freshGameRuntimeSequenceEntersBottleGrottoInOrder --rerun-tasks
```

Expected before implementation: `FAIL` at the first unimplemented or incorrect
post-rescue behavior.

### Task 2: Prove live BowWow swamp clearance

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify if required: `java/src/main/java/linksawakening/world/BowWowMotion.java`
- Modify if required: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify if required: `java/src/main/java/linksawakening/world/RoomSession.java`
- Create: `java/src/main/java/linksawakening/world/RecentRoomEntityClears.java`
- Create: `java/src/test/java/linksawakening/world/RecentRoomEntityClearsTest.java`
- Source: `LADX-Disassembly/src/code/entities/05_bow_wow.asm`
- Source: `LADX-Disassembly/src/data/entities/bow_wow_eatable.asm`

- [x] Follow the collision-valid full route through `$33`, `$23`, `$22`, and
  west into `$24`.
- [x] Assert all ROM-authored `$7C`/`$7E` flowers load in their route rooms,
  including room `$24`'s five flowers plus dynamic BowWow.
- [x] Keep Link within BowWow's source `$20` leash and tick the live runtime
  until every route obstruction is consumed.
- [x] Assert immediate transient-room persistence on revisiting `$33`, `$23`,
  and `$24`; do not mislabel these clears as permanent status bits.
- [x] Port `UpdateRecentRoomsList`'s six-slot ring and prove the sixth other
  distinct room evicts the old room's entity-clear mask while revisits do not
  advance the ring.
- [x] If the red test exposes a mismatch, add the smallest focused regression,
  implement the disassembly behavior, and rerun both focused and ordered tests.

### Task 3: Enter Bottle Grotto through ROM data

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify if required: `java/src/main/java/linksawakening/world/RoomTransitionCoordinator.java`
- Modify if required: `java/src/main/java/linksawakening/world/RoomSession.java`
- Source: `LADX-Disassembly/src/data/rooms/overworld_a.asm`
- Source: `LADX-Disassembly/src/data/rooms/indoors_a.asm`
- Source: `LADX-Disassembly/src/data/maps/layouts.asm`

- [x] Select room `$24`'s warp whose destination is map `$01`, room `$36`.
- [x] Trigger it through `handleWarpAndIndoorBoundaries` and finish the normal
  transition.
- [x] Assert map `$01`, room `$36`, layout position `(2,7)`, source arrival and
  return-warp bytes, the dungeon-warp entity, four hearts, and indoor BowWow
  exclusion while retaining the follower quest state.
- [x] Run the focused ordered test until green.
- [x] Move the real test `Link` only along ROM-collision-valid paths while
  driving BowWow, and prove a collision-valid west-entry path reaches warp
  tile `$13` before triggering it.

### Task 4: Verify, review, and commit

**Files:**
- Modify: `docs/superpowers/plans/2026-08-13-bottle-grotto-entry.md`

- [x] Run the ordered regression with `--rerun-tasks`.
- [x] Run `gradle -p java clean test`.
- [x] Run `git diff --check` and inspect the complete diff.
- [x] Request source-fidelity review and address every Critical or Important
  finding, then rerun affected verification.
- [x] Mark this plan's completed checkboxes and commit the coherent slice on
  `feature/entity-runtime`.
