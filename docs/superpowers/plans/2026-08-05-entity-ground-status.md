# Entity Ground Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the ROM ground-status and water-transition phase of `ApplyEntityInteractionWithBackground` into the Java entity runtime, including source-backed water splash rendering and audio.

**Architecture:** `OverworldCollision` will expose one padded-buffer terrain sample. `RoomSession` will apply ROM options/physics/entity exceptions and return a result-bearing ground-interaction callback. `RoomEntityRuntime` will own per-slot ground status, lifecycle reset, and side-effect queues so the callback remains ordered after each entity handler. The existing transient VFX and raw sound-event boundaries will carry the ROM water splash.

**Tech Stack:** Java records/enums, JUnit 5, Gradle, ROM-backed tile decoding, existing `RoomEntityRuntime`, `TransientVfxSystem`, and `GameplaySoundSink` layers.

---

### Task 1: Commit the reviewed design and establish the terrain callback contract

**Files:**
- Create: `docs/superpowers/specs/2026-08-05-entity-ground-status-design.md`
- Create: `docs/superpowers/plans/2026-08-05-entity-ground-status.md`

- [ ] **Step 1: Verify the design and plan contain no unresolved markers or type names**

Run:

```bash
git diff --check -- docs/superpowers/specs/2026-08-05-entity-ground-status-design.md \
  docs/superpowers/plans/2026-08-05-entity-ground-status.md
```

Expected: no matches.

- [ ] **Step 2: Commit only the design and plan**

```bash
git add docs/superpowers/specs/2026-08-05-entity-ground-status-design.md \
  docs/superpowers/plans/2026-08-05-entity-ground-status.md
git commit -m "docs: plan entity ground status interactions"
```

### Task 2: Add the padded ROM terrain sample

**Files:**
- Modify: `java/src/main/java/linksawakening/physics/OverworldCollision.java`
- Test: `java/src/test/java/linksawakening/physics/OverworldCollisionTest.java`

- [ ] **Step 1: Write the failing sample test**

Add this test beside the existing ground-physics lookup test:

```java
@Test
void groundInteractionSampleReturnsObjectPhysicsAndAlignedCell() {
    byte[] rom = new byte[0x100000];
    int physicsOffset = RomBank.romOffset(0x08, 0x4AD4);
    rom[physicsOffset + 0x100 + 0x42] = 0x07;
    RomTables tables = RomTables.loadFromRom(rom);
    OverworldCollision collision = new OverworldCollision(tables);
    collision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);

    int[] roomObjects = new int[0x100];
    Arrays.fill(roomObjects, 0xFF);
    roomObjects[0x44] = 0x42;
    collision.setRoom(roomObjects);

    assertEquals(new OverworldCollision.GroundInteractionSample(
        0x42, 0x07, 0x30, 0x20),
        collision.groundInteractionSample(0x40, 0x37));
}
```

- [ ] **Step 2: Run the focused test and verify it fails for the missing API**

```bash
gradle test --tests linksawakening.physics.OverworldCollisionTest \
  --rerun-tasks
```

Expected: compilation/test failure because `GroundInteractionSample` and
`groundInteractionSample` do not exist.

- [ ] **Step 3: Implement the sample and delegate the old physics method**

Add this record and method to `OverworldCollision`:

```java
public record GroundInteractionSample(int objectId, int physicsFlag,
                                      int objectLeft, int objectTop) {}

public GroundInteractionSample groundInteractionSample(int entityX, int entityY) {
    if (roomObjectsArea == null) {
        return new GroundInteractionSample(0xFF, PhysicsFlags.NONE, 0, 0);
    }
    int sampleX = (entityX - 1) & 0xFF;
    int sampleY = (entityY - 7) & 0xFF;
    int objectLeft = sampleX & 0xF0;
    int objectTop = sampleY & 0xF0;
    int areaIndex = ROOM_OBJECTS_BASE + objectTop + (objectLeft >>> 4);
    if (areaIndex < 0 || areaIndex >= roomObjectsArea.length) {
        return new GroundInteractionSample(0xFF, PhysicsFlags.NONE,
            objectLeft, objectTop);
    }
    int objectId = roomObjectsArea[areaIndex] & 0xFF;
    return new GroundInteractionSample(objectId,
        romTables.objectPhysicsFlag(physicsTableIndex, objectId),
        objectLeft, objectTop);
}

public int objectPhysicsFlagAtGroundInteraction(int entityX, int entityY) {
    return groundInteractionSample(entityX, entityY).physicsFlag();
}
```

