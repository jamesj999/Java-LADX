package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomLoadCoordinator operations for the live room session. */
final class RoomLoadCoordinator {
    private final RoomSession session;
    RoomLoadCoordinator(RoomSession session) { this.session = session; }

    void loadInitialOverworld(int roomId) {
        session.gpu.loadBaseOverworldTiles(session.romData);
        session.loadRoomSpecificTilesIfNeeded(roomId);
        session.loadOverworld(roomId);
    }

    void loadOverworld(int roomId) {
        session.roomLoadSerial++;
        session.indoorMapPosition = -1;
        session.clearTransientRoomState();
        session.indoorTorchPaletteEffect.clear();
        session.recentRoomEntityClears.visit(roomId);
        LoadedRoom room = session.roomLoader.loadOverworld(
            roomId, session.recentRoomEntityClears.mask(roomId), session.roomState.overworldRoomStatus);
        session.gpu.loadAnimatedTilesGroup(session.romData, room.animatedTilesGroup());
        session.setActiveRoom(room);
        session.updateWitchEnvironment();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(session.activeRoom.gbcOverlay());
        session.overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        session.synchronizeSwitchBlockCollisionState();
    }

    void loadIndoor(int mapId, int roomId) {
        session.loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        session.loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
        session.indoorMapPosition = savedIndoorMapPosition & 0xFF;
    }

