# Bottle Grotto Hinox Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reach room `$28`, implement Hinox's six-state ROM behavior, defeat it normally, and use the newly available miniboss warp.

**Architecture:** Add a focused Hinox motion component following the existing entity-motion families, dispatched generically by entity type `$89`. Carry Link grab/throw and bomb effects through existing runtime event boundaries, then extend the uninterrupted ordered regression through the shared combat, room-event, persistence, and warp paths.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, deterministic ROM-backed entity runtime.

---

### Task 1: Add RED coverage for Hinox's deterministic state machine

**Files:**
- Create: `java/src/test/java/linksawakening/world/HinoxMotionTest.java`
- Create: `java/src/main/java/linksawakening/world/HinoxMotion.java`

- [x] Add focused tests that load or construct entity `$89` and verify initial/wander cardinal motion, the `$30`-tick charge wind-up, `$18` vector toward Link, and bounce jingle `$20` cadence using deterministic RNG.
- [x] Add focused tests for charge proximity entering grab, motion blocking and held-Link offsets, then the countdown `$20` throw effect: X speed `$E0/$20`, Y speed `$20`, Z velocity `$10`, airborne `$02`, health damage `$08`, and jingle `$08`.
- [x] Add focused tests proving flash countdown `$03` enters bomb state only below grab state and that countdown `$10` spawns entity `$02` with the source offset/throw values.
- [x] Run the required RED compile/test check, then `cd java && gradle test --tests linksawakening.world.HinoxMotionTest --rerun-tasks` GREEN after implementation.

### Task 2: Implement and dispatch the shared Hinox handler

**Files:**
- Modify: `java/src/main/java/linksawakening/world/HinoxMotion.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify as required by established catalogs: `java/src/main/java/linksawakening/world/EntitySpriteHandlerCatalog.java`
- Test: `java/src/test/java/linksawakening/world/HinoxMotionTest.java`

- [x] Implement the six states from `06_hinox.asm` with existing countdown, RNG, collision, speed-vector, combat, event, and entity-slot conventions.
- [x] Dispatch type `$89` generically and preserve `IS_BOSS | NO_GROUND_INTERACTION | IS_MINI_BOSS`, big-enemy hitbox, physics group `$0C`, and health group `$14` behavior already decoded from ROM tables.
- [x] Reuse or minimally extend runtime events so the world layer can apply Link motion blocking, held position, throw velocities/airborne state, damage, jingles, dust, and bomb spawn without room-specific coupling.
- [x] Run `HinoxMotionTest` GREEN and the focused runtime/combat/catalog suites.

### Task 3: Add RED/GREEN ordered play through room `$28`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify only for a demonstrated shared defect: `java/src/main/java/linksawakening/world/RoomTransitionCoordinator.java`

- [x] Continue the same post-Stone-Beak session north `$2E -> $2C -> $28` with collision-aware boundary movement; assert room `$2C`'s event/entities and room `$28`'s event `$C1`, Hinox `$89` at `$25`, and warp `$61` at `$34`.
- [x] Prove the warp is unavailable while Hinox remains active.
- [x] Initialize Hinox, attack with live Sword collision boxes, observe at least one bomb response, and complete all health/recovery/death ticks without direct health or slot mutation.
- [x] Assert event `$C1` clears through the ordinary miniboss path and persists the correct room status.
- [x] Approach the ROM warp entity through collision and complete its ordinary post-clear warp transition toward room `$36`, asserting category, destination, and Link placement from ROM-backed warp data.
- [x] Run the ordered test RED before any required production correction, then GREEN with bounded waits and explicit terminal assertions.

### Task 4: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-hinox.md`

- [x] Record exact source labels, route correction, runtime behavior, test totals, and next gameplay frontier.
- [x] Run focused Hinox, entity runtime/combat/death, transition/warp, and ordered-play tests with `--rerun-tasks`.
- [x] Run `cd java && gradle clean test`, count JUnit XML failures/errors/skips, and run `git diff --check` plus full diff inspection.
- [x] Obtain independent source-fidelity review and fix every Critical or Important finding.
- [x] Obtain independent code-quality review only after source compliance passes and fix every Critical or Important finding.
- [x] Mark plan checkboxes only after fresh evidence, then commit all implementation, tests, plan, and roadmap changes on the current branch.
