package linksawakening.world.entity.motion;

import linksawakening.world.entity.*;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;

/** State and timing port of bank-$05 {@code WitchEntityHandler}. */
public final class WitchMotion {
    private final int[] witchGotItemCountdown = new int[EntityRoomLoader.MAX_ENTITIES];

    public int readWitchGotItemCountdown(int slot) {
        return witchGotItemCountdown[slot];
    }

    public int writeWitchGotItemCountdown(int slot, int value) {
        return witchGotItemCountdown[slot] = value;
    }

    public int[] storageWitchGotItemCountdown() {
        return witchGotItemCountdown;
    }

    public static final int ENTITY_TYPE = 0x40;
    static final int INVENTORY_MAGIC_POWDER = 0x0C;
    static final int NO_DIALOG = -1;
    static final int NO_SOUND = -1;

    public enum InventorySlot {
        NONE,
        A,
        B
    }

    public record Input(
            boolean hasToadstool,
            int itemB,
            int itemA,
            boolean actionButtonAHeld,
            boolean actionButtonBHeld,
            boolean dialogActive,
            boolean linkAirborne,
            int linkX,
            int linkY,
            int linkDirection,
            int transitionSequenceCounter,
            int roomTriggerCount,
            int bgPaletteEffectAddress,
            int paletteDataFlags,
            int defaultMusicTrack) {}

    public record Update(
            RoomEntity entity,
            int inertia,
            int transitionCountdown,
            boolean blockLink,
            int dialogGlobalId,
            int musicTrack,
            int jingleId,
            InventorySlot clearedSlot,
            boolean exchangeStarted,
            boolean grantMagicPowder,
            int motionBlockValue) {}

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] musicTrackTiming = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public Update advance(RoomEntity source, int transitionCountdown, Input input) {
        return new WitchStep(this, source, transitionCountdown, input).run();
    }

    static boolean nearbyAndFacing(RoomEntity entity, Input input) {
        int yWindow = (input.linkY() - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x2B) {
            return false;
        }
        int xWindow = (input.linkX() - entity.x() + 0x10) & 0xFF;
        if (xWindow >= 0x20) {
            return false;
        }
        return directionToLink(entity, input.linkX(), input.linkY()) == ((input.linkDirection() & 0x03) ^ 0x01);
    }

    private static int directionToLink(RoomEntity entity, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entity.x());
        int distanceY = signedByte(linkY - entity.y());
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static int signedByte(int value) {
        return (byte) value;
    }

    static RoomEntity withPresentation(RoomEntity source, int y, int variant) {
        return EntitySnapshots.withMotion(
                source, source.x(), y, variant, source.entityFlipAttribute(), source.z(), -1, false);
    }
}
