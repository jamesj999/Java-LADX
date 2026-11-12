package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.transition.Warp;

final class RoomPuzzleReveals {
    private final Dependencies dependencies;

    RoomPuzzleReveals(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void tickSwitchButton(int linkEntityX, int linkEntityY) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.puzzles().switchButtonPressed != 0) {
            return;
        }
        int objectUnderLink = dependencies
                .overworldCollision()
                .groundInteractionSample(linkEntityX, linkEntityY)
                .objectId();
        if (objectUnderLink != OBJECT_SWITCH_BUTTON) {
            dependencies.puzzles().switchButtonPressCounter = 0;
            return;
        }
        dependencies.puzzles().switchButtonPressCounter = (dependencies.puzzles().switchButtonPressCounter + 1) & 0xFF;
        if (dependencies.puzzles().switchButtonPressCounter == SWITCH_BUTTON_PRESS_FRAMES) {
            dependencies.puzzles().switchButtonPressed = SWITCH_BUTTON_PRESSED;
            dependencies.gpu().copyPressedFloorSwitchTiles(dependencies.romData());
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.FLOOR_SWITCH);
        }
    }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) {
        if (dependencies.puzzles().roomEventChestRevealCountdown == 0) {
            return;
        }
        dependencies.puzzles().roomEventChestRevealCountdown--;
        if (dependencies.puzzles().roomEventChestRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int location = RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) | 0x08;
        dependencies.roomObjectMutations().get().writeBombPuzzleObject(location, 0xA0);
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
    }

    void tickRoomEventStairReveal() {
        if (dependencies.puzzles().roomEventStairRevealCountdown == 0) {
            return;
        }
        dependencies.puzzles().roomEventStairRevealCountdown--;
        if (dependencies.puzzles().roomEventStairRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + EVENT_STAIRWAY_LOCATION;
        dependencies.location().activeRoom.roomObjectsArea()[areaIndex] = OBJECT_STAIRS_DOWN;
        dependencies.roomTransitionController().get().configureStaircase(EVENT_STAIRWAY_LOCATION);
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            GPU gpu,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            byte[] romData,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController) {}
}
