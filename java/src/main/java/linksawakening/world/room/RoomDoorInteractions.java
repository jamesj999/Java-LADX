package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.INDOOR_ROOM_STATUS_EVENT_3;
import static linksawakening.world.room.RoomSessionRules.KEYHOLE_BLOCK_CONTACT_TICKS;
import static linksawakening.world.room.RoomSessionRules.KEYHOLE_DIALOG_TABLE;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_ANIMATION_FRAMES;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_CLOSED_OBJECTS;
import static linksawakening.world.room.RoomSessionRules.KEY_DOOR_PHYSICS_BASE;
import static linksawakening.world.room.RoomSessionRules.LINK_COLLISION_TYPE_UP;
import static linksawakening.world.room.RoomSessionRules.OBJECT_CAVE_DOOR;
import static linksawakening.world.room.RoomSessionRules.OBJECT_CLOSED_GATE;
import static linksawakening.world.room.RoomSessionRules.OBJECT_FLOOR_OD;
import static linksawakening.world.room.RoomSessionRules.OBJECT_KEYHOLE_BLOCK;
import static linksawakening.world.room.RoomSessionRules.OBJECT_PUSHABLE_BLOCK;
import static linksawakening.world.room.RoomSessionRules.PUSHED_BLOCK_MOTION_FRAMES;
import static linksawakening.world.room.RoomSessionRules.PUSH_BLOCK_CONTACT_TICKS;
import static linksawakening.world.room.RoomSessionRules.ROOM_OW_KEY_CAVERN_ENTRANCE;
import static linksawakening.world.room.RoomSessionRules.ROOM_OW_TAIL_CAVE_ENTRANCE;
import static linksawakening.world.room.RoomSessionRules.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomSessionRules.SLIME_KEYHOLE_DIALOG_ID;
import static linksawakening.world.room.RoomSessionRules.TAIL_CAVE_GATE_LOCATION;
import static linksawakening.world.room.RoomSessionRules.TAIL_CAVE_RUMBLE_GATE_FRAME;
import static linksawakening.world.room.RoomSessionRules.TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN;
import static linksawakening.world.room.RoomSessionRules.TAIL_KEYHOLE_DIALOG_ID;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import linksawakening.state.Direction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.object.OverworldBushInteraction;
import linksawakening.world.transition.Warp;

/** ApplyLinkMotionState indoor key blocks and dungeon entrance interactions. */
final class RoomDoorInteractions {
    private final Dependencies dependencies;
    private final RoomChestInteractions roomChestInteractions;

    private final RoomLiftInteractions roomLiftInteractions;

    RoomDoorInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomChestInteractions = new RoomChestInteractions(new RoomChestInteractions.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.entityRuntimeState(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.romData(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomTileRedraw()));

        roomLiftInteractions = new RoomLiftInteractions(new RoomLiftInteractions.Dependencies(
                dependencies.entityRuntimeState(),
                dependencies.followers(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                this,
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw(),
                this));
    }

    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, Direction linkDirection, boolean actionPressed, int swordLevel) {
        return roomChestInteractions.tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
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
            if (physics == expectedPhysics || (doorDirection == 0 && physics == BOSS_DOOR_PHYSICS)) {
                int column = Math.floorDiv(point[0], 0x10);
                int row = Math.floorDiv(point[1], 0x10);
                touchedLocation = (row << 4) | column;
                bossDoor = physics == BOSS_DOOR_PHYSICS;
                break;
            }
        }
        if (touchedLocation < 0) {
            return false;
        }
        int touchedObject = dependencies.roomSessionViews().objectAtRoomLocation(touchedLocation);
        if (bossDoor) {
            int objectOffset = touchedObject - BOSS_DOOR_LEFT_OBJECT;
            if (objectOffset < 0
                    || objectOffset > 1
                    || !RoomInteractionGeometry.alignedWithIndoorKeyDoor(
                            touchedLocation - objectOffset, 0, linkPixelX, linkPixelY)) {
                return false;
            }
            if (dependencies.roomState().dungeonItemState.currentFlag(DungeonItemState.NIGHTMARE_KEY_INDEX) == 0) {
                dependencies
                        .entityRuntimeState()
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
        dependencies.roomSessionViews()
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

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
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
        int spawnedSlot = dependencies.entityRuntimeState().runtime == null
                ? -1
                : dependencies.entityRuntimeState().runtime.spawnPushedBlock(sourceX, sourceY);
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
        dependencies.roomTileRedraw().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        if (dependencies.entityRuntimeState().runtime != null) {
            dependencies
                    .location()
                    .activeRoom
                    .replaceEntities(dependencies
                            .entityRuntimeState()
                            .runtime
                            .inspection()
                            .view());
        }
        return true;
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask) {
        return roomLiftInteractions.tryLiftIndoorObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return roomLiftInteractions.tryLiftIndoorObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel) {
        return roomLiftInteractions.tryLiftObject(
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive,
                braceletLevel);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {
        return roomLiftInteractions.tryLiftObject(
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive,
                braceletLevel,
                anyPowerUpActive);
    }

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (this.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType)) {
            return true;
        }
        return this.tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
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
            if (dependencies.roomSessionViews().objectAtRoomLocation(location) == OBJECT_KEYHOLE_BLOCK) {
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
            dependencies.entityRuntimeState().outbox.pendingRoomDialogRequests.add(new DialogRequest(0, 0x8C));
            return true;
        }
        dependencies.puzzles().pushBlockContactTicks++;
        if (dependencies.puzzles().pushBlockContactTicks < KEYHOLE_BLOCK_CONTACT_TICKS) {
            return true;
        }
        dependencies.puzzles().pushBlockContactTicks = 0;
        dependencies.roomState().dungeonItemState.consumeSmallKey();

        dependencies.roomObjectMutations().writeBombPuzzleObject(touchedLocation, OBJECT_FLOOR_OD);
        dependencies.roomSessionViews()
                        .indoorStatusTableForMap(
                                dependencies.location().activeRoom.mapId())[
                        dependencies.location().activeRoom.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        dependencies.roomTileRedraw().refreshActiveRoomTilemap();
        dependencies
                .overworldCollision()
                .setRoom(dependencies.location().activeRoom.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        return true;
    }

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, boolean hasTailKey) {
        return this.tryUnlockOverworldDungeonKeyhole(
                linkPixelX,
                linkPixelY,
                linkDirection,
                collisionType,
                ROOM_OW_TAIL_CAVE_ENTRANCE,
                hasTailKey,
                TAIL_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, int goldenLeavesCount) {
        return this.tryUnlockOverworldDungeonKeyhole(
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
            Direction linkDirection,
            int collisionType,
            int roomId,
            boolean hasKey,
            int dialogLowId) {
        if (dependencies.location().activeRoom == null
                || dependencies.location().activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
                || dependencies.location().activeRoom.roomId() != roomId
                || linkDirection != Direction.UP
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
                    .entityRuntimeState()
                    .outbox
                    .pendingRoomDialogRequests
                    .add(new DialogRequest(KEYHOLE_DIALOG_TABLE, dialogLowId));
            return true;
        }

        dependencies.roomState().overworldRoomStatus[roomId] |= (byte) ROOM_STATUS_EVENT_1;
        dependencies.puzzles().tailCaveKeyholeCountdown = TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN;
        dependencies.puzzles().tailCaveResumeMusicTrack = dependencies.followers().entityDefaultMusicTrack;
        dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack = 0x00;
        return true;
    }

    int tickTailCaveKeyholeSequence() {
        this.tickNextWorldMusicTrackCountdown();
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
            this.writeTailCaveGateAnimationFrame(countdown);
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
                dependencies.roomObjectMutations().refreshOverworldCollisionAfterObjectMutation();
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
        int topTile = dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(frameOffset));
        int bottomTile = dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(frameOffset + 1));
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
            dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack =
                    dependencies.puzzles().tailCaveResumeMusicTrack;
        }
    }

    record Dependencies(
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            ChestContentsTable chestContentsTable,
            RoomEntityFrameState entityRuntimeState,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            Rom romData,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomObjectMutations roomTileRedraw) {
        Dependencies {
            java.util.Objects.requireNonNull(entityFrame, "entityFrame");
            java.util.Objects.requireNonNull(followers, "followers");
            java.util.Objects.requireNonNull(location, "location");
            java.util.Objects.requireNonNull(puzzles, "puzzles");
            java.util.Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            java.util.Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            java.util.Objects.requireNonNull(overworldBushInteraction, "overworldBushInteraction");
            java.util.Objects.requireNonNull(overworldCollision, "overworldCollision");
            java.util.Objects.requireNonNull(romData, "romData");
            java.util.Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            java.util.Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            java.util.Objects.requireNonNull(roomState, "roomState");
            java.util.Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
        }
    }

    private static final int BOSS_DOOR_PHYSICS = 0x98;

    private static final int BOSS_DOOR_LEFT_OBJECT = 0xA4;

    private BlockContact probePushableBlockContact(int linkPixelX, int linkPixelY, int direction) {
        int touchedLocation = -1;
        boolean keyholeBlockContact = false;
        for (int[] point : RoomInteractionGeometry.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            int object = dependencies.roomSessionViews().objectAtRoomLocation(location);
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
