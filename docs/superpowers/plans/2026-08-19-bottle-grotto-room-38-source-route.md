# Bottle Grotto Room `$38` Source Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Return from the Stone Beak through room `$33`'s switch, collect room `$38`'s Small Key, and reach room `$35` from the correct side.

**Architecture:** Extend the existing uninterrupted ordered regression using live collision, ordinary crystal-switch combat, room transitions, and chest reward events. Production changes are permitted only for a retained shared-runtime RED.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed rooms/entities/chests.

---

### Task 1: Prove the corrected route into room `$38`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Continue after Stone Beak through `$2E -> $30 -> $31 -> $32 -> $33`, asserting each room and zero Small Keys.
- [ ] Reach and strike `$33`'s live crystal switch with a Sword collision box; tick all nine VBlanks and assert state `$00 -> $02` plus representative switch-block collision.
- [ ] Cross south `$33 -> $38`; assert event `$00`, chest `$A0` at `$43`, Moblin Sword `$14` at `$62`, and switch `$66` at `$45`.
- [ ] Run the ordered test with `--rerun-tasks` and retain any shared-runtime RED.

### Task 2: Collect the key and reach room `$35`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] Reach chest `$43` through collision and open it with `tryOpenChest`.
- [ ] Assert `CHEST_SMALL_KEY`, persistence, reward, ROM-selected dialog, bounded entity teardown, and key count `$00 -> $01`.
- [ ] Cross `$38 -> $39 -> $34 -> $35`, asserting `$34` entry is on the partition's east side and `$35`'s north key door remains locked.
- [ ] Run ordered, chest, switch, and transition suites.

### Task 3: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-room-38-source-route.md`

- [ ] Record the corrected layout route, source labels, runtime evidence, test totals, and room `$35/$2F` as the next frontier.
- [ ] Run focused suites, `gradle clean test`, XML totals, and `git diff --check`.
- [ ] Complete source-fidelity review, then code-quality review, fixing every Critical/Important finding.
- [ ] Mark verified checkboxes and commit on the current branch.