- [ ] **Step 4: Run the focused test and commit**

```bash
gradle test --tests linksawakening.physics.OverworldCollisionTest
git add java/src/main/java/linksawakening/physics/OverworldCollision.java \
  java/src/test/java/linksawakening/physics/OverworldCollisionTest.java
git commit -m "feat: expose ROM entity ground samples"
```

Expected: focused test passes.

### Task 3: Make entity ground interaction result-bearing and stateful

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomEntityGroundInteraction.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Test: `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`

- [ ] **Step 1: Write failing runtime tests for status and side effects**

Replace the two-argument callback in
`activeEntitiesPassThroughTheGroundInteractionBoundaryAfterMotion` with a
five-argument callback that returns the following result, and add this
assertion after the two ticks:

```java
assertEquals(0x02, runtime.groundStatus(0));
```

Add a separate test:

```java
@Test
void groundResultQueuesSplashAndUnloadsTheSlotInSourceOrder() {
    EntitySpriteDefinition definition = pairDefinition(0x4D, 1);
    RoomEntitySnapshot initial = snapshot(
        new RoomEntity(0, 0, 0x4D, 0x40, 0x50, EntityStatus.ACTIVE, definition, 0));
    RoomEntityRuntime runtime = RoomEntityRuntime.from(initial);
    runtime.setGroundInteraction((entity, frame, previousStatus, speedZ, sideScrolling) ->
        RoomEntityGroundInteraction.Result.unloaded(entity, 0x02, true));

    runtime.tick(0, 0, 0, () -> 0);

    assertEquals(EntityStatus.DISABLED, runtime.snapshot().slots().get(0).status());
    assertEquals(0, runtime.groundStatus(0));
    assertEquals(List.of(new RoomEntityRuntime.TransientVfxRequest(
        TransientVfxType.WATER_SPLASH, 0x40, 0x50)),
        runtime.transientVfxRequests());
    assertEquals(List.of(new EntityCombatEvent(0, 0x4D, 0, false,
        EntityCombatEvent.SoundChannel.JINGLE, 0x0E)),
        runtime.consumePendingEntityEvents());
}
```

- [ ] **Step 2: Run the focused test and verify the callback contract fails**

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest \
  --rerun-tasks
```

Expected: compilation failure because the callback has no result type,
`groundStatus` does not exist, and `WATER_SPLASH` is not yet defined.

- [ ] **Step 3: Implement the result record and per-slot status**

Make `RoomEntityGroundInteraction` contain:

```java
@FunctionalInterface
interface RoomEntityGroundInteraction {
    Result apply(RoomEntity entity, int frameCounter, int previousGroundStatus,
                 int speedZ, boolean sideScrolling);

