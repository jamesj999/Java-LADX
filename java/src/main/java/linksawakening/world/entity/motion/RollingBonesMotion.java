package linksawakening.world.entity.motion;

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

    final int[] bossState = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossSpeedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] bossZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
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
        return new RollingBonesFrameStep(this, entity, bar, transitionCountdown, health, backgroundCollision).run();
    }

    public BarUpdate advanceBar(
            RoomEntity entity, int frameCounter, RoomEntityBackgroundCollision backgroundCollision) {
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
        return collision != null && collision.blocks(entity, direction, next, y)
                ? new MoveResult(x, true)
                : new MoveResult(next, false);
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
        return collision != null && collision.blocks(entity, direction, x, next)
                ? new MoveResult(y, true)
                : new MoveResult(next, false);
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
}
