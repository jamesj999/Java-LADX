# Entity collision ignore-hits propagation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Pass the live entity ignore-hits countdown into the ROM-shaped background collision resolver and preserve the existing callback API for direct runtime callers.

**Architecture:** Keep the four-argument `RoomEntityBackgroundInteraction` probe as the abstract compatibility boundary and add a default five-argument overload for state-aware callers. `RoomSession` supplies the ROM resolver with the countdown; `RoomEntityRuntime` adapts that rich probe to the legacy boolean callback consumed by entity movement and recoil handlers.

**Tech Stack:** Java 21, JUnit 5, Gradle, shipped `azle.gbc` ROM, LADX disassembly.

---

### Task 1: Specify the failing resolver cases

**Files:**
- Modify: `java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java`

- [x] **Step 1: Add the grounded pit countdown test before production changes.** Add:

```java
@Test
void ignoreHitsMakesGroundedPitsPassableExceptForMoldorm() {
    RoomEntity ordinary = entity(0x09, 0);
    for (int physics : new int[] {0x0B, 0x50, 0x51}) {
        RomTables tables = tables(0, 0, 0, 0, ordinary.type(), 0);
        assertTrue(resolve(tables, ordinary, 0, 0, physics).blocked());
        assertFalse(resolveWithIgnoreHits(tables, ordinary, 0, 0, physics, 1).blocked());
    }

    RoomEntity moldorm = entity(0x59, 0);
    RomTables moldormTables = tables(0, 0, 0, 0, moldorm.type(), 0);
    assertTrue(resolveWithIgnoreHits(moldormTables, moldorm, 0, 0, 0x50, 1).blocked());

    RoomEntity airborne = entity(ordinary.type(), 1);
    assertFalse(resolve(moldormTables, airborne, 0, 0, 0x50).blocked());
}
```

Add a helper overload that calls:

```java
return new EntityBackgroundCollisionResolver(tables).resolve(
    entity, direction,
    new EntityCollisionPointProbe.Sample(sampleX, sampleY), 0x22,
    physics, ignoreHitsCountdown);
```

- [x] **Step 2: Run the resolver test and verify RED.** Run:

```bash
gradle -p java test --tests linksawakening.world.EntityBackgroundCollisionResolverTest
```

Expected: compilation fails because the resolver has no countdown-aware overload.

### Task 2: Verify the state-aware runtime boundary with failing tests

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [x] **Step 1: Add the runtime adapter regression.** In `RoomEntityRuntimeTest`, add a Moblin recoil test using a rich probe that passes and a legacy boolean callback that blocks:

```java
@Test
void richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement() {
    RoomEntity initial = new RoomEntity(0, 0, 0x0B, 0x40, 0x40,
        EntityStatus.ACTIVE, pairDefinition(0x0B, 2), 0);
    RoomEntityRuntime runtime = RoomEntityRuntime.from(snapshot(initial));
    List<Integer> observedIgnoreHits = new ArrayList<>();
    runtime.setBackgroundInteraction(new RoomEntityBackgroundInteraction() {
        @Override
        public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                       int nextX, int nextY) {
            return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        }

        @Override
        public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                       int nextX, int nextY,
                                                       int ignoreHitsCountdown) {
            observedIgnoreHits.add(ignoreHitsCountdown);
            return EntityBackgroundCollisionResult.passable(direction, 0, nextX, nextY);
        }
    });

    List<EntityCombatEvent> events = runtime.resolveCombat(
        0, 0x40, 0x40, false, false, true,
        0x30, 0x30, 0x30, 0x30);
    assertEquals(1, events.size());
    assertTrue(runtime.enemyRecoilActive(0));

    runtime.tick(0, 0, 0, () -> 0,
        (entity, direction, nextX, nextY) -> true);

    assertTrue(observedIgnoreHits.contains(0x09));
    assertEquals(0x3D, runtime.snapshot().slots().get(0).x());
}
```

- [x] **Step 2: Add the RoomSession state regression.** Add this test, using the existing helpers:

```java
@Test
void liveEntityCollisionProbeUsesActiveIgnoreHitsCountdownForGroundedPits() {
    RoomSession session = newSession();
    session.loadIndoor(0x00, 0x0F);
    RoomEntity entity = session.activeRoom().entities().loadedEntities().stream()
        .filter(candidate -> candidate.type() == 0x1E)
        .findFirst()
        .orElseThrow();
    fillActiveObjects(session, 0x01); // Indoors1 object $01 is normal pit.

    session.setEntityIgnoreHitsCountdownForTest(entity.slot(), 1);
    assertFalse(session.entityBackgroundCollisionResultForTest(
        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y()).blocked());

    session.setEntityIgnoreHitsCountdownForTest(entity.slot(), 0);
    assertTrue(session.entityBackgroundCollisionResultForTest(
        entity, EntityBackgroundCollisionResult.RIGHT, entity.x(), entity.y()).blocked());
}
```

- [x] **Step 3: Run the two tests and verify RED.** Run:

```bash
gradle -p java test \
  --tests linksawakening.world.RoomEntityRuntimeTest.richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement \
  --tests linksawakening.world.RoomSessionTest.liveEntityCollisionProbeUsesActiveIgnoreHitsCountdownForGroundedPits
```

Expected: the new runtime test fails to compile at the state-aware `@Override`, and the session test remains red until the session forwards live state.

