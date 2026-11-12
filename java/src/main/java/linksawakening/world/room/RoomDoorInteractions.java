package linksawakening.world.room;

import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.room.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.room.RoomSession.*;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.transition.Warp;

/** RoomDoorInteractions operations for the live room session. */
final class RoomDoorInteractions {
    private final RoomSession session;

    RoomDoorInteractions(RoomSession session) {
        this.session = session;
    }

    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        if (!actionPressed
                || linkDirection != Link.DIRECTION_UP
                || session.context.activeRoom == null
                || session.context.entityBridge.entityRuntime == null) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        if (linkPixelX < 0 || linkPixelY < 0 || linkPixelX >= ROOM_PIXEL_WIDTH || linkPixelY >= ROOM_PIXEL_HEIGHT) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int column = Math.floorDiv(linkPixelX + (session.context.romData[0x1F4B] & 0xFF), 0x10);
        int row = Math.floorDiv(linkPixelY + (session.context.romData[0x1F4F] & 0xFF), 0x10);
        if (column < 0
                || column >= RoomConstants.OBJECTS_PER_ROW
                || row < 0
                || row >= RoomConstants.OBJECTS_PER_COLUMN) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int location = (row << 4) | column;
        if (session.objectAtRoomLocation(location) != 0xA0) {
            return new ChestOpenResult(false, -1, -1, location);
        }
        int itemType = session.context.chestContentsTable.itemForSpawn(
                session.context.activeRoom.mapId(),
                session.context.activeRoom.roomId(),
                swordLevel,
                session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD);
        int slot = session.context.entityBridge.entityRuntime.spawnChestWithItem(column << 4, row << 4, itemType);
        if (slot < 0) {
            return new ChestOpenResult(false, -1, -1, location);
        }

