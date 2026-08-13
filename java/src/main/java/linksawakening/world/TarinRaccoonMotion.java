package linksawakening.world;

/** Bank-$05 outdoor {@code TarinEntityHandler}, including the raccoon transformation. */
final class TarinRaccoonMotion {
    static final int ENTITY_TYPE = 0x3F;
    private static final int NO_EVENT = -1;
    private static final int[] TRANSFORMATION_VARIANTS = {0, 4, 5, 6, 7, 1};

    record Input(int frameCounter, int linkX, int linkY, int linkDirection,
                 boolean actionHeld, boolean dialogActive, boolean powderHit,
                 int linkAttackStepAnimationCountdown, boolean linkAirborne,
                 boolean inventoryAppearing, int dialogCooldown, int windowY,
                 int slowTransitionCountdown, int transitionCountdown) {
        Input(int frameCounter, int linkX, int linkY, int linkDirection,
              boolean actionHeld, boolean dialogActive, boolean powderHit) {
            this(frameCounter, linkX, linkY, linkDirection, actionHeld, dialogActive,
                powderHit, 0, false);
        }

        Input(int frameCounter, int linkX, int linkY, int linkDirection,
              boolean actionHeld, boolean dialogActive, boolean powderHit,
              int linkAttackStepAnimationCountdown) {
            this(frameCounter, linkX, linkY, linkDirection, actionHeld, dialogActive,
                powderHit, linkAttackStepAnimationCountdown, false);
        }

        Input(int frameCounter, int linkX, int linkY, int linkDirection,
              boolean actionHeld, boolean dialogActive, boolean powderHit,
              int linkAttackStepAnimationCountdown, boolean linkAirborne) {
            this(frameCounter, linkX, linkY, linkDirection, actionHeld, dialogActive,
                powderHit, linkAttackStepAnimationCountdown, linkAirborne,
                false, 0, 0x80, 0, 0);
        }

        Input {
            if (linkDirection < 0 || linkDirection > 3) {
                throw new IllegalArgumentException("Invalid Link direction: " + linkDirection);
            }
            validateByte(frameCounter, "Frame counter");
            validateByte(linkX, "Link X");
            validateByte(linkY, "Link Y");
            validateByte(dialogCooldown, "Dialog cooldown");
            validateByte(windowY, "Window Y");
            validateByte(slowTransitionCountdown, "Slow transition countdown");
            validateByte(transitionCountdown, "Transition countdown");
        }
    }

