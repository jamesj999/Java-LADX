package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.transition.Warp;

/** RoomPuzzleController operations for the live room session. */
final class RoomPuzzleController {
    private final Dependencies dependencies;

    RoomPuzzleController(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void harvestThrownRoomEventCollisions() {
        if (dependencies.entityBridge().get().entityRuntime == null) {
            return;
        }
        for (ThrownBackgroundCollisionEvent collision :
                dependencies.entityBridge().get().entityRuntime.consumePendingThrownBackgroundCollisions()) {
            int objectId = collision.objectId();
            if ((dependencies.puzzles().activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_THROW_AT_DOOR
                    && objectId >= 0x35
                    && objectId < 0x3D) {
                dependencies.puzzles().thrownAtDoorTriggerResolved = true;
            }
        }
    }

    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        dependencies.roomPuzzleController().get().tickIndoorKeyDoorAnimation();
        if (dependencies.roomPuzzleController().get().tickShutterClosing()) return;
        dependencies.roomPuzzleController().get().tickSwitchButton(linkEntityX, linkEntityY);
        dependencies.roomPuzzleController().get().tickRoomEventChestReveal(linkEntityX, linkEntityY);
        dependencies.roomPuzzleController().get().tickRoomEventStairReveal();
        if (dependencies.puzzles().pushedBlocksTriggerEffectPending) {
            dependencies.puzzles().pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.puzzles().activeRoomEvent == 0) {
            return;
        }
        int effect = dependencies.puzzles().activeRoomEvent & EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != EVENT_EFFECT_OPEN_LOCKED_DOORS
                && effect != EVENT_EFFECT_CLEAR_MIDBOSS
                && (dependencies.roomSessionViews().get().activeRoomStatusFlags() & ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = dependencies.puzzles().activeRoomEvent & EVENT_TRIGGER_MASK;
        if (!dependencies.puzzles().roomEventEffectExecuted
                && ((trigger == EVENT_TRIGGER_KILL_ALL_ENEMIES
                                && dependencies.roomPuzzleController().get().killAllEnemiesTriggerResolved())
                        || (trigger == EVENT_TRIGGER_STEP_ON_BUTTON && dependencies.puzzles().switchButtonPressed != 0)
                        || (trigger == EVENT_TRIGGER_LIGHT_TORCHES && dependencies.puzzles().roomTriggerCount == 0x02)
                        || (trigger == EVENT_TRIGGER_THROW_AT_DOOR
                                && dependencies.puzzles().thrownAtDoorTriggerResolved)
                        || (trigger == EVENT_TRIGGER_KILL_IN_ORDER
                                && dependencies.entityBridge().get().entityRuntime != null
                                && dependencies
                                                .entityBridge()
                                                .get()
                                                .entityRuntime
                                                .inspection()
                                                .killOrderAt(0)
                                        == 0
                                && dependencies
                                                .entityBridge()
                                                .get()
                                                .entityRuntime
                                                .inspection()
                                                .killOrderAt(1)
                                        == 1
                                && dependencies
                                                .entityBridge()
                                                .get()
                                                .entityRuntime
                                                .inspection()
                                                .killOrderAt(2)
                                        == 2))) {
            dependencies.puzzles().roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!dependencies.puzzles().roomEventEffectExecuted) {
            if ((effect == EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == EVENT_EFFECT_CLEAR_MIDBOSS)
                    && !dependencies.puzzles().shutterCloseStarted
                    && dependencies.puzzles().pendingShutterCloseMask != 0
                    && ((linkEntityX - 0x11) & 255) < 0x7E
                    && ((linkEntityY - 0x16) & 255) < 0x5E) {
                dependencies.puzzles().shutterCloseStarted = true;
                dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                dependencies.roomPuzzleController().get().tickShutterClosing();
            }
            return;
        }
        if (effect == EVENT_EFFECT_REVEAL_CHEST) {
            dependencies.puzzles().activeRoomEvent = 0;
            dependencies.puzzles().roomEventChestRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (dependencies.transientVfxSystem() != null) {
                dependencies
                        .transientVfxSystem()
                        .spawn(
                                TransientVfxType.CHEST_APPEARS,
                                0x88,
                                RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) + 0x10);
            }
        } else if (effect == EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            dependencies.roomPuzzleController().get().openActiveRoomShutterDoors(true);
            dependencies.puzzles().activeRoomEvent = 0;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == EVENT_EFFECT_DROP_KEY) {
            if (dependencies.entityBridge().get().entityRuntime != null
                    && dependencies.entityBridge().get().entityRuntime.spawnRoomEventKeyDrop() >= 0) {
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
            dependencies.puzzles().activeRoomEvent = 0;
        } else if (effect == EVENT_EFFECT_REVEAL_STAIRWAY) {
            dependencies.roomStoryState().get().markActiveRoomCompleted();
            dependencies.puzzles().activeRoomEvent = 0;
            dependencies.puzzles().roomEventStairRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (dependencies.transientVfxSystem() != null) {
                dependencies.transientVfxSystem().spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);
            }
        } else if (effect == EVENT_EFFECT_CLEAR_MIDBOSS) {
            dependencies.roomPuzzleController().get().openActiveRoomShutterDoors(true);
            if (dependencies.puzzles().activeRoomEvent == EVENT_CLEAR_MIDBOSS
                    && dependencies.location().activeRoom.mapId() >= 0
                    && dependencies.location().activeRoom.mapId()
                            < dependencies.roomState().dungeonProgressFlags.length) {
                dependencies.roomState()
                        .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x01;
                dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |= 0x20;
                dependencies.entityBridge().get().entityRuntime.configuration().setDungeonMinibossReady(true);
                dependencies
                        .entityBridge()
                        .get()
                        .outbox
                        .pendingRoomEntityEvents
                        .add(new EntityCombatEvent(0, 0x61, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            dependencies.puzzles().activeRoomEvent = 0;
        }
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
                            .snapshot());
        }
        if (dependencies.puzzles().pushedBlockMotionFrames != 0) {
            return;
        }
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
                            .snapshot());
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

    boolean pushedBlocksTriggerResolved() {
        if (dependencies.location().activeRoom == null || dependencies.puzzles().pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation =
                switch (dependencies.puzzles().pushedBlockDirection) {
                        // The source checks the object collided with on the block's
                        // movement-facing side; only horizontal pushes can satisfy $07.
                    case 2 -> dependencies.puzzles().pushedBlockDestinationLocation - 1;
                    case 3 -> dependencies.puzzles().pushedBlockDestinationLocation + 1;
                    default -> -1;
                };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = dependencies.roomSessionViews().get().objectAtRoomLocation(neighborLocation);
        return neighbor == OBJECT_SETTLED_PUSHED_BLOCK || neighbor == OBJECT_PUSHABLE_BLOCK;
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

    boolean killAllEnemiesTriggerResolved() {
        for (RoomEntity entity : dependencies.location().activeRoom.entities().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                    && (dependencies.romTables().entityOptions1(entity.type()) & ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL)
                            == 0) {
                return false;
            }
        }
        return true;
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

    boolean tickShutterClosing() {
        if (!dependencies.puzzles().shutterCloseStarted || dependencies.location().activeRoom == null) return false;
        if (dependencies.puzzles().shutterCloseDirection < 0) {
            if (dependencies.puzzles().pendingShutterCloseMask == 0) return false;
            dependencies.puzzles().shutterCloseDirection =
                    Integer.numberOfTrailingZeros(dependencies.puzzles().pendingShutterCloseMask);
            dependencies.puzzles().pendingShutterCloseMask &= ~(1 << dependencies.puzzles().shutterCloseDirection);
            dependencies.puzzles().shutterCloseLocation = new RoomObjectParser(dependencies.romData())
                    .indoorShutterLocation(
                            dependencies.location().activeRoom.roomObjectsArea(),
                            dependencies.puzzles().shutterCloseDirection);
            dependencies.puzzles().shutterCloseTicks = 0;
            return true;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        if (dependencies.puzzles().shutterCloseLocation >= 0) {
            int direction = dependencies.puzzles().shutterCloseDirection;
            int left = (dependencies.puzzles().shutterCloseLocation & 15) * 16
                    + Byte.toUnsignedInt(dependencies.romData()[RomBank.romOffset(2, 0x5BE0 + direction + 4)]);
            int top = (dependencies.puzzles().shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + Byte.toUnsignedInt(dependencies.romData()[rowOffset + row * 4]);
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = Byte.toUnsignedInt(dependencies.romData()[source + row * 2 + col]);
                    if (index + col < dependencies.location().activeRoom.tileIds().length) {
                        dependencies.location().activeRoom.tileIds()[index + col] = tile;
                        dependencies.puzzles().shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++dependencies.puzzles().shutterCloseTicks < 8) return true;
        int direction = dependencies.puzzles().shutterCloseDirection;
        dependencies.puzzles().shutterTileOverrides.clear();
        new RoomObjectParser(dependencies.romData())
                .applyIndoorShutterClosedState(
                        dependencies.location().activeRoom.roomObjectsArea(), 1 << direction);
        dependencies.puzzles().closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = dependencies.location().activeRoom.mapId() == 0xFF
                ? dependencies.roomState().colorDungeonRoomStatus
                : dependencies.roomState().indoorARoomStatus;
        status[dependencies.location().activeRoom.roomId()] &= (byte) ~KEY_DOOR_CURRENT_STATUS[direction];
        dependencies.puzzles().shutterCloseDirection = dependencies.puzzles().shutterCloseLocation = -1;
        dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors() {
        dependencies.roomPuzzleController().get().openActiveRoomShutterDoors(false);
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        dependencies.puzzles().pendingShutterCloseMask = dependencies.puzzles().closedShutterMask = 0;
        dependencies.puzzles().shutterCloseDirection = dependencies.puzzles().shutterCloseLocation = -1;
        dependencies.puzzles().shutterTileOverrides.clear();
        if (dependencies.location().activeRoom == null) {
            return;
        }
        int shutterMask = dependencies.location().activeRoom.shutterDoorMask();
        if (shutterMask != 0 && dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomObjectParser parser = new RoomObjectParser(dependencies.romData());
            int[] openDoorTypes = {0xF4, 0xF5, 0xF6, 0xF7};
            for (int direction = 0; direction < openDoorTypes.length; direction++) {
                if ((shutterMask & (1 << direction)) != 0) {
                    int location = parser.indoorShutterLocation(
                            dependencies.location().activeRoom.roomObjectsArea(), direction);
                    if (location < 0) {
                        continue;
                    }
                    parser.applyIndoorDoorMacro(
                            dependencies.location().activeRoom.roomObjectsArea(), openDoorTypes[direction], location);
                    if (persistDirectionalStatus) {
                        byte[] status = dependencies
                                .roomSessionViews()
                                .get()
                                .indoorStatusTableForMap(
                                        dependencies.location().activeRoom.mapId());
                        status[dependencies.location().activeRoom.roomId()] |=
                                (byte) KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom =
                                dependencies.roomSessionViews().get().adjacentIndoorRoomIdForBombWall(direction);
                        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
            dependencies
                    .overworldCollision()
                    .setRoom(dependencies.location().activeRoom.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
        dependencies.location().activeRoom.openShutterDoors();
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            java.util.function.Supplier<RoomEntityInputs> entityInputs,
            GPU gpu,
            OverworldCollision overworldCollision,
            byte[] romData,
            RomTables romTables,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomPuzzleController> roomPuzzleController,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomStoryState> roomStoryState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw,
            java.util.function.Supplier<RoomTransitionController> roomTransitionController,
            TransientVfxSystem transientVfxSystem) {}
}
