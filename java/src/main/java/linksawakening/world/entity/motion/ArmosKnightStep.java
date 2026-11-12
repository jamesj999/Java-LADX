package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for ArmosKnightMotion.advance; values are assigned in source order. */
final class ArmosKnightStep {
    private ArmosKnightMotion owner;
    private linksawakening.world.entity.motion.ArmosKnightFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkEntityX;
    private int linkEntityY;
    private int linkZ;
    private int transitionCountdown;
    private int physicsFlags;
    private int hitboxFlags;
    private int options1;
    private int health;
    private int slot;
    private boolean linkMotionBlocked;
    private int x;
    private int y;
    private int z;
    private boolean landed;
    private int variant;
    private linksawakening.world.entity.motion.ArmosKnightMotion.RubbleRequest rubbleRequest;
    private linksawakening.world.entity.motion.ArmosKnightMotion.ScreenShakeRequest screenShakeRequest;
    private int jingleId;
    private linksawakening.world.entity.RoomEntity updated;

    ArmosKnightStep() {}

    linksawakening.world.entity.motion.ArmosKnightMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceCombatState()) return completedResult;

        return new ArmosKnightMotion.Update(
                updated,
                transitionCountdown,
                physicsFlags,
                hitboxFlags,
                options1,
                jingleId,
                linkMotionBlocked,
                screenShakeRequest,
                rubbleRequest);
    }

    private linksawakening.world.entity.motion.ArmosKnightMotion.Update completedResult;

    private boolean advanceWake() {
        if (ArmosKnightMotion.withinDistance(x, linkEntityX, 0x20)
                && ArmosKnightMotion.withinDistance(y, linkEntityY, 0x20)) {
            owner.state[slot] = ArmosKnightMotion.STATE_CHARGE;
            transitionCountdown = 0x30;
        }

        return true;
    }

    private boolean advanceCharge() {
        if (transitionCountdown == 0) {
            transitionCountdown = 0x80;
            owner.state[slot] = ArmosKnightMotion.STATE_CHARGE_MOVE;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            variant = 0;
        } else {
            variant = (transitionCountdown >>> 2) & 0x01;
        }

        return true;
    }

    private boolean advanceChargeMove() {
        if (transitionCountdown == 0) {
            transitionCountdown = 0x50;
            owner.state[slot] = ArmosKnightMotion.STATE_JUMP_DELAY;
            physicsFlags &= 0x7F;
            hitboxFlags &= 0x7F;
            options1 &= 0xBF;
        } else {
            owner.speedX[slot] = (transitionCountdown & 0x04) == 0 ? 0x08 : 0xF8;
            x = ArmosKnightMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        }

        return true;
    }

    private boolean advanceJumpDelay() {
        if (transitionCountdown == 0) {
            owner.speedZ[slot] = 0x30;
            owner.state[slot] = ArmosKnightMotion.STATE_JUMP;
            jingleId = 0x24;
        } else {
            if (landed) {
                owner.speedZ[slot] = 0x0C;
                int length = health < 0x05 ? 0x0C : 0x08;
                ArmosKnightMotion.Vector vector =
                        ArmosKnightMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, length);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                jingleId = 0x20;
            }
            x = ArmosKnightMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            y = ArmosKnightMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
        }

        return true;
    }

    private boolean advanceJump() {
        if ((owner.speedZ[slot] & 0xFE) == 0) {
            transitionCountdown = 0x10;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            owner.state[slot] = ArmosKnightMotion.STATE_LAND_DELAY;
        }
        x = ArmosKnightMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
        y = ArmosKnightMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);

        return true;
    }

    private boolean advanceLandDelay() {
        if (transitionCountdown == 0) {
            owner.state[slot] = ArmosKnightMotion.STATE_BOUNCE;
            owner.speedZ[slot] = 0xB0;
        } else {
            owner.speedZ[slot] = 0;
        }

        return true;
    }

    private boolean advanceBounce() {
        if (landed) {
            transitionCountdown = 0x30;
            jingleId = 0x0B;
            screenShakeRequest = new ArmosKnightMotion.ScreenShakeRequest(0x30, 0x04);
            if (linkZ == 0) {
                owner.privateCountdown1[slot] = 0x40;
            }
            owner.state[slot] = ArmosKnightMotion.STATE_BOUNCE_DELAY;
        }

        return true;
    }

    private boolean advanceBounceDelay() {
        if (transitionCountdown == 0) {
            owner.state[slot] = ArmosKnightMotion.STATE_CHARGE_MOVE;
        }

        return true;
    }

    private boolean advanceDefault() {
        owner.state[slot] = ArmosKnightMotion.STATE_WAKE;
        owner.speedX[slot] = 0;
        owner.speedY[slot] = 0;
        owner.speedZ[slot] = 0;

        return true;
    }

    private boolean prepareFrame() {

        entity = inputs.entity();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkZ = inputs.linkZ();
        transitionCountdown = inputs.transitionCountdown();
        physicsFlags = inputs.physicsFlags();
        hitboxFlags = inputs.hitboxFlags();
        options1 = inputs.options1();
        health = inputs.health();
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }
        linkMotionBlocked = owner.privateCountdown1[slot] != 0;
        x = entity.x();
        y = entity.y();
        z = entity.z();
        landed = false;
        if (owner.speedZ[slot] != 0) {
            z = ArmosKnightMotion.addSpeedToPosition(z, owner.speedZ[slot], owner.speedZAccumulator, slot);
            owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
            if ((z & 0x80) != 0) {
                z = 0;
                owner.speedZ[slot] = 0;
                landed = true;
            }
        }

        variant = entity.spriteVariant();
        if (health < 0x08) {
            variant = health < 0x04 ? 0x03 : 0x02;
        }
        rubbleRequest = null;
        screenShakeRequest = null;
        return true;
    }

    private boolean advanceCombatState() {

        if ((health & 0xFF) != owner.privateState1[slot]) {
            owner.privateState1[slot] = health & 0xFF;
            if ((health & 0xFF) < 0x08) {
                int maximumInertia = (health & 0xFF) < 0x04 ? 2 : 1;
                if (owner.inertia[slot] < maximumInertia) {
                    owner.inertia[slot]++;
                    int rubbleX = (health & 0xFF) < 0x04 ? x - 1 : x + 7;
                    int rubbleY = y - z - 0x10;
                    rubbleRequest = new ArmosKnightMotion.RubbleRequest(rubbleX & 0xFF, rubbleY & 0xFF);
                }
            }
        }
        jingleId = -1;
        switch (owner.state[slot]) {
            case ArmosKnightMotion.STATE_WAKE -> {
                if (!advanceWake()) return false;
            }
            case ArmosKnightMotion.STATE_CHARGE -> {
                if (!advanceCharge()) return false;
            }
            case ArmosKnightMotion.STATE_CHARGE_MOVE -> {
                if (!advanceChargeMove()) return false;
            }
            case ArmosKnightMotion.STATE_JUMP_DELAY -> {
                if (!advanceJumpDelay()) return false;
            }
            case ArmosKnightMotion.STATE_JUMP -> {
                if (!advanceJump()) return false;
            }
            case ArmosKnightMotion.STATE_LAND_DELAY -> {
                if (!advanceLandDelay()) return false;
            }
            case ArmosKnightMotion.STATE_BOUNCE -> {
                if (!advanceBounce()) return false;
            }
            case ArmosKnightMotion.STATE_BOUNCE_DELAY -> {
                if (!advanceBounceDelay()) return false;
            }
            default -> {
                if (!advanceDefault()) return false;
            }
        }

        updated = EntitySnapshots.withMotion(entity, x, y, variant, entity.entityFlipAttribute(), z, -1, false);
        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.ArmosKnightMotion.Update run(
            ArmosKnightMotion owner, linksawakening.world.entity.motion.ArmosKnightFrame inputs) {
        if (inUse) {
            return new ArmosKnightStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        linkEntityX = 0;
        linkEntityY = 0;
        linkZ = 0;
        transitionCountdown = 0;
        physicsFlags = 0;
        hitboxFlags = 0;
        options1 = 0;
        health = 0;
        slot = 0;
        linkMotionBlocked = false;
        x = 0;
        y = 0;
        z = 0;
        landed = false;
        variant = 0;
        rubbleRequest = null;
        screenShakeRequest = null;
        jingleId = 0;
        updated = null;
        completedResult = null;
        this.owner = owner;
        this.inputs = inputs;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.inputs = null;
            this.entity = null;
            this.rubbleRequest = null;
            this.screenShakeRequest = null;
            this.updated = null;
            this.completedResult = null;
        }
    }
}
