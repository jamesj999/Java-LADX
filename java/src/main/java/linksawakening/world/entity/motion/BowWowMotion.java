package linksawakening.world.entity.motion;

import java.util.List;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import linksawakening.world.entity.BowWowChain;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntitySnapshots;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;

/**
 * The home, kidnapped, and following motion states of bank-$05's BowWowEntityHandler.
 *
 * <p>The original keeps Bow-Wow's setup state separate from the active
 * handler state. This class keeps those WRAM-like fields per entity slot so
 * the host renderer can consume the same position and sprite-variant result
 * without running a general Game Boy CPU.</p>
 */
public final class BowWowMotion {
    private int bowWowState;

    public int readBowWowState() {
        return bowWowState;
    }

    public int writeBowWowState(int value) {
        bowWowState = value;
        return bowWowState;
    }

    static final int[] SPEED_X_BY_RANDOM = {0x04, 0x08, 0x0C, 0x08, 0xFC, 0xF8, 0xF4, 0xF8};
    static final int[] SPEED_Y_BY_RANDOM = {0xF4, 0xF8, 0x04, 0x08, 0x0C, 0x08, 0xFC, 0xF8};

    private final BowWowChain[] chains = new BowWowChain[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] chainAvailable = new boolean[EntityRoomLoader.MAX_ENTITIES];

    {
        java.util.Arrays.setAll(chains, ignored -> new BowWowChain());
    }

