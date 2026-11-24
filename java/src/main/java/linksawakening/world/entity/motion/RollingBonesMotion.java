package linksawakening.world.entity.motion;

import java.util.Objects;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Active state machines from bank $06's Rolling Bones and rolling-bar handlers. */
public final class RollingBonesMotion {
    private final boolean[] deathInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public boolean deathInitialized(int slot) {
        return deathInitialized[slot];
    }

    public void setDeathInitialized(int slot, boolean value) {
        deathInitialized[slot] = value;
    }

    private final int[] swordHitFrame = new int[EntityRoomLoader.MAX_ENTITIES];

    public int swordHitFrame(int slot) {
        return swordHitFrame[slot];
    }

    public void setSwordHitFrame(int slot, int value) {
        swordHitFrame[slot] = value;
    }

    static final int ENTITY_BOSS = 0x81;
    static final int ENTITY_BAR = 0x82;

    public record BossUpdate(RoomEntity entity, int transitionCountdown, int jingleId) {}

    public record BarUpdate(
            RoomEntity entity,
            int animationVariant,
            boolean rollingSound,
            boolean strongBump,
            int screenShakeCountdown) {}

    static final int STATE_APPROACH_BAR = 0;
    static final int STATE_PREPARE_ROLL = 1;
    static final int STATE_WAIT_FOR_BAR = 2;
    static final int STATE_JUMP_AWAY = 3;

    private static final int BAR_STATE_RESTING = 0;
    private static final int BAR_STATE_ROLLING = 1;
    private static final int BAR_STATE_DECELERATING = 2;

