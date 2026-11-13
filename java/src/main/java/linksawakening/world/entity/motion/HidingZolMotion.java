package linksawakening.world.entity.motion;

import java.util.function.IntSupplier;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

/** Bank-$07 Hiding Zol reveal, bounce, and hide state machine. */
public final class HidingZolMotion {
    static final int ENTITY_HIDING_ZOL = 0x9B;

    final int[] state = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateState1 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] swordCollisionEnabled = new boolean[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] linkCollisionEnabled = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        swordCollisionEnabled[slot] = false;
        linkCollisionEnabled[slot] = false;
        initialized[slot] = true;
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundCollision
                    backgroundCollision) {
        RoomEntityBackgroundInteraction backgroundInteraction =
                backgroundCollision == null ? null : RoomEntityBackgroundInteraction.fromBoolean(backgroundCollision);
        return advance(entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundInteraction, 0);
    }

    public Update advance(
            RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundInteraction
                    backgroundInteraction,
            int frameCounter) {
        return hidingZolStep.run(
                entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundInteraction, frameCounter);
    }

    public boolean allowsSwordCollision(int slot) {
        return swordCollisionEnabled[slot];
    }

    public boolean allowsLinkCollision(int slot) {
        return linkCollisionEnabled[slot];
    }

    public void clear(int slot) {
        state[slot] = 0;
        transitionCountdown[slot] = 0;
        privateState1[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        swordCollisionEnabled[slot] = false;
        linkCollisionEnabled[slot] = false;
        initialized[slot] = false;
    }

    public int state(int slot) {
        return state[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int privateState1(int slot) {
        return privateState1[slot];
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

    void decrementTransitionCountdown(int slot) {
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
    }

    static boolean isWithinRevealWindow(int entityX, int entityY, int linkX, int linkY) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY) & 0xFF);
        return distanceX >= -0x20 && distanceX < 0x20 && distanceY >= -0x20 && distanceY < 0x20;
    }

    int[] move(
            RoomEntity entity,
            int x,
            int y,
            /* Optional room geometry/history; null preserves the path without that room input. */ RoomEntityBackgroundInteraction
                    backgroundInteraction,
            int frameCounter,
            int slot) {
        int movedX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        if (movedX != x && backgroundInteraction != null) {
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, signedByte(speedX[slot]) < 0 ? 1 : 0, movedX, y, 0x03, frameCounter);
            if (result.blocked()) {
                movedX = x;
            }
        }
        int movedY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        if (movedY != y && backgroundInteraction != null) {
            EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                    entity, signedByte(speedY[slot]) < 0 ? 2 : 3, movedX, movedY, 0x03, frameCounter);
            if (result.blocked()) {
                movedY = y;
            }
        }
        return new int[] {movedX, movedY};
    }

    static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }
        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    static Vector vectorTowardsLink(int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = signedByte((linkX - entityX) & 0xFF);
        int distanceY = signedByte((linkY - entityY + entityZ) & 0xFF);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
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

    private static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    public record Update(
            RoomEntity entity,
            boolean hitGround,
            boolean clearsIgnoreHitsCountdown,
            boolean jumpJingle,
            int physicsFlags) {}

    record Vector(int x, int y) {}

    final HidingZolStep hidingZolStep = new HidingZolStep(this);

    static final class HidingZolStep {
        private final HidingZolMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int linkEntityX;
        private int linkEntityY;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        // Optional room geometry/history; null preserves the path without that room input.
        private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
        private int frameCounter;
        private int slot;
        private int z;
        private boolean hitGround;
        private int x;
        private int y;
        private int variant;
        private boolean clearsIgnoreHitsCountdown;
        private boolean jumpJingle;
        private int physicsFlags;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity updated;

        HidingZolStep(HidingZolMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.HidingZolMotion.Update run() {
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }

            // State 3 calls label_3B70, which enters the sword/damage continuation
            // without the Link-contact prelude. States 4 and 5 call the complete
            // DefaultEnemyDamageCollisionHandler.
            owner.swordCollisionEnabled[slot] = owner.state[slot] >= 3 && owner.state[slot] <= 5;
            owner.linkCollisionEnabled[slot] = owner.state[slot] >= 4 && owner.state[slot] <= 5;

            owner.decrementTransitionCountdown(slot);
            z = HidingZolMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
            hitGround = (z & 0x80) != 0;
            if (hitGround) {
                z = 0;
                owner.speedZ[slot] = 0;
            }

            x = entity.x();
            y = entity.y();
            variant = entity.spriteVariant();
            clearsIgnoreHitsCountdown = false;
            jumpJingle = false;
            physicsFlags = -1;
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
                case 6 -> {
                    if (!advanceState6()) return completedResult;
                }
                default -> {
                    if (!advanceDefault()) return completedResult;
                }
            }

            updated = EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
            return new HidingZolMotion.Update(updated, hitGround, clearsIgnoreHitsCountdown, jumpJingle, physicsFlags);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.HidingZolMotion.Update completedResult;

        private boolean advanceState0() {
            if (owner.transitionCountdown[slot] == 0
                    && HidingZolMotion.isWithinRevealWindow(entity.x(), entity.y(), linkEntityX, linkEntityY)) {
                owner.state[slot] = 1;
                owner.transitionCountdown[slot] = 0x20;
                owner.privateState1[slot] = 3 + (randomByteSupplier.getAsInt() & 0x03);
            }

            return true;
        }

        private boolean advanceState1() {
            if (owner.transitionCountdown[slot] == 0) {
                z = 0x08;
                owner.speedZ[slot] = 0x08;
                variant = 3;
                owner.state[slot] = 2;
                jumpJingle = true;
                physicsFlags = 0x12;
            } else {
                variant = owner.transitionCountdown[slot] >= 0x10 ? 1 : 2;
            }

            return true;
        }

        private boolean advanceState2() {
            if (hitGround) {
                owner.transitionCountdown[slot] = 0x20;
                owner.state[slot] = 3;
            }

            return true;
        }

        private boolean advanceState3() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x10;
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.state[slot] = 4;
            } else {
                owner.speedX[slot] = (owner.transitionCountdown[slot] & 0x04) == 0 ? 0x08 : 0xF8;
                x = HidingZolMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            }
            variant = 3;

            return true;
        }

        private boolean advanceState4() {
            int[] moved = owner.move(entity, x, y, backgroundInteraction, frameCounter, slot);
            x = moved[0];
            y = moved[1];
            clearsIgnoreHitsCountdown = true;
            if (owner.transitionCountdown[slot] == 0) {
                HidingZolMotion.Vector vector =
                        HidingZolMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x0C);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.speedZ[slot] = 0x18;
                owner.state[slot] = 5;
                jumpJingle = true;
            }
            variant = 3;

            return true;
        }

        private boolean advanceState5() {
            int[] moved = owner.move(entity, x, y, backgroundInteraction, frameCounter, slot);
            x = moved[0];
            y = moved[1];
            clearsIgnoreHitsCountdown = true;
            if (hitGround) {
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.state[slot] = 4;
                owner.transitionCountdown[slot] = 0x20;
                owner.privateState1[slot]--;
                if (owner.privateState1[slot] == 0) {
                    owner.state[slot] = 6;
                    owner.transitionCountdown[slot] = 0x30;
                    physicsFlags = 0xD2;
                }
            }
            variant = 2;

            return true;
        }

        private boolean advanceState6() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x50;
                owner.state[slot] = 0;
                variant = 0;
            } else if (owner.transitionCountdown[slot] >= 0x20) {
                variant = 3;
            } else if (owner.transitionCountdown[slot] >= 0x10) {
                variant = 2;
            } else {
                variant = 1;
            }

            return true;
        }

        private boolean advanceDefault() {
            owner.state[slot] = 0;
            owner.transitionCountdown[slot] = 0;
            variant = 0;

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.HidingZolMotion.Update run(
                linksawakening.world.entity.RoomEntity entity,
                int linkEntityX,
                int linkEntityY,
                java.util.function.IntSupplier randomByteSupplier,
                /* Optional room geometry/history; null preserves the path without that room input. */ linksawakening
                                .world.entity.RoomEntityBackgroundInteraction
                        backgroundInteraction,
                int frameCounter) {
            if (inUse) {
                return new HidingZolStep(owner)
                        .run(entity, linkEntityX, linkEntityY, randomByteSupplier, backgroundInteraction, frameCounter);
            }
            inUse = true;
            slot = 0;
            z = 0;
            hitGround = false;
            x = 0;
            y = 0;
            variant = 0;
            clearsIgnoreHitsCountdown = false;
            jumpJingle = false;
            physicsFlags = 0;
            updated = null;
            completedResult = null;

            this.entity = entity;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.randomByteSupplier = randomByteSupplier;
            this.backgroundInteraction = backgroundInteraction;
            this.frameCounter = frameCounter;
            try {
                return run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;
                this.backgroundInteraction = null;
                this.updated = null;
                this.completedResult = null;
            }
        }
    }
}
