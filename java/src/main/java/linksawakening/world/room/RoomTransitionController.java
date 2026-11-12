package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gpu.GPU;
import linksawakening.rom.Rom;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

/** RoomTransitionController operations for the live room session. */
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
            int indoorRoom = dependencies.roomSessionViews().get().indoorRoomPositionForSave();
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
            dependencies.roomLoadCoordinator().get().loadOverworld(warp.destRoom());
            dependencies.roomLoadCoordinator().get().loadRoomSpecificTilesIfNeeded(warp.destRoom());
        } else {
            dependencies.roomLoadCoordinator().get().loadIndoor(warp.destMap(), warp.destRoom(), warp.category());
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
        RoomRenderSnapshot previousRoom = dependencies.roomSessionViews().get().renderSnapshot();
        int nextRoomId = dependencies.roomTransitionController().get().adjacentOverworldRoomId(direction);
        boolean discoveredTailKeyRoom = nextRoomId == ROOM_OW_TAIL_KEY_CHEST
                && direction == ScrollController.UP
                && (dependencies.roomState().overworldRoomStatus[nextRoomId] & ROOM_STATUS_DISCOVERED) == 0;
        if (discoveredTailKeyRoom) {
            dependencies.roomState().overworldRoomStatus[nextRoomId] |= (byte) ROOM_STATUS_DISCOVERED;
        }
        dependencies.roomLoadCoordinator().get().loadRoomSpecificTilesIfNeeded(nextRoomId);
        dependencies.roomLoadCoordinator().get().loadOverworld(nextRoomId, true);
        if (discoveredTailKeyRoom) {
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new EntityCombatEvent(
                            0, ENTITY_OWL_EVENT, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x02));
        }
        scrollController.start(
                direction, linkScreenX, linkScreenY, previousRoom, RoomInteractionGeometry.scrollTarget(direction));
    }

    void startMysteriousWoodsLostScroll(ScrollController scrollController, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = dependencies.roomSessionViews().get().renderSnapshot();
        dependencies.roomLoadCoordinator().get().loadRoomSpecificTilesIfNeeded(ROOM_OW_MYSTERIOUS_WOODS_LOST);
        dependencies.roomLoadCoordinator().get().loadOverworld(ROOM_OW_MYSTERIOUS_WOODS_LOST, true);
        scrollController.start(
                ScrollController.UP,
                linkScreenX,
                linkScreenY,
                previousRoom,
                RoomInteractionGeometry.scrollTarget(ScrollController.UP));
        dependencies
                .entityBridge()
                .get()
                .outbox
                .pendingRoomEntityEvents
                .add(new EntityCombatEvent(0, ENTITY_TARIN, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1E));
    }

    void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = dependencies.roomSessionViews().get().renderSnapshot();
        RoomScrollNavigation.IndoorRoomDestination destination =
                dependencies.roomTransitionController().get().adjacentIndoorRoom(direction);
        dependencies
                .roomLoadCoordinator()
                .get()
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
                || dependencies.entityBridge().get().entityRuntime == null
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
        if (!dependencies.roomTransitionController().get().ocarinaPlaying()) {
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
            if (dependencies.items().selectedSongIndex == 0x01) {
                dependencies.items().pendingManboTransition = true;
            } else if (dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                dependencies
                        .entityBridge()
                        .get()
                        .outbox
                        .pendingRoomDialogRequests
                        .add(new DialogRequest(OCARINA_MARIN_DIALOG_TABLE, OCARINA_MARIN_DIALOG_ID));
            }
            return;
        }
        if (dependencies.items().ocarinaSongFlags == 0) {
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomDialogRequests
                    .add(new DialogRequest(OCARINA_NO_SONG_DIALOG_TABLE, OCARINA_NO_SONG_DIALOG_ID));
        } else if (dependencies.items().selectedSongIndex == 0x01) {
            dependencies.items().pendingManboTransition = true;
        }
    }

    void tickOcarinaAnimationHandler() {
        if (!dependencies.roomTransitionController().get().ocarinaPlaying()) {
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
        if (location < 0) {
            dependencies.puzzles().staircaseState = STAIRCASE_NONE;
            dependencies.puzzles().staircaseX = 0;
            dependencies.puzzles().staircaseY = 0;
            return;
        }
        dependencies.puzzles().staircaseState = STAIRCASE_INACTIVE;
        dependencies.puzzles().staircaseY = (location & 0xF0) + 0x10;
        dependencies.puzzles().staircaseX = ((location & 0x0F) << 4) + 0x08;
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
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            GPU gpu,
            ManboWarpResolver manboWarpResolver,
            Rom romData,
            java.util.function.Supplier<RoomLoadCoordinator> roomLoadCoordinator,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController,
            RoomScrollNavigation scrollNavigation) {}
}
