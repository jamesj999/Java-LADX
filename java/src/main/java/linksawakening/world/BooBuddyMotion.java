package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 BooBuddyEntityHandler's drift, trigger, and flee states. */
final class BooBuddyMotion {
    static final int ENTITY_TYPE = 0x50;
    static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10;
    static final int OPTIONS1 = 0x20 | 0x08;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $50 uses the ordinary no-op initialization path. */
    void initialize(int slot) {
        state[slot] = 0;
        selectedVariant[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        initialized[slot] = true;
    }

    /**
     * Advances one handler pass. The shared entity timer has already reduced
     * the transition countdown; the private countdown is reduced here to
     * mirror UpdateEntityTimers' private-counter write.
     */
    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   int transitionCountdown, int roomTriggerCount, boolean swordMiddle,
                   IntSupplier randomByteSupplier) {
        tickTimers(entity.slot());
        return advanceAfterTimers(entity, frameCounter, linkEntityX, linkEntityY,
            transitionCountdown, roomTriggerCount, swordMiddle, randomByteSupplier);
    }

    void tickTimers(int slot) {
        ensureInitialized(slot);
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
    }

    RoomEntity advanceRecoil(RoomEntity entity, int recoilX, int recoilY) {
        int slot = entity.slot();
        ensureInitialized(slot);
        // ApplyRecoilIfNeeded_06 temporarily replaces speeds, so recoil
        // shares the ordinary position fractions and leaves drift speeds intact.
        int x = addSpeedToPosition(entity.x(), recoilX, speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), recoilY, speedYAccumulator, slot);
        return withPosition(entity, x, y, selectedVariant[slot]);
    }

    Update advanceAfterTimers(RoomEntity entity, int frameCounter,
                   int linkEntityX, int linkEntityY, int transitionCountdown,
                   int roomTriggerCount, boolean swordMiddle, IntSupplier randomByteSupplier) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Boo Buddy motion entity");
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Boo Buddy random-byte supplier cannot be null");
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        int countdown = transitionCountdown & 0xFF;
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        // $06:79AC renders before the state handlers write the next variant.
        int renderedVariant = selectedVariant[slot];
        int variant = renderedVariant;
        // func_006_5E54 reads the active coordinates cached before movement.
        boolean unloaded = ((entity.y() - entity.z()) & 0xFF) >= 0x88
            || entity.x() >= 0xA8;
        int healthOverride = -1;

        // func_006_5E54 runs before the state dispatch. ClearEntityStatus_06
        // does not persist the Boo Buddy when this boundary is reached, so
        // return the unload request before changing its state.
        if (unloaded) {
            return new Update(withPosition(entity, x, y, variant), countdown,
                unloaded, healthOverride);
        }

        switch (state[slot]) {
            case 0 -> {
                if (roomTriggerCount != 0) {
                    state[slot] = 1;
                    break;
                }
                if (swordMiddle && countdown == 0 && privateCountdown1[slot] == 0) {
                    if (!withinSwordTriggerWindow(x, y, linkEntityX, linkEntityY)) {
                        // Both distant-swing branches jump straight to animation.
                        variant = directionVariant(x, linkEntityX, frameCounter);
                        break;
                    }
                    countdown = 0x20;
                }
                if (countdown != 0) {
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    variant = animateTransition(frameCounter, x, linkEntityX, slot, countdown);
                    if (privateState1[slot] != 0) {
                        // func_006_7AB0 sets this flag at countdown one, then
                        // func_006_7A8C reflects the position around ($50,$48).
                        privateCountdown1[slot] = 0x20;
                        privateState1[slot] = 0;
                        x = (0xA0 - x) & 0xFF;
                        y = (0x90 - y) & 0xFF;
                        variant = animateTransition(frameCounter, x, linkEntityX, slot,
                            countdown);
                    }
                    break;
                }
                if (privateCountdown1[slot] != 0) {
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    variant = animateTransition(frameCounter, x, linkEntityX, slot, countdown);
                    break;
                }

                int length = 0x06 + ((randomByteSupplier.getAsInt() ^ slot) & 0x07);
                Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, length);
                speedY[slot] = approachByFour(speedY[slot], vector.y());
                speedX[slot] = approachByFour(speedX[slot], vector.x());
                variant = directionVariant(x, linkEntityX, frameCounter);
            }
            case 1 -> {
                if (roomTriggerCount == 0) {
                    state[slot] = 0;
                    break;
                }
                healthOverride = 0x01;
                Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x04);
                speedY[slot] = negate(vector.y());
                speedX[slot] = negate(vector.x());
                variant = 0x04 + ((frameCounter >>> 4) & 0x01);
            }
            default -> throw new IllegalStateException("Invalid Boo Buddy state: "
                + state[slot]);
        }

        selectedVariant[slot] = variant;
        return new Update(withPosition(entity, x, y, renderedVariant), countdown,
            false, healthOverride);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int selectedVariant(int slot) {
        ensureInitialized(slot);
        return selectedVariant[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    boolean allowsEnemyCollision(int slot, int roomTriggerCount,
                                 int transitionCountdown, boolean swordMiddle) {
        if (roomTriggerCount != 0) {
            // State 0 only increments into state 1 on this pass; neither
            // handler path reaches a collision helper on that frame.
            return state[slot] == 1;
        }
        // State 0 calls label_3B44 only on its ordinary drift path. The
        // transition and private-countdown branches, plus the sword-middle
        // branch, clear movement and return without a Link collision check.
        return state[slot] == 0 && (transitionCountdown & 0xFF) == 0
            && privateCountdown1[slot] == 0 && !swordMiddle;
    }

    boolean allowsSwordCollision(int slot, int roomTriggerCount) {
        // Only BooBuddyState1Handler calls DefaultEnemyDamageCollisionHandler.
        return roomTriggerCount != 0 && state[slot] == 1;
    }

    void setPrivateStateForTest(int slot, int value) {
        ensureInitialized(slot);
        privateState1[slot] = value & 0xFF;
    }

    void setSpeedForTest(int slot, int x, int y, int z) {
        if ((z & 0xFF) != 0) {
            throw new IllegalArgumentException("Boo Buddy has no test Z speed");
        }
        ensureInitialized(slot);
        speedX[slot] = x & 0xFF;
        speedY[slot] = y & 0xFF;
    }

    private void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    private int animateTransition(int frameCounter, int x, int linkEntityX, int slot,
                                  int countdown) {
        int variant;
        if (((frameCounter >>> 2) & 0x01) == 0) {
            variant = -1;
        } else {
            int direction = signedByte((linkEntityX - x) & 0xFF) < 0 ? 1 : 0;
            variant = direction == 0 ? 0x06 : 0x07;
        }
        privateState1[slot] = countdown == 1 ? 1 : 0;
        return variant;
    }

    private static boolean withinSwordTriggerWindow(int x, int y, int linkX, int linkY) {
        int distanceX = signedByte((linkX - x) & 0xFF);
        int distanceY = signedByte((linkY - y) & 0xFF);
        return distanceX >= -0x24 && distanceX < 0x24
            && distanceY >= -0x24 && distanceY < 0x24;
    }

    private static int directionVariant(int entityX, int linkX, int frameCounter) {
        int direction = signedByte((linkX - entityX) & 0xFF) < 0 ? 2 : 0;
        return direction + ((frameCounter >>> 4) & 0x01);
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
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

    private static int approachByFour(int current, int target) {
        int difference = (target - current) & 0xFF;
        if (difference == 0) {
            return current & 0xFF;
        }
        return (current + ((difference & 0x80) == 0 ? 4 : -4)) & 0xFF;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        boolean xNegative = distanceX < 0;
        boolean yNegative = distanceY < 0;
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int result = divideComponent(Math.min(absoluteX, absoluteY),
            Math.max(absoluteX, absoluteY), length);
        int x = absoluteX >= absoluteY ? length : result;
        int y = absoluteY > absoluteX ? length : result;
        if (xNegative) {
            x = -x;
        }
        if (yNegative) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int divideComponent(int smallerDistance, int largerDistance, int length) {
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

    private static int negate(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Vector(int x, int y) {
    }

    record Update(RoomEntity entity, int transitionCountdown, boolean unloaded,
                  int healthOverride) {
    }
}
