package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Per-invocation scratch for SpikedBeetleMotion.advance; values are assigned in source order. */
final class SpikedBeetleFrameStep {
    private final SpikedBeetleMotion owner;
    private linksawakening.world.entity.motion.SpikedBeetleFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int transitionCountdown;
    private int ignoreHitsCountdown;
    private int slot;
    private int countdown;
    private int x;
    private int y;
    private int z;
    private boolean landed;
    private int renderedVariant;
    private linksawakening.world.entity.motion.SpikedBeetleMotion.Position moved;
    private boolean collided;
    private boolean flippedHandler;
    private boolean walkAnimationRequested;
    private boolean flippedAnimationRequested;
    private int nextVariant;
    private linksawakening.world.entity.RoomEntity updated;

    SpikedBeetleFrameStep(SpikedBeetleMotion owner, linksawakening.world.entity.motion.SpikedBeetleFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.motion.SpikedBeetleMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceMovementAndState()) return completedResult;

        return new SpikedBeetleMotion.Update(
                updated,
                countdown,
                flippedHandler ? SpikedBeetleMotion.FLIPPED_OPTIONS1 : SpikedBeetleMotion.NORMAL_OPTIONS1,
                flippedHandler ? SpikedBeetleMotion.FLIPPED_HITBOX_FLAGS : SpikedBeetleMotion.NORMAL_HITBOX_FLAGS);
    }

    private linksawakening.world.entity.motion.SpikedBeetleMotion.Update completedResult;

    private boolean advanceState0() {
        owner.clearSpeed(slot);
        if (countdown == 0) {
            countdown = 0x30 + ((randomByteSupplier.getAsInt() & 0xFF) & 0x1F);
            owner.state[slot] = 1;
            int randomDirection = randomByteSupplier.getAsInt() & 0xFF;
            int direction = (randomDirection & 0x06) == 0
                    ? SpikedBeetleMotion.directionToLink(x, y, linkEntityX, linkEntityY)
                    : (randomByteSupplier.getAsInt() & 0x03);
            owner.setWalkingSpeed(slot, direction);
        }
        if (SpikedBeetleMotion.nearLinkOnOneAxis(x, y, linkEntityX, linkEntityY)) {
            owner.startDash(slot, linkEntityX, linkEntityY, x, y);
            countdown = 0xFF;
        }

        return true;
    }

    private boolean advanceState1() {
        walkAnimationRequested = true;
        if (countdown == 0) {
            countdown = 0x18;
            owner.state[slot] = 0;
        }
        if (SpikedBeetleMotion.nearLinkOnOneAxis(x, y, linkEntityX, linkEntityY)) {
            owner.startDash(slot, linkEntityX, linkEntityY, x, y);
            countdown = 0xFF;
        }

        return true;
    }

    private boolean advanceState2() {
        walkAnimationRequested = true;
        if (countdown == 0 || collided) {
            owner.state[slot] = 0;
        } else {
            int dashDirection = owner.direction[slot];
            owner.speedX[slot] =
                    SpikedBeetleMotion.approach(owner.speedX[slot], SpikedBeetleMotion.DASHING_X_SPEEDS[dashDirection]);
            owner.speedY[slot] =
                    SpikedBeetleMotion.approach(owner.speedY[slot], SpikedBeetleMotion.DASHING_Y_SPEEDS[dashDirection]);
        }

        return true;
    }

    private boolean advanceState3() {
        flippedAnimationRequested = true;
        if (countdown == 0) {
            // $07:790A increments the state, then writes B (zero)
            // through the returned state-table pointer.
            owner.state[slot] = 0;
            owner.speedZ[slot] = 0x18;
            owner.speedX[slot] = 0;
        } else if (countdown < 0x60) {
            owner.speedX[slot] = (countdown & 0x04) == 0 ? 0xF8 : 0x08;
        }

        return true;
    }

    private boolean prepareFrame() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        transitionCountdown = inputs.transitionCountdown();
        ignoreHitsCountdown = inputs.ignoreHitsCountdown();

        if ((entity.type() & 0xFF) != SpikedBeetleMotion.ENTITY_TYPE) {
            throw new IllegalArgumentException(
                    "Unsupported Spiked Beetle entity type: 0x" + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Spiked Beetle random-byte supplier cannot be null");
        }

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        countdown = transitionCountdown & 0xFF;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        z = SpikedBeetleMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;

        // SpikedBeetleEntityHandler clears horizontal speed while the shared
        // ignore-hits window is active, before applying position movement.
        if ((ignoreHitsCountdown & 0xFF) != 0) {
            owner.clearSpeed(slot);
        }

        landed = (z & 0x80) != 0 || z == 0;
        return true;
    }

    private boolean advanceMovementAndState() {

        if (landed) {
            z = 0;
            if (owner.state[slot] == 3) {
                owner.speedX[slot] = SpikedBeetleMotion.halveSignedSpeed(owner.speedX[slot]);
                owner.speedY[slot] = SpikedBeetleMotion.halveSignedSpeed(owner.speedY[slot]);
                owner.speedZ[slot] = SpikedBeetleMotion.complementHalfSpeed(owner.speedZ[slot]);
                if (owner.speedZ[slot] < 0x07) {
                    owner.speedZ[slot] = 0;
                    owner.clearSpeed(slot);
                }
            } else {
                owner.speedZ[slot] = 0;
            }
        }

        renderedVariant = owner.animationVariant[slot];
        moved = owner.move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        collided = moved.collided();
        flippedHandler = owner.state[slot] == 3;

        walkAnimationRequested = false;
        flippedAnimationRequested = false;
        switch (owner.state[slot]) {
            case 0 -> {
                if (!advanceState0()) return false;
            }
            case 1 -> {
                if (!advanceState1()) return false;
            }
            case 2 -> {
                if (!advanceState2()) return false;
            }
            case 3 -> {
                if (!advanceState3()) return false;
            }
            default -> throw new IllegalStateException("Invalid Spiked Beetle state: " + owner.state[slot]);
        }

        nextVariant = owner.animationVariant[slot];
        if (walkAnimationRequested) {
            nextVariant = (frameCounter >>> 4) & 0x01;
        } else if (flippedAnimationRequested) {
            nextVariant = 2 + ((frameCounter >>> 4) & 0x01);
        }
        owner.animationVariant[slot] = nextVariant;
        updated = EntitySnapshots.withMotion(entity, x, y, renderedVariant, entity.entityFlipAttribute(), z, -1, false);
        return true;
    }
}
