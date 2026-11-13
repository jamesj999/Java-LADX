package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_OWL_EVENT;
import static linksawakening.world.room.RoomSessionRules.ENTITY_TARIN;
import static linksawakening.world.room.RoomSessionRules.OCARINA_MARIN_DIALOG_ID;
import static linksawakening.world.room.RoomSessionRules.OCARINA_MARIN_DIALOG_TABLE;
import static linksawakening.world.room.RoomSessionRules.OCARINA_NO_SONG_DIALOG_ID;
import static linksawakening.world.room.RoomSessionRules.OCARINA_NO_SONG_DIALOG_TABLE;
import static linksawakening.world.room.RoomSessionRules.ROOM_OW_MYSTERIOUS_WOODS_LOST;
import static linksawakening.world.room.RoomSessionRules.ROOM_OW_TAIL_KEY_CHEST;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_DISCOVERED;
import static linksawakening.world.room.RoomSessionRules.STAIRCASE_ACTIVE;
import static linksawakening.world.room.RoomSessionRules.STAIRCASE_INACTIVE;
import static linksawakening.world.room.RoomSessionRules.STAIRCASE_NONE;
import static linksawakening.world.room.RoomSessionRules.W_TILESET_NO_UPDATE;

import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

/** Room replacement and scroll setup for GameplayWorldLoadRoomHandler ($01:442B). */
final class RoomTransitionController {
    private final Dependencies dependencies;

