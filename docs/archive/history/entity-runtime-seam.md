# Entity runtime seam migration

`RoomEntityRuntimeBuilder` is the single composition root. Its assembly method
uses local variables in dependency order and returns the runtime only after
room initialization. No controller receives the builder or a service bundle.
`RoomEntityRuntime` owns execution and stable role references; its marked seam
section contains only delegates required by callers outside the entity tree.

The role accessors return the behavior owner directly, without a forwarding
interface. Room callers can migrate each expression below independently. The
bridges preserve validation, overload behavior and return types at current
call sites; remove each bridge after its listed external callers migrate.

## Roles

| Accessor | Owner |
| --- | --- |
| `combat()` | `EntityCombatResolver` |
| `pickups()` | `EntityRewardController` |
| `spawns()` | `EntityPlayerSpawns` |
| `enemySpawns()` | `EntityEnemySpawns` |
| `lifecycle()` | `EntitySlotLifecycle` |
| `outbox()` | `EntityEffectConsumption` |
| `commands()` | `EntityPlayerCommands` |
| `background()` | `EntityBackgroundMechanics` |
| `inspection()` | `EntityRoomInspection` |
| `configuration()` | `EntityRuntimeConfiguration` |
| `debug()` | `EntityDebugConfiguration` |
| `enemies()` | `EntityEnemyInspection` |

The package-private `pipeline`, `execution`, `bosses`, `flyingEnemies` and
`pickupState` accessors support owned fixtures. They do not expose runtime
fields. Frame scratch remains nested: its input latch, timer gates, current-slot
scratch and presentation flags share the frame acquisition/reset/release lifecycle.

## Remaining seam bridges

Signatures distinguish overloads. Caller paths are from an attributed Java
source inventory, not a text search for similarly named room/session methods.
`runtime` denotes the room-held runtime. The construction bridge can migrate to
the public builder constructor.

