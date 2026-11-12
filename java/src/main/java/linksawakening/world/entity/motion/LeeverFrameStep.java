package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Per-invocation scratch for LeeverMotion.advance; values are assigned in source order. */
final class LeeverFrameStep {
    private final LeeverMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int x;
    private int y;
    private int variant;

    LeeverFrameStep(
            LeeverMotion owner,
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

        x = LeeverMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        y = LeeverMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);

        // ApplyEntityInteractionWithBackground restores blocked coordinates
        // before the Leever state handler; it does not reverse ordinary speed.
        if (backgroundCollision != null) {
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

        return EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), entity.z(), -1, false);
    }

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
}
