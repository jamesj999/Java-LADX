package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 WingedOctorokEntityHandler state and WRAM fields. */
final class WingedOctorokMotion {
    static final int ENTITY_WINGED_OCTOROK = 0xAE;
    private static final int ENTITY_SWORD = 0x01;
    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] BASE_VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] zPosition = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] presentationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] zInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record RockSpawn(int direction) {
        RockSpawn {
            if (direction < 0 || direction > 3) {
                throw new IllegalArgumentException("Winged Octorok rock direction out of range: "
                    + direction);
            }
        }
    }

    record Update(RoomEntity entity, int state, int transitionCountdown,
                  RockSpawn rockSpawn, boolean jumped, boolean skippedDefaultCollision) {
    }

    void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown, int ignoreHitsCountdown,
                   boolean actionButtonsHeld, int linkItemA, int linkItemB) {
        return advance(entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, transitionCountdown, ignoreHitsCountdown,
            actionButtonsHeld, actionButtonsHeld, linkItemA, linkItemB);
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, RoomEntityBackgroundCollision backgroundCollision,
                   int transitionCountdown, int ignoreHitsCountdown,
                   boolean actionButtonAHeld, boolean actionButtonBHeld,
                   int linkItemA, int linkItemB) {
        if ((entity.type() & 0xFF) != ENTITY_WINGED_OCTOROK) {
            throw new IllegalArgumentException("Unsupported Winged Octorok motion entity type: 0x"
                + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Winged Octorok random-byte supplier cannot be null");
        }

        int slot = entity.slot();
        boolean wasInitialized = initialized[slot];
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (!zInitialized[slot]) {
            zPosition[slot] = entity.z() & 0xFF;
            zInitialized[slot] = true;
        }

        int renderedVariant = wasInitialized ? presentationVariant[slot]
            : entity.spriteVariant();
        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;

        // The handler forces state 1 while the shared recoil/ignore window is live.
        if ((ignoreHitsCountdown & 0xFF) != 0) {
            state[slot] = 1;
            countdown = 0x40;
        }

        Position moved = move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        int z = addSpeedToPosition(zPosition[slot], speedZ[slot],
            speedZAccumulator, slot);
        zPosition[slot] = z;
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            zPosition[slot] = 0;
            speedZ[slot] = 0;
            privateState2[slot] = 0;
        }

        boolean stateTwoHandler = state[slot] == 2;
        RockSpawn rockSpawn = null;
        boolean jumped = false;
        switch (state[slot]) {
            case 0 -> {
                if (moved.collided() || countdown == 0) {
                    countdown = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
                    state[slot] = 1;
                    clearHorizontalSpeed(slot);
                }
                setVariantForDirection(slot);
            }
            case 1 -> {
                if (countdown != 0) {
                    if (countdown == 0x0A
                        && direction[slot] == directionToLink(x, y, linkEntityX, linkEntityY)) {
                        rockSpawn = new RockSpawn(direction[slot]);
                    }
                    clearHorizontalSpeed(slot);
                } else {
                    countdown = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                    state[slot] = 0;
                    privateState1[slot] = (privateState1[slot] + 1) & 0x03;
                    if (privateState1[slot] == 0) {
                        direction[slot] = directionToLink(x, y, linkEntityX, linkEntityY);
                    } else {
                        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
                    }
                    speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
                    speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
                }
            }
            case 2 -> {
                privateState2[slot] = (frameCounter >>> 2) & 0x01;
                direction[slot] = directionToLink(x, y, linkEntityX, linkEntityY);
                setVariantForDirection(slot);
                if (landed) {
                    state[slot] = 1;
                    countdown = 0x20;
                }
            }
            default -> throw new IllegalStateException("Invalid Winged Octorok state: "
                + state[slot]);
        }

        if (!stateTwoHandler && privateCountdown2[slot] == 0
            && withinSignedWindow(linkEntityX - x)
            && withinSignedWindow(linkEntityY - y)
            && swordAttackHeld(actionButtonAHeld, actionButtonBHeld, linkItemA, linkItemB)
            && direction[slot] != (directionToLink(x, y, linkEntityX, linkEntityY) ^ 0x01)) {
            speedZ[slot] = 0x18;
            Vector vector = vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x10);
            speedY[slot] = vector.y();
            speedX[slot] = vector.x();
            state[slot] = 2;
            jumped = true;
        }

        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), renderedVariant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
        return new Update(updated, state[slot], countdown, rockSpawn, jumped,
            stateTwoHandler || jumped);
    }

    void decrementPrivateCountdown2(int slot) {
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    void clear(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown2[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        inertia[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = false;
        zInitialized[slot] = false;
    }

    void forceStateForTest(int slot, int newState, int transitionCountdown,
                           int newPrivateCountdown2, int z, int newSpeedZ) {
        state[slot] = newState & 0xFF;
        direction[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown2[slot] = newPrivateCountdown2 & 0xFF;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = newSpeedZ & 0xFF;
        zPosition[slot] = z & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        inertia[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = true;
        zInitialized[slot] = true;
    }

    private void setVariantForDirection(int slot) {
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        presentationVariant[slot] = BASE_VARIANT_BY_DIRECTION[direction[slot] & 0x03]
            | ((inertia[slot] >>> 4) & 0x01);
    }

    private void clearHorizontalSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    private static boolean swordAttackHeld(boolean actionButtonAHeld, boolean actionButtonBHeld,
                                            int linkItemA, int linkItemB) {
        if ((linkItemB & 0xFF) == ENTITY_SWORD) {
            return actionButtonBHeld;
        }
        return (linkItemA & 0xFF) == ENTITY_SWORD && actionButtonAHeld;
    }

    private Position move(RoomEntity entity, int x, int y,
                          RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
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
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
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

    private static boolean withinSignedWindow(int difference) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -0x20 && signedDifference < 0x20;
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

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record Position(int x, int y, boolean collided) {
    }

    private record Vector(int y, int x) {
    }
}
