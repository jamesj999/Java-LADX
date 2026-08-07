package linksawakening.world;

/**
 * Handler-owned movement state for the ordinary player arrow (entity $00).
 * The tables and transition constants are the shared bank-$03 arrow path;
 * unlike enemy arrows, a player arrow rebounds at one quarter of its incoming
 * speed when it hits a wall.
 */
final class PlayerArrowMotion {
    private static final int[] OFFSET_X = {0x00, 0x00, 0x00, 0x00};
    private static final int[] OFFSET_Y = {0x00, 0x00, 0x00, 0x00};
    private static final int[] SPEED_X = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] SPEED_Y = {0x00, 0x00, 0xE0, 0x20};

    /* ArrowSpinningSpriteVariantFrames: right, down, left, up. */
    private static final int[] SPIN_VARIANTS = {0, 3, 1, 2};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] zPosition = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    static SpawnData spawnData(int projectileDirection) {
        int direction = checkedDirection(projectileDirection);
        return new SpawnData(OFFSET_X[direction], OFFSET_Y[direction],
            SPEED_X[direction], SPEED_Y[direction], direction);
    }

    void initializeSpawn(int slot, int projectileDirection) {
        validateSlot(slot);
        SpawnData data = spawnData(projectileDirection);
        direction[slot] = projectileDirection & 0x03;
        speedX[slot] = data.speedX();
        speedY[slot] = data.speedY();
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        transitionCountdown[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        validateSlot(entity.slot());
        int slot = entity.slot();
        if (!initialized[slot]) {
            initializeSpawn(slot, entity.spriteVariant() < 0 ? 0 : entity.spriteVariant());
        }

        // SpawnPlayerProjectile writes Link's current Z. The wall-rock path
        // owns the subsequent vertical motion, so capture that byte only on
        // the normal path.
        if (transitionCountdown[slot] == 0) {
            zPosition[slot] = entity.z();
        }

        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
            if (transitionCountdown[slot] == 1) {
                // ArrowRockAfterHittingWall unloads at exactly one, before
                // applying another position or gravity step.
                return new Update(entity, false, true);
            }
            if (transitionCountdown[slot] != 0) {
                return advanceWallTransition(entity);
            }
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        boolean collidedWithWall = false;
        if (backgroundCollision != null && x != entity.x()
            && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]),
                x, entity.y())) {
            x = entity.x();
            collidedWithWall = true;
        }

        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null && y != entity.y()
            && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), x, y)) {
            y = entity.y();
            collidedWithWall = true;
        }

        if (collidedWithWall) {
            transitionCountdown[slot] = 0x18;
            speedZ[slot] = 0x10;
            speedY[slot] = bounceSpeed(speedY[slot]);
            speedX[slot] = bounceSpeed(speedX[slot]);
        }

        return new Update(withPositionAndVariant(entity, x, y, entity.z(),
            entity.spriteVariant()), collidedWithWall, false);
    }

    int direction(int slot) {
        validateSlot(slot);
        return direction[slot];
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

    int transitionCountdown(int slot) {
        validateSlot(slot);
        return transitionCountdown[slot];
    }

    void clear(int slot) {
        validateSlot(slot);
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        transitionCountdown[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    void setSpeedForTest(int slot, int newSpeedX, int newSpeedY, int newSpeedZ) {
        validateSlot(slot);
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        speedZ[slot] = newSpeedZ & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    void setTransitionForTest(int slot, int countdown, int z, int newSpeedZ) {
        validateSlot(slot);
        transitionCountdown[slot] = countdown & 0xFF;
        zPosition[slot] = z & 0xFF;
        speedZ[slot] = newSpeedZ & 0xFF;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    private Update advanceWallTransition(RoomEntity entity) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = addSpeedToPosition(zPosition[slot], speedZ[slot], speedZAccumulator, slot);
        zPosition[slot] = z;
        speedZ[slot] = (speedZ[slot] - 0x02) & 0xFF;
        int variant = SPIN_VARIANTS[(transitionCountdown[slot] >>> 3) & 0x03];
        return new Update(withPositionAndVariant(entity, x, y, z, variant), false, false);
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

    private static int bounceSpeed(int speed) {
        return (-signedByte(speed) >> 2) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int checkedDirection(int direction) {
        if (direction < 0 || direction > 3) {
            throw new IllegalArgumentException("Projectile direction out of range: " + direction);
        }
        return direction;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int z, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
    }

    record SpawnData(int offsetX, int offsetY, int speedX, int speedY, int initialVariant) {
    }

    record Update(RoomEntity entity, boolean collidedWithWall, boolean unloaded) {
    }
}
