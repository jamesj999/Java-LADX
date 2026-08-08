package linksawakening.world;

/**
 * Bank-$03's hidden secret-seashell state and reveal physics.
 *
 * <p>The entity handler keeps the shell's hidden/revealed state separate from
 * its normal entity status.  In particular, a shell can remain an ACTIVE
 * entity while its sprite is suppressed, and the two tree shells use the
 * Pegasus-Boots collision branch instead of the object-under-entity branch.</p>
 */
final class SecretSeashellMotion {
    static final int ENTITY_TYPE = 0x3D;

    private static final int NORMAL_HIDDEN_STATE = 0x02;
    private static final int TREE_HIDDEN_STATE = 0x01;
    private static final int REVEAL_COUNTDOWN = 0x18;
    private static final int REVEAL_SLOW_COUNTDOWN = 0x80;
    private static final int REVEAL_VECTOR_LENGTH = 0x0C;
    private static final int REVEAL_SPEED_Z = 0x20;
    private static final int OBJECT_SHORT_GRASS = 0x04;
    private static final int OBJECT_SHOVEL_HOLE = 0xCC;

    private static final int[] SPECIAL_HIDDEN_ROOMS = {
        0xDA, 0xA5, 0x74, 0x3A, 0xA8, 0xB2
    };

    private final int[] roomId = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, int roomId) {
        validateSlot(slot);
        this.roomId[slot] = roomId & 0xFF;
        privateState3[slot] = isTreeSeashellRoom(roomId)
            ? TREE_HIDDEN_STATE : NORMAL_HIDDEN_STATE;
        privateState4[slot] = 0;
        privateCountdown1[slot] = 0;
        slowTransitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    void ensureInitialized(int slot, int roomId) {
        validateSlot(slot);
        int normalizedRoomId = roomId < 0 ? 0xFF : roomId & 0xFF;
        if (!initialized[slot] || this.roomId[slot] != normalizedRoomId) {
            initialize(slot, normalizedRoomId);
        }
    }

    /**
     * Advances the exact hidden/reveal branch and the common top-down
     * bouncing physics used after a shell has become visible.
     */
    Update advance(RoomEntity entity, int roomId, int frameCounter,
                   boolean roomTransitionActive, boolean screenShakeActive,
                   boolean pegasusCollisionActive, int linkEntityX, int linkEntityY,
                   RoomEntityObjectSample objectUnderEntity) {
        return advanceInternal(entity, roomId, frameCounter, roomTransitionActive,
            screenShakeActive, pegasusCollisionActive, false,
            entity.x() + 0x08,
            entity.y() + 0x08, linkEntityX, linkEntityY, objectUnderEntity);
    }

    /**
     * Advances the handler with the actual WRAM Pegasus collision position.
     * The ROM compares the shell centre against each byte using unsigned
     * wraparound, accepting a half-open $20-byte window on both axes.
     */
    Update advance(RoomEntity entity, int roomId, int frameCounter,
                   boolean roomTransitionActive, boolean screenShakeActive,
                   boolean pegasusCollisionActive, int pegasusCollisionX,
                   int pegasusCollisionY, int linkEntityX, int linkEntityY,
                   RoomEntityObjectSample objectUnderEntity) {
        return advanceInternal(entity, roomId, frameCounter, roomTransitionActive,
            screenShakeActive, pegasusCollisionActive, true, pegasusCollisionX,
            pegasusCollisionY, linkEntityX, linkEntityY, objectUnderEntity);
    }

    Update advance(RoomEntity entity, int roomId, int frameCounter,
                   boolean roomTransitionActive, boolean screenShakeActive,
                   boolean pegasusCollisionActive,
                   boolean pegasusCollisionCoordinatesKnown,
                   int pegasusCollisionX, int pegasusCollisionY,
                   int linkEntityX, int linkEntityY,
                   RoomEntityObjectSample objectUnderEntity) {
        return advanceInternal(entity, roomId, frameCounter, roomTransitionActive,
            screenShakeActive, pegasusCollisionActive,
            pegasusCollisionCoordinatesKnown, pegasusCollisionX,
            pegasusCollisionY, linkEntityX, linkEntityY, objectUnderEntity);
    }

    private Update advanceInternal(RoomEntity entity, int roomId, int frameCounter,
                            boolean roomTransitionActive, boolean screenShakeActive,
                            boolean pegasusCollisionActive,
                            boolean pegasusCollisionCoordinatesKnown,
                            int pegasusCollisionX, int pegasusCollisionY,
                            int linkEntityX, int linkEntityY,
                            RoomEntityObjectSample objectUnderEntity) {
        if (entity == null) {
            throw new IllegalArgumentException("Secret seashell entity cannot be null");
        }
        int slot = entity.slot();
        ensureInitialized(slot, roomId);
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if ((frameCounter & 0x03) == 0 && slowTransitionCountdown[slot] > 0) {
            slowTransitionCountdown[slot]--;
        }

        if (privateState3[slot] != 0) {
            if (roomTransitionActive) {
                return new Update(withVariant(entity, -1), true);
            }

            boolean reveal = privateState3[slot] == TREE_HIDDEN_STATE
                ? screenShakeActive && pegasusCollisionActive
                    && (!pegasusCollisionCoordinatesKnown
                        || withinPegasusCollisionWindow(entity.x() + 0x08,
                            entity.y() + 0x08, pegasusCollisionX, pegasusCollisionY))
                : shouldRevealFromObject(roomId, objectUnderEntity, slot);
            if (reveal) {
                revealAwayFromLink(slot, entity.x(), entity.y(), linkEntityX, linkEntityY);
                return new Update(withVariant(entity,
                    entity.spriteDefinition().supported() ? 0 : -1), false);
            }
            return new Update(withVariant(entity, -1), true);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 0x02) & 0xFF;
        return new Update(withPositionAndVariant(entity, x, y, z, entity.spriteVariant()), false);
    }

    private static boolean withinPegasusCollisionWindow(int entityCenterX,
                                                         int entityCenterY,
                                                         int collisionX,
                                                         int collisionY) {
        return unsignedWindowContains(entityCenterX, collisionX)
            && unsignedWindowContains(entityCenterY, collisionY);
    }

    private static boolean unsignedWindowContains(int entityCoordinate, int collisionCoordinate) {
        int difference = ((entityCoordinate & 0xFF) - (collisionCoordinate & 0xFF) + 0x10)
            & 0xFF;
        return difference < 0x20;
    }

    private boolean shouldRevealFromObject(int roomId, RoomEntityObjectSample object, int slot) {
        if (object == null) {
            return false;
        }
        int objectId = object.objectId() & 0xFF;
        if (isSpecialHiddenRoom(roomId)) {
            // The ROM deliberately records that these rooms contain a shell
            // which is not hidden under short grass, but still only reveals
            // it through the shovel-hole path.
            privateState4[slot] = 1;
            return objectId == OBJECT_SHOVEL_HOLE;
        }
        return objectId == OBJECT_SHORT_GRASS || objectId == OBJECT_SHOVEL_HOLE;
    }

    private void revealAwayFromLink(int slot, int entityX, int entityY,
                                    int linkEntityX, int linkEntityY) {
        privateState3[slot] = 0;
        privateState4[slot] = 0;
        privateCountdown1[slot] = REVEAL_COUNTDOWN;
        slowTransitionCountdown[slot] = REVEAL_SLOW_COUNTDOWN;
        Vector towardLink = vectorTowardsLink(entityX, entityY,
            linkEntityX, linkEntityY, REVEAL_VECTOR_LENGTH);
        speedX[slot] = (-towardLink.x()) & 0xFF;
        speedY[slot] = (-towardLink.y()) & 0xFF;
        speedZ[slot] = REVEAL_SPEED_Z;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    int privateState3(int slot) {
        validateSlot(slot);
        return privateState3[slot];
    }

    int privateState4(int slot) {
        validateSlot(slot);
        return privateState4[slot];
    }

    int privateCountdown1(int slot) {
        validateSlot(slot);
        return privateCountdown1[slot];
    }

    int slowTransitionCountdown(int slot) {
        validateSlot(slot);
        return slowTransitionCountdown[slot];
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    int speedZ(int slot) {
        validateSlot(slot);
        return speedZ[slot];
    }

    void clear(int slot) {
        validateSlot(slot);
        roomId[slot] = 0;
        privateState3[slot] = 0;
        privateState4[slot] = 0;
        privateCountdown1[slot] = 0;
        slowTransitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    private static boolean isTreeSeashellRoom(int roomId) {
        return (roomId & 0xFF) == 0xA4 || (roomId & 0xFF) == 0xD2;
    }

    private static boolean isSpecialHiddenRoom(int roomId) {
        int normalized = roomId & 0xFF;
        for (int specialRoom : SPECIAL_HIDDEN_ROOMS) {
            if (specialRoom == normalized) {
                return true;
            }
        }
        return false;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smallerDistance, largerDistance);
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

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (largerDistance == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
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

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z & 0xFF,
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static int signedByte(int value) {
        int normalized = value & 0xFF;
        return normalized < 0x80 ? normalized : normalized - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private record Vector(int x, int y) {
    }

    record Update(RoomEntity entity, boolean hidden) {
    }
}
