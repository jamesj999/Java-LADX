package linksawakening.world.entity.motion;

import java.util.Objects;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.RoomEntityObjectQuery;
import linksawakening.world.entity.RoomEntityObjectSample;

/** Bank-$18 ZombieEntityHandler hidden-parent and child state machine. */
public final class ZombieMotion {
    public static final int ENTITY_TYPE = 0xBF;
    public static final int INITIAL_PHYSICS_FLAGS = 0x52;
    public static final int OPTIONS1 = 0x00;
    public static final int PROJECTILE_NOCLIP_FLAG = 0x40;

    // Data_018_637D and Data_018_6385.
    static final int[] SPAWN_Y = {0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x80};
    static final int[] SPAWN_X = {0x18, 0x28, 0x38, 0x48, 0x58, 0x68, 0x78, 0x88};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record SpawnRequest(int x, int y) {}

    public record Update(
            RoomEntity entity,
            int state,
            int privateState1,
            int transitionCountdown,
            int physicsFlags,
            int speedX,
            int speedY,
            SpawnRequest spawnRequest,
            boolean unloadRequested,
            boolean appliesBackgroundInteraction) {}

    public void initialize(int slot) {
        reset(slot);
        initialized[slot] = true;
    }

    /** Seeds the private-state-$01 child created by SpawnNewEntityInRange. */
    public void initializeChild(int slot) {
        reset(slot);
        privateState1[slot] = 1;
        initialized[slot] = true;
    }

    public Update advance(ZombieFrame inputs) {
        return zombieStep.run(inputs);
    }

    boolean isChild(int slot) {
        return initialized[slot] && privateState1[slot] != 0;
    }

