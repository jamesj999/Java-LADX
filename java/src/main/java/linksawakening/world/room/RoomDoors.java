package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.state.Direction;

public interface RoomDoors {
    ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, Direction linkDirection, boolean actionPressed, int swordLevel);
    /** @deprecated Pass Direction; retained for app/gameplay door/lift consumers. */
    @Deprecated(forRemoval = true)
    default ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        return tryOpenChest(
                linkPixelX, linkPixelY, Direction.fromJavaOrUnknown(linkDirection), actionPressed, swordLevel);
    }

    boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);
    /** @deprecated Pass Direction; retained for app/gameplay door/lift consumers. */
    @Deprecated(forRemoval = true)
    default boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return tryUnlockIndoorKeyDoor(
                linkPixelX, linkPixelY, Direction.fromJavaOrUnknown(linkDirection), collisionType);
    }

    boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);

    boolean tryLiftObject(
            int x, int y, Direction direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp);
    /** @deprecated Pass Direction; retained for app/gameplay door/lift consumers. */
    @Deprecated(forRemoval = true)
    default boolean tryLiftObject(
            int x, int y, int direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp) {
        return tryLiftObject(x, y, Direction.fromJavaOrUnknown(direction), held, buttons, power, level, anyPowerUp);
    }

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
    /** @deprecated Pass Direction; retained for app/gameplay door/lift consumers. */
    @Deprecated(forRemoval = true)
    default boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return tryInteractWithIndoorBlock(
                linkPixelX, linkPixelY, Direction.fromJavaOrUnknown(linkDirection), collisionType);
    }

    boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType);

    boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, boolean hasTailKey);
    /** @deprecated Pass Direction; retained for app/gameplay door/lift consumers. */
    @Deprecated(forRemoval = true)
    default boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return tryUnlockTailCaveKeyhole(
                linkPixelX, linkPixelY, Direction.fromJavaOrUnknown(linkDirection), collisionType, hasTailKey);
    }

    boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, Direction linkDirection, int collisionType, int goldenLeavesCount);
    /** @deprecated Pass Direction; retained for app/gameplay door/lift consumers. */
    @Deprecated(forRemoval = true)
    default boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return tryUnlockKeyCavernKeyhole(
                linkPixelX, linkPixelY, Direction.fromJavaOrUnknown(linkDirection), collisionType, goldenLeavesCount);
    }

    int tickTailCaveKeyholeSequence();

    boolean tailCaveKeyholeSequenceActive();
}
