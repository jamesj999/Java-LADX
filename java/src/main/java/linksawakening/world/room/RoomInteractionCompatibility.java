package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

interface RoomInteractionCompatibility {
    RoomSession facade();

    default boolean tickCrackedFloor(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return facade().context.roomObjectMutations.tickCrackedFloor(frame, dialogGotItemActive);
    }
    /** Grounded landing probe at $02:4588, before the ordinary $00:1794 pass. */
    default boolean tickCrackedFloorOnLanding(LinkGroundFrame frame, boolean dialogGotItemActive) {
        return facade().context.roomObjectMutations.applyCrackedFloorGroundPhysics(frame, dialogGotItemActive);
    }

    default boolean breakIndoorSwordCrystal(int location) {
        return facade().context.roomObjectMutations.breakIndoorSwordCrystal(location);
    }
    /**
     * Mirrors the closed-$A0 branch in the source object interaction path.
     * The source at $00:1F80 samples SwordAreaX/YForDirection relative to
     * Link's sprite top-left (ROM entity coordinates minus 8/16).
     */
    default ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        return facade().doors().tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }
    /** Mirrors bank 2's locked-door branch for physics categories $90-$93. */
    default boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return facade().doors().tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    default boolean tryLiftObject(
            int x, int y, int direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp) {
        return facade().doors().tryLiftObject(x, y, direction, held, buttons, power, level, anyPowerUp);
    }

    default boolean tryLiftObject(int x, int y, int direction, boolean held, int buttons, boolean power, int level) {
        return facade().doors().tryLiftObject(x, y, direction, held, buttons, power, level);
    }
    /** Dispatches bank 2's shared $A7/$DE interactive-block collision branch. */
    default boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return facade().doors().tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }
    /**
     * Mirrors bank 2's Tail Key branch in ApplyCollisionWithObject. The
     * keyhole is automatic: walking upward into physics category $C0 either
     * opens Dialog230 or schedules the ROM's $DF-frame gate-opening rumble.
     */
    default boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return facade().doors()
                .tryUnlockTailCaveKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey);
    }
    /** Mirrors bank 2's room-$B5 Slime Key branch of the shared keyhole handler. */
    default boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return facade().doors()
                .tryUnlockKeyCavernKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount);
    }
    /** One RenderTranscientRumble step, called once per unpaused gameplay frame. */
    default int tickTailCaveKeyholeSequence() {
        return facade().doors().tickTailCaveKeyholeSequence();
    }

    default boolean tailCaveKeyholeSequenceActive() {
        return facade().doors().tailCaveKeyholeSequenceActive();
    }
}
