package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$04 motion and state machines shared by the two Bombite handlers. */
final class BombiteMotion {
    static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    static final int ENTITY_TIMER_BOMBITE = 0x56;

    private static final int[] TIMER_SPEED_X = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] TIMER_SPEED_Y = {0x00, 0x00, 0xF8, 0x00};
    private static final int[] BOUNCING_SPEED_X = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] BOUNCING_SPEED_Y = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] TIMER_COUNTDOWN_VARIANTS = {5, 5, 4, 3, 2, 2, 2};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown,
                  int slowTransitionCountdown, int privateCountdown1,
                  int spriteVariant, boolean explode, boolean bumpJingle) {
    }

    void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   int transitionCountdown, int slowTransitionCountdown,
                   int privateCountdown1, int enemyIgnoreHitsCountdown,
                   boolean runningWithPegasusBoots,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int nextTransition = transitionCountdown & 0xFF;
        int nextSlowTransition = slowTransitionCountdown & 0xFF;
        int nextPrivateCountdown1 = privateCountdown1 & 0xFF;
        int spriteVariant = entity.spriteVariant();
        boolean explode = false;
        boolean bumpJingle = false;

        if (entity.type() == ENTITY_TIMER_BOMBITE
            && enemyIgnoreHitsCountdown == 0x08 && state[slot] == 0) {
            // TimerBombiteEntityHandler's pre-movement hit-countdown trigger.
            state[slot] = 1;
            nextSlowTransition = 0x6F;
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        boolean blockedX = false;
        boolean blockedY = false;
        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity,
                horizontalDirection(speedX[slot]), x, entity.y())) {
                x = entity.x();
                blockedX = true;
            }
            if (y != entity.y() && backgroundCollision.blocks(entity,
                verticalDirection(speedY[slot]), x, y)) {
                y = entity.y();
                blockedY = true;
            }
        }

        RoomEntity moved = withPosition(entity, x, y);
        if (entity.type() == ENTITY_TIMER_BOMBITE) {
            if (state[slot] == 0) {
                if (nextTransition == 0) {
                    int random = randomByteSupplier.getAsInt() & 0xFF;
                    nextTransition = 0x30 + (random & 0x1F);
                    int direction = random & 0x03;
                    speedX[slot] = TIMER_SPEED_X[direction];
                    speedY[slot] = TIMER_SPEED_Y[direction];
                }
                spriteVariant = (frameCounter >>> 4) & 0x01;
            } else {
                if (runningWithPegasusBoots) {
                    state[slot] = 0;
                } else {
                    int xDistance = unsignedByteAbs(linkEntityX - moved.x());
                    int yDistance = unsignedByteAbs(linkEntityY - moved.y());
                    if (xDistance >= 0x12 || yDistance >= 0x12) {
                        if (((frameCounter ^ slot) & 0x03) == 0) {
                            Vector vector = vectorTowardsLink(
                                moved.x(), moved.y(), moved.z(),
                                linkEntityX, linkEntityY, 0x0E);
                            speedX[slot] = vector.x();
                            speedY[slot] = vector.y();
                        }
                    } else {
                        speedX[slot] = 0;
                        speedY[slot] = 0;
                    }

                    int originalSlowTransition = nextSlowTransition;
                    if (originalSlowTransition == 0) {
                        explode = true;
                    } else {
                        if (originalSlowTransition == 0x18) {
                            nextSlowTransition = 0x0A;
                            nextPrivateCountdown1 = 0x30;
                        }
                        spriteVariant = TIMER_COUNTDOWN_VARIANTS[
                            Math.min(originalSlowTransition >>> 4,
                                TIMER_COUNTDOWN_VARIANTS.length - 1)];
                    }
                }
            }
        } else if (entity.type() == ENTITY_BOUNCING_BOMBITE) {
            switch (state[slot]) {
                case 0 -> {
                    if (nextTransition == 0) {
                        state[slot] = 1;
                    }
                    spriteVariant = (frameCounter >>> 4) & 0x01;
                }
                case 1 -> {
                    int directionChoice = randomByteSupplier.getAsInt() & 0x03;
                    int direction = directionChoice == 0
                        ? directionToLink(moved.x(), moved.y(), linkEntityX, linkEntityY)
                        : randomByteSupplier.getAsInt() & 0x03;
                    speedX[slot] = BOUNCING_SPEED_X[direction];
                    speedY[slot] = BOUNCING_SPEED_Y[direction];
                    nextTransition = 0x20 + (randomByteSupplier.getAsInt() & 0x0F);
                    state[slot] = 0;
                }
                case 2 -> {
                    if (nextTransition == 0) {
                        explode = true;
                    } else {
                        if (blockedX) {
                            speedX[slot] = negate(speedX[slot]);
                            bumpJingle = true;
                        } else if (blockedY) {
                            speedY[slot] = negate(speedY[slot]);
                            bumpJingle = true;
                        }
                        spriteVariant = (frameCounter >>> 1) & 0x01;
                    }
                }
                default -> throw new IllegalStateException(
                    "Invalid Bombite state: " + state[slot]);
            }
        }

        return new Update(moved, nextTransition, nextSlowTransition,
            nextPrivateCountdown1, spriteVariant, explode, bumpJingle);
    }

    /** Starts the BouncingBombite state-$02 sword-recoil path. */
    void enterBouncingLitFromSword(int slot, RoomEntity entity,
                                   int linkEntityX, int linkEntityY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(),
            linkEntityX, linkEntityY, 0x30);
        speedX[slot] = negate(vector.x());
        speedY[slot] = negate(vector.y());
        state[slot] = 2;
    }

    /**
     * Mirrors func_003_75A2's target-side Bombite writes. The ROM copies the
     * active Bombite's current speed bytes directly; it does not recalculate
     * the vector or reset the fixed-point movement accumulators.
     */
    void enterBouncingLitFromEntityCollision(int slot, int sourceSpeedX,
                                              int sourceSpeedY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        speedX[slot] = sourceSpeedX & 0xFF;
        speedY[slot] = sourceSpeedY & 0xFF;
        state[slot] = 2;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot] & 0xFF;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                             int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int smallerComponent = divideComponent(smallerDistance, largerDistance, length);
        int x = absoluteX >= absoluteY ? length : smallerComponent;
        int y = absoluteY > absoluteX ? length : smallerComponent;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY <= 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int divideComponent(int smallerDistance, int largerDistance, int length) {
        if (largerDistance == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        return result;
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

    private static int negate(int speed) {
        return (-signedByte(speed)) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private record Vector(int x, int y) {
    }
}
