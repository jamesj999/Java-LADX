package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.physics.OverworldCollision;
import linksawakening.world.transition.Warp;

final class RoomGroundMutations {
    private final Dependencies dependencies;

    RoomGroundMutations(Dependencies dependencies) {
        this.dependencies = dependencies;
    }
    /**
     * ApplyLinkMotionState ($00:1794) calls func_002_753A after XY movement.
     * $02:7587-7589 copies the last GetObjectUnderLink result, not this
     * frame's pre-movement coordinates. Entity pushes, airborne frames and
     * skipped probes can make that older than one frame.
     */
    boolean tickCrackedFloor(RoomSession.LinkGroundFrame frame, boolean dialogGotItemActive) {

        if (dependencies.location().activeRoom == null) return false;
        if (dependencies.roomPlayerItemActions().get().hookshotActive()) {
            // $02:7549-754D takes func_002_755B: sample, but no $7589 copy
            // and no ApplyLinkGroundPhysics call.
            dependencies.owner().get().sampleLinkGround(frame.pixelX(), frame.pixelY());
            return false;
        }
        dependencies.terrain().linkGroundFinalRoomPosition = dependencies.terrain().linkGroundRoomPosition;
        // $02:7590-7593 and $75B6-75BB bypass ground physics.
        if (frame.airborne() || frame.motionState() == 0x02) return false;
        return dependencies.owner().get().applyCrackedFloorGroundPhysics(frame, dialogGotItemActive);
    }
    /** ApplyLinkGroundMotion's landing call ($02:4588), before XY and the $7589 copy. */
    boolean applyCrackedFloorGroundPhysics(RoomSession.LinkGroundFrame frame, boolean dialogGotItemActive) {

        if (dependencies.location().activeRoom == null) return false;
        boolean bypassEntry = frame.dialogActive() || frame.roomTransitionActive();
        if (!bypassEntry) {
            dependencies.owner().get().sampleLinkGround(frame.pixelX(), frame.pixelY()); // GetObjectUnderLink $02:75C7
            dependencies.terrain().linkGroundObjectPhysics = dependencies
                    .overworldCollision()
                    .objectPhysicsFlagAtPoint((frame.pixelX() + 8) & 0xFF, (frame.pixelY() + 12) & 0xFF);
            // $02:75D2-75D4/$75DA-75E5 bypass Default for these hazards.
            if ((!dependencies.overworldCollision().usesIndoorPhysicsTable()
                            && dependencies.terrain().linkGroundObjectUnderEntity == 0x61)
                    || (dependencies.overworldCollision().usesIndoorPhysicsTable()
                            && dependencies.terrain().linkGroundObjectUnderEntity == 0x4C
                            && (((frame.pixelY() + 15) & 15) >= 12))) return false;
        }
        if (!RoomObjectMutations.reachesDefaultGroundPhysics(
                dependencies.terrain().linkGroundObjectPhysics,
                bypassEntry,
                frame.slowWalkingSpeed(),
                frame.itemUsageContext())) return false;
        // Default can return at $02:77E8 while standing on a raised switch block.
        if (linksawakening.world.object.SwitchBlockLinkInteraction.marksStanding(
                dependencies.terrain().linkGroundObjectUnderEntity,
                dependencies.terrain().linkGroundObjectPhysics,
                dependencies.terrain().switchBlocksState)) return false;
        // $02:77F7-77FB gates on wIsIndoor, NOT hIsSideScrolling. Overworld
        // $DF has solid physics ($01), but still exits here without resetting.
        if (!dependencies.overworldCollision().usesIndoorPhysicsTable()) return false;
        // $02:75C4 -> 76C0 -> 7757 can re-enter Default using cached physics.
        // RoomTransitions retain wC1C9 at $02:7802; dialogs reaching $7858 reset it.
        if (frame.roomTransitionActive()) return false;
        // $02:7804-783B returns while an unpressed floor button is sinking.
        if (dependencies.terrain().linkGroundObjectUnderEntity == 0xAA
                && dependencies.puzzles().switchButtonPressed == 0) return false;
        if (dependencies.terrain().linkGroundRoomPosition != dependencies.terrain().linkGroundFinalRoomPosition
                || dependencies.terrain().linkGroundObjectUnderEntity != 0xDF
                || frame.interactionBlocked()
                || dialogGotItemActive
                || frame.dialogActive()) {
            dependencies.terrain().crackedFloorCounter = 0; // $02:7851-785D/$786C
            return false;
        }
        dependencies.terrain().crackedFloorCounter = (dependencies.terrain().crackedFloorCounter + 1) & 0xFF;
        if (dependencies.terrain().crackedFloorCounter < 0x28) return false;
        // label_002_4D97 writes $AE and redraws its tiles and GBC attributes.
        dependencies.location()
                .activeRoom
                .roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + dependencies.terrain().linkGroundRoomPosition] = 0xAE;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .entityFrame()
                .colorShellSoundSink
                .play(linksawakening.gameplay.GameplaySoundEvent.CRACKED_FLOOR_COLLAPSE);
        return true;
    }
    /** GetObjectUnderLink $02:7512-7536, translated from ROM center/bottom to Java top-left. */
    void sampleLinkGround(int pixelX, int pixelY) {
        int location = RoomObjectMutations.linkGroundLocation(pixelX, pixelY);
        dependencies.terrain().linkGroundRoomPosition = location;
        dependencies.terrain().linkGroundObjectUnderEntity =
                dependencies.roomSessionViews().get().objectAtRoomLocation(location);
    }
    /** CheckStaticSwordCollision $00:161A-1679; RevealObjectUnderObject $14:5584-55E3. */
    boolean breakIndoorSwordCrystal(int location) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || location < 0
                || (location & 15) >= 10
                || (location >> 4) >= 8
                || dependencies.roomSessionViews().get().objectAtRoomLocation(location) != 0xDD) return false;
        dependencies.location().activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + location] = 0x0D;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        if (dependencies.entityBridge().get().entityRuntime != null) {
            // Variant zero uses LiftableRockStartSmashingAnimation ($03:53E4):
            // fifteen frames of rock debris and NOISE_SFX_POT_SMASHED ($09).
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .spawnLiftableRockSmash(((location & 15) << 4) + 8, (location & 0xF0) + 16, 0);
            // Link runs before the entity tick, which clears its frame outbox.
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomEntityEvents
                    .addAll(dependencies.entityBridge().get().entityRuntime.consumePendingEntityEvents());
        }
        return true;
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomPlayerItemActions> roomPlayerItemActions,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            RoomTerrainState terrain,
            java.util.function.Supplier<RoomObjectMutations> owner) {}
}
