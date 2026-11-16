# Remaining null comparisons

This audit counts Java AST null comparisons (including multiple comparisons on one source line); the matching-line count is also reported for comparison with earlier reports. Baseline is merge `34266f0a`. No check is removed merely to lower the count.

A3 uses non-null screen/death states and eager debug wiring. A4 eagerly creates entity-selection caches and frame scratch, uses no-op draw observers, and normalizes transition/scroll completion actions at their public boundary. Public nullable helper arguments retain their existing behavior; they are validation/normalization boundaries, not optional stored collaborators.

| Area | Matching lines before / after | Comparisons before / after | Retained domain / boundary |
|---|---:|---:|---:|
| app | 88 / 62 | 91 / 63 | 38 / 25 |
| gameplay | 20 / 20 | 22 / 21 | 8 / 13 |
| render | 80 / 73 | 88 / 81 | 69 / 12 |
| world | 236 / 235 | 248 / 247 | 185 / 62 |

## Per-class classification

`boundary` includes the named lifecycle owners above. There are no optional stored collaborator checks outside those owners/configuration boundaries; `domain` includes cache occupancy, not collaborator availability. Native resources and the startup attach guard are intentionally confined to their owners. Opening sequence presence is still confined to OpeningStory and its state holder; active motions are not optional dependencies of unrelated services.