    record Result(RoomEntity entity, int groundStatus, boolean unloaded,
                  boolean waterSplash) {
        static Result unchanged(RoomEntity entity, int groundStatus) {
            return new Result(entity, groundStatus, false, false);
        }

        static Result unloaded(RoomEntity entity, int groundStatus, boolean waterSplash) {
            return new Result(entity, groundStatus, true, waterSplash);
        }
    }
}
```

Add `entityGroundStatus[]`, initialize it to zero, clear it in both
`clearEntity` and `disableEntityWithoutPersistence`, and add package-visible:

```java
int groundStatus(int slot) {
    if (slot < 0 || slot >= entityGroundStatus.length) {
        throw new IllegalArgumentException("Entity slot out of range: " + slot);
    }
    return entityGroundStatus[slot];
}
```

At the existing post-handler callback, pass the prior status and the runtime's
vertical-speed byte, store the returned status, enqueue `WATER_SPLASH` and a
JINGLE `$0E` event when requested, then call
`disableEntityWithoutPersistence` and `continue` when `unloaded` is true.
The default callback returns `Result.unchanged(entity, 0)`.

- [ ] **Step 4: Update direct runtime callback callers and run tests**

Update the two existing lambdas in `RoomEntityRuntimeTest` to accept
`(entity, frameCounter, previousStatus, speedZ, sideScrolling)` and return
`Result.unchanged(updatedEntity, previousStatus)`. Run:

```bash
gradle test --tests linksawakening.world.RoomEntityRuntimeTest
```

Expected: focused runtime tests pass.

### Task 4: Port RoomSession's ROM ground-status policy

**Files:**
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Modify: `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- Modify: motion classes that already own a ROM Z-speed getter, if needed
- Test: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [ ] **Step 1: Add failing session tests for status mapping, positive-Z skip, and deep-water exception**

Add a helper that overwrites the active 10x8 object cells and tests using a
room/entity with a loaded sprite definition:

```java
@Test
void ordinaryEntityGetsTheRomShallowWaterGroundStatus() {
    RoomSession session = newSession();
    session.loadInitialOverworld(0x2F);
    RoomEntity entity = session.activeRoom().entities().loadedEntities().stream()
        .filter(candidate -> candidate.type() == 0x09)
        .findFirst().orElseThrow();
    fillActiveObjects(session, 0x05);

    session.tickEntities(0, 0, 0);
    session.tickEntities(1, 0, 0);

    assertEquals(EntityStatus.ACTIVE,
        session.activeRoom().entities().slots().get(entity.slot()).status());
    assertEquals(0x02, session.entityGroundStatusForTest(entity.slot()));
}
```

Add an equivalent deep-water test that asserts the ordinary entity becomes
disabled, then use entity `$A0` in a room containing PeaHat to assert it stays
active with status `$02`. Add a positive-Z case that sets the entity's Z to a
nonzero positive value before ticking and asserts status `$00`.

- [ ] **Step 2: Run the focused session tests and verify they fail**

```bash
gradle test --tests linksawakening.world.RoomSessionTest \
  --rerun-tasks
```

Expected: failure because the session still returns only the conveyor nudge
and exposes no ground status test hook.

- [ ] **Step 3: Implement the ROM policy in `entityGroundInteraction`**

Add constants for `ENTITY_OPT1_SPLASH_IN_WATER`, `ENTITY_FISH`, `ENTITY_PEAHAT`,
`ENTITY_ROOSTER`, `ENTITY_BOW_WOW`, `ENTITY_MARIN_AT_THE_SHORE`, object well
`$61`, physics lava `$0B`, deep water `$07`, side-scroll water `$B0`, shallow
water `$05`, and grass `$06`.

The callback must follow this order:

