package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 Goomba random walk, side-view patrol and stomp states. */
public final class GoombaMotion {
    private static final int ENTITY_GOOMBA = 0x9F;
    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** $03:6CFF–6D19, using hLinkSpeedY in side-view and hLinkVelocityZ overhead. */
    public static boolean canStomp(boolean sideView, boolean airborne, int verticalSpeed, boolean bounceActive) {
        boolean descending = sideView ? (byte) verticalSpeed >= 0 : (byte) verticalSpeed < 0;
        return airborne && (descending || bounceActive);
    }

    public void initialize(int slot) {
        state[slot] = 0;
        inertia[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown,
            boolean sideScrolling) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        if ((entity.type() & 0xFF) != ENTITY_GOOMBA) {
            throw new IllegalArgumentException(
                    "Unsupported Goomba motion entity type: 0x" + Integer.toHexString(entity.type()));
        }
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(slot);
        }

        int countdown = transitionCountdown & 0xFF;
        int x = entity.x() & 0xFF;
        int y = entity.y() & 0xFF;
        int variant;
        boolean startDying = false;
        if (sideScrolling) {
            SideUpdate sideUpdate = advanceSideScrolling(entity, frameCounter, linkEntityX, x, y, backgroundCollision);
            x = sideUpdate.x();
            y = sideUpdate.y();
            variant = sideUpdate.variant();
            if (state[slot] == 2 && countdown == 0) {
                startDying = true;
            }
        } else {
            switch (state[slot]) {
                case 0 -> {
                    if (countdown == 0) {
                        countdown = 0x30 + (randomByteSupplier.getAsInt() & 0x3F);
                        inertia[slot] = (inertia[slot] + 1) & 0xFF;
                        if (inertia[slot] == 4) {
                            inertia[slot] = 0;
                            direction[slot] = directionToLink(x, y, linkEntityX, linkEntityY);
                        } else {
                            direction[slot] = randomByteSupplier.getAsInt() & 0x03;
                        }
                        speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
                        speedY[slot] = SPEED_Y_BY_DIRECTION[direction[slot]];
                        state[slot] = 1;
                    }
                    variant = animationVariant(frameCounter);
                }
                case 1 -> {
                    if (countdown == 0) {
                        countdown = 0x20;
                        // $07:6656 increments the state, then immediately writes zero to the same slot.
                        state[slot] = 0;
                    }
                    Position position = move(entity, x, y, backgroundCollision, false);
                    x = position.x();
                    y = position.y();
                    variant = animationVariant(frameCounter);
                }
                case 2 -> {
                    variant = 2;
                    if (countdown == 0) {
                        // func_007_666B writes the heart/drop byte and hands
                        // the entity to the shared EntityDeathHandler. The
                        // runtime owns those WRAM fields, so report the
                        // transition rather than mutating the entity here.
                        startDying = true;
                    }
                }
                default -> throw new IllegalStateException("Invalid Goomba state: " + state[slot]);
            }
        }

        RoomEntity updated =
                EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
        return new Update(updated, countdown, startDying);
    }

    /** $07:7DEE uses the patrol fractions and restores ordinary speed before returning from the handler. */
    public EnemyRecoilMotion.Update applyRecoil(
            RoomEntity entity, RoomEntityBackgroundCollision collision, EnemyRecoilMotion recoil) {
        return recoil.advanceWithHandlerFractions(entity, collision, speedXAccumulator, speedYAccumulator, false);
    }

    public void enterStomp(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        state[slot] = 2;
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    private SideUpdate advanceSideScrolling(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int x,
            int y,
            RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (state[slot] == 0) {
            direction[slot] = signedByte(linkEntityX - x) < 0 ? 1 : 0;
            speedX[slot] = SPEED_X_BY_DIRECTION[direction[slot]];
            speedY[slot] = 0;
            state[slot] = 1;
            return new SideUpdate(x, y, entity.spriteVariant());
        }
        if (state[slot] == 1) {
            Position position = move(entity, x, y, backgroundCollision, true);
            return new SideUpdate(position.x(), position.y(), position.onFloor() ? animationVariant(frameCounter) : 0);
        }
        return new SideUpdate(x, y, 2);
    }

    private Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision collision, boolean sideView) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        // Only the side-view handler adds gravity before the bank-$03 wall probes.
        if (sideView) speedY[slot] = (speedY[slot] + 2) & 0xFF;
        if (speedX[slot] != 0 && collision.blocks(entity, directionForX(speedX[slot]), nextX, nextY)) {
            nextX = x;
            if (sideView) speedX[slot] = negateByte(speedX[slot]);
        }
        boolean blockedY = speedY[slot] != 0 && collision.blocks(entity, directionForY(speedY[slot]), nextX, nextY);
        if (blockedY) nextY = y;
        boolean onFloor = sideView && blockedY && (speedY[slot] & 0x80) == 0;
        if (onFloor) {
            // $07:66DD snaps the restored Y pixel. Ceiling collisions skip this branch.
            nextY = (nextY & 0xF0) + 3;
            speedY[slot] = 0;
        }
        return new Position(nextX, nextY, onFloor);
    }

    private static int animationVariant(int frameCounter) {
        return (frameCounter >>> 4) & 0x01;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int negateByte(int value) {
        return -signedByte(value) & 0xFF;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    private record Position(int x, int y, boolean onFloor) {}

    private record SideUpdate(int x, int y, int variant) {}

    public record Update(RoomEntity entity, int transitionCountdown, boolean startDying) {}
}
