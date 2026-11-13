package linksawakening.world.entity.motion;

import java.util.Arrays;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/**
 * Bank-$04 movement and position history for Mini Moldorm ($29).
 *
 * <p>The ROM gives each loaded Mini Moldorm a 32-byte X/Y history keyed by
 * source load order.  Its head moves from the fixed-point speed tables while
 * the two tail segments render samples nine and sixteen frames behind it.</p>
 */
public final class MiniMoldormMotion {
    private static final int HISTORY_LENGTH = 0x20;
    static final int HISTORY_MASK = HISTORY_LENGTH - 1;
    static final int[] HEAD_SPRITE_VARIANTS = {
        0x03, 0x03, 0x05, 0x05, 0x00, 0x00, 0x04, 0x04,
        0x02, 0x02, 0x06, 0x06, 0x01, 0x01, 0x07, 0x07
    };
    // The ROM's four-byte MoldormYSpeeds label is immediately followed by
    // MoldormXSpeeds. Indexing it with the full 0..$0F angle therefore yields
    // this contiguous sixteen-byte sine table.
    static final int[] Y_SPEEDS = {
        0x00, 0x06, 0x0C, 0x0E, 0x10, 0x0E, 0x0C, 0x06,
        0x00, 0xFA, 0xF4, 0xF2, 0xF0, 0xF2, 0xF4, 0xFA
    };
    static final int[] X_SPEEDS = {
        0x10, 0x0E, 0x0C, 0x06, 0x00, 0xFA, 0xF4, 0xF2,
        0xF0, 0xF2, 0xF4, 0xFA, 0x00, 0x06, 0x0C, 0x0E
    };

