package linksawakening.world.entity.motion;

import java.util.Objects;

/** Per-invocation scratch for DogMotion.advance; values are assigned in source order. */
final class DogStep {
    private final DogMotion owner;
    private linksawakening.world.entity.motion.DogFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int transitionCountdown;
    private int physicsFlags;
    private int linkEntityX;
    private int linkEntityY;
    private int linkDirection;
    private boolean actionButtonAHeld;
    private boolean dialogActive;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int slot;
    private int countdown;
    private int nextPhysicsFlags;
    private int z;
    private boolean landed;
    private boolean appliesBackgroundInteraction;
    private boolean dialogRequested;
    private int dialogLowId;
    private int movementCollisionFlags;
    private linksawakening.world.entity.RoomEntity updated;

    DogStep(DogMotion owner, linksawakening.world.entity.motion.DogFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.motion.DogMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceBehavior()) return completedResult;

        return new DogMotion.Update(
                updated,
                owner.state[slot],
                countdown,
                owner.direction[slot] & 0xFF,
                owner.speedX[slot] & 0xFF,
                owner.speedY[slot] & 0xFF,
                owner.speedZ[slot] & 0xFF,
                nextPhysicsFlags & 0xFF,
                movementCollisionFlags & 0x0F,
                appliesBackgroundInteraction,
                dialogRequested,
                dialogLowId);
    }

    private linksawakening.world.entity.motion.DogMotion.Update completedResult;

    private boolean advanceState0() {
        if (countdown == 0) {
            int index = randomByteSupplier.getAsInt() & 0x07;
            owner.speedX[slot] = DogMotion.SPEEDS[index];
            owner.direction[slot] = index & 0x04;
            int yIndex = randomByteSupplier.getAsInt() & 0x07;
            owner.speedY[slot] = DogMotion.SPEEDS[yIndex];
            countdown = (randomByteSupplier.getAsInt() & 0x1F) + 0x30;
            owner.state[slot] = 1;
        }
        updated = DogMotion.withVariant(updated, owner.walkingVariant(slot, frameCounter));

        return true;
    }

    private boolean advanceState1() {
        DogMotion.PositionAndCollision moved = owner.moveWithBackground(updated, backgroundInteraction, slot);
        updated = DogMotion.withPositionAndVariant(
                updated, moved.x(), moved.y(), owner.walkingVariant(slot, frameCounter));
        movementCollisionFlags = moved.collisionFlags();
        appliesBackgroundInteraction = true;
        if (landed) {
            if (countdown != 0) {
                owner.speedZ[slot] = 0x08;
                z = (updated.z() + 1) & 0xFF;
                updated = DogMotion.withZ(updated, z);
            } else {
                countdown = 0x30;
                owner.state[slot] = 0;
            }
        }

        return true;
    }

    private boolean advanceState2() {
        if (countdown == 0) {
            owner.state[slot] = 3;
            DogMotion.Vector vector =
                    DogMotion.vectorTowardsLink(updated.x(), updated.y(), updated.z(), linkEntityX, linkEntityY, 0x24);
            owner.speedX[slot] = vector.x();
            owner.speedY[slot] = vector.y();
            owner.speedZ[slot] = 0x18;
            owner.direction[slot] = DogMotion.directionToLinkX(updated.x(), linkEntityX);
        }
        updated = DogMotion.withVariant(updated, owner.chargeVariant(slot, frameCounter));

        return true;
    }

    private boolean advanceState3() {
        DogMotion.PositionAndCollision moved = owner.moveWithBackground(updated, backgroundInteraction, slot);
        updated = DogMotion.withPositionAndVariant(updated, moved.x(), moved.y(), entity.spriteVariant());
        movementCollisionFlags = moved.collisionFlags();
        appliesBackgroundInteraction = true;
        // DogState3 briefly exposes $52 to func_003_6C6B, then restores
        // the harmless shadow flags before testing the collision byte.
        // The bounded Java port does not yet expose that separate
        // Link/entity collision callback, so only the restored value
        // leaves this motion object.
        nextPhysicsFlags = DogMotion.INITIAL_PHYSICS_FLAGS;
        if (landed) {
            owner.state[slot] = 0;
            countdown = 0x20;
        }

        return true;
    }

    private boolean prepareFrame() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        transitionCountdown = inputs.transitionCountdown();
        physicsFlags = inputs.physicsFlags();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkDirection = inputs.linkDirection();
        actionButtonAHeld = inputs.actionButtonAHeld();
        dialogActive = inputs.dialogActive();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundInteraction = inputs.backgroundInteraction();
        DogMotion.checkType(entity);
        DogMotion.validateByte(frameCounter, "Dog frame counter");
        DogMotion.validateByte(transitionCountdown, "Dog transition countdown");
        DogMotion.validateByte(physicsFlags, "Dog physics flags");
        DogMotion.validateByte(linkEntityX, "Dog Link X");
        DogMotion.validateByte(linkEntityY, "Dog Link Y");
        if (linkDirection < 0 || linkDirection > 3) {
            throw new IllegalArgumentException("Dog Link direction must be 0..3: " + linkDirection);
        }
        Objects.requireNonNull(randomByteSupplier, "Dog random-byte supplier");

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        countdown = transitionCountdown & 0xFF;
        nextPhysicsFlags = physicsFlags & 0xFF;
        z = DogMotion.addSpeedToPosition(entity.z(), owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.speedZ[slot] = (owner.speedZ[slot] - 2) & 0xFF;
        landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            owner.speedZ[slot] = 0;
        }

        // $19:490A preserves Z underflow in hMultiPurposeG. Background
        // collision flags are a different byte; states 1/3 test this landing
        // result at $19:49E1 and $19:4A41.
        appliesBackgroundInteraction = false;
        dialogRequested = false;
        dialogLowId = -1;
        movementCollisionFlags = 0;
        updated = DogMotion.withZ(entity, z);
        return true;
    }

    private boolean advanceBehavior() {

        // func_019_7CF0 runs before the state dispatch and only for states 0/1.
        if (owner.state[slot] < 2
                && owner.canTalk(updated, linkEntityX, linkEntityY, linkDirection, actionButtonAHeld, dialogActive)) {
            dialogRequested = true;
            dialogLowId = DogMotion.DIALOG_LOW_ID;
        }

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
            default -> throw new IllegalStateException("Invalid Dog state: " + owner.state[slot]);
        }
        return true;
    }
}
