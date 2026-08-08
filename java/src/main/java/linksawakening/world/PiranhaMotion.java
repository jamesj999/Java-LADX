package linksawakening.world;

/** Bank-$36 PiranhaPlantEntityHandler's pipe rise, hold, and lower states. */
final class PiranhaMotion {
    static final int ENTITY_TYPE = 0xA2;
    static final int INITIAL_PHYSICS_FLAGS = 0x14;
    static final int OPTIONS1 = 0x00;

    // Data_036_6F2E and Data_036_6F36 at bank $36:$6F2E/$6F36.
    private static final int[] RISING_VARIANTS = {4, 4, 3, 2, 1, 0, 0, 0};
    private static final int[] RISING_Y_OFFSETS = {
        0xE0, 0xE0, 0xE8, 0xF0, 0x00, 0x00, 0x00, 0x00
    };

    // Data_036_6F84 and Data_036_6F8C at bank $36:$6F84/$6F8C.
    private static final int[] LOWERING_VARIANTS = {0, 0, 0, 0, 1, 2, 3, 5};
    private static final int[] LOWERING_Y_OFFSETS = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0xF0, 0xE8, 0xE0
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] privateState2Initialized =
        new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        privateState2Initialized[slot] = false;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int transitionCountdown,
                   int linkEntityX, int linkEntityY) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Piranha Plant entity");
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        if (!privateState2Initialized[slot]) {
            privateState2Initialized[slot] = true;
            privateState1[slot] = entity.y() & 0xFF;
        }

        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int variant = entity.spriteVariant();
        switch (state[slot]) {
            case 0 -> {
                if (countdown == 0) {
                    countdown = 0x40;
                    if (!linkWithinPipeColumn(linkEntityX, x)) {
                        state[slot] = 1;
                    }
                }
            }
            case 1 -> {
                if (countdown == 0) {
                    countdown = 0x80;
                    state[slot] = 2;
                } else {
                    int index = (countdown >>> 3) & 0x07;
                    variant = RISING_VARIANTS[index];
                    y = (privateState1[slot] + RISING_Y_OFFSETS[index]) & 0xFF;
                }
            }
            case 2 -> {
                // The source tests A before writing the next transition
                // countdown, so retain the handler's incoming value here.
                int handlerCountdown = countdown;
                if (countdown == 0) {
                    countdown = 0x40;
                    state[slot] = 3;
                }
                variant = (handlerCountdown & 0x10) != 0 ? 4 : 5;
            }
            case 3 -> {
                if (countdown == 0) {
                    // The final ld [hl], b clears the transition byte after
                    // IncrementEntityState returns to state 0.
                    countdown = 0;
                    state[slot] = 0;
                } else {
                    int index = (countdown >>> 3) & 0x07;
                    variant = LOWERING_VARIANTS[index];
                    y = (privateState1[slot] + LOWERING_Y_OFFSETS[index]) & 0xFF;
                }
            }
            default -> throw new IllegalStateException("Invalid Piranha Plant state: "
                + state[slot]);
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(),
            entity.type(), x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, countdown, state[slot]);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    private static boolean linkWithinPipeColumn(int linkEntityX, int entityX) {
        int difference = (linkEntityX - entityX) & 0xFF;
        return difference >= 0xF0 || difference < 0x10;
    }

    record Update(RoomEntity entity, int transitionCountdown, int state) {
    }
}