    RoomTransitionController(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void loadWarpDestination(Warp warp) {
        // GameplayWorldLoadRoomHandler ($01:442B) clears $C000..$D2FF,
        // including the recent-room ring and transient entity-clear masks.
        // Scrolling between adjacent rooms does not take this path.
        if (warp.category() != Warp.CATEGORY_SIDESCROLL) {
            Warp spawn = warp;
            int indoorRoom = dependencies.roomSessionViews().indoorRoomPositionForSave();
            if (indoorRoom < 0) indoorRoom = dependencies.location().respawnIndoorRoom;
            if (warp.category() == Warp.CATEGORY_INDOOR && (warp.destMap() < 0x0A || warp.destMap() == 0xFF)) {
                var rom = dependencies.romData();
                var address = new linksawakening.rom.RomAddress(
                        0x14, warp.destMap() == 0xFF ? 0x4E3C : 0x4DF1 + warp.destMap() * 5);
                spawn = new Warp(
                        rom.u8(address),
                        rom.u8(address.plus(1)),
                        rom.u8(address.plus(2)),
                        rom.u8(address.plus(3)),
                        rom.u8(address.plus(4)),
                        -1);
                indoorRoom = warp.destMap() == 0xFF
                        ? 0x3A
                        : rom.u8(new linksawakening.rom.RomAddress(0x14, 0x4E41 + warp.destMap()));
            }
            dependencies.location().setRespawnPoint(spawn, indoorRoom);
        }
        dependencies.entityFrame().recentRoomEntityClears.reset();
        boolean wasIndoor = dependencies.location().activeRoom != null
                && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD;
        // hFreeWarpDataAddress is the source wIsIndoor ($00:18AD–18B0).
        // $00:1904/$00:1952 clear only when destination indoor status differs.
        if (wasIndoor != (warp.category() != Warp.CATEGORY_OVERWORLD)) {
            dependencies.location().mapBoundaryWarp.run();
        }
        dependencies.puzzles().dungeonTitleMessageCountdown = 0;
        if (warp.category() == Warp.CATEGORY_OVERWORLD) {
            if (wasIndoor) {
                dependencies.gpu().loadBaseOverworldTiles(dependencies.romData());
                dependencies.location().currentOverworldTilesetId = W_TILESET_NO_UPDATE;
            }
            dependencies.roomLoadCoordinator().loadOverworld(warp.destRoom());
            dependencies.roomLoadCoordinator().loadRoomSpecificTilesIfNeeded(warp.destRoom());
        } else {
            dependencies.roomLoadCoordinator().loadIndoor(warp.destMap(), warp.destRoom(), warp.category());
            // $00:194C-1965: old wIsIndoor is hFreeWarpDataAddress.
            if (!wasIndoor && warp.destMap() < 0x0A) dependencies.puzzles().dungeonTitleMessageCountdown = 0x30;
        }
    }

    int tickDungeonTitleMessage(
            boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        if (dependencies.puzzles().dungeonTitleMessageCountdown == 0 || dialogActive || !inventoryClosed) return -1;
        if (--dependencies.puzzles().dungeonTitleMessageCountdown != 0 || linkMotionState != 0 || freeMovement)
            return -1;
        return 0x56 + dependencies.location().activeRoom.mapId();
    }

    void startAdjacentOverworldScroll(
            ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = dependencies.roomSessionViews().renderSnapshot();
        int nextRoomId = this.adjacentOverworldRoomId(direction);
        boolean discoveredTailKeyRoom = nextRoomId == ROOM_OW_TAIL_KEY_CHEST
                && direction == ScrollController.UP
                && (dependencies.roomState().overworldRoomStatus[nextRoomId] & ROOM_STATUS_DISCOVERED) == 0;
        if (discoveredTailKeyRoom) {
            dependencies.roomState().overworldRoomStatus[nextRoomId] |= (byte) ROOM_STATUS_DISCOVERED;
        }
        dependencies.roomLoadCoordinator().loadRoomSpecificTilesIfNeeded(nextRoomId);
        dependencies.roomLoadCoordinator().loadOverworld(nextRoomId, true);
        if (discoveredTailKeyRoom) {
            dependencies
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            0, ENTITY_OWL_EVENT, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x02));
        }
        scrollController.start(
                direction, linkScreenX, linkScreenY, previousRoom, RoomInteractionGeometry.scrollTarget(direction));
    }

    void startMysteriousWoodsLostScroll(ScrollController scrollController, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = dependencies.roomSessionViews().renderSnapshot();
        dependencies.roomLoadCoordinator().loadRoomSpecificTilesIfNeeded(ROOM_OW_MYSTERIOUS_WOODS_LOST);
        dependencies.roomLoadCoordinator().loadOverworld(ROOM_OW_MYSTERIOUS_WOODS_LOST, true);
        scrollController.start(
                ScrollController.UP,
                linkScreenX,
                linkScreenY,
                previousRoom,
                RoomInteractionGeometry.scrollTarget(ScrollController.UP));
        dependencies
                .entityRuntimeState()
                .outbox
                .pendingRoomEntityEvents
                .add(new EntityCombatEvent(0, ENTITY_TARIN, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1E));
    }

    void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = dependencies.roomSessionViews().renderSnapshot();
        RoomScrollNavigation.IndoorRoomDestination destination = this.adjacentIndoorRoom(direction);
        dependencies
                .roomLoadCoordinator()
                .loadIndoor(
                        dependencies.location().activeRoom.mapId(),
                        destination.roomId(),
                        dependencies.location().activeRoom.mapCategory(),
                        switch (direction) {
                            case ScrollController.RIGHT -> 0;
                            case ScrollController.LEFT -> 1;
                            case ScrollController.UP -> 2;
                            case ScrollController.DOWN -> 3;
                            default -> throw new IllegalArgumentException("Indoor scroll direction " + direction);
                        });
        dependencies.location().indoorMapPosition = destination.mapPosition();
        scrollController.start(
                direction, linkScreenX, linkScreenY, previousRoom, RoomInteractionGeometry.scrollTarget(direction));
    }

    boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Ocarina countdown must be an unsigned byte: " + countdown);
        }
        if (songFlags < 0 || songFlags > 0xFF) {
            throw new IllegalArgumentException("Ocarina song flags must be an unsigned byte: " + songFlags);
        }
        if (selectedSong < 0 || selectedSong > 0xFF) {
            throw new IllegalArgumentException("Selected Ocarina song must be an unsigned byte: " + selectedSong);
        }
        if (dependencies.location().activeRoom == null
                || dependencies.entityRuntimeState().runtime == null
                || dependencies.items().ocarinaPlaybackCountdown != 0) {
            return false;
        }
        dependencies.items().ocarinaPlaybackCountdown = countdown;
        dependencies.items().ocarinaSongFlags = songFlags & 0xFF;
        dependencies.items().selectedSongIndex = selectedSong & 0xFF;
        dependencies.items().ocarinaAnimationCounter = 0;
        dependencies.items().ocarinaAnimationPhase = 0;
        dependencies.items().pendingManboTransition = false;
        dependencies.items().pendingInstrumentTransition = false;
        return true;
    }

    boolean ocarinaPlaying() {
        return dependencies.items().ocarinaPlaybackCountdown != 0;
    }

    int ocarinaAnimationState() {
        if (!this.ocarinaPlaying()) {
            return -1;
        }
        return dependencies.items().ocarinaAnimationPhase == 0 ? 0x76 : 0x75;
    }

    Warp consumeManboPondTransitionRequest() {
        if (!dependencies.items().pendingManboTransition || dependencies.location().activeRoom == null) {
            return null;
        }
        dependencies.items().pendingManboTransition = false;
        return dependencies
                .manboWarpResolver()
                .resolve(
                        dependencies.location().activeRoom.mapCategory(),
                        dependencies.location().activeRoom.mapId());
    }

    Warp consumeInstrumentTransitionRequest() {
        if (!dependencies.items().pendingInstrumentTransition
                || dependencies.location().activeRoom == null
                || !dependencies.location().activeRoom.hasWarps()) {
            return null;
        }
        dependencies.items().pendingInstrumentTransition = false;
        return dependencies.location().activeRoom.firstWarp();
    }

    Warp consumeClearedDungeonWarp() {
        Warp pending = dependencies.location().pendingDungeonWarp;
        dependencies.location().pendingDungeonWarp = null;
        return pending;
    }

    void finishOcarinaPlayback() {
        if (dependencies.followers().followingNpcState.marinFollowing()) {
            if (dependencies.items().selectedSong() == linksawakening.state.OcarinaSong.MANBO_MAMBO) {
                dependencies.items().pendingManboTransition = true;
            } else if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                dependencies
                        .entityRuntimeState()
                        .outbox
                        .pendingRoomDialogRequests
                        .add(new DialogRequest(OCARINA_MARIN_DIALOG_TABLE, OCARINA_MARIN_DIALOG_ID));
            }
            return;
        }
        if (dependencies.items().ocarinaSongFlags == 0) {
            dependencies
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomDialogRequests
                    .add(new DialogRequest(OCARINA_NO_SONG_DIALOG_TABLE, OCARINA_NO_SONG_DIALOG_ID));
        } else if (dependencies.items().selectedSong() == linksawakening.state.OcarinaSong.MANBO_MAMBO) {
            dependencies.items().pendingManboTransition = true;
        }
    }

    void tickOcarinaAnimationHandler() {
        if (!this.ocarinaPlaying()) {
            return;
        }
        dependencies.items().ocarinaAnimationCounter++;
        if (dependencies.items().ocarinaAnimationCounter >= 0x38) {
            dependencies.items().ocarinaAnimationCounter = 0;
            dependencies.items().ocarinaAnimationPhase ^= 0x01;
        }
    }

    Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) {
        if (dependencies.puzzles().staircaseState == STAIRCASE_NONE || dependencies.location().activeRoom == null) {
            return null;
        }
        if (dependencies.puzzles().staircaseState == STAIRCASE_INACTIVE) {
            if (!RoomInteractionGeometry.withinUnsignedByteRange(
                            linkEntityX, dependencies.puzzles().staircaseX, 0x06, 0x0C)
                    || !RoomInteractionGeometry.withinUnsignedByteRange(
                            linkEntityY, dependencies.puzzles().staircaseY, 0x06, 0x0C)) {
                dependencies.puzzles().staircaseState = STAIRCASE_ACTIVE;
            }
            return null;
        }
        if (linkZ != 0
                || carryingObject
                || !RoomInteractionGeometry.withinUnsignedByteRange(
                        linkEntityX, dependencies.puzzles().staircaseX, 0x05, 0x0A)
                || !RoomInteractionGeometry.withinUnsignedByteRange(
                        linkEntityY, dependencies.puzzles().staircaseY, 0x05, 0x0A)
                || !dependencies.location().activeRoom.hasWarps()) {
            return null;
        }
        dependencies.puzzles().staircaseState = STAIRCASE_NONE;
        return dependencies.location().activeRoom.firstWarp();
    }

    void configureStaircase(int location) {
        dependencies.puzzles().configureStaircase(location);
    }

    int adjacentOverworldRoomId(int direction) {
        return dependencies.scrollNavigation().adjacentOverworldRoomId(dependencies.location().activeRoom, direction);
    }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) {
        return dependencies
                .scrollNavigation()
                .adjacentIndoorRoom(
                        dependencies.location().activeRoom, dependencies.location().indoorMapPosition, direction);
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomItemUseState items,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomEntityFrameState entityRuntimeState,
            GPU gpu,
            ManboWarpResolver manboWarpResolver,
            Rom romData,
            RoomLoadCoordinator roomLoadCoordinator,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomScrollNavigation scrollNavigation) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(gpu, "gpu");
            java.util.Objects.requireNonNull(manboWarpResolver, "manboWarpResolver");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(roomLoadCoordinator, "roomLoadCoordinator");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(scrollNavigation, "scrollNavigation");
        }
    }
}
