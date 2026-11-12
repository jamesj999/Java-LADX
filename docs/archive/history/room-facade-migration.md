# Room facade migration

The legacy room API remains available through role-specific compatibility
interfaces inherited by `RoomSession`. Internal forwarding calls now reach
the focused collaborators directly. Boundary values and public role views
are top-level types in `world.room`.

## Types retained until the app merge

These four records must **move to top-level after app merge**. They remain
nested and deprecated to avoid conflicts in files being rewritten by the app
engineer:

| Nested record | External source |
| --- | --- |
| `RoomSession.LinkGroundFrame` | `app/gameplay/LinkEquipmentPhase.java` |
| `RoomSession.ShovelStartResult` | `app/SessionBootstrap.java` |
| `RoomSession.ArrowShotResult` | `app/SessionBootstrap.java` |
| `RoomSession.ChestOpenResult` | `app/gameplay/DialogInteractions.java` |

Every legacy method is deprecated for removal. The replacement inventory below
includes all 104 externally used delegates, plus the remaining compatibility
entry points. Remove inherited compatibility interfaces and `facade()` after
external callers migrate.

## Reflection compatibility

`RoomSession.context` is a private, deprecated alias of `RoomSessionComponents`
solely for `world/entity/SlimeEyeControlsTest` reflection. Round 3 audited this
remaining caller and retained the alias under the coordinator’s rule forbidding
edits to entity tests. It does not retain
a `RoomSessionContext`. Remove this alias after that test migrates to the
entity test-access API. Runtime room collaborators receive typed dependency
records and do not retain the facade, wiring result, or wiring class.

## Long signatures retained for external callers

Owned callers use parameter objects. These four explicit long signatures remain
`@Deprecated(forRemoval = true)` until the listed files migrate:

| Legacy signature | Replacement input | External caller |
| --- | --- | --- |
| `RoomSession` constructor (9 parameters) | `RoomSessionConfig` | `app/SessionBootstrap.java` |
| `resolveEntityCombat` (11 parameters) | `EntityCombatInputs` | `world/entity/SlimeEyeRuntimeTest.java` |
| `resolveEntityCombat` (12 parameters) | `EntityCombatInputs` | `app/gameplay/CombatPhase.java` |
| `collectEntityIfNeeded` (12 parameters) | `EntityPickupInputs` | `app/gameplay/LinkEquipmentPhase.java` |

The separately approved nested `LinkGroundFrame` record still has nine
components; it moves after the app merge with the other three nested records.
`LoadedRoom` and `RoomBoundaryState` now group constructor inputs while retaining
flat accessors, equality, hash values, text and defensive-copy behavior.

## Delegate replacements

Signatures omit the `RoomSession.` prefix. Parameter objects in long-signature
replacements retain every original value and default explicitly.

