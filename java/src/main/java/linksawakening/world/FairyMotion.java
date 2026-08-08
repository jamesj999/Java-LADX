package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Bank-$03's {@code DroppableFairyEntityHandler} movement and hover state.
 *
 * <p>A fairy is not one of the ordinary bouncing drops: its handler updates
 * X/Y with the entity speed bytes, eases Z toward {@code $10}, then either
 * gives it a random eight-pixel velocity while Link is nearby or writes the
 * ROM's length-$09 vector toward Link. The display variant is selected from
 * the pre-movement X speed's sign bit.</p>
 */
final class FairyMotion {
    static final int ENTITY_TYPE = 0x2F;
    private static final int HOVER_Z = 0x10;
    private static final int LINK_DISTANCE_LIMIT = 0x20;
    private static final int LINK_VECTOR_LENGTH = 0x09;
    private static final int RANDOM_HOVER_COUNTDOWN = 0x30;

    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int transitionCountdown) {
    }

    void initialize(int slot) {
        clear(slot);
    }

    Update advance(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier, int transitionCountdown) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(randomByteSupplier, "randomByteSupplier");
        validateSlot(entity.slot());
        validateByte(frameCounter, "Frame counter");
        validateByte(linkEntityX, "Link X");
        validateByte(linkEntityY, "Link Y");
        validateByte(transitionCountdown, "Transition countdown");

        int slot = entity.slot();
        // SetEntitySpriteVariant runs before UpdateEntityPosWithSpeed_03.
        int variant = (speedX[slot] & 0x80) == 0 ? 0 : 1;
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = updateHoverZ(entity.z(), frameCounter);
        int nextTransitionCountdown = transitionCountdown;

        // GetEntityXDistanceToLink_03 and GetEntityYDistanceToLink_03 return
        // signed byte distances. The source's raw `cp $20` checks therefore
        // enter the close branch only for non-negative distances below $20.
        int distanceX = (linkEntityX - x) & 0xFF;
        int distanceY = (linkEntityY - y + z) & 0xFF;
        boolean closeToLink = distanceX < LINK_DISTANCE_LIMIT
            && distanceY < LINK_DISTANCE_LIMIT;
        if (closeToLink) {
            if (nextTransitionCountdown == 0) {
                nextTransitionCountdown = RANDOM_HOVER_COUNTDOWN;
                speedX[slot] = randomHoverSpeed(randomByteSupplier.getAsInt());
                speedY[slot] = randomHoverSpeed(randomByteSupplier.getAsInt());
            }
        } else {
            Vector vector = vectorTowardsLink(x, y, linkEntityX, linkEntityY,
                LINK_VECTOR_LENGTH);
            // ApplyVectorTowardsLink writes Y first, then X.
            speedY[slot] = vector.y() & 0xFF;
            speedX[slot] = vector.x() & 0xFF;
        }

        return new Update(withPositionAndVariant(entity, x, y, z, variant),
            nextTransitionCountdown);
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot] & 0xFF;
    }

    void setSpeedForTest(int slot, int newSpeedX, int newSpeedY, int ignoredSpeedZ) {
        validateSlot(slot);
        validateByte(newSpeedX, "Fairy X speed");
        validateByte(newSpeedY, "Fairy Y speed");
        validateByte(ignoredSpeedZ, "Fairy Z speed");
        speedX[slot] = newSpeedX;
        speedY[slot] = newSpeedY;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    void clear(int slot) {
        validateSlot(slot);
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static int randomHoverSpeed(int randomByte) {
        return ((randomByte & 0x0F) - 0x08) & 0xFF;
    }

    private static int updateHoverZ(int z, int frameCounter) {
        z &= 0xFF;
        if ((frameCounter & 0x03) != 0 || z == HOVER_Z) {
            return z;
        }
        if ((z & 0x80) != 0 || z < HOVER_Z) {
            return (z + 1) & 0xFF;
        }
        return (z - 1) & 0xFF;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int result = divideComponent(Math.min(absoluteX, absoluteY),
            Math.max(absoluteX, absoluteY), length);
        int x = absoluteX >= absoluteY ? length : result;
        int y = absoluteY > absoluteX ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x, y);
    }

    private static int divideComponent(int smaller, int larger, int length) {
        if (larger == 0) {
            return 0;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum >= larger) {
                sum -= larger;
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

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                      int z, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z & 0xFF);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static void validateByte(int value, String name) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private record Vector(int x, int y) {
    }
}
