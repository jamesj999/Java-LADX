package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 WaterTektiteEntityHandler's three-state movement loop. */
public final class WaterTektiteMotion {
    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState1Table.
    private final int[] accelerationX = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState2Table.
    private final int[] accelerationY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $99 uses EntityInitNoop; the handler owns all later state. */
    public void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    public RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        boolean collision = false;
        {
            if (speedX[slot] != 0 && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), x, y)) {
                x = entity.x();
                collision = true;
            }
            if (speedY[slot] != 0 && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), x, y)) {
                y = entity.y();
                collision = true;
            }
        }

        if (collision) {
            // ApplyEntityInteractionWithBackground restores the active
            // coordinate for normal wall collisions before the handler reads
            // wEntitiesCollisionsTable. The handler then clears speed, resets
            // state to zero, and waits ten frames before trying again.
            speedX[slot] = 0;
            speedY[slot] = 0;
            state[slot] = 0;
            transitionCountdown[slot] = 0x10;
        } else {
            advanceState(slot, randomByteSupplier, frameCounter);
        }

        int variant = (frameCounter >>> 4) & 0x01;
        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    public int accelerationX(int slot) {
        return accelerationX[slot];
    }

    public int accelerationY(int slot) {
        return accelerationY[slot];
    }

    private void advanceState(int slot, IntSupplier randomByteSupplier, int frameCounter) {
        switch (state[slot]) {
            case 0 -> {
                if (transitionCountdown[slot] != 0) {
                    return;
                }
                transitionCountdown[slot] = 0x20;
                state[slot] = 1;
                accelerationX[slot] = ((randomByteSupplier.getAsInt() & 0x02) - 1) & 0xFF;
                accelerationY[slot] = ((randomByteSupplier.getAsInt() & 0x02) - 1) & 0xFF;
            }
            case 1 -> {
                if (transitionCountdown[slot] == 0) {
                    state[slot] = 2;
                } else if ((transitionCountdown[slot] & 0x01) == 0) {
                    speedX[slot] = (speedX[slot] + accelerationX[slot]) & 0xFF;
                    speedY[slot] = (speedY[slot] + accelerationY[slot]) & 0xFF;
                }
            }
            case 2 -> {
                if ((frameCounter & 0x01) != 0) {
                    return;
                }
                if (speedX[slot] == 0) {
                    state[slot] = 0;
                    transitionCountdown[slot] = 0x10;
                } else {
                    speedX[slot] = convergeSpeedToZero(speedX[slot]);
                    speedY[slot] = convergeSpeedToZero(speedY[slot]);
                }
            }
            default -> throw new IllegalStateException("Invalid Water Tektite state: " + state[slot]);
        }
    }

    private static int convergeSpeedToZero(int speed) {
        speed &= 0xFF;
        if ((speed & 0x80) != 0) {
            speed = (speed + 2) & 0xFF;
        }
        return (speed - 1) & 0xFF;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private void reset(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        accelerationX[slot] = 0;
        accelerationY[slot] = 0;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }
}
