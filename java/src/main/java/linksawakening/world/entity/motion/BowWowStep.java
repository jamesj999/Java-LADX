package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.BowWowMotion.*;

import linksawakening.world.entity.*;
import linksawakening.world.entity.RoomEntity;

/** Reused, reentrancy-safe scratch for BowWowBehavior.advanceAfterTimers; values are assigned in source order. */
final class BowWowStep {
    private linksawakening.world.entity.motion.BowWowMotion owner;
    private linksawakening.world.entity.motion.BowWowCollisionFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int linkEntityZ;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private java.util.List<linksawakening.world.entity.RoomEntity> entities;
    private java.util.function.IntPredicate canEatEntity;
    private int bowWowState;
    private int slot;
    private int variant;
    private boolean wasAboveGround;
    private int x;
    private int y;
    private int z;
    private linksawakening.world.entity.motion.BowWowMotion.TargetContact targetContact;

    BowWowStep() {}

    linksawakening.world.entity.motion.BowWowMotion.Update run() {
        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkEntityZ = inputs.linkEntityZ();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        entities = inputs.entities();
        canEatEntity = inputs.canEatEntity();
        bowWowState = inputs.bowWowState();
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        variant = owner.selectedVariant[slot];
        // $05:4060 draws the saved variant; $44CB then selects the next one
        // from the existing speeds, before gravity and state dispatch.
        if ((owner.speedX[slot] | owner.speedY[slot]) != 0) {
            owner.selectedVariant[slot] = owner.movementVariant(frameCounter, slot);
        }

        if (owner.privateState4[slot] == 0) {
            return new BowWowMotion.Update(owner.setup(entity), owner.targetSlot[slot], null);
        }

        if (bowWowState != 0 && bowWowState != 0x80) {
            owner.targetX[slot] = BowWowMotion.byteValue(linkEntityX);
            // Kidnapped BowWow retains the pen anchor written by setup.
            // Following BowWow uses hLinkPositionZModified ($05:40E6).
            owner.targetY[slot] = BowWowMotion.byteValue(linkEntityY - linkEntityZ);
        }

        wasAboveGround = owner.updateZ(entity.z(), slot);
        x = entity.x();
        y = entity.y();
        z = owner.updatedZ[slot];
        targetContact = null;

        switch (owner.activeState[slot]) {
            case 0 -> {
                if (!advanceState0()) return completedResult;
            }
            case 1 -> {
                if (!advanceState1()) return completedResult;
            }
            case 2, 4 -> {
                if (!advanceState2()) return completedResult;
            }
            case 3 -> {
                if (!advanceState3()) return completedResult;
            }
            default -> owner.activeState[slot] = 0;
        }

        return new BowWowMotion.Update(
                BowWowMotion.withPositionAndVariant(entity, x, y, z, variant), owner.targetSlot[slot], targetContact);
    }

    private linksawakening.world.entity.motion.BowWowMotion.Update completedResult;

    private boolean advanceState0() {
        if (owner.transitionCountdown[slot] == 0) {
            owner.transitionCountdown[slot] = 0x28;
            if (bowWowState == 0) {
                owner.speedZ[slot] = 0x20;
                owner.activeState[slot] = 2;
                BowWowMotion.Vector vector = BowWowMotion.vectorTowardsTarget(
                        entity, BowWowMotion.withPositionAndVariant(entity, linkEntityX, linkEntityY, 0, 0), z, 0x20);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                return true;
            }
            // $05:4297 skips both target scanning and its RNG read
            // while BowWow is chained inside the hideout.
            int selectedTarget = bowWowState == 0x80
                    ? -1
                    : BowWowMotion.findTarget(
                            entity, linkEntityX, linkEntityY, randomByteSupplier, entities, canEatEntity);
            if (selectedTarget >= 0) {
                RoomEntity target = entities.get(selectedTarget);
                owner.targetSlot[slot] = selectedTarget;
                BowWowMotion.Vector vector = BowWowMotion.vectorTowardsTarget(entity, target, z, 0x30);
                owner.speedX[slot] = vector.x();
                owner.speedY[slot] = vector.y();
                owner.speedZ[slot] = 0x10;
                owner.activeState[slot] = 4;
            } else {
                owner.transitionCountdown[slot] = 0x10;
            }
        } else if (owner.privateCountdown1[slot] == 0) {
            owner.privateCountdown1[slot] = 0x20 + (randomByteSupplier.getAsInt() & 0x3F);
            owner.activeState[slot] = 1;
            int direction = randomByteSupplier.getAsInt() & 0x07;
            owner.speedX[slot] = SPEED_X_BY_RANDOM[direction];
            owner.speedY[slot] = SPEED_Y_BY_RANDOM[direction];
        }

        return true;
    }

    private boolean advanceState1() {
        if (owner.privateCountdown1[slot] == 0) {
            owner.privateCountdown1[slot] = 0x20;
            // func_005_41CF increments the state pointer, then writes
            // B (zero), returning to the target-selection handler.
            owner.activeState[slot] = 0;
        }
        if (wasAboveGround) {
            owner.speedZ[slot] = 0x10;
        }
        int[] moved = owner.move(entity, x, y, slot, backgroundCollision);
        x = owner.correctTargetX(moved[0], x, slot);
        y = owner.correctTargetY(moved[1], y, slot);

        return true;
    }

    private boolean advanceState2() {
        boolean stopLunge = owner.transitionCountdown[slot] == 0;
        if (owner.transitionCountdown[slot] != 0) {
            int[] moved = owner.move(entity, x, y, slot, backgroundCollision);
            x = owner.correctTargetX(moved[0], x, slot);
            y = owner.correctTargetY(moved[1], y, slot);
            // func_005_4228 returns E>1 when either axis is restored;
            // $41F6 then enters the same pause as an expired lunge.
            stopLunge = !BowWowMotion.inSignedWindow(BowWowMotion.signedByte(owner.targetX[slot] - moved[0]), 0x20)
                    || !BowWowMotion.inSignedWindow(BowWowMotion.signedByte(owner.targetY[slot] - moved[1]), 0x20);
        }
        if (stopLunge) {
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            owner.activeState[slot] = 3;
            owner.transitionCountdown[slot] = 0x10;
        }
        // func_005_41EB reaches label_005_4335 after every moving
        // state-2/state-4 frame, not only when the timer expires.
        if (bowWowState != 0) {
            targetContact = BowWowMotion.findTargetContact(
                    entity.x(), entity.y(), entity.z(), owner.targetSlot[slot], entities);
        }

        return true;
    }

    private boolean advanceState3() {
        if (owner.transitionCountdown[slot] == 0) {
            // func_005_420E starts a ten-frame pause, then writes B
            // (zero) back to wEntitiesStateTable for a fresh scan.
            int pause = (randomByteSupplier.getAsInt() & 0x3F) + 0x30;
            owner.transitionCountdown[slot] = bowWowState == 0 ? pause : 0x10;
            owner.activeState[slot] = 0;
        }

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.BowWowMotion.Update run(
            linksawakening.world.entity.motion.BowWowMotion owner,
            linksawakening.world.entity.motion.BowWowCollisionFrame inputs) {
        if (inUse) {
            return new BowWowStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        linkEntityZ = 0;
        randomByteSupplier = null;
        backgroundCollision = null;
        entities = null;
        canEatEntity = null;
        bowWowState = 0;
        slot = 0;
        variant = 0;
        wasAboveGround = false;
        x = 0;
        y = 0;
        z = 0;
        targetContact = null;
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
            this.entities = null;
            this.canEatEntity = null;
            this.targetContact = null;
            this.completedResult = null;
        }
    }
}
