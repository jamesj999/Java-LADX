package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$06 SparkClockwise/CounterClockwiseEntityHandler's orbit loop. */
public final class SparkMotion {
    // Data_006_661D and Data_006_6625. The tables are indexed by
    // (privateState1 + privateState2), as in the ROM.
    private static final int[] SPEED_Y_BY_INDEX = {0x00, 0x10, 0x00, 0xF0, 0x00, 0xF0, 0x00, 0x10};
    private static final int[] SPEED_X_BY_INDEX = {0x10, 0x00, 0xF0, 0x00, 0x10, 0x00, 0xF0, 0x00};
    private static final int[] COLLISION_MASK_BY_INDEX = {0x01, 0x08, 0x02, 0x04, 0x01, 0x04, 0x02, 0x08};

    // ROM wEntitiesPrivateState1Table: four-way wall-following turn index, combined with the winding offset.
    private final int[] wallTurnIndex = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState2Table.
    private final int[] windingOffset = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitSparkCounterClockwise/Clockwise in bank $03. */
    public void initialize(int slot, int entityType) {
        wallTurnIndex[slot] = 0;
        windingOffset[slot] = entityType == 0x17 ? 4 : 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public RoomEntity applyInitializationOffset(RoomEntity entity) {
        int offset = windingOffset[entity.slot()] == 4 ? 3 : -3;
        return withPosition(entity, entity.x(), entity.y() + offset);
    }

    public void tickTimer(int slot) {
        if (transitionCountdown[slot] > 0) transitionCountdown[slot]--;
    }

    public RoomEntity advance(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, entity.type());
            entity = applyInitializationOffset(entity);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);
        int collisionMask = collisionMask(moved, backgroundCollision);
        int tableIndex = (wallTurnIndex[slot] + windingOffset[slot]) & 0x07;

        if ((collisionMask & COLLISION_MASK_BY_INDEX[tableIndex]) != 0) {
            wallTurnIndex[slot] = (wallTurnIndex[slot] + 1) & 0x03;
        } else if (transitionCountdown[slot] != 0) {
            if (transitionCountdown[slot] == 6) {
                wallTurnIndex[slot] = (wallTurnIndex[slot] + 3) & 0x03;
            }
        } else if ((collisionMask & 0x0F) == 0) {
            transitionCountdown[slot] = 9;
        }

        tableIndex = (wallTurnIndex[slot] + windingOffset[slot]) & 0x07;
        speedY[slot] = SPEED_Y_BY_INDEX[tableIndex];
        speedX[slot] = SPEED_X_BY_INDEX[tableIndex];
        return moved;
    }

    public EnemyRecoilMotion.Update applyRecoil(
            RoomEntity entity, RoomEntityBackgroundCollision collision, EnemyRecoilMotion recoil) {
        int slot = entity.slot();
        int vx = recoil.recoilSpeedX(slot);
        int vy = recoil.recoilSpeedY(slot);
        int x = addSpeedToPosition(entity.x(), vx, speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), vy, speedYAccumulator, slot);
        if (vx != 0) collision.blocks(entity, (vx & 0x80) == 0 ? 0 : 1, x, y);
        if (vy != 0) collision.blocks(entity, (vy & 0x80) == 0 ? 3 : 2, x, y);
        return new EnemyRecoilMotion.Update(withPosition(entity, x, y), false);
    }

    public void clear(int slot) {
        wallTurnIndex[slot] = 0;
        windingOffset[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    public int wallTurnIndex(int slot) {
        return wallTurnIndex[slot];
    }

    public int windingOffset(int slot) {
        return windingOffset[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    private static int collisionMask(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int mask = 0;
        if (backgroundCollision.blocks(entity, 0, entity.x(), entity.y())) {
            mask |= 0x01;
        }
        if (backgroundCollision.blocks(entity, 1, entity.x(), entity.y())) {
            mask |= 0x02;
        }
        if (backgroundCollision.blocks(entity, 2, entity.x(), entity.y())) {
            mask |= 0x04;
        }
        if (backgroundCollision.blocks(entity, 3, entity.x(), entity.y())) {
            mask |= 0x08;
        }
        return mask;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.z(),
                -1,
                false);
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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
}
