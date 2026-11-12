package linksawakening.world.entity.motion;

/** Reused, reentrancy-safe scratch for MoblinKingMotion.advance; values are assigned in source order. */
final class MoblinKingStep {
    private MoblinKingMotion owner;
    private linksawakening.world.entity.motion.MoblinKingFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkX;
    private int linkY;
    private int transitionCountdown;
    private int slowTransitionCountdown;
    private int frameCounter;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private boolean touchingLink;
    private int slot;
    private int x;
    private int y;
    private int z;
    private boolean blocked;
    private boolean landed;
    private linksawakening.world.entity.motion.MoblinKingMotion.ArrowRequest arrow;
    private boolean openDialog;
    private int jingle;
    private boolean strongBump;
    private int screenShake;
    private int linkDamage;
    private int ignoreLinkCollisions;
    private int variant;

    MoblinKingStep() {}

    linksawakening.world.entity.motion.MoblinKingMotion.Update run() {
        if (!prepareMotion()) return completedResult;
        if (!advanceBehavior()) return completedResult;

        return new MoblinKingMotion.Update(
                MoblinKingMotion.withPosition(entity, x, y, z),
                transitionCountdown & 0xFF,
                slowTransitionCountdown & 0xFF,
                arrow,
                openDialog,
                jingle,
                strongBump,
                screenShake,
                linkDamage,
                ignoreLinkCollisions);
    }

    private linksawakening.world.entity.motion.MoblinKingMotion.Update completedResult;

    private boolean advanceState0() {
        transitionCountdown = 0x20;
        owner.speedX[slot] = 0;
        owner.speedY[slot] = 0;
        variant = 0;
        owner.weaponFrame[slot] = 0;
        owner.state[slot] = 1;
        owner.direction[slot] = MoblinKingMotion.directionToLink(x, linkX);

        return true;
    }

    private boolean advanceState1() {
        if (transitionCountdown == 0) {
            openDialog = true;
            owner.state[slot] = 2;
            slowTransitionCountdown = 0x30;
            variant = 1;
            owner.weaponFrame[slot] = 1;
        }

        return true;
    }

    private boolean advanceState2() {
        if (owner.privateCountdown1[slot] != 0) {
            if (owner.privateCountdown1[slot] == 0x0C) {
                int facing = owner.direction[slot] & 1;
                arrow = new MoblinKingMotion.ArrowRequest(
                        (x + MoblinKingMotion.ARROW_X_OFFSETS[facing]) & 0xFF,
                        (y + MoblinKingMotion.ARROW_Y_OFFSETS[facing]) & 0xFF,
                        MoblinKingMotion.ARROW_SPEED_X[facing] & 0xFF,
                        0,
                        facing);
            }
            variant = owner.privateCountdown1[slot] < 0x0C ? 0 : 2;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
        } else if (slowTransitionCountdown == 0) {
            transitionCountdown = 0x30;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            owner.state[slot] = 3;
        } else {
            if (landed) {
                MoblinKingMotion.Vector vector = MoblinKingMotion.vectorTowardsOffsetLink(
                        x, y, linkX, linkY, MoblinKingMotion.directionToLink(x, linkX), 0x0C);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.speedZ[slot] = 0x10;
            }
            variant = (frameCounter >>> 4) & 1;
            owner.weaponFrame[slot] = variant;
            owner.direction[slot] = MoblinKingMotion.directionToLink(x, linkX);
            if (owner.privateCountdown2[slot] == 0) {
                owner.privateCountdown2[slot] = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
                owner.privateCountdown1[slot] = 0x18;
            }
        }

        return true;
    }

    private boolean advanceState3() {
        owner.weaponVisible[slot] = false;
        if (transitionCountdown == 0) {
            transitionCountdown = 0x22;
            owner.speedX[slot] = MoblinKingMotion.CHARGE_SPEED_X[owner.direction[slot] & 1];
            owner.speedY[slot] = 0;
            owner.state[slot] = 4;
        } else {
            if ((transitionCountdown & 7) == 0) {
                jingle = 0x09;
            }
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            variant = 2 + ((frameCounter >>> 3) & 1);
        }

        return true;
    }

    private boolean advanceState4() {
        owner.weaponVisible[slot] = false;
        if (touchingLink) {
            owner.state[slot] = 8;
            MoblinKingMotion.Update hit =
                    owner.applyLinkImpact(entity, x, y, z, variant, transitionCountdown, slowTransitionCountdown);
            {
                completedResult = hit;
                return false;
            }
        }
        if (transitionCountdown == 0 && !blocked) {
            owner.state[slot] = 6;
        } else if (blocked) {
            owner.speedX[slot] = -MoblinKingMotion.signedByte(owner.speedX[slot]) / 4;
            owner.speedZ[slot] = 0x28;
            transitionCountdown = 0x60;
            owner.state[slot] = 5;
            strongBump = true;
            screenShake = 0x20;
            jingle = 0x0B;
        }
        variant = 4 + ((frameCounter >>> 2) & 1);

        return true;
    }

    private boolean advanceState5() {
        owner.weaponVisible[slot] = false;
        if (transitionCountdown == 0) {
            transitionCountdown = 0x40;
            owner.state[slot] = 7;
            variant = 0;
            owner.weaponVisible[slot] = true;
            owner.weaponFrame[slot] = 0;
        } else {
            if (landed) {
                owner.speedX[slot] = 0;
                owner.speedY[slot] = 0;
            }
            variant = 0x0C + ((frameCounter >>> 3) & 1);
        }

        return true;
    }

