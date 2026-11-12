# ROM migration inventory

Snapshot before the coordinated external-call-site migration (2026-11-12).
All paths are relative to `java/src/main/java/`. Sorted by package and file.

Counts are lexical Java occurrences, not runtime reads. Parameters/fields count
`byte[] rom*` declarations; locals and return declarations are separate. Raw reads
count `rom*[expression]` occurrences (including qualified context fields), excluding
simple assignment targets. Bulk copies, typed `Rom` reads and helper calls are
not raw index reads. Zero-read rows still propagate ROM storage and need migration.
Non-ROM byte arrays (VRAM, saves, framebuffers, tiles) are excluded. No ROM data
or extracted assets are recorded here.

Totals: **68 files**, **148 parameters/fields**, **3 locals**, **5 return declarations**, **33 raw index reads**.

| Package | File | Parameters/fields | Locals | Returns | Raw index reads |
| --- | --- | ---: | ---: | ---: | ---: |
| linksawakening.app | `SaveCoordinator.java` | 0 | 0 | 1 | 0 |
| linksawakening.app | `ScreenRouter.java` | 0 | 0 | 1 | 0 |
| linksawakening.app | `SessionBootstrap.java` | 2 | 0 | 1 | 0 |
| linksawakening.app | `SessionContext.java` | 0 | 0 | 1 | 0 |
| linksawakening.app.gameplay | `GameplaySimulation.java` | 0 | 0 | 1 | 0 |
| linksawakening.audio.browser | `AudioBrowserMain.java` | 0 | 1 | 0 | 0 |
| linksawakening.audio.music | `AreaMusicResolver.java` | 2 | 0 | 0 | 0 |
| linksawakening.audio.music | `FrameAudioPlayer.java` | 1 | 0 | 0 | 0 |
| linksawakening.audio.music | `MusicCatalog.java` | 6 | 0 | 0 | 0 |
| linksawakening.audio.music | `MusicDriver.java` | 2 | 0 | 0 | 3 |
| linksawakening.audio.sfx | `RomSoundEffectEngine.java` | 2 | 0 | 0 | 1 |
| linksawakening.audio.sfx | `RomSoundEffectTables.java` | 2 | 0 | 0 | 0 |
| linksawakening.audio.sfx | `SoundEffectCatalog.java` | 1 | 0 | 0 | 0 |
| linksawakening.audio.sfx | `SoundEffectPlayer.java` | 2 | 0 | 0 | 0 |
| linksawakening.cutscene | `CutsceneManager.java` | 1 | 0 | 0 | 0 |
| linksawakening.cutscene | `IntroRomData.java` | 2 | 0 | 0 | 0 |
| linksawakening.cutscene | `IntroSequence.java` | 2 | 1 | 0 | 0 |
| linksawakening.dialog | `DialogFont.java` | 2 | 0 | 0 | 0 |
| linksawakening.dialog | `DialogRomLayout.java` | 0 | 1 | 0 | 1 |
| linksawakening.entity | `EntitySpriteCatalog.java` | 2 | 0 | 0 | 0 |
| linksawakening.entity | `EntitySpriteHandlerCatalog.java` | 2 | 0 | 0 | 11 |
| linksawakening.entity | `LinkSpriteSheet.java` | 2 | 0 | 0 | 0 |
| linksawakening.entity | `LinkTunicPalette.java` | 1 | 0 | 0 | 0 |
| linksawakening.equipment | `SwordPalette.java` | 1 | 0 | 0 | 0 |
| linksawakening.equipment | `SwordSpriteSheet.java` | 2 | 0 | 0 | 0 |
| linksawakening.gameplay | `DialogTextLoader.java` | 4 | 0 | 0 | 0 |
| linksawakening.gameplay | `SfxGameplaySoundSink.java` | 2 | 0 | 0 | 0 |
| linksawakening.gameplay | `SignpostDialogTable.java` | 1 | 0 | 0 | 0 |
| linksawakening.gpu | `AnimatedTiles.java` | 4 | 0 | 0 | 0 |
| linksawakening.gpu | `ColorDungeonTileLoader.java` | 1 | 0 | 0 | 0 |
| linksawakening.gpu | `EntitySheetTileLoader.java` | 2 | 0 | 0 | 0 |
| linksawakening.gpu | `InventoryTileLoader.java` | 6 | 0 | 0 | 0 |
| linksawakening.gpu | `RomTileLoader.java` | 22 | 0 | 0 | 0 |
| linksawakening.gpu | `RoomTileLoader.java` | 7 | 0 | 0 | 0 |
| linksawakening.gpu | `TitleIntroTileLoader.java` | 4 | 0 | 0 | 0 |
| linksawakening.scene | `BackgroundSceneLoader.java` | 2 | 0 | 0 | 0 |
| linksawakening.ui | `FileMenuRomData.java` | 2 | 0 | 0 | 0 |
| linksawakening.ui | `InventoryDungeonPanel.java` | 2 | 0 | 0 | 0 |
| linksawakening.ui | `InventoryInstrumentPanel.java` | 1 | 0 | 0 | 0 |
| linksawakening.ui | `InventoryMenu.java` | 2 | 0 | 0 | 0 |
| linksawakening.ui | `InventoryTilemapLoader.java` | 1 | 0 | 0 | 0 |
| linksawakening.vfx | `TransientVfxSpriteSheet.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.entity | `EnemyDropResolver.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.entity | `EntityRoomInitialization.java` | 1 | 0 | 0 | 6 |
| linksawakening.world.entity | `EntityRoomLoader.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.entity | `RomEnemyCombatTables.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.entity | `RoomEntityRuntime.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.entity.motion | `WarpMotion.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.object | `OverworldBushInteraction.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `ChestContentsTable.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `DungeonRoomEventTable.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `IndoorTorchPaletteEffect.java` | 2 | 0 | 0 | 1 |
| linksawakening.world.room | `ManboWarpResolver.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `OverworldTilesetTable.java` | 1 | 0 | 0 | 0 |
| linksawakening.world.room | `OwlEventDialogResolver.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `OwlStatueDialogResolver.java` | 2 | 0 | 0 | 1 |
| linksawakening.world.room | `RoomDoorInteractions.java` | 0 | 0 | 0 | 4 |
| linksawakening.world.room | `RoomLoader.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `RoomObjectParser.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `RoomPaletteLoader.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `RoomPuzzleController.java` | 0 | 0 | 0 | 3 |
| linksawakening.world.room | `RoomScrollNavigation.java` | 2 | 0 | 0 | 2 |
| linksawakening.world.room | `RoomSession.java` | 3 | 0 | 0 | 0 |
| linksawakening.world.room | `RoomSessionContext.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `RoomTilemapBuilder.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.room | `RoomWorldState.java` | 2 | 0 | 0 | 0 |
| linksawakening.world.transition | `ManboTransitionWave.java` | 1 | 0 | 0 | 0 |
| linksawakening.world.transition | `TransitionController.java` | 2 | 0 | 0 | 0 |

