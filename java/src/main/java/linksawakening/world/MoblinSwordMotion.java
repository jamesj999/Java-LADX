package linksawakening.world;

/** ROM bank-$07 Moblin Sword state machine plus bank-$03 falling presentation state. */
final class MoblinSwordMotion {
    private static final int[] VARIANT_BY_DIRECTION = {6, 4, 2, 0};
    private static final int[] SPEED_X_BY_DIRECTION = {0x06, 0xFA, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xFA, 0x06};
    private static final int MAP_BOWWOW_HIDEOUT = 0x15;

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] collisionsTable = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record Update(RoomEntity entity, boolean dialogRequested) {
        Update(RoomEntity entity) {
            this(entity, false);
        }
    }

    /** Mirrors EntityInitMoblinSword, including its post-variant direction flip. */
    void initialize(int slot, int activeX) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        int initialDirection = (activeX & 0x10) != 0 ? 0 : 3;
        direction[slot] = initialDirection ^ 0x01;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        inertia[slot] = 1;
        collisionsTable[slot] = 0;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int ignoreHitsCountdown, int frameCounter) {
        return advance(entity, linkEntityX, linkEntityY, backgroundInteraction,
            ignoreHitsCountdown, 0, frameCounter);
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int ignoreHitsCountdown, int alertingSoundCounter, int frameCounter) {
        return advance(entity, linkEntityX, linkEntityY, backgroundInteraction,
            ignoreHitsCountdown, alertingSoundCounter, -1, 0, frameCounter);
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int ignoreHitsCountdown, int alertingSoundCounter, int mapId,
                   int transitionSequenceCounter, int frameCounter) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot, entity.x());
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }

        if (ignoreHitsCountdown > 0 || alertingSoundCounter > 0) {
            enterAlertState(slot);
        }

        RoomEntity updated = entity;
        boolean dialogRequested = false;
        boolean enteredWalkingState = false;
        boolean enteredStateTwo = false;
        if (state[slot] == 0) {
            inertia[slot] = 0;
            if (shouldAlert(entity.x(), entity.y(), linkEntityX, linkEntityY, direction[slot])) {
                enterAlertState(slot);
                enteredStateTwo = true;
            } else if (transitionCountdown[slot] == 0) {
                transitionCountdown[slot] = 0x80;
                state[slot] = 1;
                reverseDirectionAndSetSpeed(slot);
                enteredWalkingState = true;
            }
        }
        if (state[slot] == 1 && !enteredWalkingState) {
            if (shouldAlert(entity.x(), entity.y(), linkEntityX, linkEntityY, direction[slot])) {
                enterAlertState(slot);
                enteredStateTwo = true;
            }
            if (collisionsTable[slot] != 0) {
                reverseDirectionAndSetSpeed(slot);
                collisionsTable[slot] = 0;
            }
            updated = moveAndRender(entity, backgroundInteraction, ignoreHitsCountdown,
                frameCounter, true);
            if (transitionCountdown[slot] == 0) {
                transitionCountdown[slot] = 0x30;
                state[slot] = 2;
                enteredStateTwo = true;
            }
        }
        if (state[slot] == 2 && !enteredStateTwo) {
            if (privateCountdown1[slot] != 0) {
                if (mapId == MAP_BOWWOW_HIDEOUT && transitionSequenceCounter == 0x04
                    && privateState3[slot] == 0) {
                    privateState3[slot] = 1;
                    dialogRequested = true;
                }
                updated = faceLink(updated, linkEntityX, linkEntityY);
            } else {
                if (transitionCountdown[slot] == 0) {
                    transitionCountdown[slot] = 0x18;
                    state[slot] = 0;
                }
                updated = setVariant(updated);
                updated = setVariant(updated);
                updated = moveAndRender(updated, backgroundInteraction,
                    ignoreHitsCountdown, frameCounter, false);
                if (((frameCounter ^ slot) & 0x0F) == 0) {
                    applyVectorTowardsLink(slot, updated.x(), updated.y(),
                        linkEntityX, linkEntityY, 0x0A);
                }
                updated = faceLink(updated, linkEntityX, linkEntityY);
            }
        }
        return new Update(updated, dialogRequested);
    }

    /** Mirrors the three SetEntityVariantForDirection calls before the sword handler. */
    int advancePresentationVariant(int slot, int activeX, int repetitions) {
        if (!initialized[slot]) {
            initialize(slot, activeX);
        }
        for (int i = 0; i < repetitions; i++) {
            inertia[slot] = (inertia[slot] + 1) & 0xFF;
        }
        return VARIANT_BY_DIRECTION[direction[slot]] | ((inertia[slot] >>> 3) & 0x01);
    }

    int state(int slot) {
        return state[slot];
    }

    int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int privateState3(int slot) {
        return privateState3[slot];
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

    int inertia(int slot) {
        return inertia[slot];
    }

    void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateState3[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        inertia[slot] = 0;
        collisionsTable[slot] = 0;
        initialized[slot] = false;
    }

    private void enterAlertState(int slot) {
        if (state[slot] != 2) {
            privateCountdown1[slot] = 0x10;
        }
        state[slot] = 2;
        transitionCountdown[slot] = 0x80;
    }

    private void reverseDirectionAndSetSpeed(int slot) {
        direction[slot] ^= 0x01;
        speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
        speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
    }

    private RoomEntity moveAndRender(RoomEntity entity,
                                     RoomEntityBackgroundInteraction backgroundInteraction,
                                     int ignoreHitsCountdown, int frameCounter,
                                     boolean renderVariant) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        collisionsTable[slot] = 0;
        if (backgroundInteraction != null) {
            if (x != entity.x()) {
                int direction = speedX[slot] < 0x80 ? 0 : 1;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction, x, entity.y(), ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    x = entity.x();
                    collisionsTable[slot] |= result.collisionFlag();
                }
            }
            if (y != entity.y()) {
                int direction = speedY[slot] < 0x80 ? 3 : 2;
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction, x, y, ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    y = entity.y();
                    collisionsTable[slot] |= result.collisionFlag();
                }
            }
        }
        RoomEntity moved = withPosition(entity, x, y);
        return renderVariant ? setVariant(moved) : moved;
    }

    private RoomEntity faceLink(RoomEntity entity, int linkEntityX, int linkEntityY) {
        direction[entity.slot()] = directionToLink(entity.x(), entity.y(),
            linkEntityX, linkEntityY);
        return setVariant(entity);
    }

    private RoomEntity setVariant(RoomEntity entity) {
        int slot = entity.slot();
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        int variant = VARIANT_BY_DIRECTION[direction[slot]] | ((inertia[slot] >>> 4) & 0x01);
        return withPositionAndVariant(entity, entity.x(), entity.y(), variant);
    }

    private void applyVectorTowardsLink(int slot, int entityX, int entityY,
                                        int linkEntityX, int linkEntityY, int length) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smaller, larger);
        int vectorX = swapped ? result : length;
        int vectorY = swapped ? length : result;
        if (distanceX < 0) {
            vectorX = -vectorX;
        }
        if (distanceY < 0) {
            vectorY = -vectorY;
        }
        speedX[slot] = vectorX & 0xFF;
        speedY[slot] = vectorY & 0xFF;
    }

    private static boolean shouldAlert(int entityX, int entityY, int linkEntityX,
                                       int linkEntityY, int currentDirection) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        if (((distanceX + 0x30) & 0xFF) >= 0x60) {
            return false;
        }
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (((distanceY + 0x30) & 0xFF) >= 0x60) {
            return false;
        }
        int linkDirection = directionToLink(entityX, entityY, linkEntityX, linkEntityY);
        return (linkDirection ^ 0x01) != currentDirection;
    }

    private static int directionToLink(int entityX, int entityY, int linkEntityX,
                                       int linkEntityY) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
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

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0) {
            return 0;
        }
        if (largerDistance == 0) {
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }
        return result;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                      int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x, y, entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }
}
