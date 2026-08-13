# Tail Cave Roc's Feather Route Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend ordered Tail Cave play from room `$03` through `$19/$18/$01/$1C` to collect Roc's Feather in `$1D`.

**Architecture:** Extend the narrowly supported map-0 side-view boundary policy to rooms `$18/$19`, using exact ROM-coordinate margins. Reuse existing adjacent indoor scrolling, warp loading, chest opening, and `PlayerState` reward paths.

**Tech Stack:** Java 17, JUnit 5, Gradle, ROM-backed layouts/warps/chest table.

---

### Task 1: Complete Tail Cave side-view boundaries

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomBoundaryControllerTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomBoundaryController.java`

- [ ] Add failing tests for exact left/right margins in map `$00` rooms `$18/$19`, `$19` left scrolling to `$18`, and `$18` vertical fading through warp 0 to room `$01`.
- [ ] Run `gradle -p java test --tests linksawakening.world.RoomBoundaryControllerTest --tests linksawakening.world.RoomTransitionCoordinatorTest` and verify RED.
- [ ] Extend the supported Tail Cave side-view policy to room `$18`; suppress generic horizontal transitions inside `[-4,-1]` and `[0x91,0x93]`, while allowing normal indoor scroll outside those margins.
- [ ] Rerun the focused tests and verify GREEN.
- [ ] Commit with `feat: complete Tail Cave side-view route`.

### Task 2: Reach and collect Roc's Feather in ordered play

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomTransitionCoordinatorTest.java`
- Modify production only if a failing ordered assertion exposes missing source behavior.

- [ ] Replace the ordered test's immediate `$19 -> $03` return with `$19 -> $18 -> $01 -> $1C -> $1D`.
- [ ] Open chest location `$24` from Link position `(0x38,0x21)`, assert `CHEST_FEATHER`, apply it to `PlayerState`, and assert inventory contains `INVENTORY_ROCS_FEATHER`.
- [ ] Run the single ordered regression and verify GREEN.
- [ ] Commit with `test: reach Rocs Feather in ordered play`.

### Task 3: Verify and review

**Files:** No intended production changes.

- [ ] Run `gradle -p java clean test` and require `BUILD SUCCESSFUL`.
- [ ] Run `git diff --check`, inspect scope, and request source-fidelity review against `MapLayout0`, rooms `$18/$19/$01/$1C/$1D`, and `CheckPositionForMapTransition`.
- [ ] Address findings and rerun the cold suite before handing off.