| Old call | Replacement | Current external caller files |
| --- | --- | --- |
| `RoomEntityRuntime.builder(snapshot)` | `new RoomEntityRuntimeBuilder(snapshot)` | `java/src/main/java/linksawakening/world/room/RoomFollowerSynchronizer.java`<br>`java/src/main/java/linksawakening/world/room/RoomLoadCoordinator.java`<br>`java/src/test/java/linksawakening/world/room/GameplayRouteScenario.java`<br>`java/src/test/java/linksawakening/world/room/RoomDoorAndObjectInteractionTest.java`<br>`java/src/test/java/linksawakening/world/room/RoomLiveBoundaryTest.java`<br>`java/src/test/java/linksawakening/world/room/RoomRenderAllocationTest.java`<br>`java/src/test/java/linksawakening/world/room/RoomRewardLifecycleTest.java`<br>`java/src/test/java/linksawakening/world/room/RoomSessionProjectilesTest.java`<br>`java/src/test/java/linksawakening/world/room/RoomSessionStoryTest.java`<br>`java/src/test/java/linksawakening/world/room/RoomSessionTestSupport.java`<br>`java/src/test/java/linksawakening/world/room/RoomStoryPersistenceTest.java` |
| `gelClingingToLink()` | `runtime.enemies().gelClingingToLink()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `setFreeMovementMode(boolean)` | `runtime.configuration().setFreeMovementMode(enabled)` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `deferContactRecoil(int,int,int)` | `runtime.combat().deferContactRecoil(slot, linkX, linkY)` | `java/src/main/java/linksawakening/world/room/RoomEntityCombatBridge.java` |
| `clearEntity(int)` | `runtime.lifecycle().clearEntity(slot)` | `java/src/main/java/linksawakening/world/room/RoomEntityCombatBridge.java`<br>`java/src/test/java/linksawakening/world/room/RoomLiveBoundaryTest.java` |
| `setHeartPiecesCount(int)` | `runtime.configuration().setHeartPiecesCount(count)` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `thrownDirection(int)` | `runtime.commands().thrownDirection(slot)` | `java/src/main/java/linksawakening/world/room/RoomCollisionQueries.java` |
| `collectIfNeeded(EntityPickupFrameInputs)` | `runtime.pickups().collectIfNeeded(inputs)` | `java/src/main/java/linksawakening/world/room/RoomEntityCombatBridge.java` |
| `resolveCombat(EntitySwordContactFrame)` | `runtime.combat().resolveCombat(inputs)` | `java/src/test/java/linksawakening/world/room/RoomSessionTestSupport.java` |
| `resolveCombat(EnemyCombatInputs)` | `runtime.combat().resolveCombat(inputs)` | `java/src/main/java/linksawakening/world/room/RoomEntityCombatBridge.java` |
| `consumePendingSwitchBlockAnimationRequest()` | `runtime.outbox().consumePendingSwitchBlockAnimationRequest()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingClearedEntityMask()` | `runtime.outbox().consumePendingClearedEntityMask()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingRoomStatusMask()` | `runtime.outbox().consumePendingRoomStatusMask()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingEntityEvents()` | `runtime.outbox().consumePendingEntityEvents()` | `java/src/main/java/linksawakening/world/room/RoomObjectMutations.java`<br>`java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingThrownBackgroundCollisions()` | `runtime.outbox().consumePendingThrownBackgroundCollisions()` | `java/src/main/java/linksawakening/world/room/RoomPuzzleController.java` |
| `consumePendingDungeonWarpRequests()` | `runtime.outbox().consumePendingDungeonWarpRequests()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingWarpLinkStateRequests()` | `runtime.outbox().consumePendingWarpLinkStateRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkFinalPositionRequests()` | `runtime.outbox().consumePendingLinkFinalPositionRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingRoosterLinkStateRequests()` | `runtime.outbox().consumePendingRoosterLinkStateRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkMotionBlockRequests()` | `runtime.outbox().consumePendingLinkMotionBlockRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingSideViewPlatformLinkRequests()` | `runtime.outbox().consumePendingSideViewPlatformLinkRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingSideViewPotLinkRequests()` | `runtime.outbox().consumePendingSideViewPotLinkRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingHinoxLinkEffectRequests()` | `runtime.outbox().consumePendingHinoxLinkEffectRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkFallenPoseRequests()` | `runtime.outbox().consumePendingLinkFallenPoseRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkFacingRequests()` | `runtime.outbox().consumePendingLinkFacingRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkAttackClearRequests()` | `runtime.outbox().consumePendingLinkAttackClearRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkAttackStepRequests()` | `runtime.outbox().consumePendingLinkAttackStepRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingRoomStatusPersistenceRequests()` | `runtime.outbox().consumePendingRoomStatusPersistenceRequests()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingLinkHeldItemPoseRequests()` | `runtime.outbox().consumePendingLinkHeldItemPoseRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkSwordSpinPoseRequests()` | `runtime.outbox().consumePendingLinkSwordSpinPoseRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLinkSwordFinalPoseRequests()` | `runtime.outbox().consumePendingLinkSwordFinalPoseRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingScreenShakeRequests()` | `runtime.outbox().consumePendingScreenShakeRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingChestRewardEvents()` | `runtime.outbox().consumePendingChestRewardEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingKeyRewardEvents()` | `runtime.outbox().consumePendingKeyRewardEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingSlimeKeyRewardEvents()` | `runtime.outbox().consumePendingSlimeKeyRewardEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingKeyQuicksandEvents()` | `runtime.outbox().consumePendingKeyQuicksandEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingMusicTrack()` | `runtime.outbox().consumePendingMusicTrack()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingMusicFadeOutCountdown()` | `runtime.outbox().consumePendingMusicFadeOutCountdown()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingLikeLikeEvents()` | `runtime.outbox().consumePendingLikeLikeEvents()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingDialogRequests()` | `runtime.outbox().consumePendingDialogRequests()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumeBombExplosionEvents()` | `runtime.outbox().consumeBombExplosionEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumeHeartPieceUpdates()` | `runtime.outbox().consumeHeartPieceUpdates()` | `java/src/main/java/linksawakening/world/room/RoomSession.java` |
| `consumePendingHeartContainerRewards()` | `runtime.outbox().consumePendingHeartContainerRewards()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingMadamMeowMeowFullHealRequests()` | `runtime.outbox().consumePendingMadamMeowMeowFullHealRequests()` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `consumePendingSwordPickupRewards()` | `runtime.outbox().consumePendingSwordPickupRewards()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingToadstoolRewards()` | `runtime.outbox().consumePendingToadstoolRewards()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingInstrumentRewards()` | `runtime.outbox().consumePendingInstrumentRewards()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingInstrumentCompletions()` | `runtime.outbox().consumePendingInstrumentCompletions()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingRichardProgressEvents()` | `runtime.outbox().consumePendingRichardProgressEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingWitchExchangeEvents()` | `runtime.outbox().consumePendingWitchExchangeEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingWitchRewardEvents()` | `runtime.outbox().consumePendingWitchRewardEvents()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `consumePendingOwlEventCompletions()` | `runtime.outbox().consumePendingOwlEventCompletions()` | `java/src/main/java/linksawakening/world/room/RoomEntityRewards.java` |
| `beginLift(int,int)` | `runtime.commands().beginLift(slot, romDirection)` | `java/src/main/java/linksawakening/world/room/RoomLiftInteractions.java` |
| `throwLiftedEntity(int)` | `runtime.commands().throwLiftedEntity(romDirection)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `applyMarinWakeUpPresentation(int,int,int)` | `runtime.commands().applyMarinWakeUpPresentation(x, y, spriteVariant)` | `java/src/main/java/linksawakening/world/room/RoomNpcPresentation.java`<br>`java/src/test/java/linksawakening/world/room/RoomLiveBoundaryTest.java` |
| `updateHouseTarinPresentation(int,int)` | `runtime.commands().updateHouseTarinPresentation(linkX, linkY)` | `java/src/main/java/linksawakening/world/room/RoomNpcPresentation.java` |
| `spawnHookshotChain(int,int,int,int)` | `runtime.spawns().spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnPushedBlock(int,int)` | `runtime.spawns().spawnPushedBlock(x, y)` | `java/src/main/java/linksawakening/world/room/RoomDoorInteractions.java` |
| `spawnLiftedRoomObject(int,int,int,int)` | `runtime.spawns().spawnLiftedRoomObject(x, y, romDirection, spriteVariant)` | `java/src/main/java/linksawakening/world/room/RoomLiftInteractions.java` |
| `movePushedBlock(int,int,int)` | `runtime.commands().movePushedBlock(slot, x, y)` | `java/src/main/java/linksawakening/world/room/RoomPuzzleController.java` |
| `resolvePushedBlockLinkContact(int,int,int)` | `runtime.background().resolvePushedBlockLinkContact(slot, linkEntityX, linkEntityY)` | `java/src/main/java/linksawakening/world/room/RoomPuzzleController.java` |
| `removePushedBlock(int)` | `runtime.commands().removePushedBlock(slot)` | `java/src/main/java/linksawakening/world/room/RoomPuzzleController.java` |
| `spawnRoomEventKeyDrop()` | `runtime.spawns().spawnRoomEventKeyDrop()` | `java/src/main/java/linksawakening/world/room/RoomPuzzleController.java` |
| `spawnArrow(int,int,int,int,boolean)` | `runtime.spawns().spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnBoomerang(int,int,int,int,int)` | `runtime.spawns().spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnSwordBeam(int,int,int,int)` | `runtime.spawns().spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnMagicRodFireball(int,int,int,int,boolean)` | `runtime.spawns().spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnMagicPowderSprinkle(int,int,int,int)` | `runtime.spawns().spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnBomb(int,int,int,int)` | `runtime.spawns().spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection)` | `java/src/main/java/linksawakening/world/room/RoomPlayerItemActions.java` |
| `spawnLiftableRockSmash(int,int,int)` | `runtime.spawns().spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant)` | `java/src/main/java/linksawakening/world/room/RoomObjectMutations.java` |
| `spawnLiftableRockRubble(int,int)` | `runtime.spawns().spawnLiftableRockRubble(entityX, entityY)` | `java/src/main/java/linksawakening/world/room/RoomObjectMutations.java` |
| `spawnShovelDrop(int,int,int,int,boolean)` | `runtime.spawns().spawnShovelDrop(objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop)` | `java/src/main/java/linksawakening/world/room/RoomEntitySimulation.java` |
| `spawnChestWithItem(int,int,int)` | `runtime.spawns().spawnChestWithItem(objectLeft, objectTop, itemType)` | `java/src/main/java/linksawakening/world/room/RoomChestInteractions.java` |

## Removed internal forwarding

Entity-owned tests now call the owning role. Unused runtime overloads for simple
combat inputs, pickups, ordinary arrows/fireballs, enemy-bomb spawning and
internal slot queries were removed. Their behavior remains on the appropriate
combat, reward, spawn, lifecycle or slot owner. Validation in deferred recoil and
thrown-direction queries moved with the operation. The pipeline owns frame RNG
preparation and the Link-speed/deferred-recoil cleanup around a tick.

## Constructor ownership review

The immutable entity values and the combat/physics columns use the same fixed
ROM slot identity. `EntitySlots` owns all three, and its descending free-slot
search resets its own combat column. Controllers receive that slot owner rather
than independently supplied combat and physics tables. The cached list view is
also obtained from the same owner. This prevents mismatched table wiring rather
than merely hiding constructor arguments in a record.

Inspection and sprite-cache arguments which were the same instance at both
positions are supplied once. Internal read aliases retain their original query
behavior. No new Services/Dependencies bundle or forwarding interface was added.

Some constructors intentionally still have more than eight inputs. Frame
execution names the separate behavior owners actually used by handlers. The
runtime constructor names its public roles. Lifecycle must reset independent
entity-family owners at the original slot-reset sites, and inspection must read
those families without owning their behavior. These are explicit edges, not
parameters to combine into an aggregate service locator merely to meet a limit.
The review groups the shared slot storage and removes duplicate identities;
it does not change the order of lifecycle resets or move queries through a hub.

### Constructor decisions by owner

All changes below are cohesive grouping or removal of duplicate identities; no
new controller slices or dependency records were introduced. Immutable frame,
event and snapshot value constructors are data contracts, not lists of service
collaborators, and were not regrouped.

| Constructor | Decision |
| --- | --- |
| `EntityBackgroundMechanics` | Combat and physics columns come from `EntitySlots`; one sprite-cache instance replaces duplicate parameters. |
| `EntityBombInteractions` | Combat and physics columns come from `EntitySlots`. |
| `EntityBossRewards` | Combat and physics columns come from `EntitySlots`. |
| `EntityCombatResolver` | Combat and physics columns come from `EntitySlots`. |
| `EntityEnemyInspection` | Combat and physics columns come from `EntitySlots`; one inspection instance replaces duplicate parameters. |
| `EntityEnemySpawns` | Combat and physics columns come from `EntitySlots`. |
| `EntityFrameExecution` | Combat and physics columns come from `EntitySlots`; slot list comes from the same owner. |
| `EntityLiftThrowController` | Combat and physics columns come from `EntitySlots`. |
| `EntityPickupController` | Combat and physics columns come from `EntitySlots`. |
| `EntityPlayerCommands` | Combat and physics columns come from `EntitySlots`. |
| `EntityPlayerSpawns` | Combat and physics columns come from `EntitySlots`; one inspection instance replaces duplicate parameters. |
| `EntityProjectileController` | Combat and physics columns come from `EntitySlots`. |
| `EntityRewardController` | Combat and physics columns come from `EntitySlots`; one sprite-cache instance replaces duplicate parameters. |
| `EntityRoomInitialization` | Combat and physics columns come from `EntitySlots`. |
| `EntityRoomInspection` | Combat and physics columns come from `EntitySlots`. |
| `EntityRuntimeConfiguration` | Physics columns come from `EntitySlots`; one sprite-cache instance replaces duplicate parameters. |
| `EntitySlotLifecycle` | Combat and physics columns come from `EntitySlots`. |
| `EntitySlotProperties` | Combat and physics columns come from `EntitySlots`; one inspection instance replaces duplicate parameters. |
| `EntityStoryController` | Combat and physics columns come from `EntitySlots`; one inspection instance replaces duplicate parameters; one sprite-cache instance replaces duplicate parameters. |
| `GenieEntityController` | Combat and physics columns come from `EntitySlots`. |
| `SlimeEyeController` | Combat and physics columns come from `EntitySlots`; slot list comes from the same owner. |
| `RoomEntityRuntime` | Retains explicit execution and role references; grouping these into a service bundle would recreate the removed hub. |

### Shape enforcement

`architecture/EntityRuntimeShapeTest` checks compiled references through the
shared reader, the exact public seam signatures, and private ownership of every
mutable field. The reader does not decode field-write bytecodes, so the final
check uses the stronger allowed alternative: no non-private non-final fields.
The runtime itself and `RoomEntityRuntimeBuilder` are the only entity main
classes permitted to reference the runtime type. Nested frame scratch remains
with its shared acquisition/reset/release lifecycle. Cross-owner Link-state
reads/writes use owner methods; the arithmetic and operation order are unchanged.

### Final validation

The required ROM-configured `./gradlew spotlessApply check` passes with fatal
javac lint and Error Prone: 2,994 tests, zero failures/errors, one existing skip.
No existing tests or fixtures were removed or weakened. The runtime shape test
contributes three assertions as separate tests.

With `-XX:-DoEscapeAnalysis` supplied through a temporary, uncommitted init
script, the representative entity tick measured 834.59 bytes (budget 1,100),
two-enemy combat 400 bytes (budget 570), cached sprite and repeated Marin
presentation zero, idle session zero and active session 137 bytes per frame.
The cold falling-definition decode remains 2,480 bytes; cached reads allocate
zero. Existing allocation guard thresholds remain unchanged.
