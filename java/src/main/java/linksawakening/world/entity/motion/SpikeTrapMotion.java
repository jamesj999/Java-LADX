package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.event.EntityCombatEvent;

/** Bank-$06 SpikeTrapEntityHandler's launch, travel, and return loop. */
public final class SpikeTrapMotion {
    private static final int NO_SOUND = -1;

    private static final int[] SPEED_X_BY_DIRECTION = {0x20, 0xE0, 0x00, 0x00};
    private static final int[] RETURN_SPEED_X_BY_DIRECTION = {0xF8, 0x08, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xE0, 0x20};
    private static final int[] RETURN_SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0x08, 0xF8, 0x30, 0x20};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState1Table.
    private final int[] homeX = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState2Table.
    private final int[] homeY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection in bank $03. */
    public void initialize(int slot, IntSupplier randomByteSupplier) {
        reset(slot);
        direction[slot] = randomByteSupplier.getAsInt() & 0x03;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int countdown,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        int slot = entity.slot();
        if (!initialized[slot]) {
            // A directly-created ACTIVE snapshot has no EntityInitHandler pass.
            // Keep that test/tool path deterministic while normal room loads use
            // the injected ROM random byte during INIT.
            initialize(slot, () -> 0);
        }

        return switch (state[slot]) {
            case 0 -> captureStart(entity, slot, countdown);
            case 1 -> launch(entity, slot, linkEntityX, linkEntityY, countdown, backgroundCollision);
            case 2 -> advanceForward(entity, slot, countdown, backgroundCollision);
            case 3 -> advanceReturn(entity, slot, countdown);
            default -> throw new IllegalStateException("Invalid Spike Trap state: " + state[slot]);
        };
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
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

    public int homeX(int slot) {
        return homeX[slot];
    }

    public int homeY(int slot) {
        return homeY[slot];
    }

    private Update captureStart(RoomEntity entity, int slot, int countdown) {
        homeX[slot] = entity.x() & 0xFF;
        homeY[slot] = (entity.y() - entity.z()) & 0xFF;
        state[slot] = 1;
        return Update.silent(entity, countdown);
    }

    private Update launch(
            RoomEntity entity,
            int slot,
            int linkEntityX,
            int linkEntityY,
            int countdown,
            RoomEntityBackgroundCollision backgroundCollision) {
        if (countdown != 0) return Update.silent(entity, countdown);

        speedX[slot] = 0;
        speedY[slot] = 0;
        int yDistance = (linkEntityY - entity.y()) & 0xFF;
        int xDistance = (linkEntityX - entity.x()) & 0xFF;
        if (withinLaunchWindow(yDistance)) {
            direction[slot] = (byte) xDistance < 0 ? 1 : 0;
            speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
            countdown = 0x18;
        } else if (withinLaunchWindow(xDistance)) {
            direction[slot] = (byte) yDistance < 0 ? 2 : 3;
            speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
            countdown = 0x10;
        } else {
            return Update.silent(entity, 0);
        }
        if (hasBackgroundCollision(entity, direction[slot], backgroundCollision)) return Update.silent(entity, 0);
        state[slot] = 2;
        return new Update(entity, countdown, EntityCombatEvent.SoundChannel.NOISE, 0x0A);
    }

    private Update advanceForward(
            RoomEntity entity, int slot, int countdown, RoomEntityBackgroundCollision backgroundCollision) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);
        if (countdown == 0 || hasBackgroundCollision(moved, direction[slot], backgroundCollision)) {
            state[slot] = 3;
            return new Update(moved, 0x20, EntityCombatEvent.SoundChannel.JINGLE, 0x07);
        }
        return Update.silent(moved, countdown);
    }

    private Update advanceReturn(RoomEntity entity, int slot, int countdown) {
        if (countdown != 0) return Update.silent(entity, countdown);

        speedX[slot] = RETURN_SPEED_X_BY_DIRECTION[direction[slot]];
        speedY[slot] = RETURN_SPEED_Y_BY_DIRECTION[direction[slot]];
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        RoomEntity moved = withPosition(entity, x, y);
        // The ROM compares the saved visual Y against raw wEntitiesPosYTable.
        if (x == homeX[slot] && y == homeY[slot]) {
            state[slot] = 1;
            return Update.silent(moved, 0x20);
        }
        return Update.silent(moved, 0);
    }

    private static boolean hasBackgroundCollision(
            RoomEntity entity, int direction, RoomEntityBackgroundCollision backgroundCollision) {
        // SpikeTrapEntityHandler sets hActiveEntityNoBGCollision. The shared
        // ROM routine still records collision bits, but does not snap the
        // entity back to the collision point.
        return backgroundCollision.blocks(entity, direction, entity.x(), entity.y());
    }

    private static boolean withinLaunchWindow(int distance) {
        return (((distance & 0xFF) + 0x12) & 0xFF) < 0x24;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    private void reset(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        homeX[slot] = 0;
        homeY[slot] = 0;
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(
            RoomEntity entity, int transitionCountdown, EntityCombatEvent.SoundChannel soundChannel, int soundId) {
        private static Update silent(RoomEntity entity, int countdown) {
            return new Update(entity, countdown, EntityCombatEvent.SoundChannel.NONE, NO_SOUND);
        }
    }
}
