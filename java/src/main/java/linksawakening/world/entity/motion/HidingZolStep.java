package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for HidingZolMotion.advance; values are assigned in source order. */
final class HidingZolStep {
    private HidingZolMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
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
    private linksawakening.world.entity.RoomEntity updated;

    HidingZolStep() {}

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
            HidingZolMotion.Vector vector = HidingZolMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x0C);
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
            HidingZolMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int linkEntityX,
            int linkEntityY,
            java.util.function.IntSupplier randomByteSupplier,
            linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction,
            int frameCounter) {
        if (inUse) {
            return new HidingZolStep()
                    .run(
                            owner,
                            entity,
                            linkEntityX,
                            linkEntityY,
                            randomByteSupplier,
                            backgroundInteraction,
                            frameCounter);
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
        this.owner = owner;
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
            this.owner = null;
            this.entity = null;
            this.randomByteSupplier = null;
            this.backgroundInteraction = null;
            this.updated = null;
            this.completedResult = null;
        }
    }
}
