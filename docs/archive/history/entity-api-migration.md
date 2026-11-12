# Entity API migration

## Done

Round 4 migrated the room seam and removed every unused adapter from the entity
round-3 migration list: the three `ForTest` delegates, four `RoomEntityRuntime.from`
overloads, boomerang constant alias, positional frame constructor, wide
combat/pickup/follower operations, five byte-array delegates and two aggregate
ROM-table constructors. Room callers use typed builders, parameter records and
specific table holders. No raw ROM parameters or storage remain in the entity
implementation.

Use `RoomEntityRuntime.builder(initial)` for construction;
`EntityFrameContext.builder()` for immutable frame inputs;
`EntityPickupFrame`, `EnemyCombatFrame`, `EntitySwordContactFrame` and
`FollowingNpcSpawn` for operations. The frame builder preserves the original
handler-collision default derived from `projectileLinkState.motionState() < 3`; omitted `creditsGameplay` remains false.

Tests use `EntityRuntimeFixture`, `RoomEntityBuilder` and inspection helpers.
Immediate same-thread room reads use `runtime.inspection().view()`; retained
frames and public room/render snapshots remain owned. A live view must not be
retained as historical state or shared across threads. A `withX` operation on
a live view materializes owned data first.

## Remaining external consumer constructors

The following four deprecated signatures remain only for off-limits tests.
Use the named event builders; their immutable canonical constructors preserve
validation and the original damage, sound, VFX and motion defaults.

| Constructor | Replacement | External test callers under `java/src/test/java/linksawakening/` |
| --- | --- | --- |
| `EntityCombatEvent` (10 arguments) | `EntityCombatEvent.builder(slot, type)` | `gameplay/EnemyCombatEventConsumerTest.java` |
| `EntityCombatEvent` (11 arguments) | Same builder | `gameplay/EnemyCombatEventConsumerTest.java` |
| `EntityProjectileEvent` (9 arguments) | `EntityProjectileEvent.builder(slot, type, kind)` | `gameplay/EnemyProjectileEventConsumerTest.java`; `audio/music/GameplayMusicControllerTest.java` |
| `EntityProjectileEvent` (11 arguments) | Same builder | `gameplay/EnemyProjectileEventConsumerTest.java` |

The external sprite-handler catalog still accepts `byte[]`; its documented
`rom.unsafeBytes()` handoff preserves the original shared-buffer ownership.
Room-status arrays are mutable RAM, not ROM data. The complete boundary inventory,
allocation measurements and public-surface audit are in
[the round-4 report](room-seam-round-4.md).
