package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.transition.Warp;

final class RoomIndoorDoorInteractions {
    private final Dependencies dependencies;

    RoomIndoorDoorInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || dependencies.puzzles().indoorKeyDoorAnimationCountdown != 0) {
            return false;
        }
        int doorDirection = RoomInteractionGeometry.keyDoorDirectionIndex(linkDirection);
        if (doorDirection < 0 || (collisionType & RoomInteractionGeometry.keyDoorCollisionBit(doorDirection)) == 0) {
            return false;
        }
        int expectedPhysics = KEY_DOOR_PHYSICS_BASE + doorDirection;
        int touchedLocation = -1;
        boolean bossDoor = false;
        for (int[] point : RoomInteractionGeometry.keyDoorCollisionPoints(linkPixelX, linkPixelY, doorDirection)) {
            int physics = dependencies.overworldCollision().objectPhysicsFlagAtPoint(point[0], point[1]);
            if (physics == expectedPhysics || (doorDirection == 0 && physics == 0x98)) {
                int column = Math.floorDiv(point[0], 0x10);
                int row = Math.floorDiv(point[1], 0x10);
                touchedLocation = (row << 4) | column;
                bossDoor = physics == 0x98;
                break;
            }
        }
        if (touchedLocation < 0) {
            return false;
        }
        int touchedObject = dependencies.roomSessionViews().get().objectAtRoomLocation(touchedLocation);
        if (bossDoor) {
            int objectOffset = touchedObject - 0xA4;
            if (objectOffset < 0
                    || objectOffset > 1
                    || !RoomInteractionGeometry.alignedWithIndoorKeyDoor(
                            touchedLocation - objectOffset, 0, linkPixelX, linkPixelY)) {
                return false;
            }
            if (dependencies.roomState().dungeonItemState.currentFlag(DungeonItemState.NIGHTMARE_KEY_INDEX) == 0) {
                dependencies
                        .entityBridge()
                        .get()
                        .outbox
                        .pendingRoomDialogRequests
                        .add(new DialogRequest(0, 0x07));
                return true;
            }
            dependencies.puzzles().indoorBossDoorOpening = true;
            dependencies.puzzles().indoorKeyDoorDirection = 0;
            dependencies.puzzles().indoorKeyDoorLocation = touchedLocation - objectOffset;
            dependencies.puzzles().indoorKeyDoorAnimationCountdown = KEY_DOOR_ANIMATION_FRAMES;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
            return true;
        }
        int objectOffset = touchedObject - KEY_DOOR_CLOSED_OBJECTS[doorDirection];
        if (objectOffset < 0 || objectOffset > 1) return false;
        int doorLocation = touchedLocation - (objectOffset == 0 ? 0 : doorDirection < 2 ? 1 : 0x10);
        if (!RoomInteractionGeometry.alignedWithIndoorKeyDoor(doorLocation, doorDirection, linkPixelX, linkPixelY)
                || !dependencies.roomState().dungeonItemState.consumeSmallKey()) return false;
        dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        dependencies.puzzles().indoorKeyDoorDirection = doorDirection;
        dependencies.puzzles().indoorKeyDoorLocation = doorLocation;
        dependencies.puzzles().indoorKeyDoorAnimationCountdown = KEY_DOOR_ANIMATION_FRAMES;
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        return true;
    }

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || dependencies.puzzles().pushedBlockMotionFrames != 0) {
            dependencies.puzzles().pushBlockContactTicks = 0;
            return false;
        }
        int direction = RoomInteractionGeometry.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & RoomInteractionGeometry.keyDoorCollisionBit(direction)) == 0) {
            dependencies.puzzles().pushBlockContactTicks = 0;
            return false;
        }
        var contact = probePushableBlockContact(linkPixelX, linkPixelY, direction);
        int touchedLocation = contact.location();
        boolean keyholeBlockContact = contact.keyhole();
        if (touchedLocation < 0) {
            if (!keyholeBlockContact) {
                dependencies.puzzles().pushBlockContactTicks = 0;
            }
            return false;
        }
        dependencies.puzzles().pushBlockContactTicks++;
        if (dependencies.puzzles().pushBlockContactTicks < PUSH_BLOCK_CONTACT_TICKS) {
            return true;
        }
        dependencies.puzzles().pushBlockContactTicks = 0;
        int destination = touchedLocation
                + switch (direction) {
                    case 0 -> -0x10;
                    case 1 -> 0x10;
                    case 2 -> -1;
                    case 3 -> 1;
                    default -> 0;
                };
        int sourceIndex = RoomConstants.ROOM_OBJECTS_BASE + touchedLocation;
        int destinationX = ((destination & 0x0F) << 4) + 0x08;
        int destinationY = (destination & 0xF0) + 0x10;
        if (PhysicsFlags.blocksWalking(
                dependencies.overworldCollision().objectPhysicsFlagAtEntityPosition(destinationX, destinationY))) {
            return true;
        }
        int sourceX = ((touchedLocation & 0x0F) << 4) + 0x08;
        int sourceY = (touchedLocation & 0xF0) + 0x10;
        int spawnedSlot = dependencies.entityBridge().get().entityRuntime == null
                ? -1
                : dependencies.entityBridge().get().entityRuntime.spawnPushedBlock(sourceX, sourceY);
        if (spawnedSlot < 0) {
            return true;
        }
        // EntityInitPushedBlock $03:5198-519A, after collision/allocation succeed.
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.BLOCK_PUSH);
        dependencies.location().activeRoom.roomObjectsArea()[sourceIndex] = OBJECT_FLOOR_OD;
        dependencies.puzzles().pushedBlockSourceLocation = touchedLocation;
        dependencies.puzzles().pushedBlockDestinationLocation = destination;
        dependencies.puzzles().pushedBlockDirection = direction;
        dependencies.puzzles().pushedBlockMotionFrames = PUSHED_BLOCK_MOTION_FRAMES;
        dependencies.puzzles().pushedBlockEntitySlot = spawnedSlot;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        if (dependencies.entityBridge().get().entityRuntime != null) {
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .snapshot());
        }
        return true;
    }

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (dependencies
                .roomDoorInteractions()
                .get()
                .tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType)) {
            return true;
        }
        return dependencies
                .roomDoorInteractions()
                .get()
                .tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            dependencies.puzzles().pushBlockContactTicks = 0;
            return false;
        }
        int direction = RoomInteractionGeometry.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & RoomInteractionGeometry.keyDoorCollisionBit(direction)) == 0) {
            dependencies.puzzles().pushBlockContactTicks = 0;
            return false;
        }
        int touchedLocation = -1;
        for (int[] point : RoomInteractionGeometry.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            if (dependencies.roomSessionViews().get().objectAtRoomLocation(location) == OBJECT_KEYHOLE_BLOCK) {
                touchedLocation = location;
                break;
            }
        }
        if (touchedLocation < 0) {
            dependencies.puzzles().pushBlockContactTicks = 0;
            return false;
        }
        if (dependencies.roomState().dungeonItemState.currentFlag(DungeonItemState.SMALL_KEYS_INDEX) == 0) {
            dependencies.puzzles().pushBlockContactTicks = 0;
            dependencies.entityBridge().get().outbox.pendingRoomDialogRequests.add(new DialogRequest(0, 0x8C));
            return true;
        }
        dependencies.puzzles().pushBlockContactTicks++;
        if (dependencies.puzzles().pushBlockContactTicks < KEYHOLE_BLOCK_CONTACT_TICKS) {
            return true;
        }
        dependencies.puzzles().pushBlockContactTicks = 0;
        dependencies.roomState().dungeonItemState.consumeSmallKey();

        dependencies.roomObjectMutations().get().writeBombPuzzleObject(touchedLocation, OBJECT_FLOOR_OD);
        dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        return true;
    }

    record Dependencies(
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomDoorInteractions> roomDoorInteractions,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {}

    private BlockContact probePushableBlockContact(int linkPixelX, int linkPixelY, int direction) {
        int touchedLocation = -1;
        boolean keyholeBlockContact = false;
        for (int[] point : RoomInteractionGeometry.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            int object = dependencies.roomSessionViews().get().objectAtRoomLocation(location);
            if (object == OBJECT_PUSHABLE_BLOCK) {
                touchedLocation = location;
                break;
            }
            keyholeBlockContact |= object == OBJECT_KEYHOLE_BLOCK;
        }
        return new BlockContact(touchedLocation, keyholeBlockContact);
    }

    private record BlockContact(int location, boolean keyhole) {}
}
