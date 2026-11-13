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
        dependencies.puzzles().pendingShutterCloseMask =
                dependencies.puzzles().closedShutterMask = dependencies.puzzles().shutterCloseTicks = 0;
        dependencies.puzzles().shutterCloseDirection = dependencies.puzzles().shutterCloseLocation = -1;
        dependencies.puzzles().shutterCloseStarted = false;
        dependencies.puzzles().shutterTileOverrides.clear();
        if (dependencies.transientVfxSystem() != null) {
            dependencies.transientVfxSystem().clear();
        }
        if (dependencies.droppableRupeeSystem() != null) {
            dependencies.droppableRupeeSystem().clear();
        }
        dependencies.terrain().hookshotBridgeTileOverrides.clear();
        dependencies.terrain().bombedWallTileOverrides.clear();
        dependencies.terrain().bombedBlockTileOverrides.clear();
        dependencies.terrain().bombedCaveDoorTileOverrides.clear();
        dependencies.terrain().retainedTorchTiles.clear();
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
        dependencies.puzzles().roomTriggerCount = 0;
        dependencies.puzzles().activeRoomEvent = 0;
        dependencies.puzzles().roomEventEffectExecuted = false;
        dependencies.puzzles().thrownAtDoorTriggerResolved = false;
        dependencies.puzzles().roomEventChestRevealCountdown = 0;
        dependencies.puzzles().roomEventStairRevealCountdown = 0;
        dependencies.puzzles().staircaseState = STAIRCASE_NONE;
        dependencies.puzzles().staircaseX = 0;
        dependencies.puzzles().staircaseY = 0;
        dependencies.puzzles().switchButtonPressCounter = 0;
        dependencies.puzzles().indoorKeyDoorAnimationCountdown = 0;
        dependencies.puzzles().indoorKeyDoorDirection = -1;
        dependencies.puzzles().indoorKeyDoorLocation = -1;
        dependencies.puzzles().indoorBossDoorOpening = false;
        // ResetRoomVariables ($20:4CA3-4CE3) does not clear wC1C9 or
        // hLinkRoomPosition/hLinkFinalRoomPosition; transitions retain them.
        dependencies.puzzles().pushBlockContactTicks = 0;
        dependencies.puzzles().indoorPotPullFrames = 0;
        dependencies.puzzles().pushedBlockMotionFrames = 0;
        dependencies.puzzles().pushedBlockSourceLocation = -1;
        dependencies.puzzles().pushedBlockDestinationLocation = -1;
        dependencies.puzzles().pushedBlockDirection = -1;
        dependencies.puzzles().pushedBlockEntitySlot = -1;
        dependencies.puzzles().pushedBlocksTriggerEffectPending = false;
        dependencies.entityFrame().worldLinkMotionBlockPending = false;
        dependencies.puzzles().tailCaveKeyholeCountdown = 0;
        dependencies.puzzles().tailCaveFinalMotionBlockPending = false;
        dependencies.puzzles().nextWorldMusicTrackCountdown = 0;
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