## Deprecated compatibility members and external callers

Counts scan `java/src` outside the `rom` package, including tests. Qualified
static calls are counted by class and method name; overloads share a count.
Instance calls are counted by method name after a dot (constructor calls by
`new Rom`). Static imports add eight `RomBank.romOffset` calls in
`GameplayDialogIntegrationTest`: 243 qualified + 8 imported = 251 total.
Other static totals: RomBank.decodeRgb555 44; RomUtil.romOffset 4;
RomUtil.decodeRgb555 5.
External code is unchanged in this work, so before/after counts are identical.

| Member | External calls | Replacement |
| --- | ---: | --- |
| `Rom.Rom` | 85 | `Rom.of` |
| `Rom.slice` | 5 | `Rom.bytes` |
| `RomTables.loadFromRom` | 123 | `RomTables.load(Rom)` |
| `RomTables.objectPhysicsFlag` | 20 | `themed holder, same method name` |
| `RomTables.overworldPhysicsFlag` | 0 | `themed holder, same method name` |
| `RomTables.entityOptions1` | 7 | `themed holder, same method name` |
| `RomTables.entityHitboxFlags` | 0 | `themed holder, same method name` |
| `RomTables.entityCollisionBoxType` | 1 | `themed holder, same method name` |
| `RomTables.entityCollisionPointX` | 1 | `themed holder, same method name` |
| `RomTables.entityCollisionPointY` | 1 | `themed holder, same method name` |
| `RomTables.entityFineCollisionShape` | 1 | `themed holder, same method name` |
| `RomTables.linkFineCollisionShape` | 6 | `themed holder, same method name` |
| `RomTables.revolvingDoorAnimation` | 1 | `themed holder, same method name` |
| `RomTables.revolvingDoorPalette` | 2 | `themed holder, same method name` |
| `RomTables.pitWarpArrivalDirection` | 1 | `themed holder, same method name` |
| `RomTables.waterCurrentSpeedX` | 1 | `themed holder, same method name` |
| `RomTables.waterCurrentSpeedY` | 1 | `themed holder, same method name` |
| `RomTables.linkSpeedX` | 20 | `themed holder, same method name` |
| `RomTables.airSpeedX` | 2 | `themed holder, same method name` |
| `RomTables.airSpeedY` | 2 | `themed holder, same method name` |
| `RomTables.linkSpeedY` | 18 | `themed holder, same method name` |
| `RomTables.swimmingSpeedX` | 1 | `themed holder, same method name` |
| `RomTables.swimmingSpeedY` | 1 | `themed holder, same method name` |
| `RomTables.swimmingEntrySpeedX` | 1 | `themed holder, same method name` |
| `RomTables.swimmingEntrySpeedY` | 1 | `themed holder, same method name` |
| `RomTables.swordDirectionAnimState` | 4 | `themed holder, same method name` |
| `RomTables.swordDirectionFor` | 4 | `themed holder, same method name` |
| `RomTables.swordSpinAnimationState` | 3 | `themed holder, same method name` |
| `RomTables.swordSpinAbsoluteDirection` | 3 | `themed holder, same method name` |
| `RomTables.swordBladeXOffset` | 3 | `themed holder, same method name` |
| `RomTables.swordBladeYOffset` | 3 | `themed holder, same method name` |
| `RomTables.swordEnemyCollisionEnabled` | 1 | `themed holder, same method name` |
| `RomTables.swordCollisionXOriginOffset` | 2 | `themed holder, same method name` |
| `RomTables.swordCollisionWidth` | 1 | `themed holder, same method name` |
| `RomTables.swordCollisionYOriginOffset` | 2 | `themed holder, same method name` |
| `RomTables.swordCollisionHeight` | 1 | `themed holder, same method name` |
| `RomTables.swordSpriteTile` | 6 | `themed holder, same method name` |
| `RomTables.swordSpriteAttr` | 6 | `themed holder, same method name` |
| `RomTables.staticSwordCollisionOffsetX` | 1 | `themed holder, same method name` |
| `RomTables.staticSwordCollisionOffsetY` | 1 | `themed holder, same method name` |
| `RomTables.magicRodXOffset` | 2 | `themed holder, same method name` |
| `RomTables.magicRodYOffset` | 2 | `themed holder, same method name` |
| `RomTables.magicRodTile` | 4 | `themed holder, same method name` |
| `RomTables.magicRodAttribute` | 4 | `themed holder, same method name` |
| `RomTables.shovelAnimationState` | 7 | `themed holder, same method name` |
| `RomUtil.romOffset` | 4 | `RomBank.romOffset` |
| `RomUtil.decodeRgb555` | 5 | `RomBank.decodeRgb555` |
| `RomUtil.loadPalettesFromRom` | 2 | `RomUtil.loadPalettesFromRom(Rom, RomAddress, …)` |
| `RomUtil.decodeBackgroundFromRom` | 2 | `RomUtil.decodeBackgroundFromRom(Rom, RomAddress, …)` |
| `RomBank.loadPalettes` | 4 | `RomBank.loadPalettes(Rom, RomAddress, …)` |
| `RomBank.decodeBackground` | 0 | `RomBank.decodeBackground(Rom, RomAddress, …)` |
| `RomBank.decodeBackgroundMap` | 8 | `RomBank.decodeBackgroundMap(Rom, RomAddress, …)` |
| `RomBank.decodeBackgroundMap` | 8 | `RomBank.decodeBackgroundMap(Rom, RomAddress, …)` |
