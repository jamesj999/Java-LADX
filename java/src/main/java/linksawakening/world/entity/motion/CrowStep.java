package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for CrowMotion.advance; values are assigned in source order. */
final class CrowStep {
    private CrowMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private int transitionCountdown;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private boolean allowLinkProximityTrigger;
    private boolean externalTakeoffTrigger;
    private int slot;
    private int countdown;
    private int x;
    private int y;
    private int z;
    private int variant;
    private boolean boomerangSound;
    private boolean unloaded;
    private linksawakening.world.entity.RoomEntity updated;

    CrowStep() {}

    linksawakening.world.entity.motion.CrowMotion.Update run() {
        if (entity == null || (entity.type() & 0xFF) != CrowMotion.ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Crow motion entity");
        }
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        countdown = transitionCountdown & 0xFF;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        z = entity.z() & 0xFF;
        variant = owner.selectedVariant[slot];
        boomerangSound = false;
        unloaded = false;

        // $5CBE mirrors only the active display variant, then renders it.
        // SetEntitySpriteVariant later writes the stored frame for the next
        // handler pass, without changing this frame's displayed facing.
        if (owner.direction[slot] == 0) {
            variant += 2;
        }

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
            default -> throw new IllegalStateException("Invalid Crow state: " + owner.state[slot]);
        }

        updated = EntitySnapshots.withMotion(
                entity,
                x,
                y,
                variant,
                entity.entityFlipAttribute(),
                z,
                entity.deathSpriteVariant(),
                entity.powerRecoilDeath());
        return new CrowMotion.Update(updated, countdown, owner.physicsFlags[slot], unloaded, boomerangSound);
    }

    private linksawakening.world.entity.motion.CrowMotion.Update completedResult;

    private boolean advanceState0() {
        // The first handler pass lifts the visual origin by four
        // pixels and advances the state before checking proximity.
        if (!owner.perchPositionAdjusted[slot]) {
            y = (y - 4) & 0xFF;
            owner.perchPositionAdjusted[slot] = true;
        }
        owner.direction[slot] = CrowMotion.signedByte((linkEntityX - x) & 0xFF) < 0 ? 1 : 0;
        int distanceX = CrowMotion.signedByte((linkEntityX - x) & 0xFF);
        int distanceY = CrowMotion.signedByte((linkEntityY - y) & 0xFF);
        if (externalTakeoffTrigger
                || allowLinkProximityTrigger
                        && distanceX >= -0x18
                        && distanceX < 0x18
                        && distanceY >= -0x30
                        && distanceY < 0x30) {
            owner.physicsFlags[slot] = CrowMotion.ACTIVE_PHYSICS_FLAGS;
            countdown = 0x22;
            owner.state[slot] = 1;
        }

        return true;
    }

    private boolean advanceState1() {
        if (countdown == 0) {
            countdown = 0x30;
            owner.state[slot] = 2;
        } else {
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            owner.speedZ[slot] = 0x08;
            z = CrowMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
            boomerangSound = true;
            // AddEntityZSpeedToPos_06 leaves HL at the updated Z byte.
            owner.selectedVariant[slot] = (z >>> 3) & 0x01;
        }

        return true;
    }

    private boolean advanceState2() {
        if (countdown == 0) {
            owner.state[slot] = 3;
        } else {
            if ((countdown & 0x01) == 0) {
                CrowMotion.Vector vector = CrowMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x20);
                owner.speedY[slot] = CrowMotion.approach(owner.speedY[slot], vector.y());
                owner.speedX[slot] = CrowMotion.approach(owner.speedX[slot], vector.x());
                owner.direction[slot] = owner.speedX[slot] & 0x80;
            }
            x = CrowMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            y = CrowMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            boomerangSound = true;
            owner.selectedVariant[slot] = CrowMotion.flightVariant(y, owner.speedY[slot]);
        }

        return true;
    }

    private boolean advanceState3() {
        if ((frameCounter & 0x03) == 0) {
            CrowMotion.Vector vector = CrowMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x20);
            owner.speedY[slot] = CrowMotion.approach(owner.speedY[slot], CrowMotion.negate(vector.y()));
            owner.speedX[slot] = CrowMotion.approach(owner.speedX[slot], CrowMotion.negate(vector.x()));
            owner.direction[slot] = owner.speedX[slot] & 0x80;
        }
        x = CrowMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        y = CrowMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        boomerangSound = true;
        owner.selectedVariant[slot] = CrowMotion.flightVariant(y, owner.speedY[slot]);
        // $5E54 reads the cached active position. The movement helpers
        // update WRAM positions without refreshing those HRAM bytes.
        unloaded = ((entity.y() - entity.z()) & 0xFF) >= 0x88 || entity.x() >= 0xA8;

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.CrowMotion.Update run(
            CrowMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            int transitionCountdown,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean allowLinkProximityTrigger,
            boolean externalTakeoffTrigger) {
        if (inUse) {
            return new CrowStep()
                    .run(
                            owner,
                            entity,
                            transitionCountdown,
                            frameCounter,
                            linkEntityX,
                            linkEntityY,
                            allowLinkProximityTrigger,
                            externalTakeoffTrigger);
        }
        inUse = true;
        slot = 0;
        countdown = 0;
        x = 0;
        y = 0;
        z = 0;
        variant = 0;
        boomerangSound = false;
        unloaded = false;
        updated = null;
        completedResult = null;
        this.owner = owner;
        this.entity = entity;
        this.transitionCountdown = transitionCountdown;
        this.frameCounter = frameCounter;
        this.linkEntityX = linkEntityX;
        this.linkEntityY = linkEntityY;
        this.allowLinkProximityTrigger = allowLinkProximityTrigger;
        this.externalTakeoffTrigger = externalTakeoffTrigger;
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
