package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.RoomEntity;

/** Bank-$07 PincerEntityHandler's hidden, lunge, and return state machine. */
public final class PincerMotion {
    static final int ENTITY_TYPE = 0xB0;
    static final int PROJECTILE_NOCLIP = 0x40;
    private static final int[] HORIZONTAL_LUNGE_DIRECTIONS = {
        0, 0, 1, 1, 1, 2, 2, 2,
        0, 0, 15, 15, 15, 14, 14, 14,
        8, 8, 7, 7, 7, 6, 6, 6,
        8, 8, 9, 9, 9, 10, 10, 10
    };
    private static final int[] VERTICAL_LUNGE_DIRECTIONS = {
        4, 4, 3, 3, 3, 2, 2, 2,
        12, 12, 13, 13, 13, 14, 14, 14,
        4, 4, 5, 5, 5, 6, 6, 6,
        12, 12, 11, 11, 11, 10, 10, 10
    };

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] holeX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] holeY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] lungeVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public record Update(RoomEntity entity, int state, int transitionCountdown, int physicsFlags) {}

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    public Update advance(RoomEntity entity, int transitionCountdown, int physicsFlags, int linkX, int linkY) {
        return pincerStep.run(this, entity, transitionCountdown, physicsFlags, linkX, linkY);
    }

    public int state(int slot) {
        return state[slot];
    }

    public int holeX(int slot) {
        return holeX[slot] & 0xFF;
    }

    public int holeY(int slot) {
        return holeY[slot] & 0xFF;
    }

    public int lungeVariant(int slot) {
        return lungeVariant[slot];
    }

    public int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    public int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public void clear(int slot) {
        state[slot] = 0;
        holeX[slot] = 0;
        holeY[slot] = 0;
        lungeVariant[slot] = 2;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        initialized[slot] = false;
    }

    static int lungeVariantForVector(int vectorY, int vectorX) {
        int y = signedByte(vectorY);
        int x = signedByte(vectorX);
        int row = (y < 0 ? 0x08 : 0) | (x < 0 ? 0x10 : 0);
        int absoluteY = Math.abs(y);
        int absoluteX = Math.abs(x);
        int direction;
        if (absoluteY < absoluteX) {
            direction = HORIZONTAL_LUNGE_DIRECTIONS[row + (absoluteY >> 2)];
        } else {
            direction = VERTICAL_LUNGE_DIRECTIONS[row + (absoluteX >> 2)];
        }
        return (direction >> 1) + 2;
    }

    static Vector vectorTowards(int entityX, int entityY, int entityZ, int targetX, int targetY, int length) {
        int distanceY = signedByte(targetY - entityY + entityZ);
        int distanceX = signedByte(targetX - entityX);
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

    Position addSpeed(int x, int y, int slot) {
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        return new Position(nextX, nextY);
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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

    static boolean withinSignedWindow(int difference, int radius) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -radius && signedDifference < radius;
    }

    static boolean withinReturnWindow(int difference) {
        int signedDifference = signedByte(difference);
        return signedDifference >= -2 && signedDifference < 2;
    }

    private static int signedByte(int value) {
        int byteValue = value & 0xFF;
        return byteValue < 0x80 ? byteValue : byteValue - 0x100;
    }

    record Vector(int y, int x) {}

    record Position(int x, int y) {}

    final PincerStep pincerStep = new PincerStep();

    static final class PincerStep {
        private PincerMotion owner;
        private linksawakening.world.entity.RoomEntity entity;
        private int transitionCountdown;
        private int physicsFlags;
        private int linkX;
        private int linkY;
        private int slot;
        private int x;
        private int y;
        private int variant;
        private int nextCountdown;
        private int nextPhysicsFlags;
        private linksawakening.world.entity.RoomEntity updated;

        PincerStep() {}

        linksawakening.world.entity.motion.PincerMotion.Update run() {
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            x = entity.x();
            y = entity.y();
            variant = entity.spriteVariant();
            nextCountdown = transitionCountdown & 0xFF;
            nextPhysicsFlags = physicsFlags & 0xFF;
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
                case 4 -> {
                    if (!advanceState4()) return completedResult;
                }
                case 5 -> {
                    if (!advanceState5()) return completedResult;
                }
                default -> {
                    if (!advanceDefault()) return completedResult;
                }
            }

            updated = new RoomEntity(
                    entity.slot(),
                    entity.sourceLoadOrder(),
                    PincerMotion.ENTITY_TYPE,
                    x & 0xFF,
                    y & 0xFF,
                    entity.status(),
                    entity.spriteDefinition(),
                    variant,
                    entity.entityFlipAttribute(),
                    entity.spriteTileOffset(),
                    entity.z(),
                    -1,
                    false);
            return new PincerMotion.Update(updated, owner.state[slot], nextCountdown, nextPhysicsFlags);
        }

        private linksawakening.world.entity.motion.PincerMotion.Update completedResult;

        private boolean advanceState0() {
            owner.holeX[slot] = x;
            owner.holeY[slot] = y;
            owner.state[slot] = 1;

            return true;
        }

        private boolean advanceState1() {
            variant = 0;
            if (nextCountdown == 0) {
                nextPhysicsFlags |= PincerMotion.PROJECTILE_NOCLIP;
                if (PincerMotion.withinSignedWindow(linkX - x, 0x20)
                        && PincerMotion.withinSignedWindow(linkY - y, 0x20)) {
                    nextCountdown = 0x30;
                    owner.state[slot] = 2;
                }
            }

            return true;
        }

        private boolean advanceState2() {
            if (nextCountdown == 0) {
                variant = owner.lungeVariant[slot];
                nextCountdown = 0x18;
                nextPhysicsFlags &= ~PincerMotion.PROJECTILE_NOCLIP;
                owner.state[slot] = 3;
            } else {
                variant = 1;
                if (nextCountdown == 0x10) {
                    PincerMotion.Vector direction = PincerMotion.vectorTowards(x, y, entity.z(), linkX, linkY, 0x1F);
                    owner.lungeVariant[slot] = PincerMotion.lungeVariantForVector(direction.y(), direction.x());
                    PincerMotion.Vector speed = PincerMotion.vectorTowards(x, y, entity.z(), linkX, linkY, 0x18);
                    owner.speedY[slot] = speed.y();
                    owner.speedX[slot] = speed.x();
                }
            }

            return true;
        }

        private boolean advanceState3() {
            PincerMotion.Position position = owner.addSpeed(x, y, slot);
            x = position.x();
            y = position.y();
            if (nextCountdown == 0) {
                nextCountdown = 0x20;
                owner.state[slot] = 4;
            }
            variant = owner.lungeVariant[slot];

            return true;
        }

        private boolean advanceState4() {
            if (nextCountdown == 0) {
                owner.state[slot] = 5;
            }
            variant = owner.lungeVariant[slot];

            return true;
        }

        private boolean advanceState5() {
            PincerMotion.Vector returnSpeed =
                    PincerMotion.vectorTowards(x, y, entity.z(), owner.holeX[slot], owner.holeY[slot], 0x10);
            owner.speedY[slot] = returnSpeed.y();
            owner.speedX[slot] = returnSpeed.x();
            boolean atHole = PincerMotion.withinReturnWindow(owner.holeX[slot] - x)
                    && PincerMotion.withinReturnWindow(owner.holeY[slot] - y);
            if (atHole) {
                x = owner.holeX[slot];
                y = owner.holeY[slot];
                variant = 0;
                owner.state[slot] = 1;
                nextCountdown = 0x20;
            }
            PincerMotion.Position position = owner.addSpeed(x, y, slot);
            x = position.x();
            y = position.y();
            if (!atHole) {
                variant = owner.lungeVariant[slot];
            }

            return true;
        }

        private boolean advanceDefault() {
            owner.state[slot] = 0;
            variant = 0;

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.PincerMotion.Update run(
                PincerMotion owner,
                linksawakening.world.entity.RoomEntity entity,
                int transitionCountdown,
                int physicsFlags,
                int linkX,
                int linkY) {
            if (inUse) {
                return new PincerStep().run(owner, entity, transitionCountdown, physicsFlags, linkX, linkY);
            }
            inUse = true;
            slot = 0;
            x = 0;
            y = 0;
            variant = 0;
            nextCountdown = 0;
            nextPhysicsFlags = 0;
            updated = null;
            completedResult = null;
            this.owner = owner;
            this.entity = entity;
            this.transitionCountdown = transitionCountdown;
            this.physicsFlags = physicsFlags;
            this.linkX = linkX;
            this.linkY = linkY;
            try {
                return run();
            } finally {
                inUse = false;
                this.owner = null;
                this.entity = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
