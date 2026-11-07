package linksawakening.world.room;

import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

import linksawakening.world.entity.event.DialogRequest;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** RoomTransitionController operations for the live room session. */
final class RoomTransitionController {
    private final RoomSession session;
    RoomTransitionController(RoomSession session) { this.session = session; }

    void loadWarpDestination(Warp warp) {
        // GameplayWorldLoadRoomHandler ($01:442B) clears $C000..$D2FF,
        // including the recent-room ring and transient entity-clear masks.
        // Scrolling between adjacent rooms does not take this path.
        session.context.recentRoomEntityClears.reset();
        boolean wasIndoor = session.context.activeRoom != null && session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD;
        session.context.dungeonTitleMessageCountdown = 0;
        if (warp.category() == Warp.CATEGORY_OVERWORLD) {
            if (wasIndoor) {
                session.context.gpu.loadBaseOverworldTiles(session.context.romData);
                session.context.currentOverworldTilesetId = session.W_TILESET_NO_UPDATE;
            }
            session.loadOverworld(warp.destRoom());
            session.loadRoomSpecificTilesIfNeeded(warp.destRoom());
        } else {
            session.loadIndoor(warp.destMap(), warp.destRoom(), warp.category());
            // $00:194C-1965: old wIsIndoor is hFreeWarpDataAddress.
            if (!wasIndoor && warp.destMap() < 0x0A) session.context.dungeonTitleMessageCountdown = 0x30;
        }
    }

    int tickDungeonTitleMessage(boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        if (session.context.dungeonTitleMessageCountdown == 0 || dialogActive || !inventoryClosed) return -1;
        if (--session.context.dungeonTitleMessageCountdown != 0 || linkMotionState != 0 || freeMovement) return -1;
        return 0x56 + session.context.activeRoom.mapId();
    }

