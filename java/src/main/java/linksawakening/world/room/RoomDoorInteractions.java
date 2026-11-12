package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

/** RoomDoorInteractions operations for the live room session. */
final class RoomDoorInteractions {
    private final Dependencies dependencies;

    RoomDoorInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        if (!actionPressed
                || linkDirection != Link.DIRECTION_UP
                || dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        if (linkPixelX < 0 || linkPixelY < 0 || linkPixelX >= ROOM_PIXEL_WIDTH || linkPixelY >= ROOM_PIXEL_HEIGHT) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int column = Math.floorDiv(linkPixelX + (dependencies.romData()[0x1F4B] & 0xFF), 0x10);
        int row = Math.floorDiv(linkPixelY + (dependencies.romData()[0x1F4F] & 0xFF), 0x10);
        if (column < 0
                || column >= RoomConstants.OBJECTS_PER_ROW
                || row < 0
                || row >= RoomConstants.OBJECTS_PER_COLUMN) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int location = (row << 4) | column;
        if (dependencies.roomSessionViews().get().objectAtRoomLocation(location) != 0xA0) {
            return new ChestOpenResult(false, -1, -1, location);
        }
        int itemType = dependencies
                .chestContentsTable()
                .itemForSpawn(
                        dependencies.location().activeRoom.mapId(),
                        dependencies.location().activeRoom.roomId(),
                        swordLevel,
                        dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD);
        int slot = dependencies.entityBridge().get().entityRuntime.spawnChestWithItem(column << 4, row << 4, itemType);
        if (slot < 0) {
            return new ChestOpenResult(false, -1, -1, location);
        }

        dependencies.roomObjectMutations().get().writeBombPuzzleObject(location, 0xA1);
        byte[] status = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? dependencies.roomState().overworldRoomStatus
                : dependencies
                        .roomSessionViews()
                        .get()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId());
        status[dependencies.location().activeRoom.roomId()] |= (byte) ROOM_STATUS_CHEST_OPEN;
        // writeBombPuzzleObject already updates the live GBC cell. Rebuilding
        // from the ROM overlay would discard other live mutations (cut bushes).
        if (dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
        }
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies
                .overworldCollision()
                .setGbcOverlay(
                        dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                                ? dependencies.location().activeRoom.gbcOverlay()
                                : null);
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
        return new ChestOpenResult(true, itemType, slot, location);
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

