package linksawakening.world;

/** Exact collision and sword-collection rules shared by static pickable handlers. */
public final class RoomEntityPickupRules {
    private static final int FIRST_PICKABLE_TYPE = 0x2D;
    private static final int LAST_PICKABLE_TYPE = 0x3D;
    private static final int IRON_MASKS_MASK = 0x32;

    // HitboxPositions._1C in home/entities.asm:3AC6. The collision helper
    // adds four to the width/height entries before comparing with Link.
    private static final int HITBOX_X = 0x08;
    private static final int HITBOX_WIDTH = 0x07;
    private static final int HITBOX_Y = 0x06;
    private static final int HITBOX_HEIGHT = 0x0A;

    private static final boolean[] PICKABLE_CAN_BE_COLLECTED_BY_SWORD = {
        true,  // $2D droppable heart
        true,  // $2E droppable rupee
        false, // $2F droppable fairy
        false, // $30 key drop point
        true,  // $31 sword/shield pickup
        false, // $32 Iron Mask's mask (enemy hitbox, not a pickable handler)
        true,  // $33 piece of power
        true,  // $34 guardian acorn
        false, // $35 heart piece
        false, // $36 heart container
        true,  // $37 arrows
        true,  // $38 bombs
        false, // $39 instrument
        false, // $3A sleepy toadstool
        true,  // $3B magic powder
        false, // $3C hiding slime key
        false  // $3D secret seashell
    };

    private RoomEntityPickupRules() {
    }

    /** Returns whether the type uses PickableCollectIfNeeded in its handler. */
    public static boolean isPickable(int type) {
        return (type >= FIRST_PICKABLE_TYPE
                && type <= LAST_PICKABLE_TYPE
                && type != IRON_MASKS_MASK)
            || FloatingItemMotion.isFloatingItem(type);
    }

    /** EntityInitTreeOrPotDroppable's indoor $80 slow-transition timer set. */
    static boolean usesIndoorDefaultSlowTimer(int type) {
        return switch (type) {
            case 0x2D, 0x2E, 0x2F, 0x32, 0x33, 0x34, 0x36, 0x37, 0x38 -> true;
            default -> false;
        };
    }

    /** Mirrors PickableCanBeCollectedBySwordTable in bank3.asm. */
    public static boolean canBeCollectedBySword(int type) {
        if (type < FIRST_PICKABLE_TYPE || type > LAST_PICKABLE_TYPE) {
            return false;
        }
        return PICKABLE_CAN_BE_COLLECTED_BY_SWORD[type - FIRST_PICKABLE_TYPE];
    }

    /** Mirrors func_003_6C6B's odd (frame xor entity-slot) cadence. */
    public static boolean collisionCadenceMatches(int frameCounter, int slot) {
        return (((frameCounter & 0xFF) ^ slot) & 0x01) != 0;
    }

    /**
     * Mirrors CheckLinkCollisionWithEnemy for the pickup hitbox. The ROM
     * computes unsigned-byte absolute differences, so the wraparound helper
     * is intentional even though ordinary room coordinates do not wrap.
     */
    public static boolean overlapsLink(RoomEntity entity, int linkPixelX, int linkPixelY) {
        return overlapsAtY(entity, linkPixelX, linkPixelY, entity.y());
    }

    /**
     * Mirrors the floating-item handler's collisionEvenInTheAir path. The
     * entity's visual Y is its room Y minus its current ROM Z; ordinary
     * static pickables intentionally continue using {@link #overlapsLink}.
     */
    public static boolean overlapsFloatingItem(RoomEntity entity,
                                                int linkPixelX,
                                                int linkPixelY) {
        return overlapsAtY(entity, linkPixelX, linkPixelY, entity.y() - entity.z());
    }

    private static boolean overlapsAtY(RoomEntity entity, int linkPixelX,
                                       int linkPixelY, int entityY) {
        int xDistance = unsignedByteAbs(
            entity.x() + HITBOX_X - linkPixelX - 0x08);
        if (xDistance >= HITBOX_WIDTH + 0x04) {
            return false;
        }

        int yDistance = unsignedByteAbs(
            entityY + HITBOX_Y - linkPixelY - 0x08);
        return yDistance < HITBOX_HEIGHT + 0x04;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }
}