    record Update(RoomEntity entity, int state, int spriteVariant,
                  boolean shouldGetLost, int dialogGlobalId,
                  boolean linkMotionBlocked, boolean clearLinkAttack,
                  int linkFacingDirection, boolean pushLink,
                  boolean roomChanged, boolean tarinFlag, int soundChannel, int soundId,
                  boolean spawnBomb, int slowTransitionCountdown,
                  int transitionCountdown, int speedX, int speedY, int speedZ,
                  int collisionFlags, boolean nearLinkLatch) {
    }

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] warningShown = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] accumulatorX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] accumulatorY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] accumulatorZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] animationAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] animationInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] animationIndex = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] nearLinkLatch = new boolean[EntityRoomLoader.MAX_ENTITIES];

    Update advance(RoomEntity source, Input input) {
        return advance(source, input, null);
    }

    Update advance(RoomEntity source, Input input,
                   RoomEntityBackgroundInteraction backgroundInteraction) {
        checkType(source);
        int slot = source.slot();
        int slowCountdown = input.slowTransitionCountdown();
        int transitionCountdown = input.transitionCountdown();
        int variant = source.spriteVariant();
        boolean shouldGetLost = false;
        int dialog = NO_EVENT;
        boolean blockLink = false;
        boolean clearAttack = false;
        int linkFacing = NO_EVENT;
        boolean pushLink = false;
        boolean roomChanged = false;
        boolean tarinFlag = false;
        int soundId = NO_EVENT;
        boolean spawnBomb = false;
        int collisionFlags = 0;
        RoomEntity entity = source;

        switch (state[slot]) {
            case 0 -> {
                variant = (input.frameCounter() >>> 4) & 1;
                if ((input.linkY() & 0xFF) < 0x30) {
                    shouldGetLost = true;
                    variant = 2 + ((input.frameCounter() >>> 3) & 1);
                } else {
                    warningShown[slot] = false;
                }
                entity = withVariant(source, variant);
                if (!input.dialogActive() && !input.inventoryAppearing()) {
                    if ((input.linkY() & 0xFF) < 0x20 && !warningShown[slot]) {
                        warningShown[slot] = true;
                        dialog = 0x021;
                    } else if (input.actionHeld() && nearbyAndFacing(entity, input)
                        && input.linkAttackStepAnimationCountdown() == 0
                        && !input.linkAirborne() && input.dialogCooldown() == 0
                        && input.windowY() == 0x80) {
                        dialog = 0x00D;
                    }
                }
            }
            case 1 -> {
                blockLink = true;
                clearAttack = true;
                linkFacing = directionToLink(source, input.linkX(), input.linkY()) ^ 0x01;

                int animationSum = animationAccumulator[slot] + animationInertia[slot];
                animationAccumulator[slot] = animationSum & 0xFF;
                if (animationSum > 0xFF) {
                    animationIndex[slot] = (animationIndex[slot] + 1) % 6;
                }
                variant = TRANSFORMATION_VARIANTS[animationIndex[slot]];
                entity = withVariant(source, variant);

                if (slowCountdown == 0) {
                    speedZ[slot] = 0;
                    state[slot] = 2;
                    variant = 9;
                    entity = withVariant(entity, variant);
                    spawnBomb = true;
                    roomChanged = true;
                    tarinFlag = true;
                    break;
                }

                if ((input.frameCounter() & 1) == 0 && animationInertia[slot] < 0xF0) {
                    animationInertia[slot]++;
                }
                Movement moved = moveWithBackground(entity, backgroundInteraction, slot);
                entity = withPositionAndVariant(entity, moved.x(), moved.y(), variant);
                collisionFlags = moved.collisionFlags();

                if (slowCountdown < 6) {
                    if ((entity.y() & 0xFF) < 0x30) {
                        slowCountdown = 8;
                    } else {
                        speedZ[slot] = (speedZ[slot] + 1) & 0xFF;
                        speedX[slot] = dampTowardsZero(speedX[slot]);
                        speedY[slot] = dampTowardsZero(speedY[slot]);
                        entity = withZ(entity, addSpeedToPosition(entity.z(), speedZ[slot],
                            accumulatorZ, slot));
                        break;
                    }
                }

                if ((collisionFlags & 0x03) != 0) {
                    speedX[slot] = (-signedByte(speedX[slot])) & 0xFF;
                    soundId = 0x09;
                }
                if ((collisionFlags & 0x0C) != 0) {
                    speedY[slot] = (-signedByte(speedY[slot])) & 0xFF;
                    soundId = 0x09;
                }
                if (slowCountdown < 0x60 && (input.frameCounter() & 3) == 0) {
                    speedX[slot] = accelerateToLimit(speedX[slot]);
                    speedY[slot] = accelerateToLimit(speedY[slot]);
                }
            }
            case 2 -> {
                blockLink = true;
                int z = addSpeedToPosition(entity.z(), speedZ[slot], accumulatorZ, slot);
                speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
                entity = withZ(entity, z);
                if ((z & 0x80) != 0) {
                    entity = withZ(entity, 0);
                    soundId = 0x23;
                    transitionCountdown = 0x40;
                    variant = 8 + directionToLink(entity, input.linkX(), input.linkY());
                    entity = withVariant(entity, variant);
                    nearLinkLatch[slot] = withinSignedWindow(input.linkX() - entity.x(), 0x12)
                        && withinSignedWindow(input.linkY() - entity.y(), 0x12);
                    state[slot] = 3;
                }
            }
            case 3 -> {
                if (transitionCountdown == 1) {
                    dialog = 0x00A;
                    break;
                }
                if (transitionCountdown != 0) {
                    blockLink = true;
                    break;
                }
                if ((input.frameCounter() & 0x1F) == 0) {
                    variant = 8 + directionToLink(entity, input.linkX(), input.linkY());
                    entity = withVariant(entity, variant);
                }
                pushLink = !nearLinkLatch[slot];
                if (!input.dialogActive() && !input.inventoryAppearing()
                    && input.actionHeld() && nearbyAndFacing(entity, input)
                    && !input.linkAirborne() && input.dialogCooldown() == 0
                    && input.windowY() == 0x80) {
                    dialog = 0x00B;
                }
            }
            default -> throw new IllegalStateException("Invalid Tarin state: " + state[slot]);
        }

        return new Update(entity, state[slot], entity.spriteVariant(), shouldGetLost, dialog,
            blockLink, clearAttack, linkFacing, pushLink, roomChanged, tarinFlag,
            soundId < 0 ? NO_EVENT : 0, soundId, spawnBomb, slowCountdown,
            transitionCountdown, speedX[slot] & 0xFF, speedY[slot] & 0xFF,
            speedZ[slot] & 0xFF, collisionFlags, nearLinkLatch[slot]);
    }

    int state(int slot) {
        return state[slot];
    }

    void startPowderTransformation(int slot) {
        if (state[slot] == 0) {
            state[slot] = 1;
        }
    }

    void setStateForTest(int slot, int newState, int newSpeedX, int newSpeedY,
                         int newSpeedZ, int privateState2, int privateState3,
                         boolean nearLatch) {
        if (newState < 0 || newState > 3) {
            throw new IllegalArgumentException("Tarin state must be 0..3");
        }
        state[slot] = newState;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
        speedZ[slot] = newSpeedZ & 0xFF;
        animationAccumulator[slot] = privateState2 & 0xFF;
        animationInertia[slot] = privateState3 & 0xFF;
        animationIndex[slot] = 0;
        nearLinkLatch[slot] = nearLatch;
        accumulatorX[slot] = 0;
        accumulatorY[slot] = 0;
        accumulatorZ[slot] = 0;
    }

    private Movement moveWithBackground(RoomEntity entity,
                                        RoomEntityBackgroundInteraction interaction,
                                        int slot) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], accumulatorX, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], accumulatorY, slot);
        int flags = 0;
        if (interaction != null && x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction,
                x, entity.y());
            if (result.blocked()) {
                flags |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (interaction != null && y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                flags |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new Movement(x & 0xFF, y & 0xFF, flags & 0x0F);
    }

    private static int accelerateToLimit(int value) {
        int unsigned = value & 0xFF;
        if (unsigned == 0x30 || unsigned == 0xD0) {
            return unsigned;
        }
        return (unsigned + (signedByte(unsigned) < 0 ? -1 : 1)) & 0xFF;
    }

    private static int dampTowardsZero(int value) {
        int signed = signedByte(value);
        return signed == 0 ? 0 : (signed + (signed < 0 ? 1 : -1)) & 0xFF;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                          int slot) {
        int fractionalSum = accumulator[slot] + ((speed & 0x0F) << 4);
        accumulator[slot] = fractionalSum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static boolean nearbyAndFacing(RoomEntity entity, Input input) {
        int yWindow = (input.linkY() - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x28) {
            return false;
        }
        int xWindow = (input.linkX() - entity.x() + 0x10) & 0xFF;
        return xWindow < 0x20 && directionToLink(entity, input.linkX(), input.linkY())
            == (input.linkDirection() ^ 0x01);
    }

    private static int directionToLink(RoomEntity entity, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entity.x());
        int distanceY = signedByte(linkY - entity.y());
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static boolean withinSignedWindow(int value, int radius) {
        return ((value + radius) & 0xFF) < radius * 2;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity source, int x, int y,
                                                      int variant) {
        return new RoomEntity(source.slot(), source.sourceLoadOrder(), source.type(), x, y,
            source.status(), source.spriteDefinition(), variant,
            source.entityFlipAttribute(), source.spriteTileOffset(), source.z(),
            source.deathSpriteVariant(), source.powerRecoilDeath());
    }

    private static RoomEntity withVariant(RoomEntity source, int variant) {
        return withPositionAndVariant(source, source.x(), source.y(), variant);
    }

    private static RoomEntity withZ(RoomEntity source, int z) {
        return new RoomEntity(source.slot(), source.sourceLoadOrder(), source.type(),
            source.x(), source.y(), source.status(), source.spriteDefinition(),
            source.spriteVariant(), source.entityFlipAttribute(), source.spriteTileOffset(), z,
            source.deathSpriteVariant(), source.powerRecoilDeath());
    }

    private static void checkType(RoomEntity source) {
        if (source == null || source.type() != ENTITY_TYPE) {
            throw new IllegalArgumentException("Entity is not Tarin");
        }
    }

    private static void validateByte(int value, String label) {
        if ((value & ~0xFF) != 0) {
            throw new IllegalArgumentException(label + " must be an unsigned byte");
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private record Movement(int x, int y, int collisionFlags) {
    }
}
