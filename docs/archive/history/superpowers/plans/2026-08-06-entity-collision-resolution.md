# Shared entity-background collision resolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the shared entity-background probe follow the ROM's fine-shape, terrain-category, and entity-specific collision rules for the cases resolvable from the current Java room state.

**Architecture:** Keep `OverworldCollision` as the Link/ground policy and add a pure `EntityBackgroundCollisionResolver` for the bank-$03 `ApplyEntityCollisionWithObject` decision. Load the ROM fine-shape rows through `RomTables`, then delegate the existing `RoomSession` rich collision boundary to the resolver so the already-wired roaming enemy path receives the correction without duplicating logic in handlers.

**Tech Stack:** Java 21, JUnit 5, Gradle, shipped `azle.gbc` ROM, LADX disassembly.

---

### Task 1: Lock the ROM fine-shape table contract

**Files:**
- Modify: `java/src/test/java/linksawakening/rom/RomTablesTest.java`
- Modify: `java/src/main/java/linksawakening/rom/RomTables.java`

- [ ] **Step 1: Add the failing table-load assertion.** Extend `RomTablesTest` with:

```java
@Test
void loadsUnsignedFineCollisionRowsFromBankThree() {
    byte[] rom = new byte[0x100000];
    int offset = RomBank.romOffset(0x03, 0x7A85);
    int rowOffset = (0x80 - 0x7C) * 4;
    rom[offset + rowOffset] = (byte) 0xFF;
    rom[offset + rowOffset + 1] = 0x01;
    rom[offset + rowOffset + 2] = 0x00;
    rom[offset + rowOffset + 3] = (byte) 0x80;

    RomTables tables = RomTables.loadFromRom(rom);

    assertEquals(0xFF, tables.entityFineCollisionShape(0x80, 0));
    assertEquals(0x01, tables.entityFineCollisionShape(0x80, 1));
    assertEquals(0x00, tables.entityFineCollisionShape(0x80, 2));
    assertEquals(0x80, tables.entityFineCollisionShape(0x80, 3));
    assertEquals(0, tables.entityFineCollisionShape(0x8E, 0));
}
```

- [ ] **Step 2: Run the focused test and verify RED.** From the repository root run:

```bash
gradle -p java test --tests linksawakening.rom.RomTablesTest
```

Expected: compilation fails because `entityFineCollisionShape` does not yet exist.

- [ ] **Step 3: Add the ROM table constants, storage, and accessor.** In `RomTables`, add:

```java
private static final int ENTITY_FINE_COLLISION_BANK = 0x03;
private static final int ENTITY_FINE_COLLISION_ADDR = 0x7A85;
private static final int ENTITY_FINE_COLLISION_FIRST_PHYSICS = 0x7C;
private static final int ENTITY_FINE_COLLISION_LAST_PHYSICS = 0x8D;
private static final int ENTITY_FINE_COLLISION_ROW_SIZE = 4;
private static final int ENTITY_FINE_COLLISION_LENGTH =
    (ENTITY_FINE_COLLISION_LAST_PHYSICS - ENTITY_FINE_COLLISION_FIRST_PHYSICS + 1)
        * ENTITY_FINE_COLLISION_ROW_SIZE;
```

Store the rows as an `int[]`, load them with `loadUnsignedTable` in
`loadFromRom`, and pass them through the private constructor. Expose:

```java
public int entityFineCollisionShape(int physicsFlag, int quadrant) {
    if (physicsFlag < ENTITY_FINE_COLLISION_FIRST_PHYSICS
        || physicsFlag > ENTITY_FINE_COLLISION_LAST_PHYSICS
        || quadrant < 0 || quadrant >= ENTITY_FINE_COLLISION_ROW_SIZE) {
        return 0;
    }
    int index = (physicsFlag - ENTITY_FINE_COLLISION_FIRST_PHYSICS)
        * ENTITY_FINE_COLLISION_ROW_SIZE + quadrant;
    return entityFineCollisionShapes[index];
}
```

