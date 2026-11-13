package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.STAIRCASE_NONE;

import linksawakening.vfx.TransientVfxSystem;

/** RoomTransientState operations for the live room session. */
final class RoomTransientState {
    private final Dependencies dependencies;

    RoomTransientState(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void clearTransientRoomState() {
        dependencies.puzzles().current.pendingShutterCloseMask =
                dependencies.puzzles().current.closedShutterMask = dependencies.puzzles().current.shutterCloseTicks = 0;
        dependencies.puzzles().current.shutterCloseDirection = dependencies.puzzles().current.shutterCloseLocation = -1;
        dependencies.puzzles().current.shutterCloseStarted = false;
        dependencies.puzzles().current.shutterTileOverrides.clear();
        if (dependencies.transientVfxSystem() != null) {
            dependencies.transientVfxSystem().clear();
        }
        if (dependencies.droppableRupeeSystem() != null) {
            dependencies.droppableRupeeSystem().clear();
        }
        dependencies.terrain().overrides.hookshotBridgeTileOverrides.clear();
        dependencies.terrain().overrides.bombedWallTileOverrides.clear();
        dependencies.terrain().overrides.bombedBlockTileOverrides.clear();
        dependencies.terrain().overrides.bombedCaveDoorTileOverrides.clear();
        dependencies.terrain().overrides.retainedTorchTiles.clear();
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
        dependencies.puzzles().current.roomTriggerCount = 0;
        dependencies.puzzles().current.activeRoomEvent = 0;
        dependencies.puzzles().current.roomEventEffectExecuted = false;
        dependencies.puzzles().current.thrownAtDoorTriggerResolved = false;
        dependencies.puzzles().current.roomEventChestRevealCountdown = 0;
        dependencies.puzzles().current.roomEventStairRevealCountdown = 0;
        dependencies.puzzles().current.staircaseState = STAIRCASE_NONE;
        dependencies.puzzles().current.staircaseX = 0;
        dependencies.puzzles().current.staircaseY = 0;
        dependencies.puzzles().current.switchButtonPressCounter = 0;
        dependencies.puzzles().current.indoorKeyDoorAnimationCountdown = 0;
        dependencies.puzzles().current.indoorKeyDoorDirection = -1;
        dependencies.puzzles().current.indoorKeyDoorLocation = -1;
        dependencies.puzzles().current.indoorBossDoorOpening = false;
        // ResetRoomVariables ($20:4CA3-4CE3) does not clear wC1C9 or
        // hLinkRoomPosition/hLinkFinalRoomPosition; transitions retain them.
        dependencies.puzzles().current.pushBlockContactTicks = 0;
        dependencies.puzzles().current.indoorPotPullFrames = 0;
        dependencies.puzzles().current.pushedBlockMotionFrames = 0;
        dependencies.puzzles().current.pushedBlockSourceLocation = -1;
        dependencies.puzzles().current.pushedBlockDestinationLocation = -1;
        dependencies.puzzles().current.pushedBlockDirection = -1;
        dependencies.puzzles().current.pushedBlockEntitySlot = -1;
        dependencies.puzzles().current.pushedBlocksTriggerEffectPending = false;
        dependencies.entityFrame().worldLinkMotionBlockPending = false;
        dependencies.puzzles().current.tailCaveKeyholeCountdown = 0;
        dependencies.puzzles().current.tailCaveFinalMotionBlockPending = false;
        dependencies.puzzles().current.nextWorldMusicTrackCountdown = 0;
        dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack = -1;
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomItemUseState items,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomTerrainState terrain,
            DroppableRupeeSystem droppableRupeeSystem,
            RoomEntitySessionState entityRuntimeState,
            TransientVfxSystem transientVfxSystem) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(items, "items");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(terrain, "terrain");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
        }
    }
}
