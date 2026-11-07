package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

/** RoomTransientState operations for the live room session. */
final class RoomTransientState {
    private final RoomSession session;
    RoomTransientState(RoomSession session) { this.session = session; }

    void clearTransientRoomState() {
        session.context.pendingShutterCloseMask = session.context.closedShutterMask = session.context.shutterCloseTicks = 0;
        session.context.shutterCloseDirection = session.context.shutterCloseLocation = -1;
        session.context.shutterCloseStarted = false;
        session.context.shutterTileOverrides.clear();
        if (session.context.transientVfxSystem != null) {
            session.context.transientVfxSystem.clear();
        }
        if (session.context.droppableRupeeSystem != null) {
            session.context.droppableRupeeSystem.clear();
        }
        session.context.hookshotBridgeTileOverrides.clear();
        session.context.bombedWallTileOverrides.clear();
        session.context.bombedBlockTileOverrides.clear();
        session.context.bombedCaveDoorTileOverrides.clear();
        session.context.retainedTorchTiles.clear();
        session.context.entityBridge.pendingBombExplosionEvents.clear();
        session.context.pendingDungeonWarp = null;
        session.context.entityBridge.pendingRoomEntityEvents.clear();
        session.context.entityBridge.pendingChestRewardEvents.clear();
        session.context.entityBridge.pendingKeyRewardEvents.clear();
        session.context.entityBridge.pendingSlimeKeyRewardEvents.clear();
        session.context.entityBridge.pendingHeartContainerRewards.clear();
        session.context.entityBridge.pendingMadamMeowMeowFullHealRequests.clear();
        session.context.entityBridge.pendingRichardProgressEvents.clear();
        session.context.entityBridge.pendingSwordPickupRewards.clear();
        session.context.entityBridge.pendingToadstoolRewards.clear();
        session.context.entityBridge.pendingWitchExchangeEvents.clear();
        session.context.entityBridge.pendingWitchRewardEvents.clear();
        session.context.entityBridge.pendingRoomDialogRequests.clear();
        session.context.pendingManboTransition = false;
        session.context.pendingInstrumentTransition = false;
        session.context.pendingShovelDrop = null;
        session.context.shovelUseState = 0;
        session.context.roomTriggerCount = 0;
        session.context.activeRoomEvent = 0;
        session.context.roomEventEffectExecuted = false;
        session.context.thrownAtDoorTriggerResolved = false;
        session.context.roomEventChestRevealCountdown = 0;
        session.context.roomEventStairRevealCountdown = 0;
        session.context.staircaseState = session.STAIRCASE_NONE;
        session.context.staircaseX = 0;
        session.context.staircaseY = 0;
        session.context.switchButtonPressCounter = 0;
        session.context.indoorKeyDoorAnimationCountdown = 0;
        session.context.indoorKeyDoorDirection = -1;
        session.context.indoorKeyDoorLocation = -1;
        session.context.indoorBossDoorOpening = false;
        session.context.pushBlockContactTicks = 0;
        session.context.indoorPotPullFrames = 0;
        session.context.pushedBlockMotionFrames = 0;
        session.context.pushedBlockSourceLocation = -1;
        session.context.pushedBlockDestinationLocation = -1;
        session.context.pushedBlockDirection = -1;
        session.context.pushedBlockEntitySlot = -1;
        session.context.pushedBlocksTriggerEffectPending = false;
        session.context.worldLinkMotionBlockPending = false;
        session.context.tailCaveKeyholeCountdown = 0;
        session.context.tailCaveFinalMotionBlockPending = false;
        session.context.nextWorldMusicTrackCountdown = 0;
        session.context.entityBridge.pendingRoomMusicTrack = -1;
    }
}
