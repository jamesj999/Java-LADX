package linksawakening.world;

import java.util.function.IntSupplier;

/** Bank-$06 {@code HinoxEntityHandler} state machine for entity {@code $89}. */
final class HinoxMotion {
    static final int ENTITY_TYPE = 0x89;
    static final int ENTITY_BOMB = 0x02;

    static final int STATE_INITIAL = 0;
    static final int STATE_WANDER = 1;
    static final int STATE_CHARGE_WINDUP = 2;
    static final int STATE_CHARGE = 3;
    static final int STATE_GRAB = 4;
    static final int STATE_BOMB = 5;

    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] GRAB_LINK_X_OFFSETS = {
        0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF, 0xEF,
        0xF3, 0xF7, 0xFB, 0x00
    };
    private static final int[] GRAB_LINK_Z = {
        0x15, 0x15, 0x15, 0x15, 0x15, 0x14, 0x14, 0x14,
        0x10, 0x08, 0x04, 0x00
    };
    private static final int[] GRAB_BODY_VARIANT = {
        0, 0, 0, 0, 1, 1, 1, 1, 0, 0
    };

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateState3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] entityCollisionFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombOriginX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombOriginY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] bombOriginWritten = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    record BombSpawn(int type, int x, int y, int z,
                     int speedX, int speedY, int speedZ, int transitionCountdown) {
    }

    record Update(RoomEntity entity, int state, int transitionCountdown,
                  int spriteVariant, boolean linkMotionBlocked,
                  int heldLinkX, int heldLinkY, int heldLinkZ,
                  int linkSpeedX, int linkSpeedY, int linkVelocityZ,
                  int linkAirborneState, int linkDamage, int jingleId, int waveId,
                  boolean dustRequested, boolean heldLinkPositionWritten,
                  boolean applyHeldLinkPose,
                  BombSpawn bombSpawn) {
    }

    void initialize(int slot) {
        validateSlot(slot);
        state[slot] = STATE_INITIAL;
        privateState1[slot] = 0;
        privateState3[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        inertia[slot] = 0;
        privateCountdown1[slot] = 0;
        entityCollisionFlags[slot] = 0;
        bombOriginX[slot] = 0;
        bombOriginY[slot] = 0;
        bombOriginWritten[slot] = false;
        initialized[slot] = true;
    }

    Update advance(RoomEntity entity, int transitionCountdown, int frameCounter,
                   int linkX, int linkY, int linkZ, int linkMotionState,
                   IntSupplier randomByteSupplier,
                   RoomEntityBackgroundInteraction backgroundInteraction,
                   int entityCollisionFlagsForFrame, int flashCountdown) {
        validateSlot(entity.slot());
        if (!initialized[entity.slot()]) {
            initialize(entity.slot());
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Hinox random source cannot be null");
        }

        int slot = entity.slot();
        boolean linkMotionBlockedAtEntry = privateCountdown1[slot] != 0;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int transition = transitionCountdown & 0xFF;
        int frame = frameCounter & 0xFF;
        int linkSpeedX = 0;
        int linkSpeedY = 0;
        int linkVelocityZ = 0;
        int airborne = 0;
        int linkDamage = 0;
        int jingle = -1;
        int wave = -1;
        boolean dust = false;
        BombSpawn bomb = null;
        int spriteVariant = entity.spriteVariant() < 0 ? 0 : entity.spriteVariant();

        // HinoxEntityHandler performs this branch before selecting a private
        // state. A sword flash at exactly $03 is the ROM's bomb transition.
        if (state[slot] < STATE_GRAB && (flashCountdown & 0xFF) == 0x03) {
            state[slot] = STATE_BOMB;
            transition = 0x20;
            return update(entity, x, y, transition, spriteVariant, linkMotionBlockedAtEntry,
                linkX, linkY, linkZ, linkSpeedX, linkSpeedY, linkVelocityZ,
                airborne, linkDamage, jingle, wave, dust, false, false, bomb);
        }

        switch (state[slot]) {
            case STATE_INITIAL -> {
                if (((entityCollisionFlags[slot] | entityCollisionFlagsForFrame) & 0x0F) != 0
                    || transition == 0) {
                    transition = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
                    state[slot] = STATE_WANDER;
                    speedX[slot] = 0;
                    speedY[slot] = 0;
                }
                Position next = move(entity, x, y, backgroundInteraction, slot);
                x = next.x();
                y = next.y();
                spriteVariant = tickInertia(slot);
            }
            case STATE_WANDER -> {
                if (transition == 0) {
                    privateState3[slot] = (privateState3[slot] + 1) & 0xFF;
                    if (privateState3[slot] == 2) {
                        privateState3[slot] = 0;
                        if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                            state[slot] = STATE_CHARGE_WINDUP;
                            transition = 0x30;
                            speedX[slot] = 0;
                            speedY[slot] = 0;
                            break;
                        }
                    }
                    transition = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                    state[slot] = STATE_WANDER;
                    privateState1[slot] = (privateState1[slot] + 1) & 0x03;
                    int direction = privateState1[slot] == 0
                        ? directionToLink(x, y, linkX, linkY)
                        : randomByteSupplier.getAsInt() & 0x03;
                    speedX[slot] = SPEED_X_BY_DIRECTION[direction];
                    speedY[slot] = SPEED_Y_BY_DIRECTION[direction];
                }
                spriteVariant = entity.spriteVariant();
            }
            case STATE_CHARGE_WINDUP -> {
                if (transition == 0) {
                    transition = 0x20;
                    state[slot] = STATE_CHARGE;
                    Vector vector = vectorTowardsLink(x, y, entity.z(), linkX, linkY, 0x18);
                    speedX[slot] = vector.x();
                    speedY[slot] = vector.y();
                }
                tickInertia(slot);
                spriteVariant = tickInertia(slot);
                if ((frame & 0x0F) == 0) {
                    jingle = 0x20;
                }
            }
            case STATE_CHARGE -> {
                if (transition == 0) {
                    state[slot] = STATE_GRAB;
                    transition = 0x4F;
                    wave = 0x16;
                }
                Position next = move(entity, x, y, backgroundInteraction, slot);
                x = next.x();
                y = next.y();
                if (state[slot] == STATE_CHARGE
                    && withinDistance(x, linkX, 0x18)
                    && withinDistance(y, linkY, 0x18)
                    && (linkMotionState & 0xFF) == 0) {
                    state[slot] = STATE_GRAB;
                    transition = 0x4F;
                    jingle = 0x16;
                }
                dust = (frame & 0x07) == 0;
                if (dust) {
                    // State 3 writes hMultiPurpose0/1 for the later bomb
                    // handler. Preserve those source registers per entity.
                    bombOriginX[slot] = x & 0xFF;
                    bombOriginY[slot] = (y + 0x0A) & 0xFF;
                    bombOriginWritten[slot] = true;
                }
                tickInertia(slot);
                spriteVariant = tickInertia(slot);
                if ((frame & 0x0F) == 0) {
                    jingle = 0x20;
                }
            }
            case STATE_GRAB -> {
                if (transition == 0) {
                    state[slot] = STATE_INITIAL;
                    transition = 0;
                    return update(entity, x, y, transition, spriteVariant,
                        linkMotionBlockedAtEntry, linkX, linkY, linkZ,
                        linkSpeedX, linkSpeedY, linkVelocityZ, airborne, linkDamage,
                        jingle, wave, dust, false, false, bomb);
                } else if (transition == 0x20) {
                    linkSpeedY = 0x20;
                    linkSpeedX = (linkX & 0xFF) >= 0x50 ? 0xE0 : 0x20;
                    linkVelocityZ = 0x10;
                    airborne = 0x02;
                    linkDamage = 0x08;
                    jingle = 0x08;
                    linkX = x;
                    linkY = y;
                    privateCountdown1[slot] = 0x50;
                    return update(entity, x, y, transition, entity.spriteVariant(),
                        linkMotionBlockedAtEntry, linkX, linkY, linkZ,
                        linkSpeedX, linkSpeedY, linkVelocityZ, airborne, linkDamage,
                        jingle, wave, dust, true, false, bomb);
                }
                boolean blocked = linkMotionBlockedAtEntry;
                int heldX = linkX;
                int heldY = linkY;
                int heldZ = linkZ;
                boolean heldPositionWritten = false;
                boolean applyHeldLinkPose = false;
                if (transition >= 0x20 && transition <= 0x4F) {
                    int index = (transition - 0x20) >>> 2;
                    index = Math.min(index, GRAB_LINK_X_OFFSETS.length - 1);
                    heldX = (x + GRAB_LINK_X_OFFSETS[index]) & 0xFF;
                    heldY = y;
                    heldZ = GRAB_LINK_Z[index];
                    heldPositionWritten = true;
                    airborne = 0x02;
                    applyHeldLinkPose = true;
                }
                spriteVariant = GRAB_BODY_VARIANT[Math.min(transition >>> 3,
                    GRAB_BODY_VARIANT.length - 1)];
                return update(entity, x, y, transition, spriteVariant, blocked,
                    heldX, heldY, heldZ, linkSpeedX, linkSpeedY, linkVelocityZ,
                    airborne, linkDamage, jingle, wave, dust, heldPositionWritten,
                    applyHeldLinkPose, bomb);
            }
            case STATE_BOMB -> {
                if (transition == 0) {
                    state[slot] = STATE_INITIAL;
                } else if (transition == 0x10) {
                    int originX = bombOriginWritten[slot] ? bombOriginX[slot] : x;
                    int originY = bombOriginWritten[slot] ? bombOriginY[slot] : y;
                    Vector vector = vectorTowardsLink((originX - 0x0C) & 0xFF, originY,
                        0x10, linkX, linkY, 0x10);
                    bomb = new BombSpawn(ENTITY_BOMB, (originX - 0x0C) & 0xFF, originY, 0x10,
                        vector.x(), vector.y(), 0x18, 0x40);
                    jingle = 0x08;
                }
                spriteVariant = transition >= 0x10 ? 1 : 0;
            }
            default -> {
                state[slot] = STATE_INITIAL;
                speedX[slot] = 0;
                speedY[slot] = 0;
                spriteVariant = 0;
            }
        }
        return update(entity, x, y, transition, spriteVariant,
            linkMotionBlockedAtEntry,
            linkX, linkY, 0, linkSpeedX, linkSpeedY, linkVelocityZ,
            airborne, linkDamage, jingle, wave, dust, false, false, bomb);
    }

    private Update update(RoomEntity entity, int x, int y, int transition,
                          int spriteVariant, boolean blocked, int heldX, int heldY,
                          int heldZ, int linkSpeedX, int linkSpeedY, int linkVelocityZ,
                          int airborne, int linkDamage, int jingle, int wave, boolean dust,
                          boolean heldLinkPositionWritten,
                          boolean applyHeldLinkPose,
                          BombSpawn bomb) {
        RoomEntity updated = new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(),
            x & 0xFF, y & 0xFF, entity.status(), entity.spriteDefinition(),
            entity.spriteDefinition().supported()
                ? Math.min(spriteVariant, entity.spriteDefinition().variantCount() - 1)
                : -1, entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
        return new Update(updated, state[entity.slot()], transition & 0xFF, spriteVariant,
            blocked, heldX & 0xFF, heldY & 0xFF, heldZ & 0xFF,
            linkSpeedX & 0xFF, linkSpeedY & 0xFF, linkVelocityZ & 0xFF,
            airborne & 0xFF, linkDamage & 0xFF, jingle, wave, dust,
            heldLinkPositionWritten, applyHeldLinkPose, bomb);
    }

    private Position move(RoomEntity entity, int x, int y,
                           RoomEntityBackgroundInteraction background, int slot) {
        entityCollisionFlags[slot] = 0;
        int nextX = addSpeed(x, speedX[slot], speedXAccumulator, slot);
        if (nextX != x && background != null) {
            EntityBackgroundCollisionResult result = background.probe(
                entity, horizontalDirection(speedX[slot]), nextX, y);
            if (result.blocked()) {
                entityCollisionFlags[slot] |= result.collisionFlag();
                nextX = x;
            }
        }
        int nextY = addSpeed(y, speedY[slot], speedYAccumulator, slot);
        if (nextY != y && background != null) {
            EntityBackgroundCollisionResult result = background.probe(
                entity, verticalDirection(speedY[slot]), nextX, nextY);
            if (result.blocked()) {
                entityCollisionFlags[slot] |= result.collisionFlag();
                nextY = y;
            }
        }
        return new Position(nextX, nextY);
    }

    private int tickInertia(int slot) {
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        return (inertia[slot] >>> 4) & 0x01;
    }

    void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int state(int slot) { validateSlot(slot); return state[slot]; }
    int speedX(int slot) { validateSlot(slot); return speedX[slot] & 0xFF; }
    int speedY(int slot) { validateSlot(slot); return speedY[slot] & 0xFF; }
    int inertia(int slot) { validateSlot(slot); return inertia[slot] & 0xFF; }
    int privateCountdown1(int slot) {
        validateSlot(slot);
        return privateCountdown1[slot] & 0xFF;
    }

    void decrementPrivateCountdown1(int slot) {
        validateSlot(slot);
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
    }
    int collisionFlags(int slot) {
        validateSlot(slot);
        return entityCollisionFlags[slot] & 0xFF;
    }

    void setStateForTest(int slot, int value, int privateState1Value, int privateState3Value) {
        validateSlot(slot);
        if (value < 0 || value > STATE_BOMB) throw new IllegalArgumentException("Invalid Hinox state");
        state[slot] = value;
        privateState1[slot] = privateState1Value & 0x03;
        privateState3[slot] = privateState3Value & 0xFF;
        initialized[slot] = true;
    }

    void setSpeedForTest(int slot, int x, int y) {
        validateSlot(slot);
        speedX[slot] = x & 0xFF;
        speedY[slot] = y & 0xFF;
    }

    void setInertiaForTest(int slot, int value) {
        validateSlot(slot);
        inertia[slot] = value & 0xFF;
    }

    void setEntityCollisionForTest(int slot, int value) {
        validateSlot(slot);
        entityCollisionFlags[slot] = value & 0xFF;
    }

    void setPrivateState3ForTest(int slot, int value) {
        validateSlot(slot);
        privateState3[slot] = value & 0xFF;
    }

    void setPrivateCountdown1ForTest(int slot, int value) {
        validateSlot(slot);
        privateCountdown1[slot] = value & 0xFF;
    }

    private static Vector vectorTowardsLink(int x, int y, int entityZ,
                                            int linkX, int linkY, int length) {
        int dx = signedByte(linkX - x);
        int dy = signedByte(linkY - y + entityZ);
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int major = Math.max(ax, ay);
        int minor = Math.min(ax, ay);
        int small = major == 0 ? length : divide(minor, major, length);
        int vx = ax >= ay ? length : small;
        int vy = ay > ax ? length : small;
        if (dx < 0) vx = -vx;
        if (dy < 0) vy = -vy;
        return new Vector(vx & 0xFF, vy & 0xFF);
    }

    private static int divide(int minor, int major, int length) {
        int result = 0;
        int remainder = 0;
        for (int i = 0; i < length; i++) {
            int sum = remainder + minor;
            if (sum >= major) { sum -= major; result++; }
            remainder = sum;
        }
        return result;
    }

    private static boolean withinDistance(int value, int target, int distance) {
        int delta = (value - target) & 0xFF;
        int absolute = delta < 0x80 ? delta : 0x100 - delta;
        return absolute < distance;
    }

    private static int directionToLink(int x, int y, int linkX, int linkY) {
        int dx = signedByte(linkX - x);
        int dy = signedByte(linkY - y);
        if (Math.abs(dy) >= Math.abs(dx)) {
            return dy < 0 ? 2 : 3;
        }
        return dx < 0 ? 1 : 0;
    }

    private static int addSpeed(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) return position & 0xFF;
        int sum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = sum & 0xFF;
        int delta = signedByte(speed) >> 4;
        if (sum > 0xFF) delta++;
        return (position + delta) & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0
            ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    private static int verticalDirection(int speed) {
        return (speed & 0x80) != 0
            ? EntityBackgroundCollisionResult.UP : EntityBackgroundCollisionResult.DOWN;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Hinox slot out of range: " + slot);
        }
    }

    private record Vector(int x, int y) { }
    private record Position(int x, int y) { }
}