| Legacy method | Replacement |
| --- | --- |
| `ActiveRoom activeRoom()` | `rooms().activeRoom()` |
| `ArrowShotResult shootArrowResult( int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower)` | `playerItems().shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower)` |
| `ArrowShotResult shootArrowResult(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)` | `playerItems().shootArrowResult(linkEntityX, linkEntityY, linkEntityZ, romDirection)` |
| `ChestOpenResult tryOpenChest( int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel)` | `doors().tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel)` |
| `EntityPickupEvent collectEntityIfNeeded( int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection)` | `entities().collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection)` |
| `EntityPickupEvent collectEntityIfNeeded( int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ)` | `entities().collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ)` |
| `EntityPickupEvent collectEntityIfNeeded( int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, int linkDirection, int linkZ, boolean swordActive, int swordX, int swordWidth, int swordY, int swordHeight)` | `entities().collectEntityIfNeeded(new EntityPickupInputs( frameCounter, new EntityPickupInputs.Link( linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ), new SwordBox(swordActive, swordX, swordWidth, swordY, swordHeight)))` |
| `EntityPickupEvent collectEntityIfNeeded( int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive)` | `entities().collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive)` |
| `EntityPickupEvent collectEntityIfNeeded(EntityPickupInputs inputs)` | `entities().collectEntityIfNeeded(inputs)` |
| `LiftedEntityState liftedEntityState()` | `rooms().liftedEntityState()` |
| `List<DialogRequest> consumeEntityDialogRequests()` | `entityState().consumeEntityDialogRequests()` |
| `List<EntityCombatEvent> resolveEntityCombat( int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext)` | `entities().resolveEntityCombat(new EntityCombatInputs( frameCounter, linkEntityX, linkEntityY, linkAirborne, 0, linkInteractive, new SwordBox(swordCollisionActive, swordX, swordWidth, swordY, swordHeight), attackContext))` |
| `List<EntityCombatEvent> resolveEntityCombat( int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, int linkVerticalVelocity, boolean linkInteractive, boolean swordCollisionActive, int swordX, int swordWidth, int swordY, int swordHeight, EnemyAttackContext attackContext)` | `entities().resolveEntityCombat(new EntityCombatInputs( frameCounter, linkEntityX, linkEntityY, linkAirborne, linkVerticalVelocity, linkInteractive, new SwordBox(swordCollisionActive, swordX, swordWidth, swordY, swordHeight), attackContext))` |
| `List<EntityCombatEvent> resolveEntityCombat(EntityCombatInputs inputs)` | `entities().resolveEntityCombat(inputs)` |
| `List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkMotionState, int linkDirection, boolean usingShield)` | `entities().tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkMotionState, linkDirection, usingShield)` |
| `List<EntityProjectileEvent> tickEntitiesWithProjectileEvents( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkMotionState, int linkDirection, boolean usingShield)` | `entities().tickEntitiesWithProjectileEvents(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield)` |
| `List<LinkAttackClearRequest> consumeLinkAttackClearRequests()` | `entityState().consumeLinkAttackClearRequests()` |
| `List<SwordPickupRewardEvent> consumeSwordPickupRewards()` | `entityState().consumeSwordPickupRewards()` |
| `RoomRenderSnapshot renderSnapshot()` | `rooms().renderSnapshot()` |
| `ShovelStartResult startShovel(int linkEntityX, int linkEntityY, int romDirection, boolean linkAirborne)` | `playerItems().startShovel(linkEntityX, linkEntityY, romDirection, linkAirborne)` |
| `String instrumentTrace()` | `rooms().instrumentTrace()` |
| `String moblinKingTrace()` | `rooms().moblinKingTrace()` |
| `Warp respawnPoint()` | `progress().respawnPoint()` |
| `boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant)` | `story().applyMarinWakeUpPresentation(x, y, spriteVariant)` |
| `boolean bombActive()` | `playerItems().bombActive()` |
| `boolean boomerangActive()` | `playerItems().boomerangActive()` |
| `boolean breakIndoorSwordCrystal(int location)` | `terrain().breakIndoorSwordCrystal(location)` |
| `boolean deferContactRecoil(int slot, int linkX, int linkY)` | `entityState().deferContactRecoil(slot, linkX, linkY)` |
| `boolean fireBoomerang( int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask)` | `playerItems().fireBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask)` |
| `boolean fireHookshot( int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean linkAirborne, boolean linkPushing)` | `playerItems().fireHookshot(linkEntityX, linkEntityY, linkEntityZ, romDirection, linkAirborne, linkPushing)` |
| `boolean fireMagicRodFireball( int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower)` | `playerItems().fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower)` |
| `boolean fireMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)` | `playerItems().fireMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection)` |
| `boolean fireSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)` | `playerItems().fireSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection)` |
| `boolean gelClingingToLink()` | `entityState().gelClingingToLink()` |
| `boolean gotItemPresentationActive()` | `entityState().gotItemPresentationActive()` |
| `boolean hasActiveRoom()` | `rooms().hasActiveRoom()` |
| `boolean heartPiecePickupActive()` | `rooms().heartPiecePickupActive()` |
| `boolean hookshotActive()` | `playerItems().hookshotActive()` |
| `boolean inBossBattle()` | `entities().inBossBattle()` |
| `boolean lastBombPlacementPlayedBump()` | `playerItems().lastBombPlacementPlayedBump()` |
| `boolean musicSelectionIsScroll()` | `rooms().musicSelectionIsScroll()` |
| `boolean ocarinaPlaying()` | `transitions().ocarinaPlaying()` |
| `boolean placeBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)` | `playerItems().placeBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection)` |
| `boolean powerUpPickupSequenceActive()` | `entityState().powerUpPickupSequenceActive()` |
| `boolean shouldGetLostInMysteriousWoods()` | `story().shouldGetLostInMysteriousWoods()` |
| `boolean sprinkleMagicPowder(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection)` | `playerItems().sprinkleMagicPowder(linkEntityX, linkEntityY, linkEntityZ, romDirection)` |
| `boolean startOcarina(int countdown, int songFlags, int selectedSong)` | `transitions().startOcarina(countdown, songFlags, selectedSong)` |
| `boolean swordPickupSequenceActive()` | `entityState().swordPickupSequenceActive()` |
| `boolean tailCaveKeyholeSequenceActive()` | `doors().tailCaveKeyholeSequenceActive()` |
| `boolean throwLiftedEntity(int linkDirection)` | `playerItems().throwLiftedEntity(linkDirection)` |
| `boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive)` | `terrain().tickCrackedFloor(frame, dialogGotItemActive)` |
| `boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive)` | `terrain().tickCrackedFloorOnLanding(frame, dialogGotItemActive)` |
| `boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType)` | `doors().tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType)` |
| `boolean tryLiftObject( int x, int y, int direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp)` | `doors().tryLiftObject(x, y, direction, held, buttons, power, level, anyPowerUp)` |
| `boolean tryLiftObject(int x, int y, int direction, boolean held, int buttons, boolean power, int level)` | `doors().tryLiftObject(x, y, direction, held, buttons, power, level)` |
| `boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType)` | `doors().tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType)` |
| `boolean tryUnlockKeyCavernKeyhole( int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount)` | `doors().tryUnlockKeyCavernKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount)` |
| `boolean tryUnlockTailCaveKeyhole( int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey)` | `doors().tryUnlockTailCaveKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey)` |
| `byte[] colorDungeonItemFlagsSnapshot()` | `dungeon().colorDungeonItemFlagsSnapshot()` |
| `byte[] colorDungeonRoomStatusSnapshot()` | `dungeon().colorDungeonRoomStatusSnapshot()` |
| `byte[] currentDungeonItemFlagsSnapshot()` | `dungeon().currentDungeonItemFlagsSnapshot()` |
| `byte[] dungeonItemFlagsSnapshot()` | `dungeon().dungeonItemFlagsSnapshot()` |
| `byte[] dungeonMinimapRoomStatusSnapshot()` | `dungeon().dungeonMinimapRoomStatusSnapshot()` |
| `byte[] dungeonProgressFlagsSnapshot()` | `dungeon().dungeonProgressFlagsSnapshot()` |
| `byte[] indoorARoomStatusSnapshot()` | `dungeon().indoorARoomStatusSnapshot()` |
| `byte[] indoorBRoomStatusSnapshot()` | `dungeon().indoorBRoomStatusSnapshot()` |
| `byte[] overworldRoomStatusSnapshot()` | `dungeon().overworldRoomStatusSnapshot()` |
| `int activeProjectileCount()` | `playerItems().activeProjectileCount()` |
| `int bowWowState()` | `story().bowWowState()` |
| `int clearEntity(int slot)` | `entityState().clearEntity(slot)` |
| `int currentRoomId()` | `rooms().currentRoomId()` |
| `int entityDefaultMusicTrack()` | `entityState().entityDefaultMusicTrack()` |
| `int entityHealth(int slot)` | `entityState().entityHealth(slot)` |
| `int indoorRoomPositionForSave()` | `rooms().indoorRoomPositionForSave()` |
| `int mapCategory()` | `rooms().mapCategory()` |
| `int mapId()` | `rooms().mapId()` |
| `int ocarinaAnimationState()` | `transitions().ocarinaAnimationState()` |
| `int respawnIndoorRoom()` | `progress().respawnIndoorRoom()` |
| `int richardSpokenFlag()` | `story().richardSpokenFlag()` |
| `int shovelAnimationState(int javaDirection, int timer)` | `playerItems().shovelAnimationState(javaDirection, timer)` |
| `int tarinFlag()` | `story().tarinFlag()` |
| `int tickDungeonTitleMessage( boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement)` | `transitions().tickDungeonTitleMessage(dialogActive, inventoryClosed, linkMotionState, freeMovement)` |
| `int tickTailCaveKeyholeSequence()` | `doors().tickTailCaveKeyholeSequence()` |
| `int[][] palettes()` | `rooms().palettes()` |
| `void advanceBoomerangSoundClock(boolean worldGameplay)` | `entityState().advanceBoomerangSoundClock(worldGameplay)` |
| `void advanceShovel(int linkEntityX, int linkEntityY, int romDirection, int timer)` | `playerItems().advanceShovel(linkEntityX, linkEntityY, romDirection, timer)` |
| `void clearGameOverPowerCounter()` | `progress().clearGameOverPowerCounter()` |
| `void initializeNewGameWorldState()` | `story().initializeNewGameWorldState()` |
| `void loadIndoor(int mapId, int roomId)` | `rooms().loadIndoor(mapId, roomId)` |
| `void loadIndoor(int mapId, int roomId, int mapCategory)` | `rooms().loadIndoor(mapId, roomId, mapCategory)` |
| `void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition)` | `rooms().loadIndoorFromSavedPosition(mapId, roomId, savedIndoorMapPosition)` |
| `void loadInitialOverworld(int roomId)` | `rooms().loadInitialOverworld(roomId)` |
| `void loadOverworld(int roomId)` | `rooms().loadOverworld(roomId)` |
| `void loadWarpDestination(Warp warp)` | `transitions().loadWarpDestination(warp)` |
| `void onMapBoundaryWarp(Runnable callback)` | `progress().onMapBoundaryWarp(callback)` |
| `void resetGameOverSwitchBlocks()` | `progress().resetGameOverSwitchBlocks()` |
| `void resetGuardianAcornCounterOnHurt()` | `entityState().resetGuardianAcornCounterOnHurt()` |
| `void restoreDungeonItemFlags(byte[] dungeonFlags, byte[] colorFlags)` | `dungeon().restoreDungeonItemFlags(dungeonFlags, colorFlags)` |
| `void restoreDungeonProgressFlags(byte[] flags)` | `dungeon().restoreDungeonProgressFlags(flags)` |
| `void restoreRoomStatuses( byte[] overworldStatus, byte[] indoorAStatus, byte[] indoorBStatus, byte[] colorDungeonStatus)` | `story().restoreRoomStatuses(overworldStatus, indoorAStatus, indoorBStatus, colorDungeonStatus)` |
| `void setBirdKeyOwned(boolean owned)` | `story().setBirdKeyOwned(owned)` |
| `void setBowWowState(int state)` | `story().setBowWowState(state)` |
| `void setChestPlayerLevels(int shieldLevel, int swordLevel, int powerBraceletLevel)` | `story().setChestPlayerLevels(shieldLevel, swordLevel, powerBraceletLevel)` |
| `void setColorShellSoundSink(GameplaySoundSink soundSink)` | `entityState().setColorShellSoundSink(soundSink)` |
| `void setDungeonItemForDebug(int mapId, int index, int value)` | `dungeon().setDungeonItemForDebug(mapId, index, value)` |
| `void setEntityActionButtonsHeld(boolean actionButtonAHeld, boolean actionButtonBHeld)` | `entityState().setEntityActionButtonsHeld(actionButtonAHeld, actionButtonBHeld)` |
| `void setEntityActionButtonsHeld(boolean actionButtonsHeld)` | `entityState().setEntityActionButtonsHeld(actionButtonsHeld)` |
| `void setEntityAttackStepAnimationCountdown(int countdown)` | `entityState().setEntityAttackStepAnimationCountdown(countdown)` |
| `void setEntityBombButtonHeld(boolean buttonHeld)` | `entityState().setEntityBombButtonHeld(buttonHeld)` |
| `void setEntityDefaultMusicTrack(int trackId)` | `story().setEntityDefaultMusicTrack(trackId)` |
| `void setEntityDialogActive(boolean active)` | `entityState().setEntityDialogActive(active)` |
| `void setEntityPowerBraceletButtonHeld(boolean buttonHeld)` | `entityState().setEntityPowerBraceletButtonHeld(buttonHeld)` |
| `void setEntityScreenShakeCountdown(int countdown)` | `entityState().setEntityScreenShakeCountdown(countdown)` |
| `void setEntityTalkState(boolean inventoryAppearing, int dialogCooldown, int windowY)` | `entityState().setEntityTalkState(inventoryAppearing, dialogCooldown, windowY)` |
| `void setHouseNpcTradeItem(int tradeItem)` | `story().setHouseNpcTradeItem(tradeItem)` |
| `void setRespawnPoint(Warp point, int indoorRoom)` | `progress().setRespawnPoint(point, indoorRoom)` |
| `void setRichardSpokenFlag(int flag)` | `story().setRichardSpokenFlag(flag)` |
| `void setSecretSeashellPegasusCollisionState( boolean screenShakeActive, boolean collisionActive, int collisionX, int collisionY)` | `entityState().setSecretSeashellPegasusCollisionState(screenShakeActive, collisionActive, collisionX, collisionY)` |
| `void setTailKeyOwned(boolean owned)` | `story().setTailKeyOwned(owned)` |
| `void setTarinFlag(int flag)` | `story().setTarinFlag(flag)` |
| `void setToadstoolPlayerState(boolean hasToadstool, int magicPowderCount)` | `entityState().setToadstoolPlayerState(hasToadstool, magicPowderCount)` |
| `void startAdjacentOverworldScroll( ScrollController scrollController, int direction, int linkScreenX, int linkScreenY)` | `transitions().startAdjacentOverworldScroll(scrollController, direction, linkScreenX, linkScreenY)` |
| `void tickEntities( int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, boolean linkAirborne, int linkDirection)` | `entities().tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection)` |
| `void tickEntities(int frameCounter)` | `entities().tickEntities(frameCounter)` |
| `void tickEntities(int frameCounter, int linkEntityX, int linkEntityY)` | `entities().tickEntities(frameCounter, linkEntityX, linkEntityY)` |
| `void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection)` | `entities().tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection)` |
| `void tickGameplayVBlank()` | `entityState().tickGameplayVBlank()` |
| `void updateHouseTarinPresentation(int frameCounter, int linkX, int linkY)` | `story().updateHouseTarinPresentation(frameCounter, linkX, linkY)` |

