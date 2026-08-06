# ROM Enemy Death and Drop Lifecycle Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (- [ ]) syntax for tracking.

**Goal:** Connect the existing ROM-backed enemy death state to DidKillEnemy and SpawnEnemyDrop, so ordinary enemies update room persistence and produce visible, collectible ROM item entities with the original drop rules and timers.

**Architecture:** Decode the five drop tables and the low-health threshold directly from the shipped ROM in a pure EnemyDropResolver. Keep Guardian Acorn/Piece of Power counters and player/room context at the entity-runtime/session boundary, then resolve the drop at the terminal DYING frame before unloading the source slot. Spawn the chosen item in the highest free slot through EntitySpriteHandlerCatalog, the existing RoomEntity renderer, and the existing pickup path; preserve sourceLoadOrder == -1 for dynamic entities.

**Tech Stack:** Java records and immutable ROM-table decoders, JUnit 5, Gradle, the shipped azle.gbc test ROM, existing RoomEntityRuntime/RoomSession/PlayerState APIs.

---

## File map

- Create java/src/main/java/linksawakening/world/EnemyDropResolver.java for ROM table decoding, low-health threshold lookup, counter state, and the source-ordered common drop decision.
- Create java/src/test/java/linksawakening/world/EnemyDropResolverTest.java for table bytes, sentinels, counter gates, chance masks, fallback selection, and random-read order.
- Modify java/src/main/java/linksawakening/world/RoomEntityRuntime.java to hold per-slot dropped-item bytes, DidKillEnemy bookkeeping, drop context/counters, terminal death resolution, dynamic item setup, and timer state.
- Modify java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java for terminal death persistence, dynamic-entity exclusion, reverse-slot drop creation, source position/Z, and drop timers.
- Modify java/src/main/java/linksawakening/world/RoomSession.java to construct/configure the resolver, supply player/room context, carry counters across room loads, and apply the runtime's pending persistence mask.
- Modify java/src/test/java/linksawakening/world/RoomSessionTest.java for session-level counter/context propagation and a spawned item's live snapshot/pickup boundary.
- Modify java/src/main/java/linksawakening/Main.java to pass PlayerState.maxHearts(), health(), and activePowerUp() at the existing entity-tick boundary.
- Modify docs/reconstruction-roadmap.md only after focused and full verification, recording this incremental slice and its explicit deferred handler-specific drops.

### ROM addresses used by the implementation

All addresses below are CPU addresses in the bank named, and must be converted with RomBank.romOffset:

- Bank $03:$4826, 52 bytes: DestroyedEntityHealthGroupOffsetTable.
- Bank $03:$559D, 14 bytes: DropTableByIndex.
- Bank $03:$55AA, 14 bytes: RandomDropChanceTable.
- Bank $03:$55B8, 14 bytes: RandomDropChanceTableLowHealth.
- Bank $03:$55C7, 8 bytes: DropTableRandom.
- Bank $02:$6308, 15 bytes: ThresholdLowHealthTable.

The source locations are LADX-Disassembly/src/code/entities/bank3.asm:DestroyedEntityHealthGroupOffsetTable, LADX-Disassembly/src/code/entities/bank3.asm:DropTableByIndex, LADX-Disassembly/src/code/entities/bank3.asm:SpawnEnemyDrop, and LADX-Disassembly/src/code/bank2.asm:ThresholdLowHealthTable.

### Task 1: Decode ROM drop tables and implement the pure resolver

**Files:**
- Create: java/src/main/java/linksawakening/world/EnemyDropResolver.java
- Test: java/src/test/java/linksawakening/world/EnemyDropResolverTest.java

- [ ] **Step 1: Write failing ROM-decoder and sentinel tests.**

Add a resolver test fixture that loads rom/azle.gbc, wraps a random sequence in an AtomicInteger, and asserts every decoded table byte against the disassembly data:

