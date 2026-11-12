# Entity API migration

The entity refactor retains the adapters below while callers outside
`world.entity` migrate. Caller names reflect the integrated round-3 checkout;
all paths are under `java/src/main/java/linksawakening` unless marked as tests.
No adapter changes defaults, byte masking, slot order or collision ordering.

## Test-access compatibility

| Old member | Replacement | Current external callers |
| --- | --- | --- |
| `EntityDebugConfiguration.setEnemyIgnoreHitsCountdownForTest(slot, value)` | `runtime.debug().setEnemyIgnoreHitsCountdown(slot, value)` | `world.room.RoomSessionViews` |
| `EntityDebugConfiguration.setColorShellStateForTest(slot, state, countdown, direction, speedX, speedY)` | `runtime.debug().setColorShellState(...)` with the same arguments | `world.room.RoomSessionViews` |
| `EntityRoomInspection.warpTransitionCountdownForTest(slot)` | `runtime.inspection().warpTransitionCountdown(slot)` | `world.room.RoomSessionViews` |

These are the only three production `ForTest` methods in the entity tree. They
remain deprecated delegates; owned tests use the test-side `EntityRuntimeFixture`
and `RoomEntityBuilder` instead.

## Construction and wide adapters

| Old member | Replacement | Current external callers |
| --- | --- | --- |
| `RoomEntityRuntime.from(initial)` | `RoomEntityRuntime.builder(initial).build()` | Tests: `world.room.RoomSessionState1Test`, `RoomSessionProjectilesTest`, `RoomSessionRewards1Test`, `RoomSessionStoryTest` |
| `RoomEntityRuntime.from(initial, indoor)` | `builder(initial).indoorRoom(indoor).build()` | Tests: `world.room.RoomSessionRewards1Test`, `RoomSessionRewards2Test`, `RoomSessionDoors1Test`, route support `GameplayRouteStage12` |
| `RoomEntityRuntime.from(initial, indoor, rng, sprites, combat)` | `builder(initial).indoorRoom(indoor).randomByteSupplier(rng).spriteHandlers(sprites).enemyCombatTables(combat).build()` | Test: `world.room.RoomSessionTestSupport` |
| `RoomEntityRuntime.from(initial, indoor, rng, sprites, combat, chests, tables, bytes)` | Previous builder chain plus `.chestContentsTable(chests).romTables(tables).rom(rom).build()` | `world.room.RoomActiveRoomInitializer`, `RoomFollowerSynchronizer` |
| `EntityFrameContext` 20-argument constructor | `EntityFrameContext.builder().frameCounter(frameCounter)` and named setters; omitted `creditsGameplay` is false, `handlerLinkCollisionEnabled` is true | `world.room.RoomEntitySimulation` |
| `RoomEntityRuntime.collectIfNeeded` 12 arguments | `collectIfNeeded(EntityPickupFrame)` | `world.room.RoomEntityCombatBridge` |
| `RoomEntityRuntime.resolveCombat` 10 arguments | `resolveCombat(EntitySwordContactFrame)`; retains zero vertical speed and standard attack context | Test: `world.room.RoomSessionTestSupport` |
| `RoomEntityRuntime.resolveCombat` 12 arguments | `resolveCombat(EnemyCombatFrame)` | `world.room.RoomEntityCombatBridge` |
| `FollowingNpcEntitySpawner.synchronize` 9 arguments | `synchronize(FollowingNpcSpawn)` | `world.room.RoomFollowerSynchronizer` |
| `EntityCombatEvent` 10/11-argument constructors | `EntityCombatEvent.builder(slot, type)` with named damage, sound and VFX setters | Test: `gameplay.EnemyCombatEventConsumerTest` |
| `EntityProjectileEvent` 9/11-argument constructors | `EntityProjectileEvent.builder(slot, type, kind)` with named collision, damage, sound, removal and sword-poke setters | Tests: `gameplay.EnemyProjectileEventConsumerTest`, `audio.music.GameplayMusicControllerTest` |
| `RoomEntityRuntime.BOOMERANG_SFX_COUNTER_PERIOD` | `EntityRomConstants.BOOMERANG_SFX_COUNTER_PERIOD` | `world.room.RoomEntityBridge` |

Event builders preserve the old constructor defaults: absent sounds use
`NONE/-1`, absent sword-poke coordinates and Link motion writes are zero, and
combat special damage defaults to `-1`. Validation still occurs in the immutable
event constructor. Canonical record constructors remain available.

## Typed ROM boundaries

| Old member | Replacement | Current external callers |
| --- | --- | --- |
| `EntityRoomLoader(byte[])` | `EntityRoomLoader(Rom)` | `world.room.RoomLoader` |
| `EnemyDropResolver(byte[])` | `EnemyDropResolver(Rom)` | `world.room.RoomSessionContext`; test `RoomSessionTestSupport` |
| `RomEnemyCombatTables(byte[])` | `RomEnemyCombatTables(Rom)` | `world.room.RoomSessionContext`; test `RoomSessionTestSupport` |
| `RoomEntityRuntimeBuilder.romData(byte[])` | `.rom(Rom)` | No direct current caller; retained for the approved compatibility window |
| Eight-argument `RoomEntityRuntime.from(..., byte[])` | `.rom(Rom)` on the builder above | `world.room.RoomActiveRoomInitializer`, `RoomFollowerSynchronizer` |
| `EntityBackgroundCollisionResolver(RomTables)` | `EntityBackgroundCollisionResolver(tables.collisionTables(), tables.entityPropertyTables())` | `world.room.RoomSessionContext` |
| `EntityCollisionPointProbe(RomTables)` | `EntityCollisionPointProbe(tables.collisionTables(), tables.entityPropertyTables())` | `world.room.RoomSessionContext` |

The five byte-array delegates are `@Deprecated(forRemoval = true)`. Their five
remaining `[removal]` warnings intentionally use `new Rom(bytes)`: replacing
that legacy boundary with `Rom.of(bytes)` would reject partial buffers earlier
and change existing validation behavior. New callers should pass the `Rom`
already owned by their session. No raw ROM parameters, fields or index reads
remain in the entity implementation beyond those delegates.

`EntityRoomLoader` still passes `rom.unsafeBytes()` to the external
`entity.EntitySpriteHandlerCatalog`, whose constructor currently requires a
byte array. That single handoff preserves shared-buffer ownership; migrate it
when the catalog exposes a typed constructor. Room-status byte arrays are RAM
state, not ROM data.

## Per-frame inspection

| Existing API | Alternative | Current external consumers |
| --- | --- | --- |
| `runtime.inspection().snapshot()` | `runtime.inspection().view()` for immediate, same-thread reads | `world.room` simulation, combat, item and room-view consumers |

`snapshot()` remains an owned, immutable copy. `view()` returns one cached,
read-only live view whose slots and presentation metadata follow the runtime.
Do not retain it as historical state or publish it across threads. Calling a
`withX` transformation on a live view first materializes an owned snapshot.
Room/app consumers must choose ownership explicitly before adopting the view;
this entity-only change does not change their current snapshot ownership.
