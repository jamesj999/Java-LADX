package linksawakening.world.entity.motion;

import java.util.Objects;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;

/** Bank-$06 CrowEntityHandler's proximity, flight, and bounds state machine. */
public final class CrowMotion {
    public static final int ENTITY_TYPE = 0x7A;
    public static final int INITIAL_PHYSICS_FLAGS = 0x02 | 0x10 | 0x40;
    static final int ACTIVE_PHYSICS_FLAGS = 0x02 | 0x10;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] physicsFlags = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] perchPositionAdjusted = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Entity $7A uses EntityInitNoop; the handler owns all of these fields. */
    public void initialize(int slot) {
        state[slot] = 0;
        direction[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        physicsFlags[slot] = INITIAL_PHYSICS_FLAGS;
        selectedVariant[slot] = 0;
        perchPositionAdjusted[slot] = false;
        initialized[slot] = true;
    }

    /**
     * Advances one handler pass. The shared entity loop has already decremented
     * the transition byte before this method is called.
     */
    public Update advance(RoomEntity entity, int transitionCountdown, int linkEntityX, int linkEntityY) {
        return advance(entity, transitionCountdown, 0, linkEntityX, linkEntityY);
    }

    public Update advance(
            RoomEntity entity, int transitionCountdown, int frameCounter, int linkEntityX, int linkEntityY) {
        return advance(entity, transitionCountdown, frameCounter, linkEntityX, linkEntityY, true, false);
    }

    public Update advance(
            RoomEntity entity,
            int transitionCountdown,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean allowLinkProximityTrigger,
            boolean externalTakeoffTrigger) {
        return crowStep.run(
                entity,
                transitionCountdown,
                frameCounter,
                linkEntityX,
                linkEntityY,
                allowLinkProximityTrigger,
                externalTakeoffTrigger);
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

    int physicsFlags(int slot) {
        return physicsFlags[slot];
    }

    public boolean allowsEnemyCollision(int slot) {
        return state[slot] >= 1;
    }

    void ensureInitialized(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
    }

    static int flightVariant(int movedY, int speedY) {
        // At $5E0B, HL is left by UpdateEntityPosWithSpeed_06: either the
        // updated Y position, or the zero Y-speed byte on its early return.
        // Both PlayBoomerangSfx calls preserve HL.
        return speedY == 0 ? 0 : (movedY >>> 3) & 0x01;
    }

    /** Mirrors AddEntitySpeedToPos_06's signed high-nibble fixed point step. */
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

    /** Port of GetVectorTowardsLink with the requested infinity norm. */
    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        // The flight trampoline uses bank $03's height-aware distance helper,
        // unlike the bank $06 helper used for the perched proximity check.
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        boolean xNegative = distanceX < 0;
        boolean yNegative = distanceY < 0;
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
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
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (xNegative) {
            x = -x;
        }
        if (yNegative) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    /** The Crow handler changes each component by one toward its target byte. */
    static int approach(int current, int target) {
        int difference = (target - current) & 0xFF;
        if ((difference & 0x80) == 0) {
            return (current + 1) & 0xFF;
        }
        return (current - 1) & 0xFF;
    }

    static int negate(int value) {
        return -signedByte(value) & 0xFF;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Vector(int x, int y) {}

    public record Update(
            RoomEntity entity, int transitionCountdown, int physicsFlags, boolean unloaded, boolean boomerangSound) {}

    final CrowStep crowStep = new CrowStep(this);

    static final class CrowStep {
        private final CrowMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int transitionCountdown;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean allowLinkProximityTrigger;
        private boolean externalTakeoffTrigger;
        private int slot;
        private int countdown;
        private int x;
        private int y;
        private int z;
        private int variant;
        private boolean boomerangSound;
        private boolean unloaded;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        CrowStep(CrowMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.CrowMotion.Update run() {
            if (entity == null || (entity.type() & 0xFF) != CrowMotion.ENTITY_TYPE) {
                throw new IllegalArgumentException("Unsupported Crow motion entity");
            }
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            countdown = transitionCountdown & 0xFF;
            x = entity.x() & 0xFF;
            y = entity.y() & 0xFF;
            z = entity.z() & 0xFF;
            variant = owner.selectedVariant[slot];
            boomerangSound = false;
            unloaded = false;

            // $5CBE mirrors only the active display variant, then renders it.
            // SetEntitySpriteVariant later writes the stored frame for the next
            // handler pass, without changing this frame's displayed facing.
            if (owner.direction[slot] == 0) {
                variant += 2;
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
                case 3 -> {
                    if (!advanceState3()) return completedResult;
                }
                default -> throw new IllegalStateException("Invalid Crow state: " + owner.state[slot]);
            }

            updated = EntitySnapshots.withMotion(
                    entity,
                    x,
                    y,
                    variant,
                    entity.entityFlipAttribute(),
                    z,
                    entity.deathSpriteVariant(),
                    entity.powerRecoilDeath());
            return new CrowMotion.Update(updated, countdown, owner.physicsFlags[slot], unloaded, boomerangSound);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.CrowMotion.Update completedResult;

        private boolean advanceState0() {
            // The first handler pass lifts the visual origin by four
            // pixels and advances the state before checking proximity.
            if (!owner.perchPositionAdjusted[slot]) {
                y = (y - 4) & 0xFF;
                owner.perchPositionAdjusted[slot] = true;
            }
            owner.direction[slot] = CrowMotion.signedByte((linkEntityX - x) & 0xFF) < 0 ? 1 : 0;
            int distanceX = CrowMotion.signedByte((linkEntityX - x) & 0xFF);
            int distanceY = CrowMotion.signedByte((linkEntityY - y) & 0xFF);
            if (externalTakeoffTrigger
                    || (allowLinkProximityTrigger
                            && distanceX >= -0x18
                            && distanceX < 0x18
                            && distanceY >= -0x30
                            && distanceY < 0x30)) {
                owner.physicsFlags[slot] = CrowMotion.ACTIVE_PHYSICS_FLAGS;
                countdown = 0x22;
                owner.state[slot] = 1;
            }

            return true;
        }

        private boolean advanceState1() {
            if (countdown == 0) {
                countdown = 0x30;
                owner.state[slot] = 2;
            } else {
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.speedZ[slot] = 0x08;
                z = CrowMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
                boomerangSound = true;
                // AddEntityZSpeedToPos_06 leaves HL at the updated Z byte.
                owner.selectedVariant[slot] = (z >>> 3) & 0x01;
            }

            return true;
        }

        private boolean advanceState2() {
            if (countdown == 0) {
                owner.state[slot] = 3;
            } else {
                if ((countdown & 0x01) == 0) {
                    CrowMotion.Vector vector = CrowMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x20);
                    owner.speedY[slot] = CrowMotion.approach(owner.speedY[slot], vector.y());
                    owner.speedX[slot] = CrowMotion.approach(owner.speedX[slot], vector.x());
                    owner.direction[slot] = owner.speedX[slot] & 0x80;
                }
                x = CrowMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
                y = CrowMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
                boomerangSound = true;
                owner.selectedVariant[slot] = CrowMotion.flightVariant(y, owner.speedY[slot]);
            }

            return true;
        }

        private boolean advanceState3() {
            if ((frameCounter & 0x03) == 0) {
                CrowMotion.Vector vector = CrowMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x20);
                owner.speedY[slot] = CrowMotion.approach(owner.speedY[slot], CrowMotion.negate(vector.y()));
                owner.speedX[slot] = CrowMotion.approach(owner.speedX[slot], CrowMotion.negate(vector.x()));
                owner.direction[slot] = owner.speedX[slot] & 0x80;
            }
            x = CrowMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            y = CrowMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            boomerangSound = true;
            owner.selectedVariant[slot] = CrowMotion.flightVariant(y, owner.speedY[slot]);
            // $5E54 reads the cached active position. The movement helpers
            // update WRAM positions without refreshing those HRAM bytes.
            unloaded = ((entity.y() - entity.z()) & 0xFF) >= 0x88 || entity.x() >= 0xA8;

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.CrowMotion.Update run(
                linksawakening.world.entity.RoomEntity entity,
                int transitionCountdown,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                boolean allowLinkProximityTrigger,
                boolean externalTakeoffTrigger) {
            if (inUse) {
                return new CrowStep(owner)
                        .run(
                                entity,
                                transitionCountdown,
                                frameCounter,
                                linkEntityX,
                                linkEntityY,
                                allowLinkProximityTrigger,
                                externalTakeoffTrigger);
            }
            inUse = true;
            slot = 0;
            countdown = 0;
            x = 0;
            y = 0;
            z = 0;
            variant = 0;
            boomerangSound = false;
            unloaded = false;
            updated = null;
            completedResult = null;

            this.entity = entity;
            this.transitionCountdown = transitionCountdown;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.allowLinkProximityTrigger = allowLinkProximityTrigger;
            this.externalTakeoffTrigger = externalTakeoffTrigger;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
