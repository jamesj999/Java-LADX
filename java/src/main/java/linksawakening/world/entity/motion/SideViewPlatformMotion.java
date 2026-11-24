package linksawakening.world.entity.motion;

import java.util.Objects;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;

/** Motion state for the side-view moving platform entity (bank $07). */
public final class SideViewPlatformMotion {
    public static final int ENTITY_HORIZONTAL = 0xA3;
    public static final int ENTITY_VERTICAL = 0xA4;
    public static final int ENTITY_TYPE = 0xA5;

    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState2Table.
    private final int[] rumbleCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState4Table.
    private final int[] pressureTicks = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] automaticInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] automaticReturning = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity, int verticalDelta, int speedY, int privateState2, int privateState4, boolean rumble) {}

    public record Frame(RoomEntity entity, int horizontalDelta, int verticalDelta) {}

    /** Applies the autonomous acceleration at $07:6363 and $07:63FB before movement. */
    public boolean advanceAutomaticSpeed(RoomEntity entity, int countdown, int roomId) {
        validateEntity(entity);
        int slot = entity.slot();
        boolean vertical = entity.type() == ENTITY_VERTICAL;
        if (automaticInertia[slot]++ == 0 && vertical && roomId == 0x65 && entity.y() >= 0x50) {
            automaticReturning[slot] = true;
        }
        if ((automaticInertia[slot] & 3) != 0 || countdown != 0) return false;
        int[] speeds = vertical ? speedY : speedX;
        boolean returning = automaticReturning[slot];
        int speed = (speeds[slot] + (returning ? -1 : 1)) & 0xFF;
        speeds[slot] = speed;
        if (speed != (returning ? 0xFA : 0x06)) return false;
        automaticReturning[slot] = !returning;
        return true;
    }

    /** Records the standing flag written by the automatic platform's contact path. */
    public void markAutomaticStanding(int slot) {
        validateSlot(slot);
        rumbleCountdown[slot] = 0x10;
    }

    public Frame beginFrame(RoomEntity entity) {
        Objects.requireNonNull(entity, "Platform entity cannot be null");
        validateEntity(entity);
        int slot = entity.slot();
        rumbleCountdown[slot] = 0;
        int oldX = entity.x() & 0xFF;
        int oldY = entity.y() & 0xFF;
        int newX = addSpeedToPosition(oldX, speedX[slot], speedXAccumulator, slot);
        int newY = addSpeedToPosition(oldY, speedY[slot], speedYAccumulator, slot);
        return new Frame(
                withPosition(entity, newX, newY), signedByte((newX - oldX) & 0xFF), signedByte((newY - oldY) & 0xFF));
    }

    public Frame restoreVerticalPosition(RoomEntity original, Frame frame) {
        Objects.requireNonNull(original, "Platform entity cannot be null");
        Objects.requireNonNull(frame, "Platform frame cannot be null");
        return new Frame(withPosition(frame.entity(), frame.entity().x(), original.y()), frame.horizontalDelta(), 0);
    }

    public Frame restoreHorizontalPosition(RoomEntity original, Frame frame) {
        Objects.requireNonNull(original, "Platform entity cannot be null");
        Objects.requireNonNull(frame, "Platform frame cannot be null");
        return new Frame(
                withPosition(frame.entity(), original.x(), frame.entity().y()),
                frame.horizontalDelta(),
                frame.verticalDelta());
    }

    // Identity distinguishes cache generations or explicit sentinels; equal payloads must not coalesce.
    @SuppressWarnings("ReferenceEquality")
    public Update finishFrame(
            RoomEntity entity, Frame frame, int frameCounter, boolean standing, boolean activationAllowed) {
        Objects.requireNonNull(frame, "Platform frame cannot be null");
        if (frame.entity() != entity) {
            throw new IllegalArgumentException("Platform frame entity mismatch");
        }
        int slot = entity.slot();
        validateSlot(slot);
        if (standing) {
            rumbleCountdown[slot] = 0x10;
        }
        boolean rumble = false;
        if (!(standing && activationAllowed)) {
            speedY[slot] = 0;
            pressureTicks[slot] = 0;
        } else {
            int previousState4 = pressureTicks[slot];
            if (previousState4 < 4) {
                pressureTicks[slot] = previousState4 + 1;
                rumble = previousState4 == 3;
            }
            if ((frameCounter & 3) == 0 && ((speedY[slot] - 4) & 0x80) != 0) {
                speedY[slot] = (speedY[slot] + 1) & 0xFF;
            }
        }
        return new Update(
                frame.entity(),
                frame.verticalDelta(),
                speedY[slot],
                rumbleCountdown[slot],
                pressureTicks[slot],
                rumble);
    }

    Update advance(RoomEntity entity, int frameCounter, boolean standing, boolean activationAllowed) {
        Objects.requireNonNull(entity, "Platform entity cannot be null");
        if (entity.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException(
                    "Unsupported side-view platform type: 0x" + Integer.toHexString(entity.type()));
        }
        Frame frame = beginFrame(entity);
        return finishFrame(frame.entity(), frame, frameCounter, standing, activationAllowed);
    }

    public int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    public int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    public void setSpeedX(int slot, int value) {
        validateSlot(slot);
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Platform X speed must be an unsigned byte: " + value);
        }
        speedX[slot] = value;
    }

    public void setSpeedY(int slot, int value) {
        validateSlot(slot);
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Platform Y speed must be an unsigned byte: " + value);
        }
        speedY[slot] = value;
    }

    public int rumbleCountdown(int slot) {
        validateSlot(slot);
        return rumbleCountdown[slot];
    }

    public int pressureTicks(int slot) {
        validateSlot(slot);
        return pressureTicks[slot];
    }

    public void clear(int slot) {
        validateSlot(slot);
        speedY[slot] = 0;
        speedX[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        rumbleCountdown[slot] = 0;
        pressureTicks[slot] = 0;
        automaticInertia[slot] = 0;
        automaticReturning[slot] = false;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    private static void validateEntity(RoomEntity entity) {
        if (entity.type() != ENTITY_TYPE && entity.type() != ENTITY_HORIZONTAL && entity.type() != ENTITY_VERTICAL) {
            throw new IllegalArgumentException(
                    "Unsupported side-view platform type: 0x" + Integer.toHexString(entity.type()));
        }
        validateSlot(entity.slot());
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }
}
