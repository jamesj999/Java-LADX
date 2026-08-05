package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$03 state shared by Octorok and the ordinary Moblin roaming handler. */
final class RoamingEnemyMotion {
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_IRON_MASK = 0x24;
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;
    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] collisionsTable = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] horizontallyCollidedObject = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] verticallyCollidedObject = new int[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        collisionsTable[slot] = 0;
        horizontallyCollidedObject[slot] = 0;
        verticallyCollidedObject[slot] = 0;
        initialized[slot] = true;
    }

    RoomEntity advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                       IntSupplier randomByteSupplier,
                       RoomEntityBackgroundCollision backgroundCollision) {
        return advance(entity, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundCollision, false).entity();
    }

    Update advance(RoomEntity entity, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundCollision backgroundCollision,
                   boolean creditsGameplay) {
        RoomEntityBackgroundInteraction interaction = backgroundCollision == null
            ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision);
        return advanceInternal(entity, linkEntityX, linkEntityY, randomByteSupplier,
            interaction, creditsGameplay);
    }

    Update advanceWithInteraction(RoomEntity entity, int linkEntityX, int linkEntityY,
                                   IntSupplier randomByteSupplier,
                                   RoomEntityBackgroundInteraction backgroundInteraction,
                                   boolean creditsGameplay) {
        return advanceInternal(entity, linkEntityX, linkEntityY, randomByteSupplier,
            backgroundInteraction, creditsGameplay);
    }

    private Update advanceInternal(RoomEntity entity, int linkEntityX, int linkEntityY,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   boolean creditsGameplay) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }

        if (state[slot] != 0) {
            LaunchRequest launchRequest = launchRequestIfEligible(
                entity, linkEntityX, linkEntityY, creditsGameplay);
            if (launchRequest != null) {
                return new Update(entity, launchRequest);
            }
            if (transitionCountdown[slot] == 0) {
                transitionCountdown[slot] = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                state[slot] = 0;
                privateState1[slot] = (privateState1[slot] + 1) & 0x03;
                direction[slot] = privateState1[slot] == 0
                    ? directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
                    : randomByteSupplier.getAsInt() & 0x03;
                speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
                speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
            }
            return new Update(entity, null);
        }

        int x = entity.x();
        int y = entity.y();
        if ((collisionsTable[slot] & 0x0F) != 0 || transitionCountdown[slot] == 0) {
            transitionCountdown[slot] = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
            state[slot] = 1;
            speedX[slot] = 0;
            speedY[slot] = 0;
            collisionsTable[slot] = 0;
        } else {
            int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
            if (nextX != x && backgroundInteraction != null) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction[slot], nextX, y);
                if (result.blocked()) {
                    collisionsTable[slot] |= result.collisionFlag();
                    horizontallyCollidedObject[slot] = result.objectId();
                } else {
                    x = nextX;
                }
            } else {
                x = nextX;
            }

            int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
            if (nextY != y && backgroundInteraction != null) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, direction[slot], x, nextY);
                if (result.blocked()) {
                    collisionsTable[slot] |= result.collisionFlag();
                    verticallyCollidedObject[slot] = result.objectId();
                } else {
                    y = nextY;
                }
            } else {
                y = nextY;
            }
        }

        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        int variant = VARIANT_BY_DIRECTION[direction[slot]] | ((inertia[slot] >>> 3) & 0x01);
        return new Update(withPositionAndVariant(entity, x, y, variant), null);
    }

    private LaunchRequest launchRequestIfEligible(RoomEntity entity, int linkEntityX,
                                                   int linkEntityY, boolean creditsGameplay) {
        int type = entity.type() & 0xFF;
        if (transitionCountdown[entity.slot()] != 0x0A
            || privateState1[entity.slot()] != 0
            || type == ENTITY_IRON_MASK) {
            return null;
        }

        int linkDirection = directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY);
        if (linkDirection != direction[entity.slot()]) {
            return null;
        }

        if (type == ENTITY_OCTOROK) {
            if (creditsGameplay) {
                return null;
            }
            return new LaunchRequest(entity.slot(), type, ENTITY_OCTOROK_ROCK);
        }
        if (type == ENTITY_MOBLIN) {
            return new LaunchRequest(entity.slot(), type, ENTITY_MOBLIN_ARROW);
        }
        return null;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    /** Mirrors AnimateRoamingEnemy forcing state 1 while the enemy recoils. */
    void beginRecoil(int slot) {
        state[slot] = 1;
        transitionCountdown[slot] = 0x40;
        initialized[slot] = true;
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

    int collisionFlags(int slot) {
        return collisionsTable[slot];
    }

    int horizontallyCollidedObject(int slot) {
        return horizontallyCollidedObject[slot];
    }

    int verticallyCollidedObject(int slot) {
        return verticallyCollidedObject[slot];
    }

    void setStateForTest(int slot, int newState, int newTransitionCountdown,
                         int newInertia, int newPrivateState1, int newDirection) {
        state[slot] = newState & 0xFF;
        transitionCountdown[slot] = newTransitionCountdown & 0xFF;
        inertia[slot] = newInertia & 0xFF;
        privateState1[slot] = newPrivateState1 & 0x03;
        direction[slot] = newDirection & 0x03;
        speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
        speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        collisionsTable[slot] = 0;
        horizontallyCollidedObject[slot] = 0;
        verticallyCollidedObject[slot] = 0;
        initialized[slot] = true;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y,
                                                       int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), variant, entity.entityFlipAttribute(),
            entity.spriteTileOffset(), entity.z());
    }

    record Update(RoomEntity entity, LaunchRequest launchRequest) {
    }

    record LaunchRequest(int sourceSlot, int sourceType, int projectileType) {
    }
}
