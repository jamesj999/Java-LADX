package linksawakening.world;

/** Presentation state shared by the Moblin Sword handler and EntityFallHandler. */
final class MoblinSwordMotion {
    private static final int[] VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitMoblinSword, including its post-variant direction flip. */
    void initialize(int slot, int activeX) {
        int initialDirection = (activeX & 0x10) != 0 ? 0 : 3;
        direction[slot] = initialDirection ^ 0x01;
        inertia[slot] = 1;
        initialized[slot] = true;
    }

    /** Mirrors the three SetEntityVariantForDirection calls before the sword handler. */
    int advancePresentationVariant(int slot, int activeX, int repetitions) {
        if (!initialized[slot]) {
            initialize(slot, activeX);
        }
        for (int i = 0; i < repetitions; i++) {
            inertia[slot] = (inertia[slot] + 1) & 0xFF;
        }
        return VARIANT_BY_DIRECTION[direction[slot]] | ((inertia[slot] >>> 3) & 0x01);
    }

    void clear(int slot) {
        direction[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = false;
    }
}