    private boolean advanceState6() {
        owner.weaponFrame[slot] = 0;
        int signedSpeedX = MoblinKingMotion.signedByte(owner.speedX[slot]);
        if ((signedSpeedX & 0xFE) == 0) {
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            transitionCountdown = 0x40;
            owner.state[slot] = 7;
        } else {
            owner.speedX[slot] = signedSpeedX > 0 ? signedSpeedX - 2 : signedSpeedX + 2;
            variant = 0;
        }

        return true;
    }

    private boolean advanceState7() {
        owner.weaponFrame[slot] = 0;
        owner.speedX[slot] = 0;
        owner.speedY[slot] = 0;
        if (transitionCountdown == 0) {
            slowTransitionCountdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x20;
            owner.state[slot] = 2;
        }

        return true;
    }

    private boolean advanceState8() {
        owner.weaponVisible[slot] = false;
        {
            completedResult =
                    owner.applyLinkImpact(entity, x, y, z, variant, transitionCountdown, slowTransitionCountdown);
            return false;
        }
    }

    private boolean advanceState9() {
        owner.speedX[slot] = 0;
        owner.speedY[slot] = 0;
        if (transitionCountdown == 0) {
            slowTransitionCountdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x20;
            owner.state[slot] = 2;
        } else {
            variant = transitionCountdown >= 0x40 ? 0 : ((frameCounter >>> 5) & 1);
            owner.weaponFrame[slot] = variant;
            owner.direction[slot] = MoblinKingMotion.directionToLink(x, linkX);
        }

        return true;
    }

    private boolean prepareMotion() {

        entity = inputs.entity();
        linkX = inputs.linkX();
        linkY = inputs.linkY();
        transitionCountdown = inputs.transitionCountdown();
        slowTransitionCountdown = inputs.slowTransitionCountdown();
        frameCounter = inputs.frameCounter();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        touchingLink = inputs.touchingLink();
        slot = entity.slot();
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }
        if (owner.privateCountdown2[slot] > 0) {
            owner.privateCountdown2[slot]--;
        }

        x = entity.x();
        y = entity.y();
        z = entity.z();
        blocked = false;
        if (owner.speedX[slot] != 0) {
            int nextX = MoblinKingMotion.addSpeed(x, owner.speedX[slot], owner.xAccumulator, slot);
            int collisionDirection = MoblinKingMotion.signedByte(owner.speedX[slot]) < 0 ? 1 : 0;
            blocked = backgroundCollision != null && backgroundCollision.blocks(entity, collisionDirection, nextX, y);
            if (!blocked) {
                x = nextX;
            }
        }
        if (owner.speedY[slot] != 0) {
            int nextY = MoblinKingMotion.addSpeed(y, owner.speedY[slot], owner.yAccumulator, slot);
            int collisionDirection = MoblinKingMotion.signedByte(owner.speedY[slot]) < 0 ? 2 : 3;
            boolean yBlocked =
                    backgroundCollision != null && backgroundCollision.blocks(entity, collisionDirection, x, nextY);
            blocked |= yBlocked;
            if (!yBlocked) {
                y = nextY;
            }
        }

        z = MoblinKingMotion.addSpeed(z, owner.speedZ[slot], owner.zAccumulator, slot);
        owner.speedZ[slot] = MoblinKingMotion.signedByte(owner.speedZ[slot]) - 3;
        landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            owner.speedZ[slot] = 0;
        }
        return true;
    }

    private boolean advanceBehavior() {

        arrow = null;
        openDialog = false;
        jingle = -1;
        strongBump = false;
        screenShake = 0;
        linkDamage = 0;
        ignoreLinkCollisions = 0;
        variant = owner.bodyVariant[slot];
        owner.weaponVisible[slot] = true;

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
            case 4 -> {
                if (!advanceState4()) return false;
            }
            case 5 -> {
                if (!advanceState5()) return false;
            }
            case 6 -> {
                if (!advanceState6()) return false;
            }
            case 7 -> {
                if (!advanceState7()) return false;
            }
            case 8 -> {
                if (!advanceState8()) return false;
            }
            case 9 -> {
                if (!advanceState9()) return false;
            }
            default -> owner.state[slot] = 0;
        }

        owner.bodyVariant[slot] = variant;
        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.MoblinKingMotion.Update run(
            MoblinKingMotion owner, linksawakening.world.entity.motion.MoblinKingFrame inputs) {
        if (inUse) {
            return new MoblinKingStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        linkX = 0;
        linkY = 0;
        transitionCountdown = 0;
        slowTransitionCountdown = 0;
        frameCounter = 0;
        randomByteSupplier = null;
        backgroundCollision = null;
        touchingLink = false;
        slot = 0;
        x = 0;
        y = 0;
        z = 0;
        blocked = false;
        landed = false;
        arrow = null;
        openDialog = false;
        jingle = 0;
        strongBump = false;
        screenShake = 0;
        linkDamage = 0;
        ignoreLinkCollisions = 0;
        variant = 0;
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
            this.randomByteSupplier = null;
            this.backgroundCollision = null;
            this.arrow = null;
            this.completedResult = null;
        }
    }
}
