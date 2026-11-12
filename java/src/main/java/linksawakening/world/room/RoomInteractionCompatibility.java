package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

public interface RoomInteractionCompatibility {
    RoomSession facade();

    /** @deprecated Use {@code terrain().tickCrackedFloor()}. */
    @Deprecated(forRemoval = true)
    default boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return facade().terrain().tickCrackedFloor(frame, dialogGotItemActive);
    }
    /** @deprecated Use {@code terrain().tickCrackedFloorOnLanding()}. */
    @Deprecated(forRemoval = true)
    default boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return facade().terrain().tickCrackedFloorOnLanding(frame, dialogGotItemActive);
    }

    /** @deprecated Use {@code terrain().breakIndoorSwordCrystal()}. */
    @Deprecated(forRemoval = true)
    default boolean breakIndoorSwordCrystal(int location) {
        return facade().terrain().breakIndoorSwordCrystal(location);
    }
    /** @deprecated Use {@code doors().tryOpenChest()}. */
    @Deprecated(forRemoval = true)
    default ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        return facade().doors().tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }
    /** @deprecated Use {@code doors().tryUnlockIndoorKeyDoor()}. */
    @Deprecated(forRemoval = true)
    default boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return facade().doors().tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    /** @deprecated Use {@code doors().tryLiftObject()}. */
    @Deprecated(forRemoval = true)
    default boolean tryLiftObject(
            int x, int y, int direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp) {
        return facade().doors().tryLiftObject(x, y, direction, held, buttons, power, level, anyPowerUp);
    }

    /** @deprecated Use {@code doors().tryLiftObject()}. */
    @Deprecated(forRemoval = true)
    default boolean tryLiftObject(int x, int y, int direction, boolean held, int buttons, boolean power, int level) {
        return facade().doors().tryLiftObject(x, y, direction, held, buttons, power, level);
    }
    /** @deprecated Use {@code doors().tryInteractWithIndoorBlock()}. */
    @Deprecated(forRemoval = true)
    default boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return facade().doors().tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }
    /** @deprecated Use {@code doors().tryUnlockTailCaveKeyhole()}. */
    @Deprecated(forRemoval = true)
    default boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return facade().doors()
                .tryUnlockTailCaveKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey);
    }
    /** @deprecated Use {@code doors().tryUnlockKeyCavernKeyhole()}. */
    @Deprecated(forRemoval = true)
    default boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return facade().doors()
                .tryUnlockKeyCavernKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount);
    }
    /** @deprecated Use {@code doors().tickTailCaveKeyholeSequence()}. */
    @Deprecated(forRemoval = true)
    default int tickTailCaveKeyholeSequence() {
        return facade().doors().tickTailCaveKeyholeSequence();
    }

    /** @deprecated Use {@code doors().tailCaveKeyholeSequenceActive()}. */
    @Deprecated(forRemoval = true)
    default boolean tailCaveKeyholeSequenceActive() {
        return facade().doors().tailCaveKeyholeSequenceActive();
    }
}
