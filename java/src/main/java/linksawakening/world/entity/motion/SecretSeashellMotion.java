package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityObjectSample;

/**
 * Bank-$03's hidden secret-seashell state and reveal physics.
 *
 * <p>The entity handler keeps the shell's hidden/revealed state separate from
 * its normal entity status.  In particular, a shell can remain an ACTIVE
 * entity while its sprite is suppressed, and the two tree shells use the
 * Pegasus-Boots collision branch instead of the object-under-entity branch.</p>
 */
public final class SecretSeashellMotion {
    private boolean secretSeashellScreenShakeActive;

    public boolean readSecretSeashellScreenShakeActive() {
        return secretSeashellScreenShakeActive;
    }

    public boolean writeSecretSeashellScreenShakeActive(boolean value) {
        secretSeashellScreenShakeActive = value;
        return secretSeashellScreenShakeActive;
    }

    private boolean secretSeashellPegasusCollisionActive;

    public boolean readSecretSeashellPegasusCollisionActive() {
        return secretSeashellPegasusCollisionActive;
    }

    public boolean writeSecretSeashellPegasusCollisionActive(boolean value) {
        secretSeashellPegasusCollisionActive = value;
        return secretSeashellPegasusCollisionActive;
    }

    private boolean secretSeashellPegasusCollisionCoordinatesKnown;

    public boolean readSecretSeashellPegasusCollisionCoordinatesKnown() {
        return secretSeashellPegasusCollisionCoordinatesKnown;
    }

    public boolean writeSecretSeashellPegasusCollisionCoordinatesKnown(boolean value) {
        secretSeashellPegasusCollisionCoordinatesKnown = value;
        return secretSeashellPegasusCollisionCoordinatesKnown;
    }

    private int secretSeashellPegasusCollisionX;

    public int readSecretSeashellPegasusCollisionX() {
        return secretSeashellPegasusCollisionX;
    }

    public int writeSecretSeashellPegasusCollisionX(int value) {
        secretSeashellPegasusCollisionX = value;
        return secretSeashellPegasusCollisionX;
    }

    private int secretSeashellPegasusCollisionY;

    public int readSecretSeashellPegasusCollisionY() {
        return secretSeashellPegasusCollisionY;
    }

    public int writeSecretSeashellPegasusCollisionY(int value) {
        secretSeashellPegasusCollisionY = value;
        return secretSeashellPegasusCollisionY;
    }

    static final int ENTITY_TYPE = 0x3D;

    private static final int NORMAL_HIDDEN_STATE = 0x02;
    static final int TREE_HIDDEN_STATE = 0x01;
    private static final int REVEAL_COUNTDOWN = 0x18;
    private static final int REVEAL_SLOW_COUNTDOWN = 0x80;
    private static final int REVEAL_VECTOR_LENGTH = 0x0C;
    private static final int REVEAL_SPEED_Z = 0x20;
    private static final int OBJECT_SHORT_GRASS = 0x04;
    private static final int OBJECT_SHOVEL_HOLE = 0xCC;

    private static final int[] SPECIAL_HIDDEN_ROOMS = {0xDA, 0xA5, 0x74, 0x3A, 0xA8, 0xB2};