| Class | Before / after comparisons | Domain / boundary |
|---|---:|---:|
| `app/GameSession.java` | 15 / 7 | 0 / 7 |
| `app/LaunchOptions.java` | 2 / 2 | 0 / 2 |
| `app/audio/MusicDirector.java` | 5 / 0 | 0 / 0 |
| `app/debug/DebugTools.java` | 3 / 0 | 0 / 0 |
| `app/flow/SaveCoordinator.java` | 2 / 2 | 2 / 0 |
| `app/flow/ScreenRouter.java` | 3 / 3 | 3 / 0 |
| `app/flow/SessionNavigator.java` | 1 / 1 | 0 / 1 |
| `app/gameplay/ActiveLinkPhase.java` | 4 / 4 | 4 / 0 |
| `app/gameplay/CombatPhase.java` | 2 / 2 | 2 / 0 |
| `app/gameplay/FrameInputCache.java` | 4 / 2 | 2 / 0 |
| `app/gameplay/GameplaySimulation.java` | 1 / 1 | 1 / 0 |
| `app/gameplay/LinkEquipmentPhase.java` | 1 / 1 | 1 / 0 |
| `app/gameplay/entity/EntityInputCache.java` | 11 / 11 | 11 / 0 |
| `app/gameplay/entity/LinkWriteApplier.java` | 1 / 1 | 1 / 0 |
| `app/gameplay/interaction/BushInteraction.java` | 1 / 1 | 1 / 0 |
| `app/gameplay/interaction/DialogInteractions.java` | 1 / 1 | 1 / 0 |
| `app/gameplay/recovery/GameOverFlow.java` | 1 / 1 | 1 / 0 |
| `app/gameplay/recovery/PassOutController.java` | 4 / 0 | 0 / 0 |
| `app/headless/ReplayCheckpoint.java` | 3 / 3 | 3 / 0 |
| `app/headless/SessionDiagnosticSnapshot.java` | 1 / 1 | 1 / 0 |
| `app/host/DesktopHost.java` | 3 / 3 | 0 / 3 |
| `app/host/HostResources.java` | 2 / 2 | 2 / 0 |
| `app/input/InputRouter.java` | 3 / 3 | 0 / 3 |
| `app/input/SessionInput.java` | 1 / 0 | 0 / 0 |
| `app/presentation/RenderStateAssembler.java` | 1 / 0 | 0 / 0 |
| `app/screen/FileMenuScreen.java` | 2 / 0 | 0 / 0 |
| `app/screen/FileSaveScreen.java` | 2 / 0 | 0 / 0 |
| `app/screen/GameplayScreen.java` | 2 / 2 | 2 / 0 |
| `app/story/OpeningStory.java` | 7 / 7 | 0 / 7 |
| `app/wiring/RomWiring.java` | 1 / 1 | 0 / 1 |
| `app/wiring/SessionWiring.java` | 1 / 1 | 0 / 1 |
| `gameplay/DialogSoundEffectMap.java` | 1 / 1 | 1 / 0 |
| `gameplay/DialogSoundRouter.java` | 2 / 2 | 0 / 2 |
| `gameplay/DialogTextLoader.java` | 2 / 2 | 0 / 2 |
| `gameplay/EnemyCombatEventConsumer.java` | 4 / 3 | 3 / 0 |
| `gameplay/EnemyProjectileEventConsumer.java` | 3 / 3 | 1 / 2 |
| `gameplay/GameplayDialogInput.java` | 3 / 3 | 0 / 3 |
| `gameplay/GameplaySoundEffectMap.java` | 1 / 1 | 1 / 0 |
| `gameplay/OverworldDialogInteraction.java` | 3 / 3 | 0 / 3 |
| `gameplay/SfxDialogSoundSink.java` | 1 / 1 | 1 / 0 |
| `gameplay/SfxGameplaySoundSink.java` | 1 / 1 | 1 / 0 |
| `gameplay/SignpostDialogTable.java` | 1 / 1 | 0 / 1 |
| `render/BackgroundLayers.java` | 9 / 9 | 9 / 0 |
| `render/DialogRenderLayer.java` | 2 / 2 | 2 / 0 |
| `render/EntityRenderLayer.java` | 21 / 19 | 12 / 7 |
| `render/EntitySelectionSprites.java` | 2 / 2 | 2 / 0 |
| `render/EntitySpritePainter.java` | 2 / 2 | 2 / 0 |
| `render/FrameScene.java` | 4 / 2 | 1 / 1 |
| `render/GameFrameState.java` | 1 / 1 | 0 / 1 |
| `render/HardwareBackgroundRenderer.java` | 4 / 4 | 4 / 0 |
| `render/HardwareFrame.java` | 8 / 8 | 8 / 0 |
| `render/HardwareRenderLayer.java` | 3 / 3 | 0 / 3 |
| `render/IndexedRenderer.java` | 3 / 3 | 3 / 0 |
| `render/OpenGlFramePresenter.java` | 2 / 2 | 2 / 0 |
| `render/PaletteLayer.java` | 1 / 0 | 0 / 0 |
| `render/RgbaTileRasterizer.java` | 2 / 2 | 2 / 0 |
| `render/RoomInputs.java` | 14 / 14 | 14 / 0 |
| `render/RoomRenderLayer.java` | 3 / 3 | 3 / 0 |
| `render/TransientVfxRenderLayer.java` | 2 / 0 | 0 / 0 |
| `render/WarpLayers.java` | 4 / 4 | 4 / 0 |
| `render/WorldLayers.java` | 1 / 1 | 1 / 0 |
| `world/entity/EnemyProjectileCollision.java` | 3 / 3 | 3 / 0 |
| `world/entity/EntityBackgroundCollisionResolution.java` | 1 / 1 | 1 / 0 |
| `world/entity/EntityBackgroundMechanics.java` | 3 / 3 | 2 / 1 |
| `world/entity/EntityBossRewards.java` | 1 / 1 | 0 / 1 |
| `world/entity/EntityChestContents.java` | 1 / 1 | 0 / 1 |
| `world/entity/EntityCombatResolver.java` | 1 / 1 | 0 / 1 |
| `world/entity/EntityFrameContextBuilder.java` | 1 / 1 | 0 / 1 |
| `world/entity/EntityFrameRandom.java` | 1 / 1 | 0 / 1 |
| `world/entity/EntityRewardController.java` | 1 / 1 | 1 / 0 |
| `world/entity/EntityRoomLoader.java` | 4 / 4 | 2 / 2 |
| `world/entity/EntityRuntimeConfiguration.java` | 1 / 1 | 1 / 0 |
| `world/entity/EntitySlotPipeline.java` | 4 / 4 | 4 / 0 |
| `world/entity/EntitySpriteViews.java` | 1 / 1 | 0 / 1 |
| `world/entity/FollowingNpcEntitySpawner.java` | 9 / 9 | 5 / 4 |
| `world/entity/GenieEntityController.java` | 4 / 4 | 4 / 0 |
| `world/entity/RomRandomByteSource.java` | 1 / 1 | 0 / 1 |
| `world/entity/RoomEntity.java` | 2 / 2 | 1 / 1 |
| `world/entity/RoomEntityRuntimeBuilder.java` | 8 / 8 | 0 / 8 |
| `world/entity/RoomEntitySnapshot.java` | 6 / 6 | 6 / 0 |
| `world/entity/SlimeEyeController.java` | 1 / 1 | 1 / 0 |
| `world/entity/event/DungeonWarpRequest.java` | 1 / 1 | 0 / 1 |
| `world/entity/event/EntityCombatEvent.java` | 3 / 3 | 2 / 1 |
| `world/entity/event/EntityProjectileEvent.java` | 2 / 2 | 0 / 2 |
| `world/entity/event/MagicPowderObjectRequest.java` | 1 / 1 | 0 / 1 |
| `world/entity/handler/ArmosKnightHandler.java` | 2 / 2 | 2 / 0 |
| `world/entity/handler/BomberHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/CuccoHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/GiantGopongaFlowerHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/HidingSlimeKeyHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/HinoxHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/HookshotChainHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/LikeLikeHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/MadBomberHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/MoblinKingHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/PokeyHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/RoamingEnemyTypeHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/RollingBonesHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/StalfosEvasiveHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/WingedOctorokHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/WizrobeHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/ZolGelTypeHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/ZombieHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/handler/ZoraHandler.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/BlooperMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/BooBuddyMotion.java` | 2 / 2 | 1 / 1 |
| `world/entity/motion/BoomerangMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/BushCrawlerMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/BuzzBlobMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/ColorShellMotion.java` | 4 / 4 | 2 / 2 |
| `world/entity/motion/CrowMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/CuccoMotion.java` | 3 / 3 | 2 / 1 |
| `world/entity/motion/DogMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/FishMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/FollowingNpcMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/HinoxMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/HookshotBridgeMotion.java` | 5 / 5 | 5 / 0 |
| `world/entity/motion/HookshotChainMotion.java` | 6 / 6 | 6 / 0 |
| `world/entity/motion/LaserMotion.java` | 2 / 2 | 2 / 0 |
| `world/entity/motion/MadBomberMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/MiniMoldormMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/MoldormMotion.java` | 2 / 2 | 1 / 1 |
| `world/entity/motion/PiranhaMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/PolsVoiceMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/RoamingEnemyMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/SandCrabMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/SecretSeashellMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/SnakeMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/SpikedBeetleMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/TarinRaccoonMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/UrchinMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/WarpMotion.java` | 2 / 2 | 1 / 1 |
| `world/entity/motion/WingedOctorokMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/WitchRatMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/WizrobeMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/WizrobeProjectileMotion.java` | 1 / 1 | 0 / 1 |
| `world/entity/motion/ZombieMotion.java` | 1 / 1 | 1 / 0 |
| `world/entity/motion/ZoraMotion.java` | 1 / 1 | 1 / 0 |
| `world/object/OverworldBushInteraction.java` | 11 / 11 | 11 / 0 |
| `world/room/ActiveRoom.java` | 4 / 4 | 4 / 0 |
| `world/room/DroppableRupeeSystem.java` | 11 / 11 | 7 / 4 |
| `world/room/IndoorTorchPaletteEffect.java` | 2 / 2 | 2 / 0 |
| `world/room/LoadedRoom.java` | 6 / 6 | 6 / 0 |
| `world/room/ManboWarpResolver.java` | 1 / 1 | 0 / 1 |
| `world/room/OwlEventDialogResolver.java` | 1 / 1 | 0 / 1 |
| `world/room/OwlStatueDialogResolver.java` | 1 / 1 | 0 / 1 |
| `world/room/RoomCollisionQueries.java` | 4 / 3 | 3 / 0 |
| `world/room/RoomEntityBridge.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomEntityCombatBridge.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomEntitySessionState.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomEntitySimulation.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomInteractionGeometry.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomLiftInteractions.java` | 4 / 4 | 4 / 0 |
| `world/room/RoomLoadCoordinator.java` | 4 / 4 | 4 / 0 |
| `world/room/RoomLoader.java` | 3 / 3 | 3 / 0 |
| `world/room/RoomNpcPresentation.java` | 2 / 2 | 2 / 0 |
| `world/room/RoomObjectMutations.java` | 13 / 13 | 13 / 0 |
| `world/room/RoomObjectParser.java` | 3 / 3 | 3 / 0 |
| `world/room/RoomPaletteLoader.java` | 3 / 3 | 3 / 0 |
| `world/room/RoomPuzzleController.java` | 2 / 2 | 2 / 0 |
| `world/room/RoomRenderLiveView.java` | 2 / 2 | 2 / 0 |
| `world/room/RoomRenderSnapshot.java` | 3 / 3 | 3 / 0 |
| `world/room/RoomSession.java` | 3 / 3 | 0 / 3 |
| `world/room/RoomSessionContext.java` | 2 / 2 | 0 / 2 |
| `world/room/RoomSessionViews.java` | 1 / 1 | 0 / 1 |
| `world/room/RoomShovelActions.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomStoryState.java` | 1 / 1 | 1 / 0 |
| `world/room/RoomTilemap.java` | 4 / 4 | 4 / 0 |
| `world/room/RoomTilemapBuilder.java` | 4 / 4 | 4 / 0 |
| `world/room/RoomTransitionCoordinator.java` | 6 / 6 | 6 / 0 |
| `world/room/RoomWorldState.java` | 1 / 1 | 1 / 0 |
| `world/transition/ManboTransitionWave.java` | 1 / 1 | 0 / 1 |
| `world/transition/RoomBoundaryController.java` | 1 / 1 | 1 / 0 |
| `world/transition/ScrollController.java` | 1 / 1 | 0 / 1 |
| `world/transition/TransitionController.java` | 4 / 4 | 1 / 3 |