- [ ] **Step 4: Run the table test and commit the isolated ROM contract.** Run the same focused command and expect PASS, then commit:

```bash
git add java/src/main/java/linksawakening/rom/RomTables.java \
    java/src/test/java/linksawakening/rom/RomTablesTest.java
git commit -m "feat: load ROM entity fine collision shapes"
```

### Task 2: Define the resolver behavior with failing tests

**Files:**
- Create: `java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java`
- Test fixture uses: `java/src/main/java/linksawakening/world/RoomEntity.java`, `java/src/main/java/linksawakening/entity/EntitySpriteDefinition.java`

- [ ] **Step 1: Add a synthetic ROM fixture and entity helper.** Add a package-private test class with helpers equivalent to:

```java
private static RomTables tables(int fineRow0, int fineRow1,
                                int fineRow2, int fineRow3,
                                int entityType, int options) {
    byte[] rom = new byte[0x100000];
    int fineOffset = RomBank.romOffset(0x03, 0x7A85) + (0x80 - 0x7C) * 4;
    rom[fineOffset] = (byte) fineRow0;
    rom[fineOffset + 1] = (byte) fineRow1;
    rom[fineOffset + 2] = (byte) fineRow2;
    rom[fineOffset + 3] = (byte) fineRow3;
    rom[RomBank.romOffset(0x03, 0x42F1) + entityType] = (byte) options;
    return RomTables.loadFromRom(rom);
}

private static RoomEntity entity(int type, int z) {
    return new RoomEntity(0, 0, type, 0x40, 0x40, EntityStatus.ACTIVE,
        EntitySpriteDefinition.unsupported(type), -1, 0, 0, z);
}

private static EntityBackgroundCollisionResult resolve(
        RomTables tables, RoomEntity entity, int sampleX, int sampleY, int physics) {
    return new EntityBackgroundCollisionResolver(tables).resolve(
        entity, EntityBackgroundCollisionResult.RIGHT,
        new EntityCollisionPointProbe.Sample(sampleX, sampleY), 0x22, physics);
}
```

- [ ] **Step 2: Add the four-quadrant and broad-physics tests.** Assert a row `{1, 0, 1, 0}` maps to the exact quadrants `(0x00,0x00)`, `(0x08,0x00)`, `(0x00,0x08)`, and `(0x08,0x08)`. Assert `NONE` and deep water are passable for an ordinary entity, `SOLID` and `$60` are blocked, and the result keeps object `$22`, physics, sample coordinates, and the right-direction collision bit.

Add these exception assertions:

```java
assertFalse(resolve(tables(1, 1, 1, 1), entity(0x99, 0), 0, 0,
    PhysicsFlags.SHALLOW_WATER).blocked());
assertFalse(resolve(tables(1, 1, 1, 1), entity(0xCC, 0), 0, 0,
    PhysicsFlags.DEEP_WATER).blocked());
assertTrue(resolve(tables(1, 1, 1, 1), entity(0x99, 0), 0, 0,
    PhysicsFlags.SOLID).blocked());
assertTrue(resolve(tables(1, 1, 1, 1), entity(0x09, 0), 0, 0,
    PhysicsFlags.NORMAL_PIT).blocked());
assertFalse(resolve(tables(1, 1, 1, 1), entity(0x09, 1), 0, 0,
    PhysicsFlags.NORMAL_PIT).blocked());
assertFalse(resolve(tables(1, 1, 1, 1), entity(0x02, 0), 0, 0,
    0x80).blocked());
```

Add open-door Spark/boss tests using physics `$7C`, and a `NO_WALL_COLLISION`
test using an entity options byte of `$01` against `SOLID`. These tests must
also assert that a zero fine-shape row is passable and a nonzero row is
blocked.

