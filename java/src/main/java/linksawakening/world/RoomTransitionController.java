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

/** RoomTransitionController operations for the live room session. */
final class RoomTransitionController {
    private final RoomSession session;
    RoomTransitionController(RoomSession session) { this.session = session; }

    void loadWarpDestination(Warp warp) {
        // GameplayWorldLoadRoomHandler ($01:442B) clears $C000..$D2FF,
        // including the recent-room ring and transient entity-clear masks.
        // Scrolling between adjacent rooms does not take this path.
        session.recentRoomEntityClears.reset();
        boolean wasIndoor = session.activeRoom != null && session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD;
        session.dungeonTitleMessageCountdown = 0;
        if (warp.category() == Warp.CATEGORY_OVERWORLD) {
            if (wasIndoor) {
                session.gpu.loadBaseOverworldTiles(session.romData);
                session.currentOverworldTilesetId = session.W_TILESET_NO_UPDATE;
            }
            session.loadOverworld(warp.destRoom());
            session.loadRoomSpecificTilesIfNeeded(warp.destRoom());
        } else {
            session.loadIndoor(warp.destMap(), warp.destRoom(), warp.category());
            // $00:194C-1965: old wIsIndoor is hFreeWarpDataAddress.
            if (!wasIndoor && warp.destMap() < 0x0A) session.dungeonTitleMessageCountdown = 0x30;
        }
    }

    int tickDungeonTitleMessage(boolean dialogActive, boolean inventoryClosed, int linkMotionState, boolean freeMovement) {
        if (session.dungeonTitleMessageCountdown == 0 || dialogActive || !inventoryClosed) return -1;
        if (--session.dungeonTitleMessageCountdown != 0 || linkMotionState != 0 || freeMovement) return -1;
        return 0x56 + session.activeRoom.mapId();
    }

    void startAdjacentOverworldScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = session.renderSnapshot();
        int nextRoomId = session.adjacentOverworldRoomId(direction);
        boolean discoveredTailKeyRoom = nextRoomId == session.ROOM_OW_TAIL_KEY_CHEST
            && direction == ScrollController.UP
            && (session.roomState.overworldRoomStatus[nextRoomId] & session.ROOM_STATUS_DISCOVERED) == 0;
        if (discoveredTailKeyRoom) {
            session.roomState.overworldRoomStatus[nextRoomId] |= (byte) session.ROOM_STATUS_DISCOVERED;
        }
        session.loadRoomSpecificTilesIfNeeded(nextRoomId);
        session.loadOverworld(nextRoomId);
        if (discoveredTailKeyRoom) {
            session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
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
        session.entityBridge.pendingRoomEntityEvents.add(new EntityCombatEvent(
            0, session.ENTITY_TARIN, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1E));
    }

