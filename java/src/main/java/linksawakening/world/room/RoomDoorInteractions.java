package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import linksawakening.physics.OverworldCollision;
import linksawakening.world.object.OverworldBushInteraction;

/** RoomDoorInteractions operations for the live room session. */
final class RoomDoorInteractions {
    private final Dependencies dependencies;
    private final RoomChestInteractions roomChestInteractions;
    private final RoomIndoorDoorInteractions roomIndoorDoorInteractions;
    private final RoomLiftInteractions roomLiftInteractions;
    private final RoomDungeonKeyholes roomDungeonKeyholes;

    RoomDoorInteractions(Dependencies dependencies) {
        this.dependencies = dependencies;
        roomChestInteractions = new RoomChestInteractions(new RoomChestInteractions.Dependencies(
                dependencies.chestContentsTable(),
                dependencies.entityBridge(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.romData(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomTileRedraw()));
        roomIndoorDoorInteractions = new RoomIndoorDoorInteractions(new RoomIndoorDoorInteractions.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.roomDoorInteractions(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomState(),
                dependencies.roomTileRedraw()));
        roomLiftInteractions = new RoomLiftInteractions(new RoomLiftInteractions.Dependencies(
                dependencies.entityBridge(),
                dependencies.followers(),
                dependencies.location(),
                dependencies.overworldBushInteraction(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.roomDoorInteractions(),
                dependencies.roomObjectMutations(),
                dependencies.roomSessionViews(),
                dependencies.roomTileRedraw(),
                () -> this));
        roomDungeonKeyholes = new RoomDungeonKeyholes(new RoomDungeonKeyholes.Dependencies(
                dependencies.entityBridge(),
                dependencies.entityFrame(),
                dependencies.followers(),
                dependencies.location(),
                dependencies.overworldCollision(),
                dependencies.puzzles(),
                dependencies.romData(),
                dependencies.roomDoorInteractions(),
                dependencies.roomObjectMutations(),
                dependencies.roomState()));
    }

    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        return roomChestInteractions.tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return roomIndoorDoorInteractions.tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return roomIndoorDoorInteractions.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) {
        return roomLiftInteractions.tryLiftIndoorObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask);
    }

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return roomLiftInteractions.tryLiftIndoorObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive);
    }

    boolean tryLiftObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
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
            int linkDirection,
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

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return roomIndoorDoorInteractions.tryInteractWithIndoorBlock(
                linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return roomIndoorDoorInteractions.tryUnlockIndoorKeyholeBlock(
                linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return roomDungeonKeyholes.tryUnlockTailCaveKeyhole(
                linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey);
    }

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return roomDungeonKeyholes.tryUnlockKeyCavernKeyhole(
                linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount);
    }

    boolean tryUnlockOverworldDungeonKeyhole(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            int collisionType,
            int roomId,
            boolean hasKey,
            int dialogLowId) {
        return roomDungeonKeyholes.tryUnlockOverworldDungeonKeyhole(
                linkPixelX, linkPixelY, linkDirection, collisionType, roomId, hasKey, dialogLowId);
    }

    int tickTailCaveKeyholeSequence() {
        return roomDungeonKeyholes.tickTailCaveKeyholeSequence();
    }

    boolean tailCaveKeyholeSequenceActive() {
        return roomDungeonKeyholes.tailCaveKeyholeSequenceActive();
    }

    int tailCaveKeyholeCountdownForTest() {
        return roomDungeonKeyholes.tailCaveKeyholeCountdownForTest();
    }

    int nextWorldMusicTrackCountdownForTest() {
        return roomDungeonKeyholes.nextWorldMusicTrackCountdownForTest();
    }

    void writeTailCaveGateAnimationFrame(int countdown) {
        roomDungeonKeyholes.writeTailCaveGateAnimationFrame(countdown);
    }

    void tickNextWorldMusicTrackCountdown() {
        roomDungeonKeyholes.tickNextWorldMusicTrackCountdown();
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