    void startAdjacentOverworldScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = session.renderSnapshot();
        int nextRoomId = session.adjacentOverworldRoomId(direction);
        boolean discoveredTailKeyRoom = nextRoomId == session.ROOM_OW_TAIL_KEY_CHEST
            && direction == ScrollController.UP
            && (session.context.roomState.overworldRoomStatus[nextRoomId] & session.ROOM_STATUS_DISCOVERED) == 0;
        if (discoveredTailKeyRoom) {
            session.context.roomState.overworldRoomStatus[nextRoomId] |= (byte) session.ROOM_STATUS_DISCOVERED;
        }
        session.loadRoomSpecificTilesIfNeeded(nextRoomId);
        session.loadOverworld(nextRoomId);
        if (discoveredTailKeyRoom) {
            session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
                0, session.ENTITY_OWL_EVENT, 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, 0x02));
        }
        scrollController.start(direction, linkScreenX, linkScreenY, previousRoom, session.scrollTarget(direction));
    }

    void startMysteriousWoodsLostScroll(ScrollController scrollController, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = session.renderSnapshot();
        session.loadRoomSpecificTilesIfNeeded(session.ROOM_OW_MYSTERIOUS_WOODS_LOST);
        session.loadOverworld(session.ROOM_OW_MYSTERIOUS_WOODS_LOST);
        scrollController.start(ScrollController.UP, linkScreenX, linkScreenY,
            previousRoom, session.scrollTarget(ScrollController.UP));
        session.context.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
            0, session.ENTITY_TARIN, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1E));
    }

    void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = session.renderSnapshot();
        RoomScrollNavigation.IndoorRoomDestination destination = session.adjacentIndoorRoom(direction);
        session.loadIndoor(session.context.activeRoom.mapId(), destination.roomId(), session.context.activeRoom.mapCategory(),
            switch (direction) {
                case ScrollController.RIGHT -> 0;
                case ScrollController.LEFT -> 1;
                case ScrollController.UP -> 2;
                case ScrollController.DOWN -> 3;
                default -> throw new IllegalArgumentException("Indoor scroll direction " + direction);
            });
        session.context.indoorMapPosition = destination.mapPosition();
        scrollController.start(direction, linkScreenX, linkScreenY, previousRoom, session.scrollTarget(direction));
    }

    boolean startOcarina(int countdown, int songFlags, int selectedSong) {
        if (countdown < 0 || countdown > 0xFF) {
            throw new IllegalArgumentException("Ocarina countdown must be an unsigned byte: "
                + countdown);
        }
        if (songFlags < 0 || songFlags > 0xFF) {
            throw new IllegalArgumentException("Ocarina song flags must be an unsigned byte: "
                + songFlags);
        }
        if (selectedSong < 0 || selectedSong > 0xFF) {
            throw new IllegalArgumentException("Selected Ocarina song must be an unsigned byte: "
                + selectedSong);
        }
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null || session.context.ocarinaPlaybackCountdown != 0) {
            return false;
        }
        session.context.ocarinaPlaybackCountdown = countdown;
        session.context.ocarinaSongFlags = songFlags & 0xFF;
        session.context.selectedSongIndex = selectedSong & 0xFF;
        session.context.ocarinaAnimationCounter = 0;
        session.context.ocarinaAnimationPhase = 0;
        session.context.pendingManboTransition = false;
        session.context.pendingInstrumentTransition = false;
        return true;
    }

    boolean ocarinaPlaying() {
        return session.context.ocarinaPlaybackCountdown != 0;
    }

    int ocarinaAnimationState() {
        if (!session.ocarinaPlaying()) {
            return -1;
        }
        return session.context.ocarinaAnimationPhase == 0 ? 0x76 : 0x75;
    }

    Warp consumeManboPondTransitionRequest() {
        if (!session.context.pendingManboTransition || session.context.activeRoom == null) {
            return null;
        }
        session.context.pendingManboTransition = false;
        return session.context.manboWarpResolver.resolve(session.context.activeRoom.mapCategory(), session.context.activeRoom.mapId());
    }

    Warp consumeInstrumentTransitionRequest() {
        if (!session.context.pendingInstrumentTransition || session.context.activeRoom == null || !session.context.activeRoom.hasWarps()) {
            return null;
        }
        session.context.pendingInstrumentTransition = false;
        return session.context.activeRoom.firstWarp();
    }

    Warp consumeClearedDungeonWarp() {
        Warp pending = session.context.pendingDungeonWarp;
        session.context.pendingDungeonWarp = null;
        return pending;
    }

    void finishOcarinaPlayback() {
        if (session.context.followingNpcState.marinFollowing()) {
            if (session.context.selectedSongIndex == 0x01) {
                session.context.pendingManboTransition = true;
            } else if (session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.context.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                    session.OCARINA_MARIN_DIALOG_TABLE, session.OCARINA_MARIN_DIALOG_ID));
            }
            return;
        }
        if (session.context.ocarinaSongFlags == 0) {
            session.context.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                session.OCARINA_NO_SONG_DIALOG_TABLE, session.OCARINA_NO_SONG_DIALOG_ID));
        } else if (session.context.selectedSongIndex == 0x01) {
            session.context.pendingManboTransition = true;
        }
    }

    void tickOcarinaAnimationHandler() {
        if (!session.ocarinaPlaying()) {
            return;
        }
        session.context.ocarinaAnimationCounter++;
        if (session.context.ocarinaAnimationCounter >= 0x38) {
            session.context.ocarinaAnimationCounter = 0;
            session.context.ocarinaAnimationPhase ^= 0x01;
        }
    }

    Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) {
        if (session.context.staircaseState == session.STAIRCASE_NONE || session.context.activeRoom == null) {
            return null;
        }
        if (session.context.staircaseState == session.STAIRCASE_INACTIVE) {
            if (!session.withinUnsignedByteRange(linkEntityX, session.context.staircaseX, 0x06, 0x0C)
                || !session.withinUnsignedByteRange(linkEntityY, session.context.staircaseY, 0x06, 0x0C)) {
                session.context.staircaseState = session.STAIRCASE_ACTIVE;
            }
            return null;
        }
        if (linkZ != 0 || carryingObject
            || !session.withinUnsignedByteRange(linkEntityX, session.context.staircaseX, 0x05, 0x0A)
            || !session.withinUnsignedByteRange(linkEntityY, session.context.staircaseY, 0x05, 0x0A)
            || !session.context.activeRoom.hasWarps()) {
            return null;
        }
        session.context.staircaseState = session.STAIRCASE_NONE;
        return session.context.activeRoom.firstWarp();
    }

    void configureStaircase(int location) {
        if (location < 0) {
            session.context.staircaseState = session.STAIRCASE_NONE;
            session.context.staircaseX = 0;
            session.context.staircaseY = 0;
            return;
        }
        session.context.staircaseState = session.STAIRCASE_INACTIVE;
        session.context.staircaseY = (location & 0xF0) + 0x10;
        session.context.staircaseX = ((location & 0x0F) << 4) + 0x08;
    }

    int adjacentOverworldRoomId(int direction) { return session.context.scrollNavigation.adjacentOverworldRoomId(session.context.activeRoom, direction); }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) { return session.context.scrollNavigation.adjacentIndoorRoom(session.context.activeRoom, session.context.indoorMapPosition, direction); }
}
