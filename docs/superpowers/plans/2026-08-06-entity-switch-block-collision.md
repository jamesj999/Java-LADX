# Entity switch-block collision Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the ROM `$04` ocean/switch-block entity collision branch and carry `wSwitchBlocksState` through the Java room collision boundary.

**Architecture:** `EntityBackgroundCollisionState` gains the unsigned WRAM state byte while retaining its compatibility constructor. `EntityBackgroundCollisionResolver` receives the sampled object ID and applies the bank-$03 object-kind table, bomb/wrecking-ball exceptions, invalid-ocean path, and no-wall option. `RoomSession` owns the session-wide state byte and supplies it to live probes without inventing a crystal-switch toggle source.

**Tech Stack:** Java 21, JUnit 5, Gradle, shipped `azle.gbc` ROM, LADX disassembly.

---

### Task 1: Add failing ROM switch-block resolver tests

**Files:**
- Modify: `java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java`

- [x] **Step 1: Add a state-aware test helper.** Keep the existing stateless helper unchanged, and pass an explicit object ID and `switchBlocksState` into `resolveWithState`:

```java
private static EntityBackgroundCollisionResolution resolveSwitchBlockWithState(
        EntityBackgroundCollisionResolver resolver, RoomEntity entity,
        int objectId, int switchBlocksState) {
    return resolver.resolveWithState(entity, EntityBackgroundCollisionResult.RIGHT,
        new EntityCollisionPointProbe.Sample(0x20, 0x30), objectId, 0x04,
        new EntityBackgroundCollisionState(0x00, false, 0xFF, 0x00,
            switchBlocksState));
}
```

- [x] **Step 2: Add state/object-kind assertions.** Cover state `$00` passing `$DB` and blocking `$DC`, state `$02` reversing those results, and IDs outside `$DB..$DC` remaining solid.

```java
@Test
void switchBlockObjectKindMatchesTheRomStateByte() {
    RoomEntity ordinary = entity(0x30, 0);
    EntityBackgroundCollisionResolver resolver = new EntityBackgroundCollisionResolver(
        tables(0, 0, 0, 0, ordinary.type(), 0));

    assertFalse(resolveSwitchBlockWithState(resolver, ordinary, 0xDB, 0x00)
        .result().blocked());
    assertTrue(resolveSwitchBlockWithState(resolver, ordinary, 0xDC, 0x00)
        .result().blocked());
    assertTrue(resolveSwitchBlockWithState(resolver, ordinary, 0xDB, 0x02)
        .result().blocked());
    assertFalse(resolveSwitchBlockWithState(resolver, ordinary, 0xDC, 0x02)
        .result().blocked());
    assertTrue(resolveSwitchBlockWithState(resolver, ordinary, 0xDA, 0x00)
        .result().blocked());
    assertTrue(resolveSwitchBlockWithState(resolver, ordinary, 0xDD, 0x02)
        .result().blocked());
}
```

- [x] **Step 3: Add ROM exceptions.** Assert bomb `$02` and wrecking ball `$A8` pass both object kinds in both states, while the result still reports the sampled object and physics `$04`.

- [x] **Step 4: Run the focused resolver test and verify RED.** Run `gradle -p java test --tests linksawakening.world.EntityBackgroundCollisionResolverTest`. Expected: compilation fails because the five-field state constructor and state-aware switch-block decision do not exist.

- [x] **Step 5: Commit the red resolver tests.** Run `git add java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java && git commit -m "test: specify ROM switch-block collision state"`.

### Task 2: Add the failing live room-session regression

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [x] **Step 1: Add a package test seam call.** The test calls `setEntitySwitchBlocksStateForTest(int)` before probing, so the test must fail to compile until the live state boundary exists.

- [x] **Step 2: Add a shipped-ROM object-kind regression.** Load indoor room `$00:$0F`, select the existing type `$1E` entity, fill the padded active room with `$DB` and `$DC`, select states `$00` and `$02`, and assert pass/block, object ID, and physics `$04` from `entityBackgroundCollisionResultForTest`.

```java
@Test
void liveEntityCollisionProbeUsesRomSwitchBlockStateAndObjectKind() {
    RoomSession session = newSession();
    session.loadIndoor(0x00, 0x0F);
    RoomEntity entity = session.activeRoom().entities().loadedEntities().stream()
        .filter(candidate -> candidate.type() == 0x1E)
        .findFirst().orElseThrow();

    fillActiveObjects(session, 0xDB);
    session.setEntitySwitchBlocksStateForTest(0x00);
    EntityBackgroundCollisionResult lower = session.entityBackgroundCollisionResultForTest(
        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y());
    assertFalse(lower.blocked());
    assertEquals(0xDB, lower.objectId());
    assertEquals(0x04, lower.physicsFlag());

    fillActiveObjects(session, 0xDC);
    assertTrue(session.entityBackgroundCollisionResultForTest(entity,
        EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y()).blocked());
    session.setEntitySwitchBlocksStateForTest(0x02);
    assertFalse(session.entityBackgroundCollisionResultForTest(entity,
        EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y()).blocked());
}
```

