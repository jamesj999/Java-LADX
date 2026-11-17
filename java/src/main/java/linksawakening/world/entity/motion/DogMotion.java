package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$19 DogEntityHandler state machine and fixed-point movement. */
public final class DogMotion {
    public static final int ENTITY_TYPE = 0x6F;
    public static final int INITIAL_PHYSICS_FLAGS = 0x92;
    static final int CHARGE_PHYSICS_FLAGS = 0x52;
    static final int OPTIONS1 = 0x00;
    public static final int HANDLER_HEALTH = 0x4C;
    static final int DIALOG_LOW_ID = 0x20;

    // Data_019_4990.
    static final int[] SPEEDS = {0x02, 0x08, 0x0C, 0x08, 0xFE, 0xF8, 0xF4, 0xF8};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int direction,
            int speedX,
            int speedY,
            int speedZ,
            int physicsFlags,
            int collisionFlags,
            boolean appliesBackgroundInteraction,
            boolean dialogRequested,
            int dialogLowId) {}

    public void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    public Update advance(DogFrame inputs) {
        return dogStep.run(inputs);
    }

    public int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot] & 0xFF;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    int walkingVariant(int slot, int frameCounter) {
        int animation = (frameCounter >>> 3) & 0x01;
        return direction[slot] == 0 ? animation + 2 : animation;
    }

    int chargeVariant(int slot, int frameCounter) {
        int animation = (frameCounter >>> 2) & 0x01;
        return direction[slot] == 0 ? animation + 2 : animation;
    }

    PositionAndCollision moveWithBackground(RoomEntity entity, RoomEntityBackgroundInteraction interaction, int slot) {
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionFlags = 0;
        if (x != entity.x()) {
            int direction = signedByte(speedX[slot]) < 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, entity.y());
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                x = entity.x() & 0xFF;
            }
        }
        if (y != entity.y()) {
            int direction = signedByte(speedY[slot]) < 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            EntityBackgroundCollisionResult result = interaction.probe(entity, direction, x, y);
            if (result.blocked()) {
                collisionFlags |= result.collisionFlag();
                y = entity.y() & 0xFF;
            }
        }
        return new PositionAndCollision(x, y, collisionFlags);
    }

    boolean canTalk(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkDirection,
            boolean actionButtonAHeld,
            boolean dialogActive) {
        if (dialogActive || !actionButtonAHeld) {
            return false;
        }
        int yWindow = (linkEntityY - entity.y() + 0x14) & 0xFF;
        if (yWindow >= 0x28) {
            return false;
        }
        int xWindow = (linkEntityX - entity.x() + 0x10) & 0xFF;
        if (xWindow >= 0x20) {
            return false;
        }
        return directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY) == (linkDirection ^ 0x01);
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    static int directionToLinkX(int entityX, int linkX) {
        return signedByte(linkX - entityX) < 0 ? 1 : 0;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = romDivide(length, smallerDistance, largerDistance);
        int x = absoluteX < absoluteY ? result : length;
        int y = absoluteX < absoluteY ? length : result;
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
            // GetVectorTowardsLink's divide loop increments once per
            // iteration when both distances are zero.
            return length;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum > 0xFF || sum >= largerDistance) {
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

    void reset(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int variant) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                variant,
                entity.entityFlipAttribute(),
                entity.z(),
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
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

    static RoomEntity withZ(RoomEntity entity, int z) {
        return EntitySnapshots.withMotion(
                entity,
                entity.x(),
                entity.y(),
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                z & 0xFF,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
    }

    static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Dog entity");
        }
    }

    static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record PositionAndCollision(int x, int y, int collisionFlags) {}

    record Vector(int x, int y) {}

    private final DogStep dogStep = new DogStep(this);

    public record DogFrame(
            RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            int physicsFlags,
            int linkEntityX,
            int linkEntityY,
            int linkDirection,
            boolean actionButtonAHeld,
            boolean dialogActive,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction) {
        public DogFrame {
            backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
        }
    }

    static final class DogStep {
        private final DogMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.DogMotion.DogFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int transitionCountdown;
        private int physicsFlags;
        private int linkEntityX;
        private int linkEntityY;
        private int linkDirection;
        private boolean actionButtonAHeld;
        private boolean dialogActive;
        private IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int slot;
        private int countdown;
        private int nextPhysicsFlags;
        private int z;
        private boolean landed;
        private boolean appliesBackgroundInteraction;
        private boolean dialogRequested;
        private int dialogLowId;
        private int movementCollisionFlags;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        DogStep(DogMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.DogMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceBehavior()) return completedResult;

            return new DogMotion.Update(
                    updated,
                    owner.state[slot],
                    countdown,
                    owner.direction[slot] & 0xFF,
                    owner.speedX[slot] & 0xFF,
                    owner.speedY[slot] & 0xFF,
                    owner.speedZ[slot] & 0xFF,
                    nextPhysicsFlags & 0xFF,
                    movementCollisionFlags & 0x0F,
                    appliesBackgroundInteraction,
                    dialogRequested,
                    dialogLowId);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.DogMotion.Update completedResult;

        private boolean advanceState0() {
            if (countdown == 0) {
                int index = randomByteSupplier.getAsInt() & 0x07;
                owner.speedX[slot] = DogMotion.SPEEDS[index];
                owner.direction[slot] = index & 0x04;
                int yIndex = randomByteSupplier.getAsInt() & 0x07;
                owner.speedY[slot] = DogMotion.SPEEDS[yIndex];
                countdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x30;
                owner.state[slot] = 1;
            }
            updated = DogMotion.withVariant(updated, owner.walkingVariant(slot, frameCounter));

            return true;
        }

        private boolean advanceState1() {
            DogMotion.PositionAndCollision moved = owner.moveWithBackground(updated, backgroundInteraction, slot);
            updated = DogMotion.withPositionAndVariant(
                    updated, moved.x(), moved.y(), owner.walkingVariant(slot, frameCounter));
            movementCollisionFlags = moved.collisionFlags();
            appliesBackgroundInteraction = true;
            if (landed) {
                if (countdown != 0) {
                    owner.speedZ[slot] = 0x08;
                    z = (updated.z() + 1) & 0xFF;
                    updated = DogMotion.withZ(updated, z);
                } else {
                    countdown = 0x30;
                    owner.state[slot] = 0;
                }
            }

            return true;
        }

        private boolean advanceState2() {
            if (countdown == 0) {
                owner.state[slot] = 3;
                DogMotion.Vector vector = DogMotion.vectorTowardsLink(
                        updated.x(), updated.y(), updated.z(), linkEntityX, linkEntityY, 0x24);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.speedZ[slot] = 0x18;
                owner.direction[slot] = DogMotion.directionToLinkX(updated.x(), linkEntityX);
            }
            updated = DogMotion.withVariant(updated, owner.chargeVariant(slot, frameCounter));

            return true;
        }

        private boolean advanceState3() {
            DogMotion.PositionAndCollision moved = owner.moveWithBackground(updated, backgroundInteraction, slot);
            updated = DogMotion.withPositionAndVariant(updated, moved.x(), moved.y(), entity.spriteVariant());
            movementCollisionFlags = moved.collisionFlags();
            appliesBackgroundInteraction = true;
            // DogState3 briefly exposes $52 to func_003_6C6B, then restores
            // the harmless shadow flags before testing the collision byte.
            // The bounded Java port does not yet expose that separate
            // Link/entity collision callback, so only the restored value
            // leaves this motion object.
            nextPhysicsFlags = DogMotion.INITIAL_PHYSICS_FLAGS;
            if (landed) {
                owner.state[slot] = 0;
                countdown = 0x20;
            }

            return true;
        }

        private boolean prepareFrame() {

            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            transitionCountdown = inputs.transitionCountdown();
            physicsFlags = inputs.physicsFlags();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkDirection = inputs.linkDirection();
            actionButtonAHeld = inputs.actionButtonAHeld();
            dialogActive = inputs.dialogActive();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundInteraction = inputs.backgroundInteraction();
            DogMotion.checkType(entity);
            DogMotion.validateByte(frameCounter, "Dog frame counter");
            DogMotion.validateByte(transitionCountdown, "Dog transition countdown");
            DogMotion.validateByte(physicsFlags, "Dog physics flags");
            DogMotion.validateByte(linkEntityX, "Dog Link X");
            DogMotion.validateByte(linkEntityY, "Dog Link Y");
            if (linkDirection < 0 || linkDirection > 3) {
                throw new IllegalArgumentException("Dog Link direction must be 0..3: " + linkDirection);
            }
            Objects.requireNonNull(randomByteSupplier, "Dog random-byte supplier");

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            countdown = transitionCountdown & 0xFF;
            nextPhysicsFlags = physicsFlags & 0xFF;
            z = DogMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                owner.speedZ[slot] = 0;
            }

            // $19:490A preserves Z underflow in hMultiPurposeG. Background
            // collision flags are a different byte; states 1/3 test this landing
            // result at $19:49E1 and $19:4A41.
            appliesBackgroundInteraction = false;
            dialogRequested = false;
            dialogLowId = -1;
            movementCollisionFlags = 0;
            updated = DogMotion.withZ(entity, z);
            return true;
        }

        private boolean advanceBehavior() {

            // func_019_7CF0 runs before the state dispatch and only for states 0/1.
            if (owner.state[slot] < 2
                    && owner.canTalk(
                            updated, linkEntityX, linkEntityY, linkDirection, actionButtonAHeld, dialogActive)) {
                dialogRequested = true;
                dialogLowId = DogMotion.DIALOG_LOW_ID;
            }

            switch (owner.state[slot]) {
                case 0 -> {
                    if (!advanceState0()) return false;
                }
                case 1 -> {
                    if (!advanceState1()) return false;
                }
                case 2 -> {
                    if (!advanceState2()) return false;
                }
                case 3 -> {
                    if (!advanceState3()) return false;
                }
                default -> throw new IllegalStateException("Invalid Dog state: " + owner.state[slot]);
            }
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.DogMotion.Update run(
                linksawakening.world.entity.motion.DogMotion.DogFrame inputs) {
            if (inUse) {
                return new DogStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            transitionCountdown = 0;
            physicsFlags = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            linkDirection = 0;
            actionButtonAHeld = false;
            dialogActive = false;
            randomByteSupplier = null;
            backgroundInteraction = null;
            slot = 0;
            countdown = 0;
            nextPhysicsFlags = 0;
            z = 0;
            landed = false;
            appliesBackgroundInteraction = false;
            dialogRequested = false;
            dialogLowId = 0;
            movementCollisionFlags = 0;
            updated = null;
            completedResult = null;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundInteraction = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }

    int[] state() {
        return this.state;
    }

    int[] direction() {
        return this.direction;
    }

    int[] speedX() {
        return this.speedX;
    }

    int[] speedY() {
        return this.speedY;
    }

    int[] speedZ() {
        return this.speedZ;
    }

    boolean[] initialized() {
        return this.initialized;
    }
}
