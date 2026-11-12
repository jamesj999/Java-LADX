# Room facade migration

## Done

Round 3 migrated all owned app/gameplay/render and test callers to role views,
frame inputs and frame results. All nine compatibility interfaces and every
unused delegate are deleted. `RoomSession` extends nothing. The four boundary
records are top-level; owned constructors use `RoomSessionConfig`. No pending
app migration entries remain from the former 104-delegate list.

## Remaining entity-test bridges

These are the only deprecated public members on `RoomSession`: 26 methods and
one constructor. They live directly in its single marked deprecated-bridges
section. The coordinator explicitly requires retention while the entity engineer
rewrites `world/entity` main/tests. There are no remaining off-limits main-source
callers. Do not reintroduce compatibility inheritance.

Paths below are relative to `java/src/`. Parameter types disambiguate overloads.

| Member | Replacement | Remaining caller files |
| --- | --- | --- |
| `RoomSession(byte[],GPU,RoomLoader,OverworldTilesetTable,OverworldCollision,TransientVfxSystem,DroppableRupeeSystem)` | `RoomSession(RoomSessionConfig)` | `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java`; `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `tickEntities(int,int,int)` | `entities().tickEntities()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `tickEntities(int,int,int,int,int)` | `entities().tickEntities()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `tickEntities(int,int,int,int,boolean,int)` | `entities().tickEntities()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `tarinFlag()` | `story().tarinFlag()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `setTarinFlag(int)` | `story().setTarinFlag()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `setEntityDialogActive(boolean)` | `entityState().setEntityDialogActive()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `setEntityTalkState(boolean,int,int)` | `entityState().setEntityTalkState()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `setEntityActionButtonsHeld(boolean,boolean)` | `entityState().setEntityActionButtonsHeld()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `setEntityScreenShakeCountdown(int)` | `entityState().setEntityScreenShakeCountdown()` | `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java` |
| `setEntityAttackStepAnimationCountdown(int)` | `entityState().setEntityAttackStepAnimationCountdown()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `consumeLinkAttackClearRequests()` | `entityState().consumeLinkAttackClearRequests()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `consumeEntityDialogRequests()` | `entityState().consumeEntityDialogRequests()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `initializeNewGameWorldState()` | `story().initializeNewGameWorldState()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `restoreRoomStatuses(byte[],byte[],byte[],byte[])` | `story().restoreRoomStatuses()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `setChestPlayerLevels(int,int,int)` | `story().setChestPlayerLevels()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `shouldGetLostInMysteriousWoods()` | `story().shouldGetLostInMysteriousWoods()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `sprinkleMagicPowder(int,int,int,int)` | `playerItems().sprinkleMagicPowder()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `tickEntitiesWithProjectileEvents(int,int,int,int,int,int,boolean)` | `entities().tickEntitiesWithProjectileEvents()` | `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java` |
| `resolveEntityCombat(int,int,int,boolean,boolean,boolean,int,int,int,int,EnemyAttackContext)` | `entities().resolveEntityCombat(EntityCombatInputs)` | `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java` |
| `loadInitialOverworld(int)` | `rooms().loadInitialOverworld()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `loadOverworld(int)` | `rooms().loadOverworld()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `loadIndoor(int,int)` | `rooms().loadIndoor()` | `test/java/linksawakening/world/entity/SlimeEyeControlsTest.java`; `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java`; `test/java/linksawakening/world/entity/SlimeEyeSessionTest.java`; `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `entityHealth(int)` | `entityState().entityHealth()` | `test/java/linksawakening/world/entity/SlimeEyeControlsTest.java` |
| `activeRoom()` | `rooms().activeRoom()` | `test/java/linksawakening/world/entity/SlimeEyeControlsTest.java`; `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java`; `test/java/linksawakening/world/entity/SlimeEyeSessionTest.java`; `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `palettes()` | `rooms().palettes()` | `test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java` |
| `renderSnapshot()` | `rooms().renderSnapshot()` | `test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java` |

## Reflection alias

`RoomSession.context` is a private deprecated alias of `RoomSessionComponents`,
used only by `test/java/linksawakening/world/entity/SlimeEyeControlsTest.java`
to reach `entityBridge.entityRuntime`. It is not a `RoomSessionContext` bag.
Keep it until that off-limits test migrates. Runtime collaborators receive
specific typed constructor dependencies and do not read the wiring class.

## Remaining long signature

`resolveEntityCombat(int,int,int,boolean,boolean,boolean,int,int,int,int,EnemyAttackContext)`
is the sole explicit method/constructor signature above eight parameters in
room/transition/object main sources. Its only caller is
`test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java`; replace it with
`entities().resolveEntityCombat(EntityCombatInputs)` when that engineer is ready.
The retained seven-argument constructor is used by that test and
`TarinRaccoonRuntimeTest`; both should migrate to `RoomSessionConfig`.

See the [round-3 report](room-facade-round-3.md) and
[architecture-test mapping](room-architecture-test-migration.md).