- [x] **Step 3: Run the live regression and verify RED.** Run `gradle -p java test --tests linksawakening.world.RoomSessionTest`. Expected: compilation fails at the missing session setter/state constructor.

- [x] **Step 4: Commit the red live test.** Run `git add java/src/test/java/linksawakening/world/RoomSessionTest.java && git commit -m "test: cover live switch-block collision state"`.

### Task 3: Implement the ROM state boundary and resolver branch

**Files:**
- Modify: `java/src/main/java/linksawakening/world/EntityBackgroundCollisionState.java`
- Modify: `java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java`

- [x] **Step 1: Extend the state record compatibly.** Add a masked `switchBlocksState` component and keep the four-argument constructor delegating to state zero:

```java
record EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                      int thrownDirection, int ledgeTimer,
                                      int switchBlocksState) {
    EntityBackgroundCollisionState {
        frameCounter &= 0xFF;
        thrownDirection &= 0xFF;
        ledgeTimer &= 0xFF;
        switchBlocksState &= 0xFF;
    }

    EntityBackgroundCollisionState(int frameCounter, boolean indoorRoom,
                                   int thrownDirection, int ledgeTimer) {
        this(frameCounter, indoorRoom, thrownDirection, ledgeTimer, 0);
    }
}
```

- [x] **Step 2: Pass the unsigned object ID into the decision helper.** Change the resolver call to include `unsignedObjectId`; preserve every existing non-switch branch and ledge timer result.

- [x] **Step 3: Port the exact switch-block branch.** Use the source bytes `[0x00, 0x02]`. Bombs and wrecking balls pass first; object IDs outside `$DB`/`$DC` block as ocean; valid mismatches use `!noWall`, matching the source `doesCollide` then `hookshotEnd` path:

```java
private static final int OBJECT_LOWERED_BLOCK = 0xDB;
private static final int OBJECT_RAISED_BLOCK = 0xDC;
private static final int[] SWITCH_BLOCK_STATE_BY_OBJECT = {0x00, 0x02};

private static CollisionDecision resolveSwitchBlockCollision(
        RoomEntity entity, int objectId, boolean noWall,
        EntityBackgroundCollisionState state) {
    if (isBombOrWreckingBall(entity)) return passable(state);
    if (objectId < OBJECT_LOWERED_BLOCK || objectId > OBJECT_RAISED_BLOCK) {
        return blocked(state);
    }
    int expected = SWITCH_BLOCK_STATE_BY_OBJECT[objectId - OBJECT_LOWERED_BLOCK];
    return decision(expected != state.switchBlocksState() && !noWall, state);
}
```

- [x] **Step 4: Run the focused resolver tests and verify GREEN.** Run the Task 1 command and confirm the resolver suite passes.

- [x] **Step 5: Commit the resolver implementation.** Commit the two production files as `feat: model ROM switch-block collision state`.

### Task 4: Wire the session state into live probes

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`

- [x] **Step 1: Add the session-wide byte.** Declare `private int switchBlocksState;`; Java's zero initialization matches the source reset. Do not clear it during room loads because the source WRAM byte is global gameplay state.

- [x] **Step 2: Include the byte in the rich collision state.** Add `switchBlocksState` after `ledgeTimer` in the state construction inside `entityBackgroundCollisionResult(...)`.

- [x] **Step 3: Add validated package-private test accessors:**

```java
void setEntitySwitchBlocksStateForTest(int value) {
    if (value < 0 || value > 0xFF) {
        throw new IllegalArgumentException("Switch-block state must be an unsigned byte: " + value);
    }
    switchBlocksState = value;
}

int entitySwitchBlocksStateForTest() {
    return switchBlocksState & 0xFF;
}
```

- [x] **Step 4: Run the live and focused suite.** Run `gradle -p java test --tests linksawakening.world.EntityBackgroundCollisionResolverTest --tests linksawakening.world.RoomSessionTest`; expect `BUILD SUCCESSFUL`.

- [x] **Step 5: Commit the live wiring.** Commit `RoomSession.java` as `feat: propagate switch-block state through room collision`.

### Task 5: Document and verify the increment

**Files:**
- Modify: `docs/reconstruction-roadmap.md`
- Modify: `docs/superpowers/specs/2026-08-06-entity-switch-block-collision-design.md`
- Modify: `docs/superpowers/plans/2026-08-06-entity-switch-block-collision.md`

- [x] **Step 1: Add a dated roadmap entry.** Record the source table bytes, object IDs, state zero/two behavior, bomb/wrecking-ball exceptions, and the explicit crystal-switch/hookshot scope boundary.

- [x] **Step 2: Mark spec and plan implemented only after tests pass.** Include exact focused and full commands plus observed results, and change the spec status from Proposed to Implemented and verified.

- [x] **Step 3: Run fresh verification:** `gradle -p java clean test`, `git diff --check 74bf755..HEAD`, and `git status --short --branch`. Expected: `BUILD SUCCESSFUL`, no whitespace errors, and a clean feature worktree after the documentation commit.

Observed: the focused resolver/session command and `gradle -p java clean test`
reported `BUILD SUCCESSFUL`; the committed-range diff check reported no
whitespace errors. The final documentation commit is the remaining step before
the worktree is clean.
