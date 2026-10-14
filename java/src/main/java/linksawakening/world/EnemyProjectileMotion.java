package linksawakening.world;

/**
 * Shared movement state for the Octorok rock and Moblin arrow handlers.
 *
 * <p>The state in this class mirrors the per-entity WRAM fields consumed by
 * {@code ArrowRenderAndMove} and {@code ArrowRockAfterHittingWall} in bank
 * {@code $03}.  Room entities remain immutable; every frame returns a new
 * snapshot and keeps the handler-owned fixed-point state in the corresponding
 * slot.</p>
 */
final class EnemyProjectileMotion {
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;

    /* ArrowSpinningSpriteVariantFrames: right, down, left, up. */
    private static final int[] ARROW_SPIN_VARIANTS = {0, 3, 1, 2};

    /*
     * Source: 03_moblin.asm, SpawnMoblinArrow.  The four tables are indexed
     * by the low two bits of the source direction.
     */
    private static final int[] MOBLIN_OFFSET_X = {0x08, 0xF8, 0x04, 0xFC};
    private static final int[] MOBLIN_OFFSET_Y = {0xFC, 0xFC, 0xF8, 0x00};
    private static final int[] MOBLIN_SPEED_X = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] MOBLIN_SPEED_Y = {0x00, 0x00, 0xE0, 0x20};

    /*
     * Source: 03_moblin.asm, SpawnOctorokRock.  The X labels contain only
     * two declared bytes.  The ROM's direction-2/3 reads continue into the
     * immediately following Y labels, so these are the effective four-byte
     * reads rather than a guessed replacement table.
     */
    private static final int[] OCTOROK_OFFSET_X = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] OCTOROK_OFFSET_Y = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] OCTOROK_SPEED_X = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] OCTOROK_SPEED_Y = {0x00, 0x00, 0xE0, 0x20};

    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] zPosition = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] pendingLinkCollision = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    static SpawnData spawnData(int projectileType, int projectileDirection) {
        int direction = checkedDirection(projectileDirection);
        return switch (projectileType & 0xFF) {
            case ENTITY_MOBLIN_ARROW -> new SpawnData(
                MOBLIN_OFFSET_X[direction], MOBLIN_OFFSET_Y[direction],
                MOBLIN_SPEED_X[direction], MOBLIN_SPEED_Y[direction], direction);
            case ENTITY_OCTOROK_ROCK -> new SpawnData(
                OCTOROK_OFFSET_X[direction], OCTOROK_OFFSET_Y[direction],
                OCTOROK_SPEED_X[direction], OCTOROK_SPEED_Y[direction], 0);
            default -> throw new IllegalArgumentException(
                "Unsupported enemy projectile type: " + Integer.toHexString(projectileType));
        };
    }

    void initializeSpawn(int slot, int projectileType, int projectileDirection) {
        SpawnData data = spawnData(projectileType, projectileDirection);
        direction[slot] = projectileDirection & 0x03;
        speedX[slot] = data.speedX();
        speedY[slot] = data.speedY();
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        selectedVariant[slot] = data.initialVariant();
        pendingLinkCollision[slot] = false;
        transitionCountdown[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, backgroundCollision, false);
    }

    /**
     * Advances the handler after an earlier Link collision has populated the
     * ROM collision byte.  A Link collision is not a background blockage: the
     * ROM still performs this frame's position update, then starts the wall
     * transition without restoring either coordinate.
     */
    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision,
                   boolean preexistingCollision) {
        return advance(entity, backgroundCollision, preexistingCollision, 0, 0, false);
    }

    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision,
                   boolean preexistingCollision, int frameCounter, int groundStatus,
                   boolean sideScrolling) {
        return advance(entity, backgroundCollision, preexistingCollision, frameCounter,
            groundStatus, sideScrolling, true, true);
    }

    Update advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision,
                   boolean preexistingCollision, int frameCounter, int groundStatus,
                   boolean sideScrolling, boolean handlerInteractive, boolean timersInteractive) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initializeSpawn(slot, entity.type(), 0);
        }

        // The immutable entity carries the active Z position between normal
        // frames.  During a wall transition the handler-owned copy continues
        // advancing independently until the entity is unloaded.
        if (transitionCountdown[slot] == 0) {
            zPosition[slot] = entity.z();
            selectedVariant[slot] = entity.spriteVariant();
        }

        if (timersInteractive && transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        // $6AD7 renders before ReturnIfNonInteractive_03. A paused handler
        // must not move, choose another spin frame, or unload at countdown 1.
        if (!handlerInteractive) {
            pendingLinkCollision[slot] |= preexistingCollision;
            return new Update(withPositionAndVariant(entity, entity.x(), entity.y(),
                entity.z(), selectedVariant[slot]), false, false);
        }
        if (transitionCountdown[slot] == 1) {
            return new Update(entity, false, true);
        }
        if (transitionCountdown[slot] != 0) {
            return advanceWallTransition(entity, frameCounter, groundStatus, sideScrolling);
        }

        preexistingCollision |= pendingLinkCollision[slot];
        pendingLinkCollision[slot] = false;
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        boolean collidedWithWall = preexistingCollision;
        if (!preexistingCollision && backgroundCollision != null && x != entity.x()
            && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), x,
                entity.y())) {
            x = entity.x();
            collidedWithWall = true;
        }

        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (!preexistingCollision && backgroundCollision != null && y != entity.y()
            && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), x, y)) {
            y = entity.y();
            collidedWithWall = true;
        }

        if (collidedWithWall) {
            /*
             * ApplyEntityInteractionWithBackground restores each blocked
             * coordinate to the active position before ArrowRenderAndMove
             * inspects the collision table.  The enemy-projectile handler
             * then starts its shared wall transition.
             */
            transitionCountdown[slot] = 0x18;
            speedZ[slot] = 0x10;
            speedY[slot] = bounceSpeed(speedY[slot]);
            speedX[slot] = bounceSpeed(speedX[slot]);
        }

        return new Update(withPositionAndVariant(entity, x, y,
            entity.spriteVariant()), collidedWithWall, false);
    }

    boolean pendingLinkCollision(int slot) {
        return pendingLinkCollision[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    void clear(int slot) {
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        selectedVariant[slot] = 0;
        pendingLinkCollision[slot] = false;
        transitionCountdown[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    /* Package-private hooks keep physics tests deterministic without exposing
       WRAM-like state as part of the runtime API. */
    void setSpeedForTest(int slot, int newSpeedX, int newSpeedY, int newSpeedZ) {
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        speedZ[slot] = newSpeedZ & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    void setTransitionForTest(int slot, int countdown, int z, int newSpeedZ) {
        transitionCountdown[slot] = countdown & 0xFF;
        zPosition[slot] = z & 0xFF;
        speedZ[slot] = newSpeedZ & 0xFF;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    private Update advanceWallTransition(RoomEntity entity, int frameCounter,
                                         int groundStatus, boolean sideScrolling) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = zPosition[slot];
        if (sideScrolling) {
            EntitySideViewGravity.Speeds speeds = EntitySideViewGravity.advance(
                speedX[slot], speedY[slot], frameCounter, groundStatus);
            speedX[slot] = speeds.x();
            speedY[slot] = speeds.y();
        } else {
            z = addSpeedToPosition(z, speedZ[slot], speedZAccumulator, slot);
            zPosition[slot] = z;
            speedZ[slot] = (speedZ[slot] - 0x02) & 0xFF;
        }

        // The shared handler renders before writing the next spin frame.
        int variant = selectedVariant[slot];
        if ((entity.type() & 0xFF) == ENTITY_MOBLIN_ARROW) {
            selectedVariant[slot] = ARROW_SPIN_VARIANTS[
                (transitionCountdown[slot] >>> 3) & 0x03];
        }
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
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int bounceSpeed(int speed) {
        return (-signedByte(speed) >> 3) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int checkedDirection(int value) {
        if (value < 0 || value > 3) {
            throw new IllegalArgumentException("Projectile direction out of range: " + value);
        }
        return value;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return withPositionAndVariant(entity, x, y, entity.z(), variant);
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
    }

    record SpawnData(int offsetX, int offsetY, int speedX, int speedY, int initialVariant) {
    }

    record Update(RoomEntity entity, boolean collidedWithWall, boolean unloaded) {
    }
}
