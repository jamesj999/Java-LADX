package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$04 LeeverEntityHandler's hide, emerge, chase, and burrow states. */
final class LeeverMotion {
    private static final int[] EMERGING_VARIANTS = {0x01, 0x00};
    private static final int[] BURROWING_VARIANTS = {0x00, 0x01};

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
                       IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);

        // ApplyEntityInteractionWithBackground restores blocked coordinates
        // before the Leever state handler; it does not reverse ordinary speed.
        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity,
                    directionForX(speedX[slot]), x, y)) {
                x = entity.x();
            }
            if (y != entity.y() && backgroundCollision.blocks(entity,
                    directionForY(speedY[slot]), x, y)) {
                y = entity.y();
            }
        }

        int variant = entity.spriteVariant();
        switch (state[slot]) {
            case 0 -> {
                variant = -1;
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x1F;
                    state[slot] = 1;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                }
            }
            case 1 -> {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x70
                        + (randomByteSupplier.getAsInt() & 0x3F);
                    state[slot] = 2;
                } else {
                    variant = EMERGING_VARIANTS[Math.min(
                        transitionCountdown[slot] >>> 4, EMERGING_VARIANTS.length - 1)];
                }
            }
            case 2 -> {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x1F;
                    state[slot] = 3;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                } else {
                    int frame = frameCounter & 0xFF;
                    if (((frame ^ slot) & 0x0F) == 0) {
                        Vector vector = vectorTowardsLink(x, y, entity.z(),
                            linkEntityX, linkEntityY, 0x08);
                        speedX[slot] = vector.x();
                        speedY[slot] = vector.y();
                    }
                    // SetEntitySpriteVariant followed by two INC [HL] writes
                    // selects the lower pair of the four-entry display list.
                    variant = (((frame ^ slot) >>> 2) & 0x01) + 0x02;
                }
            }
            case 3 -> {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x30
                        + (randomByteSupplier.getAsInt() & 0x1F);
                    state[slot] = 0;
                    Vector vector = vectorTowardsLink(x, y, entity.z(),
                        linkEntityX, linkEntityY, 0x08);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                } else {
                    variant = BURROWING_VARIANTS[Math.min(
                        transitionCountdown[slot] >>> 4, BURROWING_VARIANTS.length - 1)];
                }
            }
            default -> throw new IllegalStateException("Invalid Leever state: " + state[slot]);
        }

        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    boolean isChasing(int slot) {
        return state[slot] == 2;
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

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        boolean swapped = Math.abs(distanceX) < Math.abs(distanceY);
        int smallerDistance = Math.min(Math.abs(distanceX), Math.abs(distanceY));
        int largerDistance = Math.max(Math.abs(distanceX), Math.abs(distanceY));
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum > 0xFF || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }

        int x = swapped ? result : length;
        int y = swapped ? length : result;
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

    private static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Vector(int x, int y) {
    }
}
