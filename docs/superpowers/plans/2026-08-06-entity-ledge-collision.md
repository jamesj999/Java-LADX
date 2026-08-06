# Entity ledge collision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Port the ROM's stateful $D0-$D3 ledge collision branch into the Java entity/background path without changing existing stateless callers.

**Architecture:** EntityBackgroundCollisionResolver consumes an immutable collision-state record and returns the normal collision result plus the next per-entity ledge timer. RoomEntityRuntime owns thrown-direction and timer bytes; RoomSession supplies room/frame context, applies the returned timer, and keeps object/physics lookup ROM-backed. The rich background-probe API gains a frame-aware default overload so legacy movement handlers and the direct roaming handler see the same frame.

**Tech Stack:** Java 21, JUnit 5, Gradle, shipped azle.gbc ROM, LADX disassembly.

---

### Task 1: Add failing ROM-ledge resolver tests

**Files:**
- Modify: java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java

- [x] **Step 1: Add matching-direction tests.** Use the existing entity(...) and tables(...) helpers and a new resolveWithState helper. Cover a grounded entity on matching ledge direction (blocked), an airborne entity on matching direction (passable and timer increment), and timer $FF wrapping to $00.

    var result = resolver.resolveWithState(entity, EntityBackgroundCollisionResult.RIGHT,
        new EntityCollisionPointProbe.Sample(0x20, 0x30), 0x22, 0xD0,
        new EntityBackgroundCollisionState(0x00, true, 0x00, 0x00));
    assertTrue(result.result().blocked());

    var airborneResult = resolver.resolveWithState(airborne,
        EntityBackgroundCollisionResult.RIGHT,
        new EntityCollisionPointProbe.Sample(0x20, 0x30), 0x22, 0xD0,
        new EntityBackgroundCollisionState(0x00, true, 0x00, 0xFF));
    assertFalse(airborneResult.result().blocked());
    assertEquals(0x00, airborneResult.nextLedgeTimer());

- [x] **Step 2: Add nonmatching-direction timing tests.** Cover zero timer (blocked), indoor frame 1 decrement, outdoor even frame no decrement, outdoor odd frame decrement, and the wrecking-ball exception (blocked without consuming the timer).

- [x] **Step 3: Run the focused resolver tests and verify RED.** Run:

    gradle -p java test --tests linksawakening.world.EntityBackgroundCollisionResolverTest

Expected: compilation fails because EntityBackgroundCollisionState and resolveWithState do not exist.

- [x] **Step 4: Commit the red tests.**

    git add java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java
    git commit -m "test: specify ROM ledge collision state"

### Task 2: Add failing live-path propagation tests

**Files:**
- Modify: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
- Modify: java/src/test/java/linksawakening/world/RoamingEnemyMotionTest.java
- Modify: java/src/test/java/linksawakening/world/RoomSessionTest.java

- [x] **Step 1: Extend the roaming rich-probe test.** Add a six-argument override that records frameCounter, then assert the new frame-aware overload supplies the frame to both X and Y probes while preserving the existing ignore-hits assertion.

    @Override
    public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
        int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
        observedIgnoreHits.set(ignoreHitsCountdown);
        observedFrame.set(frameCounter);
        return EntityBackgroundCollisionResult.blocked(direction, 0x22, 0x01, nextX, nextY);
    }

- [x] **Step 2: Add runtime state lifecycle assertions.** Create a runtime with one loaded entity, assert the ROM reset values (thrownDirection == 0xFF, ledge timer 0), set both through package-private test setters, clear the entity, and assert the reset values again.

- [x] **Step 3: Add a shipped-room ledge probe regression.** Use the existing RoomSessionTest fixture and active-object helpers. Set a loaded entity's thrown direction and ledge timer, arrange a ROM physics sample of $D0/$D1, call an explicit-frame package test probe, and assert the pass/block result plus the timer written back to runtime. Probe an outdoor even frame and odd frame to cover the two cadence branches.

- [x] **Step 4: Run the new tests and verify RED.** Run:

    gradle -p java test \
      --tests linksawakening.world.RoomEntityRuntimeTest \
      --tests linksawakening.world.RoamingEnemyMotionTest \
      --tests linksawakening.world.RoomSessionTest

Expected: compilation fails at the new state/frame-aware APIs.

- [x] **Step 5: Commit the red integration tests.**

    git add java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
      java/src/test/java/linksawakening/world/RoamingEnemyMotionTest.java \
      java/src/test/java/linksawakening/world/RoomSessionTest.java
    git commit -m "test: cover live ledge collision state propagation"

### Task 3: Implement the ROM-shaped resolver and probe API

**Files:**
- Create: java/src/main/java/linksawakening/world/EntityBackgroundCollisionState.java
- Create: java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolution.java
- Modify: java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java
- Modify: java/src/main/java/linksawakening/world/RoomEntityBackgroundInteraction.java

- [x] **Step 1: Add immutable state/result records.** Mask byte fields to $FF and reject a null result:

    record EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                          int thrownDirection, int ledgeTimer) {
        EntityBackgroundCollisionState {
            frameCounter &= 0xFF;
            thrownDirection &= 0xFF;
            ledgeTimer &= 0xFF;
        }
    }

    record EntityBackgroundCollisionResolution(EntityBackgroundCollisionResult result,
                                                int nextLedgeTimer) {
        EntityBackgroundCollisionResolution {
            if (result == null) {
                throw new IllegalArgumentException("Collision result cannot be null");
            }
            nextLedgeTimer &= 0xFF;
        }
    }