~~~java
EnemyDropResolver resolver = new EnemyDropResolver(loadRom());
assertArrayEquals(new int[] {
    0x02, 0x06, 0x01, 0x03, 0x03, 0x03, 0x0D, 0x08,
    0x0A, 0x02, 0x07, 0x0B, 0x00, 0x04, 0x00, 0x08,
    0x04, 0x0E, 0x0E, 0x0E, 0x0E, 0x0E, 0x00, 0x03,
    0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03, 0x03,
    0x03, 0x02, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00,
    0x00, 0x06, 0x06, 0x0D, 0x0E, 0x00, 0x09, 0x03,
    0x06, 0x00, 0x02, 0x0E, 0x0E
}, resolver.destroyedHealthGroupOffsets());
assertArrayEquals(new int[] {
    0x2E, 0x2E, 0x2D, 0x2D, 0x37, 0x2D, 0xFF, 0xFF,
    0x2F, 0x37, 0x38, 0x2E, 0x2F, 0x2F
}, resolver.dropTableByIndex());
assertArrayEquals(new int[] {
    0x03, 0x01, 0x01, 0x00, 0x03, 0x03, 0x03,
    0x03, 0x01, 0x00, 0x00, 0x00, 0x03, 0x00
}, resolver.randomDropChanceTable());
assertArrayEquals(new int[] {
    0x01, 0x01, 0x01, 0x00, 0x01, 0x01, 0x01,
    0x01, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00
}, resolver.randomDropChanceTableLowHealth());
assertArrayEquals(new int[] {0x2E, 0x2D, 0x38, 0x2F, 0x2E, 0x2D, 0x38, 0x37},
    resolver.dropTableRandom());
assertArrayEquals(new int[] {
    0x00, 0x22, 0xC9, 0x05, 0x05, 0x05, 0x09, 0x09,
    0x09, 0x11, 0x11, 0x11, 0x19, 0x19, 0x19
}, resolver.thresholdLowHealthTable());

EnemyDropResolver.Result explicitNone = resolver.resolve(new EnemyDropResolver.Context(
    0x09, 0x00, 0xFF, 3, 6, false, 0, false,
    new EnemyDropResolver.CounterState(0, 0), () -> 0));
assertEquals(EnemyDropResolver.ENTITY_NONE, explicitNone.itemType());
assertEquals(0, explicitNone.randomReads());

EnemyDropResolver.Result explicitHeart = resolver.resolve(new EnemyDropResolver.Context(
    0x09, 0x00, 0x2D, 3, 6, false, 0, false,
    new EnemyDropResolver.CounterState(0, 0), () -> 0));
assertEquals(0x2D, explicitHeart.itemType());
assertEquals(0, explicitHeart.randomReads());
~~~

- [ ] **Step 2: Run the focused test to verify it fails.**

Run: gradle -p java test --tests linksawakening.world.EnemyDropResolverTest --no-build-cache

Expected: FAIL because EnemyDropResolver and its table accessors do not exist.

- [ ] **Step 3: Add the minimal ROM decoder and validated value records.**

Implement EnemyDropResolver with this public state shape:

~~~java
public record CounterState(int guardianAcornCounter, int pieceOfPowerKillCount) {
    public CounterState {
        if (guardianAcornCounter < 0 || guardianAcornCounter > 0xFF
            || pieceOfPowerKillCount < 0 || pieceOfPowerKillCount > 0xFF) {
            throw new IllegalArgumentException("Drop counters must be unsigned bytes");
        }
    }
}

public record Context(int entityType, int healthGroup, int droppedItem,
                      int maxHearts, int health, boolean bossBattle,
                      int activePowerUp, boolean sideScrolling,
                      CounterState counters, IntSupplier randomByteSupplier) {
    public Context {
        if ((entityType & ~0xFF) != 0 || (healthGroup & ~0xFF) != 0
            || (droppedItem & ~0xFF) != 0 || (maxHearts & ~0xFF) != 0
            || (health & ~0xFF) != 0 || (activePowerUp & ~0xFF) != 0) {
            throw new IllegalArgumentException("Drop context fields must be unsigned bytes");
        }
        Objects.requireNonNull(counters, "Drop counters cannot be null");
        Objects.requireNonNull(randomByteSupplier, "Drop random source cannot be null");
    }
}

