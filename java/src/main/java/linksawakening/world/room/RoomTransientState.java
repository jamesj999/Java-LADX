package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.STAIRCASE_NONE;

import java.util.Objects;
import linksawakening.vfx.TransientVfxSystem;

/** RoomTransientState operations for the live room session. */
final class RoomTransientState {
    private final Dependencies dependencies;

    RoomTransientState(Dependencies dependencies) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies");
    }

    void clearTransientRoomState() {

        dependencies.transientVfxSystem().clear();

        dependencies.droppableRupeeSystem().clear();

        dependencies.entityRuntimeState().outbox.pendingBombExplosionEvents.clear();
        dependencies.location().pendingDungeonWarp = null;
        dependencies.entityRuntimeState().outbox.pendingRoomEntityEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingChestRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingKeyRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingSlimeKeyRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingHeartContainerRewards.clear();
        dependencies
                .entityRuntimeState()
                .outbox
                .pendingMadamMeowMeowFullHealRequests
                .clear();
        dependencies.entityRuntimeState().outbox.pendingRichardProgressEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingSwordPickupRewards.clear();
        dependencies.entityRuntimeState().outbox.pendingToadstoolRewards.clear();
        dependencies.entityRuntimeState().outbox.pendingWitchExchangeEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingWitchRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingRoomDialogRequests.clear();
        dependencies.items().pendingManboTransition = false;
        dependencies.items().pendingInstrumentTransition = false;
        dependencies.items().pendingShovelDrop = null;
        dependencies.items().shovelUseState = 0;
        // ResetRoomVariables ($20:4CA3-4CE3) does not clear wC1C9 or
        // hLinkRoomPosition/hLinkFinalRoomPosition; transitions retain them.
        dependencies.entityFrame().worldLinkMotionBlockPending = false;
        dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack = -1;
    }

    void clearTransientRoomState(RoomContents contents) {
        contents.puzzles.pendingShutterCloseMask =
                contents.puzzles.closedShutterMask = contents.puzzles.shutterCloseTicks = 0;
        contents.puzzles.shutterCloseDirection = contents.puzzles.shutterCloseLocation = -1;
        contents.puzzles.shutterCloseStarted = false;
        contents.puzzles.shutterTileOverrides.clear();
        dependencies.transientVfxSystem().clear();
        dependencies.droppableRupeeSystem().clear();
        contents.overrides.hookshotBridgeTileOverrides.clear();
        contents.overrides.bombedWallTileOverrides.clear();
        contents.overrides.bombedBlockTileOverrides.clear();
        contents.overrides.bombedCaveDoorTileOverrides.clear();
        contents.overrides.retainedTorchTiles.clear();
        dependencies.entityRuntimeState().outbox.pendingBombExplosionEvents.clear();
        dependencies.location().pendingDungeonWarp = null;
        dependencies.entityRuntimeState().outbox.pendingRoomEntityEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingChestRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingKeyRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingSlimeKeyRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingHeartContainerRewards.clear();
        dependencies
                .entityRuntimeState()
                .outbox
                .pendingMadamMeowMeowFullHealRequests
                .clear();
        dependencies.entityRuntimeState().outbox.pendingRichardProgressEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingSwordPickupRewards.clear();
        dependencies.entityRuntimeState().outbox.pendingToadstoolRewards.clear();
        dependencies.entityRuntimeState().outbox.pendingWitchExchangeEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingWitchRewardEvents.clear();
        dependencies.entityRuntimeState().outbox.pendingRoomDialogRequests.clear();
        dependencies.items().pendingManboTransition = false;
        dependencies.items().pendingInstrumentTransition = false;
        dependencies.items().pendingShovelDrop = null;
        dependencies.items().shovelUseState = 0;
        contents.puzzles.roomTriggerCount = 0;
        contents.puzzles.activeRoomEvent = 0;
        contents.puzzles.roomEventEffectExecuted = false;
        contents.puzzles.thrownAtDoorTriggerResolved = false;
        contents.puzzles.roomEventChestRevealCountdown = 0;
        contents.puzzles.roomEventStairRevealCountdown = 0;
        contents.puzzles.staircaseState = STAIRCASE_NONE;
        contents.puzzles.staircaseX = 0;
        contents.puzzles.staircaseY = 0;
        contents.puzzles.switchButtonPressCounter = 0;
        contents.puzzles.indoorKeyDoorAnimationCountdown = 0;
        contents.puzzles.indoorKeyDoorDirection = -1;
        contents.puzzles.indoorKeyDoorLocation = -1;
        contents.puzzles.indoorBossDoorOpening = false;
        // ResetRoomVariables ($20:4CA3-4CE3) does not clear wC1C9 or
        // hLinkRoomPosition/hLinkFinalRoomPosition; transitions retain them.
        contents.puzzles.pushBlockContactTicks = 0;
        contents.puzzles.indoorPotPullFrames = 0;
        contents.puzzles.pushedBlockMotionFrames = 0;
        contents.puzzles.pushedBlockSourceLocation = -1;
        contents.puzzles.pushedBlockDestinationLocation = -1;
        contents.puzzles.pushedBlockDirection = -1;
        contents.puzzles.pushedBlockEntitySlot = -1;
        contents.puzzles.pushedBlocksTriggerEffectPending = false;
        dependencies.entityFrame().worldLinkMotionBlockPending = false;
        contents.puzzles.tailCaveKeyholeCountdown = 0;
        contents.puzzles.tailCaveFinalMotionBlockPending = false;
        contents.puzzles.nextWorldMusicTrackCountdown = 0;
        dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack = -1;
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomItemUseState items,
            RoomLocationState location,
            DroppableRupeeSystem droppableRupeeSystem,
            RoomEntitySessionState entityRuntimeState,
            TransientVfxSystem transientVfxSystem) {
        Dependencies {
            Objects.requireNonNull(transientVfxSystem, "transientVfxSystem");
            Objects.requireNonNull(droppableRupeeSystem, "droppableRupeeSystem");
            Objects.requireNonNull(entityFrame, "entityFrame");
            Objects.requireNonNull(items, "items");
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
        }
    }
}
