package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.TarinRaccoonMotion.*;

/** Reused, reentrancy-safe scratch for TarinRaccoonBehavior.advance; values are assigned in source order. */
final class TarinRaccoonStep {
    private linksawakening.world.entity.motion.TarinRaccoonMotion owner;
    private linksawakening.world.entity.RoomEntity source;
    private linksawakening.world.entity.motion.TarinRaccoonMotion.Input input;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int slot;
    private int slowCountdown;
    private int transitionCountdown;
    private int variant;
    private boolean shouldGetLost;
    private int dialog;
    private boolean blockLink;
    private boolean clearAttack;
    private int linkFacing;
    private boolean pushLink;
    private boolean roomChanged;
    private boolean tarinFlag;
    private int soundId;
    private boolean spawnBomb;
    private int collisionFlags;
    private linksawakening.world.entity.RoomEntity entity;

    TarinRaccoonStep() {}

    linksawakening.world.entity.motion.TarinRaccoonMotion.Update run() {
        TarinRaccoonMotion.checkType(source);
        slot = source.slot();
        slowCountdown = input.slowTransitionCountdown();
        transitionCountdown = input.transitionCountdown();
        variant = source.spriteVariant();
        shouldGetLost = false;
        dialog = NO_EVENT;
        blockLink = false;
        clearAttack = false;
        linkFacing = NO_EVENT;
        pushLink = false;
        roomChanged = false;
        tarinFlag = false;
        soundId = NO_EVENT;
        spawnBomb = false;
        collisionFlags = 0;
        entity = source;

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
            default -> throw new IllegalStateException("Invalid Tarin state: " + owner.state[slot]);
        }

