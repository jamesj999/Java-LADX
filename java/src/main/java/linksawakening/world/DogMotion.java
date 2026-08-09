package linksawakening.world;

import java.util.Objects;
import java.util.function.IntSupplier;

/** Bank-$19 DogEntityHandler state machine and fixed-point movement. */
final class DogMotion {
    static final int ENTITY_TYPE = 0x6F;
    static final int INITIAL_PHYSICS_FLAGS = 0x92;
    static final int CHARGE_PHYSICS_FLAGS = 0x52;
    static final int OPTIONS1 = 0x00;
    static final int HANDLER_HEALTH = 0x4C;
    static final int DIALOG_LOW_ID = 0x20;

    // Data_019_4990.
    private static final int[] SPEEDS = {
        0x02, 0x08, 0x0C, 0x08, 0xFE, 0xF8, 0xF4, 0xF8
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int state, int transitionCountdown, int direction,
                  int speedX, int speedY, int speedZ, int physicsFlags, int collisionFlags,
                  boolean appliesBackgroundInteraction, boolean dialogRequested,
                  int dialogLowId) {
    }

    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int transitionCountdown,
                   int physicsFlags, int linkEntityX, int linkEntityY, int linkDirection,
                   boolean actionButtonAHeld, boolean dialogActive,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        checkType(entity);
        validateByte(frameCounter, "Dog frame counter");
        validateByte(transitionCountdown, "Dog transition countdown");
        validateByte(physicsFlags, "Dog physics flags");
        validateByte(linkEntityX, "Dog Link X");
        validateByte(linkEntityY, "Dog Link Y");
        if (linkDirection < 0 || linkDirection > 3) {
            throw new IllegalArgumentException("Dog Link direction must be 0..3: "
                + linkDirection);
        }
        Objects.requireNonNull(randomByteSupplier, "Dog random-byte supplier");

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int nextPhysicsFlags = physicsFlags & 0xFF;
        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 2) & 0xFF;
        if ((z & 0x80) != 0) {
            z = 0;
            speedZ[slot] = 0;
        }

        boolean appliesBackgroundInteraction = false;
        boolean dialogRequested = false;
        int dialogLowId = -1;
        int movementCollisionFlags = 0;
        RoomEntity updated = withZ(entity, z);

        // func_019_7CF0 runs before the state dispatch and only for states 0/1.
        if (state[slot] < 2 && canTalk(updated, linkEntityX, linkEntityY,
            linkDirection, actionButtonAHeld, dialogActive)) {
            dialogRequested = true;
            dialogLowId = DIALOG_LOW_ID;
        }

        switch (state[slot]) {
            case 0 -> {
                if (countdown == 0) {
                    int index = randomByteSupplier.getAsInt() & 0x07;
                    speedX[slot] = SPEEDS[index];
                    direction[slot] = index & 0x04;
                    int yIndex = randomByteSupplier.getAsInt() & 0x07;
                    speedY[slot] = SPEEDS[yIndex];
                    countdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x30;
                    state[slot] = 1;
                }
                updated = withVariant(updated, walkingVariant(slot, frameCounter));
            }
            case 1 -> {
                PositionAndCollision moved = moveWithBackground(updated,
                    backgroundInteraction, slot);
                updated = withPositionAndVariant(updated, moved.x(), moved.y(),
                    walkingVariant(slot, frameCounter));
                movementCollisionFlags = moved.collisionFlags();
                appliesBackgroundInteraction = true;
                if ((moved.collisionFlags() & 0x0F) != 0) {
                    if (countdown != 0) {
                        speedZ[slot] = 0x08;
                        z = (updated.z() + 1) & 0xFF;
                        updated = withZ(updated, z);
                    } else {
                        countdown = 0x30;
                        state[slot] = 0;
                    }
                }
            }
            case 2 -> {
                if (countdown == 0) {
                    state[slot] = 3;
                    Vector vector = vectorTowardsLink(updated.x(), updated.y(), updated.z(),
                        linkEntityX, linkEntityY, 0x24);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                    speedZ[slot] = 0x18;
                    direction[slot] = directionToLinkX(updated.x(), linkEntityX);
                }
                updated = withVariant(updated, chargeVariant(slot, frameCounter));
            }
            case 3 -> {
                PositionAndCollision moved = moveWithBackground(updated,
                    backgroundInteraction, slot);
                updated = withPositionAndVariant(updated, moved.x(), moved.y(),
                    entity.spriteVariant());
                movementCollisionFlags = moved.collisionFlags();
                appliesBackgroundInteraction = true;
                // DogState3 briefly exposes $52 to func_003_6C6B, then restores
                // the harmless shadow flags before testing the collision byte.
                // The bounded Java port does not yet expose that separate
                // Link/entity collision callback, so only the restored value
                // leaves this motion object.
                nextPhysicsFlags = INITIAL_PHYSICS_FLAGS;
                if ((moved.collisionFlags() & 0x0F) != 0) {
                    state[slot] = 0;
                    countdown = 0x20;
                }
            }
            default -> throw new IllegalStateException("Invalid Dog state: " + state[slot]);
        }

        return new Update(updated, state[slot], countdown, direction[slot] & 0xFF,
            speedX[slot] & 0xFF, speedY[slot] & 0xFF, speedZ[slot] & 0xFF,
            nextPhysicsFlags & 0xFF, movementCollisionFlags & 0x0F,
            appliesBackgroundInteraction, dialogRequested, dialogLowId);
    }

    void setStateForTest(int slot, int newState, int newDirection,
                         int newSpeedX, int newSpeedY, int newSpeedZ) {
        if (newState < 0 || newState > 3) {
            throw new IllegalArgumentException("Dog state must be 0..3: " + newState);
        }
        validateByte(newDirection, "Dog direction");
        validateByte(newSpeedX, "Dog X speed");
        validateByte(newSpeedY, "Dog Y speed");
        validateByte(newSpeedZ, "Dog Z speed");
        reset(slot);
        state[slot] = newState;
        direction[slot] = newDirection;
        speedX[slot] = newSpeedX;
        speedY[slot] = newSpeedY;
        speedZ[slot] = newSpeedZ;
        initialized[slot] = true;
    }

    int state(int slot) {
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

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    private int walkingVariant(int slot, int frameCounter) {
        int animation = (frameCounter >>> 3) & 0x01;
        return direction[slot] == 0 ? animation + 2 : animation;
    }

    private int chargeVariant(int slot, int frameCounter) {
        int animation = (frameCounter >>> 2) & 0x01;
        return direction[slot] == 0 ? animation + 2 : animation;
    }

    private PositionAndCollision moveWithBackground(RoomEntity entity,
                                                     RoomEntityBackgroundInteraction interaction,
                                                     int slot) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionFlags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(
                entity, direction, x, entity.y());
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                x = entity.x() & 0xFF;
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y, collisionFlags);
    }

    private boolean canTalk(RoomEntity entity, int linkEntityX, int linkEntityY,
                             int linkDirection, boolean actionButtonAHeld,
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
        return directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
            == (linkDirection ^ 0x01);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static int directionToLinkX(int entityX, int linkX) {
        return signedByte(linkX - entityX) < 0 ? 1 : 0;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
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

    private void reset(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static RoomEntity withZ(RoomEntity entity, int z) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            z & 0xFF, entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Dog entity");
        }
    }

    private static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record PositionAndCollision(int x, int y, int collisionFlags) {
    }

    private record Vector(int x, int y) {
    }
}
