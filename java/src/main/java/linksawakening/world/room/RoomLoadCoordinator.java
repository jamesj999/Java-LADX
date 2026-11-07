package linksawakening.world.room;

import linksawakening.world.object.SwitchBlockAnimation;

import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.RoomEntitySnapshot;

import linksawakening.world.transition.Warp;

import linksawakening.rom.RomTables;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** RoomLoadCoordinator operations for the live room session. */
final class RoomLoadCoordinator {
    private final RoomSession session;
    RoomLoadCoordinator(RoomSession session) { this.session = session; }

    void loadInitialOverworld(int roomId) {
        session.context.gpu.loadBaseOverworldTiles(session.context.romData);
        session.loadRoomSpecificTilesIfNeeded(roomId);
        session.loadOverworld(roomId);
    }

    void loadOverworld(int roomId) {
        session.context.roomLoadSerial++;
        session.context.indoorMapPosition = -1;
        session.clearTransientRoomState();
        session.context.indoorTorchPaletteEffect.clear();
        session.context.recentRoomEntityClears.visit(roomId);
        LoadedRoom room = session.context.roomLoader.loadOverworld(
            roomId, session.context.recentRoomEntityClears.mask(roomId), session.context.roomState.overworldRoomStatus);
        session.context.gpu.loadAnimatedTilesGroup(session.context.romData, room.animatedTilesGroup());
        session.setActiveRoom(room);
        session.updateWitchEnvironment();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(session.context.activeRoom.gbcOverlay());
        session.context.overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_OVERWORLD);
        session.synchronizeSwitchBlockCollisionState();
    }

    void loadIndoor(int mapId, int roomId) {
        session.loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
    }

    void loadIndoorFromSavedPosition(int mapId, int roomId, int savedIndoorMapPosition) {
        session.loadIndoor(mapId, roomId, Warp.CATEGORY_INDOOR);
        session.context.indoorMapPosition = savedIndoorMapPosition & 0xFF;
    }

    void loadIndoor(int mapId, int roomId, int mapCategory) {
        session.loadIndoor(mapId, roomId, mapCategory, 4);
    }

    void loadIndoor(int mapId, int roomId, int mapCategory, int transitionDirection) {
        session.context.roomLoadSerial++;
        session.context.indoorMapPosition = session.findIndoorMapPosition(mapId, roomId);
        session.clearTransientRoomState();
        session.context.recentRoomEntityClears.visit(roomId);
        session.context.roomState.dungeonItemState.loadForMap(mapId, true);
        // LoadRoom $00:3162: side-view passages do not mark the map visited.
        if (mapCategory != Warp.CATEGORY_SIDESCROLL)
            session.indoorStatusTableForMap(mapId)[roomId] |= (byte) 0x80;
        session.context.activeRoomEvent = session.context.dungeonRoomEventTable.eventFor(mapId, roomId);
        session.context.gpu.loadIndoorTiles(session.context.romData, mapId, roomId, mapCategory == Warp.CATEGORY_SIDESCROLL);
        LoadedRoom room = session.context.roomLoader.loadIndoor(
            mapId, roomId, session.context.activeRoom == null ? null : session.context.activeRoom.palettes(), mapCategory,
            session.context.recentRoomEntityClears.mask(roomId), session.indoorStatusTableForMap(mapId), session.context.hasBirdKey,
            session.context.tarinFlag, session.context.playerShieldLevel);
        session.context.gpu.loadAnimatedTilesGroup(session.context.romData, room.animatedTilesGroup());
        var oldSelection = session.context.activeRoom == null || session.context.activeRoom.entities() == null
            ? null : session.context.activeRoom.entities().spriteSelection();
        var palettes = session.context.roomPaletteLoader.indoorTransition(mapId, roomId, transitionDirection,
            room.palettes(), room.entities().spriteSelection().objectPalettes(),
            session.context.activeRoom == null ? null : session.context.activeRoom.palettes(),
            oldSelection == null ? null : oldSelection.objectPalettes());
        session.setActiveRoom(room, palettes.objects());
        for (int row = 0; row < 8; row++)
            System.arraycopy(palettes.background()[row], 0, session.context.activeRoom.palettes()[row], 0, 4);
        session.initializeSwitchBlockTiles();
        session.context.indoorTorchPaletteEffect.reset(session.context.activeRoom.palettes(), session.countUnlitTorches());
        session.copyIndoorTorchPalettesToActiveRoom();
        session.updateWitchEnvironment();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
        session.context.overworldCollision.setPhysicsTable(RomTables.PHYSICS_TABLE_INDOORS1);
        session.synchronizeSwitchBlockCollisionState();
    }

    void setActiveRoom(LoadedRoom room) {
        session.setActiveRoom(room, null);
    }

    void setActiveRoom(LoadedRoom room, int[][] objectPalettes) {
        session.context.entityInputs.secretSeashellScreenShakeActive = false;
        session.context.entityInputs.secretSeashellPegasusCollisionActive = false;
        session.context.entityInputs.secretSeashellPegasusCollisionX = 0;
        session.context.entityInputs.secretSeashellPegasusCollisionY = 0;
        RoomEntitySnapshot entities = room.entities();
        if (objectPalettes != null && entities != null && entities.spriteSelection() != null) {
            entities = entities.withSpriteSelection(entities.spriteSelection().withObjectPalettes(objectPalettes));
        }
        if (entities != null && entities.spriteSelection() != null) {
            var selection = entities.spriteSelection();
            if (selection.hasStandardSheets()) {
                session.context.gpu.loadEntitySpriteSheets(session.context.romData, selection.sheetValues());
                entities = entities.withSpriteTiles(session.context.gpu.snapshotEntityTiles());
            } else if (selection.roomTable() == EntityRoomLoader.RoomTable.COLOR_DUNGEON) {
                session.context.gpu.loadColorDungeonEntitySheets(session.context.romData, selection.roomId());
                entities = entities.withSpriteTiles(session.context.gpu.snapshotEntityTiles());
            }
        }
        if (entities != null) {
            entities = entities.withSideScrolling(room.mapCategory() == Warp.CATEGORY_SIDESCROLL);
        }
        session.context.activeRoom = ActiveRoom.from(room, entities);
        if (session.context.activeRoom.shutterDoorMask() != 0
            && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            boolean ordinaryEventPending = session.context.activeRoomEvent != 0
                && ((session.context.activeRoomEvent & session.EVENT_EFFECT_MASK) == session.EVENT_EFFECT_OPEN_LOCKED_DOORS
                    || (session.activeRoomStatusFlags() & session.ROOM_STATUS_EVENT_1) == 0);
            if (ordinaryEventPending) {
                // $02:5E7B keeps entry doors open until Link is inside the room.
                session.context.pendingShutterCloseMask = session.context.activeRoom.shutterDoorMask();
            } else {
                session.openActiveRoomShutterDoors();
            }
        }
        session.configureStaircase(session.context.activeRoom.staircaseLocation());
        boolean minibossDefeated = session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
            && session.context.activeRoom.mapId() >= 0
            && session.context.activeRoom.mapId() < session.context.roomState.dungeonProgressFlags.length
            && ((session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] & 0x01) != 0
                || (session.activeRoomStatusFlags() & session.ROOM_STATUS_EVENT_2) != 0);
        if (minibossDefeated && session.context.activeRoomEvent == session.EVENT_CLEAR_MIDBOSS) {
            session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] |= 0x01;
            session.openActiveRoomShutterDoors();
            session.context.activeRoomEvent = 0;
        }
        session.context.entityBridge.entityRuntime = entities == null ? null : RoomEntityRuntime.from(
            entities, session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD,
            session.context.entityRandomByteSource, session.context.entitySpriteHandlerCatalog, session.context.enemyCombatTables,
            session.context.chestContentsTable, session.context.romTables, session.context.romData);
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.entityBridge.entityRuntime.configuration().setBooBuddyTriggerCount(session.context.roomTriggerCount);
            session.context.entityBridge.entityRuntime.configuration().setColorShellWorld(session.context.colorShellWorld);
            session.context.entityBridge.entityRuntime.configuration().setFollowingNpcState(session.context.followingNpcState);
            session.context.entityBridge.entityRuntime.configuration().setEntityMapId(session.context.activeRoom.mapId());
            session.context.entityBridge.entityRuntime.configuration().setEntityRoomId(session.context.activeRoom.roomId());
            session.context.entityBridge.entityRuntime.configuration().setOwlDialogResolver(session.context.owlEventDialogResolver::globalDialogId);
            session.context.entityBridge.entityRuntime.configuration().setOwlDefaultMusicResolver(session.context.owlEventDialogResolver::defaultMusicTrack);
            session.context.entityBridge.entityRuntime.configuration().setGroundInteraction(session::entityGroundInteraction);
            session.context.entityBridge.entityRuntime.configuration().setBackgroundInteraction(session.context.entityBackgroundInteraction);
            session.context.entityBridge.entityRuntime.configuration().setObjectQuery(session::entityObjectSample);
            session.context.entityBridge.entityRuntime.configuration().setObjectIntersectionQuery(session::entityObjectIntersectionSample);
            session.context.entityBridge.entityRuntime.configuration().setGroundInteractionSideScrolling(
                session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL);
            session.context.entityBridge.entityRuntime.configuration().setActionButtonsHeld(session.context.entityInputs.actionButtonAHeld, session.context.entityInputs.actionButtonBHeld);
            session.context.entityBridge.entityRuntime.configuration().setTalkState(
                session.context.entityInputs.entityInventoryAppearing, session.context.entityInputs.entityDialogCooldown, session.context.entityInputs.entityWindowY);
            session.context.entityBridge.entityRuntime.configuration().setJoypadHeld(session.context.entityInputs.joypadHeld);
            session.context.entityBridge.entityRuntime.configuration().setPressedButtonsMask(session.context.entityInputs.entityPressedButtonsMask);
            session.context.entityBridge.entityRuntime.configuration().setPowerBraceletButtonHeld(session.context.entityInputs.powerBraceletButtonHeld);
            session.context.entityBridge.entityRuntime.configuration().setBombButtonHeld(session.context.entityInputs.bombButtonHeld);
            session.context.entityBridge.entityRuntime.configuration().setLiftedLinkC13B(session.context.followingEntityYOffset);
            session.context.entityBridge.entityRuntime.configuration().setEntityRoomStatus(session.activeRoomStatusFlags());
            session.context.entityBridge.entityRuntime.configuration().setDungeonMinibossReady(session.context.activeRoom.mapId() >= 0
                && session.context.activeRoom.mapId() < session.context.roomState.dungeonProgressFlags.length
                && (session.context.roomState.dungeonProgressFlags[session.context.activeRoom.mapId()] & 0x01) != 0);
            session.context.entityBridge.entityRuntime.configuration().setTailKeyOwned(session.context.hasTailKey);
            session.context.entityBridge.entityRuntime.configuration().setOwlInstrumentFlags(
                Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[0]),
                Byte.toUnsignedInt(session.context.roomState.dungeonProgressFlags[1]));
            session.context.entityBridge.entityRuntime.configuration().setSecretSeashellPegasusCollisionState(
                session.context.entityInputs.secretSeashellScreenShakeActive, session.context.entityInputs.secretSeashellPegasusCollisionActive,
                session.context.entityInputs.secretSeashellPegasusCollisionX, session.context.entityInputs.secretSeashellPegasusCollisionY);
            session.configureEnemyDropRuntime();
        }
        session.context.followingNpcRoomNeedsSync = true;
        session.synchronizeFollowingNpcEntitiesIfNeeded();
        if (session.context.roomLoadListener != null) {
            session.context.roomLoadListener.roomLoaded(session.context.activeRoom);
        }
    }

    void initializeSwitchBlockTiles() {
        // LoadRoom configures wRoomSwitchableObject only for $DB/$DC.
        // These VRAM slots hold furniture in houses and must stay intact there.
        boolean hasBlocks = false;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int col = 0; col < RoomConstants.OBJECTS_PER_ROW; col++) {
                int object = session.context.activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + col];
                hasBlocks |= object == 0xDB || object == 0xDC;
            }
        }
        if (!hasBlocks) return;
        for (SwitchBlockAnimation.TileCopy copy
            : SwitchBlockAnimation.initialCopies(session.context.switchBlocksState)) {
            session.context.gpu.copySwitchBlockTiles(session.context.romData, copy.sourceOffset(), copy.destinationTile());
        }
    }

    void synchronizeSwitchBlockCollisionState() {
        session.context.overworldCollision.setSwitchBlocksState(session.context.switchBlocksState);
    }

    void configureEnemyDropRuntime() {
        if (session.context.entityBridge.entityRuntime == null) {
            return;
        }
        session.context.entityBridge.entityRuntime.configuration().setDialogActive(session.context.entityInputs.entityDialogActive);
        session.context.entityBridge.entityRuntime.configuration().setEnemyDropResolver(session.context.enemyDropResolver);
        session.context.entityBridge.entityRuntime.configuration().setEnemyDropCounters(session.context.enemyDropCounters);
        session.context.entityBridge.entityRuntime.configuration().setEnemyDropPlayerState(
            session.context.enemyDropMaxHearts, session.context.enemyDropHealth, session.context.enemyDropActivePowerUp);
        session.context.entityBridge.entityRuntime.configuration().setEnemyDropBossBattle(session.roomHasBossBattle());
    }

    boolean roomHasBossBattle() {
        if (session.context.activeRoom == null || session.context.activeRoom.entities() == null) {
            return false;
        }
        for (RoomEntity entity : session.context.activeRoom.entities().loadedEntities()) {
            if ((session.context.romTables.entityOptions1(entity.type()) & 0x80) != 0) {
                return true;
            }
        }
        return false;
    }

    void loadRoomSpecificTilesIfNeeded(int roomId) {
        int tilesetId = session.context.overworldTilesetTable.tilesetIdForRoom(roomId);
        if (OverworldTilesetTable.shouldLoadRoomSpecificTileset(roomId, tilesetId, session.context.currentOverworldTilesetId)) {
            session.context.gpu.loadRoomSpecificTiles(session.context.romData, roomId, tilesetId);
            session.context.currentOverworldTilesetId = tilesetId;
        }
    }
}