    private final int[] bossState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bossZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] barState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] barSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] barAnimationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] barXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] barTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private int rollingSoundCounter;

    public void initializeBoss(int slot) {
        bossState[slot] = STATE_APPROACH_BAR;
        bossDirection[slot] = 0;
        bossSpriteVariant[slot] = 0;
        bossSpeedX[slot] = 0;
        bossSpeedY[slot] = 0;
        bossSpeedZ[slot] = 0;
        bossXAccumulator[slot] = 0;
        bossYAccumulator[slot] = 0;
        bossZAccumulator[slot] = 0;
    }

    public void initializeBar(int slot) {
        barState[slot] = BAR_STATE_RESTING;
        barSpeedX[slot] = 0;
        barAnimationVariant[slot] = 0;
        barXAccumulator[slot] = 0;
        barTransitionCountdown[slot] = 0;
    }

    public BossUpdate advanceBoss(
            RoomEntity entity,
            RoomEntity bar,
            int transitionCountdown,
            int health,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        return rollingBonesFrameStep.run(entity, bar, transitionCountdown, health, backgroundCollision);
    }

    public BarUpdate advanceBar(
            RoomEntity entity, int frameCounter, RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        int slot = entity.slot();
        if (barTransitionCountdown[slot] > 0) {
            barTransitionCountdown[slot]--;
        }
        int x = entity.x();
        boolean rollingSound = false;
        boolean strongBump = false;
        int screenShake = 0;
        boolean wasRolling = barState[slot] == BAR_STATE_ROLLING;
        boolean blockedWhileRolling = false;

        if (barState[slot] != BAR_STATE_RESTING) {
            MoveResult movement =
                    moveX(entity, x, entity.y(), barSpeedX[slot], barXAccumulator, slot, backgroundCollision);
            x = movement.position();
            blockedWhileRolling = barState[slot] == BAR_STATE_ROLLING && movement.blocked();
        }

        // The source updates wEntitiesSpriteVariant after movement/background
        // collision has populated flags, but before the rolling-state rebound
        // or deceleration handler changes speed. A stopped bar skips
        // SetEntitySpriteVariant and therefore retains its last animation bit.
        if (barSpeedX[slot] != 0) {
            int absoluteSpeed = Math.abs(barSpeedX[slot]);
            int cadenceMask = absoluteSpeed >= 8 ? 4 : absoluteSpeed >= 4 ? 8 : absoluteSpeed >= 2 ? 0x10 : 0x20;
            barAnimationVariant[slot] = (frameCounter & cadenceMask) == 0 ? 0 : 1;
        }

        if (blockedWhileRolling) {
            barSpeedX[slot] = -(barSpeedX[slot] >> 1);
            barState[slot] = BAR_STATE_DECELERATING;
            strongBump = true;
            screenShake = 0x20;
        }

        if (wasRolling) {
            rollingSoundCounter++;
            if (rollingSoundCounter >= 9) {
                rollingSoundCounter = 0;
                rollingSound = true;
            }
        } else if (barState[slot] == BAR_STATE_DECELERATING && (frameCounter & 0x07) == 0) {
            if (barSpeedX[slot] == 0) {
                barState[slot] = BAR_STATE_RESTING;
                barTransitionCountdown[slot] = 0x50;
            } else {
                barSpeedX[slot] += barSpeedX[slot] < 0 ? 1 : -1;
            }
        }

        // The bar renderer bakes the selected ROM pair into a one-variant
        // dynamic definition. Keep the RoomEntity's definition index at zero
        // and carry the source animation bit separately until that definition
        // is rebuilt by RoomEntityRuntime.
        return new BarUpdate(
                withPositionAndVariant(entity, x, entity.y(), entity.z(), 0),
                barAnimationVariant[slot],
                rollingSound,
                strongBump,
                screenShake);
    }

    void launchBar(int slot, int speedX) {
        barSpeedX[slot] = speedX;
        barState[slot] = BAR_STATE_ROLLING;
    }

    static boolean barCanDamageLink(int linkZ) {
        return (linkZ & 0xFF) == 0;
    }

    int bossState(int slot) {
        return bossState[slot];
    }

    public int barState(int slot) {
        return barState[slot];
    }

    public int barSpeedX(int slot) {
        return barSpeedX[slot];
    }

    int barTransitionCountdown(int slot) {
        return barTransitionCountdown[slot];
    }

    static MoveResult moveX(
            RoomEntity entity,
            int x,
            int y,
            int speed,
            int[] accumulators,
            int slot,
            RoomEntityBackgroundCollision collision) {
        int next = addSpeed(x, speed, accumulators, slot);
        int direction = speed < 0 ? 1 : 0;
        return collision.blocks(entity, direction, next, y) ? new MoveResult(x, true) : new MoveResult(next, false);
    }

    static MoveResult moveY(
            RoomEntity entity,
            int x,
            int y,
            int speed,
            int[] accumulators,
            int slot,
            RoomEntityBackgroundCollision collision) {
        int next = addSpeed(y, speed, accumulators, slot);
        int direction = speed < 0 ? 2 : 3;
        return collision.blocks(entity, direction, x, next) ? new MoveResult(y, true) : new MoveResult(next, false);
    }

    static int addSpeed(int position, int speed, int[] accumulators, int slot) {
        int fixed = (position << 4) + accumulators[slot] + speed;
        accumulators[slot] = fixed & 0x0F;
        return (fixed >> 4) & 0xFF;
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned >= 0x80 ? unsigned - 0x100 : unsigned;
    }

    record MoveResult(int position, boolean blocked) {}

    private final RollingBonesFrameStep rollingBonesFrameStep = new RollingBonesFrameStep(this);

    static final class RollingBonesFrameStep {
        private final RollingBonesMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity bar;
        private int transitionCountdown;
        private int health;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int slot;
        private int x;
        private int y;
        private int z;
        private int variant;
        private int jingleId;
        private int previousSpeedZ;
        private boolean landed;
        private int displayVariant;

        RollingBonesFrameStep(RollingBonesMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.RollingBonesMotion.BossUpdate run() {
            slot = entity.slot();
            x = entity.x();
            y = entity.y();
            z = entity.z();
            variant = owner.bossSpriteVariant[slot];
            jingleId = -1;

            previousSpeedZ = owner.bossSpeedZ[slot];
            z = RollingBonesMotion.addSpeed(z, previousSpeedZ, owner.bossZAccumulator, slot);
            owner.bossSpeedZ[slot] -= 2;
            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                int impactSpeedZ = owner.bossSpeedZ[slot];
                owner.bossSpeedZ[slot] = 0;
                if (impactSpeedZ < -0x0E) {
                    jingleId = 0x20;
                }
            }

            if (owner.bossSpeedX[slot] != 0) {
                owner.bossDirection[slot] = owner.bossSpeedX[slot] < 0 ? 0 : 3;
            }

            switch (owner.bossState[slot]) {
                case RollingBonesMotion.STATE_APPROACH_BAR -> {
                    if (!advanceApproachBar()) return completedResult;
                }
                case RollingBonesMotion.STATE_PREPARE_ROLL -> {
                    if (!advancePrepareRoll()) return completedResult;
                }
                case RollingBonesMotion.STATE_WAIT_FOR_BAR -> {
                    if (!advanceWaitForBar()) return completedResult;
                }
                case RollingBonesMotion.STATE_JUMP_AWAY -> {
                    if (!advanceJumpAway()) return completedResult;
                }
                default -> owner.initializeBoss(slot);
            }

            // func_006_6E7E indexes the six rectangle variants by adding the
            // direction-table value (0 or 3) to the handler's animation variant.
            owner.bossSpriteVariant[slot] = variant;
            displayVariant = variant + owner.bossDirection[slot];
            return new RollingBonesMotion.BossUpdate(
                    RollingBonesMotion.withPositionAndVariant(entity, x, y, z, displayVariant),
                    transitionCountdown,
                    jingleId);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.RollingBonesMotion.BossUpdate completedResult;

        private boolean advanceApproachBar() {
            if (transitionCountdown == 0) {
                int delta = RollingBonesMotion.signedByte(entity.x() - bar.x());
                owner.bossSpeedX[slot] = delta < 0 ? 0x08 : -0x08;
                if (delta >= -0x10 && delta < 0x10) {
                    transitionCountdown = 0x18;
                    owner.bossState[slot] = RollingBonesMotion.STATE_PREPARE_ROLL;
                } else {
                    // $06:6D68 applies X speed without the background collision pass.
                    x = RollingBonesMotion.addSpeed(x, owner.bossSpeedX[slot], owner.bossXAccumulator, slot);
                    if (landed) {
                        owner.bossSpeedZ[slot] = 0x0C;
                    }
                    variant = 1;
                }
            }

            return true;
        }

        private boolean advancePrepareRoll() {
            if (transitionCountdown == 0) {
                int barSpeed = owner.bossDirection[slot] == 0 ? -0x10 : 0x10;
                owner.launchBar(bar.slot(), barSpeed);
                transitionCountdown = 0x20;
                owner.bossState[slot] = RollingBonesMotion.STATE_WAIT_FOR_BAR;
                variant = 0;
            }

            return true;
        }

        private boolean advanceWaitForBar() {
            if (transitionCountdown == 0) {
                owner.bossState[slot] = RollingBonesMotion.STATE_JUMP_AWAY;
            }

            return true;
        }

        private boolean advanceJumpAway() {
            variant = z < 0x08 ? 1 : 2;
            if (transitionCountdown == 1) {
                int length = health < 5 ? 0x14 : 0x10;
                owner.bossSpeedZ[slot] = health < 5 ? 0x16 : 0x19;
                owner.bossSpeedX[slot] = owner.bossDirection[slot] == 0 ? -length : length;
                owner.bossSpeedY[slot] = ((entity.y() - entity.z()) & 0xFF) < 0x50 ? length : -length;
            } else if (transitionCountdown == 0 && landed) {
                transitionCountdown = 0x10;
            }
            // $06:6DD4 falls through to $6E04 on the launch frame.
            if (transitionCountdown == 1 || (transitionCountdown == 0 && !landed)) {
                RollingBonesMotion.MoveResult xMove = RollingBonesMotion.moveX(
                        entity, x, y, owner.bossSpeedX[slot], owner.bossXAccumulator, slot, backgroundCollision);
                x = xMove.position();
                RollingBonesMotion.MoveResult yMove = RollingBonesMotion.moveY(
                        entity, x, y, owner.bossSpeedY[slot], owner.bossYAccumulator, slot, backgroundCollision);
                y = yMove.position();
                if (xMove.blocked()) {
                    owner.bossState[slot] = RollingBonesMotion.STATE_APPROACH_BAR;
                    transitionCountdown = 0x08;
                    owner.bossSpeedX[slot] = 0;
                    owner.bossSpeedY[slot] = 0;
                }
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.RollingBonesMotion.BossUpdate run(
                linksawakening.world.entity.RoomEntity entity,
                linksawakening.world.entity.RoomEntity bar,
                int transitionCountdown,
                int health,
                linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {
            if (inUse) {
                return new RollingBonesFrameStep(owner)
                        .run(entity, bar, transitionCountdown, health, backgroundCollision);
            }
            inUse = true;
            slot = 0;
            x = 0;
            y = 0;
            z = 0;
            variant = 0;
            jingleId = 0;
            previousSpeedZ = 0;
            landed = false;
            displayVariant = 0;
            completedResult = null;

            this.entity = entity;
            this.bar = bar;
            this.transitionCountdown = transitionCountdown;
            this.health = health;
            this.backgroundCollision = backgroundCollision;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.bar = null;
                this.backgroundCollision = null;
                this.completedResult = null;
            }
        }
    }
}
