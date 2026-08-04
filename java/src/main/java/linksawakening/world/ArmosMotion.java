package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 ArmosStatueEntityHandler states 0, 1, and 2. */
final class ArmosMotion {
    // Data_006_74C2. The state-2 X table is eight bytes long.
    private static final int[] SPEED_X_BY_INDEX = {
        0x00, 0x06, 0x08, 0x06, 0x00, 0xFA, 0xF8, 0xFA
    };

    // Data_006_74C0 is immediately before Data_006_74C2. The handler indexes
    // this contiguous ROM region with the same 0..7 value, so entries 2..7
    // intentionally read the first six bytes of the X table.
    private static final int[] SPEED_Y_BY_INDEX = {
        0xF8, 0xFA, 0x00, 0x06, 0x08, 0x06, 0x00, 0xFA
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int x = entity.x();
        int y = entity.y();
        boolean linkCollision = RoomEntityCombatRules.overlapsLink(
            entity, linkEntityX, linkEntityY);
        if (linkCollision && state[slot] < 2) {
            // The original copies hLinkFinalPositionX/Y here. The current
            // room API exposes hLinkPositionX/Y, so its supplied coordinates
            // are the faithful available source until final-position state is
            // threaded through RoomEntityRuntime.
            x = linkEntityX & 0xFF;
            y = linkEntityY & 0xFF;
        }

        x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);

        if (state[slot] == 0) {
            if (linkCollision) {
                state[slot] = 1;
                transitionCountdown[slot] = 0x30;
            }
        } else if (state[slot] == 1) {
            if (transitionCountdown[slot] == 0) {
                state[slot] = 2;
                speedX[slot] = 0;
                speedY[slot] = 0;
            } else {
                speedX[slot] = (transitionCountdown[slot] & 0x04) == 0
                    ? 0x08 : 0xF8;
            }
        } else if (transitionCountdown[slot] == 0) {
            int random = randomByteSupplier.getAsInt() & 0xFF;
            transitionCountdown[slot] = 0x20 | (random & 0x3F);
            int index = random & 0x07;
            speedX[slot] = SPEED_X_BY_INDEX[index];
            speedY[slot] = SPEED_Y_BY_INDEX[index];
        }

        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = speed < 0x80 ? speed : speed - 0x100;
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }
}
