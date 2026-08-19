# Bottle Grotto Key Door and Push-Blocks Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Continue ordered Bottle Grotto play through room `$35`'s north key door and solve room `$2F`'s two-block staircase event.

**Architecture:** Extend the uninterrupted fresh-game regression with the existing key-door, transition, pushed-block, and stair-reveal APIs. Add one shared source-shaped trigger `$07` predicate at pushed-block settlement; do not add room-specific logic.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed room/event data.

---

### Task 1: Add the failing shared trigger `$07` regression

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [ ] Add `bottleGrottoTwoHorizontalBlocksRevealTheRoom2FStaircase()`.
- [ ] Load indoor map `$01`, room `$2F`; assert event `$A7`, blocks `$A7` at `$33/$36`, and hidden stairs `$BF` at `$18`.
- [ ] Hold right against block `$33` for `$40` calls to `tryPushIndoorBlock(0x25, 0x28, Link.DIRECTION_RIGHT, 0x08)`, then tick 33 entity frames so it settles at `$34` as `$A6`.
- [ ] Assert event `$A7` remains active and room status bit `$10` remains clear after the first push.
- [ ] Hold left against block `$36` for `$40` calls using a collision probe that touches `$36`, then tick 33 entity frames so it settles at `$35` beside `$34`.
- [ ] Tick the generic event/reveal countdown and assert event zero, room status `$10`, `$A6` at `$34/$35`, and stairs `$BE` at `$18`.
- [ ] Run:

```bash
cd java
gradle test --tests linksawakening.world.RoomSessionTest.bottleGrottoTwoHorizontalBlocksRevealTheRoom2FStaircase
```

  Expected: FAIL because the second settled block does not mark trigger `$07` resolved.

### Task 2: Implement the shared horizontal-adjacency trigger

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`

- [ ] Add `EVENT_TRIGGER_PUSH_BLOCKS = 0x07` beside the existing trigger constants.
- [ ] After `tickPushedBlockMotion()` writes the settled `$A6`, determine whether the active trigger is `$07` and either horizontal neighbor of `pushedBlockDestinationLocation` is `$A7` or `$A6`.
- [ ] When and only when that predicate is true, set `roomEventEffectExecuted`, emit `GameplaySoundEvent.PUZZLE_SOLVED`, and let `tickRoomEvent()` execute the existing `$A0` stair effect on the next normal tick.
- [ ] Keep trigger `$02` behavior unchanged and avoid checking vertical neighbors.
- [ ] Re-run the focused test and expect PASS.
- [ ] Run all `RoomSessionTest` tests and expect PASS.

### Task 3: Extend uninterrupted ordered play through rooms `$35` and `$2F`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Continue `freshGameRuntimeSequenceCollectsBottleGrottoFirstKeyInOrder` after room `$34` key pickup by crossing right into `$35`.
- [ ] Assert room `$35`, event `$00`, exactly two entity `$2C` Spiked Beetles, and two Small Keys before unlocking.
- [ ] Place Link at the source top-door probe, call `tryUnlockIndoorKeyDoor(..., Link.DIRECTION_UP, 0x01)`, assert one key remains, and tick exactly eight animation frames while asserting Link-motion blocking.
- [ ] Assert the top door is open and directional persistence bits are set in both `$35` and adjacent `$2F`, then cross north using `walkToAndCrossIndoorBoundary` and assert room `$2F` event `$A7`.
- [ ] Perform the two sustained inward pushes through `tryInteractWithIndoorBlock`, bounded motion waits, and ordinary entity ticks. Assert the first push does not resolve the event and the second does.
- [ ] Wait with a deadline for `$18` to become stairs `$BE`, then assert room `$2F` status bit `$10` and one remaining Small Key.
- [ ] Run the ordered-play test and expect PASS after Task 2's shared fix.

### Task 4: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-key-door-push-blocks.md`

- [ ] Record exact source labels, observed runtime behavior, test count, and the next accessible gameplay frontier.
- [ ] Run focused key-door, pushed-block, room-event, and ordered-transition tests with `--rerun-tasks`.
- [ ] Run `gradle clean test`, count JUnit XML totals, and run `git diff --check`.
- [ ] Request independent source-fidelity and code-quality reviews; address every Critical and Important finding and repeat affected verification.
- [ ] Mark every plan checkbox complete only after fresh evidence exists.
- [ ] Commit the implementation, tests, plan, and roadmap on the current branch.