- [x] **Step 2: Add resolveWithState and preserve old overloads.** Existing resolve(...) methods delegate with zero state. Keep every non-ledge branch behaviorally identical. For $D0-$D3, implement the ROM order: matching direction blocks only at z == 0, otherwise increments the timer and passes; nonmatching wrecking ball or zero timer blocks; a timed pass decrements only when (frame & 3) != 0 and indoors or (frame & 1) != 0.

    if (ledgeDirection == state.thrownDirection()) {
        if (entity.z() == 0) {
            blocked = true;
        } else {
            nextTimer = (nextTimer + 1) & 0xFF;
            blocked = false;
        }
    } else if (entity.type() == ENTITY_WRECKING_BALL || nextTimer == 0) {
        blocked = true;
    } else {
        blocked = false;
        if ((state.frameCounter() & 0x03) != 0
            && (state.indoorRoom() || (state.frameCounter() & 0x01) != 0)) {
            nextTimer = (nextTimer - 1) & 0xFF;
        }
    }

- [x] **Step 3: Add the frame-aware default probe.** Keep the four-argument abstract method and five-argument default method. Add:

    default EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
        int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
        return probe(entity, direction, nextX, nextY, ignoreHitsCountdown);
    }

- [x] **Step 4: Run the resolver tests and verify GREEN.** Run the Task 1 Gradle command and confirm the resolver tests pass.

- [x] **Step 5: Commit the resolver/API implementation.**

    git add java/src/main/java/linksawakening/world/EntityBackgroundCollisionState.java \
      java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolution.java \
      java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java \
      java/src/main/java/linksawakening/world/RoomEntityBackgroundInteraction.java
    git commit -m "feat: model ROM ledge collision state"

### Task 4: Wire state through runtime, roaming motion, and room session

**Files:**
- Modify: java/src/main/java/linksawakening/world/RoomEntityRuntime.java
- Modify: java/src/main/java/linksawakening/world/RoamingEnemyMotion.java
- Modify: java/src/main/java/linksawakening/world/RoomSession.java

- [x] **Step 1: Add runtime arrays and lifecycle handling.** Add ledgeTransitionTimer[] beside thrownDirection[]; initialize/reset thrown direction to $FF and the timer to 0 in constructor and every clear path. Keep throw setup's existing direction assignment. Add validated package-private accessors/setters used by tests and the session.

- [x] **Step 2: Capture the current frame in the rich-to-legacy adapter.** Compute frame = frameCounter & 0xFF before constructing backgroundCollision, then call the six-argument rich probe with the slot's current ignore-hits value and frame.

- [x] **Step 3: Propagate frame through RoamingEnemyMotion.** Preserve existing overloads by delegating with frame zero. Add a frame-aware overload and pass both state values to X and Y rich probes. Update the live runtime call to supply frame.

- [x] **Step 4: Make RoomSession stateful.** Override the six-argument probe in the existing anonymous interaction. Route all overloads to a helper accepting ignore-hits and frame; build state from active-room indoor status, runtime thrown direction/timer, and entity Z. If (z & 0x80) != 0, resolve with timer zero. Store nextLedgeTimer in the runtime before returning the collision result.

- [x] **Step 5: Add an explicit-frame package test helper.** Keep the current four-argument helper delegating with frame zero; add a package-private overload accepting frameCounter for cadence tests.

- [x] **Step 6: Run focused tests and verify GREEN.** Run the Task 2 Gradle command and confirm all new resolver, runtime, roaming, and session tests pass.

- [x] **Step 7: Commit the live wiring.**

    git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
      java/src/main/java/linksawakening/world/RoamingEnemyMotion.java \
      java/src/main/java/linksawakening/world/RoomSession.java
    git commit -m "feat: propagate ROM ledge state through entity movement"

### Task 5: Document and verify the increment

**Files:**
- Modify: docs/reconstruction-roadmap.md
- Modify: docs/superpowers/specs/2026-08-06-entity-ledge-collision-design.md
- Modify: docs/superpowers/plans/2026-08-06-entity-ledge-collision.md

- [x] **Step 1: Update the roadmap.** Record the ledge direction/timer/cadence implementation and leave switch-block WRAM and hookshot-chain transition state as the next collision gaps.

- [x] **Step 2: Mark the spec and plan status only after verification.** Include the exact focused and full test commands and their observed results.

- [x] **Step 3: Run fresh verification.**

    gradle -p java clean test
    git diff --check f603857..HEAD
    git status --short --branch

Observed: the focused ledge/runtime/session tests and `gradle -p java clean test` both
reported `BUILD SUCCESSFUL`; the diff check reported no whitespace errors.

- [x] **Step 4: Request focused code review.** Review branch order against bank3.asm, frame cadence, state reset paths, and compatibility overloads. Address actionable findings, rerun full verification, and report only evidence-backed results.

The delegated review worker timed out before returning a findings summary. A local
source audit was completed against `bank3.asm` before final verification; no
unverified external review result is claimed.