    int state(int slot) {
        return state[slot];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    /** State 2 is the only Zombie handler branch that calls enemy damage collision. */
    public boolean allowsEnemyCollision(int slot) {
        return initialized[slot] && privateState1[slot] != 0 && state[slot] == 2;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public void clear(int slot) {
        reset(slot);
        initialized[slot] = false;
    }

    Update update(
            RoomEntity entity,
            int countdown,
            int physicsFlags,
            int nextSpeedX,
            int nextSpeedY,
            SpawnRequest spawnRequest,
            boolean unloadRequested,
            boolean appliesBackgroundInteraction) {
        return new Update(
                entity,
                state[entity.slot()],
                privateState1[entity.slot()],
                countdown & 0xFF,
                physicsFlags & 0xFF,
                nextSpeedX & 0xFF,
                nextSpeedY & 0xFF,
                spawnRequest,
                unloadRequested,
                appliesBackgroundInteraction);
    }

    PositionAndCollision moveWithBackground(
            RoomEntity entity, /* Optional room geometry/history; null preserves the path without that room input. */
            RoomEntityBackgroundInteraction interaction,
            int ignoreHitsCountdown,
            int frameCounter) {
        int slot = entity.slot();
        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        int collisionMask = 0;

        // ApplyEntityInteractionWithBackground probes nonzero speeds even
        // when fixed-point movement has not advanced a complete pixel.
        if (speedX[slot] != 0) {
            int direction =
                    speedX[slot] < 0x80 ? EntityBackgroundCollisionResult.RIGHT : EntityBackgroundCollisionResult.LEFT;
            EntityBackgroundCollisionResult result =
                    interaction.probe(entity, direction, x, y, ignoreHitsCountdown, frameCounter);
            if (result.blocked()) {
                collisionMask |= result.collisionFlag();
                x = entity.x();
            }
        }
        if (speedY[slot] != 0) {
            int direction =
                    speedY[slot] < 0x80 ? EntityBackgroundCollisionResult.DOWN : EntityBackgroundCollisionResult.UP;
            EntityBackgroundCollisionResult result =
                    interaction.probe(entity, direction, x, y, ignoreHitsCountdown, frameCounter);
            if (result.blocked()) {
                collisionMask |= result.collisionFlag();
                y = entity.y();
            }
        }
        return new PositionAndCollision(x, y, collisionMask);
    }

    static boolean isAllowedSpawnPhysics(int physicsFlag) {
        return physicsFlag == 0x00 || physicsFlag == 0x06 || physicsFlag == 0x09;
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static Vector vectorTowardsLink(int entityX, int entityY, int linkEntityX, int linkEntityY, int length) {
        int distanceX = signedByte(linkEntityX - entityX);
        int distanceY = signedByte(linkEntityY - entityY);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsDominant = absoluteX < absoluteY;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
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
        int x = yIsDominant ? result : length;
        int y = yIsDominant ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int variant) {
        return EntitySnapshots.withMotion(
                entity, x & 0xFF, y & 0xFF, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity,
                x & 0xFF,
                y & 0xFF,
                entity.spriteVariant(),
                entity.entityFlipAttribute(),
                entity.z(),
                -1,
                false);
    }

    static RoomEntity withVariant(RoomEntity entity, int variant) {
        return EntitySnapshots.withMotion(
                entity, entity.x(), entity.y(), variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    private void reset(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        selectedVariant[slot] = 0;
    }

    static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Zombie entity");
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

    record Vector(int x, int y) {}

    record PositionAndCollision(int x, int y, int collisionMask) {}

    final ZombieStep zombieStep = new ZombieStep(this);

    public record ZombieFrame(
            RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            int physicsFlags,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            // Optional room geometry/history; null preserves the path without that room input.
            RoomEntityObjectQuery objectQuery,
            // Optional room geometry/history; null preserves the path without that room input.
            RoomEntityBackgroundInteraction backgroundInteraction,
            int ignoreHitsCountdown) {
        public ZombieFrame {
            backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
        }
    }

    static final class ZombieStep {
        private final ZombieMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ZombieMotion.ZombieFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int transitionCountdown;
        private int physicsFlags;
        private int linkEntityX;
        private int linkEntityY;
        private java.util.function.IntSupplier randomByteSupplier;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityObjectQuery objectQuery;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int ignoreHitsCountdown;
        private int slot;
        private int countdown;
        private int nextPhysicsFlags;
        private int nextSpeedX;
        private int nextSpeedY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ZombieMotion.SpawnRequest spawnRequest;
        private boolean unloadRequested;
        private boolean appliesBackgroundInteraction;
        private int renderedVariant;
        private int variant;

        ZombieStep(ZombieMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.ZombieMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceMarkerOrZombie()) return completedResult;

            return owner.update(
                    updated,
                    countdown,
                    nextPhysicsFlags,
                    nextSpeedX,
                    nextSpeedY,
                    null,
                    unloadRequested,
                    appliesBackgroundInteraction);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ZombieMotion.Update completedResult;

        private boolean advanceState0() {
            countdown = 0x30;
            owner.state[slot] = 1;

            return true;
        }

        private boolean advanceState1() {
            int animationSelector = countdown;
            if (countdown == 0) {
                countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x70;
                int vectorLength = (randomByteSupplier.getAsInt() & 0x07) + 0x05;
                ZombieMotion.Vector vector =
                        ZombieMotion.vectorTowardsLink(entity.x(), entity.y(), linkEntityX, linkEntityY, vectorLength);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                nextSpeedX = owner.speedX[slot];
                nextSpeedY = owner.speedY[slot];
                owner.state[slot] = 2;
                // ApplyVectorTowardsLink returns A = X speed, and both
                // ReloadSavedBank and IncrementEntityState preserve A.
                // The fall-through comparison at $6447 reads that byte.
                animationSelector = owner.speedX[slot];
            }
            variant = animationSelector >= 0x18 ? 1 : 2;

            return true;
        }

        private boolean advanceState2() {
            appliesBackgroundInteraction = true;
            ZombieMotion.PositionAndCollision moved =
                    owner.moveWithBackground(entity, backgroundInteraction, ignoreHitsCountdown, frameCounter);
            int x = moved.x();
            int y = moved.y();
            int collisionMask = moved.collisionMask();
            updated = ZombieMotion.withPosition(entity, x, y);
            if ((collisionMask & 0x0F) != 0 || countdown == 0) {
                countdown = 0x30;
                owner.state[slot] = 3;
                nextPhysicsFlags |= ZombieMotion.PROJECTILE_NOCLIP_FLAG;
            }
            variant = 3 + ((frameCounter >>> 4) & 0x01);

            return true;
        }

        private boolean advanceState3() {
            if (countdown == 0) {
                unloadRequested = true;
            } else {
                variant = countdown >= 0x18 ? 2 : 1;
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
            randomByteSupplier = inputs.randomByteSupplier();
            objectQuery = RoomEntityObjectQuery.orNone(inputs.objectQuery());
            backgroundInteraction = inputs.backgroundInteraction();
            ignoreHitsCountdown = inputs.ignoreHitsCountdown();
            ZombieMotion.checkType(entity);
            Objects.requireNonNull(randomByteSupplier, "Zombie random-byte supplier");
            ZombieMotion.validateByte(transitionCountdown, "Zombie transition countdown");
            ZombieMotion.validateByte(physicsFlags, "Zombie physics flags");
            ZombieMotion.validateByte(ignoreHitsCountdown, "Zombie ignore-hits countdown");

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            countdown = transitionCountdown;
            nextPhysicsFlags = physicsFlags;
            nextSpeedX = owner.speedX[slot];
            nextSpeedY = owner.speedY[slot];
            updated = entity;
            spawnRequest = null;
            unloadRequested = false;
            appliesBackgroundInteraction = false;
            return true;
        }

        private boolean advanceMarkerOrZombie() {

            if (owner.privateState1[slot] == 0) {
                // The room-loaded entity is a permanently hidden spawn marker. The
                // handler still relocates it before testing the object physics.
                int variant = -1;
                if (countdown == 0) {
                    int index = randomByteSupplier.getAsInt() & 0x07;
                    int x = ZombieMotion.SPAWN_X[index];
                    int y = ZombieMotion.SPAWN_Y[index];
                    updated = ZombieMotion.withPositionAndVariant(entity, x, y, variant);
                    RoomEntityObjectSample object = objectQuery.sampleOrAbsent(updated);
                    if (object.sampled() && ZombieMotion.isAllowedSpawnPhysics(object.physicsFlag())) {
                        countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x40;
                        spawnRequest = new ZombieMotion.SpawnRequest(x, y);
                    }
                } else {
                    updated = ZombieMotion.withVariant(entity, variant);
                }

                completedResult = owner.update(
                        updated,
                        countdown,
                        nextPhysicsFlags,
                        nextSpeedX,
                        nextSpeedY,
                        spawnRequest,
                        false,
                        appliesBackgroundInteraction);
                return false;
            }

            // $640F renders before any state handler writes the next stored frame.
            renderedVariant = owner.selectedVariant[slot];
            variant = renderedVariant;
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
                default -> throw new IllegalStateException("Invalid Zombie state: " + owner.state[slot]);
            }

            owner.selectedVariant[slot] = variant;
            updated = ZombieMotion.withVariant(updated, renderedVariant);
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.ZombieMotion.Update run(
                linksawakening.world.entity.motion.ZombieMotion.ZombieFrame inputs) {
            if (inUse) {
                return new ZombieStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            transitionCountdown = 0;
            physicsFlags = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            randomByteSupplier = null;
            objectQuery = null;
            backgroundInteraction = null;
            ignoreHitsCountdown = 0;
            slot = 0;
            countdown = 0;
            nextPhysicsFlags = 0;
            nextSpeedX = 0;
            nextSpeedY = 0;
            updated = null;
            spawnRequest = null;
            unloadRequested = false;
            appliesBackgroundInteraction = false;
            renderedVariant = 0;
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
                this.objectQuery = null;
                this.backgroundInteraction = null;
                this.updated = null;
                this.spawnRequest = null;
                this.completedResult = null;
            }
        }
    }
}
