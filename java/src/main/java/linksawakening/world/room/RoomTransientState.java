package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

/** RoomTransientState operations for the live room session. */
final class RoomTransientState {
    private final RoomSession session;

    RoomTransientState(RoomSession session) {
        this.session = session;
    }

    void clearTransientRoomState() {
        session.context.pendingShutterCloseMask =
                session.context.closedShutterMask = session.context.shutterCloseTicks = 0;
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
        session.context.entityBridge.outbox.pendingBombExplosionEvents.clear();
        session.context.pendingDungeonWarp = null;
        session.context.entityBridge.outbox.pendingRoomEntityEvents.clear();
        session.context.entityBridge.outbox.pendingChestRewardEvents.clear();
        session.context.entityBridge.outbox.pendingKeyRewardEvents.clear();
        session.context.entityBridge.outbox.pendingSlimeKeyRewardEvents.clear();
        session.context.entityBridge.outbox.pendingHeartContainerRewards.clear();
        session.context.entityBridge.outbox.pendingMadamMeowMeowFullHealRequests.clear();
        session.context.entityBridge.outbox.pendingRichardProgressEvents.clear();
        session.context.entityBridge.outbox.pendingSwordPickupRewards.clear();
        session.context.entityBridge.outbox.pendingToadstoolRewards.clear();
        session.context.entityBridge.outbox.pendingWitchExchangeEvents.clear();
        session.context.entityBridge.outbox.pendingWitchRewardEvents.clear();
        session.context.entityBridge.outbox.pendingRoomDialogRequests.clear();
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
        // ResetRoomVariables ($20:4CA3-4CE3) does not clear wC1C9 or
        // hLinkRoomPosition/hLinkFinalRoomPosition; transitions retain them.
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
        session.context.entityBridge.outbox.pendingRoomMusicTrack = -1;
    }
}
