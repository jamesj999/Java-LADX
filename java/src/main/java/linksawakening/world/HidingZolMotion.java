package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 Hiding Zol reveal, bounce, and hide state machine. */
final class HidingZolMotion {
    static final int ENTITY_HIDING_ZOL = 0x9B;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] swordCollisionEnabled = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] linkCollisionEnabled = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        swordCollisionEnabled[slot] = false;
        linkCollisionEnabled[slot] = false;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision) {
        RoomEntityBackgroundInteraction backgroundInteraction = backgroundCollision == null
            ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision);
        return advance(entity, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundInteraction, 0);
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int frameCounter) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        // State 3 calls label_3B70, which enters the sword/damage continuation
        // without the Link-contact prelude. States 4 and 5 call the complete
        // DefaultEnemyDamageCollisionHandler.
        swordCollisionEnabled[slot] = state[slot] >= 3 && state[slot] <= 5;
        linkCollisionEnabled[slot] = state[slot] >= 4 && state[slot] <= 5;

        decrementTransitionCountdown(slot);
        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 2) & 0xFF;
        boolean hitGround = (z & 0x80) != 0;
        if (hitGround) {
            z = 0;
            speedZ[slot] = 0;
        }

        int x = entity.x();
        int y = entity.y();
        int variant = entity.spriteVariant();
        boolean clearsIgnoreHitsCountdown = false;
        boolean jumpJingle = false;
        int physicsFlags = -1;
        switch (state[slot]) {
            case 0 -> {
                if (transitionCountdown[slot] == 0
                    && isWithinRevealWindow(entity.x(), entity.y(), linkEntityX, linkEntityY)) {
                    state[slot] = 1;
                    transitionCountdown[slot] = 0x20;
                    privateState1[slot] = 3 + (randomByteSupplier.getAsInt() & 0x03);
                }
            }
            case 1 -> {
                if (transitionCountdown[slot] == 0) {
                    z = 0x08;
                    speedZ[slot] = 0x08;
                    variant = 3;
                    state[slot] = 2;
                    jumpJingle = true;
                    physicsFlags = 0x12;
                } else {
                    variant = transitionCountdown[slot] >= 0x10 ? 1 : 2;
                }
            }
            case 2 -> {
                if (hitGround) {
                    transitionCountdown[slot] = 0x20;
                    state[slot] = 3;
                }
            }
            case 3 -> {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x10;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    state[slot] = 4;
                } else {
                    speedX[slot] = (transitionCountdown[slot] & 0x04) == 0
                        ? 0x08 : 0xF8;
                    x = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
                }
                variant = 3;
            }
            case 4 -> {
                int[] moved = move(entity, x, y, backgroundInteraction, frameCounter, slot);
                x = moved[0];
                y = moved[1];
                clearsIgnoreHitsCountdown = true;
                if (transitionCountdown[slot] == 0) {
                    Vector vector = vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x0C);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                    speedZ[slot] = 0x18;
                    state[slot] = 5;
                    jumpJingle = true;
                }
                variant = 3;
            }
            case 5 -> {
                int[] moved = move(entity, x, y, backgroundInteraction, frameCounter, slot);
                x = moved[0];
                y = moved[1];
                clearsIgnoreHitsCountdown = true;
                if (hitGround) {
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    state[slot] = 4;
                    transitionCountdown[slot] = 0x20;
                    privateState1[slot]--;
                    if (privateState1[slot] == 0) {
                        state[slot] = 6;
                        transitionCountdown[slot] = 0x30;
                        physicsFlags = 0xD2;
                    }
                }
                variant = 2;
            }
            case 6 -> {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x50;
                    state[slot] = 0;
                    variant = 0;
                } else if (transitionCountdown[slot] >= 0x20) {
                    variant = 3;
                } else if (transitionCountdown[slot] >= 0x10) {
                    variant = 2;
                } else {
                    variant = 1;
                }
            }
            default -> {
                state[slot] = 0;
                transitionCountdown[slot] = 0;
                variant = 0;
            }
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(),
            entity.type(), x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
        return new Update(updated, hitGround, clearsIgnoreHitsCountdown, jumpJingle,
            physicsFlags);
    }

    boolean allowsSwordCollision(int slot) {
        return swordCollisionEnabled[slot];
    }

    boolean allowsLinkCollision(int slot) {
        return linkCollisionEnabled[slot];
    }

    void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        swordCollisionEnabled[slot] = false;
        linkCollisionEnabled[slot] = false;
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
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

    private void decrementTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    private static boolean isWithinRevealWindow(int entityX, int entityY,
                                                int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        return distanceX >= -0x20 && distanceX < 0x20
            && distanceY >= -0x20 && distanceY < 0x20;
    }

    private int[] move(RoomEntity entity, int x, int y,
                       RoomEntityBackgroundInteraction backgroundInteraction,
                       int frameCounter, int slot) {
        int movedX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (movedX != x && backgroundInteraction != null) {
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                entity, signedByte(speedX[slot]) < 0 ? 1 : 0, movedX, y,
                0x03, frameCounter);
            if (result.blocked()) {
                movedX = x;
            }
        }
        int movedY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (movedY != y && backgroundInteraction != null) {
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                entity, signedByte(speedY[slot]) < 0 ? 2 : 3, movedX, movedY,
                0x03, frameCounter);
            if (result.blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
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

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Update(RoomEntity entity, boolean hitGround, boolean clearsIgnoreHitsCountdown,
                  boolean jumpJingle, int physicsFlags) {
    }

    private record Vector(int x, int y) {
    }
}
