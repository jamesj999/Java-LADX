package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$04 motion and state machines shared by the two Bombite handlers. */
public final class BombiteMotion {
    static final int ENTITY_BOUNCING_BOMBITE = 0x55;
    static final int ENTITY_TIMER_BOMBITE = 0x56;

    static final int[] TIMER_SPEED_X = {0x08, 0xF8, 0x00, 0x00};
    static final int[] TIMER_SPEED_Y = {0x00, 0x00, 0xF8, 0x00};
    static final int[] BOUNCING_SPEED_X = {0x08, 0xF8, 0x00, 0x00};
    static final int[] BOUNCING_SPEED_Y = {0x00, 0x00, 0xF8, 0x08};
    static final int[] TIMER_COUNTDOWN_VARIANTS = {5, 5, 4, 3, 2, 2, 2};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int slowTransitionCountdown,
            int privateCountdown1,
            int spriteVariant,
            boolean explode,
            boolean bumpJingle) {}

    public void initialize(int slot) {
        state[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(BombiteFrame inputs) {
        return bombiteStep.run(inputs);
    }

    /** Starts the BouncingBombite state-$02 sword-recoil path. */
    public void enterBouncingLitFromSword(int slot, RoomEntity entity, int linkEntityX, int linkEntityY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        Vector vector = vectorTowardsLink(entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x30);
        speedX[slot] = negate(vector.x());
        speedY[slot] = negate(vector.y());
        state[slot] = 2;
    }

    /**
     * Mirrors func_003_75A2's target-side Bombite writes. The ROM copies the
     * active Bombite's current speed bytes directly; it does not recalculate
     * the vector or reset the fixed-point movement accumulators.
     */
    public void enterBouncingLitFromEntityCollision(int slot, int sourceSpeedX, int sourceSpeedY) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        speedX[slot] = sourceSpeedX & 0xFF;
        speedY[slot] = sourceSpeedY & 0xFF;
        state[slot] = 2;
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot] & 0xFF;
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY);
        if (Math.abs(distanceY) >= Math.abs(distanceX)) {
            return distanceY < 0 ? 2 : 3;
        }
        return distanceX < 0 ? 1 : 0;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int smallerComponent = divideComponent(smallerDistance, largerDistance, length);
        int x = absoluteX >= absoluteY ? length : smallerComponent;
        int y = absoluteY > absoluteX ? length : smallerComponent;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY <= 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int divideComponent(int smallerDistance, int largerDistance, int length) {
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
            remainder = sum;
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

    static int negate(int speed) {
        return -signedByte(speed) & 0xFF;
    }

    static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return EntitySnapshots.withMotion(
                entity, x, y, entity.spriteVariant(), entity.entityFlipAttribute(), entity.z(), -1, false);
    }

    record Vector(int x, int y) {}

    final BombiteStep bombiteStep = new BombiteStep(this);

    public record BombiteFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int transitionCountdown,
            int slowTransitionCountdown,
            int privateCountdown1,
            int enemyIgnoreHitsCountdown,
            boolean runningWithPegasusBoots,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        public BombiteFrame {
            backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        }
    }

    static final class BombiteStep {
        private final BombiteMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.BombiteMotion.BombiteFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private int transitionCountdown;
        private int slowTransitionCountdown;
        private int privateCountdown1;
        private int enemyIgnoreHitsCountdown;
        private boolean runningWithPegasusBoots;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int slot;
        private int nextTransition;
        private int nextSlowTransition;
        private int nextPrivateCountdown1;
        private int spriteVariant;
        private boolean explode;
        private boolean bumpJingle;
        private int x;
        private int y;
        private boolean blockedX;
        private boolean blockedY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity moved;

        BombiteStep(BombiteMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.BombiteMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceBombite()) return completedResult;

            return new BombiteMotion.Update(
                    moved,
                    nextTransition,
                    nextSlowTransition,
                    nextPrivateCountdown1,
                    spriteVariant,
                    explode,
                    bumpJingle);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.BombiteMotion.Update completedResult;

        private boolean advanceState0() {
            if (nextTransition == 0) {
                owner.state[slot] = 1;
            }
            spriteVariant = (frameCounter >>> 4) & 0x01;

            return true;
        }

        private boolean advanceState1() {
            int directionChoice = randomByteSupplier.getAsInt() & 0x03;
            int direction = directionChoice == 0
                    ? BombiteMotion.directionToLink(moved.x(), moved.y(), linkEntityX, linkEntityY)
                    : randomByteSupplier.getAsInt() & 0x03;
            owner.speedX[slot] = BombiteMotion.BOUNCING_SPEED_X[direction];
            owner.speedY[slot] = BombiteMotion.BOUNCING_SPEED_Y[direction];
            nextTransition = 0x20 + (randomByteSupplier.getAsInt() & 0x0F);
            owner.state[slot] = 0;

            return true;
        }

        private boolean advanceState2() {
            if (nextTransition == 0) {
                explode = true;
            } else {
                if (blockedX) {
                    owner.speedX[slot] = BombiteMotion.negate(owner.speedX[slot]);
                    bumpJingle = true;
                } else if (blockedY) {
                    owner.speedY[slot] = BombiteMotion.negate(owner.speedY[slot]);
                    bumpJingle = true;
                }
                spriteVariant = (frameCounter >>> 1) & 0x01;
            }

            return true;
        }

        private boolean prepareFrame() {

            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            transitionCountdown = inputs.transitionCountdown();
            slowTransitionCountdown = inputs.slowTransitionCountdown();
            privateCountdown1 = inputs.privateCountdown1();
            enemyIgnoreHitsCountdown = inputs.enemyIgnoreHitsCountdown();
            runningWithPegasusBoots = inputs.runningWithPegasusBoots();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            nextTransition = transitionCountdown & 0xFF;
            nextSlowTransition = slowTransitionCountdown & 0xFF;
            nextPrivateCountdown1 = privateCountdown1 & 0xFF;
            spriteVariant = entity.spriteVariant();
            explode = false;
            bumpJingle = false;

            if (entity.type() == BombiteMotion.ENTITY_TIMER_BOMBITE
                    && enemyIgnoreHitsCountdown == 0x08
                    && owner.state[slot] == 0) {
                // TimerBombiteEntityHandler's pre-movement hit-countdown trigger.
                owner.state[slot] = 1;
                nextSlowTransition = 0x6F;
            }

            x = BombiteMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
            y = BombiteMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
            blockedX = false;
            blockedY = false;
            {
                if (x != entity.x()
                        && backgroundCollision.blocks(
                                entity, BombiteMotion.horizontalDirection(owner.speedX[slot]), x, entity.y())) {
                    x = entity.x();
                    blockedX = true;
                }
                if (y != entity.y()
                        && backgroundCollision.blocks(
                                entity, BombiteMotion.verticalDirection(owner.speedY[slot]), x, y)) {
                    y = entity.y();
                    blockedY = true;
                }
            }

            moved = BombiteMotion.withPosition(entity, x, y);
            return true;
        }

        private boolean advanceBombite() {

            if (entity.type() == BombiteMotion.ENTITY_TIMER_BOMBITE) {
                if (owner.state[slot] == 0) {
                    if (nextTransition == 0) {
                        int random = randomByteSupplier.getAsInt() & 0xFF;
                        nextTransition = 0x30 + (random & 0x1F);
                        int direction = random & 0x03;
                        owner.speedX[slot] = BombiteMotion.TIMER_SPEED_X[direction];
                        owner.speedY[slot] = BombiteMotion.TIMER_SPEED_Y[direction];
                    }
                    spriteVariant = (frameCounter >>> 4) & 0x01;
                } else {
                    if (runningWithPegasusBoots) {
                        owner.state[slot] = 0;
                    } else {
                        int xDistance = BombiteMotion.unsignedByteAbs(linkEntityX - moved.x());
                        int yDistance = BombiteMotion.unsignedByteAbs(linkEntityY - moved.y());
                        if (xDistance >= 0x12 || yDistance >= 0x12) {
                            if (((frameCounter ^ slot) & 0x03) == 0) {
                                BombiteMotion.Vector vector = BombiteMotion.vectorTowardsLink(
                                        moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x0E);
                                owner.speedX[slot] = vector.x();
                                owner.speedY[slot] = vector.y();
                            }
                        } else {
                            owner.speedX[slot] = 0;
                            owner.speedY[slot] = 0;
                        }

                        int originalSlowTransition = nextSlowTransition;
                        if (originalSlowTransition == 0) {
                            explode = true;
                        } else {
                            if (originalSlowTransition == 0x18) {
                                nextSlowTransition = 0x0A;
                                nextPrivateCountdown1 = 0x30;
                            }
                            spriteVariant = BombiteMotion.TIMER_COUNTDOWN_VARIANTS[
                                    Math.min(
                                            originalSlowTransition >>> 4,
                                            BombiteMotion.TIMER_COUNTDOWN_VARIANTS.length - 1)];
                        }
                    }
                }
            } else if (entity.type() == BombiteMotion.ENTITY_BOUNCING_BOMBITE) {
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
                    default -> throw new IllegalStateException("Invalid Bombite state: " + owner.state[slot]);
                }
            }
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.BombiteMotion.Update run(
                linksawakening.world.entity.motion.BombiteMotion.BombiteFrame inputs) {
            if (inUse) {
                return new BombiteStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            transitionCountdown = 0;
            slowTransitionCountdown = 0;
            privateCountdown1 = 0;
            enemyIgnoreHitsCountdown = 0;
            runningWithPegasusBoots = false;
            randomByteSupplier = null;
            backgroundCollision = null;
            slot = 0;
            nextTransition = 0;
            nextSlowTransition = 0;
            nextPrivateCountdown1 = 0;
            spriteVariant = 0;
            explode = false;
            bumpJingle = false;
            x = 0;
            y = 0;
            blockedX = false;
            blockedY = false;
            moved = null;
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
                this.completedResult = null;
            }
        }
    }
}