- [ ] **Step 3: Run the resolver test and verify RED.** Run:

```bash
gradle -p java test --tests linksawakening.world.EntityBackgroundCollisionResolverTest
```

Expected: compilation fails because `EntityBackgroundCollisionResolver` does not yet exist.

### Task 3: Implement the ROM-shaped pure resolver and room wiring

**Files:**
- Create: `java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java`
- Modify: `java/src/main/java/linksawakening/world/RoomSession.java`
- Test: `java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java`

- [ ] **Step 1: Add the resolver interface and exact source constants.** Define a package-private final class with constructor `EntityBackgroundCollisionResolver(RomTables romTables)` and method:

```java
EntityBackgroundCollisionResult resolve(
    RoomEntity entity,
    int direction,
    EntityCollisionPointProbe.Sample sample,
    int objectId,
    int physicsFlag)
```

Use source values for Fish `$CC`, Water Tektite `$99`, Bomb `$02`, Wrecking
Ball `$A8`, Spark `$16/$17`, `NO_WALL_COLLISION` `$01`, boss `$80`, shallow
water `$05`, deep water `$07`, lava `$0B`, pit `$50/$51`, switch block `$04`,
fine range `$7C`–`$8F`, ledge `$D0`–`$D3`, tractor `$FF`, and the `$A0` broad
pass-through boundary.

- [ ] **Step 2: Implement the decision in source order.** Implement the boolean predicate behind `resolve` with this exact sequence:

```java
boolean noWall = (romTables.entityOptions1(entity.type()) & 0x01) != 0;
if (isWaterEntity(entity)) {
    if (physics == 0x05 || physics == 0x07) return false;
    return !noWall;
}
if (physics == 0x00) return false;
if (physics == 0x0B || physics == 0x50 || physics == 0x51) {
    return entity.z() == 0 && !noWall;
}
if (physics >= 0x7C && physics < 0x90) {
    if (physics < 0x80 && isSparkOrBoss(entity)) return true;
    if (physics >= 0x80 && isBombOrWreckingBall(entity)) return false;
    int quadrant = ((sample.x() & 0xFF) >>> 3 & 0x01)
        | (((sample.y() & 0xFF) >>> 3 & 0x01) << 1);
    if (romTables.entityFineCollisionShape(physics, quadrant) == 0) return false;
    return !noWall;
}
if (physics >= 0xD0 && physics < 0xD4) return true;
if (physics == 0xFF || physics == 0x04) return true;
if (physics >= 0xA0) return false;
if (physics >= 0x10) return !noWall;
if (physics == 0x01 || physics == 0x03) return !noWall;
return false;
```

Return `EntityBackgroundCollisionResult.blocked(...)` or
`passableWithObject(...)` with the original values and `sample.x()/sample.y()`.
The `$D0`–`$D3`, `$04`, and `$FF` branches are the documented conservative
fallbacks for WRAM state not yet exposed; do not add guessed switch or
hookshot state.

- [ ] **Step 3: Replace the Link-policy decision in `RoomSession`.** Add one field initialized from the existing `romTables`:

```java
private final EntityBackgroundCollisionResolver entityBackgroundCollisionResolver;
this.entityBackgroundCollisionResolver = new EntityBackgroundCollisionResolver(romTables);
```

In `entityBackgroundCollisionResult`, preserve the existing sample/object/physics lookup and replace the `overworldCollision.pointBlocked` plus Water Tektite conditional with:

```java
return entityBackgroundCollisionResolver.resolve(
    entity, direction, sample, objectId, physicsFlag);
```

Add a package-private test boundary immediately below it:

```java
EntityBackgroundCollisionResult entityBackgroundCollisionResultForTest(
        RoomEntity entity, int direction, int nextX, int nextY) {
    return entityBackgroundCollisionResult(entity, direction, nextX, nextY);
}
```

- [ ] **Step 4: Run focused resolver and existing collision tests.** Run:

```bash
gradle -p java test \
  --tests linksawakening.world.EntityBackgroundCollisionResolverTest \
  --tests linksawakening.world.EntityCollisionPointProbeTest \
  --tests linksawakening.world.RoamingEnemyMotionTest
```

Expected: PASS, with the old rich Octorok/Moblin collision contract unchanged except for the newly covered physics rules.

- [ ] **Step 5: Commit the resolver implementation.** Commit the table, resolver, and room-boundary changes together:

```bash
git add java/src/main/java/linksawakening/world/EntityBackgroundCollisionResolver.java \
    java/src/main/java/linksawakening/world/RoomSession.java \
    java/src/test/java/linksawakening/world/EntityBackgroundCollisionResolverTest.java
git commit -m "feat: resolve ROM entity background collision shapes"
```

### Task 4: Verify the live room boundary with an existing roaming enemy

**Files:**
- Modify: `java/src/test/java/linksawakening/world/RoomSessionTest.java`

- [ ] **Step 1: Add the shipped-ROM regression.** Load overworld room `$2F`, select its room-defined Octorok (`$09`), and use the existing active-room helper to fill the padded active cells. Probe through `entityBackgroundCollisionResultForTest` with direction `RIGHT` and the Octorok's current coordinates:

```java
@Test
void liveOctorokUsesEntityPhysicsInsteadOfLinkBlockingPolicy() {
    RoomSession session = newSession();
    session.loadInitialOverworld(0x2F);
    RoomEntity octorok = session.activeRoom().entities().loadedEntities().stream()
        .filter(entity -> entity.type() == 0x09)
        .findFirst()
        .orElseThrow();

    fillActiveObjects(session, 0x0E); // Overworld deep-water physics $07.
    EntityBackgroundCollisionResult water =
        session.entityBackgroundCollisionResultForTest(
            octorok, EntityBackgroundCollisionResult.RIGHT, octorok.x(), octorok.y());
    assertFalse(water.blocked());
    assertEquals(0x0E, water.objectId());
    assertEquals(PhysicsFlags.DEEP_WATER, water.physicsFlag());

    fillActiveObjects(session, 0x00); // Overworld solid physics $01.
    EntityBackgroundCollisionResult solid =
        session.entityBackgroundCollisionResultForTest(
            octorok, EntityBackgroundCollisionResult.RIGHT, octorok.x(), octorok.y());
    assertTrue(solid.blocked());
    assertEquals(0x00, solid.objectId());
    assertEquals(PhysicsFlags.SOLID, solid.physicsFlag());
}
```

- [ ] **Step 2: Run the room-session regression.** Run:

```bash
gradle -p java test --tests linksawakening.world.RoomSessionTest
```

Expected: PASS, including the existing Water Tektite and deep-water unload tests.

- [ ] **Step 3: Commit the live integration test.**

```bash
git add java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "test: verify live roaming entity collision physics"
```

### Task 5: Verify and record the increment

**Files:**
- Modify: `docs/reconstruction-roadmap.md`

- [ ] **Step 1: Record the verified scope.** Add a dated section immediately after `Verified ROM entity collision result boundary — 2026-08-06` stating that `FineCollisionShapes` at `$03:$7A85` is ROM-loaded, the entity resolver now distinguishes Link/entity water behavior, and the shipped-ROM Octorok probe covers deep-water passability and solid blocking. Explicitly list ignore-hits, ledge timers, switch-block state, hookshot-chain transitions, and other handler migrations as pending.

- [ ] **Step 2: Run the complete verification set.** From the repository root run:

```bash
gradle -p java clean test
git diff --check HEAD~4..HEAD
git status --short --branch
```

Expected: the complete Java suite passes, `git diff --check` prints no errors, and the worktree is clean on `feature/entity-runtime`.

- [ ] **Step 3: Commit the roadmap update.**

```bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record entity collision resolution"
```