    void loadIndoor(int mapId, int roomId, int mapCategory) {
        session.loadIndoor(mapId, roomId, mapCategory, 4);
    }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        session.roomLoadSerial++;
        session.indoorMapPosition = session.findIndoorMapPosition(mapId, roomId);
        session.clearTransientRoomState();
        session.recentRoomEntityClears.visit(roomId);
        session.roomState.dungeonItemState.loadForMap(mapId, true);
        // LoadRoom $00:3162: side-view passages do not mark the map visited.
        if (mapCategory != Warp.CATEGORY_SIDESCROLL)
            session.indoorStatusTableForMap(mapId)[roomId] |= (byte) 0x80;
        session.activeRoomEvent = session.dungeonRoomEventTable.eventFor(mapId, roomId);
        session.gpu.loadIndoorTiles(session.romData, mapId, roomId, mapCategory == Warp.CATEGORY_SIDESCROLL);
        LoadedRoom room = session.roomLoader.loadIndoor(
            mapId, roomId, session.activeRoom == null ? null : session.activeRoom.palettes(), mapCategory,
            session.recentRoomEntityClears.mask(roomId), session.indoorStatusTableForMap(mapId), session.hasBirdKey,
            session.tarinFlag, session.playerShieldLevel);
        session.gpu.loadAnimatedTilesGroup(session.romData, room.animatedTilesGroup());
        var oldSelection = session.activeRoom == null || session.activeRoom.entities() == null
            ? null : session.activeRoom.entities().spriteSelection();
        var palettes = session.roomPaletteLoader.indoorTransition(mapId, roomId, transitionDirection,
            room.palettes(), room.entities().spriteSelection().objectPalettes(),
            session.activeRoom == null ? null : session.activeRoom.palettes(),
            oldSelection == null ? null : oldSelection.objectPalettes());
        session.setActiveRoom(room, palettes.objects());
        for (int row = 0; row < 8; row++)
            System.arraycopy(palettes.background()[row], 0, session.activeRoom.palettes()[row], 0, 4);
        session.initializeSwitchBlockTiles();
        session.indoorTorchPaletteEffect.reset(session.activeRoom.palettes(), session.countUnlitTorches());
        session.copyIndoorTorchPalettesToActiveRoom();
        session.updateWitchEnvironment();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
        session.overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        session.synchronizeSwitchBlockCollisionState();
    }

    void setActiveRoom(LoadedRoom room) {
        session.setActiveRoom(room, null);
    }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) {
        session.entityInputs.secretSeashellScreenShakeActive = false;
        session.entityInputs.secretSeashellPegasusCollisionActive = false;
        session.entityInputs.secretSeashellPegasusCollisionX = 0;
        session.entityInputs.secretSeashellPegasusCollisionY = 0;
        RoomEntitySnapshot entities = room.entities();
        if (objectPalettes != null && entities != null && entities.spriteSelection() != null) {
            entities = entities.withSpriteSelection(entities.spriteSelection().withObjectPalettes(objectPalettes));
        }
        if (entities != null && entities.spriteSelection() != null) {
            var selection = entities.spriteSelection();
            if (selection.hasStandardSheets()) {
                session.gpu.loadEntitySpriteSheets(session.romData, selection.sheetValues());
                entities = entities.withSpriteTiles(session.gpu.snapshotEntityTiles());
            } else if (selection.roomTable() == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
                session.gpu.loadColorDungeonEntitySheets(session.romData, selection.roomId());
                entities = entities.withSpriteTiles(session.gpu.snapshotEntityTiles());
            }
        }
        if (entities != null) {
            entities = entities.withSideScrolling(room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        }
        session.activeRoom = ActiveRoom.from(room, entities);
        if (session.activeRoom.shutterDoorMask() != 0
            && session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            boolean ordinaryEventPending = session.activeRoomEvent != 0
                && ((session.activeRoomEvent & session.EVENT_EFFECT_MASK) == session.EVENT_EFFECT_OPEN_LOCKED_DOORS
                    || (session.activeRoomStatusFlags() & session.ROOM_STATUS_EVENT_1) == 0);
            if (ordinaryEventPending) {
                // $02:5E7B keeps entry doors open until Link is inside the room.
                session.pendingShutterCloseMask = session.activeRoom.shutterDoorMask();
            } else {
                session.openActiveRoomShutterDoors();
            }
        }
        session.configureStaircase(session.activeRoom.staircaseLocation());
        boolean minibossDefeated = session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
            && session.activeRoom.mapId() >= 0
            && session.activeRoom.mapId() < session.roomState.dungeonProgressFlags.length
            && ((session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] & 0x01) != 0
                || (session.activeRoomStatusFlags() & session.ROOM_STATUS_EVENT_2) != 0);
        if (minibossDefeated && session.activeRoomEvent == session.EVENT_CLEAR_MIDBOSS) {
            session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] |= 0x01;
            session.openActiveRoomShutterDoors();
            session.activeRoomEvent = 0;
        }
        session.entityBridge.entityRuntime = entities == null ? null : RoomEntityRuntime.from(
            entities, session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            session.entityRandomByteSource, session.entitySpriteHandlerCatalog, session.enemyCombatTables,
            session.chestContentsTable, session.romTables, session.romData);
        if (session.entityBridge.entityRuntime != null) {
            session.entityBridge.entityRuntime.setBooBuddyTriggerCount(session.roomTriggerCount);
            session.entityBridge.entityRuntime.setColorShellWorld(session.colorShellWorld);
            session.entityBridge.entityRuntime.setFollowingNpcState(session.followingNpcState);
            session.entityBridge.entityRuntime.setEntityMapId(session.activeRoom.mapId());
            session.entityBridge.entityRuntime.setEntityRoomId(session.activeRoom.roomId());
            session.entityBridge.entityRuntime.setOwlDialogResolver(session.owlEventDialogResolver::globalDialogId);
            session.entityBridge.entityRuntime.setOwlDefaultMusicResolver(session.owlEventDialogResolver::defaultMusicTrack);
            session.entityBridge.entityRuntime.setGroundInteraction(session::entityGroundInteraction);
            session.entityBridge.entityRuntime.setBackgroundInteraction(session.entityBackgroundInteraction);
            session.entityBridge.entityRuntime.setObjectQuery(session::entityObjectSample);
            session.entityBridge.entityRuntime.setObjectIntersectionQuery(session::entityObjectIntersectionSample);
            session.entityBridge.entityRuntime.setGroundInteractionSideScrolling(
                session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
            session.entityBridge.entityRuntime.setActionButtonsHeld(session.entityInputs.actionButtonAHeld, session.entityInputs.actionButtonBHeld);
            session.entityBridge.entityRuntime.setTalkState(
                session.entityInputs.entityInventoryAppearing, session.entityInputs.entityDialogCooldown, session.entityInputs.entityWindowY);
            session.entityBridge.entityRuntime.setJoypadHeld(session.entityInputs.joypadHeld);
            session.entityBridge.entityRuntime.setPressedButtonsMask(session.entityInputs.entityPressedButtonsMask);
            session.entityBridge.entityRuntime.setPowerBraceletButtonHeld(session.entityInputs.powerBraceletButtonHeld);
            session.entityBridge.entityRuntime.setBombButtonHeld(session.entityInputs.bombButtonHeld);
            session.entityBridge.entityRuntime.setLiftedLinkC13B(session.followingEntityYOffset);
            session.entityBridge.entityRuntime.setEntityRoomStatus(session.activeRoomStatusFlags());
            session.entityBridge.entityRuntime.setDungeonMinibossReady(session.activeRoom.mapId() >= 0
                && session.activeRoom.mapId() < session.roomState.dungeonProgressFlags.length
                && (session.roomState.dungeonProgressFlags[session.activeRoom.mapId()] & 0x01) != 0);
            session.entityBridge.entityRuntime.setTailKeyOwned(session.hasTailKey);
            session.entityBridge.entityRuntime.setOwlInstrumentFlags(
                Byte.toUnsignedInt(session.roomState.dungeonProgressFlags[0]),
                Byte.toUnsignedInt(session.roomState.dungeonProgressFlags[1]));
            session.entityBridge.entityRuntime.setSecretSeashellPegasusCollisionState(
                session.entityInputs.secretSeashellScreenShakeActive, session.entityInputs.secretSeashellPegasusCollisionActive,
                session.entityInputs.secretSeashellPegasusCollisionX, session.entityInputs.secretSeashellPegasusCollisionY);
            session.configureEnemyDropRuntime();
        }
        session.followingNpcRoomNeedsSync = true;
        session.synchronizeFollowingNpcEntitiesIfNeeded();
        if (session.roomLoadListener != null) {
            session.roomLoadListener.roomLoaded(session.activeRoom);
        }
    }

    void initializeSwitchBlockTiles() {
        // LoadRoom configures wRoomSwitchableObject only for $DB/$DC.
        // These VRAM slots hold furniture in houses and must stay intact there.
        boolean hasBlocks = false;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int col = 0; col < RoomConstants.OBJECTS_PER_ROW; col++) {
                int object = session.activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + col];
                hasBlocks |= object == 0xDB || object == 0xDC;
            }
        }
        if (!hasBlocks) return;
        for (SwitchBlockAnimation.TileCopy copy
            : SwitchBlockAnimation.initialCopies(session.switchBlocksState)) {
            session.gpu.copySwitchBlockTiles(session.romData, copy.sourceOffset(), copy.destinationTile());
        }
    }

    void synchronizeSwitchBlockCollisionState() {
        session.overworldCollision.setSwitchBlocksState(session.switchBlocksState);
    }

    void configureEnemyDropRuntime() {
        if (session.entityBridge.entityRuntime == null) {
            return;
        }
        session.entityBridge.entityRuntime.setDialogActive(session.entityInputs.entityDialogActive);
        session.entityBridge.entityRuntime.setEnemyDropResolver(session.enemyDropResolver);
        session.entityBridge.entityRuntime.setEnemyDropCounters(session.enemyDropCounters);
        session.entityBridge.entityRuntime.setEnemyDropPlayerState(
            session.enemyDropMaxHearts, session.enemyDropHealth, session.enemyDropActivePowerUp);
        session.entityBridge.entityRuntime.setEnemyDropBossBattle(session.roomHasBossBattle());
    }

    boolean roomHasBossBattle() {
        if (session.activeRoom == null || session.activeRoom.entities() == null) {
            return false;
        }
        for (RoomEntity entity : session.activeRoom.entities().loadedEntities()) {
            if ((session.romTables.entityOptions1(entity.type()) & 0x80) != 0) {
                return true;
            }
        }
        return false;
    }

    void loadRoomSpecificTilesIfNeeded(int roomId) {
        int tilesetId = session.overworldTilesetTable.tilesetIdForRoom(roomId);
        if (OverworldTilesetTable.shouldLoadRoomSpecificTileset(roomId, tilesetId, session.currentOverworldTilesetId)) {
            session.gpu.loadRoomSpecificTiles(session.romData, roomId, tilesetId);
            session.currentOverworldTilesetId = tilesetId;
        }
    }
}