        session.writeBombPuzzleObject(location, 0xA1);
        byte[] status = session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                ? session.context.roomState.overworldRoomStatus
                : session.indoorStatusTableForMap(session.context.activeRoom.mapId());
        status[session.context.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_CHEST_OPEN;
        // writeBombPuzzleObject already updates the live GBC cell. Rebuilding
        // from the ROM overlay would discard other live mutations (cut bushes).
        if (session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            session.refreshActiveRoomTilemap();
        }
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(
                session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                        ? session.context.activeRoom.gbcOverlay()
                        : null);
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        return new ChestOpenResult(true, itemType, slot, location);
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.context.activeRoom == null
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || session.context.indoorKeyDoorAnimationCountdown != 0) {
            return false;
        }
        int doorDirection = session.keyDoorDirectionIndex(linkDirection);
        if (doorDirection < 0 || (collisionType & session.keyDoorCollisionBit(doorDirection)) == 0) {
            return false;
        }
        int expectedPhysics = session.KEY_DOOR_PHYSICS_BASE + doorDirection;
        int touchedLocation = -1;
        boolean bossDoor = false;
        for (int[] point : session.keyDoorCollisionPoints(linkPixelX, linkPixelY, doorDirection)) {
            int physics = session.context.overworldCollision.objectPhysicsFlagAtPoint(point[0], point[1]);
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
        int touchedObject = session.objectAtRoomLocation(touchedLocation);
        if (bossDoor) {
            int objectOffset = touchedObject - 0xA4;
            if (objectOffset < 0
                    || objectOffset > 1
                    || !session.alignedWithIndoorKeyDoor(touchedLocation - objectOffset, 0, linkPixelX, linkPixelY)) {
                return false;
            }
            if (session.context.roomState.dungeonItemState.currentFlag(DungeonItemState.NIGHTMARE_KEY_INDEX) == 0) {
                session.context.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0x07));
                return true;
            }
            session.context.indoorBossDoorOpening = true;
            session.context.indoorKeyDoorDirection = 0;
            session.context.indoorKeyDoorLocation = touchedLocation - objectOffset;
            session.context.indoorKeyDoorAnimationCountdown = session.KEY_DOOR_ANIMATION_FRAMES;
            session.context.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
            return true;
        }
        int objectOffset = touchedObject - session.KEY_DOOR_CLOSED_OBJECTS[doorDirection];
        if (objectOffset < 0 || objectOffset > 1) return false;
        int doorLocation = touchedLocation - (objectOffset == 0 ? 0 : doorDirection < 2 ? 1 : 0x10);
        if (!session.alignedWithIndoorKeyDoor(doorLocation, doorDirection, linkPixelX, linkPixelY)
                || !session.context.roomState.dungeonItemState.consumeSmallKey()) return false;
        session.indoorStatusTableForMap(session.context.activeRoom.mapId())[session.context.activeRoom.roomId()] |=
                (byte) session.INDOOR_ROOM_STATUS_EVENT_3;
        session.context.indoorKeyDoorDirection = doorDirection;
        session.context.indoorKeyDoorLocation = doorLocation;
        session.context.indoorKeyDoorAnimationCountdown = session.KEY_DOOR_ANIMATION_FRAMES;
        session.context.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        return true;
    }

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.context.activeRoom == null
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || session.context.pushedBlockMotionFrames != 0) {
            session.context.pushBlockContactTicks = 0;
            return false;
        }
        int direction = session.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & session.keyDoorCollisionBit(direction)) == 0) {
            session.context.pushBlockContactTicks = 0;
            return false;
        }
        int touchedLocation = -1;
        boolean keyholeBlockContact = false;
        for (int[] point : session.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            int object = session.objectAtRoomLocation(location);
            if (object == session.OBJECT_PUSHABLE_BLOCK) {
                touchedLocation = location;
                break;
            }
            keyholeBlockContact |= object == session.OBJECT_KEYHOLE_BLOCK;
        }
        if (touchedLocation < 0) {
            if (!keyholeBlockContact) {
                session.context.pushBlockContactTicks = 0;
            }
            return false;
        }
        session.context.pushBlockContactTicks++;
        if (session.context.pushBlockContactTicks < session.PUSH_BLOCK_CONTACT_TICKS) {
            return true;
        }
        session.context.pushBlockContactTicks = 0;
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
                session.context.overworldCollision.objectPhysicsFlagAtEntityPosition(destinationX, destinationY))) {
            return true;
        }
        int sourceX = ((touchedLocation & 0x0F) << 4) + 0x08;
        int sourceY = (touchedLocation & 0xF0) + 0x10;
        int spawnedSlot = session.context.entityBridge.entityRuntime == null
                ? -1
                : session.context.entityBridge.entityRuntime.spawnPushedBlock(sourceX, sourceY);
        if (spawnedSlot < 0) {
            return true;
        }
        // EntityInitPushedBlock $03:5198-519A, after collision/allocation succeed.
        session.context.colorShellSoundSink.play(GameplaySoundEvent.BLOCK_PUSH);
        session.context.activeRoom.roomObjectsArea()[sourceIndex] = session.OBJECT_FLOOR_OD;
        session.context.pushedBlockSourceLocation = touchedLocation;
        session.context.pushedBlockDestinationLocation = destination;
        session.context.pushedBlockDirection = direction;
        session.context.pushedBlockMotionFrames = session.PUSHED_BLOCK_MOTION_FRAMES;
        session.context.pushedBlockEntitySlot = spawnedSlot;
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
        if (session.context.entityBridge.entityRuntime != null) {
            session.context.activeRoom.replaceEntities(
                    session.context.entityBridge.entityRuntime.inspection().snapshot());
        }
        return true;
    }

    boolean tryLiftIndoorObject(
            int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) {
        return session.tryLiftIndoorObject(
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
        if (session.context.activeRoom == null
                || session.context.entityBridge.entityRuntime == null
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || !braceletButtonHeld
                || session.context
                        .entityBridge
                        .entityRuntime
                        .inspection()
                        .liftedEntityState()
                        .active()) {
            session.context.indoorPotPullFrames = 0;
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
        int touchedObject = session.objectAtRoomLocation(touchedLocation);
        boolean outdoor = session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD;
        // Statue standing handler, $19:4076: asymmetric 32 by 48 contact window.
        linksawakening.world.entity.RoomEntity statue = null;
        if (!outdoor) {
            for (var entity : session.context
                    .entityBridge
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
                && touchedObject != session.OBJECT_LIFTABLE_POT
                && !(outdoor && touchedObject == 0x5C)
                && touchedObject != session.OBJECT_POT_WITH_SWITCH) {
            session.context.indoorPotPullFrames = 0;
            return false;
        }
        if (oppositeButton == 0
                || (pressedButtonsMask & oppositeButton) == 0
                || (statue != null && braceletLevel != 2)) {
            session.context.indoorPotPullFrames = 0;
            return true;
        }
        session.context.followingEntityYOffset = linkDirection == Link.DIRECTION_UP ? 4 : 0;
        session.context.indoorPotPullFrames++;
        int pullFrames =
                (statue != null ? anyPowerUpActive : pieceOfPowerActive) ? 0x03 : session.INDOOR_POT_PULL_FRAMES;
        if (session.context.indoorPotPullFrames < pullFrames) {
            return true;
        }
        session.context.indoorPotPullFrames = 0;

        if (statue != null) {
            session.context.entityBridge.entityRuntime.beginLift(
                    statue.slot(), session.romDirectionForProjectileCollision(linkDirection));
            session.context.entityBridge.pendingRoomEntityEvents.add(
                    new linksawakening.world.entity.event.EntityCombatEvent(
                            statue.slot(),
                            0x9D,
                            0,
                            false,
                            linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.WAVE,
                            2));
            session.context.activeRoom.replaceEntities(
                    session.context.entityBridge.entityRuntime.inspection().snapshot());
            return true;
        }
        if (outdoor) {
            if (touchedObject == 0x5C) {
                session.context.overworldBushInteraction.revealObjectAtLocation(
                        touchedLocation,
                        session.context.activeRoom.roomId(),
                        true,
                        session.context.activeRoom.roomObjectsArea(),
                        session.context.activeRoom.renderValues(),
                        session.context.activeRoom.gbcOverlay(),
                        session.context.activeRoom.tileIds(),
                        session.context.activeRoom.tileAttrs());
            } else {
                int room = session.context.activeRoom.roomId();
                int replacement =
                        room < 0x20 || room == 0xE0 || room == 0xE1 || room == 0xE3 || room == 0xE4 || (room == 0x75)
                                ? 3
                                : 4;
                if (room == 0x52 || room == 0x04) replacement = 0xC6;
                session.writeBombPuzzleObject(touchedLocation, replacement);
            }
            session.refreshOverworldCollisionAfterObjectMutation();
        } else {
            session.context.activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + touchedLocation] =
                    touchedObject == session.OBJECT_POT_WITH_SWITCH ? session.OBJECT_SWITCH_BUTTON : 0x0D;
            session.refreshActiveRoomTilemap();
            session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
            session.context.overworldCollision.setGbcOverlay(null);
        }
        int x = linkPixelX + 0x08;
        int y = linkPixelY + 0x10;
        session.context.entityBridge.entityRuntime.spawnLiftedRoomObject(
                x, y, session.romDirectionForProjectileCollision(linkDirection), touchedObject == 0x5C ? 1 : 0);
        session.context.activeRoom.replaceEntities(
                session.context.entityBridge.entityRuntime.inspection().snapshot());
        return true;
    }

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType)) {
            return true;
        }
        return session.tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.context.activeRoom == null
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
                || session.context.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            session.context.pushBlockContactTicks = 0;
            return false;
        }
        int direction = session.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & session.keyDoorCollisionBit(direction)) == 0) {
            session.context.pushBlockContactTicks = 0;
            return false;
        }
        int touchedLocation = -1;
        for (int[] point : session.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            if (session.objectAtRoomLocation(location) == session.OBJECT_KEYHOLE_BLOCK) {
                touchedLocation = location;
                break;
            }
        }
        if (touchedLocation < 0) {
            session.context.pushBlockContactTicks = 0;
            return false;
        }
        if (session.context.roomState.dungeonItemState.currentFlag(DungeonItemState.SMALL_KEYS_INDEX) == 0) {
            session.context.pushBlockContactTicks = 0;
            session.context.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0x8C));
            return true;
        }
        session.context.pushBlockContactTicks++;
        if (session.context.pushBlockContactTicks < session.KEYHOLE_BLOCK_CONTACT_TICKS) {
            return true;
        }
        session.context.pushBlockContactTicks = 0;
        session.context.roomState.dungeonItemState.consumeSmallKey();

        session.writeBombPuzzleObject(touchedLocation, session.OBJECT_FLOOR_OD);
        session.indoorStatusTableForMap(session.context.activeRoom.mapId())[session.context.activeRoom.roomId()] |=
                (byte) session.INDOOR_ROOM_STATUS_EVENT_3;
        session.context.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        session.refreshActiveRoomTilemap();
        session.context.overworldCollision.setRoom(session.context.activeRoom.roomObjectsArea());
        session.context.overworldCollision.setGbcOverlay(null);
        return true;
    }

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return session.tryUnlockOverworldDungeonKeyhole(
                linkPixelX,
                linkPixelY,
                linkDirection,
                collisionType,
                session.ROOM_OW_TAIL_CAVE_ENTRANCE,
                hasTailKey,
                session.TAIL_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return session.tryUnlockOverworldDungeonKeyhole(
                linkPixelX,
                linkPixelY,
                linkDirection,
                collisionType,
                session.ROOM_OW_KEY_CAVERN_ENTRANCE,
                (goldenLeavesCount & 0xFF) == 0x06,
                session.SLIME_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockOverworldDungeonKeyhole(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            int collisionType,
            int roomId,
            boolean hasKey,
            int dialogLowId) {
        if (session.context.activeRoom == null
                || session.context.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                || session.context.activeRoom.roomId() != roomId
                || linkDirection != Link.DIRECTION_UP
                || (collisionType & session.LINK_COLLISION_TYPE_UP) == 0
                || (session.context.roomState.overworldRoomStatus[roomId] & session.ROOM_STATUS_EVENT_1) != 0) {
            return false;
        }

        int collisionPointY = linkPixelY + 0x06;
        boolean leftProbeIsKeyhole =
                session.context.overworldCollision.objectPhysicsFlagAtPoint(linkPixelX + 0x06, collisionPointY)
                        == PhysicsFlags.CAT_KEYHOLE;
        boolean rightProbeIsKeyhole =
                session.context.overworldCollision.objectPhysicsFlagAtPoint(linkPixelX + 0x09, collisionPointY)
                        == PhysicsFlags.CAT_KEYHOLE;
        if (!leftProbeIsKeyhole && !rightProbeIsKeyhole) {
            return false;
        }
        if (!hasKey) {
            session.context.entityBridge.pendingRoomDialogRequests.add(
                    new DialogRequest(session.KEYHOLE_DIALOG_TABLE, dialogLowId));
            return true;
        }

        session.context.roomState.overworldRoomStatus[roomId] |= (byte) session.ROOM_STATUS_EVENT_1;
        session.context.tailCaveKeyholeCountdown = session.TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN;
        session.context.tailCaveResumeMusicTrack = session.context.entityDefaultMusicTrack;
        session.context.entityBridge.pendingRoomMusicTrack = 0x00;
        return true;
    }

    int tickTailCaveKeyholeSequence() {
        session.tickNextWorldMusicTrackCountdown();
        if (session.context.tailCaveKeyholeCountdown == 0) {
            session.context.tailCaveFinalMotionBlockPending = false;
            return 0;
        }
        session.context.tailCaveKeyholeCountdown = (session.context.tailCaveKeyholeCountdown - 1) & 0xFF;
        int countdown = session.context.tailCaveKeyholeCountdown;
        if (countdown == 0xDE) {
            session.context.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        }
        if (countdown == 0xA0) {
            session.context.colorShellSoundSink.play(GameplaySoundEvent.OPEN_KEY_CAVERN);
        }
        if (countdown < 0x20 && (countdown & 0x0F) == 0x08) {
            session.writeTailCaveGateAnimationFrame(countdown);
        }
        if (countdown == session.TAIL_CAVE_RUMBLE_GATE_FRAME) {
            int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + session.TAIL_CAVE_GATE_LOCATION;
            if (session.context.activeRoom != null
                    && session.context.activeRoom.roomObjectsArea()[gateIndex] == session.OBJECT_CLOSED_GATE) {
                session.context.activeRoom.roomObjectsArea()[gateIndex] = session.OBJECT_CAVE_DOOR;
                // BackupObjectInRAM2($82): force-copy [hl], then return to bank 2.
                // $82 is a control argument, not the replacement object value.
                int overlayIndex = 0x10;
                session.context.activeRoom.gbcOverlay()[overlayIndex] = session.OBJECT_CAVE_DOOR;
                session.context.activeRoom.renderValues()[gateIndex] = session.OBJECT_CAVE_DOOR;
                // The source's draw command changes tile IDs only; keep attrs.
                session.refreshOverworldCollisionAfterObjectMutation();
            }
            session.context.colorShellSoundSink.play(GameplaySoundEvent.DUNGEON_OPENED);
        }
        if (countdown == 0x0A) {
            session.context.nextWorldMusicTrackCountdown = 0x50;
        }
        if (countdown == 0) {
            session.context.tailCaveFinalMotionBlockPending = true;
        }
        if (countdown < 0x20 || countdown >= 0x9C) {
            return 0;
        }
        return (countdown & 0x04) == 0 ? 1 : -2;
    }

    boolean tailCaveKeyholeSequenceActive() {
        return session.context.tailCaveKeyholeCountdown != 0 || session.context.tailCaveFinalMotionBlockPending;
    }

    int tailCaveKeyholeCountdownForTest() {
        return session.context.tailCaveKeyholeCountdown;
    }

    int nextWorldMusicTrackCountdownForTest() {
        return session.context.nextWorldMusicTrackCountdown;
    }

    void writeTailCaveGateAnimationFrame(int countdown) {
        if (session.context.activeRoom == null
                || session.context.activeRoom.roomId() != session.ROOM_OW_TAIL_CAVE_ENTRANCE
                        && session.context.activeRoom.roomId() != session.ROOM_OW_KEY_CAVERN_ENTRANCE) {
            return;
        }
        int frameIndex = (countdown >>> 3) & 0x02;
        int frameOffset = RomBank.romOffset(0x02, 0x5642) + frameIndex;
        int topTile = session.context.romData[frameOffset] & 0xFF;
        int bottomTile = session.context.romData[frameOffset + 1] & 0xFF;
        int tileX = (session.TAIL_CAVE_GATE_LOCATION & 0x0F) * 2;
        int tileY = (session.TAIL_CAVE_GATE_LOCATION >>> 4) * 2;
        int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
        int[] tiles = session.context.activeRoom.tileIds();
        tiles[topLeft] = topTile;
        tiles[topLeft + 1] = topTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH] = bottomTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = bottomTile;
    }

    void tickNextWorldMusicTrackCountdown() {
        if (session.context.nextWorldMusicTrackCountdown == 0) {
            return;
        }
        session.context.nextWorldMusicTrackCountdown--;
        if (session.context.nextWorldMusicTrackCountdown == 0) {
            session.context.entityBridge.pendingRoomMusicTrack = session.context.tailCaveResumeMusicTrack;
        }
    }
}
