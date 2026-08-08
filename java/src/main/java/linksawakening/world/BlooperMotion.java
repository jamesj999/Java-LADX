package linksawakening.world;

/** Bank-$07 BlooperEntityHandler's water-bound movement and swim state machine. */
final class BlooperMotion {
    private static final int STATE_SWIM_UP = 0;
    private static final int STATE_CHASE = 1;
    private static final int DIRECTION_RIGHT = 0;
    private static final int DIRECTION_LEFT = 1;
    private static final int DIRECTION_UP = 2;
    private static final int DIRECTION_DOWN = 3;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Mirrors UpdateEntityPosWithSpeed_07 and its background helper. */
    Movement move(RoomEntity entity, int frameCounter,
                  RoomEntityBackgroundInteraction backgroundInteraction,
                  int ignoreHitsCountdown) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (backgroundInteraction != null) {
            if (x != entity.x()) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, directionForHorizontalSpeed(speedX[slot]), x, entity.y(),
                    ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    x = entity.x();
                }
            }
            if (y != entity.y()) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, directionForVerticalSpeed(speedY[slot]), x, y,
                    ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    y = entity.y();
                }
            }
        }

        return new Movement(entity, withPosition(entity, x, y));
    }

    /**
     * Runs the source's ground-status check and then the Blooper state handler.
     * The runtime invokes the ground helper before this method because the ROM
     * handler reads the status written by ApplyEntityInteractionWithBackground.
     */
    Update finish(Movement movement, RoomEntityGroundInteraction.Result groundResult,
                  int frameCounter, int linkEntityX, int linkEntityY) {
        if (groundResult == null) {
            throw new IllegalArgumentException("Blooper ground result cannot be null");
        }
        int slot = movement.entity().slot();
        RoomEntity updated = groundResult.entity();
        if ((groundResult.groundStatus() & 0xFF) == 0) {
            // hActiveEntityPosX/Y still contain the pre-move handler position.
            privateCountdown3[slot] = 0x10;
            updated = withPosition(updated, movement.original().x(), movement.original().y());
        }

        int variant = 0;
        if (state[slot] == STATE_SWIM_UP) {
            if (transitionCountdown[slot] != 0) {
                speedY[slot] = approachFour(speedY[slot]);
                speedX[slot] = approachZero(speedX[slot]);
            } else if (directionToLinkY(updated.y(), linkEntityY) != DIRECTION_DOWN) {
                speedX[slot] = 0;
                speedY[slot] = 0;
                transitionCountdown[slot] = 0x25;
                direction[slot] = directionToLinkX(updated.x(), linkEntityX);
                state[slot] = STATE_CHASE;
            } else {
                speedY[slot] = approachFour(speedY[slot]);
                speedX[slot] = approachZero(speedX[slot]);
            }
        } else if (state[slot] == STATE_CHASE) {
            if (transitionCountdown[slot] == 0) {
                transitionCountdown[slot] = 0x40;
                state[slot] = STATE_SWIM_UP;
            } else {
                variant = 1;
                if ((frameCounter & 0x01) == 0) {
                    speedY[slot] = (speedY[slot] - 1) & 0xFF;
                    speedX[slot] = (speedX[slot]
                        + (direction[slot] == DIRECTION_RIGHT ? 1 : -1)) & 0xFF;
                }
            }
        } else {
            throw new IllegalStateException("Invalid Blooper state: " + state[slot]);
        }

        RoomEntity presented = withVariant(updated, variant);
        RoomEntityGroundInteraction.Result finalGroundResult =
            new RoomEntityGroundInteraction.Result(
                presented, groundResult.groundStatus(), groundResult.pitTransition(),
                groundResult.unloaded(), groundResult.waterSplash());
        return new Update(presented, finalGroundResult);
    }

    void decrementPrivateCountdown3(int slot) {
        if (privateCountdown3[slot] > 0) {
            privateCountdown3[slot]--;
        }
    }

    void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    int privateCountdown3(int slot) {
        return privateCountdown3[slot];
    }

    void setStateForTest(int slot, int newState, int newCountdown, int newDirection,
                         int newSpeedX, int newSpeedY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        state[slot] = newState;
        transitionCountdown[slot] = newCountdown & 0xFF;
        direction[slot] = newDirection & 0xFF;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private void reset(int slot) {
        state[slot] = STATE_SWIM_UP;
        transitionCountdown[slot] = 0;
        direction[slot] = DIRECTION_RIGHT;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateCountdown3[slot] = 0;
    }

    private static int approachFour(int speed) {
        int difference = (speed - 0x04) & 0xFF;
        if (difference == 0) {
            return speed & 0xFF;
        }
        return (difference & 0x80) != 0
            ? (speed + 1) & 0xFF : (speed - 1) & 0xFF;
    }

    private static int approachZero(int speed) {
        speed &= 0xFF;
        if (speed == 0) {
            return 0;
        }
        return (speed & 0x80) != 0 ? (speed + 1) & 0xFF : speed - 1;
    }

    private static int directionToLinkX(int entityX, int linkX) {
        return signedByte((linkX - entityX) & 0xFF) < 0
            ? DIRECTION_LEFT : DIRECTION_RIGHT;
    }

    private static int directionToLinkY(int entityY, int linkY) {
        return signedByte((linkY - entityY) & 0xFF) < 0
            ? DIRECTION_UP : DIRECTION_DOWN;
    }

    private static int directionForHorizontalSpeed(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT
            : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int directionForVerticalSpeed(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.UP
            : EntityBackgroundCollisionResult.DOWN;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteVariant(), entity.entityFlipAttribute(), entity.spriteTileOffset(),
            entity.z());
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            entity.x(), entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Movement(RoomEntity original, RoomEntity entity) {
    }

    record Update(RoomEntity entity, RoomEntityGroundInteraction.Result groundResult) {
    }
}
