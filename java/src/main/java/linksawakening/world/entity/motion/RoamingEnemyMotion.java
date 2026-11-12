package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$03 state shared by Octorok, Moblin, and masked Iron Mask roaming. */
public final class RoamingEnemyMotion {
    static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_IRON_MASK = 0x24;
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;
    static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] IRON_MASK_SPEED_X_BY_DIRECTION = {0x0C, 0xF4, 0x00, 0x00};
    private static final int[] IRON_MASK_SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF4, 0x0C};
    static final int[] VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final int[] collisionsTable = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] horizontallyCollidedObject = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] verticallyCollidedObject = new int[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        inertia[slot] = 0;
        collisionsTable[slot] = 0;
        horizontallyCollidedObject[slot] = 0;
        verticallyCollidedObject[slot] = 0;
        initialized[slot] = true;
    }

    public RoomEntity advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision, false)
                .entity();
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean creditsGameplay) {
        RoomEntityBackgroundInteraction interaction =
                backgroundCollision == null ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision);
        return advanceInternal(
                entity, linkEntityX, linkEntityY, randomByteSupplier, interaction, 0, 0, creditsGameplay);
    }

    public Update advanceWithInteraction(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            boolean creditsGameplay) {
        return advanceWithInteraction(
                entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundInteraction, 0, creditsGameplay);
    }

    public Update advanceWithInteraction(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            boolean creditsGameplay) {
        return advanceWithInteraction(
                entity,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundInteraction,
                ignoreHitsCountdown,
                0,
                creditsGameplay);
    }

    public Update advanceWithInteraction(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter,
            boolean creditsGameplay) {
        return advanceInternal(
                entity,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundInteraction,
                ignoreHitsCountdown,
                frameCounter,
                creditsGameplay);
    }

    private Update advanceInternal(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter,
            boolean creditsGameplay) {
        return advanceInternal(new RoamingEnemyFrame(
                entity,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundInteraction,
                ignoreHitsCountdown,
                frameCounter,
                creditsGameplay));
    }

    private linksawakening.world.entity.motion.RoamingEnemyMotion.Update advanceInternal(RoamingEnemyFrame inputs) {
        return roamingEnemyFrameStep.run(this, inputs);
    }

    LaunchRequest launchRequestIfEligible(
            RoomEntity entity, int linkEntityX, int linkEntityY, boolean creditsGameplay) {
        int type = entity.type() & 0xFF;
        if (transitionCountdown[entity.slot()] != 0x0A
                || privateCountdown1[entity.slot()] != 0
                || type == ENTITY_IRON_MASK) {
            return null;
        }

        int linkDirection = directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
        if (linkDirection != direction[entity.slot()]) {
            return null;
        }

        if (type == ENTITY_OCTOROK) {
            if (creditsGameplay) {
                return null;
            }
            return new LaunchRequest(entity.slot(), type, ENTITY_OCTOROK_ROCK);
        }
        if (type == ENTITY_MOBLIN) {
            return new LaunchRequest(entity.slot(), type, ENTITY_MOBLIN_ARROW);
        }
        return null;
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    /** Mirrors AnimateRoamingEnemy forcing state 1 while the enemy recoils. */
    public void beginRecoil(int slot) {
        state[slot] = 1;
        transitionCountdown[slot] = 0x40;
        initialized[slot] = true;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int direction(int slot) {
        return direction[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    int collisionFlags(int slot) {
        return collisionsTable[slot];
    }

    int horizontallyCollidedObject(int slot) {
        return horizontallyCollidedObject[slot];
    }

    int verticallyCollidedObject(int slot) {
        return verticallyCollidedObject[slot];
    }

    /**
     * Mirrors the three SetEntityVariantForDirection_03 calls made by
     * EntityFallHandler before dispatching an Octorok or Moblin handler.
     * The active handler returns immediately for a falling entity, so this
     * presentation-only update must not advance movement or collision state.
     */
    public int advancePresentationVariant(int slot, int repetitions) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        for (int i = 0; i < repetitions; i++) {
            inertia[slot] = (inertia[slot] + 1) & 0xFF;
        }
        return VARIANT_BY_DIRECTION[direction[slot]] | ((inertia[slot] >>> 3) & 0x01);
    }

    static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = speed < 0x80 ? speed : speed - 0x100;
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static int speedXFor(int entityType, int entityDirection) {
        return entityType == ENTITY_IRON_MASK
                ? IRON_MASK_SPEED_X_BY_DIRECTION[entityDirection]
                : SPEED_X_BY_DIRECTION[entityDirection];
    }

    static int speedYFor(int entityType, int entityDirection) {
        return entityType == ENTITY_IRON_MASK
                ? IRON_MASK_SPEED_Y_BY_DIRECTION[entityDirection]
                : SPEED_Y_BY_DIRECTION[entityDirection];
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int variant) {
        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    public record Update(RoomEntity entity, LaunchRequest launchRequest) {}

    public record LaunchRequest(int sourceSlot, int sourceType, int projectileType)
            implements linksawakening.world.entity.event.EntityRequestBridge {}

    final RoamingEnemyFrameStep roamingEnemyFrameStep = new RoamingEnemyFrameStep();
}
