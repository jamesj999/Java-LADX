package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.EntityStatus;
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

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateCountdown1Table.
    private final int[] attackPauseCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState1Table.
    private final int[] wanderTurnCount = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] collisionsTable = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] horizontallyCollidedObject = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] verticallyCollidedObject = new int[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        wanderTurnCount[slot] = 0;
        attackPauseCountdown[slot] = 0;
        inertia[slot] = 0;
        collisionsTable[slot] = 0;
        horizontallyCollidedObject[slot] = 0;
        verticallyCollidedObject[slot] = 0;
        initialized[slot] = true;
    }

    /** EntityInitWithCountdown ($03:4FAF) initializes Octorok and ordinary Moblin. */
    public void initializeWithProjectileDelay(int slot) {
        initialize(slot);
        attackPauseCountdown[slot] = 0xA0;
    }

    public RoomEntity advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
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
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        RoomEntityBackgroundInteraction interaction = RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision);
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
        backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
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
        backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
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
        backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
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
        if (!initialized[entity.slot()]) initialize(entity.slot());
        tickTimers(entity.slot());
        return advanceAfterTimers(
                entity,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundInteraction,
                ignoreHitsCountdown,
                frameCounter,
                creditsGameplay);
    }

    /** Executes AnimateRoamingEnemy after the shared entity countdowns have run. */
    public Update advanceAfterTimers(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter,
            boolean creditsGameplay) {
        return roamingEnemyFrameStep.run(new RoamingEnemyFrame(
                entity,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundInteraction,
                ignoreHitsCountdown,
                frameCounter,
                creditsGameplay));
    }

    LaunchRequest launchRequestIfEligible(
            RoomEntity entity, int linkEntityX, int linkEntityY, boolean creditsGameplay) {
        int type = entity.type() & 0xFF;
        if (transitionCountdown[entity.slot()] != 0x0A
                || attackPauseCountdown[entity.slot()] != 0
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

    public EnemyRecoilMotion.Update applyIncomingRecoil(
            RoomEntity entity, RoomEntityBackgroundCollision background, EnemyRecoilMotion recoil) {
        int slot = entity.slot();
        if (!initialized[slot]) initialize(slot);
        if (entity.status() != EntityStatus.DYING) beginRecoil(slot);
        return recoil.advanceWithHandlerFractions(entity, background, speedXAccumulator, speedYAccumulator, true);
    }

    public void finishLethalSwordHitWhilePaused(int slot) {
        state[slot] = 0;
    }

    public void tickTimers(int slot) {
        if (attackPauseCountdown[slot] > 0) attackPauseCountdown[slot]--;
        if (transitionCountdown[slot] > 0) transitionCountdown[slot]--;
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

    public record Update(
            RoomEntity entity, /* Optional launch request; absent when the firing gate is closed. */
            LaunchRequest launchRequest) {}

    public record LaunchRequest(int sourceSlot, int sourceType, int projectileType)
            implements linksawakening.world.entity.event.EntityRequestBridge {}

    private final RoamingEnemyFrameStep roamingEnemyFrameStep = new RoamingEnemyFrameStep(this);

    record RoamingEnemyFrame(
            linksawakening.world.entity.RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown,
            int frameCounter,
            boolean creditsGameplay) {}

    static final class RoamingEnemyFrameStep {
        private final RoamingEnemyMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.RoamingEnemyMotion.RoamingEnemyFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int ignoreHitsCountdown;
        private int frameCounter;
        private boolean creditsGameplay;
        private int slot;
        private int x;
        private int y;
        private int variant;

        RoamingEnemyFrameStep(RoamingEnemyMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.RoamingEnemyMotion.Update run() {
            if (!prepareTimersAndMotion()) return completedResult;
            if (!chooseDirectionAndAnimation()) return completedResult;

            return new RoamingEnemyMotion.Update(
                    RoamingEnemyMotion.withPositionAndVariant(entity, x, y, variant), null);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.RoamingEnemyMotion.Update completedResult;

        private boolean prepareTimersAndMotion() {

            entity = inputs.entity();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundInteraction = inputs.backgroundInteraction();
            ignoreHitsCountdown = inputs.ignoreHitsCountdown();
            frameCounter = inputs.frameCounter();
            creditsGameplay = inputs.creditsGameplay();

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }
            if (owner.state[slot] != 0) {
                RoamingEnemyMotion.LaunchRequest launchRequest =
                        owner.launchRequestIfEligible(entity, linkEntityX, linkEntityY, creditsGameplay);
                if (launchRequest != null && entity.type() == RoamingEnemyMotion.ENTITY_OCTOROK) {

                    completedResult = new RoamingEnemyMotion.Update(entity, launchRequest);
                    return false;
                }
                if (owner.transitionCountdown[slot] == 0) {
                    owner.transitionCountdown[slot] = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                    owner.state[slot] = 0;
                    owner.wanderTurnCount[slot] = (owner.wanderTurnCount[slot] + 1) & 0x03;
                    owner.direction[slot] = owner.wanderTurnCount[slot] == 0
                            ? RoamingEnemyMotion.directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
                            : randomByteSupplier.getAsInt() & 0x03;
                    owner.speedX[slot] = RoamingEnemyMotion.speedXFor(entity.type(), owner.direction[slot]);
                    owner.speedY[slot] = RoamingEnemyMotion.speedYFor(entity.type(), owner.direction[slot]);
                } else {
                    probePausedBackground();
                }

                completedResult = new RoamingEnemyMotion.Update(entity, launchRequest);
                return false;
            }

            x = entity.x();
            y = entity.y();
            return true;
        }

        private void probePausedBackground() {
            // $03:5889 still probes with the ordinary speeds restored by
            // recoil, at the current coordinates without advancing XY.
            owner.collisionsTable[slot] = 0;
            owner.horizontallyCollidedObject[slot] = 0;
            if (owner.speedX[slot] != 0) {
                int directionX = (owner.speedX[slot] & 0x80) == 0 ? 0 : 1;
                var result = backgroundInteraction.probe(
                        entity, directionX, entity.x(), entity.y(), ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    owner.collisionsTable[slot] |= result.collisionFlag();
                    owner.horizontallyCollidedObject[slot] = result.objectId();
                }
            }
            if (owner.speedY[slot] != 0) {
                int directionY = (owner.speedY[slot] & 0x80) == 0 ? 3 : 2;
                var result = backgroundInteraction.probe(
                        entity, directionY, entity.x(), entity.y(), ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    owner.collisionsTable[slot] |= result.collisionFlag();
                    owner.verticallyCollidedObject[slot] = result.objectId();
                }
            }
        }

        private boolean chooseDirectionAndAnimation() {

            if ((owner.collisionsTable[slot] & 0x0F) != 0 || owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
                owner.state[slot] = 1;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.collisionsTable[slot] = 0;
            } else {
                int nextX = RoamingEnemyMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
                // $03:7A36 and $03:7A5D test speed, including steps that
                // update only the fractional position accumulator.
                if (owner.speedX[slot] != 0) {
                    EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                            entity, owner.direction[slot], nextX, y, ignoreHitsCountdown, frameCounter);
                    if (result.blocked()) {
                        owner.collisionsTable[slot] |= result.collisionFlag();
                        owner.horizontallyCollidedObject[slot] = result.objectId();
                    } else {
                        x = nextX;
                    }
                } else {
                    x = nextX;
                }

                int nextY = RoamingEnemyMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
                if (owner.speedY[slot] != 0) {
                    EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                            entity, owner.direction[slot], x, nextY, ignoreHitsCountdown, frameCounter);
                    if (result.blocked()) {
                        owner.collisionsTable[slot] |= result.collisionFlag();
                        owner.verticallyCollidedObject[slot] = result.objectId();
                    } else {
                        y = nextY;
                    }
                } else {
                    y = nextY;
                }
            }

            owner.inertia[slot] = (owner.inertia[slot] + 1) & 0xFF;
            variant = RoamingEnemyMotion.VARIANT_BY_DIRECTION[owner.direction[slot]]
                    | ((owner.inertia[slot] >>> 3) & 0x01);
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.RoamingEnemyMotion.Update run(
                linksawakening.world.entity.motion.RoamingEnemyMotion.RoamingEnemyFrame inputs) {
            if (inUse) {
                return new RoamingEnemyFrameStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            linkEntityX = 0;
            linkEntityY = 0;
            randomByteSupplier = null;
            backgroundInteraction = null;
            ignoreHitsCountdown = 0;
            frameCounter = 0;
            creditsGameplay = false;
            slot = 0;
            x = 0;
            y = 0;
            variant = 0;
            completedResult = null;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundInteraction = null;
                this.completedResult = null;
            }
        }
    }

    int[] attackPauseCountdown() {
        return this.attackPauseCountdown;
    }

    int[] state() {
        return this.state;
    }

    int[] transitionCountdown() {
        return this.transitionCountdown;
    }

    int[] inertia() {
        return this.inertia;
    }

    int[] wanderTurnCount() {
        return this.wanderTurnCount;
    }

    int[] direction() {
        return this.direction;
    }

    int[] speedX() {
        return this.speedX;
    }

    int[] speedY() {
        return this.speedY;
    }

    int[] speedXAccumulator() {
        return this.speedXAccumulator;
    }

    int[] speedYAccumulator() {
        return this.speedYAccumulator;
    }

    int[] collisionsTable() {
        return this.collisionsTable;
    }

    int[] horizontallyCollidedObject() {
        return this.horizontallyCollidedObject;
    }

    int[] verticallyCollidedObject() {
        return this.verticallyCollidedObject;
    }

    boolean[] initialized() {
        return this.initialized;
    }
}
