package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.world.entity.EnemyProjectileCollision;

final class RoomBlockMotion {
    private final Dependencies dependencies;

    RoomBlockMotion(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) {
        if (dependencies.puzzles().pushedBlockMotionFrames == 0 || dependencies.location().activeRoom == null) {
            return;
        }
        if (dependencies.entityInputs().get().entityDialogActive
                || dependencies.entityInputs().get().entityInventoryAppearing
                || dependencies.entityInputs().get().entityDialogCooldown != 0) {
            return;
        }
        dependencies.puzzles().pushedBlockMotionFrames--;
        int elapsedFrames = PUSHED_BLOCK_MOTION_FRAMES - dependencies.puzzles().pushedBlockMotionFrames;
        int distance = elapsedFrames >>> 1;
        int sourceX = ((dependencies.puzzles().pushedBlockSourceLocation & 0x0F) << 4) + 0x08;
        int sourceY = (dependencies.puzzles().pushedBlockSourceLocation & 0xF0) + 0x10;
        int x = sourceX;
        int y = sourceY;
        switch (dependencies.puzzles().pushedBlockDirection) {
            case 0 -> y -= distance;
            case 1 -> y += distance;
            case 2 -> x -= distance;
            case 3 -> x += distance;
            default -> {
                dependencies.puzzles().pushedBlockMotionFrames = 0;
                return;
            }
        }
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .movePushedBlock(dependencies.puzzles().pushedBlockEntitySlot, x, y);
            if (dependencies.entityFrame().currentLinkMotionState
                    < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .resolvePushedBlockLinkContact(
                                dependencies.puzzles().pushedBlockEntitySlot, linkEntityX, linkEntityY);
            }
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .view());
        }
        if (dependencies.puzzles().pushedBlockMotionFrames != 0) {
            return;
        }
        finishPushedBlock();
    }

    void tickIndoorKeyDoorAnimation() {
        if (dependencies.puzzles().indoorKeyDoorAnimationCountdown == 0
                || dependencies.puzzles().indoorKeyDoorDirection < 0
                || dependencies.location().activeRoom == null) {
            return;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        dependencies.puzzles().indoorKeyDoorAnimationCountdown--;
        if (dependencies.puzzles().indoorKeyDoorAnimationCountdown != 0) {
            return;
        }

        int direction = dependencies.puzzles().indoorKeyDoorDirection;
        dependencies.puzzles().indoorKeyDoorDirection = -1;
        if (dependencies.puzzles().indoorBossDoorOpening) {
            dependencies
                    .roomPuzzleController()
                    .get()
                    .replaceBossDoorObjects(dependencies.puzzles().indoorKeyDoorLocation);
            dependencies.puzzles().indoorBossDoorOpening = false;
        } else {
            dependencies
                    .roomPuzzleController()
                    .get()
                    .replaceKeyDoorObjects(direction, dependencies.puzzles().indoorKeyDoorLocation);
        }
        dependencies.puzzles().indoorKeyDoorLocation = -1;
        byte[] status = dependencies
                .roomSessionViews()
                .get()
                .indoorStatusTableForMap(dependencies.location().activeRoom.mapId());
        status[dependencies.location().activeRoom.roomId()] |= (byte) KEY_DOOR_CURRENT_STATUS[direction];
        int adjacentRoom = dependencies.roomSessionViews().get().adjacentIndoorRoomIdForBombWall(direction);
        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    void replaceKeyDoorObjects(int direction, int location) {
        int firstClosedObject = KEY_DOOR_CLOSED_OBJECTS[direction];
        int[] openObjects = KEY_DOOR_OPEN_OBJECTS[direction];
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        int secondLocation = location + (direction < 2 ? 1 : 0x10);
        int[] locations = {location, secondLocation};
        for (int objectOffset = 0; objectOffset < locations.length; objectOffset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + locations[objectOffset];
            if (areaIndex >= 0
                    && areaIndex < objects.length
                    && (objects[areaIndex] & 0xFF) == firstClosedObject + objectOffset) {
                objects[areaIndex] = openObjects[objectOffset];
                if (dependencies.location().activeRoom.renderValues() != null) {
                    dependencies.location().activeRoom.renderValues()[areaIndex] = openObjects[objectOffset];
                }
            }
        }
    }

    void replaceBossDoorObjects(int location) {
        int[] objects = dependencies.location().activeRoom.roomObjectsArea();
        int[] openObjects = KEY_DOOR_OPEN_OBJECTS[0];
        for (int offset = 0; offset < 2; offset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location + offset;
            if (areaIndex >= 0 && areaIndex < objects.length && objects[areaIndex] == 0xA4 + offset) {
                objects[areaIndex] = openObjects[offset];
                if (dependencies.location().activeRoom.renderValues() != null) {
                    dependencies.location().activeRoom.renderValues()[areaIndex] = openObjects[offset];
                }
            }
        }
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {}

    private void finishPushedBlock() {
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE + dependencies.puzzles().pushedBlockDestinationLocation;
        if (destinationIndex >= 0
                && destinationIndex < dependencies.location().activeRoom.roomObjectsArea().length) {
            dependencies.location().activeRoom.roomObjectsArea()[destinationIndex] = OBJECT_SETTLED_PUSHED_BLOCK;
        }
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .removePushedBlock(dependencies.puzzles().pushedBlockEntitySlot);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .view());
        }
        dependencies.puzzles().pushedBlockEntitySlot = -1;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        if ((dependencies.puzzles().activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_SINGLE_BLOCK) {
            dependencies.puzzles().roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        } else if ((dependencies.puzzles().activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_BLOCKS
                && dependencies.roomPuzzleController().get().pushedBlocksTriggerResolved()) {
            dependencies.puzzles().roomEventEffectExecuted = true;
            dependencies.puzzles().pushedBlocksTriggerEffectPending = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
    }
}