```java
int oldStatus = previousGroundStatus & 0xFF;
int currentStatus = 0;
int sampledPhysicsFlag = PhysicsFlags.NONE;
if ((options & ENTITY_OPT1_NO_GROUND_INTERACTION) == 0
    && ((entity.z() & 0xFF) == 0 || (entity.z() & 0x80) != 0)) {
    OverworldCollision.GroundInteractionSample sample =
        overworldCollision.groundInteractionSample(entity.x(), entity.y());
    int flag = sample.physicsFlag();
    sampledPhysicsFlag = flag;
    boolean deep = flag == PhysicsFlags.LAVA || flag == PhysicsFlags.DEEP_WATER;
    boolean retainedDeepWater = entity.type() == ENTITY_FISH
        || entity.type() == ENTITY_PEAHAT
        || entity.type() == ENTITY_ROOSTER
        || entity.type() == ENTITY_BOW_WOW
        || entity.type() == ENTITY_MARIN_AT_THE_SHORE;
    if (deep && !retainedDeepWater) {
        return new RoomEntityGroundInteraction.Result(entity, 0, true,
            splashAllowed(entity, oldStatus, 0, speedZ, sideScrolling,
                options));
    }
    if (deep) {
        currentStatus = 0x02;
    } else if (sample.objectId() == OBJECT_WATER_LADDER_SIDESCROLL
        || flag == PhysicsFlags.WATER_SIDESCROLL) {
        currentStatus = 0x01;
    } else if (flag != PhysicsFlags.NONE) {
        currentStatus = flag == PhysicsFlags.SHALLOW_WATER ? 0x02
            : flag == PhysicsFlags.GRASS ? 0x03 : 0x01;
    }
}
boolean splash = splashAllowed(entity, oldStatus, currentStatus, speedZ,
    sideScrolling, options);
RoomEntity updated = applyConveyorIfDue(entity, frameCounter, sampledPhysicsFlag);
return new RoomEntityGroundInteraction.Result(updated, currentStatus, false, splash);
```

Use a local sample even when no conveyor is present; do not re-read the room
object through the old physics-only method. Preserve the existing eight-entry
conveyor arrays and apply their four-frame gate after status/splash logic.
Implement `splashAllowed` with the ROM rules: options bit `$08`, status
change, neither status `$03`, top-down `speedZ` bit 7 set and signed speed
less than `$E7`, or the source side-scroll gate. Pass the current link motion
state from `tickEntitiesWithProjectileEvents` into a field used only during
the current entity pass, and add a package-visible
`entityGroundStatusForTest(int)` accessor.

- [ ] **Step 4: Add only the vertical-speed getters already represented by motion objects**

Expose `speedZ(int slot)` from `StalfosAggressiveMotion`,
`StalfosEvasiveMotion`, `TektiteMotion`, `ZolGelMotion`, `HidingZolMotion`,
`EnemyProjectileMotion`, and `ColorShellMotion` where absent. In
`RoomEntityRuntime`, select the getter by entity family and pass zero for
families without a shared Java speed table. Do not synthesize a speed from
the rendered `RoomEntity.z()` value.

- [ ] **Step 5: Run focused session tests and commit**

```bash
gradle test --tests linksawakening.physics.OverworldCollisionTest \
  --tests linksawakening.world.RoomEntityRuntimeTest \
  --tests linksawakening.world.RoomSessionTest
git add java/src/main/java/linksawakening/physics/OverworldCollision.java \
  java/src/main/java/linksawakening/world/RoomEntityGroundInteraction.java \
  java/src/main/java/linksawakening/world/RoomEntityRuntime.java \
  java/src/main/java/linksawakening/world/RoomSession.java \
  java/src/main/java/linksawakening/world/*Motion.java \
  java/src/test/java/linksawakening/physics/OverworldCollisionTest.java \
  java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java \
  java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "feat: port entity ground status interactions"
```

### Task 5: Add ROM water-splash VFX and audio mapping

**Files:**
- Modify: `java/src/main/java/linksawakening/vfx/TransientVfxType.java`
- Modify: `java/src/main/java/linksawakening/vfx/TransientVfxSpriteSheet.java`
- Modify: `java/src/main/java/linksawakening/vfx/CutLeavesEffectRenderer.java`
- Modify: `java/src/main/java/linksawakening/render/TransientVfxRenderLayer.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEvent.java`
- Modify: `java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java`
- Modify: `java/src/main/java/linksawakening/gameplay/EnemyCombatEventConsumer.java`
- Test: `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`
- Test: `java/src/test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java`
- Create: `java/src/test/java/linksawakening/gameplay/GameplaySoundEffectMapTest.java`

- [ ] **Step 1: Write the failing VFX/audio tests**

Add:

```java
@Test
void waterSplashRendererUsesRomTwoSpritePhases() throws IOException {
    CutLeavesEffectRenderer renderer = new CutLeavesEffectRenderer(
        TransientVfxSpriteSheet.loadFromRom(loadRom()));

    var first = renderer.renderWaterSplash(0x40, 0x60, 0x07);
    var second = renderer.renderWaterSplash(0x40, 0x60, 0x0F);

    assertEquals(2, first.size());
    assertEquals(0x40 - 0x08 - 0x02, first.get(0).x());
    assertEquals(0x60 - 0x10 - 0x0A, first.get(0).y());
    assertEquals(0x18, first.get(0).tileId());
    assertEquals(0x20, first.get(1).attributes());
    assertNotEquals(first.get(0).attributes(), second.get(0).attributes());
    assertNotNull(first.get(0).tile());
}
```

Add an event-consumer assertion that raw JINGLE `$0E` produces
`GameplaySoundEvent.WATER_SPLASH`, and create a catalog-map test that the
event resolves to JINGLE `$0E` named `JINGLE_WATER_SPLASH`.

- [ ] **Step 2: Run focused tests and verify they fail**

```bash
gradle test --tests linksawakening.vfx.TransientVfxSystemTest \
  --tests linksawakening.gameplay.EnemyCombatEventConsumerTest \
  --tests linksawakening.gameplay.GameplaySoundEffectMapTest --rerun-tasks
```

Expected: compilation failure for the missing enum/method/event mapping.

- [ ] **Step 3: Implement the source-shaped VFX and audio plumbing**

Add `WATER_SPLASH(0x01, 0x0F)` to `TransientVfxType`. Decode Link-character
tiles `$00..$1F` from bank `$0C` address `$4000` and retain character-VFX
tiles `$20..$3F` from `$4200` in `TransientVfxSpriteSheet`. Add the exact
`Data_002_57FD` placement table to `CutLeavesEffectRenderer`:

```java
private static final int[][] WATER_SPLASH_SPRITE_RECT = {
    {-10, -2, 0x18, 0x00, -8, 10, 0x18, 0x20},
    {-4,   0, 0x18, 0x00, -2,  8, 0x18, 0x20}
};
```

Select phase from countdown bit `$08`, subtract OAM X bias `$08` and Y bias
`$10`, and have `TransientVfxRenderLayer` dispatch `WATER_SPLASH` to it.
Add `WATER_SPLASH` to `GameplaySoundEvent`, map it to catalog JINGLE `$0E`,
and map raw JINGLE `$0E` in `EnemyCombatEventConsumer`.

- [ ] **Step 4: Run focused VFX/audio tests and commit**

```bash
gradle test --tests linksawakening.vfx.TransientVfxSystemTest \
  --tests linksawakening.gameplay.EnemyCombatEventConsumerTest \
  --tests linksawakening.gameplay.GameplaySoundEffectMapTest
git add java/src/main/java/linksawakening/vfx \
  java/src/main/java/linksawakening/render/TransientVfxRenderLayer.java \
  java/src/main/java/linksawakening/gameplay \
  java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java \
  java/src/test/java/linksawakening/gameplay/EnemyCombatEventConsumerTest.java \
  java/src/test/java/linksawakening/gameplay/GameplaySoundEffectMapTest.java
git commit -m "feat: render entity water splashes"
```

### Task 6: Verify the increment and update the roadmap

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Run the full clean suite**

```bash
gradle clean test
```

Expected: `BUILD SUCCESSFUL`, with zero failed or errored tests.

- [ ] **Step 2: Inspect the diff and update the roadmap with evidence**

```bash
git status --short
git diff HEAD~4 --check
git log -4 --oneline
```

Update the shared-terrain entry to state exactly which ground-status,
deep-water exception, splash, and rendering behaviors are now verified, and
leave pit falling/wall collision marked as pending. Do not claim complete
`ApplyEntityInteractionWithBackground`.

- [ ] **Step 3: Commit the roadmap update**

```bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record entity ground status parity"
```
