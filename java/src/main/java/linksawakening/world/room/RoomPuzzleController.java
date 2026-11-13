package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL;
import static linksawakening.world.room.RoomSessionRules.EVENT_CLEAR_MIDBOSS;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_CLEAR_MIDBOSS;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_DROP_KEY;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_MASK;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_OPEN_LOCKED_DOORS;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_REVEAL_CHEST;
import static linksawakening.world.room.RoomSessionRules.EVENT_EFFECT_REVEAL_STAIRWAY;
import static linksawakening.world.room.RoomSessionRules.EVENT_STAIRWAY_LOCATION;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_KILL_ALL_ENEMIES;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_KILL_IN_ORDER;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_LIGHT_TORCHES;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_MASK;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_PUSH_BLOCKS;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_PUSH_SINGLE_BLOCK;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_STEP_ON_BUTTON;
import static linksawakening.world.room.RoomSessionRules.EVENT_TRIGGER_THROW_AT_DOOR;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_ADJACENT_STATUS;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_CLOSED_OBJECTS;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_CURRENT_STATUS;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_OPEN_OBJECTS;
import static linksawakening.world.room.RoomSessionRules.OBJECT_PUSHABLE_BLOCK;
import static linksawakening.world.room.RoomSessionRules.OBJECT_SETTLED_PUSHED_BLOCK;
import static linksawakening.world.room.RoomSessionRules.OBJECT_STAIRS_DOWN;
import static linksawakening.world.room.RoomSessionRules.OBJECT_SWITCH_BUTTON;
import static linksawakening.world.room.RoomSessionRules.PUSHED_BLOCK_MOTION_FRAMES;
import static linksawakening.world.room.RoomSessionRules.ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
import static linksawakening.world.room.RoomSessionRules.ROOM_EVENT_CHEST_WRITE_COUNTDOWN;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomSessionRules.SWITCH_BUTTON_PRESSED;
import static linksawakening.world.room.RoomSessionRules.SWITCH_BUTTON_PRESS_FRAMES;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.transition.Warp;

/** Bank $02 room-event, switch, block and shutter progression over retained puzzle state. */
final class RoomPuzzleController {
    private final Dependencies dependencies;