    boolean tryLiftIndoorObject(
            int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) {
        return dependencies
                .roomDoorInteractions()
                .get()
                .tryLiftIndoorObject(
                        linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, false);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return tryLiftObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive, 1);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel) {
        return tryLiftObject(
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive,
                braceletLevel,
                pieceOfPowerActive);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {
        if (dependencies.location().activeRoom == null
                || dependencies.entityBridge().get().entityRuntime == null
                || dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || !braceletButtonHeld
                || dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .liftedEntityState()
                        .active()) {
            dependencies.puzzles().indoorPotPullFrames = 0;
            return false;
        }
        int oppositeButton =
                switch (linkDirection) {
                    case Link.DIRECTION_UP -> 0x08;
                    case Link.DIRECTION_DOWN -> 0x04;
                    case Link.DIRECTION_LEFT -> 0x01;
                    case Link.DIRECTION_RIGHT -> 0x02;
                    default -> 0;
                };
        int swordAreaX =
                switch (linkDirection) {
                    case Link.DIRECTION_RIGHT -> 0x0C;
                    case Link.DIRECTION_LEFT -> 0x03;
                    case Link.DIRECTION_UP, Link.DIRECTION_DOWN -> 0x08;
                    default -> 0;
                };
        int swordAreaY =
                switch (linkDirection) {
                    case Link.DIRECTION_RIGHT, Link.DIRECTION_LEFT -> 0x0A;
                    case Link.DIRECTION_UP -> 0x05;
                    case Link.DIRECTION_DOWN -> 0x10;
                    default -> 0;
                };
        int objectLeft = (linkPixelX + swordAreaX) & 0xF0;
        int objectTop = (linkPixelY + swordAreaY) & 0xF0;
        int touchedLocation = objectTop | (objectLeft >>> 4);
        int touchedObject = dependencies.roomSessionViews().get().objectAtRoomLocation(touchedLocation);
        boolean outdoor = dependencies.location().activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD;
        // Statue standing handler, $19:4076: asymmetric 32 by 48 contact window.
        linksawakening.world.entity.RoomEntity statue = null;
        if (!outdoor) {
            for (var entity : dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .inspection()
                    .snapshot()
                    .loadedEntities()) {
                if (entity.type() == 0x9D
                        && entity.status() == linksawakening.world.entity.EntityStatus.ACTIVE
                        && (((linkPixelX + 8) - entity.x() + 0x10) & 0xFF) < 0x20
                        && (((linkPixelY + 16) - entity.y() + 0x20) & 0xFF) < 0x30) {
                    statue = entity;
                    break;
                }
            }
        }

        if (statue == null
                && touchedObject != OBJECT_LIFTABLE_POT
                && !(outdoor && touchedObject == 0x5C)
                && touchedObject != OBJECT_POT_WITH_SWITCH) {
            dependencies.puzzles().indoorPotPullFrames = 0;
            return false;
        }
        if (oppositeButton == 0
                || (pressedButtonsMask & oppositeButton) == 0
                || (statue != null && braceletLevel != 2)) {
            dependencies.puzzles().indoorPotPullFrames = 0;
            return true;
        }
        dependencies.followers().followingEntityYOffset = linkDirection == Link.DIRECTION_UP ? 4 : 0;
        dependencies.puzzles().indoorPotPullFrames++;
        int pullFrames = (statue != null ? anyPowerUpActive : pieceOfPowerActive) ? 0x03 : INDOOR_POT_PULL_FRAMES;
        if (dependencies.puzzles().indoorPotPullFrames < pullFrames) {
            return true;
        }
        dependencies.puzzles().indoorPotPullFrames = 0;

        if (statue != null) {
            dependencies
                    .entityBridge()
                    .get()
                    .entityRuntime
                    .beginLift(
                            statue.slot(), RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection));
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomEntityEvents
                    .add(new linksawakening.world.entity.event.EntityCombatEvent(
                            statue.slot(),
                            0x9D,
                            0,
                            false,
                            linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.WAVE,
                            2));
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityBridge()
                            .get()
                            .entityRuntime
                            .inspection()
                            .snapshot());
            return true;
        }
        if (outdoor) {
            if (touchedObject == 0x5C) {
                dependencies
                        .overworldBushInteraction()
                        .revealObjectAtLocation(
                                touchedLocation,
                                dependencies.location().activeRoom.roomId(),
                                true,
                                dependencies.location().activeRoom.roomObjectsArea(),
                                dependencies.location().activeRoom.renderValues(),
                                dependencies.location().activeRoom.gbcOverlay(),
                                dependencies.location().activeRoom.tileIds(),
                                dependencies.location().activeRoom.tileAttrs());
            } else {
                int room = dependencies.location().activeRoom.roomId();
                int replacement =
                        room < 0x20 || room == 0xE0 || room == 0xE1 || room == 0xE3 || room == 0xE4 || (room == 0x75)
                                ? 3
                                : 4;
                if (room == 0x52 || room == 0x04) replacement = 0xC6;
                dependencies.roomObjectMutations().get().writeBombPuzzleObject(touchedLocation, replacement);
            }
            dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
        } else {
            dependencies.location().activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + touchedLocation] =
                    touchedObject == OBJECT_POT_WITH_SWITCH ? OBJECT_SWITCH_BUTTON : 0x0D;
            dependencies.roomTileRedraw().get().refreshActiveRoomTilemap();
            dependencies
                    .overworldCollision()
                    .setRoom(dependencies.location().activeRoom.roomObjectsArea());
            dependencies.overworldCollision().setGbcOverlay(null);
        }
        int x = linkPixelX + 0x08;
        int y = linkPixelY + 0x10;
        dependencies
                .entityBridge()
                .get()
                .entityRuntime
                .spawnLiftedRoomObject(
                        x,
                        y,
                        RoomInteractionGeometry.romDirectionForProjectileCollision(linkDirection),
                        touchedObject == 0x5C ? 1 : 0);
        dependencies
                .location()
                .activeRoom
                .replaceEntities(dependencies
                        .entityBridge()
                        .get()
                        .entityRuntime
                        .inspection()
                        .snapshot());
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

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return dependencies
                .roomDoorInteractions()
                .get()
                .tryUnlockOverworldDungeonKeyhole(
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        collisionType,
                        ROOM_OW_TAIL_CAVE_ENTRANCE,
                        hasTailKey,
                        TAIL_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return dependencies
                .roomDoorInteractions()
                .get()
                .tryUnlockOverworldDungeonKeyhole(
                        linkPixelX,
                        linkPixelY,
                        linkDirection,
                        collisionType,
                        ROOM_OW_KEY_CAVERN_ENTRANCE,
                        (goldenLeavesCount & 0xFF) == 0x06,
                        SLIME_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockOverworldDungeonKeyhole(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            int collisionType,
            int roomId,
            boolean hasKey,
            int dialogLowId) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                || dependencies.location().activeRoom.roomId() != roomId
                || linkDirection != Link.DIRECTION_UP
                || (collisionType & LINK_COLLISION_TYPE_UP) == 0
                || (dependencies.roomState().overworldRoomStatus[roomId] & ROOM_STATUS_EVENT_1) != 0) {
            return false;
        }

        int collisionPointY = linkPixelY + 0x06;
        boolean leftProbeIsKeyhole =
                dependencies.overworldCollision().objectPhysicsFlagAtPoint(linkPixelX + 0x06, collisionPointY)
                        == PhysicsFlags.CAT_KEYHOLE;
        boolean rightProbeIsKeyhole =
                dependencies.overworldCollision().objectPhysicsFlagAtPoint(linkPixelX + 0x09, collisionPointY)
                        == PhysicsFlags.CAT_KEYHOLE;
        if (!leftProbeIsKeyhole && !rightProbeIsKeyhole) {
            return false;
        }
        if (!hasKey) {
            dependencies
                    .entityBridge()
                    .get()
                    .outbox
                    .pendingRoomDialogRequests
                    .add(new DialogRequest(KEYHOLE_DIALOG_TABLE, dialogLowId));
            return true;
        }

        dependencies.roomState().overworldRoomStatus[roomId] |= (byte) ROOM_STATUS_EVENT_1;
        dependencies.puzzles().tailCaveKeyholeCountdown = TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN;
        dependencies.puzzles().tailCaveResumeMusicTrack = dependencies.followers().entityDefaultMusicTrack;
        dependencies.entityBridge().get().outbox.pendingRoomMusicTrack = 0x00;
        return true;
    }

    int tickTailCaveKeyholeSequence() {
        dependencies.roomDoorInteractions().get().tickNextWorldMusicTrackCountdown();
        if (dependencies.puzzles().tailCaveKeyholeCountdown == 0) {
            dependencies.puzzles().tailCaveFinalMotionBlockPending = false;
            return 0;
        }
        dependencies.puzzles().tailCaveKeyholeCountdown = (dependencies.puzzles().tailCaveKeyholeCountdown - 1) & 0xFF;
        int countdown = dependencies.puzzles().tailCaveKeyholeCountdown;
        if (countdown == 0xDE) {
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        }
        if (countdown == 0xA0) {
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.OPEN_KEY_CAVERN);
        }
        if (countdown < 0x20 && (countdown & 0x0F) == 0x08) {
            dependencies.roomDoorInteractions().get().writeTailCaveGateAnimationFrame(countdown);
        }
        if (countdown == TAIL_CAVE_RUMBLE_GATE_FRAME) {
            int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + TAIL_CAVE_GATE_LOCATION;
            if (dependencies.location().activeRoom != null
                    && dependencies.location().activeRoom.roomObjectsArea()[gateIndex] == OBJECT_CLOSED_GATE) {
                dependencies.location().activeRoom.roomObjectsArea()[gateIndex] = OBJECT_CAVE_DOOR;
                // BackupObjectInRAM2($82): force-copy [hl], then return to bank 2.
                // $82 is a control argument, not the replacement object value.
                int overlayIndex = 0x10;
                dependencies.location().activeRoom.gbcOverlay()[overlayIndex] = OBJECT_CAVE_DOOR;
                dependencies.location().activeRoom.renderValues()[gateIndex] = OBJECT_CAVE_DOOR;
                // The source's draw command changes tile IDs only; keep attrs.
                dependencies.roomObjectMutations().get().refreshOverworldCollisionAfterObjectMutation();
            }
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DUNGEON_OPENED);
        }
        if (countdown == 0x0A) {
            dependencies.puzzles().nextWorldMusicTrackCountdown = 0x50;
        }
        if (countdown == 0) {
            dependencies.puzzles().tailCaveFinalMotionBlockPending = true;
        }
        if (countdown < 0x20 || countdown >= 0x9C) {
            return 0;
        }
        return (countdown & 0x04) == 0 ? 1 : -2;
    }

    boolean tailCaveKeyholeSequenceActive() {
        return dependencies.puzzles().tailCaveKeyholeCountdown != 0
                || dependencies.puzzles().tailCaveFinalMotionBlockPending;
    }

    int tailCaveKeyholeCountdownForTest() {
        return dependencies.puzzles().tailCaveKeyholeCountdown;
    }

    int nextWorldMusicTrackCountdownForTest() {
        return dependencies.puzzles().nextWorldMusicTrackCountdown;
    }

    void writeTailCaveGateAnimationFrame(int countdown) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.roomId() != ROOM_OW_TAIL_CAVE_ENTRANCE
                        && dependencies.location().activeRoom.roomId() != ROOM_OW_KEY_CAVERN_ENTRANCE) {
            return;
        }
        int frameIndex = (countdown >>> 3) & 0x02;
        int frameOffset = RomBank.romOffset(0x02, 0x5642) + frameIndex;
        int topTile = dependencies.romData()[frameOffset] & 0xFF;
        int bottomTile = dependencies.romData()[frameOffset + 1] & 0xFF;
        int tileX = (TAIL_CAVE_GATE_LOCATION & 0x0F) * 2;
        int tileY = (TAIL_CAVE_GATE_LOCATION >>> 4) * 2;
        int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
        int[] tiles = dependencies.location().activeRoom.tileIds();
        tiles[topLeft] = topTile;
        tiles[topLeft + 1] = topTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH] = bottomTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = bottomTile;
    }

    void tickNextWorldMusicTrackCountdown() {
        if (dependencies.puzzles().nextWorldMusicTrackCountdown == 0) {
            return;
        }
        dependencies.puzzles().nextWorldMusicTrackCountdown--;
        if (dependencies.puzzles().nextWorldMusicTrackCountdown == 0) {
            dependencies.entityBridge().get().outbox.pendingRoomMusicTrack =
                    dependencies.puzzles().tailCaveResumeMusicTrack;
        }
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            ChestContentsTable chestContentsTable,
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            byte[] romData,
            java.util.function.Supplier<RoomDoorInteractions> roomDoorInteractions,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            java.util.function.Supplier<RoomSessionViews> roomSessionViews,
            RoomWorldState roomState,
            java.util.function.Supplier<RoomTileRedraw> roomTileRedraw) {}
}
