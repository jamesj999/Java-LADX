# Bottle Grotto Room `$2F` Staircase Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Unlock room `$35`'s north door and reveal room `$2F`'s hidden staircase through two real block pushes.

**Architecture:** Extend the uninterrupted ordered regression and add one shared trigger-`$07` settlement predicate only if the focused RED proves it absent. Reuse directional door persistence and generic stair reveal effects.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed rooms/events.

---

### Task 1: Add focused trigger `$07` RED/GREEN coverage

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify if RED requires it: `java/src/main/java/linksawakening/world/RoomSession.java`

- [ ] Load room `$2F`; assert event `$A7`, blocks `$A7` at `$33/$36`, and hidden stairs `$BF` at `$18`.
- [ ] Push `$33` right once and tick its 33-frame motion; assert it settles `$A6` without resolving.
- [ ] Push `$36` left once and tick settlement; assert horizontal `$A6` adjacency resolves trigger `$07`.
- [ ] Assert vertical adjacency and in-motion states do not resolve.
- [ ] Let the generic effect persist completion and reveal `$BE` at `$18`; run RED before shared implementation and GREEN afterward.

### Task 2: Extend ordered play through rooms `$35/$2F`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [ ] From the room `$35` checkpoint, reach and unlock top door `$EC` through collision; assert one key is consumed, eight motion-blocking frames occur, and both rooms persist matching door status.
- [ ] Cross north into `$2F`; assert source event, blocks, stairs, Keese/Spark/excluded items.
- [ ] Perform both sustained inward pushes through normal interaction and bounded motion ticks; assert first unresolved and second resolved.
- [ ] Wait for `$BF -> $BE`, assert room status/event persistence, zero Small Keys, and the active stairs warp to side-view `$3F`.

### Task 3: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-room-2f-staircase.md`

- [ ] Record source/runtime evidence, fresh totals, and room `$3F` as next frontier.
- [ ] Run focused suites, `gradle clean test`, XML totals, and `git diff --check`.
- [ ] Complete source-fidelity then code-quality reviews and fix every Critical/Important finding.
- [ ] Mark verified checkboxes and commit on the current branch.
