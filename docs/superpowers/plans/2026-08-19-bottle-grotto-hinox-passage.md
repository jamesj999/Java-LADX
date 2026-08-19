# Bottle Grotto Hinox Passage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Traverse `$2F -> $3F -> $3E -> $2C -> $28` through normal stairs, side-view, warp, and one-way-door paths.

**Architecture:** Extend the uninterrupted ordered regression using existing transition controllers and ROM warps. Add production code only for a retained shared-runtime RED.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed warps/layouts.

---

### Task 1: Traverse the side-view rooms

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomBoundaryController.java`
- Modify: `java/src/main/java/linksawakening/world/RoomBoundaryState.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/world/RoomTransitionCoordinator.java`
- Modify: `java/src/main/java/linksawakening/entity/Link.java`
- Modify: `java/src/test/java/linksawakening/world/RoomBoundaryControllerTest.java`

- [x] Activate room `$2F` stairs through collision and assert category `$02`, room `$3F`, and ROM Link placement.
- [x] Cross the live left side-view boundary `$3F -> $3E`, asserting scroll completion and collision-valid Link movement.
- [x] Activate `$3E`'s ROM exit warp and assert category `$01`, room `$2C`, Link `(x=$78,y=$70)`.

### Task 2: Enter room `$28`

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`

- [x] Assert room `$2C` event `$00` and exact source entities.
- [x] Traverse its collision-valid north one-way door into `$28`.
- [x] Assert event `$C1`, Hinox `$89` at `$25`, and warp `$61` at `$34`; prove the unresolved miniboss warp cannot transition yet.

### Task 3: Verify, review, document, and commit

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/plans/2026-08-19-bottle-grotto-hinox-passage.md`

- [x] Record source route, runtime evidence, and Hinox combat as next frontier.
- [x] Run focused suites, `gradle clean test`, XML totals, and `git diff --check`.
- [x] Complete source-fidelity then code-quality reviews; fix every Critical/Important finding.
- [x] Mark verified checkboxes; commit intentionally deferred by the coordinating agent.
