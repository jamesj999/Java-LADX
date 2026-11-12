package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$19 DogEntityHandler state machine and fixed-point movement. */
public final class DogMotion {
    public static final int ENTITY_TYPE = 0x6F;
    public static final int INITIAL_PHYSICS_FLAGS = 0x92;
    static final int CHARGE_PHYSICS_FLAGS = 0x52;
    static final int OPTIONS1 = 0x00;
    public static final int HANDLER_HEALTH = 0x4C;
    static final int DIALOG_LOW_ID = 0x20;

    // Data_019_4990.
    static final int[] SPEEDS = {0x02, 0x08, 0x0C, 0x08, 0xFE, 0xF8, 0xF4, 0xF8};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int direction,
            int speedX,
            int speedY,
            int speedZ,
            int physicsFlags,
            int collisionFlags,
            boolean appliesBackgroundInteraction,
            boolean dialogRequested,
            int dialogLowId) {}

    public void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    public Update advance(DogFrame inputs) {
        return dogStep.run(this, inputs);
    }

    public int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot] & 0xFF;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int walkingVariant(int slot, int frameCounter) {
        int animation = (frameCounter >>> 3) & 0x01;
        return direction[slot] == 0 ? animation + 2 : animation;
    }

    int chargeVariant(int slot, int frameCounter) {
        int animation = (frameCounter >>> 2) & 0x01;
        return direction[slot] == 0 ? animation + 2 : animation;
    }

    PositionAndCollision moveWithBackground(RoomEntity entity, RoomEntityBackgroundInteraction interaction, int slot) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionFlags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, entity.y());
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                x = entity.x() & 0xFF;
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y, collisionFlags);
    }

    boolean canTalk(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkDirection,
            boolean actionButtonAHeld,
            boolean dialogActive) {
        if (dialogActive || !actionButtonAHeld) {
            return false;
        }
        int yWindow = (linkEntityY - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x28) {
            return false;
        }
        int xWindow = (linkEntityX - entity.x() + 0x10) & 0xFF;
        if (xWindow >= 0x20) {
            return false;
        }
        return directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY) == (linkDirection ^ 0x01);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    static int directionToLinkX(int entityX, int linkX) {
        return signedByte(linkX - entityX) < 0 ? 1 : 0;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smallerDistance, largerDistance);
        int x = absoluteX < absoluteY ? result : length;
        int y = absoluteX < absoluteY ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (largerDistance == 0) {
            // GetVectorTowardsLink's divide loop increments once per
            // iteration when both distances are zero.
            return length;
        }
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
        return result;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    void reset(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                variant,
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    static RoomEntity withVariant(RoomEntity entity, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                entity.x(),
                entity.y(),
                variant,
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    static RoomEntity withZ(RoomEntity entity, int z) {
        return EntitySnapshots.withMotion(
                entity,
                entity.x(),
                entity.y(),
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                z & 0xFF,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Dog entity");
        }
    }

    static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record PositionAndCollision(int x, int y, int collisionFlags) {}

    record Vector(int x, int y) {}

    final DogStep dogStep = new DogStep();
}