        return new TarinRaccoonMotion.Update(
                entity,
                owner.state[slot],
                entity.spriteVariant(),
                shouldGetLost,
                dialog,
                blockLink,
                clearAttack,
                linkFacing,
                pushLink,
                roomChanged,
                tarinFlag,
                soundId < 0 ? NO_EVENT : 0,
                soundId,
                spawnBomb,
                slowCountdown,
                transitionCountdown,
                owner.speedX[slot] & 0xFF,
                owner.speedY[slot] & 0xFF,
                owner.speedZ[slot] & 0xFF,
                collisionFlags,
                owner.nearLinkLatch[slot]);
    }

    private linksawakening.world.entity.motion.TarinRaccoonMotion.Update completedResult;

    private boolean advanceState0() {
        variant = (input.frameCounter() >>> 4) & 1;
        if ((input.linkY() & 0xFF) < 0x30) {
            shouldGetLost = true;
            variant = 2 + ((input.frameCounter() >>> 3) & 1);
        } else {
            owner.warningShown[slot] = false;
        }
        entity = TarinRaccoonMotion.withVariant(source, variant);
        if (!input.dialogActive() && !input.inventoryAppearing()) {
            if ((input.linkY() & 0xFF) < 0x20 && !owner.warningShown[slot]) {
                owner.warningShown[slot] = true;
                dialog = 0x021;
            } else if (input.actionHeld()
                    && TarinRaccoonMotion.nearbyAndFacing(entity, input)
                    && input.linkAttackStepAnimationCountdown() == 0
                    && !input.linkAirborne()
                    && input.dialogCooldown() == 0
                    && input.windowY() == 0x80) {
                dialog = 0x00D;
            }
        }

        return true;
    }

    private boolean advanceState1() {
        blockLink = true;
        clearAttack = true;
        linkFacing = TarinRaccoonMotion.directionToLink(source, input.linkX(), input.linkY()) ^ 0x01;

        int animationSum = owner.animationAccumulator[slot] + owner.animationInertia[slot];
        owner.animationAccumulator[slot] = animationSum & 0xFF;
        if (animationSum > 0xFF) {
            owner.animationIndex[slot] = (owner.animationIndex[slot] + 1) % 6;
        }
        variant = TRANSFORMATION_VARIANTS[owner.animationIndex[slot]];
        entity = TarinRaccoonMotion.withVariant(source, variant);

        if (slowCountdown == 0) {
            owner.speedZ[slot] = 0;
            owner.state[slot] = 2;
            variant = 9;
            entity = TarinRaccoonMotion.withVariant(entity, variant);
            spawnBomb = true;
            roomChanged = true;
            tarinFlag = true;
            return true;
        }

        if ((input.frameCounter() & 1) == 0 && owner.animationInertia[slot] < 0xF0) {
            owner.animationInertia[slot]++;
        }
        TarinRaccoonMotion.Movement moved = owner.moveWithBackground(entity, backgroundInteraction, slot);
        entity = TarinRaccoonMotion.withPositionAndVariant(entity, moved.x(), moved.y(), variant);
        collisionFlags = moved.collisionFlags();

        if (slowCountdown < 6) {
            if ((entity.y() & 0xFF) < 0x30) {
                slowCountdown = 8;
            } else {
                owner.speedZ[slot] = (owner.speedZ[slot] + 1) & 0xFF;
                owner.speedX[slot] = TarinRaccoonMotion.dampTowardsZero(owner.speedX[slot]);
                owner.speedY[slot] = TarinRaccoonMotion.dampTowardsZero(owner.speedY[slot]);
                entity = TarinRaccoonMotion.withZ(
                        entity,
                        TarinRaccoonMotion.addSpeedToPosition(
                                entity.z(), owner.speedZ[slot], owner.accumulatorZ, slot));
                return true;
            }
        }

        if ((collisionFlags & 0x03) != 0) {
            owner.speedX[slot] = (-TarinRaccoonMotion.signedByte(owner.speedX[slot])) & 0xFF;
            soundId = 0x09;
        }
        if ((collisionFlags & 0x0C) != 0) {
            owner.speedY[slot] = (-TarinRaccoonMotion.signedByte(owner.speedY[slot])) & 0xFF;
            soundId = 0x09;
        }
        if (slowCountdown < 0x60 && (input.frameCounter() & 3) == 0) {
            owner.speedX[slot] = TarinRaccoonMotion.accelerateToLimit(owner.speedX[slot]);
            owner.speedY[slot] = TarinRaccoonMotion.accelerateToLimit(owner.speedY[slot]);
        }

        return true;
    }

    private boolean advanceState2() {
        blockLink = true;
        int z = TarinRaccoonMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.accumulatorZ, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        entity = TarinRaccoonMotion.withZ(entity, z);
        if ((z & 0x80) != 0) {
            entity = TarinRaccoonMotion.withZ(entity, 0);
            soundId = 0x23;
            transitionCountdown = 0x40;
            variant = 8 + TarinRaccoonMotion.directionToLink(entity, input.linkX(), input.linkY());
            entity = TarinRaccoonMotion.withVariant(entity, variant);
            owner.nearLinkLatch[slot] = TarinRaccoonMotion.withinSignedWindow(input.linkX() - entity.x(), 0x12)
                    && TarinRaccoonMotion.withinSignedWindow(input.linkY() - entity.y(), 0x12);
            owner.state[slot] = 3;
        }

        return true;
    }

    private boolean advanceState3() {
        if (transitionCountdown == 1) {
            dialog = 0x00A;
            return true;
        }
        if (transitionCountdown != 0) {
            blockLink = true;
            return true;
        }
        if ((input.frameCounter() & 0x1F) == 0) {
            variant = 8 + TarinRaccoonMotion.directionToLink(entity, input.linkX(), input.linkY());
            entity = TarinRaccoonMotion.withVariant(entity, variant);
        }
        pushLink = !owner.nearLinkLatch[slot];
        if (!input.dialogActive()
                && !input.inventoryAppearing()
                && input.actionHeld()
                && TarinRaccoonMotion.nearbyAndFacing(entity, input)
                && !input.linkAirborne()
                && input.dialogCooldown() == 0
                && input.windowY() == 0x80) {
            dialog = 0x00B;
        }

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.TarinRaccoonMotion.Update run(
            linksawakening.world.entity.motion.TarinRaccoonMotion owner,
            linksawakening.world.entity.RoomEntity source,
            linksawakening.world.entity.motion.TarinRaccoonMotion.Input input,
            linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction) {
        if (inUse) {
            return new TarinRaccoonStep().run(owner, source, input, backgroundInteraction);
        }
        inUse = true;
        slot = 0;
        slowCountdown = 0;
        transitionCountdown = 0;
        variant = 0;
        shouldGetLost = false;
        dialog = 0;
        blockLink = false;
        clearAttack = false;
        linkFacing = 0;
        pushLink = false;
        roomChanged = false;
        tarinFlag = false;
        soundId = 0;
        spawnBomb = false;
        collisionFlags = 0;
        entity = null;
        completedResult = null;
        this.owner = owner;
        this.source = source;
        this.input = input;
        this.backgroundInteraction = backgroundInteraction;
        try {
            return run();
        } finally {
            inUse = false;
            this.owner = null;
            this.source = null;
            this.input = null;
            this.backgroundInteraction = null;
            this.entity = null;
            this.completedResult = null;
        }
    }
}
