package linksawakening.world.entity.motion;

/** Reused, reentrancy-safe scratch for RollingBonesMotion.advanceBoss; values are assigned in source order. */
final class RollingBonesFrameStep {
    private RollingBonesMotion owner;
    private linksawakening.world.entity.RoomEntity entity;
    private linksawakening.world.entity.RoomEntity bar;
    private int transitionCountdown;
    private int health;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int x;
    private int y;
    private int z;
    private int variant;
    private int jingleId;
    private int previousSpeedZ;
    private boolean landed;
    private int displayVariant;

    RollingBonesFrameStep() {}

    linksawakening.world.entity.motion.RollingBonesMotion.BossUpdate run() {
        slot = entity.slot();
        x = entity.x();
        y = entity.y();
        z = entity.z();
        variant = owner.bossSpriteVariant[slot];
        jingleId = -1;

        previousSpeedZ = owner.bossSpeedZ[slot];
        z = RollingBonesMotion.addSpeed(z, previousSpeedZ, owner.bossZAccumulator, slot);
        owner.bossSpeedZ[slot] -= 2;
        landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            int impactSpeedZ = owner.bossSpeedZ[slot];
            owner.bossSpeedZ[slot] = 0;
            if (impactSpeedZ < -0x0E) {
                jingleId = 0x20;
            }
        }

        if (owner.bossSpeedX[slot] != 0) {
            owner.bossDirection[slot] = owner.bossSpeedX[slot] < 0 ? 0 : 3;
        }

        switch (owner.bossState[slot]) {
            case RollingBonesMotion.STATE_APPROACH_BAR -> {
                if (!advanceApproachBar()) return completedResult;
            }
            case RollingBonesMotion.STATE_PREPARE_ROLL -> {
                if (!advancePrepareRoll()) return completedResult;
            }
            case RollingBonesMotion.STATE_WAIT_FOR_BAR -> {
                if (!advanceWaitForBar()) return completedResult;
            }
            case RollingBonesMotion.STATE_JUMP_AWAY -> {
                if (!advanceJumpAway()) return completedResult;
            }
            default -> owner.initializeBoss(slot);
        }

        // func_006_6E7E indexes the six rectangle variants by adding the
        // direction-table value (0 or 3) to the handler's animation variant.
        owner.bossSpriteVariant[slot] = variant;
        displayVariant = variant + owner.bossDirection[slot];
        return new RollingBonesMotion.BossUpdate(
                RollingBonesMotion.withPositionAndVariant(entity, x, y, z, displayVariant),
                transitionCountdown,
                jingleId);
    }

    private linksawakening.world.entity.motion.RollingBonesMotion.BossUpdate completedResult;

    private boolean advanceApproachBar() {
        if (transitionCountdown == 0) {
            int delta = RollingBonesMotion.signedByte(entity.x() - bar.x());
            owner.bossSpeedX[slot] = delta < 0 ? 0x08 : -0x08;
            if (delta >= -0x10 && delta < 0x10) {
                transitionCountdown = 0x18;
                owner.bossState[slot] = RollingBonesMotion.STATE_PREPARE_ROLL;
            } else {
                x = RollingBonesMotion.moveX(
                                entity, x, y, owner.bossSpeedX[slot], owner.bossXAccumulator, slot, backgroundCollision)
                        .position();
                if (landed) {
                    owner.bossSpeedZ[slot] = 0x0C;
                }
            }
            variant = 1;
        }

        return true;
    }

    private boolean advancePrepareRoll() {
        if (transitionCountdown == 0) {
            int barSpeed = owner.bossDirection[slot] == 0 ? -0x10 : 0x10;
            owner.launchBar(bar.slot(), barSpeed);
            transitionCountdown = 0x20;
            owner.bossState[slot] = RollingBonesMotion.STATE_WAIT_FOR_BAR;
            variant = 0;
        }

        return true;
    }

    private boolean advanceWaitForBar() {
        if (transitionCountdown == 0) {
            owner.bossState[slot] = RollingBonesMotion.STATE_JUMP_AWAY;
        }

        return true;
    }

    private boolean advanceJumpAway() {
        variant = z < 0x08 ? 1 : 2;
        if (transitionCountdown == 1) {
            int length = health < 5 ? 0x14 : 0x10;
            owner.bossSpeedZ[slot] = health < 5 ? 0x16 : 0x19;
            owner.bossSpeedX[slot] = owner.bossDirection[slot] == 0 ? -length : length;
            owner.bossSpeedY[slot] = ((entity.y() - entity.z()) & 0xFF) < 0x50 ? length : -length;
        } else if (transitionCountdown == 0 && landed) {
            transitionCountdown = 0x10;
        }
        // $06:6DD4 falls through to $6E04 on the launch frame.
        if (transitionCountdown == 1 || (transitionCountdown == 0 && !landed)) {
            RollingBonesMotion.MoveResult xMove = RollingBonesMotion.moveX(
                    entity, x, y, owner.bossSpeedX[slot], owner.bossXAccumulator, slot, backgroundCollision);
            x = xMove.position();
            RollingBonesMotion.MoveResult yMove = RollingBonesMotion.moveY(
                    entity, x, y, owner.bossSpeedY[slot], owner.bossYAccumulator, slot, backgroundCollision);
            y = yMove.position();
            if (xMove.blocked()) {
                owner.bossState[slot] = RollingBonesMotion.STATE_APPROACH_BAR;
                transitionCountdown = 0x08;
                owner.bossSpeedX[slot] = 0;
                owner.bossSpeedY[slot] = 0;
            }
        }

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.RollingBonesMotion.BossUpdate run(
            RollingBonesMotion owner,
            linksawakening.world.entity.RoomEntity entity,
            linksawakening.world.entity.RoomEntity bar,
            int transitionCountdown,
            int health,
            linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision) {
        if (inUse) {
            return new RollingBonesFrameStep()
                    .run(owner, entity, bar, transitionCountdown, health, backgroundCollision);
        }
        inUse = true;
        slot = 0;
        x = 0;
        y = 0;
        z = 0;
        variant = 0;
        jingleId = 0;
        previousSpeedZ = 0;
        landed = false;
        displayVariant = 0;
        completedResult = null;
        this.owner = owner;
        this.entity = entity;
        this.bar = bar;
        this.transitionCountdown = transitionCountdown;
        this.health = health;
        this.backgroundCollision = backgroundCollision;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.bar = null;
            this.backgroundCollision = null;
            this.completedResult = null;
        }
    }
}
