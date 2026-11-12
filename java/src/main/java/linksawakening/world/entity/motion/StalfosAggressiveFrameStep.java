package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Per-invocation scratch for StalfosAggressiveMotion.advance; values are assigned in source order. */
final class StalfosAggressiveFrameStep {
    private final StalfosAggressiveMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int frame;
    private int x;
    private int y;
    private int z;
    private int variant;

    StalfosAggressiveFrameStep(
            StalfosAggressiveMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {
        this.owner = owner;
        this.entity = entity;
        this.frameCounter = frameCounter;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.randomByteSupplier = randomByteSupplier;
        this.backgroundCollision = backgroundCollision;
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
}