public record Result(int itemType, CounterState counters, int randomReads) {
    public boolean dropped() {
        return itemType != ENTITY_NONE;
    }
}
~~~

Load each table with RomBank.romOffset, reject truncated ROM data, copy returned arrays from accessors, and mask every random byte with & 0xFF. Add isLowHealth(int maxHearts, int health) using the bank-$02 threshold table, clamping indexes above 14 to 14.

- [ ] **Step 4: Add source-ordered counter/chance/fallback tests.**

Add tests for direct nonzero drops, explicit $FF, Guardian Acorn threshold and its boss/power-up/side-scroll gates, all three Piece of Power thresholds and gates, health-group zero, normal versus low-health chance masks, the indexed table, and the eight-entry fallback. Verify random-call order with a counting supplier:

~~~java
@Test
void noneIndexedEntryConsumesASecondRandomByteForEightWayFallback() throws IOException {
    AtomicInteger index = new AtomicInteger();
    int[] bytes = {0x00, 0x07};
    EnemyDropResolver.Result result = new EnemyDropResolver(loadRom()).resolve(
        new EnemyDropResolver.Context(0x09, 0x06, 0x00, 3, 6, false, 0, false,
            new EnemyDropResolver.CounterState(0, 0),
            () -> bytes[index.getAndIncrement()]));
    assertEquals(0x37, result.itemType());
    assertEquals(2, result.randomReads());
}
~~~

Cover no-random-read early returns and assert counters reset at threshold even when the item is suppressed by a gate.

- [ ] **Step 5: Implement the resolver and run the focused tests.**

Implement resolve(Context) in this order: return no item for $FF; return the direct nonzero item; increment the Guardian Acorn counter and, at $0C, reset and return $34 only when all three source gates are false; read the health-group offset and return no item for zero; increment Piece of Power kills and select $1E/$23/$28 by max hearts, resetting at threshold and returning $33 only when all three source gates are false; read one chance byte and one random byte, returning no item when (random & chanceMask) != 0; read DropTableByIndex[offset], and when it is $FF, read a second random byte masked by $07 and use DropTableRandom.

Run: gradle -p java test --tests linksawakening.world.EnemyDropResolverTest --no-build-cache

Expected: PASS.

- [ ] **Step 6: Commit the pure resolver.**

~~~bash
git add java/src/main/java/linksawakening/world/EnemyDropResolver.java java/src/test/java/linksawakening/world/EnemyDropResolverTest.java
git commit -m "feat: decode ROM enemy drop rules"
~~~

### Task 2: Model terminal DidKillEnemy state in RoomEntityRuntime

**Files:**
- Modify: java/src/main/java/linksawakening/world/RoomEntityRuntime.java
- Test: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java

- [ ] **Step 1: Write failing terminal-death tests.**

Create a supported Keese/Octorok in slot 0 with EntityStatus.DYING, run enough frames to reach countdown zero, and assert the source is disabled, consumePendingClearedEntityMask() returns bit 0 for source order 0, and killCount()/killOrder() record the source load order. Add a dynamic source-order -1 test that asserts no kill count, no kill order, and no persistence bit.

Use the existing RoomEntityRuntime.from(snapshot, false, () -> 0, catalog, tables) constructor and this terminal assertion shape:

~~~java
runtime.tick(0x40, 120, 120, () -> 0);
assertFalse(runtime.snapshot().slots().get(0).loaded());
assertEquals(1, runtime.killCount());
assertEquals(0, runtime.killOrderAt(0));
assertEquals(1, runtime.consumePendingClearedEntityMask());
~~~

- [ ] **Step 2: Run the focused runtime tests to verify they fail.**

