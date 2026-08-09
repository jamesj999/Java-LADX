package linksawakening.world;

/** Bank-$15 UrchinEntityHandler direction, shield push, and fixed-point movement. */
final class UrchinMotion {
    static final int ENTITY_TYPE = 0xC5;
    static final int INITIAL_PHYSICS_FLAGS = 0x12;
    static final int OPTIONS1 = 0x02;
    static final int PUSH_JINGLE_ID = 0x3E;

    private static final int ENTITY_PHYSICS_HARMLESS = 0x80;

    // Data_015_73A3 and Data_015_73A7.
    private static final int[] PUSH_SPEED_X = {0xFD, 0x03, 0x00, 0x00};
    private static final int[] PUSH_SPEED_Y = {0x00, 0x00, 0x03, 0xFD};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int direction, int speedX, int speedY,
                  int physicsFlags, int collisionFlags, boolean linkCollision,
                  boolean pushed, boolean appliesBackgroundInteraction, int jingleId) {
    }

    void initialize(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int frameCounter, int physicsFlags,
                   int linkEntityX, int linkEntityY, int linkDirection,
                   boolean usingShield, boolean swordCollisionActive,
                   boolean linkInteractive,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        checkType(entity);
        validateByte(frameCounter, "Urchin frame counter");
        validateByte(physicsFlags, "Urchin physics flags");
        validateByte(linkEntityX, "Urchin Link X");
        validateByte(linkEntityY, "Urchin Link Y");
        if (linkDirection < 0 || linkDirection > 3) {
            throw new IllegalArgumentException("Urchin Link direction must be 0..3: "
                + linkDirection);
        }

        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (!linkInteractive) {
            return new Update(entity, direction[slot], speedX[slot], speedY[slot],
                physicsFlags, 0, false, false, false, -1);
        }

        direction[slot] = directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
        int nextPhysicsFlags = physicsFlags & ~ENTITY_PHYSICS_HARMLESS;
        if (usingShield && ((linkDirection ^ 0x01) & 0x03) == direction[slot]) {
            nextPhysicsFlags |= ENTITY_PHYSICS_HARMLESS;
        }

        RoomEntity updated = withVariant(entity, (frameCounter >>> 4) & 0x03);
        boolean linkCollision = RoomEntityCombatRules.overlapsLink(
            entity, linkEntityX, linkEntityY)
            && (((nextPhysicsFlags & ENTITY_PHYSICS_HARMLESS) != 0)
                || swordCollisionActive);
        boolean pushed = false;
        boolean appliesBackgroundInteraction = false;
        int collisionFlags = 0;
        int jingleId = -1;
        if (linkCollision && (nextPhysicsFlags & ENTITY_PHYSICS_HARMLESS) != 0) {
            speedX[slot] = PUSH_SPEED_X[direction[slot]];
            speedY[slot] = PUSH_SPEED_Y[direction[slot]];
            PositionAndCollision moved = moveWithBackground(
                entity, backgroundInteraction, slot, frameCounter);
            updated = withPositionAndVariant(updated, moved.x(), moved.y(), updated.spriteVariant());
            collisionFlags = moved.collisionFlags();
            pushed = true;
            appliesBackgroundInteraction = true;
            jingleId = PUSH_JINGLE_ID;
        }

        return new Update(updated, direction[slot], speedX[slot], speedY[slot],
            nextPhysicsFlags, collisionFlags, linkCollision, pushed,
            appliesBackgroundInteraction, jingleId);
    }

    int direction(int slot) {
        validateSlot(slot);
        return direction[slot];
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot] & 0xFF;
    }

    void clear(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = false;
    }

    private PositionAndCollision moveWithBackground(RoomEntity entity,
                                                      RoomEntityBackgroundInteraction interaction,
                                                      int slot, int frameCounter) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionFlags = 0;
        if (interaction != null && x != entity.x()) {
            int movementDirection = signedByte(speedX[slot]) < 0
                ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(
                entity, movementDirection, x, entity.y(), 0x03, frameCounter);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                x = entity.x() & 0xFF;
            }
        }
        if (interaction != null && y != entity.y()) {
            int movementDirection = signedByte(speedY[slot]) < 0
                ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(
                entity, movementDirection, x, y, 0x03, frameCounter);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y, collisionFlags & 0x0F);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
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
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
    }

    private void reset(int slot) {
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Urchin entity");
        }
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Urchin slot out of range: " + slot);
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
}
