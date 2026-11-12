package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntity;

/** Reused, reentrancy-safe scratch for PincerMotion.advance; values are assigned in source order. */
final class PincerStep {
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
            if (PincerMotion.withinSignedWindow(linkX - x, 0x20) && PincerMotion.withinSignedWindow(linkY - y, 0x20)) {
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