Run: gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest --no-build-cache

Expected: FAIL because terminal DYING currently calls disableEntityWithoutPersistence without DidKillEnemy bookkeeping.

- [ ] **Step 3: Add runtime state and reset it on every slot unload/spawn.**

Add these fields:

~~~java
private final int[] droppedItemBySlot = new int[EntityRoomLoader.MAX_ENTITIES];
private final int[] killOrder = new int[0x100];
private int killCount;
private EnemyDropResolver enemyDropResolver;
private EnemyDropResolver.CounterState enemyDropCounters =
    new EnemyDropResolver.CounterState(0, 0);
private int enemyDropMaxHearts = 3;
private int enemyDropHealth = 6;
private int enemyDropActivePowerUp;
private boolean enemyDropBossBattle;
~~~

Initialize the dropped-item array to zero in the constructor, clear its slot in both clearEntity and disableEntityWithoutPersistence, and expose package-visible methods:

~~~java
void setEnemyDropResolver(EnemyDropResolver resolver) { enemyDropResolver = resolver; }
void setEnemyDropPlayerState(int maxHearts, int health, int activePowerUp) {
    validateUnsignedByte(maxHearts, "max hearts");
    validateUnsignedByte(health, "health");
    validateUnsignedByte(activePowerUp, "active power-up");
    enemyDropMaxHearts = maxHearts;
    enemyDropHealth = health;
    enemyDropActivePowerUp = activePowerUp;
}
void setEnemyDropBossBattle(boolean bossBattle) { enemyDropBossBattle = bossBattle; }
void setDroppedItemForTest(int slot, int itemType) {
    validateEntitySlot(slot);
    validateUnsignedByte(itemType, "dropped item");
    droppedItemBySlot[slot] = itemType;
}
int droppedItemForTest(int slot) {
    validateEntitySlot(slot);
    return droppedItemBySlot[slot];
}
int killCount() { return killCount; }
int killOrderAt(int index) { return killOrder[index & 0xFF]; }
EnemyDropResolver.CounterState enemyDropCounters() { return enemyDropCounters; }
~~~

Validate setters as unsigned bytes and slots/indexes with the existing runtime helpers. Preserve all existing constructors by leaving drop resolution disabled when enemyDropResolver == null.

- [ ] **Step 4: Implement the terminal DidKillEnemy transition.**

Replace the DYING countdown-zero branch with:

~~~java
if (dyingCountdown[entity.slot()] == 0) {
    finishEnemyDeath(entity, randomByteSupplier);
} else {
    RoomEntity updated = refreshColorShellDisplay(entity, status);
    slots[index] = withDeathPresentation(updated,
        deathSpriteVariantForCountdown(dyingCountdown[entity.slot()]),
        powerRecoilDeath[entity.slot()]);
}
continue;
~~~

Implement finishEnemyDeath so it calls the resolver only for sourceLoadOrder >= 0 entities and otherwise unloads immediately. For a static source, resolve with enemyCombatTables.healthGroup(entity.type()), the per-slot dropped-item byte, supplied player/room fields, and the current frame-scoped random supplier; store the returned counter state; append the source load order to killOrder[killCount & 0xFF]; increment killCount modulo $100; OR persistentClearMask(entity) into pendingClearedEntityMask; then either spawn the resolved item or unload the source.

- [ ] **Step 5: Run the terminal runtime tests and commit.**

Run: gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest --no-build-cache

Expected: PASS, with the existing death-presentation tests unchanged.

~~~bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: record ROM enemy death persistence"
~~~

### Task 3: Spawn dynamic ROM item entities with source timers

**Files:**
- Modify: java/src/main/java/linksawakening/world/RoomEntityRuntime.java
- Test: java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java

- [ ] **Step 1: Write failing reverse-slot/timer/visibility tests.**

