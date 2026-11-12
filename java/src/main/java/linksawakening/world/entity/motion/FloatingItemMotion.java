package linksawakening.world.entity.motion;

import linksawakening.world.entity.event.FloatingItemPickupEffect;

/** Pure ROM rules shared by the two bank-$06 floating-item handlers. */
public final class FloatingItemMotion {
    public static final int ENTITY_FLOATING_ITEM = 0x86;
    public static final int ENTITY_FLOATING_ITEM_2 = 0xE5;

    private static final int INITIAL_Z = 0x13;
    // Data_006_7AFB in bank $06, selected when hIsSideScrolling is clear.
    private static final int[] TOP_DOWN_Z = {0x0F, 0x0F, 0x10, 0x11, 0x11, 0x11, 0x10, 0x0F};
    // Data_006_7B03 in bank $06, selected when hIsSideScrolling is set.
    private static final int[] SIDE_SCROLLING_Z = {0x00, 0x00, 0x01, 0x02, 0x02, 0x02, 0x01, 0x00};

    private FloatingItemMotion() {}

    public static boolean isFloatingItem(int entityType) {
        return entityType == ENTITY_FLOATING_ITEM || entityType == ENTITY_FLOATING_ITEM_2;
    }

    /** Mirrors EntityInitFloatingItem and EntityInitFloatingItem2 in bank $03. */
    public static int initialVariant(int entityType, int x, int y) {
        validateEntityType(entityType);
        validateByte(x, "Entity X");
        validateByte(y, "Entity Y");
        if (entityType == ENTITY_FLOATING_ITEM_2) {
            return 0x04 + ((x >>> 4) & 0x01);
        }
        int xParity = (x >>> 4) & 0x01;
        // The source performs swap, inc, rla, and and $02 on hActiveEntityPosY.
        int yParity = ((((y >>> 4) + 1) & 0xFF) << 1) & 0x02;
        return xParity | yParity;
    }

    public static int initialZ(int entityType) {
        validateEntityType(entityType);
        return INITIAL_Z;
    }

    /** Mirrors the handler's (hFrameCounter >> 3) & $07 table index. */
    public static int zForFrame(boolean sideScrolling, int frameCounter) {
        int index = ((frameCounter & 0xFF) >>> 3) & 0x07;
        return (sideScrolling ? SIDE_SCROLLING_Z : TOP_DOWN_Z)[index];
    }

    /** Mirrors FloatingItemEntityHandler's top-down Link-Z early return. */
    public static boolean linkZAllowsCollection(boolean sideScrolling, int linkZ) {
        validateByte(linkZ, "Link Z");
        return sideScrolling || linkZ >= 0x0C;
    }

    /** Returns the ordinary item entity represented by a floating source variant. */
    public static int pickupEntityType(int entityType, int sourceVariant) {
        return switch (pickupEffect(entityType, sourceVariant)) {
            case TEN_RUPEES -> 0x2E;
            case MAGIC_POWDER -> 0x3B;
            case TEN_BOMBS -> 0x38;
            case HEALTH_18 -> 0x2D;
            case TEN_ARROWS -> 0x37;
        };
    }

    /** Mirrors the six-entry JP_TABLE at bank $06:$7B95. */
    public static FloatingItemPickupEffect pickupEffect(int entityType, int sourceVariant) {
        return FloatingItemPickupEffect.pickupEffect(entityType, sourceVariant);
    }

    private static void validateEntityType(int entityType) {
        FloatingItemPickupEffect.validateEntityType(entityType);
    }

    private static void validateByte(int value, String name) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }
}