    void startAdjacentIndoorScroll(ScrollController scrollController, int direction, int linkScreenX, int linkScreenY) {
        RoomRenderSnapshot previousRoom = session.renderSnapshot();
        RoomScrollNavigation.IndoorRoomDestination destination = session.adjacentIndoorRoom(direction);
        session.loadIndoor(session.activeRoom.mapId(), destination.roomId(), session.activeRoom.mapCategory(),
            switch (direction) {
                case ScrollController.RIGHT -> 0;
                case ScrollController.LEFT -> 1;
                case ScrollController.UP -> 2;
                case ScrollController.DOWN -> 3;
                default -> throw new IllegalArgumentException("Indoor scroll direction " + direction);
            });
        session.indoorMapPosition = destination.mapPosition();
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
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null || session.ocarinaPlaybackCountdown != 0) {
            return false;
        }
        session.ocarinaPlaybackCountdown = countdown;
        session.ocarinaSongFlags = songFlags & 0xFF;
        session.selectedSongIndex = selectedSong & 0xFF;
        session.ocarinaAnimationCounter = 0;
        session.ocarinaAnimationPhase = 0;
        session.pendingManboTransition = false;
        session.pendingInstrumentTransition = false;
        return true;
    }

    boolean ocarinaPlaying() {
        return session.ocarinaPlaybackCountdown != 0;
    }

    int ocarinaAnimationState() {
        if (!session.ocarinaPlaying()) {
            return -1;
        }
        return session.ocarinaAnimationPhase == 0 ? 0x76 : 0x75;
    }

    Warp consumeManboPondTransitionRequest() {
        if (!session.pendingManboTransition || session.activeRoom == null) {
            return null;
        }
        session.pendingManboTransition = false;
        return session.manboWarpResolver.resolve(session.activeRoom.mapCategory(), session.activeRoom.mapId());
    }

    Warp consumeInstrumentTransitionRequest() {
        if (!session.pendingInstrumentTransition || session.activeRoom == null || !session.activeRoom.hasWarps()) {
            return null;
        }
        session.pendingInstrumentTransition = false;
        return session.activeRoom.firstWarp();
    }

    Warp consumeClearedDungeonWarp() {
        Warp pending = session.pendingDungeonWarp;
        session.pendingDungeonWarp = null;
        return pending;
    }

    void finishOcarinaPlayback() {
        if (session.followingNpcState.marinFollowing()) {
            if (session.selectedSongIndex == 0x01) {
                session.pendingManboTransition = true;
            } else if (session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD) {
                session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                    session.OCARINA_MARIN_DIALOG_TABLE, session.OCARINA_MARIN_DIALOG_ID));
            }
            return;
        }
        if (session.ocarinaSongFlags == 0) {
            session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                session.OCARINA_NO_SONG_DIALOG_TABLE, session.OCARINA_NO_SONG_DIALOG_ID));
        } else if (session.selectedSongIndex == 0x01) {
            session.pendingManboTransition = true;
        }
    }

    void tickOcarinaAnimationHandler() {
        if (!session.ocarinaPlaying()) {
            return;
        }
        session.ocarinaAnimationCounter++;
        if (session.ocarinaAnimationCounter >= 0x38) {
            session.ocarinaAnimationCounter = 0;
            session.ocarinaAnimationPhase ^= 0x01;
        }
    }

    Warp pollStaircaseWarp(int linkEntityX, int linkEntityY, int linkZ, boolean carryingObject) {
        if (session.staircaseState == session.STAIRCASE_NONE || session.activeRoom == null) {
            return null;
        }
        if (session.staircaseState == session.STAIRCASE_INACTIVE) {
            if (!session.withinUnsignedByteRange(linkEntityX, session.staircaseX, 0x06, 0x0C)
                || !session.withinUnsignedByteRange(linkEntityY, session.staircaseY, 0x06, 0x0C)) {
                session.staircaseState = session.STAIRCASE_ACTIVE;
            }
            return null;
        }
        if (linkZ != 0 || carryingObject
            || !session.withinUnsignedByteRange(linkEntityX, session.staircaseX, 0x05, 0x0A)
            || !session.withinUnsignedByteRange(linkEntityY, session.staircaseY, 0x05, 0x0A)
            || !session.activeRoom.hasWarps()) {
            return null;
        }
        session.staircaseState = session.STAIRCASE_NONE;
        return session.activeRoom.firstWarp();
    }

    void configureStaircase(int location) {
        if (location < 0) {
            session.staircaseState = session.STAIRCASE_NONE;
            session.staircaseX = 0;
            session.staircaseY = 0;
            return;
        }
        session.staircaseState = session.STAIRCASE_INACTIVE;
        session.staircaseY = (location & 0xF0) + 0x10;
        session.staircaseX = ((location & 0x0F) << 4) + 0x08;
    }

    int adjacentOverworldRoomId(int direction) { return session.scrollNavigation.adjacentOverworldRoomId(session.activeRoom, direction); }

    RoomScrollNavigation.IndoorRoomDestination adjacentIndoorRoom(int direction) { return session.scrollNavigation.adjacentIndoorRoom(session.activeRoom, session.indoorMapPosition, direction); }
}