Fill every slot except 15 with loaded entities, configure slot 0 as a terminal dying Octorok with random-drop sentinel, and select the ROM indexed rupee path. Assert slot 15 is an active dynamic rupee with sourceLoadOrder() == -1, source X/Y/Z, a supported ROM sprite definition, slowTransitionCountdown(15) == 0x80, dropPrivateCountdown1(15) == 0x18, and dropPrivateCountdown3(15) == 0x03. Repeat with a side-scroll snapshot and assert dropSpeedY(15) == 0xEC; assert the top-down case has dropSpeedZ(15) == 0x18.

Also assert the new item is accepted by collectIfNeeded on the ROM pickup cadence and produces the existing EntityPickupEvent.

- [ ] **Step 2: Run the focused runtime tests to verify they fail.**

Run: gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest --no-build-cache

Expected: FAIL because terminal death does not create a dynamic item and the drop timer/speed state does not exist.

- [ ] **Step 3: Add drop timer/speed state and reverse-slot spawn.**

Add:

~~~java
private final int[] dropPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
private final int[] dropPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
private final int[] dropSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
private final int[] dropSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
~~~

Clear all four in both unload paths. Implement spawnEnemyDrop(RoomEntity source, int itemType) with findFreeEntitySlot(), spriteDefinitionFor(itemType), and the item definition's initialVariant():

~~~java
RoomEntity drop = new RoomEntity(freeSlot, -1, itemType,
    source.x(), source.y(), EntityStatus.ACTIVE, definition, variant,
    0, 0, source.z());
~~~

Set slowTransitionCountdown = 0x80, slowTimerInitialized = true, private countdowns 0x18 and 0x03, and side-scroll speed-Y 0xEC or top-down speed-Z 0x18. Mark dynamicEntitySpawnedThisFrame[freeSlot] = true so the new slot is not processed during the current reverse scan. If no slot is free, still unload and persist the source as the ROM does.

Keep the normal RoomEntityRuntime display and collectIfNeeded path. Do not fabricate a definition when EntitySpriteHandlerCatalog reports unsupported.

- [ ] **Step 4: Add timer decrement and accessors.**

Decrement the two new private timers once per frame, preserving zero, while leaving slowTransitionCountdown on the existing four-frame cadence. Expose validated package-visible accessors:

~~~java
int dropPrivateCountdown1(int slot) { return dropPrivateCountdown1[slot]; }
int dropPrivateCountdown3(int slot) { return dropPrivateCountdown3[slot]; }
int dropSpeedY(int slot) { return dropSpeedY[slot]; }
int dropSpeedZ(int slot) { return dropSpeedZ[slot]; }
~~~

On the next tick assert $18 -> $17 and $03 -> $02. Preserve byte-exact signed speed values; detailed terrain bounce behavior remains outside this slice.

- [ ] **Step 5: Run focused tests and commit the dynamic spawn.**

Run: gradle -p java test --tests linksawakening.world.RoomEntityRuntimeTest --no-build-cache

Expected: PASS.

~~~bash
git add java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java
git commit -m "feat: spawn ROM enemy death drops"
~~~

### Task 4: Wire session/player context and the live pickup boundary

**Files:**
- Modify: java/src/main/java/linksawakening/world/RoomSession.java
- Modify: java/src/main/java/linksawakening/Main.java
- Test: java/src/test/java/linksawakening/world/RoomSessionTest.java

- [ ] **Step 1: Write failing session-context tests.**

Construct a RoomSession with the shipped ROM, load an entity room containing a supported ordinary enemy, configure maxHearts = 3, health = 1, and no active power-up, then kill the enemy through the existing combat/tick boundary. Assert that low-health behavior uses the low-health chance table, the dynamic item remains in activeRoom().entities(), and collectEntityIfNeeded returns the existing pickup event at the spawned position. Add a room-transition test proving counters survive room loads.

- [ ] **Step 2: Run focused session tests to verify they fail.**

Run: gradle -p java test --tests linksawakening.world.RoomSessionTest --no-build-cache

Expected: FAIL because the session currently constructs no drop resolver, supplies no player context, and has no counter accessors.

