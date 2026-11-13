package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$15 StalfosEvasiveEntityHandler's normal and airborne states. */
public final class StalfosEvasiveMotion {
    private static final int[] RANDOM_DIRECTION_SPEEDS = {0x00, 0x06, 0xFA, 0xFA, 0x06};

    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            boolean unloadRequested,
            boolean swordPokeRequested,
            CloneRequest cloneRequest) {}

    public record CloneRequest(int sourceSlot, int x, int y, int z, int speedX, int speedY) {}

    public void initialize(int slot) {
        reset(slot);
        // EntityInitHandlersTable.$1E points to IncrementEntityState.
        state[slot] = 1;
        initialized[slot] = true;
    }

    void initializeForActiveEntity(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    void reset(int slot) {
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        state[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    public Update advance(StalfosEvasiveFrame inputs) {
        return stalfosEvasiveFrameStep.run(this, inputs);
    }

    Update advanceAirborne(
            RoomEntity entity,
            int transitionCountdown,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundCollision
                    backgroundCollision,
            CloneRequest cloneRequest) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        MoveResult movement = applyBackground(entity, x, y, backgroundCollision);
        x = movement.x();
        y = movement.y();

        int z = addSpeedToPosition(entity.z(), speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = (speedZ[slot] - 1) & 0xFF;
        if ((z & 0x80) != 0) {
            z = 0;
            speedZ[slot] = 0;
            speedX[slot] = 0x08;
            speedY[slot] = 0x08;
            inertia[slot] = 0;
            privateCountdown1[slot] = 0x10;
        }
        return new Update(
                withPositionAndVariant(entity, x, y, z, 0x02), transitionCountdown, false, false, cloneRequest);
    }

    Update advanceFleeing(
            RoomEntity entity,
            int frame,
            int transitionCountdown,
            int ignoreHitsCountdown,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundCollision
                    backgroundCollision) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        MoveResult movement = applyBackground(entity, x, y, backgroundCollision);
        RoomEntity updated =
                withPositionAndVariant(entity, movement.x(), movement.y(), entity.z(), (frame >>> 3) & 0x01);
        if (movement.blocked() || ignoreHitsCountdown != 0) {
            return new Update(updated, transitionCountdown, true, true, null);
        }
        if (movement.x() >= 0xA8 || ((movement.y() - updated.z()) & 0xFF) >= 0x84) {
            return new Update(updated, transitionCountdown, true, false, null);
        }
        return new Update(updated, transitionCountdown, false, false, null);
    }

    public void clear(int slot) {
        privateState1[slot] = 0;
        privateCountdown1[slot] = 0;
        state[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    public int privateState1(int slot) {
        return privateState1[slot];
    }

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    int state(int slot) {
        return state[slot];
    }

    public int inertia(int slot) {
        return inertia[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    public void initializeClone(int slot, int newSpeedX, int newSpeedY) {
        reset(slot);
        initialized[slot] = true;
        privateState1[slot] = 1;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    void chooseRandomDirection(int slot, IntSupplier randomByteSupplier) {
        speedY[slot] = 0;
        speedX[slot] = RANDOM_DIRECTION_SPEEDS[randomByteSupplier.getAsInt() & 0x03];
        if (speedX[slot] == 0) {
            speedY[slot] = RANDOM_DIRECTION_SPEEDS[3 + (randomByteSupplier.getAsInt() & 0x01)];
        }
        // The disassembly writes zero to the entity state after choosing.
        state[slot] = 0;
    }

    static MoveResult applyBackground(
            RoomEntity entity,
            int x,
            int y, /* Optional room geometry/history; null preserves the path without that room input. */
            RoomEntityBackgroundCollision backgroundCollision) {
        boolean xBlocked = false;
        boolean yBlocked = false;
        if (backgroundCollision != null) {
            if (x != entity.x() && backgroundCollision.blocks(entity, directionForX(x - entity.x()), x, entity.y())) {
                x = entity.x();
                xBlocked = true;
            }
            if (y != entity.y() && backgroundCollision.blocks(entity, directionForY(y - entity.y()), x, y)) {
                y = entity.y();
                yBlocked = true;
            }
        }
        return new MoveResult(x, y, xBlocked, yBlocked);
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum >= larger) {
                sum -= larger;
                result++;
            }
            remainder = sum & 0xFF;
        }
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    static boolean distanceInJumpWindow(int distance) {
        return distance >= -0x24 && distance < 0x24;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
    }

    private static int directionForX(int delta) {
        return delta < 0 ? 1 : 0;
    }

    private static int directionForY(int delta) {
        return delta < 0 ? 2 : 3;
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record MoveResult(int x, int y, boolean xBlocked, boolean yBlocked) {
        boolean blocked() {
            return xBlocked || yBlocked;
        }
    }

    record Vector(int x, int y) {}

    final StalfosEvasiveFrameStep stalfosEvasiveFrameStep = new StalfosEvasiveFrameStep(this);

    public record StalfosEvasiveFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean actionButtonsHeld,
            int transitionCountdown,
            int ignoreHitsCountdown,
            java.util.function.IntSupplier randomByteSupplier,
            // Optional room geometry/history; null preserves the path without that room input.
            RoomEntityBackgroundCollision backgroundCollision,
            int mapId) {}

    static final class StalfosEvasiveFrameStep {
        private final StalfosEvasiveMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.StalfosEvasiveMotion.StalfosEvasiveFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean actionButtonsHeld;
        private int transitionCountdown;
        private int ignoreHitsCountdown;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int mapId;
        private int slot;
        private boolean cloneBranchReady;
        private int frame;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.StalfosEvasiveMotion.CloneRequest cloneRequest;
        private int x;
        private int y;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.StalfosEvasiveMotion.MoveResult movement;
        private int distanceX;
        private int distanceY;
        private int variant;

        StalfosEvasiveFrameStep(StalfosEvasiveMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.StalfosEvasiveMotion.Update run() {
            if (!prepareCloneBranch()) return completedResult;
            if (!advanceMovement()) return completedResult;

            return new StalfosEvasiveMotion.Update(
                    StalfosEvasiveMotion.withPositionAndVariant(entity, x, y, entity.z(), variant),
                    transitionCountdown,
                    false,
                    false,
                    cloneRequest);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.StalfosEvasiveMotion.Update completedResult;

        private boolean prepareCloneBranch() {

            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            actionButtonsHeld = inputs.actionButtonsHeld();
            transitionCountdown = inputs.transitionCountdown();
            ignoreHitsCountdown = inputs.ignoreHitsCountdown();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();
            mapId = inputs.mapId();
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                // A Gibdo burn conversion calls ConfigureNewEntity.attributes and
                // returns ACTIVE without running EntityInitHandlersTable.$1E.
                owner.initializeForActiveEntity(slot);
            }
            cloneBranchReady = false;
            if (owner.privateCountdown1[slot] > 0) {
                owner.privateCountdown1[slot]--;
                cloneBranchReady = owner.privateCountdown1[slot] == 1;
            }

            frame = frameCounter & 0xFF;
            if (owner.privateState1[slot] != 0) {
                {
                    completedResult = owner.advanceFleeing(
                            entity, frame, transitionCountdown, ignoreHitsCountdown, backgroundCollision);
                    return false;
                }
            }

            cloneRequest = null;
            if (cloneBranchReady) {
                // The source returns immediately when hMapId is below
                // MAP_ANGLERS_TUNNEL, before its inertia dispatch or movement.
                if (mapId < 0x03) {
                    int variant = (frame >>> 3) & 0x01;
                    {
                        completedResult = new StalfosEvasiveMotion.Update(
                                StalfosEvasiveMotion.withPositionAndVariant(
                                        entity, entity.x(), entity.y(), entity.z(), variant),
                                transitionCountdown,
                                false,
                                false,
                                null);
                        return false;
                    }
                }
                StalfosEvasiveMotion.Vector vector = StalfosEvasiveMotion.vectorTowardsLink(
                        entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x18);
                cloneRequest = new StalfosEvasiveMotion.CloneRequest(
                        slot, entity.x(), entity.y(), entity.z(), vector.x(), vector.y());
            }
            return true;
        }

        private boolean advanceMovement() {

            if (owner.inertia[slot] != 0) {
                {
                    completedResult =
                            owner.advanceAirborne(entity, transitionCountdown, backgroundCollision, cloneRequest);
                    return false;
                }
            }

            x = StalfosEvasiveMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
            y = StalfosEvasiveMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
            movement = StalfosEvasiveMotion.applyBackground(entity, x, y, backgroundCollision);
            x = movement.x();
            y = movement.y();
            if (movement.xBlocked()) {
                owner.speedX[slot] ^= 0xF0;
            }
            if (movement.yBlocked()) {
                owner.speedY[slot] ^= 0xF0;
            }

            distanceX = StalfosEvasiveMotion.signedByte(linkEntityX - entity.x());
            distanceY = StalfosEvasiveMotion.signedByte(linkEntityY - entity.y());
            if (actionButtonsHeld
                    && StalfosEvasiveMotion.distanceInJumpWindow(distanceX)
                    && StalfosEvasiveMotion.distanceInJumpWindow(distanceY)) {
                transitionCountdown = 0x08;
                owner.privateCountdown1[slot] = 0;
                owner.inertia[slot] = 1;
                owner.speedZ[slot] = 0x15;
                StalfosEvasiveMotion.Vector vector = StalfosEvasiveMotion.vectorTowardsLink(
                        entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x12);
                owner.speedX[slot] = (-vector.x()) & 0xFF;
                owner.speedY[slot] = (-vector.y()) & 0xFF;
                {
                    completedResult = new StalfosEvasiveMotion.Update(
                            StalfosEvasiveMotion.withPositionAndVariant(entity, x, y, entity.z(), 0x02),
                            transitionCountdown,
                            false,
                            false,
                            cloneRequest);
                    return false;
                }
            }

            // jr_015_4FCE: the initialization state or random low six bits forces
            // a new choice; otherwise the current fixed-point speed is retained.
            if (owner.state[slot] != 0 || (randomByteSupplier.getAsInt() & 0x2F) == 0) {
                owner.chooseRandomDirection(slot, randomByteSupplier);
            }
            variant = (frame >>> 3) & 0x01;
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.StalfosEvasiveMotion.Update run(
                StalfosEvasiveMotion owner,
                linksawakening.world.entity.motion.StalfosEvasiveMotion.StalfosEvasiveFrame inputs) {
            if (inUse) {
                return new StalfosEvasiveFrameStep(owner).run(owner, inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            actionButtonsHeld = false;
            transitionCountdown = 0;
            ignoreHitsCountdown = 0;
            randomByteSupplier = null;
            backgroundCollision = null;
            mapId = 0;
            slot = 0;
            cloneBranchReady = false;
            frame = 0;
            cloneRequest = null;
            x = 0;
            y = 0;
            movement = null;
            distanceX = 0;
            distanceY = 0;
            variant = 0;
            completedResult = null;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.cloneRequest = null;
                this.movement = null;
                this.completedResult = null;
            }
        }
    }
}
