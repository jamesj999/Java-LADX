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

    void harvestThrownRoomEventCollisions(RoomRuntime loaded) {

        var collisions = loaded.entities.outbox().consumePendingThrownBackgroundCollisions();
        for (int i = 0; i < collisions.size(); i++) {
            var collision = collisions.get(i);
            int objectId = collision.objectId();
            if ((loaded.contents.puzzles.activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_THROW_AT_DOOR
                    && objectId >= 0x35
                    && objectId < 0x3D) {
                loaded.contents.puzzles.thrownAtDoorTriggerResolved = true;
            }
        }
    }

    void tickRoomEvent(RoomRuntime loaded, int linkEntityX, int linkEntityY) {
        this.tickIndoorKeyDoorAnimation(loaded.contents);
        if (this.tickShutterClosing(loaded.contents)) return;
        this.tickSwitchButton(loaded.contents, linkEntityX, linkEntityY);
        this.tickRoomEventChestReveal(loaded.contents, linkEntityX, linkEntityY);
        this.tickRoomEventStairReveal(loaded.contents);
        if (loaded.contents.puzzles.pushedBlocksTriggerEffectPending) {
            loaded.contents.puzzles.pushedBlocksTriggerEffectPending = false;
            return;
        }
        if (loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD || loaded.contents.puzzles.activeRoomEvent == 0) {
            return;
        }
        int effect = loaded.contents.puzzles.activeRoomEvent & EVENT_EFFECT_MASK;
        // OpenLockedDoorsEffectHandler does not call EventEffectGuard:
        // opening a chest sets bit $10 too, but must not cancel the shutters.
        if (effect != EVENT_EFFECT_OPEN_LOCKED_DOORS
                && effect != EVENT_EFFECT_CLEAR_MIDBOSS
                && (dependencies.roomSessionViews().activeRoomStatusFlags(loaded.room) & ROOM_STATUS_EVENT_1) != 0) {
            return;
        }
        int trigger = loaded.contents.puzzles.activeRoomEvent & EVENT_TRIGGER_MASK;
        if (!loaded.contents.puzzles.roomEventEffectExecuted
                && ((trigger == EVENT_TRIGGER_KILL_ALL_ENEMIES && this.killAllEnemiesTriggerResolved(loaded.contents))
                        || (trigger == EVENT_TRIGGER_STEP_ON_BUTTON && dependencies.puzzles().switchButtonPressed != 0)
                        || (trigger == EVENT_TRIGGER_LIGHT_TORCHES && loaded.contents.puzzles.roomTriggerCount == 0x02)
                        || (trigger == EVENT_TRIGGER_THROW_AT_DOOR
                                && loaded.contents.puzzles.thrownAtDoorTriggerResolved)
                        || (trigger == EVENT_TRIGGER_KILL_IN_ORDER
                                && loaded.entities.inspection().killOrderAt(0) == 0
                                && loaded.entities.inspection().killOrderAt(1) == 1
                                && loaded.entities.inspection().killOrderAt(2) == 2))) {
            loaded.contents.puzzles.roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
        if (!loaded.contents.puzzles.roomEventEffectExecuted) {
            if ((effect == EVENT_EFFECT_OPEN_LOCKED_DOORS || effect == EVENT_EFFECT_CLEAR_MIDBOSS)
                    && !loaded.contents.puzzles.shutterCloseStarted
                    && loaded.contents.puzzles.pendingShutterCloseMask != 0
                    && ((linkEntityX - 0x11) & 255) < 0x7E
                    && ((linkEntityY - 0x16) & 255) < 0x5E) {
                loaded.contents.puzzles.shutterCloseStarted = true;
                dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_CLOSED);
                this.tickShutterClosing(loaded.contents);
            }
            return;
        }
        applyRoomEventEffect(loaded, effect, linkEntityX, linkEntityY);
    }

    void tickPushedBlockMotion(RoomRuntime loaded, int linkEntityX, int linkEntityY) {
        if (loaded.contents.puzzles.pushedBlockMotionFrames == 0) {
            return;
        }
        if (dependencies.entityInputs().entityDialogActive
                || dependencies.entityInputs().entityInventoryAppearing
                || dependencies.entityInputs().entityDialogCooldown != 0) {
            return;
        }
        loaded.contents.puzzles.pushedBlockMotionFrames--;
        int elapsedFrames = PUSHED_BLOCK_MOTION_FRAMES - loaded.contents.puzzles.pushedBlockMotionFrames;
        int distance = elapsedFrames >>> 1;
        int sourceX = ((loaded.contents.puzzles.pushedBlockSourceLocation & 0x0F) << 4) + 0x08;
        int sourceY = (loaded.contents.puzzles.pushedBlockSourceLocation & 0xF0) + 0x10;
        int x = sourceX;
        int y = sourceY;
        switch (loaded.contents.puzzles.pushedBlockDirection) {
            case 0 -> y -= distance;
            case 1 -> y += distance;
            case 2 -> x -= distance;
            case 3 -> x += distance;
            default -> {
                loaded.contents.puzzles.pushedBlockMotionFrames = 0;
                return;
            }
        }

        loaded.entities.movePushedBlock(loaded.contents.puzzles.pushedBlockEntitySlot, x, y);
        if (dependencies.entityFrame().currentLinkMotionState < EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE) {
            loaded.entities.resolvePushedBlockLinkContact(
                    loaded.contents.puzzles.pushedBlockEntitySlot, linkEntityX, linkEntityY);
        }
        loaded.room.replaceEntities(loaded.entities.inspection().view());

        if (loaded.contents.puzzles.pushedBlockMotionFrames != 0) {
            return;
        }
        finishPushedBlock(loaded);
    }

    boolean pushedBlocksTriggerResolved(RoomContents contents) {
        if (contents.puzzles.pushedBlockDestinationLocation < 0) {
            return false;
        }
        int neighborLocation =
                switch (contents.puzzles.pushedBlockDirection) {
                        // The source checks the object collided with on the block's
                        // movement-facing side; only horizontal pushes can satisfy $07.
                    case 2 -> contents.puzzles.pushedBlockDestinationLocation - 1;
                    case 3 -> contents.puzzles.pushedBlockDestinationLocation + 1;
                    default -> -1;
                };
        if (neighborLocation < 0) {
            return false;
        }
        int neighbor = dependencies.roomSessionViews().objectAtRoomLocation(contents.room, neighborLocation);
        return neighbor == OBJECT_SETTLED_PUSHED_BLOCK || neighbor == OBJECT_PUSHABLE_BLOCK;
    }

    void tickIndoorKeyDoorAnimation(RoomContents contents) {
        if (contents.puzzles.indoorKeyDoorAnimationCountdown == 0 || contents.puzzles.indoorKeyDoorDirection < 0) {
            return;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        contents.puzzles.indoorKeyDoorAnimationCountdown--;
        if (contents.puzzles.indoorKeyDoorAnimationCountdown != 0) {
            return;
        }

        int direction = contents.puzzles.indoorKeyDoorDirection;
        contents.puzzles.indoorKeyDoorDirection = -1;
        if (contents.puzzles.indoorBossDoorOpening) {
            this.replaceBossDoorObjects(contents, contents.puzzles.indoorKeyDoorLocation);
            contents.puzzles.indoorBossDoorOpening = false;
        } else {
            this.replaceKeyDoorObjects(contents, direction, contents.puzzles.indoorKeyDoorLocation);
        }
        contents.puzzles.indoorKeyDoorLocation = -1;
        byte[] status = dependencies.roomSessionViews().indoorStatusTableForMap(contents.room.mapId());
        status[contents.room.roomId()] |= (byte) KEY_DOOR_CURRENT_STATUS[direction];
        int adjacentRoom = dependencies.roomSessionViews().adjacentIndoorRoomIdForBombWall(contents.room, direction);
        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
        dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    void replaceKeyDoorObjects(RoomContents contents, int direction, int location) {
        int firstClosedObject = KEY_DOOR_CLOSED_OBJECTS[direction];
        int[] openObjects = KEY_DOOR_OPEN_OBJECTS[direction];
        int[] objects = contents.room.roomObjectsArea();
        int secondLocation = location + (direction < 2 ? 1 : 0x10);
        int[] locations = {location, secondLocation};
        for (int objectOffset = 0; objectOffset < locations.length; objectOffset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + locations[objectOffset];
            if (areaIndex >= 0
                    && areaIndex < objects.length
                    && (objects[areaIndex] & 0xFF) == firstClosedObject + objectOffset) {
                objects[areaIndex] = openObjects[objectOffset];
                if (contents.room.renderValues() != null) {
                    contents.room.renderValues()[areaIndex] = openObjects[objectOffset];
                }
            }
        }
    }

    void replaceBossDoorObjects(RoomContents contents, int location) {
        int[] objects = contents.room.roomObjectsArea();
        int[] openObjects = KEY_DOOR_OPEN_OBJECTS[0];
        for (int offset = 0; offset < 2; offset++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location + offset;
            if (areaIndex >= 0 && areaIndex < objects.length && objects[areaIndex] == 0xA4 + offset) {
                objects[areaIndex] = openObjects[offset];
                if (contents.room.renderValues() != null) {
                    contents.room.renderValues()[areaIndex] = openObjects[offset];
                }
            }
        }
    }

    boolean killAllEnemiesTriggerResolved(RoomContents contents) {
        for (RoomEntity entity : contents.room.entityView().slots()) {
            if (entity.status() != EntityStatus.DISABLED
                    && (dependencies.romTables().entityPropertyTables().entityOptions1(entity.type())
                                    & ENTITY_OPTION_EXCLUDED_FROM_KILL_ALL)
                            == 0) {
                return false;
            }
        }
        return true;
    }

    void tickSwitchButton(RoomContents contents, int linkEntityX, int linkEntityY) {
        if (contents.room.mapCategory() == Warp.CATEGORY_OVERWORLD || dependencies.puzzles().switchButtonPressed != 0) {
            return;
        }
        int objectUnderLink = dependencies
                .overworldCollision()
                .groundInteractionSample(linkEntityX, linkEntityY)
                .objectId();
        if (objectUnderLink != OBJECT_SWITCH_BUTTON) {
            contents.puzzles.switchButtonPressCounter = 0;
            return;
        }
        contents.puzzles.switchButtonPressCounter = (contents.puzzles.switchButtonPressCounter + 1) & 0xFF;
        if (contents.puzzles.switchButtonPressCounter == SWITCH_BUTTON_PRESS_FRAMES) {
            dependencies.puzzles().switchButtonPressed = SWITCH_BUTTON_PRESSED;
            dependencies.gpu().copyPressedFloorSwitchTiles(dependencies.romData());
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.FLOOR_SWITCH);
        }
    }

    void tickRoomEventChestReveal(RoomContents contents, int linkEntityX, int linkEntityY) {
        if (contents.puzzles.roomEventChestRevealCountdown == 0) {
            return;
        }
        contents.puzzles.roomEventChestRevealCountdown--;
        if (contents.puzzles.roomEventChestRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int location = RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) | 0x08;
        dependencies.roomObjectMutations().writeBombPuzzleObject(contents, location, 0xA0);
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
        dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
    }

    void tickRoomEventStairReveal(RoomContents contents) {
        if (contents.puzzles.roomEventStairRevealCountdown == 0) {
            return;
        }
        contents.puzzles.roomEventStairRevealCountdown--;
        if (contents.puzzles.roomEventStairRevealCountdown != ROOM_EVENT_CHEST_WRITE_COUNTDOWN) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + EVENT_STAIRWAY_LOCATION;
        contents.room.roomObjectsArea()[areaIndex] = OBJECT_STAIRS_DOWN;
        contents.puzzles.configureStaircase(EVENT_STAIRWAY_LOCATION);
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
        dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
    }

    boolean tickShutterClosing(RoomContents contents) {
        if (!contents.puzzles.shutterCloseStarted) return false;
        if (contents.puzzles.shutterCloseDirection < 0) {
            if (contents.puzzles.pendingShutterCloseMask == 0) return false;
            contents.puzzles.shutterCloseDirection =
                    Integer.numberOfTrailingZeros(contents.puzzles.pendingShutterCloseMask);
            contents.puzzles.pendingShutterCloseMask &= ~(1 << contents.puzzles.shutterCloseDirection);
            contents.puzzles.shutterCloseLocation = new RoomObjectParser(dependencies.romData())
                    .indoorShutterLocation(contents.room.roomObjectsArea(), contents.puzzles.shutterCloseDirection);
            contents.puzzles.shutterCloseTicks = 0;
            return true;
        }
        dependencies.entityFrame().worldLinkMotionBlockPending = true;
        if (contents.puzzles.shutterCloseLocation >= 0) {
            int direction = contents.puzzles.shutterCloseDirection;
            int left = (contents.puzzles.shutterCloseLocation & 15) * 16
                    + dependencies.romData().u8(RomAddress.fromOffset(RomBank.romOffset(2, 0x5BE0 + direction + 4)));
            int top = (contents.puzzles.shutterCloseLocation >>> 4) * 16;
            int source = RomBank.romOffset(2, 0x5BC4 + direction * 4);
            int rowOffset = RomBank.romOffset(2, 0x5BE8 + direction + 4);
            for (int row = 0; row < 2; row++) {
                int y = top + dependencies.romData().u8(RomAddress.fromOffset(rowOffset + row * 4));
                int index = (y / 8) * RoomConstants.ROOM_TILE_WIDTH + left / 8;
                for (int col = 0; col < 2; col++) {
                    int tile = dependencies.romData().u8(RomAddress.fromOffset(source + row * 2 + col));
                    if (index + col < contents.room.tileIds().length) {
                        contents.room.tileIds()[index + col] = tile;
                        contents.puzzles.shutterTileOverrides.put(index + col, tile);
                    }
                }
            }
        }
        if (++contents.puzzles.shutterCloseTicks < 8) return true;
        int direction = contents.puzzles.shutterCloseDirection;
        contents.puzzles.shutterTileOverrides.clear();
        new RoomObjectParser(dependencies.romData())
                .applyIndoorShutterClosedState(contents.room.roomObjectsArea(), 1 << direction);
        contents.puzzles.closedShutterMask |= 1 << direction;
        // $02:5D21 uses indoor A even for indoor B (the original's bank selection).
        byte[] status = contents.room.mapId() == 0xFF
                ? dependencies.roomState().colorDungeonRoomStatus
                : dependencies.roomState().indoorARoomStatus;
        status[contents.room.roomId()] &= (byte) ~KEY_DOOR_CURRENT_STATUS[direction];
        contents.puzzles.shutterCloseDirection = contents.puzzles.shutterCloseLocation = -1;
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
        dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
        return true;
    }

    void openActiveRoomShutterDoors(RoomContents contents) {
        this.openActiveRoomShutterDoors(contents, false);
    }

    void openActiveRoomShutterDoors(RoomContents contents, boolean persistDirectionalStatus) {
        contents.puzzles.pendingShutterCloseMask = contents.puzzles.closedShutterMask = 0;
        contents.puzzles.shutterCloseDirection = contents.puzzles.shutterCloseLocation = -1;
        contents.puzzles.shutterTileOverrides.clear();

        int shutterMask = contents.room.shutterDoorMask();
        if (shutterMask != 0 && contents.room.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            RoomObjectParser parser = new RoomObjectParser(dependencies.romData());
            int[] openDoorTypes = {0xF4, 0xF5, 0xF6, 0xF7};
            for (int direction = 0; direction < openDoorTypes.length; direction++) {
                if ((shutterMask & (1 << direction)) != 0) {
                    int location = parser.indoorShutterLocation(contents.room.roomObjectsArea(), direction);
                    if (location < 0) {
                        continue;
                    }
                    parser.applyIndoorDoorMacro(contents.room.roomObjectsArea(), openDoorTypes[direction], location);
                    if (persistDirectionalStatus) {
                        byte[] status = dependencies.roomSessionViews().indoorStatusTableForMap(contents.room.mapId());
                        status[contents.room.roomId()] |= (byte) KEY_DOOR_CURRENT_STATUS[direction];
                        int adjacentRoom = dependencies
                                .roomSessionViews()
                                .adjacentIndoorRoomIdForBombWall(contents.room, direction);
                        status[adjacentRoom] |= (byte) KEY_DOOR_ADJACENT_STATUS[direction];
                    }
                }
            }
            dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
            dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
        contents.room.openShutterDoors();
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
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
            java.util.Objects.requireNonNull(transientVfxSystem, "transientVfxSystem");
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
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

    private void applyRoomEventEffect(RoomRuntime loaded, int effect, int linkEntityX, int linkEntityY) {
        if (effect == EVENT_EFFECT_REVEAL_CHEST) {
            loaded.contents.puzzles.activeRoomEvent = 0;
            loaded.contents.puzzles.roomEventChestRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;

            dependencies
                    .transientVfxSystem()
                    .spawn(
                            TransientVfxType.CHEST_APPEARS,
                            0x88,
                            RoomInteractionGeometry.roomEventChestTop(linkEntityX, linkEntityY) + 0x10);

        } else if (effect == EVENT_EFFECT_OPEN_LOCKED_DOORS) {
            this.openActiveRoomShutterDoors(loaded.contents, true);
            loaded.contents.puzzles.activeRoomEvent = 0;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        } else if (effect == EVENT_EFFECT_DROP_KEY) {
            if (loaded.entities.spawns().spawnRoomEventKeyDrop() >= 0) {
                loaded.room.replaceEntities(loaded.entities.inspection().view());
            }
            loaded.contents.puzzles.activeRoomEvent = 0;
        } else if (effect == EVENT_EFFECT_REVEAL_STAIRWAY) {
            dependencies.roomState().markCompleted(loaded.room);
            loaded.contents.puzzles.activeRoomEvent = 0;
            loaded.contents.puzzles.roomEventStairRevealCountdown = ROOM_EVENT_CHEST_REVEAL_COUNTDOWN;

            dependencies.transientVfxSystem().spawn(TransientVfxType.STAIRS_APPEARS, 0x88, 0x20);

        } else if (effect == EVENT_EFFECT_CLEAR_MIDBOSS) {
            this.openActiveRoomShutterDoors(loaded.contents, true);
            if (loaded.contents.puzzles.activeRoomEvent == EVENT_CLEAR_MIDBOSS
                    && loaded.room.mapId() >= 0
                    && loaded.room.mapId() < dependencies.roomState().dungeonProgressFlags.length) {
                dependencies.roomState().dungeonProgressFlags[loaded.room.mapId()] |= 0x01;
                dependencies.roomSessionViews().indoorStatusTableForMap(loaded.room.mapId())[loaded.room.roomId()] |=
                        0x20;
                loaded.entities.configuration().setDungeonMinibossReady(true);
                dependencies
                        .entityRuntimeState()
                        .outbox
                        .pendingRoomEntityEvents
                        .add(new EntityCombatEvent(0, 0x61, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x1B));
            }
            loaded.contents.puzzles.activeRoomEvent = 0;
        }
    }

    private void finishPushedBlock(RoomRuntime loaded) {
        int destinationIndex = RoomConstants.ROOM_OBJECTS_BASE + loaded.contents.puzzles.pushedBlockDestinationLocation;
        if (destinationIndex >= 0 && destinationIndex < loaded.room.roomObjectsArea().length) {
            loaded.room.roomObjectsArea()[destinationIndex] = OBJECT_SETTLED_PUSHED_BLOCK;
        }

        loaded.entities.removePushedBlock(loaded.contents.puzzles.pushedBlockEntitySlot);
        loaded.room.replaceEntities(loaded.entities.inspection().view());

        loaded.contents.puzzles.pushedBlockEntitySlot = -1;
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(loaded.contents);
        dependencies.overworldCollision().setRoom(loaded.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        if ((loaded.contents.puzzles.activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_SINGLE_BLOCK) {
            loaded.contents.puzzles.roomEventEffectExecuted = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        } else if ((loaded.contents.puzzles.activeRoomEvent & EVENT_TRIGGER_MASK) == EVENT_TRIGGER_PUSH_BLOCKS
                && this.pushedBlocksTriggerResolved(loaded.contents)) {
            loaded.contents.puzzles.roomEventEffectExecuted = true;
            loaded.contents.puzzles.pushedBlocksTriggerEffectPending = true;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.PUZZLE_SOLVED);
        }
    }
}
