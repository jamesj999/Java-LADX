package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$07 PeaHatEntityHandler's rest, takeoff, and flying states. */
final class PeaHatMotion {
    // PeaHatYSpeeds and PeaHatXSpeeds, including their signed-byte ROM values.
    // PeaHatYSpeeds is four bytes immediately followed by PeaHatXSpeeds in ROM;
    // the original indexed lookup can therefore read the first twelve X bytes
    // when privateState4 is 4..15.
    private static final int[] SPEED_Y_BY_PHASE = {
        0x00, 0x05, 0x0A, 0x0D, 0x0E, 0x0D, 0x0A, 0x05,
        0x00, 0xFB, 0xF6, 0xF3, 0xF2, 0xF3, 0xF6, 0xFB
    };
    private static final int[] SPEED_X_BY_PHASE = {
        0x0E, 0x0D, 0x0A, 0x05, 0x00, 0xFB, 0xF6, 0xF3,
        0xF2, 0xF3, 0xF6, 0xFB, 0x00, 0x05, 0x0A, 0x0D
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        slowTransitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateState3[slot] = 0;
        privateState4[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int frameCounter, IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        int frame = frameCounter & 0xFF;
        if ((frame & 0x03) == 0 && slowTransitionCountdown[slot] > 0) {
            slowTransitionCountdown[slot]--;
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int z = entity.z();
        int variant = entity.spriteVariant();

        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity,
                    directionForX(speedX[slot]), x, y)) {
                x = entity.x();
            }
            if (y != entity.y() && backgroundCollision.blocks(entity,
                    directionForY(speedY[slot]), x, y)) {
                y = entity.y();
            }
        }

        switch (state[slot]) {
            case 0 -> {
                if (z != 0 && (frame & 0x07) == 0) {
                    z = (z - 1) & 0xFF;
                }
                if ((frame & 0x07) == 0) {
                    speedX[slot] = approachZero(speedX[slot]);
                    speedY[slot] = approachZero(speedY[slot]);
                }
                if (slowTransitionCountdown[slot] == 0) {
                    state[slot] = 1;
                }
                if (privateState1[slot] != 0 && (frame & 0x1F) == 0) {
                    privateState1[slot]--;
                }
                variant = animate(slot);
            }
            case 1 -> {
                if (privateState1[slot] < 0x08) {
                    if ((frame & 0x0F) == 0) {
                        privateState1[slot]++;
                    }
                    variant = animate(slot);
                } else {
                    slowTransitionCountdown[slot] = 0x80
                        + (randomByteSupplier.getAsInt() & 0x1F);
                    state[slot] = 2;
                }
            }
            case 2 -> {
                variant = animate(slot);
                if (z != 0x10) {
                    if ((frame & 0x07) == 0) {
                        z = (z + 1) & 0xFF;
                    }
                } else {
                    if (slowTransitionCountdown[slot] == 0) {
                        slowTransitionCountdown[slot] = 0x60;
                        state[slot] = 0;
                    }
                    updateFlyingSpeed(slot, randomByteSupplier);
                }
            }
            default -> throw new IllegalStateException("Invalid PeaHat state: " + state[slot]);
        }

        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), z);
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    boolean isGrounded(RoomEntity entity) {
        return state[entity.slot()] == 0 && entity.z() == 0;
    }

    int state(int slot) {
        return state[slot];
    }

    int slowTransitionCountdown(int slot) {
        return slowTransitionCountdown[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState4(int slot) {
        return privateState4[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
    }

    int privateState3(int slot) {
        return privateState3[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private int animate(int slot) {
        int sum = privateState1[slot] + inertia[slot];
        int value = sum & 0xFF;
        boolean carry = sum > 0xFF;
        inertia[slot] = value;
        for (int count = 0; count < 5; count++) {
            boolean nextCarry = (value & 0x01) != 0;
            value = (value >>> 1) | (carry ? 0x80 : 0);
            carry = nextCarry;
        }
        return value & 0x01;
    }

    private void updateFlyingSpeed(int slot, IntSupplier randomByteSupplier) {
        privateState3[slot] = (privateState3[slot] + 1) & 0xFF;
        if (privateState3[slot] < 0x18) {
            return;
        }
        privateState3[slot] = 0;
        privateState4[slot] = (privateState2[slot] + privateState4[slot]) & 0x0F;
        speedY[slot] = signedShiftRight(SPEED_Y_BY_PHASE[privateState4[slot]], 1);
        speedX[slot] = signedShiftRight(SPEED_X_BY_PHASE[privateState4[slot]], 1);
        if ((randomByteSupplier.getAsInt() & 0x07) != 0) {
            return;
        }
        privateState2[slot] = ((randomByteSupplier.getAsInt() & 0x02) - 1) & 0xFF;
    }

    private static int approachZero(int speed) {
        int signedSpeed = (speed & 0x80) != 0 ? speed - 0x100 : speed;
        if (signedSpeed < 0) {
            return (speed + 1) & 0xFF;
        }
        if (signedSpeed > 0) {
            return (speed - 1) & 0xFF;
        }
        return 0;
    }

    private static int signedShiftRight(int value, int amount) {
        int signedValue = (value & 0x80) != 0 ? value - 0x100 : value;
        return (signedValue >> amount) & 0xFF;
    }

    private static int directionForX(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
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
