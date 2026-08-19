# Bottle Grotto Compass Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue uninterrupted Bottle Grotto play from room `$34` through the collision-valid backtrack, room `$32`'s bottom key door, and room `$37`'s Compass encounter.

**Architecture:** Extend the existing ordered-play regression using live boundary traversal, generic key-door animation/persistence, Masked-Mimic combat, room event `$61`, chest reveal, and ROM-backed chest reward paths. Add shared production behavior only if a genuine failing regression exposes a gap.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed room/event/chest tables.

---

### Task 1: Backtrack and unlock room `$32`'s bottom door

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Continue the current fresh-game test from room `$34` without changing Link's position manually.
- [ ] Use `walkToAndCrossIndoorBoundary` west to `$33`, then west to `$32`; assert both room ids and retain two Small Keys.
- [ ] Find the loaded bottom key-door pair `$2F/$30` produced by source macro `$74/$ED`, derive its downward collision probe, and call `tryUnlockIndoorKeyDoor(..., Link.DIRECTION_DOWN, 0x02)`.
- [ ] Assert the key count changes from two to one, tick eight animation frames with motion-block assertions, then verify the next tick is unblocked.
- [ ] Assert the bottom door becomes the ROM open pair, room `$32`'s down status bit `$08` and room `$37`'s up status bit `$04` are synchronized.
- [ ] Cross south collision-validly into room `$37` and assert event `$61`, one `$8F` Masked Mimic, and two `$2E` droppable rupees.

### Task 2: Resolve event `$61` and collect the Compass

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only if RED requires it: shared runtime files under `java/src/main/java/linksawakening/world/`

- [ ] Drive the room `$37` Mimic to options `$08` with two source-shaped input ticks, apply two one-damage sword hits through `resolveEntityCombat`, and use bounded recovery/death waits.
- [ ] Assert event `$61` stays active until the Mimic is disabled and both rupees remain loaded.
- [ ] Tick the ordinary chest-reveal countdown with a deadline and assert closed chest `$A0` at location `$28`, event zero, and room status `$10` remains clear until pickup semantics require it.
- [ ] Open the chest through `tryOpenChest` from its live upward interaction point; assert item type `CHEST_COMPASS`.
- [ ] Tick the spawned chest entity through the ordinary reward path, consume reward events as existing ordered tests do, and assert `DungeonItemState.COMPASS_INDEX == 1` plus one remaining Small Key.
- [ ] Run the focused ordered-play test. If it fails, capture the genuine RED and implement only the shared ROM-shaped behavior responsible; rerun to GREEN.

### Task 3: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-compass-route.md`

- [ ] Record source labels, collision-valid route, verified behavior, and next reachable gameplay frontier.
- [ ] Run focused transition, key-door, Masked-Mimic, chest, and room-event tests with `--rerun-tasks`.
- [ ] Run `gradle clean test`, count JUnit XML totals, and run `git diff --check`.
- [ ] Request source-fidelity and code-quality review; address every Critical and Important finding and repeat affected verification.
- [ ] Mark checkboxes complete only after fresh evidence exists, then commit on the current branch.
