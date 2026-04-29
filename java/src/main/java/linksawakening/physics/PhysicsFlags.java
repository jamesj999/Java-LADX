package linksawakening.physics;

/**
 * Object physics flags from LADX-Disassembly/src/constants/physics.asm.
 * One byte per object id, with three parallel 256-byte tables starting at
 * {@code OverworldObjectPhysicFlags} (bank $08, addr $4AD4): Overworld,
 * Indoors1 (regular indoor), Indoors2 (color dungeon / variant).
 *
 * <p>Most flag values are a category in the high nibble and a sub-index in
 * the low nibble — e.g. {@code $Dx} = LEDGE with direction x, {@code $9x} =
 * closed door with direction/variant x.
 */
public final class PhysicsFlags {

    // Single-byte values (flag == value)
    public static final int NONE = 0x00;
    public static final int SOLID = 0x01;
    public static final int STAIRS = 0x02;
    public static final int DOOR = 0x03;
    public static final int SHALLOW_WATER = 0x05;
    public static final int GRASS = 0x06;
    public static final int DEEP_WATER = 0x07;
    public static final int RAISED = 0x08;
    public static final int LOWERED = 0x09;
    public static final int WIDE_STAIRS = 0x0A;
    public static final int LAVA = 0x0B;
    public static final int NORMAL_PIT = 0x50;
    public static final int PIT_WARP = 0x51;

    // Category bases (match high nibble after a category mask)
    public static final int CAT_LEDGE_OVERWORLD = 0x10;
    public static final int CAT_REMOVABLE_OBSTACLE = 0x30;
    public static final int CAT_PIT = 0x50;
    public static final int CAT_HOOKSHOTABLE = 0x60;
    public static final int CAT_DOOR_OPEN = 0x70;
    public static final int CAT_FINE_COLLISION = 0x80;
    public static final int CAT_DOOR_CLOSED = 0x90;
    public static final int CAT_KEYHOLE = 0xC0;
    public static final int CAT_LEDGE = 0xD0;
    public static final int CAT_SPIKES = 0xE0;
    public static final int CAT_CONVEYOR = 0xF0;

    private PhysicsFlags() {
    }

    /**
     * Return true if an object with this physics flag blocks walking in the
     * top-down view. Walkable by default — this is an allow-list of values
     * the disassembly treats as solid obstacles. {@link #DOOR} ({@code $03})
     * is intentionally walkable so Link can step onto overworld house doors
     * and trigger the warp (the warp is matched by object-id, not by
     * collision); actual closed/locked doors in indoor rooms live in the
     * {@link #CAT_DOOR_CLOSED} category and do block.
     */
    public static boolean blocksWalking(int flag) {
        if (flag == NONE) return false;

        switch (flag) {
            case SOLID:
            case DEEP_WATER:
            case LAVA:
                return true;
        }

        if (isPitCategory(flag)) {
            return true;
        }

        switch (flag & 0xF0) {
            case CAT_LEDGE_OVERWORLD:
            case CAT_REMOVABLE_OBSTACLE:
            case CAT_HOOKSHOTABLE:
            case CAT_FINE_COLLISION:
            case CAT_DOOR_CLOSED:
            case CAT_KEYHOLE:
            case CAT_LEDGE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Return true for ground types that use {@code GROUND_STATUS_SLOW}
     * in {@code ApplyLinkGroundPhysics} (bank2.asm:7750+).
     */
    public static boolean slowsWalking(int flag) {
        return flag == SHALLOW_WATER || flag == GRASS;
    }

    public static boolean isNormalPit(int flag) {
        return flag == NORMAL_PIT;
    }

    public static boolean isPitCategory(int flag) {
        return (flag & 0xF0) == CAT_PIT;
    }
}
