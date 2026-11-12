package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.physics.OverworldCollision;
import linksawakening.physics.PhysicsFlags;
import linksawakening.rom.RomBank;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.transition.Warp;

final class RoomDungeonKeyholes {
    private final Dependencies dependencies;

    RoomDungeonKeyholes(Dependencies dependencies) {
        this.dependencies = dependencies;
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
            java.util.function.Supplier<RoomEntityBridge> entityBridge,
            RoomEntityFrameState entityFrame,
            RoomFollowerState followers,
            RoomLocationState location,
            OverworldCollision overworldCollision,
            RoomPuzzleState puzzles,
            byte[] romData,
            java.util.function.Supplier<RoomDoorInteractions> roomDoorInteractions,
            java.util.function.Supplier<RoomObjectMutations> roomObjectMutations,
            RoomWorldState roomState) {}
}
