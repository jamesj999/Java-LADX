package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$15 WitchRatEntityHandler movement, hopping, and facing state. */
public final class WitchRatMotion {
    public static final int ENTITY_TYPE = 0xE1;
    public static final int INITIAL_PHYSICS_FLAGS = 0xD2;
    public static final int OPTIONS1 = 0x02;

    // Data_015_78D6.
    static final int[] SPEEDS = {0x02, 0x08, 0x0C, 0x08, 0xFE, 0xF8, 0xF4, 0xF8};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] storedSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            int direction,
            int speedX,
            int speedY,
            int speedZ,
            int collisionFlags,
            boolean appliesBackgroundInteraction) {}

    public void initialize(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundInteraction backgroundInteraction) {
        backgroundInteraction = RoomEntityBackgroundInteraction.orNone(backgroundInteraction);
        return witchRatFrameStep.run(
                entity, frameCounter, transitionCountdown, randomByteSupplier, backgroundInteraction);
    }

    public int state(int slot) {
        validateSlot(slot);
        return state[slot];
    }

    int direction(int slot) {
        validateSlot(slot);
        return direction[slot];
    }

    int speedX(int slot) {
        validateSlot(slot);
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        validateSlot(slot);
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        validateSlot(slot);
        return speedZ[slot] & 0xFF;
    }

    public void clear(int slot) {
        validateSlot(slot);
        reset(slot);
        initialized[slot] = false;
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
        return new PositionAndCollision(x, y, collisionFlags & 0x0F);
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
        storedSpriteVariant[slot] = 0;
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int z, int variant) {
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

    static void checkType(RoomEntity entity) {
        if (entity == null || (entity.type() & 0xFF) != ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Witch Rat entity");
        }
    }

    static void validateSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Witch Rat slot out of range: " + slot);
        }
    }

    static void validateByte(int value, String name) {
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException(name + " must be an unsigned byte: " + value);
        }
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

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record PositionAndCollision(int x, int y, int collisionFlags) {}

    private final WitchRatFrameStep witchRatFrameStep = new WitchRatFrameStep(this);

    static final class WitchRatFrameStep {
        private final WitchRatMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int transitionCountdown;
        private IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int slot;
        private int countdown;
        private int renderedVariant;
        private int z;
        private boolean wrappedZ;
        private int collisionFlags;
        private boolean appliesBackgroundInteraction;

        WitchRatFrameStep(WitchRatMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.WitchRatMotion.Update run() {
            WitchRatMotion.checkType(entity);
            WitchRatMotion.validateByte(frameCounter, "Witch Rat frame counter");
            WitchRatMotion.validateByte(transitionCountdown, "Witch Rat transition countdown");
            Objects.requireNonNull(randomByteSupplier, "Witch Rat random-byte supplier");

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            countdown = transitionCountdown & 0xFF;
            renderedVariant = owner.storedSpriteVariant[slot] + (owner.direction[slot] == 0 ? 0x02 : 0x00);
            z = WitchRatMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
            wrappedZ = (z & 0x80) != 0;
            if (wrappedZ) {
                z = 0;
                owner.speedZ[slot] = 0;
            }

            collisionFlags = 0;
            appliesBackgroundInteraction = false;
            switch (owner.state[slot]) {
                case 0 -> {
                    if (!advanceState0()) return completedResult;
                }
                case 1 -> {
                    if (!advanceState1()) return completedResult;
                }
                default -> throw new IllegalStateException("Invalid Witch Rat state: " + owner.state[slot]);
            }

            // State 0 did not move in X/Y; state 1 already replaced those
            // coordinates with the background-probed values. Both paths still
            // need the top-of-handler Z update and the source-rendered variant.
            entity = WitchRatMotion.withPosition(entity, entity.x(), entity.y(), z, renderedVariant);
            return new WitchRatMotion.Update(
                    entity,
                    owner.state[slot],
                    countdown,
                    owner.direction[slot],
                    owner.speedX[slot] & 0xFF,
                    owner.speedY[slot] & 0xFF,
                    owner.speedZ[slot] & 0xFF,
                    collisionFlags,
                    appliesBackgroundInteraction);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.WitchRatMotion.Update completedResult;

        private boolean advanceState0() {
            if (countdown == 0) {
                int index = randomByteSupplier.getAsInt() & 0x07;
                owner.speedX[slot] = WitchRatMotion.SPEEDS[index];
                owner.direction[slot] = index & 0x04;
                int yIndex = randomByteSupplier.getAsInt() & 0x07;
                owner.speedY[slot] = WitchRatMotion.SPEEDS[yIndex];
                countdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x30;
                owner.state[slot] = 1;
            } else {
                owner.storedSpriteVariant[slot] = 0x01;
                if ((frameCounter & 0x1F) == 0 && (randomByteSupplier.getAsInt() & 0x01) == 0) {
                    owner.direction[slot] ^= 0x04;
                }
            }

            return true;
        }

        private boolean advanceState1() {
            WitchRatMotion.PositionAndCollision moved = owner.moveWithBackground(entity, backgroundInteraction, slot);
            entity = WitchRatMotion.withPosition(entity, moved.x(), moved.y(), z, renderedVariant);
            collisionFlags = moved.collisionFlags();
            appliesBackgroundInteraction = true;
            if (wrappedZ) {
                if (countdown == 0) {
                    countdown = 0x48;
                    owner.state[slot] = 0;
                    // The ROM returns before its final SetEntitySpriteVariant.

                    completedResult = new WitchRatMotion.Update(
                            entity,
                            owner.state[slot],
                            countdown,
                            owner.direction[slot],
                            owner.speedX[slot] & 0xFF,
                            owner.speedY[slot] & 0xFF,
                            owner.speedZ[slot] & 0xFF,
                            collisionFlags,
                            appliesBackgroundInteraction);
                    return false;
                }
                owner.speedZ[slot] = 0x08;
                z = (z + 1) & 0xFF;
                entity = WitchRatMotion.withPosition(entity, moved.x(), moved.y(), z, renderedVariant);
            }
            owner.storedSpriteVariant[slot] = 0x00;

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.WitchRatMotion.Update run(
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int transitionCountdown,
                IntSupplier randomByteSupplier,
                linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction) {
            if (inUse) {
                return new WitchRatFrameStep(owner)
                        .run(entity, frameCounter, transitionCountdown, randomByteSupplier, backgroundInteraction);
            }
            inUse = true;
            slot = 0;
            countdown = 0;
            renderedVariant = 0;
            z = 0;
            wrappedZ = false;
            collisionFlags = 0;
            appliesBackgroundInteraction = false;
            completedResult = null;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.transitionCountdown = transitionCountdown;
            this.randomByteSupplier = randomByteSupplier;
            this.backgroundInteraction = backgroundInteraction;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundInteraction = null;
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
