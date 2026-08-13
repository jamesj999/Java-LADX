# Tail Cave Block and Beetles Implementation Plan

> **For Codex:** Execute this plan in the `feature/entity-runtime` worktree with TDD and verify each task before moving on.

**Goal:** Make the ordered Tail Cave route playable through room `$04`'s single-block puzzle and room `$03`'s Spiked Beetle stair reveal, following the ROM/disassembly behavior.

**Architecture:** Keep collision-owned push timing and room-event state in `RoomSession`. Model the source's temporary `ENTITY_PUSHED_BLOCK` as a short room-session motion state which mutates the real room object only when it settles. Reuse the existing kill-all event dispatcher for room `$03`, extending only what the ordered runtime regression proves is missing.

**Tech Stack:** Java 17, JUnit 5, Gradle, ROM-backed room/event tables.

---

### Task 1: Implement the single-push-block interaction

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/Main.java`

1. Add failing tests proving object `$A7` requires 64 consecutive directional collision ticks, resets when contact breaks, and starts a 33-frame move toward Link's facing direction.
2. Run the focused test and confirm failure for the missing interaction.
3. Implement the collision bridge and pushed-block motion state using the source direction/speed tables.
4. Add a failing test proving settlement writes indoor floor object `$A6`, marks `TRIGGER_PUSH_SINGLE_BLOCK` resolved, opens the room's locked doors through effect `$20`, and persists room status.
5. Implement settlement, tilemap/collision refresh, trigger dispatch, and status persistence.
6. Wire the collision bridge into `Main` beside keyhole/key-door probes.
7. Run focused room-session tests.

### Task 2: Extend the ordered Tail Cave runtime regression

**Files:**
- Modify: `java/src/test/java/linksawakening/world/TailCaveRuntimeRegressionTest.java`
- Modify only if required by the failing test: relevant world/entity runtime classes.

1. Extend the route through room `$04`, push the lone block right, and verify the left shutter opens.
2. Enter room `$03`, defeat both ROM-loaded Spiked Beetles through the real combat path, and verify event `$A1` reveals the staircase.
3. Run the focused ordered regression and confirm any missing behavior before implementing it.
4. Implement only source-backed behavior exposed by that failure.
5. Re-run the ordered regression and related focused tests.

### Task 3: Verify and review

**Files:** No intended production changes.

1. Run the full clean Java test suite.
2. Inspect the worktree diff for accidental or unrelated changes.
3. Request a focused code review against the disassembly and address actionable findings.
4. Commit the completed slice with an intentional message.

