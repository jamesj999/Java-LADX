package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for FishMotion.advance; values are assigned in source order. */
final class FishFrameStep {
    private FishMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int transitionCountdown;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int countdown;
    private int x;
    private boolean horizontalCollision;
    private int z;
    private int variant;
    private boolean waterSplash;
    private linksawakening.world.entity.RoomEntity updated;

    FishFrameStep() {}

    linksawakening.world.entity.motion.FishMotion.Update run() {
        if ((entity.type() & 0xFF) != FishMotion.ENTITY_TYPE) {
            throw new IllegalArgumentException(
                    "Unsupported Fish motion entity type: 0x" + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Fish random-byte supplier cannot be null");
        }

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        countdown = transitionCountdown & 0xFF;
        x = FishMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        horizontalCollision = false;
        if (owner.speedX[slot] != 0
                && backgroundCollision != null
                && backgroundCollision.blocks(
                        entity, FishMotion.horizontalDirection(owner.speedX[slot]), x, entity.y())) {
            x = entity.x() & 0xFF;
            horizontalCollision = true;
        }

        z = entity.z() & 0xFF;
        variant = entity.spriteVariant();
        waterSplash = false;
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
            default -> throw new IllegalStateException("Invalid Fish state: " + owner.state[slot]);
        }

        updated = EntitySnapshots.withMotion(
                entity,
                x,
                entity.y(),
                variant,
                entity.entityFlipAttribute(),
                z,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
        return new FishMotion.Update(updated, countdown, owner.physicsFlags[slot], waterSplash);
    }

    private linksawakening.world.entity.motion.FishMotion.Update completedResult;

    private boolean advanceState0() {
        owner.speedX[slot] = 0x08;
        countdown = 0x40 + (randomByteSupplier.getAsInt() & 0x7F);
        owner.state[slot] = 1;

        return true;
    }

    private boolean advanceState1() {
        if (countdown != 0) {
            if (horizontalCollision) {
                owner.speedX[slot] = FishMotion.negate(owner.speedX[slot]);
            }
            variant = owner.speedX[slot] < 0x80 ? 0x06 : 0x05;
        } else {
            owner.physicsFlags[slot] = FishMotion.JUMP_PHYSICS_FLAGS;
            owner.speedZ[slot] = 0x18;
            owner.state[slot] = 2;
            owner.speedX[slot] = (owner.speedX[slot] << 1) & 0xFF;
            waterSplash = true;
        }

        return true;
    }

    private boolean advanceState2() {
        z = FishMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        if ((z & 0x80) != 0) {
            z = 0;
            waterSplash = true;
            countdown = 0x50 + (randomByteSupplier.getAsInt() & 0x7F);
            owner.speedX[slot] = FishMotion.arithmeticShiftRight(owner.speedX[slot]);
            owner.physicsFlags[slot] = FishMotion.INITIAL_PHYSICS_FLAGS;
            owner.state[slot] = 1;
        } else {
            variant = FishMotion.signedByte(owner.speedX[slot]) < 0 ? 0x01 : 0x03;
            if (FishMotion.signedByte(owner.speedZ[slot]) < 0) {
                variant++;
            }
        }

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.FishMotion.Update run(
            FishMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int transitionCountdown,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {
        if (inUse) {
            return new FishFrameStep().run(owner, entity, transitionCountdown, randomByteSupplier, backgroundCollision);
        }
        inUse = true;
        slot = 0;
        countdown = 0;
        x = 0;
        horizontalCollision = false;
        z = 0;
        variant = 0;
        waterSplash = false;
        updated = null;
        completedResult = null;
        this.owner = owner;
        this.entity = entity;
        this.transitionCountdown = transitionCountdown;
        this.randomByteSupplier = randomByteSupplier;
        this.backgroundCollision = backgroundCollision;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.randomByteSupplier = null;
            this.backgroundCollision = null;
            this.updated = null;
            this.completedResult = null;
        }
    }
}
