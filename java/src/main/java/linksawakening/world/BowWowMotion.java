package linksawakening.world;

import java.util.function.IntSupplier;
import java.util.function.IntPredicate;
import java.util.List;

/**
 * The bounded ordinary-following part of bank-$05's Bow-Wow handler.
 *
 * <p>The original keeps Bow-Wow's setup state separate from the active
 * handler state. This class keeps those WRAM-like fields per entity slot so
 * the host renderer can consume the same position and sprite-variant result
 * without running a general Game Boy CPU.</p>
 */
final class BowWowMotion {
    private static final int[] SPEED_X_BY_RANDOM = {0x04, 0x08, 0x0C, 0x08,
        0xFC, 0xF8, 0xF4, 0xF8};
    private static final int[] SPEED_Y_BY_RANDOM = {0xF4, 0xF8, 0x04, 0x08,
        0x0C, 0x08, 0xFC, 0xF8};

    private final int[] activeState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] targetSlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] updatedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                       int linkEntityZ, IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        return advanceWithTargetScan(entity, frameCounter, linkEntityX, linkEntityY,
            linkEntityZ, randomByteSupplier, backgroundCollision, List.of(entity),
            type -> false).entity();
    }

    Update advanceWithTargetScan(RoomEntity entity, int frameCounter, int linkEntityX,
                                 int linkEntityY, int linkEntityZ,
                                 IntSupplier randomByteSupplier,
                                 RoomEntityBackgroundCollision backgroundCollision,
                                 List<RoomEntity> entities,
                                 IntPredicate canEatEntity) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        if (privateState4[slot] == 0) {
            return new Update(setup(entity), targetSlot[slot]);
        }

        decrementTimers(slot);
        targetX[slot] = byteValue(linkEntityX);
        // Bow-Wow follows hLinkPositionZModified, which the original builds
        // as hLinkPositionY - hLinkPositionZ before entering this handler.
        targetY[slot] = byteValue(linkEntityY - linkEntityZ);

        boolean wasAboveGround = updateZ(entity.z(), slot);
        int x = entity.x();
        int y = entity.y();
        int z = updatedZ[slot];
        int variant = entity.spriteVariant();

        switch (activeState[slot]) {
            case 0 -> {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x28;
                    int selectedTarget = findTarget(entity, linkEntityX, linkEntityY,
                        randomByteSupplier, entities, canEatEntity);
                    if (selectedTarget >= 0) {
                        RoomEntity target = entities.get(selectedTarget);
                        targetSlot[slot] = selectedTarget;
                        Vector vector = vectorTowardsTarget(entity, target, 0x30);
                        speedX[slot] = vector.x();
                        speedY[slot] = vector.y();
                        speedZ[slot] = 0x10;
                        activeState[slot] = 4;
                    } else {
                        transitionCountdown[slot] = 0x10;
                    }
                } else if (privateCountdown1[slot] == 0) {
                    privateCountdown1[slot] = 0x20
                        + (randomByteSupplier.getAsInt() & 0x3F);
                    activeState[slot] = 1;
                    int direction = randomByteSupplier.getAsInt() & 0x07;
                    speedX[slot] = SPEED_X_BY_RANDOM[direction];
                    speedY[slot] = SPEED_Y_BY_RANDOM[direction];
                }
            }
            case 1 -> {
                if (privateCountdown1[slot] == 0) {
                    privateCountdown1[slot] = 0x20;
                    activeState[slot] = 2;
                }
                if (wasAboveGround) {
                    speedZ[slot] = 0x10;
                }
                int[] moved = move(entity, x, y, slot, backgroundCollision);
                x = correctTargetX(moved[0], x, slot);
                y = correctTargetY(moved[1], y, slot);
                variant = movementVariant(frameCounter, slot);
            }
            case 2, 4 -> {
                if (transitionCountdown[slot] != 0) {
                    int[] moved = move(entity, x, y, slot, backgroundCollision);
                    x = correctTargetX(moved[0], x, slot);
                    y = correctTargetY(moved[1], y, slot);
                    variant = movementVariant(frameCounter, slot);
                } else {
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                    activeState[slot] = 3;
                    transitionCountdown[slot] = 0x10;
                }
            }
            case 3 -> {
                if (transitionCountdown[slot] == 0) {
                    // func_005_420E starts a new ten-frame follower phase.
                    randomByteSupplier.getAsInt();
                    transitionCountdown[slot] = 0x10;
                    activeState[slot] = 4;
                }
            }
            default -> activeState[slot] = 0;
        }

        return new Update(withPositionAndVariant(entity, x, y, z, variant), targetSlot[slot]);
    }

    void clear(int slot) {
        activeState[slot] = 0;
        privateState4[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        targetX[slot] = 0;
        targetY[slot] = 0;
        targetSlot[slot] = -1;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        updatedZ[slot] = 0;
        initialized[slot] = false;
    }

    int activeState(int slot) {
        return activeState[slot];
    }

    int privateState4(int slot) {
        return privateState4[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int targetX(int slot) {
        return targetX[slot];
    }

    int targetY(int slot) {
        return targetY[slot];
    }

    int targetSlot(int slot) {
        return targetSlot[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    private RoomEntity setup(RoomEntity entity) {
        int slot = entity.slot();
        int x = byteValue(entity.x() + 0x04);
        int y = byteValue(entity.y() + 0x08);
        targetX[slot] = x;
        targetY[slot] = y;
        privateState4[slot] = 1;
        return withPositionAndVariant(entity, x, y, entity.z(), entity.spriteVariant());
    }

    private void decrementTimers(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
    }

    /** Returns whether the source's gravity step crossed below signed zero. */
    private boolean updateZ(int positionZ, int slot) {
        int nextZ = addSpeedToPosition(positionZ, speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = byteValue(speedZ[slot] - 0x02);
        if ((nextZ & 0x80) == 0) {
            updatedZ[slot] = nextZ;
            return false;
        }
        updatedZ[slot] = 0;
        speedZ[slot] = 0;
        return true;
    }

    private int[] move(RoomEntity entity, int x, int y, int slot,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (backgroundCollision != null && nextX != x
            && backgroundCollision.blocks(entity, signedByte(speedX[slot]) < 0 ? 1 : 0,
                nextX, y)) {
            nextX = x;
            speedX[slot] = 0;
        }
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null && nextY != y
            && backgroundCollision.blocks(entity, signedByte(speedY[slot]) < 0 ? 2 : 3,
                nextX, nextY)) {
            nextY = y;
            speedY[slot] = 0;
        }
        return new int[] {nextX, nextY};
    }

    private int correctTargetX(int nextX, int beforeX, int slot) {
        return inSignedWindow(signedByte(targetX[slot] - nextX), 0x20) ? nextX : beforeX;
    }

    private int correctTargetY(int nextY, int beforeY, int slot) {
        return inSignedWindow(signedByte(targetY[slot] - nextY), 0x20) ? nextY : beforeY;
    }

    private int movementVariant(int frameCounter, int slot) {
        int horizontal = Math.abs(signedByte(speedX[slot]));
        int vertical = Math.abs(signedByte(speedY[slot]));
        int base;
        if (vertical >= horizontal) {
            base = signedByte(speedY[slot]) < 0 ? 6 : 0;
        } else {
            base = signedByte(speedX[slot]) < 0 ? 2 : 4;
        }
        return base == 6 ? 6 : base + ((frameCounter >>> 3) & 0x01);
    }

    private static int findTarget(RoomEntity source, int linkEntityX, int linkEntityY,
                                  IntSupplier randomByteSupplier, List<RoomEntity> entities,
                                  IntPredicate canEatEntity) {
        boolean descending = (randomByteSupplier.getAsInt() & 0x01) == 0;
        int step = descending ? -1 : 1;
        int candidate = descending ? EntityRoomLoader.MAX_ENTITIES - 1 : 0;
        int end = source.slot();
        while (candidate != end) {
            if (candidate >= 0 && candidate < entities.size()) {
                RoomEntity target = entities.get(candidate);
                if (target.loaded()
                    && target.status() != EntityStatus.DYING
                    && target.spriteVariant() != 1
                    && canEatEntity.test(target.type())
                    && inUnsignedWindow(linkEntityX - target.x(), 0x2F, 0x5E)
                    && inUnsignedWindow(linkEntityY - target.y(), 0x2F, 0x5E)) {
                    return candidate;
                }
            }
            candidate += step;
        }
        return -1;
    }

    private static Vector vectorTowardsTarget(RoomEntity source, RoomEntity target, int length) {
        int distanceX = signedByte(target.x() - source.x());
        int distanceY = signedByte(target.y() - source.y() + source.z());
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = divideComponent(length, smaller, larger);
        int vectorX = swapped ? result : length;
        int vectorY = swapped ? length : result;
        if (distanceX < 0) {
            vectorX = -vectorX;
        }
        if (distanceY < 0) {
            vectorY = -vectorY;
        }
        return new Vector(vectorX & 0xFF, vectorY & 0xFF);
    }

    private static int divideComponent(int length, int smaller, int larger) {
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum > 0xFF || sum >= larger) {
                sum -= larger;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    private static boolean inUnsignedWindow(int difference, int offset, int limit) {
        return ((difference + offset) & 0xFF) < limit;
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
        return byteValue(position + delta);
    }

    private static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private static int byteValue(int value) {
        return value & 0xFF;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int z, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
    }

    record Update(RoomEntity entity, int targetSlot) {
        boolean targetAcquired() {
            return targetSlot >= 0;
        }
    }

    private record Vector(int x, int y) {
    }
}
