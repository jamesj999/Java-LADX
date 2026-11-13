package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.event.ScreenShakeRequest;

/** Bank-$06 ArmosKnightEntityHandler's active state machine. */
public final class ArmosKnightMotion {
    public record RubbleRequest(int x, int y) {}

    public record ScreenShakeRequest(int countdown, int phase) {}

    public record Update(
            RoomEntity entity,
            int transitionCountdown,
            int physicsFlags,
            int hitboxFlags,
            int options1,
            int jingleId,
            boolean linkMotionBlocked,
            /* Optional screen-shake effect; absent when this visit does not request it. */ ScreenShakeRequest
                    screenShakeRequest,
            /* Optional rubble effect; absent outside the landing transition. */ RubbleRequest rubbleRequest) {}

    static final int STATE_WAKE = 0;
    static final int STATE_CHARGE = 1;
    static final int STATE_CHARGE_MOVE = 2;
    static final int STATE_JUMP_DELAY = 3;
    static final int STATE_JUMP = 4;
    static final int STATE_LAND_DELAY = 5;
    static final int STATE_BOUNCE = 6;
    static final int STATE_BOUNCE_DELAY = 7;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = STATE_WAKE;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        privateCountdown1[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(ArmosKnightFrame inputs) {
        return armosKnightStep.run(inputs);
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

    public int privateCountdown1(int slot) {
        return privateCountdown1[slot];
    }

    public int inertia(int slot) {
        return inertia[slot];
    }

    static boolean withinDistance(int value, int target, int distance) {
        int delta = (value - target) & 0xFF;
        int absolute = delta < 0x80 ? delta : 0x100 - delta;
        return absolute < distance;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance == 0 || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }

        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Vector(int x, int y) {}

    final ArmosKnightStep armosKnightStep = new ArmosKnightStep(this);

    public record ArmosKnightFrame(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int transitionCountdown,
            int physicsFlags,
            int hitboxFlags,
            int options1,
            int health) {}

    static final class ArmosKnightStep {
        private final ArmosKnightMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ArmosKnightMotion.ArmosKnightFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        private int linkZ;
        private int transitionCountdown;
        private int physicsFlags;
        private int hitboxFlags;
        private int options1;
        private int health;
        private int slot;
        private boolean linkMotionBlocked;
        private int x;
        private int y;
        private int z;
        private boolean landed;
        private int variant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional rubble effect; absent outside the landing transition. */ private linksawakening.world.entity.motion
                        .ArmosKnightMotion.RubbleRequest
                rubbleRequest;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional screen-shake effect; absent when this visit does not request it. */ private linksawakening.world
                        .entity.motion.ArmosKnightMotion.ScreenShakeRequest
                screenShakeRequest;
        private int jingleId;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        ArmosKnightStep(ArmosKnightMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.ArmosKnightMotion.Update run() {
            if (!prepareFrame()) return completedResult;
            if (!advanceCombatState()) return completedResult;

            return new ArmosKnightMotion.Update(
                    updated,
                    transitionCountdown,
                    physicsFlags,
                    hitboxFlags,
                    options1,
                    jingleId,
                    linkMotionBlocked,
                    screenShakeRequest,
                    rubbleRequest);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.ArmosKnightMotion.Update completedResult;

        private boolean advanceWake() {
            if (ArmosKnightMotion.withinDistance(x, linkEntityX, 0x20)
                    && ArmosKnightMotion.withinDistance(y, linkEntityY, 0x20)) {
                owner.state[slot] = ArmosKnightMotion.STATE_CHARGE;
                transitionCountdown = 0x30;
            }

            return true;
        }

        private boolean advanceCharge() {
            if (transitionCountdown == 0) {
                transitionCountdown = 0x80;
                owner.state[slot] = ArmosKnightMotion.STATE_CHARGE_MOVE;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                variant = 0;
            } else {
                variant = (transitionCountdown >>> 2) & 0x01;
            }

            return true;
        }

        private boolean advanceChargeMove() {
            if (transitionCountdown == 0) {
                transitionCountdown = 0x50;
                owner.state[slot] = ArmosKnightMotion.STATE_JUMP_DELAY;
                physicsFlags &= 0x7F;
                hitboxFlags &= 0x7F;
                options1 &= 0xBF;
            } else {
                owner.speedX[slot] = (transitionCountdown & 0x04) == 0 ? 0x08 : 0xF8;
                x = ArmosKnightMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            }

            return true;
        }

        private boolean advanceJumpDelay() {
            if (transitionCountdown == 0) {
                owner.speedZ[slot] = 0x30;
                owner.state[slot] = ArmosKnightMotion.STATE_JUMP;
                jingleId = 0x24;
            } else {
                if (landed) {
                    owner.speedZ[slot] = 0x0C;
                    int length = health < 0x05 ? 0x0C : 0x08;
                    ArmosKnightMotion.Vector vector =
                            ArmosKnightMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, length);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                    jingleId = 0x20;
                }
                x = ArmosKnightMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
                y = ArmosKnightMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            }

            return true;
        }

        private boolean advanceJump() {
            if ((owner.speedZ[slot] & 0xFE) == 0) {
                transitionCountdown = 0x10;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.state[slot] = ArmosKnightMotion.STATE_LAND_DELAY;
            }
            x = ArmosKnightMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            y = ArmosKnightMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);

            return true;
        }

        private boolean advanceLandDelay() {
            if (transitionCountdown == 0) {
                owner.state[slot] = ArmosKnightMotion.STATE_BOUNCE;
                owner.speedZ[slot] = 0xB0;
            } else {
                owner.speedZ[slot] = 0;
            }

            return true;
        }

        private boolean advanceBounce() {
            if (landed) {
                transitionCountdown = 0x30;
                jingleId = 0x0B;
                screenShakeRequest = new ArmosKnightMotion.ScreenShakeRequest(0x30, 0x04);
                if (linkZ == 0) {
                    owner.privateCountdown1[slot] = 0x40;
                }
                owner.state[slot] = ArmosKnightMotion.STATE_BOUNCE_DELAY;
            }

            return true;
        }

        private boolean advanceBounceDelay() {
            if (transitionCountdown == 0) {
                owner.state[slot] = ArmosKnightMotion.STATE_CHARGE_MOVE;
            }

            return true;
        }

        private boolean advanceDefault() {
            owner.state[slot] = ArmosKnightMotion.STATE_WAKE;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            owner.speedZ[slot] = 0;

            return true;
        }

        private boolean prepareFrame() {

            entity = inputs.entity();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkZ = inputs.linkZ();
            transitionCountdown = inputs.transitionCountdown();
            physicsFlags = inputs.physicsFlags();
            hitboxFlags = inputs.hitboxFlags();
            options1 = inputs.options1();
            health = inputs.health();
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            if (owner.privateCountdown1[slot] > 0) {
                owner.privateCountdown1[slot]--;
            }
            linkMotionBlocked = owner.privateCountdown1[slot] != 0;
            x = entity.x();
            y = entity.y();
            z = entity.z();
            landed = false;
            if (owner.speedZ[slot] != 0) {
                z = ArmosKnightMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
                owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
                if ((z & 0x80) != 0) {
                    z = 0;
                    owner.speedZ[slot] = 0;
                    landed = true;
                }
            }

            variant = entity.spriteVariant();
            if (health < 0x08) {
                variant = health < 0x04 ? 0x03 : 0x02;
            }
            rubbleRequest = null;
            screenShakeRequest = null;
            return true;
        }

        private boolean advanceCombatState() {

            if ((health & 0xFF) != owner.privateState1[slot]) {
                owner.privateState1[slot] = health & 0xFF;
                if ((health & 0xFF) < 0x08) {
                    int maximumInertia = (health & 0xFF) < 0x04 ? 2 : 1;
                    if (owner.inertia[slot] < maximumInertia) {
                        owner.inertia[slot]++;
                        int rubbleX = (health & 0xFF) < 0x04 ? x - 1 : x + 7;
                        int rubbleY = y - z - 0x10;
                        rubbleRequest = new ArmosKnightMotion.RubbleRequest(rubbleX & 0xFF, rubbleY & 0xFF);
                    }
                }
            }
            jingleId = -1;
            switch (owner.state[slot]) {
                case ArmosKnightMotion.STATE_WAKE -> {
                    if (!advanceWake()) return false;
                }
                case ArmosKnightMotion.STATE_CHARGE -> {
                    if (!advanceCharge()) return false;
                }
                case ArmosKnightMotion.STATE_CHARGE_MOVE -> {
                    if (!advanceChargeMove()) return false;
                }
                case ArmosKnightMotion.STATE_JUMP_DELAY -> {
                    if (!advanceJumpDelay()) return false;
                }
                case ArmosKnightMotion.STATE_JUMP -> {
                    if (!advanceJump()) return false;
                }
                case ArmosKnightMotion.STATE_LAND_DELAY -> {
                    if (!advanceLandDelay()) return false;
                }
                case ArmosKnightMotion.STATE_BOUNCE -> {
                    if (!advanceBounce()) return false;
                }
                case ArmosKnightMotion.STATE_BOUNCE_DELAY -> {
                    if (!advanceBounceDelay()) return false;
                }
                default -> {
                    if (!advanceDefault()) return false;
                }
            }

            updated = EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.ArmosKnightMotion.Update run(
                linksawakening.world.entity.motion.ArmosKnightMotion.ArmosKnightFrame inputs) {
            if (inUse) {
                return new ArmosKnightStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            linkEntityX = 0;
            linkEntityY = 0;
            linkZ = 0;
            transitionCountdown = 0;
            physicsFlags = 0;
            hitboxFlags = 0;
            options1 = 0;
            health = 0;
            slot = 0;
            linkMotionBlocked = false;
            x = 0;
            y = 0;
            z = 0;
            landed = false;
            variant = 0;
            rubbleRequest = null;
            screenShakeRequest = null;
            jingleId = 0;
            updated = null;
            completedResult = null;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
                this.entity = null;
                this.rubbleRequest = null;
                this.screenShakeRequest = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