    final int[] activeState = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] selectedVariant = new int[EntityRoomLoader.MAX_ENTITIES];
    // wEntitiesPrivateState4Table: setup latch, separate from active handler state.
    final int[] setupComplete = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] privateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] privateCountdown2 = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] targetX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] targetY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] targetSlot = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] speedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedZAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    final int[] updatedZ = new int[EntityRoomLoader.MAX_ENTITIES];
    final boolean[] initialized = new boolean[EntityRoomLoader.MAX_ENTITIES];

    public void initialize(int slot) {
        clear(slot);
        initialized[slot] = true;
    }

    RoomEntity advance(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision) {
        return advanceWithTargetScan(new BowWowTargetFrame(
                        entity,
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        randomByteSupplier,
                        backgroundCollision,
                        List.of(entity),
                        type -> false))
                .entity();
    }

    Update advanceWithTargetScan(BowWowTargetFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        java.util.List<RoomEntity> entities = inputs.entities();
        java.util.function.IntPredicate canEatEntity = inputs.canEatEntity();
        this.tickTimers(entity.slot());
        return this.advanceAfterTimers(new BowWowFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                backgroundCollision,
                entities,
                canEatEntity));
    }

    public Update advanceAfterTimers(BowWowFrame inputs) {
        return BowWowMotion.advanceAfterTimersBody(this, inputs);
    }

    public Update advanceAfterTimers(BowWowCollisionFrame inputs) {
        return BowWowMotion.advanceAfterTimersBody(this, inputs);
    }

    public void clear(int slot) {
        chains[slot] = new BowWowChain();
        chainAvailable[slot] = true;
        activeState[slot] = 0;
        selectedVariant[slot] = 0;
        setupComplete[slot] = 0;
        transitionCountdown[slot] = 0;
        privateCountdown1[slot] = 0;
        privateCountdown2[slot] = 0;
        targetX[slot] = 0;
        targetY[slot] = 0;
        targetSlot[slot] = -1;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedZ[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
        speedZAccumulator[slot] = 0;
        updatedZ[slot] = 0;
        initialized[slot] = false;
    }

    public RoomEntity advanceChain(RoomEntity entity, int questState, boolean linkMoving) {
        int slot = entity.slot();
        chainAvailable[slot] = true;
        return chains[slot].advance(entity, targetX[slot], targetY[slot], questState, linkMoving);
    }

    public List<BowWowChain.Entry> chainEntries(int slot, int questState, int frame) {
        return !chainAvailable[slot] ? List.of() : chains[slot].entries(slot, questState, frame);
    }

    int activeState(int slot) {
        return activeState[slot];
    }

    public int selectedVariant(int slot) {
        return selectedVariant[slot];
    }

    int privateState4(int slot) {
        return setupComplete[slot];
    }

    public int transitionCountdown(int slot) {
        return transitionCountdown[slot];
    }

    public int privateCountdown2(int slot) {
        return privateCountdown2[slot];
    }

    int targetX(int slot) {
        return targetX[slot];
    }

    int targetY(int slot) {
        return targetY[slot];
    }

    public int targetSlot(int slot) {
        return targetSlot[slot];
    }

    int speedZ(int slot) {
        return speedZ[slot] & 0xFF;
    }

    int speedX(int slot) {
        return speedX[slot] & 0xFF;
    }

    int speedY(int slot) {
        return speedY[slot] & 0xFF;
    }

    public void setTransitionCountdown(int slot, int value) {
        if (slot < 0 || slot >= transitionCountdown.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Transition countdown must be an unsigned byte: " + value);
        }
        transitionCountdown[slot] = value;
    }

    public void setPrivateCountdown2(int slot, int value) {
        if (slot < 0 || slot >= privateCountdown2.length) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if (value < 0 || value > 0xFF) {
            throw new IllegalArgumentException("Private countdown 2 must be an unsigned byte: " + value);
        }
        privateCountdown2[slot] = value;
    }

    RoomEntity setup(RoomEntity entity) {
        int slot = entity.slot();
        int x = byteValue(entity.x() + 0x04);
        int y = byteValue(entity.y() + 0x08);
        targetX[slot] = x;
        targetY[slot] = y;
        chains[slot].initialize(x, y - entity.z());
        setupComplete[slot] = 1;
        return withPositionAndVariant(entity, x, y, entity.z(), entity.spriteVariant());
    }

    public void tickTimers(int slot) {
        if (!initialized[slot]) {
            initialize(slot);
        }
        if (transitionCountdown[slot] > 0) {
            transitionCountdown[slot]--;
        }
        if (privateCountdown1[slot] > 0) {
            privateCountdown1[slot]--;
        }
        if (privateCountdown2[slot] > 0) {
            privateCountdown2[slot]--;
        }
    }

    /** Returns whether the source's gravity step crossed below signed zero. */
    boolean updateZ(int positionZ, int slot) {
        int nextZ = addSpeedToPosition(positionZ, speedZ[slot], speedZAccumulator, slot);
        speedZ[slot] = byteValue(speedZ[slot] - 0x02);
        if ((nextZ & 0x80) == 0) {
            updatedZ[slot] = nextZ;
            return false;
        }
        updatedZ[slot] = 0;
        speedZ[slot] = 0;
        return true;
    }

    int[] move(RoomEntity entity, int x, int y, int slot, RoomEntityBackgroundCollision backgroundCollision) {
        int nextX = addSpeedToPosition(x, speedX[slot], speedXAccumulator, slot);
        int nextY = addSpeedToPosition(y, speedY[slot], speedYAccumulator, slot);
        // ApplyEntityInteractionWithBackground returns before the generic wall
        // collision path when hActiveEntityType is BowWow. Its earlier ground
        // status/splash work remains outside this bounded motion port.
        return new int[] {nextX, nextY};
    }

    int correctTargetX(int nextX, int beforeX, int slot) {
        return inSignedWindow(signedByte(targetX[slot] - nextX), 0x20) ? nextX : beforeX;
    }

    int correctTargetY(int nextY, int beforeY, int slot) {
        return inSignedWindow(signedByte(targetY[slot] - nextY), 0x20) ? nextY : beforeY;
    }

    int movementVariant(int frameCounter, int slot) {
        int horizontal = Math.abs(signedByte(speedX[slot]));
        int vertical = Math.abs(signedByte(speedY[slot]));
        int base;
        if (vertical >= horizontal) {
            base = signedByte(speedY[slot]) < 0 ? 6 : 0;
        } else {
            base = signedByte(speedX[slot]) < 0 ? 2 : 4;
        }
        return base == 6 ? 6 : base + ((frameCounter >>> 3) & 0x01);
    }

    static int findTarget(
            RoomEntity source,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            List<RoomEntity> entities,
            IntPredicate canEatEntity) {
        return BowWowMotion.findTargetBody(
                source, linkEntityX, linkEntityY, randomByteSupplier, entities, canEatEntity);
    }

    static Vector vectorTowardsTarget(RoomEntity source, RoomEntity target, int sourceZ, int length) {
        int distanceX = signedByte(target.x() - source.x());
        // The shared vector helper reads Z after this handler's gravity update.
        int distanceY = signedByte(target.y() - source.y() + sourceZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = divideComponent(length, smaller, larger);
        int vectorX = swapped ? result : length;
        int vectorY = swapped ? length : result;
        if (distanceX < 0) {
            vectorX = -vectorX;
        }
        if (distanceY < 0) {
            vectorY = -vectorY;
        }
        return new Vector(vectorX & 0xFF, vectorY & 0xFF);
    }

    private static int divideComponent(int length, int smaller, int larger) {
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smaller;
            if (sum > 0xFF || sum >= larger) {
                sum -= larger;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }

    static boolean inUnsignedWindow(int difference, int offset, int limit) {
        return ((difference + offset) & 0xFF) < limit;
    }

    /** Mirrors label_005_4335's final X/visual-Y contact test. */
    static TargetContact findTargetContact(
            int sourceX, int sourceY, int sourceZ, int targetSlot, List<RoomEntity> entities) {
        if (targetSlot < 0 || targetSlot >= entities.size()) {
            return null;
        }
        RoomEntity target = entities.get(targetSlot);
        if (!target.loaded()
                || !inUnsignedWindow(sourceX - target.x(), 0x0E, 0x1A)
                || !inUnsignedWindow((sourceY - sourceZ) - target.y(), 0x10, 0x20)) {
            return null;
        }
        return new TargetContact(targetSlot, target.type());
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator, int slot) {
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
        return byteValue(position + delta);
    }

    static boolean inSignedWindow(int distance, int halfOpen) {
        return distance >= -halfOpen && distance < halfOpen;
    }

    static int signedByte(int value) {
        value &= 0xFF;
        return value < 0x80 ? value : value - 0x100;
    }

    static int byteValue(int value) {
        return value & 0xFF;
    }

    static RoomEntity withPositionAndVariant(RoomEntity entity, int x, int y, int z, int variant) {
        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
    }

    public record Update(
            RoomEntity entity,
            int targetSlot, /* Optional Bow-Wow target contact; absent when no target was reached. */
            TargetContact targetContact) {
        boolean targetAcquired() {
            return targetSlot >= 0;
        }
    }

    public record TargetContact(int slot, int type) {}

    record Vector(int x, int y) {}

    final BowWowStep bowWowStep = new BowWowStep(this);

    static BowWowMotion.Update advanceAfterTimersBody(BowWowMotion owner, BowWowFrame inputs) {
        RoomEntity entity = inputs.entity();
        int frameCounter = inputs.frameCounter();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        java.util.function.IntSupplier randomByteSupplier = inputs.randomByteSupplier();
        RoomEntityBackgroundCollision backgroundCollision = inputs.backgroundCollision();
        java.util.List<RoomEntity> entities = inputs.entities();
        java.util.function.IntPredicate canEatEntity = inputs.canEatEntity();
        return owner.advanceAfterTimers(new BowWowCollisionFrame(
                entity,
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                randomByteSupplier,
                backgroundCollision,
                entities,
                canEatEntity,
                1));
    }

    static BowWowMotion.Update advanceAfterTimersBody(BowWowMotion owner, BowWowCollisionFrame inputs) {
        return owner.bowWowStep.run(inputs);
    }

    static int findTargetBody(
            RoomEntity source,
            int linkEntityX,
            int linkEntityY,
            IntSupplier randomByteSupplier,
            List<RoomEntity> entities,
            IntPredicate canEatEntity) {
        boolean descending = (randomByteSupplier.getAsInt() & 0x01) == 0;
        int step = descending ? -1 : 1;
        int candidate = descending ? EntityRoomLoader.MAX_ENTITIES - 1 : 0;
        int end = descending ? -1 : EntityRoomLoader.MAX_ENTITIES;
        while (candidate != end) {
            if (candidate != source.slot() && candidate >= 0 && candidate < entities.size()) {
                RoomEntity target = entities.get(candidate);
                if (target.loaded()
                        && target.status() != EntityStatus.DYING
                        && target.spriteVariant() != 1
                        && canEatEntity.test(target.type())
                        && BowWowMotion.inUnsignedWindow(linkEntityX - target.x(), 0x2F, 0x5E)
                        && BowWowMotion.inUnsignedWindow(linkEntityY - target.y(), 0x2F, 0x5E)) {
                    return candidate;
                }
            }
            candidate += step;
        }
        return -1;
    }

    public record BowWowCollisionFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            java.util.List<RoomEntity> entities,
            java.util.function.IntPredicate canEatEntity,
            int bowWowState) {
        public BowWowCollisionFrame {
            backgroundCollision = RoomEntityBackgroundCollision.orOpen(backgroundCollision);
        }
    }

    record BowWowFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            java.util.List<RoomEntity> entities,
            java.util.function.IntPredicate canEatEntity) {}

    static final class BowWowStep {
        private final linksawakening.world.entity.motion.BowWowMotion owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.BowWowMotion.BowWowCollisionFrame inputs;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private int linkEntityZ;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntSupplier randomByteSupplier;
        private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.List<linksawakening.world.entity.RoomEntity> entities;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private java.util.function.IntPredicate canEatEntity;
        private int bowWowState;
        private int slot;
        private int variant;
        private boolean wasAboveGround;
        private int x;
        private int y;
        private int z;
        // Absent before initialization or while this reusable execution scratch is inactive.
        /* Optional Bow-Wow target contact; absent when no target was reached. */ private linksawakening.world.entity
                        .motion.BowWowMotion.TargetContact
                targetContact;

        BowWowStep(linksawakening.world.entity.motion.BowWowMotion owner) {
            this.owner = java.util.Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.motion.BowWowMotion.Update run() {
            entity = inputs.entity();
            frameCounter = inputs.frameCounter();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkEntityZ = inputs.linkEntityZ();
            randomByteSupplier = inputs.randomByteSupplier();
            backgroundCollision = inputs.backgroundCollision();
            entities = inputs.entities();
            canEatEntity = inputs.canEatEntity();
            bowWowState = inputs.bowWowState();
            slot = entity.slot();
            if (!owner.initialized[slot]) {
                owner.initialize(slot);
            }
            variant = owner.selectedVariant[slot];
            // $05:4060 draws the saved variant; $44CB then selects the next one
            // from the existing speeds, before gravity and state dispatch.
            if ((owner.speedX[slot] | owner.speedY[slot]) != 0) {
                owner.selectedVariant[slot] = owner.movementVariant(frameCounter, slot);
            }

            if (owner.setupComplete[slot] == 0) {
                return new BowWowMotion.Update(owner.setup(entity), owner.targetSlot[slot], null);
            }

            if (bowWowState != 0 && bowWowState != 0x80) {
                owner.targetX[slot] = BowWowMotion.byteValue(linkEntityX);
                // Kidnapped BowWow retains the pen anchor written by setup.
                // Following BowWow uses hLinkPositionZModified ($05:40E6).
                owner.targetY[slot] = BowWowMotion.byteValue(linkEntityY - linkEntityZ);
            }

            wasAboveGround = owner.updateZ(entity.z(), slot);
            x = entity.x();
            y = entity.y();
            z = owner.updatedZ[slot];
            targetContact = null;

            switch (owner.activeState[slot]) {
                case 0 -> {
                    if (!advanceState0()) return completedResult;
                }
                case 1 -> {
                    if (!advanceState1()) return completedResult;
                }
                case 2, 4 -> {
                    if (!advanceState2()) return completedResult;
                }
                case 3 -> {
                    if (!advanceState3()) return completedResult;
                }
                default -> owner.activeState[slot] = 0;
            }

            return new BowWowMotion.Update(
                    BowWowMotion.withPositionAndVariant(entity, x, y, z, variant),
                    owner.targetSlot[slot],
                    targetContact);
        }

        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.motion.BowWowMotion.Update completedResult;

        private boolean advanceState0() {
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x28;
                if (bowWowState == 0) {
                    owner.speedZ[slot] = 0x20;
                    owner.activeState[slot] = 2;
                    BowWowMotion.Vector vector = BowWowMotion.vectorTowardsTarget(
                            entity,
                            BowWowMotion.withPositionAndVariant(entity, linkEntityX, linkEntityY, 0, 0),
                            z,
                            0x20);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                    return true;
                }
                // $05:4297 skips both target scanning and its RNG read
                // while BowWow is chained inside the hideout.
                int selectedTarget = bowWowState == 0x80
                        ? -1
                        : BowWowMotion.findTarget(
                                entity, linkEntityX, linkEntityY, randomByteSupplier, entities, canEatEntity);
                if (selectedTarget >= 0) {
                    RoomEntity target = entities.get(selectedTarget);
                    owner.targetSlot[slot] = selectedTarget;
                    BowWowMotion.Vector vector = BowWowMotion.vectorTowardsTarget(entity, target, z, 0x30);
                    owner.speedX[slot] = vector.x();
                    owner.speedY[slot] = vector.y();
                    owner.speedZ[slot] = 0x10;
                    owner.activeState[slot] = 4;
                } else {
                    owner.transitionCountdown[slot] = 0x10;
                }
            } else if (owner.privateCountdown1[slot] == 0) {
                owner.privateCountdown1[slot] = 0x20 + (randomByteSupplier.getAsInt() & 0x3F);
                owner.activeState[slot] = 1;
                int direction = randomByteSupplier.getAsInt() & 0x07;
                owner.speedX[slot] = SPEED_X_BY_RANDOM[direction];
                owner.speedY[slot] = SPEED_Y_BY_RANDOM[direction];
            }

            return true;
        }

        private boolean advanceState1() {
            if (owner.privateCountdown1[slot] == 0) {
                owner.privateCountdown1[slot] = 0x20;
                // func_005_41CF increments the state pointer, then writes
                // B (zero), returning to the target-selection handler.
                owner.activeState[slot] = 0;
            }
            if (wasAboveGround) {
                owner.speedZ[slot] = 0x10;
            }
            int[] moved = owner.move(entity, x, y, slot, backgroundCollision);
            x = owner.correctTargetX(moved[0], x, slot);
            y = owner.correctTargetY(moved[1], y, slot);

            return true;
        }

        private boolean advanceState2() {
            boolean stopLunge = owner.transitionCountdown[slot] == 0;
            if (owner.transitionCountdown[slot] != 0) {
                int[] moved = owner.move(entity, x, y, slot, backgroundCollision);
                x = owner.correctTargetX(moved[0], x, slot);
                y = owner.correctTargetY(moved[1], y, slot);
                // func_005_4228 returns E>1 when either axis is restored;
                // $41F6 then enters the same pause as an expired lunge.
                stopLunge = !BowWowMotion.inSignedWindow(BowWowMotion.signedByte(owner.targetX[slot] - moved[0]), 0x20)
                        || !BowWowMotion.inSignedWindow(BowWowMotion.signedByte(owner.targetY[slot] - moved[1]), 0x20);
            }
            if (stopLunge) {
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
                owner.activeState[slot] = 3;
                owner.transitionCountdown[slot] = 0x10;
            }
            // func_005_41EB reaches label_005_4335 after every moving
            // state-2/state-4 frame, not only when the timer expires.
            if (bowWowState != 0) {
                targetContact = BowWowMotion.findTargetContact(
                        entity.x(), entity.y(), entity.z(), owner.targetSlot[slot], entities);
            }

            return true;
        }

        private boolean advanceState3() {
            if (owner.transitionCountdown[slot] == 0) {
                // func_005_420E starts a ten-frame pause, then writes B
                // (zero) back to wEntitiesStateTable for a fresh scan.
                int pause = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
                owner.transitionCountdown[slot] = bowWowState == 0 ? pause : 0x10;
                owner.activeState[slot] = 0;
            }

            return true;
        }

        private boolean inUse;

        linksawakening.world.entity.motion.BowWowMotion.Update run(
                linksawakening.world.entity.motion.BowWowMotion.BowWowCollisionFrame inputs) {
            if (inUse) {
                return new BowWowStep(owner).run(inputs);
            }
            inUse = true;
            entity = null;
            frameCounter = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            linkEntityZ = 0;
            randomByteSupplier = null;
            backgroundCollision = null;
            entities = null;
            canEatEntity = null;
            bowWowState = 0;
            slot = 0;
            variant = 0;
            wasAboveGround = false;
            x = 0;
            y = 0;
            z = 0;
            targetContact = null;
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
                this.entities = null;
                this.canEatEntity = null;
                this.targetContact = null;
                this.completedResult = null;
            }
        }
    }

    record BowWowTargetFrame(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            java.util.function.IntSupplier randomByteSupplier,
            RoomEntityBackgroundCollision backgroundCollision,
            java.util.List<RoomEntity> entities,
            java.util.function.IntPredicate canEatEntity) {}
}
