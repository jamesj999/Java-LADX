package linksawakening.world;

/** Active state machines from bank $06's Rolling Bones and rolling-bar handlers. */
final class RollingBonesMotion {
    static final int ENTITY_BOSS = 0x81;
    static final int ENTITY_BAR = 0x82;

    record BossUpdate(RoomEntity entity, int transitionCountdown, int jingleId) {
    }

    record BarUpdate(RoomEntity entity, boolean rollingSound, boolean strongBump,
                     int screenShakeCountdown) {
    }

    private static final int STATE_APPROACH_BAR = 0;
    private static final int STATE_PREPARE_ROLL = 1;
    private static final int STATE_WAIT_FOR_BAR = 2;
    private static final int STATE_JUMP_AWAY = 3;

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
    private final int[] barXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] barTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private int rollingSoundCounter;

    void initializeBoss(int slot) {
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

    void initializeBar(int slot) {
        barState[slot] = BAR_STATE_RESTING;
        barSpeedX[slot] = 0;
        barXAccumulator[slot] = 0;
        barTransitionCountdown[slot] = 0;
    }

    BossUpdate advanceBoss(RoomEntity entity, RoomEntity bar, int transitionCountdown,
                           int health, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int x = entity.x();
        int y = entity.y();
        int z = entity.z();
        int variant = bossSpriteVariant[slot];
        int jingleId = -1;

        int previousSpeedZ = bossSpeedZ[slot];
        z = addSpeed(z, previousSpeedZ, bossZAccumulator, slot);
        bossSpeedZ[slot] -= 2;
        boolean landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            int impactSpeedZ = bossSpeedZ[slot];
            bossSpeedZ[slot] = 0;
            if (impactSpeedZ < -0x0E) {
                jingleId = 0x20;
            }
        }

        if (bossSpeedX[slot] != 0) {
            bossDirection[slot] = bossSpeedX[slot] < 0 ? 0 : 3;
        }

        switch (bossState[slot]) {
            case STATE_APPROACH_BAR -> {
                if (transitionCountdown == 0) {
                    int delta = signedByte(entity.x() - bar.x());
                    bossSpeedX[slot] = delta < 0 ? 0x08 : -0x08;
                    if (delta >= -0x10 && delta < 0x10) {
                        transitionCountdown = 0x18;
                        bossState[slot] = STATE_PREPARE_ROLL;
                    } else {
                        x = moveX(entity, x, y, bossSpeedX[slot],
                            bossXAccumulator, slot, backgroundCollision).position();
                        if (landed) {
                            bossSpeedZ[slot] = 0x0C;
                        }
                    }
                    variant = 1;
                }
            }
            case STATE_PREPARE_ROLL -> {
                if (transitionCountdown == 0) {
                    int barSpeed = bossDirection[slot] == 0 ? -0x10 : 0x10;
                    launchBar(bar.slot(), barSpeed);
                    transitionCountdown = 0x20;
                    bossState[slot] = STATE_WAIT_FOR_BAR;
                    variant = 0;
                }
            }
            case STATE_WAIT_FOR_BAR -> {
                if (transitionCountdown == 0) {
                    bossState[slot] = STATE_JUMP_AWAY;
                }
            }
            case STATE_JUMP_AWAY -> {
                variant = z < 0x08 ? 1 : 2;
                if (transitionCountdown == 1) {
                    int length = health < 5 ? 0x14 : 0x10;
                    bossSpeedZ[slot] = health < 5 ? 0x16 : 0x19;
                    bossSpeedX[slot] = bossDirection[slot] == 0 ? -length : length;
                    bossSpeedY[slot] = y < 0x50 ? length : -length;
                } else if (transitionCountdown == 0 && landed) {
                    transitionCountdown = 0x10;
                } else if (transitionCountdown == 0) {
                    MoveResult xMove = moveX(entity, x, y, bossSpeedX[slot],
                        bossXAccumulator, slot, backgroundCollision);
                    x = xMove.position();
                    MoveResult yMove = moveY(entity, x, y, bossSpeedY[slot],
                        bossYAccumulator, slot, backgroundCollision);
                    y = yMove.position();
                    if (xMove.blocked()) {
                        bossState[slot] = STATE_APPROACH_BAR;
                        transitionCountdown = 0x08;
                        bossSpeedX[slot] = 0;
                        bossSpeedY[slot] = 0;
                    }
                }
            }
            default -> initializeBoss(slot);
        }

        // func_006_6E7E indexes the six rectangle variants by adding the
        // direction-table value (0 or 3) to the handler's animation variant.
        bossSpriteVariant[slot] = variant;
        int displayVariant = variant + bossDirection[slot];
        return new BossUpdate(withPositionAndVariant(entity, x, y, z, displayVariant),
            transitionCountdown, jingleId);
    }

    BarUpdate advanceBar(RoomEntity entity, int frameCounter,
                         RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (barTransitionCountdown[slot] > 0) {
            barTransitionCountdown[slot]--;
        }
        int x = entity.x();
        boolean rollingSound = false;
        boolean strongBump = false;
        int screenShake = 0;
        boolean wasRolling = barState[slot] == BAR_STATE_ROLLING;

        if (barState[slot] != BAR_STATE_RESTING) {
            MoveResult movement = moveX(entity, x, entity.y(), barSpeedX[slot],
                barXAccumulator, slot, backgroundCollision);
            x = movement.position();
            if (barState[slot] == BAR_STATE_ROLLING && movement.blocked()) {
                barSpeedX[slot] = -(barSpeedX[slot] >> 1);
                barState[slot] = BAR_STATE_DECELERATING;
                strongBump = true;
                screenShake = 0x20;
            }
        }

        if (wasRolling) {
            rollingSoundCounter++;
            if (rollingSoundCounter >= 9) {
                rollingSoundCounter = 0;
                rollingSound = true;
            }
        } else if (barState[slot] == BAR_STATE_DECELERATING
            && (frameCounter & 0x07) == 0) {
            if (barSpeedX[slot] == 0) {
                barState[slot] = BAR_STATE_RESTING;
                barTransitionCountdown[slot] = 0x50;
            } else {
                barSpeedX[slot] += barSpeedX[slot] < 0 ? 1 : -1;
            }
        }

        int absoluteSpeed = Math.abs(barSpeedX[slot]);
        int cadenceMask = absoluteSpeed >= 8 ? 4 : absoluteSpeed >= 4 ? 8
            : absoluteSpeed >= 2 ? 0x10 : 0x20;
        int variant = barSpeedX[slot] == 0 ? entity.spriteVariant()
            : (frameCounter & cadenceMask) == 0 ? 0 : 1;
        return new BarUpdate(withPositionAndVariant(entity, x, entity.y(), entity.z(), variant),
            rollingSound, strongBump, screenShake);
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

    int barState(int slot) {
        return barState[slot];
    }

    int barSpeedX(int slot) {
        return barSpeedX[slot];
    }

    int barTransitionCountdown(int slot) {
        return barTransitionCountdown[slot];
    }

    private static MoveResult moveX(RoomEntity entity, int x, int y, int speed,
                                    int[] accumulators, int slot,
                                    RoomEntityBackgroundCollision collision) {
        int next = addSpeed(x, speed, accumulators, slot);
        int direction = speed < 0 ? 1 : 0;
        return collision != null && collision.blocks(entity, direction, next, y)
            ? new MoveResult(x, true) : new MoveResult(next, false);
    }

    private static MoveResult moveY(RoomEntity entity, int x, int y, int speed,
                                    int[] accumulators, int slot,
                                    RoomEntityBackgroundCollision collision) {
        int next = addSpeed(y, speed, accumulators, slot);
        int direction = speed < 0 ? 2 : 3;
        return collision != null && collision.blocks(entity, direction, x, next)
            ? new MoveResult(y, true) : new MoveResult(next, false);
    }

    private static int addSpeed(int position, int speed, int[] accumulators, int slot) {
        int fixed = (position << 4) + accumulators[slot] + speed;
        accumulators[slot] = fixed & 0x0F;
        return (fixed >> 4) & 0xFF;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                      int z, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), z);
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned >= 0x80 ? unsigned - 0x100 : unsigned;
    }

    private record MoveResult(int position, boolean blocked) {
    }
}