    private final int[] roomId = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState3Table.
    final int[] hiddenMode = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateState4Table.
    private final int[] shovelRevealPending = new int[EntityRoomLoader.MAX_ENTITIES];
    // ROM wEntitiesPrivateCountdown1Table.
    final int[] revealCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] slowTransitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    void initialize(int slot, int roomId) {
        validateSlot(slot);
        this.roomId[slot] = roomId & 0xFF;
        hiddenMode[slot] = isTreeSeashellRoom(roomId) ? TREE_HIDDEN_STATE : NORMAL_HIDDEN_STATE;
        shovelRevealPending[slot] = 0;
        revealCountdown[slot] = 0;
        slowTransitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public void ensureInitialized(int slot, int roomId) {
        validateSlot(slot);
        int normalizedRoomId = roomId < 0 ? 0xFF : roomId & 0xFF;
        if (!initialized[slot] || this.roomId[slot] != normalizedRoomId) {
            initialize(slot, normalizedRoomId);
        }
    }

    /**
     * Advances the exact hidden/reveal branch and the common top-down
     * bouncing physics used after a shell has become visible.
     */
    public Update advance(SecretSeashellFrame inputs) {
        return SecretSeashellMotion.advanceBody(this, inputs);
    }

    /**
     * Advances the handler with the actual WRAM Pegasus collision position.
     * The ROM compares the shell centre against each byte using unsigned
     * wraparound, accepting a half-open $20-byte window on both axes.
     */
    public Update advance(SecretSeashellWorldFrame inputs) {
        return SecretSeashellMotion.advanceBody(this, inputs);
    }

    public Update advance(SecretSeashellInteractionFrame inputs) {
        return SecretSeashellMotion.advanceBody(this, inputs);
    }

    Update advanceInternal(SecretSeashellInteractionFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        boolean pegasusCollisionCoordinatesKnown = inputs.pegasusCollisionCoordinatesKnown();
        int pegasusCollisionX = inputs.pegasusCollisionX();
        int pegasusCollisionY = inputs.pegasusCollisionY();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = RoomEntityObjectSample.orAbsent(inputs.objectUnderEntity());
        if (entity == null) {
            throw new IllegalArgumentException("Secret seashell entity cannot be null");
        }
        int slot = entity.slot();
        this.ensureInitialized(slot, roomId);
        if (this.revealCountdown[slot] > 0) {
            this.revealCountdown[slot]--;
        }
        if ((frameCounter & 0x03) == 0 && this.slowTransitionCountdown[slot] > 0) {
            this.slowTransitionCountdown[slot]--;
        }

        if (this.hiddenMode[slot] != 0) {
            if (roomTransitionActive) {
                return new SecretSeashellMotion.Update(SecretSeashellMotion.withVariant(entity, -1), true);
            }

            boolean reveal = this.hiddenMode[slot] == TREE_HIDDEN_STATE
                    ? screenShakeActive
                            && pegasusCollisionActive
                            && (!pegasusCollisionCoordinatesKnown
                                    || SecretSeashellMotion.withinPegasusCollisionWindow(
                                            entity.x() + 0x08, entity.y() + 0x08, pegasusCollisionX, pegasusCollisionY))
                    : this.shouldRevealFromObject(roomId, objectUnderEntity, slot);
            if (reveal) {
                this.revealAwayFromLink(slot, entity.x(), entity.y(), linkEntityX, linkEntityY);
                return new SecretSeashellMotion.Update(
                        SecretSeashellMotion.withVariant(
                                entity, entity.spriteDefinition().supported() ? 0 : -1),
                        false);
            }
            return new SecretSeashellMotion.Update(SecretSeashellMotion.withVariant(entity, -1), true);
        }

        int x = SecretSeashellMotion.addSpeedToPosition(entity.x(), this.speedX[slot], this.speedXAccumulator, slot);
        int y = SecretSeashellMotion.addSpeedToPosition(entity.y(), this.speedY[slot], this.speedYAccumulator, slot);
        int z = SecretSeashellMotion.addSpeedToPosition(entity.z(), this.speedZ[slot], this.speedZAccumulator, slot);
        this.speedZ[slot] = (this.speedZ[slot] - 0x02) & 0xFF;
        return new SecretSeashellMotion.Update(
                SecretSeashellMotion.withPositionAndVariant(entity, x, y, z, entity.spriteVariant()), false);
    }

    static boolean withinPegasusCollisionWindow(int entityCenterX, int entityCenterY, int collisionX, int collisionY) {
        return unsignedWindowContains(entityCenterX, collisionX) && unsignedWindowContains(entityCenterY, collisionY);
    }

    private static boolean unsignedWindowContains(int entityCoordinate, int collisionCoordinate) {
        int difference = ((entityCoordinate & 0xFF) - (collisionCoordinate & 0xFF) + 0x10) & 0xFF;
        return difference < 0x20;
    }

    boolean shouldRevealFromObject(int roomId, RoomEntityObjectSample object, int slot) {
        if (!object.sampled()) {
            return false;
        }
        int objectId = object.objectId() & 0xFF;
        if (isSpecialHiddenRoom(roomId)) {
            // The ROM deliberately records that these rooms contain a shell
            // which is not hidden under short grass, but still only reveals
            // it through the shovel-hole path.
            shovelRevealPending[slot] = 1;
            return objectId == OBJECT_SHOVEL_HOLE;
        }
        return objectId == OBJECT_SHORT_GRASS || objectId == OBJECT_SHOVEL_HOLE;
    }

    void revealAwayFromLink(int slot, int entityX, int entityY, int linkEntityX, int linkEntityY) {
        hiddenMode[slot] = 0;
        shovelRevealPending[slot] = 0;
        revealCountdown[slot] = REVEAL_COUNTDOWN;
        slowTransitionCountdown[slot] = REVEAL_SLOW_COUNTDOWN;
        Vector towardLink = vectorTowardsLink(entityX, entityY, linkEntityX, linkEntityY, REVEAL_VECTOR_LENGTH);
        speedX[slot] = -towardLink.x() & 0xFF;
        speedY[slot] = -towardLink.y() & 0xFF;
        speedZ[slot] = REVEAL_SPEED_Z;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    public int hiddenMode(int slot) {
        validateSlot(slot);
        return hiddenMode[slot];
    }

    public int shovelRevealPending(int slot) {
        validateSlot(slot);
        return shovelRevealPending[slot];
    }

    public int revealCountdown(int slot) {
        validateSlot(slot);
        return revealCountdown[slot];
    }

    public int slowTransitionCountdown(int slot) {
        validateSlot(slot);
        return slowTransitionCountdown[slot];
    }

    public int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot];
    }

    public int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot];
    }

    public int speedZ(int slot) {
        validateSlot(slot);
        return speedZ[slot];
    }

    public void clear(int slot) {
        validateSlot(slot);
        roomId[slot] = 0;
        hiddenMode[slot] = 0;
        shovelRevealPending[slot] = 0;
        revealCountdown[slot] = 0;
        slowTransitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    private static boolean isTreeSeashellRoom(int roomId) {
        return (roomId & 0xFF) == 0xA4 || (roomId & 0xFF) == 0xD2;
    }

    private static boolean isSpecialHiddenRoom(int roomId) {
        int normalized = roomId & 0xFF;
        for (int specialRoom : SPECIAL_HIDDEN_ROOMS) {
            if (specialRoom == normalized) {
                return true;
            }
        }
        return false;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smallerDistance, largerDistance);
        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int romDivide(int length, int smallerDistance, int largerDistance) {
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
            remainder = sum & 0xFF;
        }
        return result;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static RoomEntity withVariant(RoomEntity entity, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                entity.x(),
                entity.y(),
                variant,
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                variant,
                entity.entityFlipAttribute(),
                z & 0xFF,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    private static int signedByte(int value) {
        int normalized = value & 0xFF;
        return normalized < 0x80 ? normalized : normalized - 0x100;
    }

    private static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
    }

    private record Vector(int x, int y) {}

    public record Update(RoomEntity entity, boolean hidden) {}

    static SecretSeashellMotion.Update advanceBody(SecretSeashellMotion owner, SecretSeashellFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = RoomEntityObjectSample.orAbsent(inputs.objectUnderEntity());
        return owner.advanceInternal(new SecretSeashellInteractionFrame(
                entity,
                roomId,
                frameCounter,
                roomTransitionActive,
                screenShakeActive,
                pegasusCollisionActive,
                false,
                entity.x() + 0x08,
                entity.y() + 0x08,
                linkEntityX,
                linkEntityY,
                objectUnderEntity));
    }

    static SecretSeashellMotion.Update advanceBody(SecretSeashellMotion owner, SecretSeashellWorldFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        int pegasusCollisionX = inputs.pegasusCollisionX();
        int pegasusCollisionY = inputs.pegasusCollisionY();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = RoomEntityObjectSample.orAbsent(inputs.objectUnderEntity());
        return owner.advanceInternal(new SecretSeashellInteractionFrame(
                entity,
                roomId,
                frameCounter,
                roomTransitionActive,
                screenShakeActive,
                pegasusCollisionActive,
                true,
                pegasusCollisionX,
                pegasusCollisionY,
                linkEntityX,
                linkEntityY,
                objectUnderEntity));
    }

    static SecretSeashellMotion.Update advanceBody(SecretSeashellMotion owner, SecretSeashellInteractionFrame inputs) {
        RoomEntity entity = inputs.entity();
        int roomId = inputs.roomId();
        int frameCounter = inputs.frameCounter();
        boolean roomTransitionActive = inputs.roomTransitionActive();
        boolean screenShakeActive = inputs.screenShakeActive();
        boolean pegasusCollisionActive = inputs.pegasusCollisionActive();
        boolean pegasusCollisionCoordinatesKnown = inputs.pegasusCollisionCoordinatesKnown();
        int pegasusCollisionX = inputs.pegasusCollisionX();
        int pegasusCollisionY = inputs.pegasusCollisionY();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        RoomEntityObjectSample objectUnderEntity = RoomEntityObjectSample.orAbsent(inputs.objectUnderEntity());
        return owner.advanceInternal(new SecretSeashellInteractionFrame(
                entity,
                roomId,
                frameCounter,
                roomTransitionActive,
                screenShakeActive,
                pegasusCollisionActive,
                pegasusCollisionCoordinatesKnown,
                pegasusCollisionX,
                pegasusCollisionY,
                linkEntityX,
                linkEntityY,
                objectUnderEntity));
    }

    record SecretSeashellFrame(
            RoomEntity entity,
            int roomId,
            int frameCounter,
            boolean roomTransitionActive,
            boolean screenShakeActive,
            boolean pegasusCollisionActive,
            int linkEntityX,
            int linkEntityY,
            RoomEntityObjectSample objectUnderEntity) {}

    public record SecretSeashellInteractionFrame(
            RoomEntity entity,
            int roomId,
            int frameCounter,
            boolean roomTransitionActive,
            boolean screenShakeActive,
            boolean pegasusCollisionActive,
            boolean pegasusCollisionCoordinatesKnown,
            int pegasusCollisionX,
            int pegasusCollisionY,
            int linkEntityX,
            int linkEntityY,
            RoomEntityObjectSample objectUnderEntity) {}

    record SecretSeashellWorldFrame(
            RoomEntity entity,
            int roomId,
            int frameCounter,
            boolean roomTransitionActive,
            boolean screenShakeActive,
            boolean pegasusCollisionActive,
            int pegasusCollisionX,
            int pegasusCollisionY,
            int linkEntityX,
            int linkEntityY,
            RoomEntityObjectSample objectUnderEntity) {}
}
