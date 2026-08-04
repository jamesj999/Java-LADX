package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 SpikeTrapEntityHandler's launch, travel, and return loop. */
final class SpikeTrapMotion {
    private static final int[] SPEED_X_BY_DIRECTION = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] RETURN_SPEED_X_BY_DIRECTION = {0xF8, 0x08, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xE0, 0x20};
    private static final int[] RETURN_SPEED_Y_BY_DIRECTION = {
        0x00, 0x00, 0x08, 0xF8, 0x30, 0x20
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection in bank $03. */
    void initialize(int slot, IntSupplier randomByteSupplier) {
        reset(slot);
        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            // A directly-created ACTIVE snapshot has no EntityInitHandler pass.
            // Keep that test/tool path deterministic while normal room loads use
            // the injected ROM random byte during INIT.
            initialize(slot, () -> 0);
        }

        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        return switch (state[slot]) {
            case 0 -> captureStart(entity, slot);
            case 1 -> launch(entity, slot, linkEntityX, linkEntityY, backgroundCollision);
            case 2 -> advanceForward(entity, slot, backgroundCollision);
            case 3 -> advanceReturn(entity, slot);
            default -> throw new IllegalStateException("Invalid Spike Trap state: "
                + state[slot]);
        };
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

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
    }

    private RoomEntity captureStart(RoomEntity entity, int slot) {
        privateState1[slot] = entity.x() & 0xFF;
        privateState2[slot] = (entity.y() - entity.z()) & 0xFF;
        state[slot] = 1;
        return entity;
    }

    private RoomEntity launch(RoomEntity entity, int slot, int linkEntityX, int linkEntityY,
                              RoomEntityBackgroundCollision backgroundCollision) {
        if (transitionCountdown[slot] != 0) {
            return entity;
        }

        speedX[slot] = 0;
        speedY[slot] = 0;
        int yDistance = (linkEntityY - entity.y()) & 0xFF;
        if (withinLaunchWindow(yDistance)) {
            int xDistance = (linkEntityX - entity.x()) & 0xFF;
            direction[slot] = signedByte(xDistance) < 0 ? 1 : 0;
            speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
            transitionCountdown[slot] = 0x18;
            if (hasBackgroundCollision(entity, direction[slot], backgroundCollision)) {
                transitionCountdown[slot] = 0;
                return entity;
            }
            state[slot] = 2;
            return entity;
        }

        int xDistance = (linkEntityX - entity.x()) & 0xFF;
        if (!withinLaunchWindow(xDistance)) {
            return entity;
        }

        int yDistanceDirection = signedByte(yDistance) < 0 ? 2 : 3;
        direction[slot] = yDistanceDirection;
        speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
        transitionCountdown[slot] = 0x10;
        if (hasBackgroundCollision(entity, direction[slot], backgroundCollision)) {
            transitionCountdown[slot] = 0;
            return entity;
        }
        state[slot] = 2;
        return entity;
    }

    private RoomEntity advanceForward(RoomEntity entity, int slot,
                                      RoomEntityBackgroundCollision backgroundCollision) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);

        if (transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x20;
            state[slot] = 3;
        } else if (hasBackgroundCollision(moved, direction[slot], backgroundCollision)) {
            transitionCountdown[slot] = 0x20;
            state[slot] = 3;
        }
        return moved;
    }

    private RoomEntity advanceReturn(RoomEntity entity, int slot) {
        if (transitionCountdown[slot] != 0) {
            return entity;
        }

        speedX[slot] = RETURN_SPEED_X_BY_DIRECTION[direction[slot]];
        speedY[slot] = RETURN_SPEED_Y_BY_DIRECTION[direction[slot]];
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);
        // The ROM compares the saved visual Y against raw wEntitiesPosYTable.
        if (x == privateState1[slot] && y == privateState2[slot]) {
            transitionCountdown[slot] = 0x20;
            state[slot] = 1;
        }
        return moved;
    }

    private static boolean hasBackgroundCollision(RoomEntity entity, int direction,
                                                   RoomEntityBackgroundCollision backgroundCollision) {
        if (backgroundCollision == null) {
            return false;
        }
        // SpikeTrapEntityHandler sets hActiveEntityNoBGCollision. The shared
        // ROM routine still records collision bits, but does not snap the
        // entity back to the collision point.
        return backgroundCollision.blocks(entity, direction, entity.x(), entity.y());
    }

    private static boolean withinLaunchWindow(int distance) {
        return (((distance & 0xFF) + 0x12) & 0xFF) < 0x24;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
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

    private void reset(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }
}
