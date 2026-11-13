package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 SnakeEntityHandler states 0, 1, and 2. */
public final class SnakeMotion {
    static final int ENTITY_SNAKE = 0xA1;

    private static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    private static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] SPRITE_DIRECTION_OFFSET = {0x02, 0x00, -1, -1};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        privateCountdown1[slot] = 0x30;
        animationVariant[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundCollision
                    backgroundCollision,
            int transitionCountdown) {
        return snakeFrameStep.run(
                this,
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                transitionCountdown);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    public int speedX(int slot) {
        return speedX[slot];
    }

    public int speedY(int slot) {
        return speedY[slot];
    }

    int startDashIfNeeded(int slot, int entityX, int entityY, int linkEntityX, int linkEntityY, int countdown) {
        if (privateCountdown1[slot] != 0) {
            return countdown;
        }

        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        if (withinDashWindow(distanceX)) {
            dash(slot, directionForY(linkEntityY - entityY));
            return 0x30;
        }

        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (withinDashWindow(distanceY)) {
            dash(slot, directionForX(linkEntityX - entityX));
            return 0x30;
        }
        return countdown;
    }

    private void dash(int slot, int directionIndex) {
        setSpeed(slot, directionIndex);
        speedX[slot] = (speedX[slot] << 1) & 0xFF;
        speedY[slot] = (speedY[slot] << 1) & 0xFF;
        state[slot] = 2;
        // SnakeStartDashIfNeeded overwrites the state-1 $18 countdown with
        // the state-2 dash window.
    }

    Position move(
            RoomEntity entity,
            int x,
            int y, /* Optional room geometry/history; null preserves the path without that room input. */
            RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (backgroundCollision != null
                && nextX != x
                && backgroundCollision.blocks(entity, directionForX(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (backgroundCollision != null
                && nextY != y
                && backgroundCollision.blocks(entity, directionForY(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    void setSpeed(int slot, int directionIndex) {
        int index = directionIndex & 0x03;
        speedX[slot] = SPEED_X_BY_DIRECTION[index];
        speedY[slot] = SPEED_Y_BY_DIRECTION[index];
        int spriteDirection = SPRITE_DIRECTION_OFFSET[index];
        if (spriteDirection >= 0) {
            direction[slot] = spriteDirection;
        }
    }

    void clearSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    void setAnimationVariant(int slot, int variant) {
        animationVariant[slot] = variant & 0x01;
    }

    private static boolean withinDashWindow(int distance) {
        return ((distance + 0x08) & 0xFF) < 0x10;
    }

    private static int directionForX(int distance) {
        return signedByte(distance) < 0 ? 1 : 0;
    }

    private static int directionForY(int distance) {
        return signedByte(distance) < 0 ? 2 : 3;
    }

    static int crawlingAnimation(int frameCounter) {
        return (frameCounter >>> 3) & 0x01;
    }

    static int dashingAnimation(int frameCounter) {
        return (frameCounter >>> 2) & 0x01;
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    record Position(int x, int y, boolean collided) {}

    public record Update(RoomEntity entity, int transitionCountdown) {}

    final SnakeFrameStep snakeFrameStep = new SnakeFrameStep(this);

    static final class SnakeFrameStep {
        private final SnakeMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int transitionCountdown;
        private int slot;
        private int countdown;
        private int x;
        private int y;
        private int renderedVariant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.SnakeMotion.Position moved;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        SnakeFrameStep(SnakeMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.SnakeMotion.Update run() {
            if ((entity.type() & 0xFF) != SnakeMotion.ENTITY_SNAKE) {
                throw new IllegalArgumentException(
                        "Unsupported Snake motion entity type: 0x" + Integer.toHexString(entity.type()));
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Snake random-byte supplier cannot be null");
            }

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }
            if (owner.privateCountdown1[slot] > 0) {
                owner.privateCountdown1[slot]--;
            }

            countdown = transitionCountdown & 0xFF;
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;
            // RenderActiveEntitySpritesPair runs before movement and state
            // dispatch. The handler adds the persistent direction offset to the
            // previously selected base animation variant, then writes the next
            // base variant only after the current frame has been drawn.
            renderedVariant = (owner.animationVariant[slot] + owner.direction[slot]) & 0xFF;

            // SnakeEntityHandler updates its position and applies background
            // interaction before dispatching the state table. A collision then
            // resets the state, transition byte, and private countdown; state 0
            // clears the speed in the same handler pass.
            moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            if (moved.collided()) {
                owner.state[slot] = 0;
                countdown = 0x08;
                owner.privateCountdown1[slot] = 0x20;
            }

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
                default -> throw new IllegalStateException("Invalid Snake state: " + owner.state[slot]);
            }

            updated = EntitySnapshots.withMotion(
                    entity, x, y, renderedVariant, entity.entityFlipAttribute(), entity.z(), -1, false);
            return new SnakeMotion.Update(updated, countdown);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.SnakeMotion.Update completedResult;

        private boolean advanceState0() {
            if (countdown == 0) {
                owner.state[slot] = 1;
                int random = randomByteSupplier.getAsInt() & 0xFF;
                countdown = 0x30 + (random & 0x1F);
                owner.setSpeed(slot, random & 0x03);
                // The ROM returns immediately from this branch, leaving
                // the previous base animation variant in place.
            } else {
                owner.clearSpeed(slot);
                countdown = owner.startDashIfNeeded(slot, x, y, linkEntityX, linkEntityY, countdown);
                owner.setAnimationVariant(slot, SnakeMotion.crawlingAnimation(frameCounter));
            }

            return true;
        }

        private boolean advanceState1() {
            if (countdown == 0) {
                countdown = 0x18;
                owner.state[slot] = 2;
            }
            countdown = owner.startDashIfNeeded(slot, x, y, linkEntityX, linkEntityY, countdown);
            owner.setAnimationVariant(slot, SnakeMotion.crawlingAnimation(frameCounter));

            return true;
        }

        private boolean advanceState2() {
            if (countdown == 0) {
                countdown = 0x20;
                owner.state[slot] = 0;
                owner.privateCountdown1[slot] = 0x40;
            }
            owner.setAnimationVariant(slot, SnakeMotion.dashingAnimation(frameCounter));

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.SnakeMotion.Update run(
                SnakeMotion owner,
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                java.util.function.IntSupplier randomByteSupplier,
                /* Optional room geometry/history; null preserves the path without that room input. */ linksawakening
                                .world.entity.RoomEntityBackgroundCollision
                        backgroundCollision,
                int transitionCountdown) {
            if (inUse) {
                return new SnakeFrameStep(owner)
                        .run(
                                owner,
                                entity,
                                frameCounter,
                                linkEntityX,
                                linkEntityY,
                                randomByteSupplier,
                                backgroundCollision,
                                transitionCountdown);
            }
            inUse = true;
            slot = 0;
            countdown = 0;
            x = 0;
            y = 0;
            renderedVariant = 0;
            moved = null;
            updated = null;
            completedResult = null;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.randomByteSupplier = randomByteSupplier;
            this.backgroundCollision = backgroundCollision;
            this.transitionCountdown = transitionCountdown;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.moved = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
