package linksawakening.world;

import java.util.Objects;

/** Motion state for the side-view moving platform entity (bank $07). */
final class SideViewPlatformMotion {
    static final int ENTITY_TYPE = 0xA5;

    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, int verticalDelta, int speedY, int privateState2,
                  int privateState4, boolean rumble) {
    }

    Update advance(RoomEntity entity, int frameCounter, boolean standing,
                   boolean activationAllowed) {
        Objects.requireNonNull(entity, "Platform entity cannot be null");
        if (entity.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported side-view platform type: 0x"
                + Integer.toHexString(entity.type()));
        }
        int slot = entity.slot();
        validateSlot(slot);

        // 07:639E clears private state before applying the current speed.
        privateState2[slot] = 0;
        int oldY = entity.y() & 0xFF;
        int newY = addSpeedToPosition(oldY, speedY[slot], speedYAccumulator, slot);
        int verticalDelta = signedByte((newY - oldY) & 0xFF);

        if (standing) {
            privateState2[slot] = 0x10;
        }
        boolean rumble = false;
        if (!(standing && activationAllowed)) {
            speedY[slot] = 0;
            privateState4[slot] = 0;
        } else {
            int previousState4 = privateState4[slot];
            if (previousState4 < 4) {
                privateState4[slot] = previousState4 + 1;
                rumble = previousState4 == 3;
            }
            if ((frameCounter & 3) == 0 && ((speedY[slot] - 4) & 0x80) != 0) {
                speedY[slot] = (speedY[slot] + 1) & 0xFF;
            }
        }

        return new Update(withY(entity, newY), verticalDelta, speedY[slot],
            privateState2[slot], privateState4[slot], rumble);
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    void setSpeedY(int slot, int value) {
        validateSlot(slot);
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Platform Y speed must be an unsigned byte: "
                + value);
        }
        speedY[slot] = value;
    }

    int privateState2(int slot) {
        validateSlot(slot);
        return privateState2[slot];
    }

    int privateState4(int slot) {
        validateSlot(slot);
        return privateState4[slot];
    }

    void clear(int slot) {
        validateSlot(slot);
        speedY[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState2[slot] = 0;
        privateState4[slot] = 0;
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

    private static RoomEntity withY(RoomEntity entity, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(),
            y & 0xFF, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z(),
            entity.deathSpriteVariant(), entity.powerRecoilDeath());
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
