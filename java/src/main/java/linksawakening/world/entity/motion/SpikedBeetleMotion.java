package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 SpikedBeetleEntityHandler's rest, walk, dash, and flip states. */
public final class SpikedBeetleMotion {
    public static final int ENTITY_TYPE = 0x2C;
    static final int NORMAL_OPTIONS1 = 0x48;
    static final int FLIPPED_OPTIONS1 = 0x08;
    static final int NORMAL_HITBOX_FLAGS = 0x80;
    static final int FLIPPED_HITBOX_FLAGS = 0x00;

    private static final int[] WALKING_X_SPEEDS = {0x06, 0xFA, 0x00, 0x00};
    private static final int[] WALKING_Y_SPEEDS = {0x00, 0x00, 0xFA, 0x06};
    static final int[] DASHING_X_SPEEDS = {0x18, 0xE8, 0x00, 0x00};
    static final int[] DASHING_Y_SPEEDS = {0x00, 0x00, 0xE8, 0x18};
    private static final int[] FLIP_X_SPEEDS = {0x10, 0xF0, 0x00, 0x00};
    private static final int[] FLIP_Y_SPEEDS = {0x00, 0x00, 0xF0, 0x10};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] animationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        animationVariant[slot] = 0;
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
            int ignoreHitsCountdown) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        return advance(new SpikedBeetleFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                transitionCountdown,
                ignoreHitsCountdown));
    }

    public linksawakening.world.entity.motion.SpikedBeetleMotion.Update advance(SpikedBeetleFrame inputs) {
        return spikedBeetleFrameStep.run(inputs);
    }

    /** Mirrors the shield-contact branch at $03:6F2A. */
    public void flipFromShield(int slot, int romLinkDirection) {
        if (romLinkDirection < 0 || romLinkDirection > 3) {
            throw new IllegalArgumentException("ROM Link direction must be 0..3: " + romLinkDirection);
        }
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (state[slot] == 3) {
            return;
        }
        state[slot] = 3;
        speedZ[slot] = 0x20;
        speedX[slot] = FLIP_X_SPEEDS[romLinkDirection];
        speedY[slot] = FLIP_Y_SPEEDS[romLinkDirection];
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
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

    public int speedZ(int slot) {
        return speedZ[slot];
    }

    public boolean allowsCombat(int slot) {
        // SpikedBeetleEntityHandler calls DefaultEnemyDamageCollisionHandler
        // before dispatching every state, including flipped state $03.
        return initialized[slot];
    }

    void startDash(int slot, int linkEntityX, int linkEntityY, int entityX, int entityY) {
        state[slot]++;
        direction[slot] = directionToLink(entityX, entityY, linkEntityX, linkEntityY);
    }

    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];

    void setWalkingSpeed(int slot, int newDirection) {
        direction[slot] = newDirection & 0x03;
        speedX[slot] = WALKING_X_SPEEDS[direction[slot]];
        speedY[slot] = WALKING_Y_SPEEDS[direction[slot]];
    }

    static boolean nearLinkOnOneAxis(int entityX, int entityY, int linkEntityX, int linkEntityY) {
        return withinSourceWindow((linkEntityX - entityX) & 0xFF) || withinSourceWindow((linkEntityY - entityY) & 0xFF);
    }

    private static boolean withinSourceWindow(int distance) {
        return (((distance & 0xFF) + 0x06) & 0xFF) < 0x0A;
    }

    static int directionToLink(int entityX, int entityY, int linkEntityX, int linkEntityY) {
        int distanceX = signedByte((linkEntityX - entityX) & 0xFF);
        int distanceY = signedByte((linkEntityY - entityY) & 0xFF);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (nextX != x && backgroundCollision.blocks(entity, directionForX(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (nextY != y && backgroundCollision.blocks(entity, directionForY(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
    }

    void clearSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    static int approach(int current, int target) {
        int difference = (target - current) & 0xFF;
        if (difference == 0) {
            return current & 0xFF;
        }
        return (difference & 0x80) != 0 ? (current - 1) & 0xFF : (current + 1) & 0xFF;
    }

    static int halveSignedSpeed(int speed) {
        int signed = signedByte(speed);
        return (signed >> 1) & 0xFF;
    }

    static int complementHalfSpeed(int speed) {
        return (~(signedByte(speed) >> 1)) & 0xFF;
    }

    private static int directionForX(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    private static int directionForY(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
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
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Position(int x, int y, boolean collided) {}

    public record Update(RoomEntity entity, int transitionCountdown, int options1, int hitboxFlags) {}

    final SpikedBeetleFrameStep spikedBeetleFrameStep = new SpikedBeetleFrameStep(this);

    record SpikedBeetleFrame(
            linksawakening.world.entity.RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown,
            int ignoreHitsCountdown) {}

    static final class SpikedBeetleFrameStep {
        private final SpikedBeetleMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.SpikedBeetleMotion.SpikedBeetleFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int transitionCountdown;
        private int ignoreHitsCountdown;
        private int slot;
        private int countdown;
        private int x;
        private int y;
        private int z;
        private boolean landed;
        private int renderedVariant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.SpikedBeetleMotion.Position moved;
        private boolean collided;
        private boolean flippedHandler;
        private boolean walkAnimationRequested;
        private boolean flippedAnimationRequested;
        private int nextVariant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        SpikedBeetleFrameStep(SpikedBeetleMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.SpikedBeetleMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceMovementAndState()) return completedResult;

            return new SpikedBeetleMotion.Update(
                    updated,
                    countdown,
                    flippedHandler ? SpikedBeetleMotion.FLIPPED_OPTIONS1 : SpikedBeetleMotion.NORMAL_OPTIONS1,
                    flippedHandler ? SpikedBeetleMotion.FLIPPED_HITBOX_FLAGS : SpikedBeetleMotion.NORMAL_HITBOX_FLAGS);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.SpikedBeetleMotion.Update completedResult;

        private boolean advanceState0() {
            owner.clearSpeed(slot);
            if (countdown == 0) {
                countdown = 0x30 + ((randomByteSupplier.getAsInt() & 0xFF) & 0x1F);
                owner.state[slot] = 1;
                int randomDirection = randomByteSupplier.getAsInt() & 0xFF;
                int direction = (randomDirection & 0x06) == 0
                        ? SpikedBeetleMotion.directionToLink(x, y, linkEntityX, linkEntityY)
                        : (randomByteSupplier.getAsInt() & 0x03);
                owner.setWalkingSpeed(slot, direction);
            }
            if (SpikedBeetleMotion.nearLinkOnOneAxis(x, y, linkEntityX, linkEntityY)) {
                owner.startDash(slot, linkEntityX, linkEntityY, x, y);
                countdown = 0xFF;
            }

            return true;
        }

        private boolean advanceState1() {
            walkAnimationRequested = true;
            if (countdown == 0) {
                countdown = 0x18;
                owner.state[slot] = 0;
            }
            if (SpikedBeetleMotion.nearLinkOnOneAxis(x, y, linkEntityX, linkEntityY)) {
                owner.startDash(slot, linkEntityX, linkEntityY, x, y);
                countdown = 0xFF;
            }

            return true;
        }

        private boolean advanceState2() {
            walkAnimationRequested = true;
            if (countdown == 0 || collided) {
                owner.state[slot] = 0;
            } else {
                int dashDirection = owner.direction[slot];
                owner.speedX[slot] = SpikedBeetleMotion.approach(
                        owner.speedX[slot], SpikedBeetleMotion.DASHING_X_SPEEDS[dashDirection]);
                owner.speedY[slot] = SpikedBeetleMotion.approach(
                        owner.speedY[slot], SpikedBeetleMotion.DASHING_Y_SPEEDS[dashDirection]);
            }

            return true;
        }

        private boolean advanceState3() {
            flippedAnimationRequested = true;
            if (countdown == 0) {
                // $07:790A increments the state, then writes B (zero)
                // through the returned state-table pointer.
                owner.state[slot] = 0;
                owner.speedZ[slot] = 0x18;
                owner.speedX[slot] = 0;
            } else if (countdown < 0x60) {
                owner.speedX[slot] = (countdown & 0x04) == 0 ? 0xF8 : 0x08;
            }

            return true;
        }

        private boolean prepareFrame() {

            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();
            transitionCountdown = inputs.transitionCountdown();
            ignoreHitsCountdown = inputs.ignoreHitsCountdown();

            if ((entity.type() & 0xFF) != SpikedBeetleMotion.ENTITY_TYPE) {
                throw new IllegalArgumentException(
                        "Unsupported Spiked Beetle entity type: 0x" + Integer.toHexString(entity.type()));
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Spiked Beetle random-byte supplier cannot be null");
            }

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            countdown = transitionCountdown & 0xFF;
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;
            z = SpikedBeetleMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;

            // SpikedBeetleEntityHandler clears horizontal speed while the shared
            // ignore-hits window is active, before applying position movement.
            if ((ignoreHitsCountdown & 0xFF) != 0) {
                owner.clearSpeed(slot);
            }

            landed = (z & 0x80) != 0 || z == 0;
            return true;
        }

        private boolean advanceMovementAndState() {

            if (landed) {
                z = 0;
                if (owner.state[slot] == 3) {
                    owner.speedX[slot] = SpikedBeetleMotion.halveSignedSpeed(owner.speedX[slot]);
                    owner.speedY[slot] = SpikedBeetleMotion.halveSignedSpeed(owner.speedY[slot]);
                    owner.speedZ[slot] = SpikedBeetleMotion.complementHalfSpeed(owner.speedZ[slot]);
                    if (owner.speedZ[slot] < 0x07) {
                        owner.speedZ[slot] = 0;
                        owner.clearSpeed(slot);
                    }
                } else {
                    owner.speedZ[slot] = 0;
                }
            }

            renderedVariant = owner.animationVariant[slot];
            moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            collided = moved.collided();
            flippedHandler = owner.state[slot] == 3;

            walkAnimationRequested = false;
            flippedAnimationRequested = false;
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
                default -> throw new IllegalStateException("Invalid Spiked Beetle state: " + owner.state[slot]);
            }

            nextVariant = owner.animationVariant[slot];
            if (walkAnimationRequested) {
                nextVariant = (frameCounter >>> 4) & 0x01;
            } else if (flippedAnimationRequested) {
                nextVariant = 2 + ((frameCounter >>> 4) & 0x01);
            }
            owner.animationVariant[slot] = nextVariant;
            updated = EntitySnapshots.withMotion(
                    entity, x, y, renderedVariant, entity.entityFlipAttribute(), z, -1, false);
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.SpikedBeetleMotion.Update run(
                linksawakening.world.entity.motion.SpikedBeetleMotion.SpikedBeetleFrame inputs) {
            if (inUse) {
                return new SpikedBeetleFrameStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            randomByteSupplier = null;
            backgroundCollision = null;
            transitionCountdown = 0;
            ignoreHitsCountdown = 0;
            slot = 0;
            countdown = 0;
            x = 0;
            y = 0;
            z = 0;
            landed = false;
            renderedVariant = 0;
            moved = null;
            collided = false;
            flippedHandler = false;
            walkAnimationRequested = false;
            flippedAnimationRequested = false;
            nextVariant = 0;
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
                this.backgroundCollision = null;
                this.moved = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
