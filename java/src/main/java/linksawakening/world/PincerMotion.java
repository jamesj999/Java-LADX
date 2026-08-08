package linksawakening.world;

/** Bank-$07 PincerEntityHandler's hidden, lunge, and return state machine. */
final class PincerMotion {
    private static final int ENTITY_TYPE = 0xB0;
    private static final int PROJECTILE_NOCLIP = 0x40;
    private static final int[] HORIZONTAL_LUNGE_DIRECTIONS = {
        0, 0, 1, 1, 1, 2, 2, 2,
        0, 0, 15, 15, 15, 14, 14, 14,
        8, 8, 7, 7, 7, 6, 6, 6,
        8, 8, 9, 9, 9, 10, 10, 10
    };
    private static final int[] VERTICAL_LUNGE_DIRECTIONS = {
        4, 4, 3, 3, 3, 2, 2, 2,
        12, 12, 13, 13, 13, 14, 14, 14,
        4, 4, 5, 5, 5, 6, 6, 6,
        12, 12, 11, 11, 11, 10, 10, 10
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] holeX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] holeY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] lungeVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int state, int transitionCountdown, int physicsFlags) {
    }

    void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int transitionCountdown, int physicsFlags,
                   int linkX, int linkY) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int x = entity.x();
        int y = entity.y();
        int variant = entity.spriteVariant();
        int nextCountdown = transitionCountdown & 0xFF;
        int nextPhysicsFlags = physicsFlags & 0xFF;
        switch (state[slot]) {
            case 0 -> {
                holeX[slot] = x;
                holeY[slot] = y;
                state[slot] = 1;
            }
            case 1 -> {
                variant = 0;
                if (nextCountdown == 0) {
                    nextPhysicsFlags |= PROJECTILE_NOCLIP;
                    if (withinSignedWindow(linkX - x, 0x20)
                        && withinSignedWindow(linkY - y, 0x20)) {
                        nextCountdown = 0x30;
                        state[slot] = 2;
                    }
                }
            }
            case 2 -> {
                if (nextCountdown == 0) {
                    variant = lungeVariant[slot];
                    nextCountdown = 0x18;
                    nextPhysicsFlags &= ~PROJECTILE_NOCLIP;
                    state[slot] = 3;
                } else {
                    variant = 1;
                    if (nextCountdown == 0x10) {
                        Vector direction = vectorTowards(x, y, entity.z(), linkX, linkY, 0x1F);
                        lungeVariant[slot] = lungeVariantForVector(
                            direction.y(), direction.x());
                        Vector speed = vectorTowards(x, y, entity.z(), linkX, linkY, 0x18);
                        speedY[slot] = speed.y();
                        speedX[slot] = speed.x();
                    }
                }
            }
            case 3 -> {
                Position position = addSpeed(x, y, slot);
                x = position.x();
                y = position.y();
                if (nextCountdown == 0) {
                    nextCountdown = 0x20;
                    state[slot] = 4;
                }
                variant = lungeVariant[slot];
            }
            case 4 -> {
                if (nextCountdown == 0) {
                    state[slot] = 5;
                }
                variant = lungeVariant[slot];
            }
            case 5 -> {
                Vector returnSpeed = vectorTowards(x, y, entity.z(),
                    holeX[slot], holeY[slot], 0x10);
                speedY[slot] = returnSpeed.y();
                speedX[slot] = returnSpeed.x();
                boolean atHole = withinReturnWindow(holeX[slot] - x)
                    && withinReturnWindow(holeY[slot] - y);
                if (atHole) {
                    x = holeX[slot];
                    y = holeY[slot];
                    variant = 0;
                    state[slot] = 1;
                    nextCountdown = 0x20;
                }
                Position position = addSpeed(x, y, slot);
                x = position.x();
                y = position.y();
                if (!atHole) {
                    variant = lungeVariant[slot];
                }
            }
            default -> {
                state[slot] = 0;
                variant = 0;
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), ENTITY_TYPE,
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, state[slot], nextCountdown, nextPhysicsFlags);
    }

    int state(int slot) {
        return state[slot];
    }

    int holeX(int slot) {
        return holeX[slot] & 0xFF;
    }

    int holeY(int slot) {
        return holeY[slot] & 0xFF;
    }

    int lungeVariant(int slot) {
        return lungeVariant[slot];
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    void clear(int slot) {
        state[slot] = 0;
        holeX[slot] = 0;
        holeY[slot] = 0;
        lungeVariant[slot] = 2;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    void forceStateForTest(int slot, int newState, int transitionCountdown,
                           int physicsFlags) {
        state[slot] = newState & 0xFF;
        lungeVariant[slot] = 2;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    void setHoleForTest(int slot, int x, int y) {
        holeX[slot] = x & 0xFF;
        holeY[slot] = y & 0xFF;
    }

    static int lungeVariantForVector(int vectorY, int vectorX) {
        int y = signedByte(vectorY);
        int x = signedByte(vectorX);
        int row = (y < 0 ? 0x08 : 0) | (x < 0 ? 0x10 : 0);
        int absoluteY = Math.abs(y);
        int absoluteX = Math.abs(x);
        int direction;
        if (absoluteY < absoluteX) {
            direction = HORIZONTAL_LUNGE_DIRECTIONS[row + (absoluteY >> 2)];
        } else {
            direction = VERTICAL_LUNGE_DIRECTIONS[row + (absoluteX >> 2)];
        }
        return (direction >> 1) + 2;
    }

    private static Vector vectorTowards(int entityX, int entityY, int entityZ,
                                        int targetX, int targetY, int length) {
        int distanceY = signedByte(targetY - entityY + entityZ);
        int distanceX = signedByte(targetX - entityX);
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int quotient = larger == 0 ? length : (length * smaller) / larger;
        int y = absoluteY < absoluteX ? quotient : length;
        int x = absoluteY < absoluteX ? length : quotient;
        if (distanceY < 0) {
            y = -y;
        }
        if (distanceX < 0) {
            x = -x;
        }
        return new Vector(y & 0xFF, x & 0xFF);
    }

    private Position addSpeed(int x, int y, int slot) {
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        return new Position(nextX, nextY);
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

    private static boolean withinSignedWindow(int difference, int radius) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -radius && signedDifference < radius;
    }

    private static boolean withinReturnWindow(int difference) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -2 && signedDifference < 2;
    }

    private static int signedByte(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }

    private record Vector(int y, int x) {
    }

    private record Position(int x, int y) {
    }
}