## Round 2 verification and metrics

The integration baseline is `111f087` after a clean fast-forward merge of
`develop`. All six implementation steps passed `spotlessApply test` with the
user-supplied ROM. The final suite has 2,747 tests, zero failures, zero errors,
and one existing native-audio skip. No test or opening fixture was removed,
weakened or disabled.

| Metric | Integration baseline | Final |
| --- | ---: | ---: |
| `RoomSession.java` lines | 2,348 | 373 |
| Declared public `RoomSession` methods (constructors excluded) | 138 | 25 |
| Context field accesses outside wiring (comments excluded) | 1,828 | 0 |
| Main files above 400 lines in room/transition/object | 6 | 0 |
| Longest method in those packages | 196 | 78 |
| Explicit method/constructor declarations above 8 parameters | 29 | 4 |
| `EntityPhase.java` lines | 393 | 166 |

The final public facade surface includes 117 inherited methods, for 142 total
public methods excluding `Object`. Of these, 128 compatibility methods are
deprecated for removal. A reflection test pins all original 104 external
delegate signatures and their removal annotations. Six declared story bridges
keep the unowned debug menu's method references bound to `RoomSession`; they
can be removed with the compatibility layer after the app migration.

The parameter audit uses Java syntax trees, including explicit constructors.
Its final counts supersede the provisional step-4 commit message: immediately
before parameter migration there were 33 long declarations, following helper
extraction; the round-2 integration baseline had 29. The four final exceptions
and the approved nested records are listed above.
