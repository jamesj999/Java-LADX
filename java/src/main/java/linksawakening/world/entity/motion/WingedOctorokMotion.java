package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$07 WingedOctorokEntityHandler state and WRAM fields. */
public final class WingedOctorokMotion {
    static final int ENTITY_WINGED_OCTOROK = 0xAE;
    private static final int ENTITY_SWORD = 0x01;
    static final int[] SPEED_X_BY_DIRECTION = {0x08, 0xF8, 0x00, 0x00};
    static final int[] SPEED_Y_BY_DIRECTION = {0x00, 0x00, 0xF8, 0x08};
    private static final int[] BASE_VARIANT_BY_DIRECTION = {6, 4, 2, 0};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] zPosition = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] presentationVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] zInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record RockSpawn(int direction) {
        public RockSpawn {
            if (direction < 0 || direction > 3) {
                throw new IllegalArgumentException("Winged Octorok rock direction out of range: " + direction);
            }
        }
    }

    public record Update(
            RoomEntity entity,
            int state,
            int transitionCountdown,
            /* Optional emitted rock; absent when this visit does not launch one. */ RockSpawn rockSpawn,
            boolean jumped,
            boolean skippedDefaultCollision) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public Update advance(WingedOctorokFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        int transitionCountdown = inputs.transitionCountdown();
        int ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        boolean actionButtonsHeld = inputs.actionButtonsHeld();
        int linkItemA = inputs.linkItemA();
        int linkItemB = inputs.linkItemB();
        return advance(new WingedOctorokInteractionFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                randomByteSupplier,
                backgroundCollision,
                transitionCountdown,
                ignoreHitsCountdown,
                actionButtonsHeld,
                actionButtonsHeld,
                linkItemA,
                linkItemB));
    }

    public Update advance(WingedOctorokInteractionFrame inputs) {
        return wingedOctorokStep.run(inputs);
    }

    public void decrementPrivateCountdown2(int slot) {
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    public int state(int slot) {
        return state[slot];
    }

    int direction(int slot) {
        return direction[slot];
    }

    public int privateState2(int slot) {
        return privateState2[slot];
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    public void clear(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        privateState1[slot] = 0;
        privateState2[slot] = 0;
        privateCountdown2[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        zPosition[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        inertia[slot] = 0;
        presentationVariant[slot] = 0;
        initialized[slot] = false;
        zInitialized[slot] = false;
    }

    void setVariantForDirection(int slot) {
        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        presentationVariant[slot] = BASE_VARIANT_BY_DIRECTION[direction[slot] & 0x03] | ((inertia[slot] >>> 4) & 0x01);
    }

    void clearHorizontalSpeed(int slot) {
        speedX[slot] = 0;
        speedY[slot] = 0;
    }

    static boolean swordAttackHeld(boolean actionButtonAHeld, boolean actionButtonBHeld, int linkItemA, int linkItemB) {
        if ((linkItemB & 0xFF) == ENTITY_SWORD) {
            return actionButtonBHeld;
        }
        return (linkItemA & 0xFF) == ENTITY_SWORD && actionButtonAHeld;
    }

    Position move(RoomEntity entity, int x, int y, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        boolean collided = false;
        if (nextX != x && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), nextX, y)) {
            nextX = x;
            collided = true;
        }
        if (nextY != y && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), nextX, nextY)) {
            nextY = y;
            collided = true;
        }
        return new Position(nextX, nextY, collided);
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
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int quotient = larger == 0 ? length : (length * smaller) / larger;
        int y = absoluteY < absoluteX ? quotient : length;
        int x = absoluteY < absoluteX ? length : quotient;
        if (distanceY < 0) {
            y = -y;
        }
        if (distanceX < 0) {
            x = -x;
        }
        return new Vector(y & 0xFF, x & 0xFF);
    }

    static boolean withinSignedWindow(int difference) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -0x20 && signedDifference < 0x20;
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

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    record Position(int x, int y, boolean collided) {}

    record Vector(int y, int x) {}

    final WingedOctorokStep wingedOctorokStep = new WingedOctorokStep(this);

    record WingedOctorokFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown,
            int ignoreHitsCountdown,
            boolean actionButtonsHeld,
            int linkItemA,
            int linkItemB) {}

    public record WingedOctorokInteractionFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            int transitionCountdown,
            int ignoreHitsCountdown,
            boolean actionButtonAHeld,
            boolean actionButtonBHeld,
            int linkItemA,
            int linkItemB) {
        public WingedOctorokInteractionFrame {
            backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        }
    }

    static final class WingedOctorokStep {
        private final WingedOctorokMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.WingedOctorokMotion.WingedOctorokInteractionFrame inputs;
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
        private boolean actionButtonAHeld;
        private boolean actionButtonBHeld;
        private int linkItemA;
        private int linkItemB;
        private int slot;
        private boolean wasInitialized;
        private int renderedVariant;
        private int countdown;
        private int x;
        private int y;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.WingedOctorokMotion.Position moved;
        private int z;
        private boolean landed;
        private boolean stateTwoHandler;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional emitted rock; absent when this visit does not launch one. */ private linksawakening.world.entity
                        .motion.WingedOctorokMotion.RockSpawn
                rockSpawn;
        private boolean jumped;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        WingedOctorokStep(WingedOctorokMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.WingedOctorokMotion.Update run() {
            if (!prepareMotion()) return completedResult;
            if (!advanceLandingAndState()) return completedResult;

            return new WingedOctorokMotion.Update(
                    updated, owner.state[slot], countdown, rockSpawn, jumped, stateTwoHandler || jumped);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.WingedOctorokMotion.Update completedResult;

        private boolean advanceState0() {
            if (moved.collided() || countdown == 0) {
                countdown = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
                owner.state[slot] = 1;
                owner.clearHorizontalSpeed(slot);
            }
            owner.setVariantForDirection(slot);

            return true;
        }

        private boolean advanceState1() {
            if (countdown != 0) {
                if (countdown == 0x0A
                        && owner.direction[slot]
                                == WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY)) {
                    rockSpawn = new WingedOctorokMotion.RockSpawn(owner.direction[slot]);
                }
                owner.clearHorizontalSpeed(slot);
            } else {
                countdown = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                owner.state[slot] = 0;
                owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0x03;
                if (owner.privateState1[slot] == 0) {
                    owner.direction[slot] = WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY);
                } else {
                    owner.direction[slot] = randomByteSupplier.getAsInt() & 0x03;
                }
                owner.speedX[slot] = WingedOctorokMotion.SPEED_X_BY_DIRECTION[owner.direction[slot]];
                owner.speedY[slot] = WingedOctorokMotion.SPEED_Y_BY_DIRECTION[owner.direction[slot]];
            }

            return true;
        }

        private boolean advanceState2() {
            owner.privateState2[slot] = (frameCounter >>> 2) & 0x01;
            owner.direction[slot] = WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY);
            owner.setVariantForDirection(slot);
            if (landed) {
                owner.state[slot] = 1;
                countdown = 0x20;
            }

            return true;
        }

        private boolean prepareMotion() {

            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();
            transitionCountdown = inputs.transitionCountdown();
            ignoreHitsCountdown = inputs.ignoreHitsCountdown();
            actionButtonAHeld = inputs.actionButtonAHeld();
            actionButtonBHeld = inputs.actionButtonBHeld();
            linkItemA = inputs.linkItemA();
            linkItemB = inputs.linkItemB();
            if ((entity.type() & 0xFF) != WingedOctorokMotion.ENTITY_WINGED_OCTOROK) {
                throw new IllegalArgumentException(
                        "Unsupported Winged Octorok motion entity type: 0x" + Integer.toHexString(entity.type()));
            }
            if (randomByteSupplier == null) {
                throw new IllegalArgumentException("Winged Octorok random-byte supplier cannot be null");
            }

            slot = entity.slot();
            wasInitialized = owner.initialized[slot];
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }
            if (!owner.zInitialized[slot]) {
                owner.zPosition[slot] = entity.z() & 0xFF;
                owner.zInitialized[slot] = true;
            }

            renderedVariant = wasInitialized ? owner.presentationVariant[slot] : entity.spriteVariant();
            countdown = transitionCountdown & 0xFF;
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;

            // The handler forces state 1 while the shared recoil/ignore window is live.
            if ((ignoreHitsCountdown & 0xFF) != 0) {
                owner.state[slot] = 1;
                countdown = 0x40;
            }

            moved = owner.move(entity, x, y, backgroundCollision);
            x = moved.x();
            y = moved.y();
            z = WingedOctorokMotion.addSpeedToPosition(
                    owner.zPosition[slot], owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.zPosition[slot] = z;
            owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
            return true;
        }

        private boolean advanceLandingAndState() {

            landed = (z & 0x80) != 0;
            if (landed) {
                z = 0;
                owner.zPosition[slot] = 0;
                owner.speedZ[slot] = 0;
                owner.privateState2[slot] = 0;
            }

            stateTwoHandler = owner.state[slot] == 2;
            rockSpawn = null;
            jumped = false;
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
                default -> throw new IllegalStateException("Invalid Winged Octorok state: " + owner.state[slot]);
            }

            if (!stateTwoHandler
                    && owner.privateCountdown2[slot] == 0
                    && WingedOctorokMotion.withinSignedWindow(linkEntityX - x)
                    && WingedOctorokMotion.withinSignedWindow(linkEntityY - y)
                    && WingedOctorokMotion.swordAttackHeld(actionButtonAHeld, actionButtonBHeld, linkItemA, linkItemB)
                    && owner.direction[slot]
                            != (WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY) ^ 0x01)) {
                owner.speedZ[slot] = 0x18;
                WingedOctorokMotion.Vector vector =
                        WingedOctorokMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x10);
                owner.speedY[slot] = vector.y();
                owner.speedX[slot] = vector.x();
                owner.state[slot] = 2;
                jumped = true;
            }

            updated = EntitySnapshots.withMotion(
                    entity, x, y, renderedVariant, entity.entityFlipAttribute(), z, -1, false);
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.WingedOctorokMotion.Update run(
                linksawakening.world.entity.motion.WingedOctorokMotion.WingedOctorokInteractionFrame inputs) {
            if (inUse) {
                return new WingedOctorokStep(owner).run(inputs);
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
            actionButtonAHeld = false;
            actionButtonBHeld = false;
            linkItemA = 0;
            linkItemB = 0;
            slot = 0;
            wasInitialized = false;
            renderedVariant = 0;
            countdown = 0;
            x = 0;
            y = 0;
            moved = null;
            z = 0;
            landed = false;
            stateTwoHandler = false;
            rockSpawn = null;
            jumped = false;
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
                this.rockSpawn = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