### Task 3: Add the state-aware probe and resolver overload

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityBackgroundInteraction.java`
- Modify: `java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java`

- [x] **Step 1: Add the compatibility-preserving default probe.** Keep the four-argument method abstract and add:

```java
default EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                               int nextX, int nextY,
                                               int ignoreHitsCountdown) {
    return probe(entity, direction, nextX, nextY);
}
```

- [x] **Step 2: Add the resolver overload.** Keep the existing `resolve` signature and delegate it with zero:

```java
return resolve(entity, direction, sample, objectId, physicsFlag, 0);
```

Add the six-argument overload, pass the value into `isBlocked`, and change only the pit/lava branch to:

```java
if (physicsFlag == PHYSICS_LAVA
    || physicsFlag == PHYSICS_NORMAL_PIT
    || physicsFlag == PHYSICS_PIT_WARP) {
    if (entity.z() != 0) {
        return false;
    }
    if ((ignoreHitsCountdown & UNSIGNED_BYTE_MASK) != 0
        && entity.type() != ENTITY_MOLDORM) {
        return false;
    }
    return !noWall;
}
```

Define `ENTITY_MOLDORM = 0x59` beside the other entity constants. Do not modify any other physics branch.

- [x] **Step 3: Run the resolver test and verify GREEN.** Run the focused resolver command again and expect PASS.

### Task 4: Wire RoomSession and legacy movement handlers to live state

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`

- [x] **Step 1: Give RoomSession a state-aware rich probe.** Add this field beside the existing collision resolver field:

```java
private final RoomEntityBackgroundInteraction entityBackgroundInteraction =
    new RoomEntityBackgroundInteraction() {
        @Override
        public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                       int nextX, int nextY) {
            return entityBackgroundCollisionResult(entity, direction, nextX, nextY, 0);
        }

        @Override
        public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction,
                                                       int nextX, int nextY,
                                                       int ignoreHitsCountdown) {
            return entityBackgroundCollisionResult(
                entity, direction, nextX, nextY, ignoreHitsCountdown);
        }
    };
```

Replace both `this::entityBackgroundCollisionResult` registrations with
`entityBackgroundInteraction`. Route both methods to this private overload:

```java
private EntityBackgroundCollisionResult entityBackgroundCollisionResult(
        RoomEntity entity, int direction, int nextX, int nextY,
        int ignoreHitsCountdown) {
    EntityCollisionPointProbe.Sample sample = entityCollisionPointProbe.sample(
        entity, direction, nextX, nextY);
    int pointX = sample.x();
    int pointY = sample.y();
    int objectId = overworldCollision.objectIdAtPoint(pointX, pointY);
    int physicsFlag = overworldCollision.objectPhysicsFlagAtPoint(pointX, pointY);
    return entityBackgroundCollisionResolver.resolve(
        entity, direction, sample, objectId, physicsFlag, ignoreHitsCountdown);
}
```

The existing four-argument helper must read
`entityRuntime.enemyIgnoreHitsCountdown(entity.slot())` when a runtime is
active, otherwise use zero, so direct session tests observe the same state.

- [x] **Step 2: Adapt the runtime's old collision callback.** At the beginning of `tickInternal`, after null validation and before the entity loop, if `backgroundInteraction != null`, replace the local `backgroundCollision` callback with a lambda that calls:

```java
backgroundInteraction.probe(entity, direction, nextX, nextY,
    enemyIgnoreHitsCountdown[entity.slot()]).blocked()
```

Do not alter the public tick overloads. Runtime callers with only the old callback continue using it unchanged.

- [x] **Step 3: Run the runtime and session regressions.** Run:

```bash
gradle -p java test \
  --tests linksawakening.world.RoomEntityRuntimeTest.richBackgroundProbeSuppliesIgnoreHitsToLegacyRecoilMovement \
  --tests linksawakening.world.RoomSessionTest.liveEntityCollisionProbeUsesActiveIgnoreHitsCountdownForGroundedPits
```

Expected: both pass, including the countdown value observed after the existing recoil decrement (`$09` for the `$0A` sword-hit countdown).

- [x] **Step 4: Commit the focused implementation.** Commit the interface, resolver, session, runtime, and tests:

```bash
git add java/src/main/java/linksawakening/world/RoomEntityBackgroundInteraction.java \
    java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java \
    java/src/main/java/linksawakening/world/RoomSession.java \
    java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
    java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java \
    java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
    java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "feat: propagate entity ignore-hits collision state"
```

### Task 5: Verify and document the parity increment

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [x] **Step 1: Run focused collision coverage.** Run:

```bash
gradle -p java test \
  --tests linksawakening.world.EntityBackgroundCollisionResolverTest \
  --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.world.RoomSessionTest
```

- [x] **Step 2: Run the complete Java suite and whitespace verification.** Run:

```bash
gradle -p java clean test
git diff --check HEAD~1..HEAD
```

Expected: `BUILD SUCCESSFUL` and no whitespace errors.

- [x] **Step 3: Record the exact scope and remaining gaps.** Add a dated roadmap entry stating that the shared resolver now receives the live ignore-hits countdown, that grounded pits/lava/pit-warps pass during nonzero countdown except Moldorm, and that ledge timers, switch-block state, hookshot transitions, and per-handler decrement timing remain separate work.

- [x] **Step 4: Commit the roadmap entry.** Run:

```bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record entity collision ignore-hits propagation"
```
