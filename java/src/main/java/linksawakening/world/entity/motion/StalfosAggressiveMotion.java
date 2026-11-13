package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$06 StalfosAggressiveEntityHandler's pursuit and jump states. */
public final class StalfosAggressiveMotion {
    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    /** Mirrors EntityInitWithRandomDirection's consumed random byte. */
    public void initialize(int slot, IntSupplier randomByteSupplier) {
        initialize(slot);
        randomByteSupplier.getAsInt();
    }

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundCollision
                    backgroundCollision) {
        return stalfosAggressiveFrameStep.run(
                this, entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
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

    static boolean distanceInJumpWindow(int distance) {
        return distance >= -0x1C && distance < 0x1C;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceY = signedByte(linkY - entityY + entityZ);
        int distanceX = signedByte(linkX - entityX);
        boolean yNegative = distanceY < 0;
        boolean xNegative = distanceX < 0;
        int absoluteY = Math.abs(distanceY);
        int absoluteX = Math.abs(distanceX);

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

    static int signedByte(int value) {
        int result = value & 0xFF;
        return result < 0x80 ? result : result - 0x100;
    }

    record Vector(int x, int y) {}

    final StalfosAggressiveFrameStep stalfosAggressiveFrameStep = new StalfosAggressiveFrameStep(this);

    static final class StalfosAggressiveFrameStep {
        private final StalfosAggressiveMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int slot;
        private int frame;
        private int x;
        private int y;
        private int z;
        private int variant;

        StalfosAggressiveFrameStep(StalfosAggressiveMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.RoomEntity run() {
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }
            if (owner.transitionCountdown[slot] > 0) {
                owner.transitionCountdown[slot]--;
            }

            frame = frameCounter & 0xFF;
            x = entity.x();
            y = entity.y();
            z = entity.z();
            variant = entity.spriteVariant();

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
                default -> throw new IllegalStateException("Invalid aggressive Stalfos state: " + owner.state[slot]);
            }

            // ApplyEntityInteractionWithBackground_06 supplies collision flags and
            // may spawn landing dust. The current room query has no equivalent
            // directional/object-under flags, so this handler leaves that side
            // effect pending instead of treating a generic collision as identical.
            return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity completedResult;

        private boolean advanceState0() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.state[slot] = 1;
            }

            return true;
        }

        private boolean advanceState1() {
            if (((frame ^ slot) & 0x03) == 0) {
                StalfosAggressiveMotion.Vector vector =
                        StalfosAggressiveMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x08);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
            }

            int distanceX = StalfosAggressiveMotion.signedByte(linkEntityX - x);
            int distanceY = StalfosAggressiveMotion.signedByte(linkEntityY - y + z);
            if (StalfosAggressiveMotion.distanceInJumpWindow(distanceX)
                    && StalfosAggressiveMotion.distanceInJumpWindow(distanceY)) {
                owner.speedZ[slot] = 0x28;
                StalfosAggressiveMotion.Vector vector =
                        StalfosAggressiveMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x10);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.state[slot] = 2;
            }

            x = StalfosAggressiveMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            y = StalfosAggressiveMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            variant = (frame >>> 2) & 0x01;

            return true;
        }

        private boolean advanceState2() {
            x = StalfosAggressiveMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            y = StalfosAggressiveMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            z = StalfosAggressiveMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
            if (owner.speedZ[slot] < 0x02) {
                owner.speedZ[slot] = 0xC0;
                owner.transitionCountdown[slot] = 0x10;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.state[slot] = 3;
            }
            variant = 2;

            return true;
        }

        private boolean advanceState3() {
            if (owner.transitionCountdown[slot] == 0) {
                z = StalfosAggressiveMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
                if (z == 0 || (z & 0x80) != 0) {
                    z = 0;
                    owner.transitionCountdown[slot] = 0x20;
                    owner.state[slot] = 0;
                    owner.speedZ[slot] = 0;
                }
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.RoomEntity run(
                StalfosAggressiveMotion owner,
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                java.util.function.IntSupplier randomByteSupplier,
                /* Optional room geometry/history; null preserves the path without that room input. */ linksawakening
                                .world.entity.RoomEntityBackgroundCollision
                        backgroundCollision) {
            if (inUse) {
                return new StalfosAggressiveFrameStep(owner)
                        .run(
                                owner,
                                entity,
                                frameCounter,
                                linkEntityX,
                                linkEntityY,
                                randomByteSupplier,
                                backgroundCollision);
            }
            inUse = true;
            slot = 0;
            frame = 0;
            x = 0;
            y = 0;
            z = 0;
            variant = 0;
            completedResult = null;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.randomByteSupplier = randomByteSupplier;
            this.backgroundCollision = backgroundCollision;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.completedResult = null;
            }
        }
    }
}
