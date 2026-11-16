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

import java.util.Objects;
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
final class RoomDoorInteractions implements RoomDoors {
    private final LoadedRoomHandle rooms;
    private final Dependencies dependencies;
    private final RoomChestInteractions roomChestInteractions;

    private final RoomLiftInteractions roomLiftInteractions;

    RoomDoorInteractions(LoadedRoomHandle rooms, Dependencies dependencies) {
        this.rooms = Objects.requireNonNull(rooms);
        this.dependencies = dependencies;
        roomChestInteractions = new RoomChestInteractions(new RoomChestInteractions.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.overworldCollision(),
                dependencies.romData(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomTileRedraw()));

        roomLiftInteractions = new RoomLiftInteractions(new RoomLiftInteractions.Dependencies(
                dependencies.entityRuntimeState(),
                dependencies.followers(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                this,
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw(),
                this));
    }

    @Override
    public ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, Direction linkDirection, boolean actionPressed, int swordLevel) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        return tryOpenChest(loaded, linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }

    @Override
    public boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryUnlockIndoorKeyDoor(loaded.contents, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryPushIndoorBlock(loaded, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryLiftObject(
            int x,
            int y,
            Direction direction,
            boolean held,
            int buttons,
            boolean power,
            int level,
            boolean anyPowerUp) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryLiftObject(loaded, x, y, direction, held, buttons, power, level, anyPowerUp);
    }

    @Override
    public boolean tryLiftObject(
            int x, int y, Direction direction, boolean held, int buttons, boolean power, int level) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryLiftObject(loaded, x, y, direction, held, buttons, power, level);
    }

