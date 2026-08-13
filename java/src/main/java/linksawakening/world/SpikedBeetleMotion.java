package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 SpikedBeetleEntityHandler's rest, walk, dash, and flip states. */
final class SpikedBeetleMotion {
    static final int ENTITY_TYPE = 0x2C;
    static final int NORMAL_OPTIONS1 = 0x48;
    static final int FLIPPED_OPTIONS1 = 0x08;
    static final int NORMAL_HITBOX_FLAGS = 0x80;
    static final int FLIPPED_HITBOX_FLAGS = 0x00;

    private static final int[] WALKING_X_SPEEDS = {0x06, 0xFA, 0x00, 0x00};
    private static final int[] WALKING_Y_SPEEDS = {0x00, 0x00, 0xFA, 0x06};
    private static final int[] DASHING_X_SPEEDS = {0x18, 0xE8, 0x00, 0x00};
    private static final int[] DASHING_Y_SPEEDS = {0x00, 0x00, 0xE8, 0x18};
    private static final int[] FLIP_X_SPEEDS = {0x10, 0xF0, 0x00, 0x00};
    private static final int[] FLIP_Y_SPEEDS = {0x00, 0x00, 0xF0, 0x10};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] animationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        animationVariant[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown, int ignoreHitsCountdown) {
        if ((entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Spiked Beetle entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Spiked Beetle random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 2) & 0xFF;

        // SpikedBeetleEntityHandler clears horizontal speed while the shared
        // ignore-hits window is active, before applying position movement.
        if ((ignoreHitsCountdown & 0xFF) != 0) {
            clearSpeed(slot);
        }

        boolean landed = (z & 0x80) != 0 || z == 0;
        if (landed) {
            z = 0;
            if (state[slot] == 3) {
                speedX[slot] = halveSignedSpeed(speedX[slot]);
                speedY[slot] = halveSignedSpeed(speedY[slot]);
                speedZ[slot] = complementHalfSpeed(speedZ[slot]);
                if (speedZ[slot] < 0x07) {
                    speedZ[slot] = 0;
                    clearSpeed(slot);
                }
            } else {
                speedZ[slot] = 0;
            }
        }

        int renderedVariant = animationVariant[slot];
        boolean collided = false;
        if (state[slot] != 4) {
            Position moved = move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            collided = moved.collided();
        }

        boolean walkAnimationRequested = false;
        boolean flippedAnimationRequested = false;
        switch (state[slot]) {
            case 0 -> {
                clearSpeed(slot);
                if (countdown == 0) {
                    countdown = 0x30 + ((randomByteSupplier.getAsInt() & 0xFF) & 0x1F);
                    state[slot] = 1;
                    int randomDirection = randomByteSupplier.getAsInt() & 0xFF;
                    int direction = (randomDirection & 0x06) == 0
                        ? directionToLink(x, y, linkEntityX, linkEntityY)
                        : (randomByteSupplier.getAsInt() & 0x03);
                    setWalkingSpeed(slot, direction);
                }
                if (nearLinkOnOneAxis(x, y, linkEntityX, linkEntityY)) {
                    startDash(slot, linkEntityX, linkEntityY, x, y);
                    countdown = 0xFF;
                }
            }
            case 1 -> {
                walkAnimationRequested = true;
                if (countdown == 0) {
                    countdown = 0x18;
                    state[slot] = 0;
                }
                if (nearLinkOnOneAxis(x, y, linkEntityX, linkEntityY)) {
                    startDash(slot, linkEntityX, linkEntityY, x, y);
                    countdown = 0xFF;
                }
            }
            case 2 -> {
                walkAnimationRequested = true;
                if (countdown == 0 || collided) {
                    state[slot] = 0;
                } else {
                    int dashDirection = direction[slot];
                    speedX[slot] = approach(speedX[slot], DASHING_X_SPEEDS[dashDirection]);
                    speedY[slot] = approach(speedY[slot], DASHING_Y_SPEEDS[dashDirection]);
                }
            }
            case 3 -> {
                flippedAnimationRequested = true;
                if (countdown == 0) {
                    state[slot] = 4;
                    speedZ[slot] = 0x18;
                    speedX[slot] = 0;
                } else if (countdown < 0x60) {
                    speedX[slot] = (countdown & 0x04) == 0 ? 0x08 : 0xF8;
                }
            }
            case 4 -> {
                // The source handler returns immediately after the one-frame
                // state-$04 handoff. Keep the final flipped presentation.
            }
            default -> throw new IllegalStateException("Invalid Spiked Beetle state: "
                + state[slot]);
        }

        int nextVariant = animationVariant[slot];
        if (walkAnimationRequested) {
            nextVariant = (frameCounter >>> 4) & 0x01;
        } else if (flippedAnimationRequested) {
            nextVariant = 2 + ((frameCounter >>> 4) & 0x01);
        }
        animationVariant[slot] = nextVariant;
        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), renderedVariant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
        return new Update(updated, countdown, state[slot] >= 3
            ? FLIPPED_OPTIONS1 : NORMAL_OPTIONS1,
            state[slot] >= 3 ? FLIPPED_HITBOX_FLAGS : NORMAL_HITBOX_FLAGS);
    }

    /** Mirrors EnemyCollidedWithSword's ENTITY_SPIKED_BEETLE branch. */
    void flipFromSword(int slot, int romLinkDirection) {
        if (romLinkDirection < 0 || romLinkDirection > 3) {
            throw new IllegalArgumentException("ROM Link direction must be 0..3: "
                + romLinkDirection);
        }
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (state[slot] == 3 || state[slot] == 4) {
            return;
        }
        state[slot] = 3;
        speedZ[slot] = 0x20;
        speedX[slot] = FLIP_X_SPEEDS[romLinkDirection];
        speedY[slot] = FLIP_Y_SPEEDS[romLinkDirection];
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

    boolean allowsCombat(int slot) {
        // SpikedBeetleEntityHandler calls DefaultEnemyDamageCollisionHandler
        // before dispatching every state, including flipped states $03/$04.
        return initialized[slot];
    }

    private void startDash(int slot, int linkEntityX, int linkEntityY, int entityX,
                           int entityY) {
        state[slot]++;
        direction[slot] = directionToLink(entityX, entityY, linkEntityX, linkEntityY);
    }

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];

    private void setWalkingSpeed(int slot, int newDirection) {
        direction[slot] = newDirection & 0x03;
        speedX[slot] = WALKING_X_SPEEDS[direction[slot]];
        speedY[slot] = WALKING_Y_SPEEDS[direction[slot]];
    }

    private static boolean nearLinkOnOneAxis(int entityX, int entityY,
                                              int linkEntityX, int linkEntityY) {
        return withinSourceWindow((linkEntityX - entityX) & 0xFF)
            || withinSourceWindow((linkEntityY - entityY) & 0xFF);
    }

    private static boolean withinSourceWindow(int distance) {
        return (((distance & 0xFF) + 0x06) & 0xFF) < 0x0A;
    }

    private static int directionToLink(int entityX, int entityY, int linkEntityX,
                                       int linkEntityY) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private Position move(RoomEntity entity, int x, int y,
                           RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, directionForX(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, directionForY(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    private void clearSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    private static int approach(int current, int target) {
        int difference = (target - current) & 0xFF;
        if (difference == 0) {
            return current & 0xFF;
        }
        return (difference & 0x80) != 0 ? (current - 1) & 0xFF : (current + 1) & 0xFF;
    }

    private static int halveSignedSpeed(int speed) {
        int signed = signedByte(speed);
        return (signed >> 1) & 0xFF;
    }

    private static int complementHalfSpeed(int speed) {
        return (~(signedByte(speed) >> 1)) & 0xFF;
    }

    private static int directionForX(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Position(int x, int y, boolean collided) {
    }

    record Update(RoomEntity entity, int transitionCountdown, int options1, int hitboxFlags) {
    }
}
