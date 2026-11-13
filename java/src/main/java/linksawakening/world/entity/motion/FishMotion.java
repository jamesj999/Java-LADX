package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$15 FishEntityHandler's hidden swim and jump loop. */
public final class FishMotion {
    public static final int ENTITY_TYPE = 0xCC;

    public static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10 | 0x40;
    static final int JUMP_PHYSICS_FLAGS = 0x02 | 0x10;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $CC uses EntityInitNoop; the handler seeds its own state. */
    public void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        physicsFlags[slot] = INITIAL_PHYSICS_FLAGS;
        initialized[slot] = true;
    }

    /**
     * Advances the handler after the shared entity loop has decremented the
     * transition byte for this frame.
     */
    public Update advance(
            RoomEntity entity,
            int transitionCountdown,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        return fishFrameStep.run(entity, transitionCountdown, randomByteSupplier, backgroundCollision);
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

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    public boolean allowsEnemyCollision(int slot) {
        return state[slot] == 2;
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

    static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? EntityBackgroundCollisionResult.LEFT : EntityBackgroundCollisionResult.RIGHT;
    }

    static int negate(int speed) {
        return (-signedByte(speed)) & 0xFF;
    }

    static int arithmeticShiftRight(int speed) {
        return (signedByte(speed) >> 1) & 0xFF;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(RoomEntity entity, int transitionCountdown, int physicsFlags, boolean waterSplash) {}

    final FishFrameStep fishFrameStep = new FishFrameStep(this);

    static final class FishFrameStep {
        private final FishMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int transitionCountdown;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int slot;
        private int countdown;
        private int x;
        private boolean horizontalCollision;
        private int z;
        private int variant;
        private boolean waterSplash;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        FishFrameStep(FishMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.FishMotion.Update run() {
            if ((entity.type() & 0xFF) != FishMotion.ENTITY_TYPE) {
                throw new IllegalArgumentException(
                        "Unsupported Fish motion entity type: 0x" + Integer.toHexString(entity.type()));
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Fish random-byte supplier cannot be null");
            }

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            countdown = transitionCountdown & 0xFF;
            x = FishMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
            horizontalCollision = false;
            if (owner.speedX[slot] != 0
                    && backgroundCollision.blocks(
                            entity, FishMotion.horizontalDirection(owner.speedX[slot]), x, entity.y())) {
                x = entity.x() & 0xFF;
                horizontalCollision = true;
            }

            z = entity.z() & 0xFF;
            variant = entity.spriteVariant();
            waterSplash = false;
            switch (owner.state[slot]) {
                case 0 -> {
                    if (!advanceState0()) return completedResult;
                }
                case 1 -> {
                    if (!advanceState1()) return completedResult;
                }
                case 2 -> {
                    if (!advanceState2()) return completedResult;
                }
                default -> throw new IllegalStateException("Invalid Fish state: " + owner.state[slot]);
            }

            updated = EntitySnapshots.withMotion(
                    entity,
                    x,
                    entity.y(),
                    variant,
                    entity.entityFlipAttribute(),
                    z,
                    entity.deathSpriteVariant(),
                    entity.powerRecoilDeath());
            return new FishMotion.Update(updated, countdown, owner.physicsFlags[slot], waterSplash);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.FishMotion.Update completedResult;

        private boolean advanceState0() {
            owner.speedX[slot] = 0x08;
            countdown = 0x40 + (randomByteSupplier.getAsInt() & 0x7F);
            owner.state[slot] = 1;

            return true;
        }

        private boolean advanceState1() {
            if (countdown != 0) {
                if (horizontalCollision) {
                    owner.speedX[slot] = FishMotion.negate(owner.speedX[slot]);
                }
                variant = owner.speedX[slot] < 0x80 ? 0x06 : 0x05;
            } else {
                owner.physicsFlags[slot] = FishMotion.JUMP_PHYSICS_FLAGS;
                owner.speedZ[slot] = 0x18;
                owner.state[slot] = 2;
                owner.speedX[slot] = (owner.speedX[slot] << 1) & 0xFF;
                waterSplash = true;
            }

            return true;
        }

        private boolean advanceState2() {
            z = FishMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                waterSplash = true;
                countdown = 0x50 + (randomByteSupplier.getAsInt() & 0x7F);
                owner.speedX[slot] = FishMotion.arithmeticShiftRight(owner.speedX[slot]);
                owner.physicsFlags[slot] = FishMotion.INITIAL_PHYSICS_FLAGS;
                owner.state[slot] = 1;
            } else {
                variant = FishMotion.signedByte(owner.speedX[slot]) < 0 ? 0x01 : 0x03;
                if (FishMotion.signedByte(owner.speedZ[slot]) < 0) {
                    variant++;
                }
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.FishMotion.Update run(
                linksawakening.world.entity.RoomEntity entity,
                int transitionCountdown,
                java.util.function.IntSupplier randomByteSupplier,
                linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {
            if (inUse) {
                return new FishFrameStep(owner)
                        .run(entity, transitionCountdown, randomByteSupplier, backgroundCollision);
            }
            inUse = true;
            slot = 0;
            countdown = 0;
            x = 0;
            horizontalCollision = false;
            z = 0;
            variant = 0;
            waterSplash = false;
            updated = null;
            completedResult = null;

            this.entity = entity;
            this.transitionCountdown = transitionCountdown;
            this.randomByteSupplier = randomByteSupplier;
            this.backgroundCollision = backgroundCollision;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
