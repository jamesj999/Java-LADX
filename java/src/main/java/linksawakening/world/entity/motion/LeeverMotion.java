package linksawakening.world.entity.motion;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$04 LeeverEntityHandler's hide, emerge, chase, and burrow states. */
public final class LeeverMotion {
    static final int[] EMERGING_VARIANTS = {0x01, 0x00};
    static final int[] BURROWING_VARIANTS = {0x00, 0x01};

    private final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        return leeverFrameStep.run(
                entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    public boolean isChasing(int slot) {
        return state[slot] == 2;
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

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        boolean swapped = Math.abs(distanceX) < Math.abs(distanceY);
        int smallerDistance = Math.min(Math.abs(distanceX), Math.abs(distanceY));
        int largerDistance = Math.max(Math.abs(distanceX), Math.abs(distanceY));
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

    static int directionForX(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    static int directionForY(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    record Vector(int x, int y) {}

    private final LeeverFrameStep leeverFrameStep = new LeeverFrameStep(this);

    static final class LeeverFrameStep {
        private final LeeverMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int slot;
        private int x;
        private int y;
        private int variant;

        LeeverFrameStep(LeeverMotion owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.RoomEntity run() {
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }
            if (owner.transitionCountdown[slot] > 0) {
                owner.transitionCountdown[slot]--;
            }

            x = LeeverMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
            y = LeeverMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);

            // ApplyEntityInteractionWithBackground restores blocked coordinates
            // before the Leever state handler; it does not reverse ordinary speed.
            {
                if (x != entity.x()
                        && backgroundCollision.blocks(entity, LeeverMotion.directionForX(owner.speedX[slot]), x, y)) {
                    x = entity.x();
                }
                if (y != entity.y()
                        && backgroundCollision.blocks(entity, LeeverMotion.directionForY(owner.speedY[slot]), x, y)) {
                    y = entity.y();
                }
            }

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
                default -> throw new IllegalStateException("Invalid Leever state: " + owner.state[slot]);
            }

            return EntitySnapshots.withMotion(
                    entity, x, y, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity completedResult;

        private boolean advanceState0() {
            variant = -1;
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x1F;
                owner.state[slot] = 1;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
            }

            return true;
        }

        private boolean advanceState1() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x70 + (randomByteSupplier.getAsInt() & 0x3F);
                owner.state[slot] = 2;
            } else {
                variant = LeeverMotion.EMERGING_VARIANTS[
                        Math.min(owner.transitionCountdown[slot] >>> 4, LeeverMotion.EMERGING_VARIANTS.length - 1)];
            }

            return true;
        }

        private boolean advanceState2() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x1F;
                owner.state[slot] = 3;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
            } else {
                int frame = frameCounter & 0xFF;
                if (((frame ^ slot) & 0x0F) == 0) {
                    LeeverMotion.Vector vector =
                            LeeverMotion.vectorTowardsLink(x, y, entity.z(), linkEntityX, linkEntityY, 0x08);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                }
                // SetEntitySpriteVariant followed by two INC [HL] writes
                // selects the lower pair of the four-entry display list.
                variant = (((frame ^ slot) >>> 2) & 0x01) + 0x02;
            }

            return true;
        }

        private boolean advanceState3() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x30 + (randomByteSupplier.getAsInt() & 0x1F);
                owner.state[slot] = 0;
                LeeverMotion.Vector vector =
                        LeeverMotion.vectorTowardsLink(x, y, entity.z(), linkEntityX, linkEntityY, 0x08);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
            } else {
                variant = LeeverMotion.BURROWING_VARIANTS[
                        Math.min(owner.transitionCountdown[slot] >>> 4, LeeverMotion.BURROWING_VARIANTS.length - 1)];
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.RoomEntity run(
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                IntSupplier randomByteSupplier,
                linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {
            if (inUse) {
                return new LeeverFrameStep(owner)
                        .run(entity, frameCounter, linkEntityX, linkEntityY, randomByteSupplier, backgroundCollision);
            }
            inUse = true;
            slot = 0;
            x = 0;
            y = 0;
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
