package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$04 PairoddEntityHandler's resting/teleport state machine. */
final class PairoddMotion {
    private static final int STATE_RESTING = 0;
    private static final int STATE_DISAPPEARING = 1;
    private static final int STATE_REAPPEARING = 2;
    private static final int[] DISAPPEARING_VARIANTS = {4, 3, 2};
    private static final int[] REAPPEARING_VARIANTS = {2, 3, 4};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection for a room-loaded Pairodd. */
    void initialize(int slot, IntSupplier randomByteSupplier) {
        reset(slot);
        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
        initialized[slot] = true;
    }

    /** Initializes a directly supplied active snapshot without consuming ROM randomness. */
    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, boolean flashCountdownActive) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int variant = entity.spriteVariant();
        boolean spawnProjectile = false;
        boolean teleportJingle = false;
        int x = entity.x();
        int y = entity.y();
        switch (state[slot]) {
            case STATE_RESTING -> {
                variant = (frameCounter >>> 4) & 0x01;
                if (transitionCountdown[slot] == 0x18) {
                    spawnProjectile = true;
                } else if (transitionCountdown[slot] <= 0x18
                    && !flashCountdownActive
                    && inSignedWindow(linkEntityX - x, 0x20)
                    && inSignedWindow(linkEntityY - y, 0x20)) {
                    transitionCountdown[slot] = 0x20;
                    state[slot] = STATE_DISAPPEARING;
                    teleportJingle = true;
                }
            }
            case STATE_DISAPPEARING -> {
                if (transitionCountdown[slot] < 0x18) {
                    if (transitionCountdown[slot] == 0) {
                        transitionCountdown[slot] = 0x40;
                        state[slot] = STATE_REAPPEARING;
                        variant = -1;
                        x = (0x50 - (yByte(x) - 0x50)) & 0xFF;
                        y = (0x48 - (yByte(y) - 0x48)) & 0xFF;
                    } else {
                        variant = DISAPPEARING_VARIANTS[
                            (transitionCountdown[slot] >>> 3) & 0x03];
                    }
                }
            }
            case STATE_REAPPEARING -> {
                if (transitionCountdown[slot] < 0x18) {
                    if (transitionCountdown[slot] == 0) {
                        transitionCountdown[slot] = 0x30;
                        state[slot] = STATE_RESTING;
                        variant = -1;
                    } else {
                        variant = REAPPEARING_VARIANTS[
                            (transitionCountdown[slot] >>> 3) & 0x03];
                    }
                }
            }
            default -> throw new IllegalStateException("Invalid Pairodd state: "
                + state[slot]);
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, spawnProjectile, teleportJingle);
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    /** The handler calls DefaultEnemyDamageCollisionHandler only in these phases. */
    boolean allowsEnemyCollision(int slot) {
        return switch (state[slot]) {
            case STATE_RESTING -> true;
            case STATE_DISAPPEARING -> transitionCountdown[slot] >= 0x18;
            case STATE_REAPPEARING -> false;
            default -> throw new IllegalStateException("Invalid Pairodd state: "
                + state[slot]);
        };
    }

    private void reset(int slot) {
        state[slot] = STATE_RESTING;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
    }

    private static boolean inSignedWindow(int value, int halfOpen) {
        int distance = signedByte(value);
        return distance >= -halfOpen && distance < halfOpen;
    }

    private static int yByte(int value) {
        return value & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record Update(RoomEntity entity, boolean spawnProjectile, boolean teleportJingle) {
    }
}
