package linksawakening.world.entity.motion;

import linksawakening.world.entity.*;

/** Reused, reentrancy-safe scratch for BooBuddyMotion.advanceAfterTimers; values are assigned in source order. */
final class BooBuddyFrameStep {
    private BooBuddyMotion owner;
    private linksawakening.world.entity.motion.BooBuddyFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int transitionCountdown;
    private int roomTriggerCount;
    private boolean swordMiddle;
    private java.util.function.IntSupplier randomByteSupplier;
    private int slot;
    private int countdown;
    private int x;
    private int y;
    private int renderedVariant;
    private int variant;
    private boolean unloaded;
    private int healthOverride;

    BooBuddyFrameStep() {}

    linksawakening.world.entity.motion.BooBuddyMotion.Update run() {
        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        transitionCountdown = inputs.transitionCountdown();
        roomTriggerCount = inputs.roomTriggerCount();
        swordMiddle = inputs.swordMiddle();
        randomByteSupplier = inputs.randomByteSupplier();

        if (entity == null || (entity.type() & 0xFF) != BooBuddyMotion.ENTITY_TYPE) {
            throw new IllegalArgumentException("Unsupported Boo Buddy motion entity");
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Boo Buddy random-byte supplier cannot be null");
        }
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        countdown = transitionCountdown & 0xFF;
        x = BooBuddyMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        y = BooBuddyMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
        // $06:79AC renders before the state handlers write the next variant.
        renderedVariant = owner.selectedVariant[slot];
        variant = renderedVariant;
        // func_006_5E54 reads the active coordinates cached before movement.
        unloaded = ((entity.y() - entity.z()) & 0xFF) >= 0x88 || entity.x() >= 0xA8;
        healthOverride = -1;

        // func_006_5E54 runs before the state dispatch. ClearEntityStatus_06
        // does not persist the Boo Buddy when this boundary is reached, so
        // return the unload request before changing its state.
        if (unloaded) {
            return new BooBuddyMotion.Update(
                    BooBuddyMotion.withPosition(entity, x, y, variant), countdown, unloaded, healthOverride);
        }

        switch (owner.state[slot]) {
            case 0 -> {
                if (!advanceState0()) return completedResult;
            }
            case 1 -> {
                if (!advanceState1()) return completedResult;
            }
            default -> throw new IllegalStateException("Invalid Boo Buddy state: " + owner.state[slot]);
        }

        owner.selectedVariant[slot] = variant;
        return new BooBuddyMotion.Update(
                BooBuddyMotion.withPosition(entity, x, y, renderedVariant), countdown, false, healthOverride);
    }

    private linksawakening.world.entity.motion.BooBuddyMotion.Update completedResult;

    private boolean advanceState0() {
        if (roomTriggerCount != 0) {
            owner.state[slot] = 1;
            return true;
        }
        if (swordMiddle && countdown == 0 && owner.privateCountdown1[slot] == 0) {
            if (!BooBuddyMotion.withinSwordTriggerWindow(x, y, linkEntityX, linkEntityY)) {
                // Both distant-swing branches jump straight to animation.
                variant = BooBuddyMotion.directionVariant(x, linkEntityX, frameCounter);
                return true;
            }
            countdown = 0x20;
        }
        if (countdown != 0) {
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            variant = owner.animateTransition(frameCounter, x, linkEntityX, slot, countdown);
            if (owner.privateState1[slot] != 0) {
                // func_006_7AB0 sets this flag at countdown one, then
                // func_006_7A8C reflects the position around ($50,$48).
                owner.privateCountdown1[slot] = 0x20;
                owner.privateState1[slot] = 0;
                x = (0xA0 - x) & 0xFF;
                y = (0x90 - y) & 0xFF;
                variant = owner.animateTransition(frameCounter, x, linkEntityX, slot, countdown);
            }
            return true;
        }
        if (owner.privateCountdown1[slot] != 0) {
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            variant = owner.animateTransition(frameCounter, x, linkEntityX, slot, countdown);
            return true;
        }

        int length = 0x06 + ((randomByteSupplier.getAsInt() ^ slot) & 0x07);
        BooBuddyMotion.Vector vector = BooBuddyMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, length);
        owner.speedY[slot] = BooBuddyMotion.approachByFour(owner.speedY[slot], vector.y());
        owner.speedX[slot] = BooBuddyMotion.approachByFour(owner.speedX[slot], vector.x());
        variant = BooBuddyMotion.directionVariant(x, linkEntityX, frameCounter);

        return true;
    }

    private boolean advanceState1() {
        if (roomTriggerCount == 0) {
            owner.state[slot] = 0;
            return true;
        }
        healthOverride = 0x01;
        BooBuddyMotion.Vector vector = BooBuddyMotion.vectorTowardsLink(x, y, linkEntityX, linkEntityY, 0x04);
        owner.speedY[slot] = BooBuddyMotion.negate(vector.y());
        owner.speedX[slot] = BooBuddyMotion.negate(vector.x());
        variant = 0x04 + ((frameCounter >>> 4) & 0x01);

        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.BooBuddyMotion.Update run(
            BooBuddyMotion owner, linksawakening.world.entity.motion.BooBuddyFrame inputs) {
        if (inUse) {
            return new BooBuddyFrameStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        transitionCountdown = 0;
        roomTriggerCount = 0;
        swordMiddle = false;
        randomByteSupplier = null;
        slot = 0;
        countdown = 0;
        x = 0;
        y = 0;
        renderedVariant = 0;
        variant = 0;
        unloaded = false;
        healthOverride = 0;
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
            this.completedResult = null;
        }
    }
}