    RoomPuzzleController(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void harvestThrownRoomEventCollisions() {
        if (dependencies.entityRuntimeState().runtime == null) {
            return;
        }
        var collisions = dependencies.entityRuntimeState().runtime.consumePendingThrownBackgroundCollisions();
        for (int i = 0; i < collisions.size(); i++) {
            var collision = collisions.get(i);
            int objectId = collision.objectId();
            if ((dependencies.puzzles().current.activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_THROW_AT_DOOR
                    && objectId >= 0x35
                    && objectId < 0x3D) {
                dependencies.puzzles().current.thrownAtDoorTriggerResolved = true;
            }
        }
    }

    void tickRoomEvent(int linkEntityX, int linkEntityY) {
        this.tickIndoorKeyDoorAnimation();
        if (this.tickShutterClosing()) return;
        this.tickSwitchButton(linkEntityX, linkEntityY);
        this.tickRoomEventChestReveal(linkEntityX, linkEntityY);
        this.tickRoomEventStairReveal();
        if (dependencies.puzzles().current.pushedBlocksTriggerEffectPending) {
            dependencies.puzzles().current.pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || dependencies.puzzles().current.activeRoomEvent == 0) {
            return;
        }
        int effect = dependencies.puzzles().current.activeRoomEvent & EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != EVENT_EFFECT_OPEN_LOCKED_DOORS
                && effect != EVENT_EFFECT_CLEAR_MIDBOSS
                && (dependencies.roomSessionViews().activeRoomStatusFlags() & ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = dependencies.puzzles().current.activeRoomEvent & EVENT_TRIGGER_MASK;
        if (!dependencies.puzzles().current.roomEventEffectExecuted
                && ((trigger == EVENT_TRIGGER_KILL_ALL_ENEMIES && this.killAllEnemiesTriggerResolved())
                        || (trigger == EVENT_TRIGGER_STEP_ON_BUTTON && dependencies.puzzles().switchButtonPressed != 0)
                        || (trigger == EVENT_TRIGGER_LIGHT_TORCHES
                                && dependencies.puzzles().current.roomTriggerCount == 0x02)
                        || (trigger == EVENT_TRIGGER_THROW_AT_DOOR
                                && dependencies.puzzles().current.thrownAtDoorTriggerResolved)
                        || (trigger == EVENT_TRIGGER_KILL_IN_ORDER
                                && dependencies.entityRuntimeState().runtime != null
                                && dependencies
                                                .entityRuntimeState()
                                                .runtime
                                                .inspection()
                                                .killOrderAt(0)
                                        == 0
                                && dependencies
                                                .entityRuntimeState()
                                                .runtime
                                                .inspection()
                                                .killOrderAt(1)
                                        == 1
                                && dependencies
                                                .entityRuntimeState()
                                                .runtime
                                                .inspection()
                                                .killOrderAt(2)
                                        == 2))) {
            dependencies.puzzles().current.roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!dependencies.puzzles().current.roomEventEffectExecuted) {
            if ((effect == EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == EVENT_EFFECT_CLEAR_MIDBOSS)
                    && !dependencies.puzzles().current.shutterCloseStarted
                    && dependencies.puzzles().current.pendingShutterCloseMask != 0
                    && ((linkEntityX - 0x11) & 255) < 0x7E
                    && ((linkEntityY - 0x16) & 255) < 0x5E) {
                dependencies.puzzles().current.shutterCloseStarted = true;
                dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                this.tickShutterClosing();
            }
            return;
        }
        applyRoomEventEffect(effect, linkEntityX, linkEntityY);
    }

    void tickPushedBlockMotion(int linkEntityX, int linkEntityY) {
        if (dependencies.puzzles().current.pushedBlockMotionFrames == 0 || dependencies.location().activeRoom == null) {
            return;
        }
        if (dependencies.entityInputs().entityDialogActive
                || dependencies.entityInputs().entityInventoryAppearing
                || dependencies.entityInputs().entityDialogCooldown != 0) {
            return;
        }
        dependencies.puzzles().current.pushedBlockMotionFrames--;
        int elapsedFrames = PUSHED_BLOCK_MOTION_FRAMES - dependencies.puzzles().current.pushedBlockMotionFrames;
        int distance = elapsedFrames >>> 1;
        int sourceX = ((dependencies.puzzles().current.pushedBlockSourceLocation & 0x0F) << 4) + 0x08;
        int sourceY = (dependencies.puzzles().current.pushedBlockSourceLocation & 0xF0) + 0x10;
        int x = sourceX;
        int y = sourceY;
        switch (dependencies.puzzles().current.pushedBlockDirection) {
            case 0 -> y -= distance;
            case 1 -> y += distance;
            case 2 -> x -= distance;
            case 3 -> x += distance;
            default -> {
                dependencies.puzzles().current.pushedBlockMotionFrames = 0;
                return;
            }
        }
        if (dependencies.entityRuntimeState().runtime != null) {
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .movePushedBlock(dependencies.puzzles().current.pushedBlockEntitySlot, x, y);
            if (dependencies.entityFrame().currentLinkMotionState
                    < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
                dependencies
                        .entityRuntimeState()
                        .runtime
                        .resolvePushedBlockLinkContact(
                                dependencies.puzzles().current.pushedBlockEntitySlot, linkEntityX, linkEntityY);
            }
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityRuntimeState()
                            .runtime
                            .inspection()
                            .view());
        }
        if (dependencies.puzzles().current.pushedBlockMotionFrames != 0) {
            return;
        }
        finishPushedBlock();
    }

    boolean pushedBlocksTriggerResolved() {
        if (dependencies.location().activeRoom == null
                || dependencies.puzzles().current.pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation =
                switch (dependencies.puzzles().current.pushedBlockDirection) {
                        // The source checks the object collided with on the block's
                        // movement-facing side; only horizontal pushes can satisfy $07.
                    case 2 -> dependencies.puzzles().current.pushedBlockDestinationLocation - 1;
                    case 3 -> dependencies.puzzles().current.pushedBlockDestinationLocation + 1;
                    default -> -1;
                };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = dependencies.roomSessionViews().objectAtRoomLocation(neighborLocation);
        return neighbor == OBJECT_SETTLED_PUSHED_BLOCK || neighbor == OBJECT_PUSHABLE_BLOCK;
    }

    void tickIndoorKeyDoorAnimation() {
        if (dependencies.puzzles().current.indoorKeyDoorAnimationCountdown == 0
                || dependencies.puzzles().current.indoorKeyDoorDirection < 0
                || dependencies.location().activeRoom == null) {
            return;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        dependencies.puzzles().current.indoorKeyDoorAnimationCountdown--;
        if (dependencies.puzzles().current.indoorKeyDoorAnimationCountdown != 0) {
            return;
        }

        int direction = dependencies.puzzles().current.indoorKeyDoorDirection;
        dependencies.puzzles().current.indoorKeyDoorDirection = -1;
        if (dependencies.puzzles().current.indoorBossDoorOpening) {
            this.replaceBossDoorObjects(dependencies.puzzles().current.indoorKeyDoorLocation);
            dependencies.puzzles().current.indoorBossDoorOpening = false;
        } else {
            this.replaceKeyDoorObjects(direction, dependencies.puzzles().current.indoorKeyDoorLocation);
        }
        dependencies.puzzles().current.indoorKeyDoorLocation = -1;
        byte[] status = dependencies
                .roomSessionViews()
                .indoorStatusTableForMap(dependencies.location().activeRoom.mapId());
        status[dependencies.location().activeRoom.roomId()] |= (byte) KEY_DOOR_CURRENT_STATUS[direction];
        int adjacentRoom = dependencies.roomSessionViews().adjacentIndoorRoomIdForBombWall(direction);
        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(dependencies.location().contents);
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
        for (RoomEntity entity : dependencies.location().activeRoom.entityView().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                    && (dependencies.romTables().entityPropertyTables().entityOptions1(entity.type())
                                    & ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL)
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
            dependencies.puzzles().current.switchButtonPressCounter = 0;
            return;
        }
        dependencies.puzzles().current.switchButtonPressCounter =
                (dependencies.puzzles().current.switchButtonPressCounter + 1) & 0xFF;
        if (dependencies.puzzles().current.switchButtonPressCounter == SWITCH_BUTTON_PRESS_FRAMES) {
            dependencies.puzzles().switchButtonPressed = SWITCH_BUTTON_PRESSED;
            dependencies.gpu().copyPressedFloorSwitchTiles(dependencies.romData());
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.FLOOR_SWITCH);
        }
    }

    void tickRoomEventChestReveal(int linkEntityX, int linkEntityY) {
        if (dependencies.puzzles().current.roomEventChestRevealCountdown == 0) {
            return;
        }
        dependencies.puzzles().current.roomEventChestRevealCountdown--;
        if (dependencies.puzzles().current.roomEventChestRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int location = RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) | 0x08;
        dependencies.roomObjectMutations().writeBombPuzzleObject(dependencies.location().contents, location, 0xA0);
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(dependencies.location().contents);
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
    }

    void tickRoomEventStairReveal() {
        if (dependencies.puzzles().current.roomEventStairRevealCountdown == 0) {
            return;
        }
        dependencies.puzzles().current.roomEventStairRevealCountdown--;
        if (dependencies.puzzles().current.roomEventStairRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + EVENT_STAIRWAY_LOCATION;
        dependencies.location().activeRoom.roomObjectsArea()[areaIndex] = OBJECT_STAIRS_DOWN;
        dependencies.puzzles().current.configureStaircase(EVENT_STAIRWAY_LOCATION);
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(dependencies.location().contents);
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    boolean tickShutterClosing() {
        if (!dependencies.puzzles().current.shutterCloseStarted || dependencies.location().activeRoom == null)
            return false;
        if (dependencies.puzzles().current.shutterCloseDirection < 0) {
            if (dependencies.puzzles().current.pendingShutterCloseMask == 0) return false;
            dependencies.puzzles().current.shutterCloseDirection =
                    Integer.numberOfTrailingZeros(dependencies.puzzles().current.pendingShutterCloseMask);
            dependencies.puzzles().current.pendingShutterCloseMask &=
                    ~(1 << dependencies.puzzles().current.shutterCloseDirection);
            dependencies.puzzles().current.shutterCloseLocation = new RoomObjectParser(dependencies.romData())
                    .indoorShutterLocation(
                            dependencies.location().activeRoom.roomObjectsArea(),
                            dependencies.puzzles().current.shutterCloseDirection);
            dependencies.puzzles().current.shutterCloseTicks = 0;
            return true;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        if (dependencies.puzzles().current.shutterCloseLocation >= 0) {
            int direction = dependencies.puzzles().current.shutterCloseDirection;
            int left = (dependencies.puzzles().current.shutterCloseLocation & 15) * 16
                    + dependencies.romData().u8(RomAddress.fromOffset(RomBank.romOffset(2, 0x5BE0 + direction + 4)));
            int top = (dependencies.puzzles().current.shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + dependencies.romData().u8(RomAddress.fromOffset(rowOffset + row * 4));
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = dependencies.romData().u8(RomAddress.fromOffset(source + row * 2 + col));
                    if (index + col < dependencies.location().activeRoom.tileIds().length) {
                        dependencies.location().activeRoom.tileIds()[index + col] = tile;
                        dependencies.puzzles().current.shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++dependencies.puzzles().current.shutterCloseTicks < 8) return true;
        int direction = dependencies.puzzles().current.shutterCloseDirection;
        dependencies.puzzles().current.shutterTileOverrides.clear();
        new RoomObjectParser(dependencies.romData())
                .applyIndoorShutterClosedState(
                        dependencies.location().activeRoom.roomObjectsArea(), 1 << direction);
        dependencies.puzzles().current.closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = dependencies.location().activeRoom.mapId() == 0xFF
                ? dependencies.roomState().colorDungeonRoomStatus
                : dependencies.roomState().indoorARoomStatus;
        status[dependencies.location().activeRoom.roomId()] &= (byte) ~KEY_DOOR_CURRENT_STATUS[direction];
        dependencies.puzzles().current.shutterCloseDirection = dependencies.puzzles().current.shutterCloseLocation = -1;
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(dependencies.location().contents);
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors() {
        this.openActiveRoomShutterDoors(false);
    }

    void openActiveRoomShutterDoors(boolean persistDirectionalStatus) {
        dependencies.puzzles().current.pendingShutterCloseMask = dependencies.puzzles().current.closedShutterMask = 0;
        dependencies.puzzles().current.shutterCloseDirection = dependencies.puzzles().current.shutterCloseLocation = -1;
        dependencies.puzzles().current.shutterTileOverrides.clear();
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
                                .indoorStatusTableForMap(
                                        dependencies.location().activeRoom.mapId());
                        status[dependencies.location().activeRoom.roomId()] |=
                                (byte) KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom = dependencies.roomSessionViews().adjacentIndoorRoomIdForBombWall(direction);
                        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            dependencies.roomTileRedraw().refreshActiveRoomTilemap(dependencies.location().contents);
            dependencies
                    .overworldCollision()
                    .setRoom(dependencies.location().activeRoom.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
        dependencies.location().activeRoom.openShutterDoors();
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            RoomEntitySessionState entityRuntimeState,
            RoomEntityInputs entityInputs,
            GPU gpu,
            OverworldCollision overworldCollision,
            Rom romData,
            RomTables romTables,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomObjectMutations roomTileRedraw,
            TransientVfxSystem transientVfxSystem) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(entityInputs, "entityInputs");
            java.util.Objects.requireNonNull(gpu, "gpu");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(romTables, "romTables");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
        }
    }

    private void applyRoomEventEffect(int effect, int linkEntityX, int linkEntityY) {
        if (effect == EVENT_EFFECT_REVEAL_CHEST) {
            dependencies.puzzles().current.activeRoomEvent = 0;
            dependencies.puzzles().current.roomEventChestRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (dependencies.transientVfxSystem() != null) {
                dependencies
                        .transientVfxSystem()
                        .spawn(
                                TransientVfxType.CHEST_APPEARS,
                                0x88,
                                RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) + 0x10);
            }
        } else if (effect == EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            this.openActiveRoomShutterDoors(true);
            dependencies.puzzles().current.activeRoomEvent = 0;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == EVENT_EFFECT_DROP_KEY) {
            if (dependencies.entityRuntimeState().runtime != null
                    && dependencies.entityRuntimeState().runtime.spawnRoomEventKeyDrop() >= 0) {
                dependencies
                        .location()
                        .activeRoom
                        .replaceEntities(dependencies
                                .entityRuntimeState()
                                .runtime
                                .inspection()
                                .view());
            }
            dependencies.puzzles().current.activeRoomEvent = 0;
        } else if (effect == EVENT_EFFECT_REVEAL_STAIRWAY) {
            dependencies.roomState().markCompleted(dependencies.location().activeRoom);
            dependencies.puzzles().current.activeRoomEvent = 0;
            dependencies.puzzles().current.roomEventStairRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;
            if (dependencies.transientVfxSystem() != null) {
                dependencies.transientVfxSystem().spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);
            }
        } else if (effect == EVENT_EFFECT_CLEAR_MIDBOSS) {
            this.openActiveRoomShutterDoors(true);
            if (dependencies.puzzles().current.activeRoomEvent == EVENT_CLEAR_MIDBOSS
                    && dependencies.location().activeRoom.mapId() >= 0
                    && dependencies.location().activeRoom.mapId()
                            < dependencies.roomState().dungeonProgressFlags.length) {
                dependencies.roomState()
                        .dungeonProgressFlags[dependencies.location().activeRoom.mapId()] |= 0x01;
                dependencies.roomSessionViews()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |= 0x20;
                dependencies.entityRuntimeState().runtime.configuration().setDungeonMinibossReady(true);
                dependencies
                        .entityRuntimeState()
                        .outbox
                        .pendingRoomEntityEvents
                        .add(new EntityCombatEvent(0, 0x61, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            dependencies.puzzles().current.activeRoomEvent = 0;
        }
    }

    private void finishPushedBlock() {
        int destinationIndex =
                RoomConstants.ROOM_OBJECTS_BASE + dependencies.puzzles().current.pushedBlockDestinationLocation;
        if (destinationIndex >= 0
                && destinationIndex < dependencies.location().activeRoom.roomObjectsArea().length) {
            dependencies.location().activeRoom.roomObjectsArea()[destinationIndex] = OBJECT_SETTLED_PUSHED_BLOCK;
        }
        if (dependencies.entityRuntimeState().runtime != null) {
            dependencies
                    .entityRuntimeState()
                    .runtime
                    .removePushedBlock(dependencies.puzzles().current.pushedBlockEntitySlot);
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityRuntimeState()
                            .runtime
                            .inspection()
                            .view());
        }
        dependencies.puzzles().current.pushedBlockEntitySlot = -1;
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(dependencies.location().contents);
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        if ((dependencies.puzzles().current.activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_SINGLE_BLOCK) {
            dependencies.puzzles().current.roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        } else if ((dependencies.puzzles().current.activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_BLOCKS
                && this.pushedBlocksTriggerResolved()) {
            dependencies.puzzles().current.roomEventEffectExecuted = true;
            dependencies.puzzles().current.pushedBlocksTriggerEffectPending = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
    }
}
