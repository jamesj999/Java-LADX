package linksawakening.world.entity.motion;

import java.util.Objects;

/** Reused, reentrancy-safe scratch for WitchRatMotion.advance; values are assigned in source order. */
final class WitchRatFrameStep {
    private WitchRatMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int transitionCountdown;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int slot;
    private int countdown;
    private int renderedVariant;
    private int z;
    private boolean wrappedZ;
    private int collisionFlags;
    private boolean appliesBackgroundInteraction;

    WitchRatFrameStep() {}

    linksawakening.world.entity.motion.WitchRatMotion.Update run() {
        WitchRatMotion.checkType(entity);
        WitchRatMotion.validateByte(frameCounter, "Witch Rat frame counter");
        WitchRatMotion.validateByte(transitionCountdown, "Witch Rat transition countdown");
        Objects.requireNonNull(randomByteSupplier, "Witch Rat random-byte supplier");

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        countdown = transitionCountdown & 0xFF;
        renderedVariant = owner.storedSpriteVariant[slot] + (owner.direction[slot] == 0 ? 0x02 : 0x00);
        z = WitchRatMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
        wrappedZ = (z & 0x80) != 0;
        if (wrappedZ) {
            z = 0;
            owner.speedZ[slot] = 0;
        }

        collisionFlags = 0;
        appliesBackgroundInteraction = false;
        switch (owner.state[slot]) {
            case 0 -> {
                if (!advanceState0()) return completedResult;
            }
            case 1 -> {
                if (!advanceState1()) return completedResult;
            }
            default -> throw new IllegalStateException("Invalid Witch Rat state: " + owner.state[slot]);
        }

        // State 0 did not move in X/Y; state 1 already replaced those
        // coordinates with the background-probed values. Both paths still
        // need the top-of-handler Z update and the source-rendered variant.
        entity = WitchRatMotion.withPosition(entity, entity.x(), entity.y(), z, renderedVariant);
        return new WitchRatMotion.Update(
                entity,
                owner.state[slot],
                countdown,
                owner.direction[slot],
                owner.speedX[slot] & 0xFF,
                owner.speedY[slot] & 0xFF,
                owner.speedZ[slot] & 0xFF,
                collisionFlags,
                appliesBackgroundInteraction);
    }

    private linksawakening.world.entity.motion.WitchRatMotion.Update completedResult;

    private boolean advanceState0() {
        if (countdown == 0) {
            int index = randomByteSupplier.getAsInt() & 0x07;
            owner.speedX[slot] = WitchRatMotion.SPEEDS[index];
            owner.direction[slot] = index & 0x04;
            int yIndex = randomByteSupplier.getAsInt() & 0x07;
            owner.speedY[slot] = WitchRatMotion.SPEEDS[yIndex];
            countdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x30;
            owner.state[slot] = 1;
        } else {
            owner.storedSpriteVariant[slot] = 0x01;
            if ((frameCounter & 0x1F) == 0 && (randomByteSupplier.getAsInt() & 0x01) == 0) {
                owner.direction[slot] ^= 0x04;
            }
        }

        return true;
    }

    private boolean advanceState1() {
        WitchRatMotion.PositionAndCollision moved = owner.moveWithBackground(entity, backgroundInteraction, slot);
        entity = WitchRatMotion.withPosition(entity, moved.x(), moved.y(), z, renderedVariant);
        collisionFlags = moved.collisionFlags();
        appliesBackgroundInteraction = true;
        if (wrappedZ) {
            if (countdown == 0) {
                countdown = 0x48;
                owner.state[slot] = 0;
                // The ROM returns before its final SetEntitySpriteVariant.
                {
                    completedResult = new WitchRatMotion.Update(
                            entity,
                            owner.state[slot],
                            countdown,
                            owner.direction[slot],
                            owner.speedX[slot] & 0xFF,
                            owner.speedY[slot] & 0xFF,
                            owner.speedZ[slot] & 0xFF,
                            collisionFlags,
                            appliesBackgroundInteraction);
                    return false;
                }
            }
            owner.speedZ[slot] = 0x08;
            z = (z + 1) & 0xFF;
            entity = WitchRatMotion.withPosition(entity, moved.x(), moved.y(), z, renderedVariant);
        }
        owner.storedSpriteVariant[slot] = 0x00;

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.WitchRatMotion.Update run(
            WitchRatMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int frameCounter,
            int transitionCountdown,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction) {
        if (inUse) {
            return new WitchRatFrameStep()
                    .run(owner, entity, frameCounter, transitionCountdown, randomByteSupplier, backgroundInteraction);
        }
        inUse = true;
        slot = 0;
        countdown = 0;
        renderedVariant = 0;
        z = 0;
        wrappedZ = false;
        collisionFlags = 0;
        appliesBackgroundInteraction = false;
        completedResult = null;
        this.owner = owner;
        this.entity = entity;
        this.frameCounter = frameCounter;
        this.transitionCountdown = transitionCountdown;
        this.randomByteSupplier = randomByteSupplier;
        this.backgroundInteraction = backgroundInteraction;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.randomByteSupplier = null;
            this.backgroundInteraction = null;
            this.completedResult = null;
        }
    }
}
