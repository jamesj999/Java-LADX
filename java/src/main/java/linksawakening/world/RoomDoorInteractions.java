package linksawakening.world;

import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.ChestRewardEvent;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.DungeonWarpRequest;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.HinoxLinkEffectRequest;
import linksawakening.world.entity.event.HookshotBridgeUpdate;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LikeLikeEvent;
import linksawakening.world.entity.event.LinkAttackClearRequest;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.LinkFallenPoseRequest;
import linksawakening.world.entity.event.LinkFinalPositionRequest;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.MadamMeowMeowFullHealRequest;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.RichardProgressEvent;
import linksawakening.world.entity.event.RoomStatusPersistenceRequest;
import linksawakening.world.entity.event.RoosterLinkStateRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.event.SlimeKeyRewardEvent;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ThrownBackgroundCollisionEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.event.WarpDestination;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.gpu.GPU;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_HEIGHT;
import static linksawakening.world.RoomConstants.ROOM_PIXEL_WIDTH;
import static linksawakening.world.RoomSession.*;

/** RoomDoorInteractions operations for the live room session. */
final class RoomDoorInteractions {
    private final RoomSession session;
    RoomDoorInteractions(RoomSession session) { this.session = session; }

    ChestOpenResult tryOpenChest(int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        if (!actionPressed || linkDirection != Link.DIRECTION_UP
            || session.activeRoom == null || session.entityBridge.entityRuntime == null) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        if (linkPixelX < 0 || linkPixelY < 0
            || linkPixelX >= ROOM_PIXEL_WIDTH || linkPixelY >= ROOM_PIXEL_HEIGHT) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int column = Math.floorDiv(linkPixelX + (session.romData[0x1F4B] & 0xFF), 0x10);
        int row = Math.floorDiv(linkPixelY + (session.romData[0x1F4F] & 0xFF), 0x10);
        if (column < 0 || column >= RoomConstants.OBJECTS_PER_ROW
            || row < 0 || row >= RoomConstants.OBJECTS_PER_COLUMN) {
            return new ChestOpenResult(false, -1, -1, -1);
        }
        int location = (row << 4) | column;
        if (session.objectAtRoomLocation(location) != 0xA0) {
            return new ChestOpenResult(false, -1, -1, location);
        }
        int itemType = session.chestContentsTable.itemForSpawn(
            session.activeRoom.mapId(), session.activeRoom.roomId(), swordLevel,
            session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD);
        int slot = session.entityBridge.entityRuntime.spawnChestWithItem(column << 4, row << 4, itemType);
        if (slot < 0) {
            return new ChestOpenResult(false, -1, -1, location);
        }

        session.writeBombPuzzleObject(location, 0xA1);
        byte[] status = session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? session.roomState.overworldRoomStatus : session.indoorStatusTableForMap(session.activeRoom.mapId());
        status[session.activeRoom.roomId()] |= (byte) session.ROOM_STATUS_CHEST_OPEN;
        // writeBombPuzzleObject already updates the live GBC cell. Rebuilding
        // from the ROM overlay would discard other live mutations (cut bushes).
        if (session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD) {
            session.refreshActiveRoomTilemap();
        }
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            ? session.activeRoom.gbcOverlay() : null);
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return new ChestOpenResult(true, itemType, slot, location);
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.activeRoom == null
            || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
            || session.indoorKeyDoorAnimationCountdown != 0) {
            return false;
        }
        int doorDirection = session.keyDoorDirectionIndex(linkDirection);
        if (doorDirection < 0
            || (collisionType & session.keyDoorCollisionBit(doorDirection)) == 0) {
            return false;
        }
        int expectedPhysics = session.KEY_DOOR_PHYSICS_BASE + doorDirection;
        int touchedLocation = -1;
        boolean bossDoor = false;
        for (int[] point : session.keyDoorCollisionPoints(linkPixelX, linkPixelY, doorDirection)) {
            int physics = session.overworldCollision.objectPhysicsFlagAtPoint(point[0], point[1]);
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
            if (objectOffset < 0 || objectOffset > 1
                || !session.alignedWithIndoorKeyDoor(touchedLocation - objectOffset,
                    0, linkPixelX, linkPixelY)) {
                return false;
            }
            if (session.roomState.dungeonItemState.currentFlag(DungeonItemState.NIGHTMARE_KEY_INDEX) == 0) {
                session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0x07));
                return true;
            }
            session.indoorBossDoorOpening = true;
            session.indoorKeyDoorDirection = 0;
            session.indoorKeyDoorLocation = touchedLocation - objectOffset;
            session.indoorKeyDoorAnimationCountdown = session.KEY_DOOR_ANIMATION_FRAMES;
            session.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
            return true;
        }
        int objectOffset = touchedObject - session.KEY_DOOR_CLOSED_OBJECTS[doorDirection];
        if (objectOffset < 0 || objectOffset > 1) return false;
        int doorLocation = touchedLocation
            - (objectOffset == 0 ? 0 : doorDirection < 2 ? 1 : 0x10);
        if (!session.alignedWithIndoorKeyDoor(doorLocation, doorDirection, linkPixelX, linkPixelY)
            || !session.roomState.dungeonItemState.consumeSmallKey()) return false;
        session.indoorStatusTableForMap(session.activeRoom.mapId())[session.activeRoom.roomId()]
            |= (byte) session.INDOOR_ROOM_STATUS_EVENT_3;
        session.indoorKeyDoorDirection = doorDirection;
        session.indoorKeyDoorLocation = doorLocation;
        session.indoorKeyDoorAnimationCountdown = session.KEY_DOOR_ANIMATION_FRAMES;
        session.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        return true;
    }

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.activeRoom == null
            || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
            || session.pushedBlockMotionFrames != 0) {
            session.pushBlockContactTicks = 0;
            return false;
        }
        int direction = session.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & session.keyDoorCollisionBit(direction)) == 0) {
            session.pushBlockContactTicks = 0;
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
                session.pushBlockContactTicks = 0;
            }
            return false;
        }
        session.pushBlockContactTicks++;
        if (session.pushBlockContactTicks < session.PUSH_BLOCK_CONTACT_TICKS) {
            return true;
        }
        session.pushBlockContactTicks = 0;
        int destination = touchedLocation + switch (direction) {
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
                session.overworldCollision.objectPhysicsFlagAtEntityPosition(
                    destinationX, destinationY))) {
            return true;
        }
        int sourceX = ((touchedLocation & 0x0F) << 4) + 0x08;
        int sourceY = (touchedLocation & 0xF0) + 0x10;
        int spawnedSlot = session.entityBridge.entityRuntime == null
            ? -1 : session.entityBridge.entityRuntime.spawnPushedBlock(sourceX, sourceY);
        if (spawnedSlot < 0) {
            return true;
        }
        session.activeRoom.roomObjectsArea()[sourceIndex] = session.OBJECT_FLOOR_OD;
        session.pushedBlockSourceLocation = touchedLocation;
        session.pushedBlockDestinationLocation = destination;
        session.pushedBlockDirection = direction;
        session.pushedBlockMotionFrames = session.PUSHED_BLOCK_MOTION_FRAMES;
        session.pushedBlockEntitySlot = spawnedSlot;
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
        if (session.entityBridge.entityRuntime != null) {
            session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        }
        return true;
    }

    boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) {
        return session.tryLiftIndoorObject(linkPixelX, linkPixelY, linkDirection,
            braceletButtonHeld, pressedButtonsMask, false);
    }

    boolean tryLiftIndoorObject(int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask, boolean pieceOfPowerActive) {
        if (session.activeRoom == null || session.entityBridge.entityRuntime == null
            || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL
            || !braceletButtonHeld || session.entityBridge.entityRuntime.liftedEntityState().active()) {
            session.indoorPotPullFrames = 0;
            return false;
        }
        int oppositeButton = switch (linkDirection) {
            case Link.DIRECTION_UP -> 0x08;
            case Link.DIRECTION_DOWN -> 0x04;
            case Link.DIRECTION_LEFT -> 0x01;
            case Link.DIRECTION_RIGHT -> 0x02;
            default -> 0;
        };
        if (oppositeButton == 0 || (pressedButtonsMask & oppositeButton) == 0) {
            session.indoorPotPullFrames = 0;
            return false;
        }

        int swordAreaX = switch (linkDirection) {
            case Link.DIRECTION_RIGHT -> 0x0C;
            case Link.DIRECTION_LEFT -> 0x03;
            case Link.DIRECTION_UP, Link.DIRECTION_DOWN -> 0x08;
            default -> 0;
        };
        int swordAreaY = switch (linkDirection) {
            case Link.DIRECTION_RIGHT, Link.DIRECTION_LEFT -> 0x0A;
            case Link.DIRECTION_UP -> 0x05;
            case Link.DIRECTION_DOWN -> 0x10;
            default -> 0;
        };
        int objectLeft = (linkPixelX + swordAreaX) & 0xF0;
        int objectTop = (linkPixelY + swordAreaY) & 0xF0;
        int touchedLocation = objectTop | (objectLeft >>> 4);
        int touchedObject = session.objectAtRoomLocation(touchedLocation);
        if (touchedObject != session.OBJECT_LIFTABLE_POT
            && touchedObject != session.OBJECT_POT_WITH_SWITCH) {
            session.indoorPotPullFrames = 0;
            return false;
        }
        session.indoorPotPullFrames++;
        int pullFrames = pieceOfPowerActive ? 0x03 : session.INDOOR_POT_PULL_FRAMES;
        if (session.indoorPotPullFrames < pullFrames) {
            return true;
        }
        session.indoorPotPullFrames = 0;

        session.activeRoom.roomObjectsArea()[RoomConstants.ROOM_OBJECTS_BASE + touchedLocation] =
            touchedObject == session.OBJECT_POT_WITH_SWITCH
                ? session.OBJECT_SWITCH_BUTTON : 0x0D;
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
        int x = linkPixelX + 0x08;
        int y = linkPixelY + 0x10;
        session.entityBridge.entityRuntime.spawnLiftedRoomObject(
            x, y, session.romDirectionForProjectileCollision(linkDirection), 0);
        session.activeRoom.replaceEntities(session.entityBridge.entityRuntime.snapshot());
        return true;
    }

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType)) {
            return true;
        }
        return session.tryUnlockIndoorKeyholeBlock(
            linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        if (session.activeRoom == null
            || session.activeRoom.mapCategory() == Warp.CATEGORY_OVERWORLD
            || session.activeRoom.mapCategory() == Warp.CATEGORY_SIDESCROLL) {
            session.pushBlockContactTicks = 0;
            return false;
        }
        int direction = session.keyDoorDirectionIndex(linkDirection);
        if (direction < 0 || (collisionType & session.keyDoorCollisionBit(direction)) == 0) {
            session.pushBlockContactTicks = 0;
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
            session.pushBlockContactTicks = 0;
            return false;
        }
        if (session.roomState.dungeonItemState.currentFlag(DungeonItemState.SMALL_KEYS_INDEX) == 0) {
            session.pushBlockContactTicks = 0;
            session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(0, 0x8C));
            return true;
        }
        session.pushBlockContactTicks++;
        if (session.pushBlockContactTicks < session.KEYHOLE_BLOCK_CONTACT_TICKS) {
            return true;
        }
        session.pushBlockContactTicks = 0;
        session.roomState.dungeonItemState.consumeSmallKey();

        session.writeBombPuzzleObject(touchedLocation, session.OBJECT_FLOOR_OD);
        session.indoorStatusTableForMap(session.activeRoom.mapId())[session.activeRoom.roomId()]
            |= (byte) session.INDOOR_ROOM_STATUS_EVENT_3;
        session.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        session.refreshActiveRoomTilemap();
        session.overworldCollision.setRoom(session.activeRoom.roomObjectsArea());
        session.overworldCollision.setGbcOverlay(null);
        return true;
    }

    boolean tryUnlockTailCaveKeyhole(int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return session.tryUnlockOverworldDungeonKeyhole(
            linkPixelX, linkPixelY, linkDirection, collisionType,
            session.ROOM_OW_TAIL_CAVE_ENTRANCE, hasTailKey, session.TAIL_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockKeyCavernKeyhole(int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return session.tryUnlockOverworldDungeonKeyhole(
            linkPixelX, linkPixelY, linkDirection, collisionType,
            session.ROOM_OW_KEY_CAVERN_ENTRANCE, (goldenLeavesCount & 0xFF) == 0x06,
            session.SLIME_KEYHOLE_DIALOG_ID);
    }

    boolean tryUnlockOverworldDungeonKeyhole( int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int roomId, boolean hasKey, int dialogLowId) {
        if (session.activeRoom == null
            || session.activeRoom.mapCategory() != Warp.CATEGORY_OVERWORLD
            || session.activeRoom.roomId() != roomId
            || linkDirection != Link.DIRECTION_UP
            || (collisionType & session.LINK_COLLISION_TYPE_UP) == 0
            || (session.roomState.overworldRoomStatus[roomId] & session.ROOM_STATUS_EVENT_1) != 0) {
            return false;
        }

        int collisionPointY = linkPixelY + 0x06;
        boolean leftProbeIsKeyhole = session.overworldCollision.objectPhysicsFlagAtPoint(
            linkPixelX + 0x06, collisionPointY) == PhysicsFlags.CAT_KEYHOLE;
        boolean rightProbeIsKeyhole = session.overworldCollision.objectPhysicsFlagAtPoint(
            linkPixelX + 0x09, collisionPointY) == PhysicsFlags.CAT_KEYHOLE;
        if (!leftProbeIsKeyhole && !rightProbeIsKeyhole) {
            return false;
        }
        if (!hasKey) {
            session.entityBridge.pendingRoomDialogRequests.add(new DialogRequest(
                session.KEYHOLE_DIALOG_TABLE, dialogLowId));
            return true;
        }

        session.roomState.overworldRoomStatus[roomId] |= (byte) session.ROOM_STATUS_EVENT_1;
        session.tailCaveKeyholeCountdown = session.TAIL_CAVE_RUMBLE_INITIAL_COUNTDOWN;
        session.tailCaveResumeMusicTrack = session.entityDefaultMusicTrack;
        session.entityBridge.pendingRoomMusicTrack = 0x00;
        return true;
    }

    int tickTailCaveKeyholeSequence() {
        session.tickNextWorldMusicTrackCountdown();
        if (session.tailCaveKeyholeCountdown == 0) {
            session.tailCaveFinalMotionBlockPending = false;
            return 0;
        }
        session.tailCaveKeyholeCountdown = (session.tailCaveKeyholeCountdown - 1) & 0xFF;
        int countdown = session.tailCaveKeyholeCountdown;
        if (countdown == 0xDE) {
            session.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
        }
        if (countdown == 0xA0) {
            session.colorShellSoundSink.play(GameplaySoundEvent.OPEN_KEY_CAVERN);
        }
        if (countdown < 0x20 && (countdown & 0x0F) == 0x08) {
            session.writeTailCaveGateAnimationFrame(countdown);
        }
        if (countdown == session.TAIL_CAVE_RUMBLE_GATE_FRAME) {
            int gateIndex = RoomConstants.ROOM_OBJECTS_BASE + session.TAIL_CAVE_GATE_LOCATION;
            if (session.activeRoom != null
                && session.activeRoom.roomObjectsArea()[gateIndex] == session.OBJECT_CLOSED_GATE) {
                session.activeRoom.roomObjectsArea()[gateIndex] = session.OBJECT_CAVE_DOOR;
                // BackupObjectInRAM2($82): force-copy [hl], then return to bank 2.
                // $82 is a control argument, not the replacement object value.
                int overlayIndex = 0x10;
                session.activeRoom.gbcOverlay()[overlayIndex] = session.OBJECT_CAVE_DOOR;
                session.activeRoom.renderValues()[gateIndex] = session.OBJECT_CAVE_DOOR;
                // The source's draw command changes tile IDs only; keep attrs.
                session.refreshOverworldCollisionAfterObjectMutation();
            }
            session.colorShellSoundSink.play(GameplaySoundEvent.DUNGEON_OPENED);
        }
        if (countdown == 0x0A) {
            session.nextWorldMusicTrackCountdown = 0x50;
        }
        if (countdown == 0) {
            session.tailCaveFinalMotionBlockPending = true;
        }
        if (countdown < 0x20 || countdown >= 0x9C) {
            return 0;
        }
        return (countdown & 0x04) == 0 ? 1 : -2;
    }

    boolean tailCaveKeyholeSequenceActive() {
        return session.tailCaveKeyholeCountdown != 0 || session.tailCaveFinalMotionBlockPending;
    }

    int tailCaveKeyholeCountdownForTest() {
        return session.tailCaveKeyholeCountdown;
    }

    int nextWorldMusicTrackCountdownForTest() {
        return session.nextWorldMusicTrackCountdown;
    }

    void writeTailCaveGateAnimationFrame(int countdown) {
        if (session.activeRoom == null
            || session.activeRoom.roomId() != session.ROOM_OW_TAIL_CAVE_ENTRANCE
                && session.activeRoom.roomId() != session.ROOM_OW_KEY_CAVERN_ENTRANCE) {
            return;
        }
        int frameIndex = (countdown >>> 3) & 0x02;
        int frameOffset = RomBank.romOffset(0x02, 0x5642) + frameIndex;
        int topTile = session.romData[frameOffset] & 0xFF;
        int bottomTile = session.romData[frameOffset + 1] & 0xFF;
        int tileX = (session.TAIL_CAVE_GATE_LOCATION & 0x0F) * 2;
        int tileY = (session.TAIL_CAVE_GATE_LOCATION >>> 4) * 2;
        int topLeft = tileY * RoomConstants.ROOM_TILE_WIDTH + tileX;
        int[] tiles = session.activeRoom.tileIds();
        tiles[topLeft] = topTile;
        tiles[topLeft + 1] = topTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH] = bottomTile;
        tiles[topLeft + RoomConstants.ROOM_TILE_WIDTH + 1] = bottomTile;
    }

    void tickNextWorldMusicTrackCountdown() {
        if (session.nextWorldMusicTrackCountdown == 0) {
            return;
        }
        session.nextWorldMusicTrackCountdown--;
        if (session.nextWorldMusicTrackCountdown == 0) {
            session.entityBridge.pendingRoomMusicTrack = session.tailCaveResumeMusicTrack;
        }
    }
}
