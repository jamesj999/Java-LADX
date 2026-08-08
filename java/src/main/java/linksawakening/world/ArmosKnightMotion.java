package linksawakening.world;

/** Bank-$06 ArmosKnightEntityHandler's active state machine. */
final class ArmosKnightMotion {
    record RubbleRequest(int x, int y) {
    }

    record Update(RoomEntity entity, int transitionCountdown, int physicsFlags,
                  int hitboxFlags, int options1, int jingleId,
                  boolean linkMotionBlocked, RubbleRequest rubbleRequest) {
    }

    private static final int STATE_WAKE = 0;
    private static final int STATE_CHARGE = 1;
    private static final int STATE_CHARGE_MOVE = 2;
    private static final int STATE_JUMP_DELAY = 3;
    private static final int STATE_JUMP = 4;
    private static final int STATE_LAND_DELAY = 5;
    private static final int STATE_BOUNCE = 6;
    private static final int STATE_BOUNCE_DELAY = 7;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = STATE_WAKE;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        privateCountdown1[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ,
                   int transitionCountdown, int physicsFlags, int hitboxFlags,
                   int options1, int health) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        boolean linkMotionBlocked = privateCountdown1[slot] != 0;
        int x = entity.x();
        int y = entity.y();
        int z = entity.z();
        boolean landed = false;
        if (speedZ[slot] != 0) {
            z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
            speedZ[slot] = (speedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                speedZ[slot] = 0;
                landed = true;
            }
        }

        int variant = entity.spriteVariant();
        if (health < 0x08) {
            variant = health < 0x04 ? 0x03 : 0x02;
        }
        RubbleRequest rubbleRequest = null;
        if ((health & 0xFF) != privateState1[slot]) {
            privateState1[slot] = health & 0xFF;
            if ((health & 0xFF) < 0x08) {
                int maximumInertia = (health & 0xFF) < 0x04 ? 2 : 1;
                if (inertia[slot] < maximumInertia) {
                    inertia[slot]++;
                    int rubbleX = (health & 0xFF) < 0x04 ? x - 1 : x + 7;
                    int rubbleY = y - z - 0x10;
                    rubbleRequest = new RubbleRequest(rubbleX & 0xFF, rubbleY & 0xFF);
                }
            }
        }
        int jingleId = -1;
        switch (state[slot]) {
            case STATE_WAKE -> {
                if (withinDistance(x, linkEntityX, 0x20)
                    && withinDistance(y, linkEntityY, 0x20)) {
                    state[slot] = STATE_CHARGE;
                    transitionCountdown = 0x30;
                }
            }
            case STATE_CHARGE -> {
                if (transitionCountdown == 0) {
                    transitionCountdown = 0x80;
                    state[slot] = STATE_CHARGE_MOVE;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    variant = 0;
                } else {
                    variant = (transitionCountdown >>> 2) & 0x01;
                }
            }
            case STATE_CHARGE_MOVE -> {
                if (transitionCountdown == 0) {
                    transitionCountdown = 0x50;
                    state[slot] = STATE_JUMP_DELAY;
                    physicsFlags &= 0x7F;
                    hitboxFlags &= 0x7F;
                    options1 &= 0xBF;
                } else {
                    speedX[slot] = (transitionCountdown & 0x04) == 0 ? 0x08 : 0xF8;
                    x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                }
            }
            case STATE_JUMP_DELAY -> {
                if (transitionCountdown == 0) {
                    speedZ[slot] = 0x30;
                    state[slot] = STATE_JUMP;
                    jingleId = 0x24;
                } else {
                    if (landed) {
                        speedZ[slot] = 0x0C;
                        int length = health < 0x05 ? 0x0C : 0x08;
                        Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY,
                            length);
                        speedX[slot] = vector.x();
                        speedY[slot] = vector.y();
                        jingleId = 0x20;
                    }
                    x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                    y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
                }
            }
            case STATE_JUMP -> {
                if ((speedZ[slot] & 0xFE) == 0) {
                    transitionCountdown = 0x10;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    state[slot] = STATE_LAND_DELAY;
                }
                x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                y = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
            }
            case STATE_LAND_DELAY -> {
                if (transitionCountdown == 0) {
                    state[slot] = STATE_BOUNCE;
                    speedZ[slot] = 0xB0;
                } else {
                    speedZ[slot] = 0;
                }
            }
            case STATE_BOUNCE -> {
                if (landed) {
                    transitionCountdown = 0x30;
                    jingleId = 0x0B;
                    if (linkZ == 0) {
                        privateCountdown1[slot] = 0x40;
                    }
                    state[slot] = STATE_BOUNCE_DELAY;
                }
            }
            case STATE_BOUNCE_DELAY -> {
                if (transitionCountdown == 0) {
                    state[slot] = STATE_CHARGE_MOVE;
                }
            }
            default -> {
                state[slot] = STATE_WAKE;
                speedX[slot] = 0;
                speedY[slot] = 0;
                speedZ[slot] = 0;
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
        return new Update(updated, transitionCountdown, physicsFlags, hitboxFlags, options1,
            jingleId, linkMotionBlocked, rubbleRequest);
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

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int inertia(int slot) {
        return inertia[slot];
    }

    private static boolean withinDistance(int value, int target, int distance) {
        int delta = (value - target) & 0xFF;
        int absolute = delta < 0x80 ? delta : 0x100 - delta;
        return absolute < distance;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance == 0 || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }

        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
