package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/** Bank-$18 BomberEntityHandler state and movement. */
public final class BomberMotion {
    static final int[] Z_BY_INERTIA = {0x10, 0x11, 0x12, 0x11};
    static final int[] SPEED_X_BY_RANDOM = {0x00, 0x06, 0x08, 0x06, 0x00, 0xFA, 0xF8, 0xFA};

    // Data_018_7865 is immediately before Data_018_7867. The source indexes
    // the contiguous bytes with the same 0..7 value.
    static final int[] SPEED_Y_BY_RANDOM = {0xF8, 0xFA, 0x00, 0x06, 0x08, 0x06, 0x00, 0xFA};

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record BombSpawn(int x, int y, int z, int speedX, int speedY, int speedZ, int transitionCountdown) {}

    public record Update(RoomEntity entity, int transitionCountdown, BombSpawn bombSpawn, boolean playJingle) {}

    public void initialize(int slot) {
        state[slot] = 0;
        privateState1[slot] = 0;
        inertia[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int transitionCountdown,
            int linkEntityX,
            int linkEntityY,
            int romLinkDirection,
            boolean swordAnimationActive,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return advance(new BomberFrame(
                entity,
                transitionCountdown,
                linkEntityX,
                linkEntityY,
                romLinkDirection,
                swordAnimationActive,
                randomByteSupplier,
                backgroundCollision));
    }

    public linksawakening.world.entity.motion.BomberMotion.Update advance(BomberFrame inputs) {
        return bomberFrameStep.run(this, inputs);
    }

    public void clear(int slot) {
        initialize(slot);
        initialized[slot] = false;
    }

    int inertia(int slot) {
        return inertia[slot] & 0xFF;
    }

    static boolean matchesSwordAttackWindow(
            RoomEntity entity, int linkEntityX, int linkEntityY, int romLinkDirection, boolean swordAnimationActive) {
        if (!swordAnimationActive
                || directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
                        != ((romLinkDirection ^ 0x01) & 0x03)) {
            return false;
        }
        int xDistance = unsignedByteAbs(entity.x() - linkEntityX);
        int visualY = (entity.y() - entity.z()) & 0xFF;
        int linkVisualY = linkEntityY & 0xFF;
        int yDistance = unsignedByteAbs(visualY - linkVisualY);
        return xDistance < 0x20 && yDistance < 0x20;
    }

    private static int directionToLink(int entityX, int entityY, int linkX, int linkY) {
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
        // GetVectorTowardsLink uses the UP branch for a zero Y distance.
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

    static int horizontalDirection(int speed) {
        return (speed & 0x80) != 0 ? 1 : 0;
    }

    static int verticalDirection(int speed) {
        return (speed & 0x80) != 0 ? 2 : 3;
    }

    static RoomEntity withPosition(RoomEntity entity, int x, int y, int z) {
        return EntitySnapshots.withMotion(
                entity, x, y, entity.spriteVariant(), entity.entityFlipAttribute(), z, -1, false);
    }

    static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int unsignedByteAbs(int value) {
        int difference = value & 0xFF;
        return difference < 0x80 ? difference : 0x100 - difference;
    }

    record Vector(int x, int y) {}

    final BomberFrameStep bomberFrameStep = new BomberFrameStep();

    record BomberFrame(
            linksawakening.world.entity.RoomEntity entity,
            int transitionCountdown,
            int linkEntityX,
            int linkEntityY,
            int romLinkDirection,
            boolean swordAnimationActive,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {}

    static final class BomberFrameStep {
        private BomberMotion owner;
        private linksawakening.world.entity.motion.BomberMotion.BomberFrame inputs;
        private linksawakening.world.entity.RoomEntity entity;
        private int transitionCountdown;
        private int linkEntityX;
        private int linkEntityY;
        private int romLinkDirection;
        private boolean swordAnimationActive;
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        private int slot;
        private int x;
        private int y;
        private int z;
        private linksawakening.world.entity.RoomEntity moved;
        private int nextTransitionCountdown;
        private int oldInertia;
        private linksawakening.world.entity.motion.BomberMotion.BombSpawn bombSpawn;
        private boolean playJingle;

        BomberFrameStep() {}

        linksawakening.world.entity.motion.BomberMotion.Update run() {
            entity = inputs.entity();
            transitionCountdown = inputs.transitionCountdown();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            romLinkDirection = inputs.romLinkDirection();
            swordAnimationActive = inputs.swordAnimationActive();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();

            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            x = BomberMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
            y = BomberMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
            if (backgroundCollision != null) {
                if (x != entity.x()
                        && backgroundCollision.blocks(
                                entity, BomberMotion.horizontalDirection(owner.speedX[slot]), x, entity.y())) {
                    x = entity.x();
                    owner.speedX[slot] = 0;
                }
                if (y != entity.y()
                        && backgroundCollision.blocks(
                                entity, BomberMotion.verticalDirection(owner.speedY[slot]), x, y)) {
                    y = entity.y();
                    owner.speedY[slot] = 0;
                }
            }

            z = BomberMotion.Z_BY_INERTIA[(owner.inertia[slot] >>> 3) & 0x03];
            moved = BomberMotion.withPosition(entity, x, y, z);
            nextTransitionCountdown = transitionCountdown & 0xFF;

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
                default -> owner.state[slot] = 0;
            }

            oldInertia = owner.inertia[slot];
            owner.inertia[slot] = (oldInertia + 1) & 0xFF;
            bombSpawn = null;
            playJingle = false;
            if ((oldInertia & 0x7F) == 0) {
                BomberMotion.Vector vector =
                        BomberMotion.vectorTowardsLink(moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x10);
                bombSpawn =
                        new BomberMotion.BombSpawn(moved.x(), moved.y(), moved.z(), vector.x(), vector.y(), 0x08, 0x40);
                playJingle = true;
            }

            if (BomberMotion.matchesSwordAttackWindow(
                    moved, linkEntityX, linkEntityY, romLinkDirection, swordAnimationActive)) {
                owner.state[slot] = 2;
                nextTransitionCountdown = 0x12;
                BomberMotion.Vector vector =
                        BomberMotion.vectorTowardsLink(moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x20);
                owner.speedX[slot] = (-BomberMotion.signedByte(vector.x())) & 0xFF;
                owner.speedY[slot] = (-BomberMotion.signedByte(vector.y())) & 0xFF;
            }

            return new BomberMotion.Update(moved, nextTransitionCountdown, bombSpawn, playJingle);
        }

        private linksawakening.world.entity.motion.BomberMotion.Update completedResult;

        private boolean advanceState0() {
            if (nextTransitionCountdown == 0) {
                int random = randomByteSupplier.getAsInt() & 0xFF;
                nextTransitionCountdown = 0x20 + (random & 0x1F);
                int direction = random & 0x07;
                owner.speedX[slot] = BomberMotion.SPEED_X_BY_RANDOM[direction];
                owner.speedY[slot] = BomberMotion.SPEED_Y_BY_RANDOM[direction];
                owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0xFF;
                if ((owner.privateState1[slot] & 0x07) == 0) {
                    BomberMotion.Vector vector = BomberMotion.vectorTowardsLink(
                            moved.x(), moved.y(), moved.z(), linkEntityX, linkEntityY, 0x0A);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                }
                owner.state[slot] = 1;
            }

            return true;
        }

        private boolean advanceState1() {
            if (nextTransitionCountdown == 0) {
                nextTransitionCountdown = 0x20;
                owner.state[slot] = 0;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
            }

            return true;
        }

        private boolean advanceState2() {
            if (nextTransitionCountdown == 0) {
                owner.state[slot] = 0;
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.BomberMotion.Update run(
                BomberMotion owner, linksawakening.world.entity.motion.BomberMotion.BomberFrame inputs) {
            if (inUse) {
                return new BomberFrameStep().run(owner, inputs);
            }
            inUse = true;
            entity = null;
            transitionCountdown = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            romLinkDirection = 0;
            swordAnimationActive = false;
            randomByteSupplier = null;
            backgroundCollision = null;
            slot = 0;
            x = 0;
            y = 0;
            z = 0;
            moved = null;
            nextTransitionCountdown = 0;
            oldInertia = 0;
            bombSpawn = null;
            playJingle = false;
            completedResult = null;
            this.owner = owner;
            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;
                this.owner = null;
                this.inputs = null;
                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundCollision = null;
                this.moved = null;
                this.bombSpawn = null;
                this.completedResult = null;
            }
        }
    }
}