- [ ] **Step 3: Configure resolver and persistent counters in RoomSession.**

Add:

~~~java
private final EnemyDropResolver enemyDropResolver;
private int enemyDropMaxHearts = 3;
private int enemyDropHealth = 6;
private int enemyDropActivePowerUp;
~~~

Construct enemyDropResolver = new EnemyDropResolver(romData) beside enemyCombatTables. Add setEnemyDropPlayerState(int maxHearts, int health, int activePowerUp) and a package-visible counter accessor. In both runtime construction paths, set the resolver, player fields, and isBossBattleForRoom().

Implement isBossBattleForRoom() by scanning activeRoom.entities().loadedEntities() and returning true when romTables.entityOptions1(entity.type()) & 0x80 or & 0x04 is nonzero. This is the room-boundary approximation of source wInBossBattle, with ROM option bits as authority. Do not reset the counters in clearTransientRoomState, loadOverworld, or loadIndoor.

- [ ] **Step 4: Pass current PlayerState values from the live loop.**

Immediately before the existing roomSession.tickEntitiesWithProjectileEvents call in Main.java, add:

~~~java
if (playerState != null) {
    roomSession.setEnemyDropPlayerState(
        playerState.maxHearts(), playerState.health(), playerState.activePowerUp());
}
~~~

Keep the existing RomRandomByteSource frame boundary unchanged. Do not add another random generator or a separate item-rendering call.

- [ ] **Step 5: Run focused session/Main tests and commit.**

Run: gradle -p java test --tests linksawakening.world.RoomSessionTest --tests linksawakening.MainTest --no-build-cache

Expected: PASS.

~~~bash
git add java/src/main/java/linksawakening/world/RoomSession.java java/src/main/java/linksawakening/Main.java java/src/test/java/linksawakening/world/RoomSessionTest.java
git commit -m "feat: wire enemy drops through the live room session"
~~~

### Task 5: Verify the increment, review the diff, and record the roadmap slice

**Files:**
- Modify: docs/reconstruction-roadmap.md

- [ ] **Step 1: Run focused regression tests.**

Run: gradle -p java test --tests linksawakening.world.EnemyDropResolverTest --tests linksawakening.world.RoomEntityRuntimeTest --tests linksawakening.world.RoomSessionTest --tests linksawakening.MainTest --no-build-cache

Expected: PASS.

- [ ] **Step 2: Run the complete Java suite from a clean build cache.**

Run: gradle -p java test --no-build-cache

Expected: BUILD SUCCESSFUL with all tests passing.

- [ ] **Step 3: Inspect source scope and formatting.**

Run:

~~~bash
git diff --check
git status --short
git diff --stat HEAD~4..HEAD
rg -n "TODO|TBD|FIXME|placeholder|fake|guessed" java/src/main/java/linksawakening/world/EnemyDropResolver.java java/src/main/java/linksawakening/world/RoomEntityRuntime.java java/src/main/java/linksawakening/world/RoomSession.java
~~~

Expected: no whitespace errors, only intended files changed, and no new placeholder or guessed-table language.

- [ ] **Step 4: Record the verified increment and explicit deferrals.**

Insert a dated section near the top of docs/reconstruction-roadmap.md stating that ordinary terminal enemy deaths now decode the five common drop tables and low-health threshold from ROM, update dynamic/static persistence correctly, preserve Guardian Acorn/Piece of Power counters, spawn reverse-slot ROM item entities with $80/$18/$03 timers and side-scroll speed-Y $EC, and reuse the renderer/pickup path. State that Like-Like shield recovery, handler-specific key writes, Color Dungeon scripts, bosses, bomb destruction, and detailed drop bounce/terrain motion remain pending because this slice does not fabricate their handler-owned writes.

- [ ] **Step 5: Commit documentation only after verification.**

~~~bash
git add docs/reconstruction-roadmap.md
git commit -m "docs: record ROM enemy death drops"
~~~