    @Override
    public boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryLiftIndoorObject(
                loaded, linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask);
    }

    @Override
    public boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryLiftIndoorObject(
                loaded,
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive);
    }

    @Override
    public boolean tryInteractWithIndoorBlock(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryInteractWithIndoorBlock(loaded, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryUnlockIndoorKeyholeBlock(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryUnlockIndoorKeyholeBlock(loaded.contents, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, boolean hasTailKey) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryUnlockTailCaveKeyhole(
                loaded.contents, linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey);
    }

    @Override
    public boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, int goldenLeavesCount) {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tryUnlockKeyCavernKeyhole(
                loaded.contents, linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount);
    }

    @Override
    public int tickTailCaveKeyholeSequence() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return 0;
        }
        return tickTailCaveKeyholeSequence(loaded.contents);
    }

    @Override
    public boolean tailCaveKeyholeSequenceActive() {
        RoomRuntime loaded = rooms.currentOrNull();
        if (loaded == null) {
            return false;
        }
        return tailCaveKeyholeSequenceActive(loaded.contents);
    }

    ChestOpenResult tryOpenChest(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean actionPressed,
            int swordLevel) {
        return roomChestInteractions.tryOpenChest(
                loaded, linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }

    boolean tryUnlockIndoorKeyDoor(
            RoomContents contents, int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (contents.room.mapCategory() == Warp.CATEGORY_OVERWORLD
                || contents.room.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || contents.puzzles.indoorKeyDoorAnimationCountdown != 0) {
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
        int touchedObject = dependencies.roomSessionViews().objectAtRoomLocation(contents.room, touchedLocation);
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
            contents.puzzles.indoorBossDoorOpening = true;
            contents.puzzles.indoorKeyDoorDirection = 0;
            contents.puzzles.indoorKeyDoorLocation = touchedLocation - objectOffset;
            contents.puzzles.indoorKeyDoorAnimationCountdown = KEY_DOOR_ANIMATION_FRAMES;
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
            return true;
        }
        int objectOffset = touchedObject - KEY_DOOR_CLOSED_OBJECTS[doorDirection];
        if (objectOffset < 0 || objectOffset > 1) return false;
        int doorLocation = touchedLocation - (objectOffset == 0 ? 0 : doorDirection < 2 ? 1 : 0x10);
        if (!RoomInteractionGeometry.alignedWithIndoorKeyDoor(doorLocation, doorDirection, linkPixelX, linkPixelY)
                || !dependencies.roomState().dungeonItemState.consumeSmallKey()) return false;
        dependencies.roomSessionViews().indoorStatusTableForMap(contents.room.mapId())[contents.room.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        contents.puzzles.indoorKeyDoorDirection = doorDirection;
        contents.puzzles.indoorKeyDoorLocation = doorLocation;
        contents.puzzles.indoorKeyDoorAnimationCountdown = KEY_DOOR_ANIMATION_FRAMES;
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        return true;
    }

    boolean tryPushIndoorBlock(
            RoomRuntime loaded, int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (loaded.room.mapCategory() == Warp.CATEGORY_OVERWORLD
                || loaded.room.mapCategory() == Warp.CATEGORY_SIDESCROLL
                || loaded.contents.puzzles.pushedBlockMotionFrames != 0) {
            loaded.contents.puzzles.pushBlockContactTicks = 0;
            return false;
        }
        int direction = RoomInteractionGeometry.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & RoomInteractionGeometry.keyDoorCollisionBit(direction)) == 0) {
            loaded.contents.puzzles.pushBlockContactTicks = 0;
            return false;
        }
        var contact = probePushableBlockContact(loaded.contents, linkPixelX, linkPixelY, direction);
        int touchedLocation = contact.location();
        boolean keyholeBlockContact = contact.keyhole();
        if (touchedLocation < 0) {
            if (!keyholeBlockContact) {
                loaded.contents.puzzles.pushBlockContactTicks = 0;
            }
            return false;
        }
        loaded.contents.puzzles.pushBlockContactTicks++;
        if (loaded.contents.puzzles.pushBlockContactTicks < PUSH_BLOCK_CONTACT_TICKS) {
            return true;
        }
        loaded.contents.puzzles.pushBlockContactTicks = 0;
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
        int spawnedSlot = loaded.entities.spawns().spawnPushedBlock(sourceX, sourceY);
        if (spawnedSlot < 0) {
            return true;
        }
        // EntityInitPushedBlock $03:5198-519A, after collision/allocation succeed.
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.BLOCK_PUSH);
        loaded.room.roomObjectsArea()[sourceIndex] = OBJECT_FLOOR_OD;
        loaded.contents.puzzles.pushedBlockSourceLocation = touchedLocation;
        loaded.contents.puzzles.pushedBlockDestinationLocation = destination;
        loaded.contents.puzzles.pushedBlockDirection = direction;
        loaded.contents.puzzles.pushedBlockMotionFrames = PUSHED_BLOCK_MOTION_FRAMES;
        loaded.contents.puzzles.pushedBlockEntitySlot = spawnedSlot;
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(loaded.contents);
        dependencies.overworldCollision().setRoom(loaded.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);

        loaded.room.replaceEntities(loaded.entities.inspection().view());

        return true;
    }

    boolean tryLiftIndoorObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask) {
        return roomLiftInteractions.tryLiftIndoorObject(
                loaded, linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask);
    }

    boolean tryLiftIndoorObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return roomLiftInteractions.tryLiftIndoorObject(
                loaded,
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive);
    }

    boolean tryLiftObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel) {
        return roomLiftInteractions.tryLiftObject(
                loaded,
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive,
                braceletLevel);
    }

    boolean tryLiftObject(
            RoomRuntime loaded,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive,
            int braceletLevel,
            boolean anyPowerUpActive) {
        return roomLiftInteractions.tryLiftObject(
                loaded,
                linkPixelX,
                linkPixelY,
                linkDirection,
                braceletButtonHeld,
                pressedButtonsMask,
                pieceOfPowerActive,
                braceletLevel,
                anyPowerUpActive);
    }

    boolean tryInteractWithIndoorBlock(
            RoomRuntime loaded, int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (this.tryPushIndoorBlock(loaded, linkPixelX, linkPixelY, linkDirection, collisionType)) {
            return true;
        }
        return this.tryUnlockIndoorKeyholeBlock(loaded.contents, linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(
            RoomContents contents, int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType) {
        if (contents.room.mapCategory() == Warp.CATEGORY_OVERWORLD
                || contents.room.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            contents.puzzles.pushBlockContactTicks = 0;
            return false;
        }
        int direction = RoomInteractionGeometry.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & RoomInteractionGeometry.keyDoorCollisionBit(direction)) == 0) {
            contents.puzzles.pushBlockContactTicks = 0;
            return false;
        }
        int touchedLocation = -1;
        for (int[] point : RoomInteractionGeometry.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            if (dependencies.roomSessionViews().objectAtRoomLocation(contents.room, location) == OBJECT_KEYHOLE_BLOCK) {
                touchedLocation = location;
                break;
            }
        }
        if (touchedLocation < 0) {
            contents.puzzles.pushBlockContactTicks = 0;
            return false;
        }
        if (dependencies.roomState().dungeonItemState.currentFlag(DungeonItemState.SMALL_KEYS_INDEX) == 0) {
            contents.puzzles.pushBlockContactTicks = 0;
            dependencies.entityRuntimeState().outbox.pendingRoomDialogRequests.add(new DialogRequest(0, 0x8C));
            return true;
        }
        contents.puzzles.pushBlockContactTicks++;
        if (contents.puzzles.pushBlockContactTicks < KEYHOLE_BLOCK_CONTACT_TICKS) {
            return true;
        }
        contents.puzzles.pushBlockContactTicks = 0;
        dependencies.roomState().dungeonItemState.consumeSmallKey();

        dependencies.roomObjectMutations().writeBombPuzzleObject(contents, touchedLocation, OBJECT_FLOOR_OD);
        dependencies.roomSessionViews().indoorStatusTableForMap(contents.room.mapId())[contents.room.roomId()] |=
                (byte) INDOOR_ROOM_STATUS_EVENT_3;
        dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        dependencies.roomTileRedraw().refreshActiveRoomTilemap(contents);
        dependencies.overworldCollision().setRoom(contents.room.roomObjectsArea());
        dependencies.overworldCollision().setGbcOverlay(null);
        return true;
    }

    boolean tryUnlockTailCaveKeyhole(
            RoomContents contents,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            int collisionType,
            boolean hasTailKey) {
        return this.tryUnlockOverworldDungeonKeyhole(
                contents,
                linkPixelX,
                linkPixelY,
                linkDirection,
                collisionType,
                ROOM_OW_TAIL_CAVE_ENTRANCE,
                hasTailKey,
                TAIL_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockKeyCavernKeyhole(
            RoomContents contents,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            int collisionType,
            int goldenLeavesCount) {
        return this.tryUnlockOverworldDungeonKeyhole(
                contents,
                linkPixelX,
                linkPixelY,
                linkDirection,
                collisionType,
                ROOM_OW_KEY_CAVERN_ENTRANCE,
                (goldenLeavesCount & 0xFF) == 0x06,
                SLIME_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockOverworldDungeonKeyhole(
            RoomContents contents,
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            int collisionType,
            int roomId,
            boolean hasKey,
            int dialogLowId) {
        if (contents.room.mapCategory() != Warp.CATEGORY_OVERWORLD
                || contents.room.roomId() != roomId
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
        contents.puzzles.tailCaveKeyholeCountdown = TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN;
        dependencies.puzzles().tailCaveResumeMusicTrack = dependencies.followers().entityDefaultMusicTrack;
        dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack = 0x00;
        return true;
    }

    int tickTailCaveKeyholeSequence(RoomContents contents) {
        this.tickNextWorldMusicTrackCountdown(contents);
        if (contents.puzzles.tailCaveKeyholeCountdown == 0) {
            contents.puzzles.tailCaveFinalMotionBlockPending = false;
            return 0;
        }
        contents.puzzles.tailCaveKeyholeCountdown = (contents.puzzles.tailCaveKeyholeCountdown - 1) & 0xFF;
        int countdown = contents.puzzles.tailCaveKeyholeCountdown;
        if (countdown == 0xDE) {
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        }
        if (countdown == 0xA0) {
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.OPEN_KEY_CAVERN);
        }
        if (countdown < 0x20 && (countdown & 0x0F) == 0x08) {
            this.writeTailCaveGateAnimationFrame(contents, countdown);
        }
        if (countdown == TAIL_CAVE_RUMBLE_GATE_FRAME) {
            int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + TAIL_CAVE_GATE_LOCATION;
            if (contents.room.roomObjectsArea()[gateIndex] == OBJECT_CLOSED_GATE) {
                contents.room.roomObjectsArea()[gateIndex] = OBJECT_CAVE_DOOR;
                // BackupObjectInRAM2($82): force-copy [hl], then return to bank 2.
                // $82 is a control argument, not the replacement object value.
                int overlayIndex = 0x10;
                contents.room.gbcOverlay()[overlayIndex] = OBJECT_CAVE_DOOR;
                contents.room.renderValues()[gateIndex] = OBJECT_CAVE_DOOR;
                // The source's draw command changes tile IDs only; keep attrs.
                dependencies.roomObjectMutations().refreshOverworldCollisionAfterObjectMutation(contents);
            }
            dependencies.entityFrame().colorShellSoundSink.play(GameplaySoundEvent.DUNGEON_OPENED);
        }
        if (countdown == 0x0A) {
            contents.puzzles.nextWorldMusicTrackCountdown = 0x50;
        }
        if (countdown == 0) {
            contents.puzzles.tailCaveFinalMotionBlockPending = true;
        }
        if (countdown < 0x20 || countdown >= 0x9C) {
            return 0;
        }
        return (countdown & 0x04) == 0 ? 1 : -2;
    }

    boolean tailCaveKeyholeSequenceActive(RoomContents contents) {
        return contents.puzzles.tailCaveKeyholeCountdown != 0 || contents.puzzles.tailCaveFinalMotionBlockPending;
    }

    int tailCaveKeyholeCountdownForTest(RoomContents contents) {
        return contents.puzzles.tailCaveKeyholeCountdown;
    }

    int nextWorldMusicTrackCountdownForTest(RoomContents contents) {
        return contents.puzzles.nextWorldMusicTrackCountdown;
    }

    void writeTailCaveGateAnimationFrame(RoomContents contents, int countdown) {
        if (contents.room.roomId() != ROOM_OW_TAIL_CAVE_ENTRANCE
                && contents.room.roomId() != ROOM_OW_KEY_CAVERN_ENTRANCE) {
            return;
        }
        int frameIndex = (countdown >>> 3) & 0x02;
        int frameOffset = RomBank.romOffset(0x02, 0x5642) + frameIndex;
        int topTile = dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(frameOffset));
        int bottomTile = dependencies.romData().u8(linksawakening.rom.RomAddress.fromOffset(frameOffset + 1));
        int tileX = (TAIL_CAVE_GATE_LOCATION & 0x0F) * 2;
        int tileY = (TAIL_CAVE_GATE_LOCATION >>> 4) * 2;
        int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
        int[] tiles = contents.room.tileIds();
        tiles[topLeft] = topTile;
        tiles[topLeft + 1] = topTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH] = bottomTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = bottomTile;
    }

    void tickNextWorldMusicTrackCountdown(RoomContents contents) {
        if (contents.puzzles.nextWorldMusicTrackCountdown == 0) {
            return;
        }
        contents.puzzles.nextWorldMusicTrackCountdown--;
        if (contents.puzzles.nextWorldMusicTrackCountdown == 0) {
            dependencies.entityRuntimeState().outbox.pendingRoomMusicTrack =
                    dependencies.puzzles().tailCaveResumeMusicTrack;
        }
    }

    record Dependencies(
            RoomEntitySessionState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            RoomPuzzleState puzzles,
            ChestContentsTable chestContentsTable,
            RoomEntitySessionState entityRuntimeState,
            OverworldBushInteraction overworldBushInteraction,
            OverworldCollision overworldCollision,
            Rom romData,
            RoomObjectMutations roomObjectMutations,
            RoomSessionViews roomSessionViews,
            RoomWorldState roomState,
            RoomObjectMutations roomTileRedraw) {
        Dependencies {
            Objects.requireNonNull(entityFrame, "entityFrame");
            Objects.requireNonNull(followers, "followers");
            Objects.requireNonNull(location, "location");
            Objects.requireNonNull(puzzles, "puzzles");
            Objects.requireNonNull(chestContentsTable, "chestContentsTable");
            Objects.requireNonNull(entityRuntimeState, "entityRuntimeState");
            Objects.requireNonNull(overworldBushInteraction, "overworldBushInteraction");
            Objects.requireNonNull(overworldCollision, "overworldCollision");
            Objects.requireNonNull(romData, "romData");
            Objects.requireNonNull(roomObjectMutations, "roomObjectMutations");
            Objects.requireNonNull(roomSessionViews, "roomSessionViews");
            Objects.requireNonNull(roomState, "roomState");
            Objects.requireNonNull(roomTileRedraw, "roomTileRedraw");
        }
    }

    private static final int BOSS_DOOR_PHYSICS = 0x98;

    private static final int BOSS_DOOR_LEFT_OBJECT = 0xA4;

    private BlockContact probePushableBlockContact(
            RoomContents contents, int linkPixelX, int linkPixelY, int direction) {
        int touchedLocation = -1;
        boolean keyholeBlockContact = false;
        for (int[] point : RoomInteractionGeometry.keyDoorCollisionPoints(linkPixelX, linkPixelY, direction)) {
            int column = Math.floorDiv(point[0], 0x10);
            int row = Math.floorDiv(point[1], 0x10);
            int location = (row << 4) | column;
            int object = dependencies.roomSessionViews().objectAtRoomLocation(contents.room, location);
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