## Genuine optional values

Sparse tiles, palettes, graphics selection and absent scroll history; absent collision contacts and probe history; unoccupied entity/effect/drop slots; optional spawn/reward/action requests; saved spawn/warp lookups; nullable sound-map results; cache occupancy; first resource-close failure; configured recording options; ROM-free Manbo wave data; optional public Link/config/dialog targets in compatibility helpers. Room-loaded availability remains checked solely by RoomSession.

## Site ledger

| Class:line | Expression | Classification | Reason |
|---|---|---|---|
| `app/GameSession.java:72` | `context != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/GameSession.java:79` | `recorder != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/GameSession.java:161` | `options.recording() != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/GameSession.java:214` | `context != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/GameSession.java:234` | `context != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/GameSession.java:241` | `context != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/GameSession.java:248` | `context != null` | boundary | Session publication/close and the single recording lifecycle owner |
| `app/LaunchOptions.java:49` | `output == null` | boundary | Public input/resource lookup or wiring selection |
| `app/LaunchOptions.java:55` | `output == null` | boundary | Public input/resource lookup or wiring selection |
| `app/flow/SaveCoordinator.java:192` | `spawn == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/flow/SaveCoordinator.java:273` | `spawn != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/flow/ScreenRouter.java:75` | `spec == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/flow/ScreenRouter.java:115` | `spec == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/flow/ScreenRouter.java:130` | `spec != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/flow/SessionNavigator.java:30` | `targets != null` | boundary | One-time attach guard for the explicit wiring cycle |
| `app/gameplay/ActiveLinkPhase.java:177` | `splash != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/ActiveLinkPhase.java:227` | `pegasusShake != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/ActiveLinkPhase.java:258` | `sword == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/ActiveLinkPhase.java:268` | `pickup != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/CombatPhase.java:23` | `sword == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/CombatPhase.java:36` | `sword != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/FrameInputCache.java:26` | `attack == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/FrameInputCache.java:43` | `sword == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/GameplaySimulation.java:290` | `dialog != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/LinkEquipmentPhase.java:70` | `resourceSound != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:32` | `linkPosition == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:44` | `linkMotion == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:65` | `linkInputs == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:85` | `progressInputs == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:99` | `inventoryInputs == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:110` | `playerLevels == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:120` | `playerHealth == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:134` | `playerInputs == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:151` | `uiInputs == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:166` | `buttonInputs == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/EntityInputCache.java:179` | `swordBox == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/entity/LinkWriteApplier.java:203` | `sword != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/interaction/BushInteraction.java:42` | `sword == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/interaction/DialogInteractions.java:81` | `request == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/gameplay/recovery/GameOverFlow.java:36` | `sword != null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/headless/ReplayCheckpoint.java:70` | `tarin == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/headless/ReplayCheckpoint.java:71` | `tarin == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/headless/ReplayCheckpoint.java:81` | `snapshot.equipmentController().activeSword() == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/headless/SessionDiagnosticSnapshot.java:167` | `sword == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/host/DesktopHost.java:69` | `resources != null` | boundary | Native host resource acquisition and release |
| `app/host/DesktopHost.java:196` | `framePresenter != null` | boundary | Native host resource acquisition and release |
| `app/host/DesktopHost.java:207` | `owned != null` | boundary | Native host resource acquisition and release |
| `app/host/HostResources.java:37` | `first == null` | domain | First failure retained while closing all native resources |
| `app/host/HostResources.java:41` | `first != null` | domain | First failure retained while closing all native resources |
| `app/input/InputRouter.java:147` | `manager != null` | boundary | Public input/resource lookup or wiring selection |
| `app/input/InputRouter.java:261` | `inputConfig == null` | boundary | Public input/resource lookup or wiring selection |
| `app/input/InputRouter.java:261` | `inputState == null` | boundary | Public input/resource lookup or wiring selection |
| `app/screen/GameplayScreen.java:36` | `reusableKeys == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/screen/GameplayScreen.java:72` | `snapshot == null` | domain | Cached immutable value, selected equipment, pending request, saved spawn or rendered frame |
| `app/story/OpeningStory.java:49` | `state.opening().wakeUp() == null` | boundary | Opening-story sequence availability |
| `app/story/OpeningStory.java:78` | `state.opening().wakeUp() != null` | boundary | Opening-story sequence availability |
| `app/story/OpeningStory.java:90` | `state.opening().wakeUp() == null` | boundary | Opening-story sequence availability |
| `app/story/OpeningStory.java:121` | `state.opening().wakeUp() != null` | boundary | Opening-story sequence availability |
| `app/story/OpeningStory.java:122` | `state.opening().shield() == null` | boundary | Opening-story sequence availability |
| `app/story/OpeningStory.java:168` | `state.opening().wakeUp() == null` | boundary | Opening-story sequence availability |
| `app/story/OpeningStory.java:210` | `state.opening().shield() == null` | boundary | Opening-story sequence availability |
| `app/wiring/RomWiring.java:23` | `is == null` | boundary | Public input/resource lookup or wiring selection |
| `app/wiring/SessionWiring.java:67` | `headlessPcm == null` | boundary | Public input/resource lookup or wiring selection |
| `gameplay/DialogSoundEffectMap.java:30` | `event == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/DialogSoundRouter.java:11` | `dialogController == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/DialogSoundRouter.java:11` | `soundSink == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/DialogTextLoader.java:27` | `romData == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/DialogTextLoader.java:41` | `dialogRef == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/EnemyCombatEventConsumer.java:55` | `event == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/EnemyCombatEventConsumer.java:60` | `event.swordPokeVfx() != null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/EnemyCombatEventConsumer.java:70` | `channel == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/EnemyProjectileEventConsumer.java:78` | `event == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/EnemyProjectileEventConsumer.java:116` | `link != null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/EnemyProjectileEventConsumer.java:135` | `link != null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/GameplayDialogInput.java:20` | `inputConfig != null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/GameplayDialogInput.java:33` | `dialogController != null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/GameplayDialogInput.java:44` | `inputConfig != null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/GameplaySoundEffectMap.java:28` | `event == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/OverworldDialogInteraction.java:41` | `blockers != null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/OverworldDialogInteraction.java:44` | `roomObjectsArea == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/OverworldDialogInteraction.java:45` | `dialogController == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `gameplay/SfxDialogSoundSink.java:42` | `effect == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/SfxGameplaySoundSink.java:50` | `effect == null` | domain | Optional event, sound-map entry or VFX payload |
| `gameplay/SignpostDialogTable.java:27` | `romData == null` | boundary | Legacy nullable public input; omitted target/configuration is explicitly supported |
| `render/BackgroundLayers.java:72` | `uploadBytes != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:74` | `obj != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:101` | `source == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:103` | `introX == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:112` | `revealTiles == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:138` | `lineY == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:139` | `lineY != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:142` | `lineX != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/BackgroundLayers.java:150` | `sprites != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/DialogRenderLayer.java:10` | `painter == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/DialogRenderLayer.java:43` | `objectPalettes != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:54` | `snapshot == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:54` | `objectPalettes == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:54` | `scrollController == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:61` | `palette == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:86` | `context == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:86` | `context.buffer() == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:86` | `context.gpu() == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/EntityRenderLayer.java:91` | `previousRoom != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:92` | `previousRoom.entities() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:93` | `previousRoom.entities().spriteSelection() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:133` | `death != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:203` | `entities.spriteSelection() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:205` | `overlay != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:222` | `entities.spriteSelection() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:224` | `burning != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:241` | `entities.spriteSelection() == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:245` | `death != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:252` | `entities.spriteSelection() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntityRenderLayer.java:254` | `override != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntitySelectionSprites.java:16` | `next == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntitySelectionSprites.java:17` | `next == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntitySpritePainter.java:258` | `variant.second() == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/EntitySpritePainter.java:361` | `tiles == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/FrameScene.java:60` | `suppliedFrame != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/FrameScene.java:84` | `observer == null` | boundary | Nullable inspection observer is converted to a no-op on attachment |
| `render/GameFrameState.java:371` | `value != null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/HardwareBackgroundRenderer.java:46` | `lineOffsets == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareBackgroundRenderer.java:150` | `lineScrollY == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareBackgroundRenderer.java:191` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareBackgroundRenderer.java:233` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:39` | `indexedContext == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:47` | `backgroundScratch == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:57` | `observer == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:103` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:130` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:156` | `backgroundPalettes[pixel] == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:297` | `backgroundPalettes[pixel] == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareFrame.java:311` | `entry.tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/HardwareRenderLayer.java:9` | `context == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/HardwareRenderLayer.java:9` | `context.buffer() == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/HardwareRenderLayer.java:9` | `context.gpu() == null` | boundary | Required context validation or conversion into explicit frame-section presence |
| `render/IndexedRenderer.java:57` | `lineOffsets == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/IndexedRenderer.java:161` | `lineScrollY == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/IndexedRenderer.java:189` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/OpenGlFramePresenter.java:368` | `first == null` | domain | First failure retained while closing all native resources |
| `render/OpenGlFramePresenter.java:372` | `first != null` | domain | First failure retained while closing all native resources |
| `render/RgbaTileRasterizer.java:29` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RgbaTileRasterizer.java:71` | `tile == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:30` | `room == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:50` | `liveTiles == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:51` | `liveAttributes == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:53` | `livePalettes == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:58` | `livePalettes[p] == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:81` | `tiles != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:82` | `entities != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:82` | `entities.spriteSelection() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:83` | `entities.spriteTiles() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:84` | `warp != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:85` | `heart != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:97` | `entities == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:98` | `next == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomInputs.java:110` | `tiles == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomRenderLayer.java:47` | `currentSnapshot != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomRenderLayer.java:59` | `waveOffsets == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/RoomRenderLayer.java:77` | `previousRoom != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/WarpLayers.java:35` | `sourceObj == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/WarpLayers.java:48` | `state.roomInputs.entities != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/WarpLayers.java:48` | `state.roomInputs.entities.spriteSelection() != null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/WarpLayers.java:86` | `state.roomInputs.entities.spriteTiles() == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `render/WorldLayers.java:129` | `data.palettes == null` | domain | Sparse tile/palette/sprite data, optional scroll history or retained decoded/render scratch cache |
| `world/entity/EnemyProjectileCollision.java:53` | `projectile == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EnemyProjectileCollision.java:53` | `link == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EnemyProjectileCollision.java:136` | `projectile == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityBackgroundCollisionResolution.java:6` | `result == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityBackgroundMechanics.java:109` | `bridgeState == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityBackgroundMechanics.java:352` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityBackgroundMechanics.java:352` | `policy == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityBossRewards.java:33` | `state.readEnemyDropLowHealth() == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityChestContents.java:17` | `table == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityCombatResolver.java:159` | `attackContext == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityFrameContextBuilder.java:171` | `handlerLinkCollisionEnabled != null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityFrameRandom.java:22` | `configured == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityRewardController.java:392` | `pickup != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityRoomLoader.java:56` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityRoomLoader.java:93` | `table == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/EntityRoomLoader.java:177` | `roomStatusTable == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityRoomLoader.java:195` | `roomStatusTable == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntityRuntimeConfiguration.java:314` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntitySlotPipeline.java:270` | `state.inputs().linkPositionHistory() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntitySlotPipeline.java:920` | `bowWowUpdate.targetContact() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntitySlotPipeline.java:1030` | `slotState.preAppliedGroundResult() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntitySlotPipeline.java:1046` | `groundResult.pitTransition() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/EntitySpriteViews.java:32` | `catalog == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/FollowingNpcEntitySpawner.java:34` | `spriteHandlers == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/FollowingNpcEntitySpawner.java:88` | `selection == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/FollowingNpcEntitySpawner.java:113` | `snapshot == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/FollowingNpcEntitySpawner.java:113` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/FollowingNpcEntitySpawner.java:183` | `initial == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/FollowingNpcEntitySpawner.java:183` | `room == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/FollowingNpcEntitySpawner.java:183` | `requestedState == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/FollowingNpcEntitySpawner.java:183` | `history == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/FollowingNpcEntitySpawner.java:193` | `initial.spriteSelection() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/GenieEntityController.java:965` | `update.bodySpawn() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/GenieEntityController.java:978` | `update.bodySpawn() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/GenieEntityController.java:1013` | `update.bodySpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/GenieEntityController.java:1021` | `update.rockSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RomRandomByteSource.java:27` | `observer == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntity.java:43` | `status == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntity.java:56` | `spriteDefinition == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RoomEntityRuntimeBuilder.java:121` | `initial == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:122` | `enemyCombatTables == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:123` | `chestContentsTable == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:124` | `romTables == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:125` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:129` | `initial == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:130` | `enemyCombatTables == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntityRuntimeBuilder.java:132` | `chestContentsTable == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/RoomEntitySnapshot.java:31` | `other == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RoomEntitySnapshot.java:183` | `slots == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RoomEntitySnapshot.java:187` | `hookshotChainOam == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RoomEntitySnapshot.java:190` | `pincerBodyOam == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RoomEntitySnapshot.java:193` | `wingedOctorokOam == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/RoomEntitySnapshot.java:196` | `visualYOffsetBySlot == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/SlimeEyeController.java:65` | `rom == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/event/DungeonWarpRequest.java:9` | `destination == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/event/EntityCombatEvent.java:109` | `linkAction == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/event/EntityCombatEvent.java:112` | `linkCollisionResponse == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/event/EntityCombatEvent.java:160` | `channel == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/event/EntityProjectileEvent.java:48` | `kind == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/event/EntityProjectileEvent.java:57` | `soundChannel == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/event/MagicPowderObjectRequest.java:16` | `action == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/handler/ArmosKnightHandler.java:75` | `armosKnightUpdate.screenShakeRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/ArmosKnightHandler.java:79` | `armosKnightUpdate.rubbleRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/BomberHandler.java:40` | `bomberUpdate.bombSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/CuccoHandler.java:82` | `cuccoUpdate.spawnRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/GiantGopongaFlowerHandler.java:45` | `giantUpdate.projectileSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/HidingSlimeKeyHandler.java:61` | `keyUpdate.reward() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/HinoxHandler.java:138` | `hinoxUpdate.bombSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/HookshotChainHandler.java:87` | `hookshotState == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/LikeLikeHandler.java:47` | `likeLikeUpdate.event() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/MadBomberHandler.java:44` | `madBomberUpdate.bombSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/MoblinKingHandler.java:164` | `kingUpdate.arrowRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/PokeyHandler.java:80` | `pokeyUpdate.segmentSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/RoamingEnemyTypeHandler.java:66` | `roamingUpdate.launchRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/RollingBonesHandler.java:41` | `bar != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/StalfosEvasiveHandler.java:48` | `evasiveUpdate.cloneRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/WingedOctorokHandler.java:54` | `wingedUpdate.rockSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/WizrobeHandler.java:64` | `wizrobeUpdate.projectileSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/ZolGelTypeHandler.java:64` | `zolGelUpdate.split() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/ZombieHandler.java:66` | `zombieUpdate.spawnRequest() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/handler/ZoraHandler.java:55` | `zoraUpdate.projectileSpawn() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/BlooperMotion.java:86` | `groundResult == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/BooBuddyMotion.java:316` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/BooBuddyMotion.java:319` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/BoomerangMotion.java:180` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/BushCrawlerMotion.java:240` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/BuzzBlobMotion.java:206` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/ColorShellMotion.java:339` | `other == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/ColorShellMotion.java:503` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/ColorShellMotion.java:503` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/ColorShellMotion.java:516` | `allEntities == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/CrowMotion.java:215` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/CuccoMotion.java:472` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/CuccoMotion.java:475` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/CuccoMotion.java:590` | `spawnRequest != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/DogMotion.java:251` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/FishMotion.java:137` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/FollowingNpcMotion.java:99` | `linkPositionHistory == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HinoxMotion.java:524` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/HookshotBridgeMotion.java:30` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotBridgeMotion.java:54` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotBridgeMotion.java:67` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotBridgeMotion.java:85` | `states[slot] != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotBridgeMotion.java:91` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotChainMotion.java:61` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotChainMotion.java:182` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotChainMotion.java:261` | `states[slot] != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotChainMotion.java:312` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotChainMotion.java:319` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/HookshotChainMotion.java:369` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/LaserMotion.java:125` | `parent == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/LaserMotion.java:128` | `parent == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/MadBomberMotion.java:99` | `bombSpawn != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/MiniMoldormMotion.java:259` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/MoldormMotion.java:148` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/MoldormMotion.java:228` | `position == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/PiranhaMotion.java:34` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/PolsVoiceMotion.java:50` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/RoamingEnemyMotion.java:367` | `launchRequest != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/SandCrabMotion.java:141` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/SecretSeashellMotion.java:156` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/SnakeMotion.java:218` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/SpikedBeetleMotion.java:356` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/TarinRaccoonMotion.java:260` | `source == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/UrchinMotion.java:217` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/WarpMotion.java:37` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/WarpMotion.java:57` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/WingedOctorokMotion.java:381` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/WitchRatMotion.java:142` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/WizrobeMotion.java:154` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/WizrobeProjectileMotion.java:22` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/entity/motion/ZombieMotion.java:227` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/entity/motion/ZoraMotion.java:199` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:204` | `roomObjectsArea == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:210` | `rule == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:216` | `mutableRenderValues != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:240` | `roomObjectsArea == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:246` | `rule == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:251` | `mutableRenderValues != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:272` | `roomObjectsArea == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:272` | `roomTileIds == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:272` | `roomTileAttrs == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:289` | `mutableRenderValues != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/object/OverworldBushInteraction.java:294` | `fallbackOverlay != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/ActiveRoom.java:130` | `entities == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/ActiveRoom.java:134` | `previous != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/ActiveRoom.java:155` | `tileIds == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/ActiveRoom.java:157` | `tileAttrs == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:91` | `randomByteSupplier == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/DroppableRupeeSystem.java:110` | `slots[i] == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:119` | `playerState == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/DroppableRupeeSystem.java:125` | `slot == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:156` | `displayBuffer == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/DroppableRupeeSystem.java:156` | `gpu == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/DroppableRupeeSystem.java:163` | `slot == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:181` | `slot != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:194` | `slots[index] != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:217` | `slot != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/DroppableRupeeSystem.java:264` | `tile == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/IndoorTorchPaletteEffect.java:173` | `source == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/IndoorTorchPaletteEffect.java:178` | `source[index] == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/LoadedRoom.java:20` | `objects.gbcOverlay() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/LoadedRoom.java:21` | `objects.renderValues() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/LoadedRoom.java:64` | `objects.gbcOverlay() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/LoadedRoom.java:68` | `objects.renderValues() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/LoadedRoom.java:136` | `source == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/LoadedRoom.java:141` | `source[i] == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/ManboWarpResolver.java:18` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/OwlEventDialogResolver.java:21` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/OwlStatueDialogResolver.java:13` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/RoomCollisionQueries.java:67` | `previous == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomCollisionQueries.java:83` | `previous == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomCollisionQueries.java:210` | `previous == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomEntityBridge.java:76` | `step.tileCopy() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomEntityCombatBridge.java:49` | `event == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomEntitySessionState.java:40` | `enemyDropLowHealth != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomEntitySimulation.java:108` | `dependencies.items().pendingShovelDrop != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomInteractionGeometry.java:83` | `values == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLiftInteractions.java:181` | `target.statue() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLiftInteractions.java:190` | `target.statue() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLiftInteractions.java:196` | `target.statue() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLiftInteractions.java:204` | `target.statue() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoadCoordinator.java:278` | `objectPalettes != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoadCoordinator.java:278` | `entities.spriteSelection() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoadCoordinator.java:281` | `entities.spriteSelection() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoadCoordinator.java:381` | `selection == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoader.java:56` | `overworldRoomStatus == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoader.java:154` | `indoorRoomStatus == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomLoader.java:158` | `indoorRoomStatus != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomNpcPresentation.java:35` | `loaded.room.entityView().spriteSelection() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomNpcPresentation.java:40` | `overrides != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:192` | `bomb != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:199` | `bomb != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:210` | `candidate == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:306` | `contents.room.renderValues() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:385` | `contents.room.renderValues() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:446` | `target == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:450` | `target[palette] == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:460` | `candidate == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:527` | `contents.room.renderValues() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:549` | `candidate == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:606` | `candidate == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:633` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectMutations.java:642` | `entity == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectParser.java:152` | `targetRoomObjectsArea == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectParser.java:169` | `targetRoomObjectsArea == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomObjectParser.java:193` | `targetRoomObjectsArea == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomPaletteLoader.java:109` | `previousBackground == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomPaletteLoader.java:110` | `previousObjects == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomPaletteLoader.java:134` | `source == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomPuzzleController.java:221` | `contents.room.renderValues() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomPuzzleController.java:235` | `contents.room.renderValues() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomRenderLiveView.java:122` | `entities == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomRenderLiveView.java:124` | `selection == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomRenderSnapshot.java:51` | `entities == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomRenderSnapshot.java:105` | `entities == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomRenderSnapshot.java:105` | `entities.spriteSelection() == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomSession.java:76` | `loadedRoom != null` | boundary | Single loaded-room lifecycle owner |
| `world/room/RoomSession.java:93` | `config.presentation().roomLoadListener() == null` | boundary | Single loaded-room lifecycle owner |
| `world/room/RoomSession.java:608` | `first != null` | boundary | Single loaded-room lifecycle owner |
| `world/room/RoomSessionContext.java:35` | `config.presentation().transientVfxSystem() != null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/RoomSessionContext.java:39` | `config.presentation().droppableRupeeSystem() == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/RoomSessionViews.java:62` | `soundSink == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/room/RoomShovelActions.java:125` | `loaded.room.renderValues() != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomStoryState.java:106` | `state == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemap.java:8` | `gbcOverlay == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemap.java:9` | `renderValues == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemap.java:24` | `gbcOverlay == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemap.java:29` | `renderValues == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemapBuilder.java:191` | `gbcOverlay == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemapBuilder.java:202` | `roomObjectsArea != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemapBuilder.java:221` | `renderValues != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTilemapBuilder.java:227` | `gbcOverlay != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTransitionCoordinator.java:104` | `staircaseWarp != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTransitionCoordinator.java:161` | `link == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTransitionCoordinator.java:165` | `target == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTransitionCoordinator.java:174` | `link == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTransitionCoordinator.java:178` | `target == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomTransitionCoordinator.java:226` | `dungeonWarp != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/room/RoomWorldState.java:124` | `values == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/transition/ManboTransitionWave.java:13` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/transition/RoomBoundaryController.java:105` | `sideViewDecision != null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
| `world/transition/ScrollController.java:40` | `completion == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/transition/TransitionController.java:68` | `romData == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/transition/TransitionController.java:81` | `loadNewRoom == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/transition/TransitionController.java:91` | `onComplete == null` | boundary | Public argument validation or optional configuration converted once to a non-null/default policy |
| `world/transition/TransitionController.java:138` | `manboTransitionWave == null` | domain | Optional ROM action/target, sparse slot/graphics, collision history/cache or pending request |
