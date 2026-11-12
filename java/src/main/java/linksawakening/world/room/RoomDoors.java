package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.state.Direction;

public interface RoomDoors {
    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, Direction linkDirection, boolean actionPressed, int swordLevel);

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);

    boolean tryLiftObject(
            int x, int y, Direction direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp);

    boolean tryLiftObject(int x, int y, Direction direction, boolean held, int buttons, boolean power, int level);

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask);

    boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            Direction linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive);

    boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, boolean hasTailKey);

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, int goldenLeavesCount);

    int tickTailCaveKeyholeSequence();

    boolean tailCaveKeyholeSequenceActive();
}