    final int[][] historyX = new int[EntityRoomLoader.MAX_ENTITIES][HISTORY_LENGTH];
    final int[][] historyY = new int[EntityRoomLoader.MAX_ENTITIES][HISTORY_LENGTH];
    final int[] historyKey = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] headSpriteVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int headSpriteVariant,
            int collisionFlags,
            int segment1X,
            int segment1Y,
            int segment2X,
            int segment2Y) {}

    public void initialize(RoomEntity entity) {
        int slot = entity.slot();
        int key = historyKeyFor(entity);
        historyKey[slot] = key;
        Arrays.fill(historyX[key], 0);
        Arrays.fill(historyY[key], 0);
        inertia[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        headSpriteVariant[slot] =
                entity.spriteVariant() >= 0 && entity.spriteVariant() < 8 ? entity.spriteVariant() : 0;
        initialized[slot] = true;
    }

    public void clear(int slot) {
        if (slot < 0 || slot >= initialized.length) {
            return;
        }
        if (initialized[slot]) {
            Arrays.fill(historyX[historyKey[slot]], 0);
            Arrays.fill(historyY[historyKey[slot]], 0);
        }
        initialized[slot] = false;
        inertia[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        headSpriteVariant[slot] = 0;
    }

    /** Records the current visual position after the ROM's reset check. */
    public void beginFrame(RoomEntity entity, boolean resetHistory) {
        int slot = entity.slot();
        if (!initialized[slot]) {
            initialize(entity);
        }
        int key = historyKey[slot];
        if (resetHistory) {
            Arrays.fill(historyX[key], 0);
            Arrays.fill(historyY[key], 0);
        }
        inertia[slot] = (inertia[slot] + 1) & HISTORY_MASK;
        int index = inertia[slot];
        historyX[key][index] = entity.x() & 0xFF;
        historyY[key][index] = (entity.y() - entity.z()) & 0xFF;
    }

    public Update advanceAfterHistory(
            RoomEntity entity,
            int transitionCountdown,
            int ignoreHitsCountdown,
            int flashCountdown,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundInteraction
                    backgroundInteraction,
            IntSupplier randomByteSupplier,
            int frameCounter) {
        return miniMoldormFrameStep.run(
                entity,
                transitionCountdown,
                ignoreHitsCountdown,
                flashCountdown,
                backgroundInteraction,
                randomByteSupplier,
                frameCounter);
    }

    int inertia(int slot) {
        return inertia[slot];
    }

    int historyX(int sourceLoadOrder, int index) {
        return historyX[historyKeyFor(sourceLoadOrder)][index & HISTORY_MASK];
    }

    int historyY(int sourceLoadOrder, int index) {
        return historyY[historyKeyFor(sourceLoadOrder)][index & HISTORY_MASK];
    }

    int privateState1(int slot) {
        return privateState1[slot];
    }

    int privateState2(int slot) {
        return privateState2[slot];
    }

    int speedX(int slot) {
        return speedX[slot];
    }

    int speedY(int slot) {
        return speedY[slot];
    }

    private static int historyKeyFor(RoomEntity entity) {
        return historyKeyFor(entity.sourceLoadOrder() < 0 ? entity.slot() : entity.sourceLoadOrder());
    }

    private static int historyKeyFor(int sourceLoadOrder) {
        if (sourceLoadOrder < 0 || sourceLoadOrder >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Mini Moldorm history key out of range: " + sourceLoadOrder);
        }
        return sourceLoadOrder;
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

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    final MiniMoldormFrameStep miniMoldormFrameStep = new MiniMoldormFrameStep(this);

    static final class MiniMoldormFrameStep {
        private final MiniMoldormMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int transitionCountdown;
        private int ignoreHitsCountdown;
        private int flashCountdown;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private int frameCounter;
        private int slot;
        private int nextTransitionCountdown;
        private int x;
        private int y;
        private int collisionFlags;
        private int key;
        private int segment1Index;
        private int segment2Index;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity moved;

        MiniMoldormFrameStep(MiniMoldormMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.MiniMoldormMotion.Update run() {
            if (!prepareTimers()) return completedResult;
            if (!advanceCollision()) return completedResult;
            if (!advanceDirectionAndHistory()) return completedResult;

            return new MiniMoldormMotion.Update(
                    moved,
                    nextTransitionCountdown,
                    owner.headSpriteVariant[slot],
                    collisionFlags,
                    owner.historyX[key][segment1Index],
                    owner.historyY[key][segment1Index],
                    owner.historyX[key][segment2Index],
                    owner.historyY[key][segment2Index]);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.MiniMoldormMotion.Update completedResult;

        private boolean prepareTimers() {

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(entity);
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Mini Moldorm random source cannot be null");
            }
            if (owner.privateCountdown1[slot] > 0) {
                owner.privateCountdown1[slot]--;
            }

            nextTransitionCountdown = transitionCountdown & 0xFF;
            if (nextTransitionCountdown > 0) {
                nextTransitionCountdown--;
            }
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;
            collisionFlags = 0;
            return true;
        }

        private boolean advanceCollision() {

            if ((flashCountdown & 0xFF) == 0) {
                int nextX = MiniMoldormMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
                if (nextX != x && backgroundInteraction != null) {
                    int direction = MiniMoldormMotion.signedByte(owner.speedX[slot]) < 0
                            ? EntityBackgroundCollisionResult.LEFT
                            : EntityBackgroundCollisionResult.RIGHT;
                    EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                            entity, direction, nextX, y, ignoreHitsCountdown & 0xFF, frameCounter);
                    if (result.blocked()) {
                        collisionFlags |= result.collisionFlag();
                    } else {
                        x = nextX;
                    }
                } else {
                    x = nextX;
                }

                int nextY = MiniMoldormMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
                if (nextY != y && backgroundInteraction != null) {
                    int direction = MiniMoldormMotion.signedByte(owner.speedY[slot]) < 0
                            ? EntityBackgroundCollisionResult.UP
                            : EntityBackgroundCollisionResult.DOWN;
                    EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                            entity, direction, x, nextY, ignoreHitsCountdown & 0xFF, frameCounter);
                    if (result.blocked()) {
                        collisionFlags |= result.collisionFlag();
                    } else {
                        y = nextY;
                    }
                } else {
                    y = nextY;
                }
            }

            if (collisionFlags != 0) {
                owner.privateState1[slot] = (collisionFlags & 0x01) != 0
                        ? 0x08
                        : (collisionFlags & 0x02) != 0 ? 0x00 : (collisionFlags & 0x04) != 0 ? 0x04 : 0x0C;
                if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                    owner.privateState2[slot] = -owner.privateState2[slot];
                }
                nextTransitionCountdown = 0x10;
            }
            return true;
        }

        private boolean advanceDirectionAndHistory() {

            if (owner.privateCountdown1[slot] == 0) {
                owner.privateCountdown1[slot] = 0x04;
                owner.privateState1[slot] = (owner.privateState1[slot] + owner.privateState2[slot]) & 0x0F;
                owner.headSpriteVariant[slot] = MiniMoldormMotion.HEAD_SPRITE_VARIANTS[owner.privateState1[slot]];
                owner.speedY[slot] = MiniMoldormMotion.Y_SPEEDS[owner.privateState1[slot]];
                owner.speedX[slot] = MiniMoldormMotion.X_SPEEDS[owner.privateState1[slot]];
            }

            if (nextTransitionCountdown == 0) {
                nextTransitionCountdown = 0x10 + (randomByteSupplier.getAsInt() & 0x1F);
                owner.privateState2[slot] = (randomByteSupplier.getAsInt() & 0x02) - 1;
            }

            key = owner.historyKey[slot];
            segment1Index = (owner.inertia[slot] - 0x09) & MiniMoldormMotion.HISTORY_MASK;
            segment2Index = (owner.inertia[slot] - 0x10) & MiniMoldormMotion.HISTORY_MASK;
            moved = MiniMoldormMotion.withPosition(entity, x, y);
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.MiniMoldormMotion.Update run(
                linksawakening.world.entity.RoomEntity entity,
                int transitionCountdown,
                int ignoreHitsCountdown,
                int flashCountdown,
                /* Optional room geometry/history; null preserves the path without that room input. */ linksawakening
                                .world.entity.RoomEntityBackgroundInteraction
                        backgroundInteraction,
                java.util.function.IntSupplier randomByteSupplier,
                int frameCounter) {
            if (inUse) {
                return new MiniMoldormFrameStep(owner)
                        .run(
                                entity,
                                transitionCountdown,
                                ignoreHitsCountdown,
                                flashCountdown,
                                backgroundInteraction,
                                randomByteSupplier,
                                frameCounter);
            }
            inUse = true;
            slot = 0;
            nextTransitionCountdown = 0;
            x = 0;
            y = 0;
            collisionFlags = 0;
            key = 0;
            segment1Index = 0;
            segment2Index = 0;
            moved = null;
            completedResult = null;

            this.entity = entity;
            this.transitionCountdown = transitionCountdown;
            this.ignoreHitsCountdown = ignoreHitsCountdown;
            this.flashCountdown = flashCountdown;
            this.backgroundInteraction = backgroundInteraction;
            this.randomByteSupplier = randomByteSupplier;
            this.frameCounter = frameCounter;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.backgroundInteraction = null;
                this.randomByteSupplier = null;
                this.moved = null;
                this.completedResult = null;
            }
        }
    }
}
