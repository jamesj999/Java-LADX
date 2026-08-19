# Bottle Grotto Stone Beak Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue the ordered Bottle Grotto trace through room `$31`'s west key door and collect the Stone Beak in room `$2E`.

**Architecture:** Exercise the generic key-door, entity combat/death, kill-all shutter, feather traversal, pit interaction, and chest reward systems against ROM-loaded rooms. Production changes require a genuine source-backed RED and must remain shared rather than room-specific.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed dungeon data.

---

### Task 1: Unlock room `$31`'s west door

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Keep the collision-validated `$37 -> $32 -> $31` return route and assert the previously resolved torch event remains clear.
- [ ] Assert the source closed pair `$31/$32` begins at location `$30` and locate a reachable Link position whose one-pixel left contact probes physics `$92`.
- [ ] Perform the contact step and call `tryUnlockIndoorKeyDoor` with `Link.DIRECTION_LEFT` and collision bit `$04`.
- [ ] Assert the last Small Key is consumed, eight animation ticks block Link, the next tick releases motion, objects become `$09/$0A`, and room status synchronizes `$31 & $02` with `$30 & $01`.
- [ ] Cross west through collision into room `$30`.

### Task 2: Clear room `$30` and open its north shutter

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only after a genuine RED: shared runtime files under `java/src/main/java/linksawakening/world/`

- [ ] Assert event `$21`, four spike traps, and two Keese from the ROM entity list.
- [ ] Initialize entities, reach each Keese through collision, damage it through `resolveEntityCombat`, and use bounded recovery/death loops.
- [ ] Assert spike traps remain loaded and excluded while event `$21` persists until both Keese are gone.
- [ ] Wait for the ordinary event completion, assert event zero and north shutter open, then cross north into room `$2E`.

### Task 3: Obtain the Stone Beak in room `$2E`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only after a genuine RED: shared Hardhat/pit/chest/runtime files under `java/src/main/java/linksawakening/world/`

- [ ] Assert the room's Hardhat Beetle, event `$21`, trench objects, and chest `$A0` at `$24`.
- [ ] Exercise the Hardhat's shared collision/knockback and pit-death path with bounded waits; do not mutate health or status directly.
- [ ] Use collision-aware or existing Feather-aware movement to reach the last passable position below chest `$24`, perform one upward contact step, and assert the interaction location.
- [ ] Open through `tryOpenChest`, assert `CHEST_STONE_BEAK`, room status `$10`, and tick at most `$40` frames through reward, dialog, and entity teardown.
- [ ] Apply reward events and assert the dungeon Stone Beak flag while keeping Small Keys at zero.

### Task 4: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-stone-beak-route.md`

- [ ] Run the ordered test and focused transition, key-door, Keese, Hardhat, pit, chest, and room-event suites with `--rerun-tasks`.
- [ ] Obtain source-fidelity review, fix all Critical/Important findings, then obtain code-quality review and fix all Critical/Important findings.
- [ ] Run `gradle clean test`, count JUnit XML totals, and run `git diff --check`.
- [ ] Record verified behavior and room `$28` as the next milestone in the roadmap.
- [ ] Mark checkboxes complete only after fresh evidence exists, then commit on the current branch.
