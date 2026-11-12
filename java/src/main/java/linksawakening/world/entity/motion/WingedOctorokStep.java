package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntitySnapshots;

/** Reused, reentrancy-safe scratch for WingedOctorokMotion.advance; values are assigned in source order. */
final class WingedOctorokStep {
    private WingedOctorokMotion owner;
    private linksawakening.world.entity.motion.WingedOctorokInteractionFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int transitionCountdown;
    private int ignoreHitsCountdown;
    private boolean actionButtonAHeld;
    private boolean actionButtonBHeld;
    private int linkItemA;
    private int linkItemB;
    private int slot;
    private boolean wasInitialized;
    private int renderedVariant;
    private int countdown;
    private int x;
    private int y;
    private linksawakening.world.entity.motion.WingedOctorokMotion.Position moved;
    private int z;
    private boolean landed;
    private boolean stateTwoHandler;
    private linksawakening.world.entity.motion.WingedOctorokMotion.RockSpawn rockSpawn;
    private boolean jumped;
    private linksawakening.world.entity.RoomEntity updated;

    WingedOctorokStep() {}

    linksawakening.world.entity.motion.WingedOctorokMotion.Update run() {
        if (!prepareMotion()) return completedResult;
        if (!advanceLandingAndState()) return completedResult;

        return new WingedOctorokMotion.Update(
                updated, owner.state[slot], countdown, rockSpawn, jumped, stateTwoHandler || jumped);
    }

    private linksawakening.world.entity.motion.WingedOctorokMotion.Update completedResult;

    private boolean advanceState0() {
        if (moved.collided() || countdown == 0) {
            countdown = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
            owner.state[slot] = 1;
            owner.clearHorizontalSpeed(slot);
        }
        owner.setVariantForDirection(slot);

        return true;
    }

    private boolean advanceState1() {
        if (countdown != 0) {
            if (countdown == 0x0A
                    && owner.direction[slot] == WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY)) {
                rockSpawn = new WingedOctorokMotion.RockSpawn(owner.direction[slot]);
            }
            owner.clearHorizontalSpeed(slot);
        } else {
            countdown = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
            owner.state[slot] = 0;
            owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0x03;
            if (owner.privateState1[slot] == 0) {
                owner.direction[slot] = WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY);
            } else {
                owner.direction[slot] = randomByteSupplier.getAsInt() & 0x03;
            }
            owner.speedX[slot] = WingedOctorokMotion.SPEED_X_BY_DIRECTION[owner.direction[slot]];
            owner.speedY[slot] = WingedOctorokMotion.SPEED_Y_BY_DIRECTION[owner.direction[slot]];
        }

        return true;
    }

    private boolean advanceState2() {
        owner.privateState2[slot] = (frameCounter >>> 2) & 0x01;
        owner.direction[slot] = WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY);
        owner.setVariantForDirection(slot);
        if (landed) {
            owner.state[slot] = 1;
            countdown = 0x20;
        }

        return true;
    }

    private boolean prepareMotion() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        transitionCountdown = inputs.transitionCountdown();
        ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        actionButtonAHeld = inputs.actionButtonAHeld();
        actionButtonBHeld = inputs.actionButtonBHeld();
        linkItemA = inputs.linkItemA();
        linkItemB = inputs.linkItemB();
        if ((entity.type() & 0xFF) != WingedOctorokMotion.ENTITY_WINGED_OCTOROK) {
            throw new IllegalArgumentException(
                    "Unsupported Winged Octorok motion entity type: 0x" + Integer.toHexString(entity.type()));
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Winged Octorok random-byte supplier cannot be null");
        }

        slot = entity.slot();
        wasInitialized = owner.initialized[slot];
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        if (!owner.zInitialized[slot]) {
            owner.zPosition[slot] = entity.z() & 0xFF;
            owner.zInitialized[slot] = true;
        }

        renderedVariant = wasInitialized ? owner.presentationVariant[slot] : entity.spriteVariant();
        countdown = transitionCountdown & 0xFF;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;

        // The handler forces state 1 while the shared recoil/ignore window is live.
        if ((ignoreHitsCountdown & 0xFF) != 0) {
            owner.state[slot] = 1;
            countdown = 0x40;
        }

        moved = owner.move(entity, x, y, backgroundCollision);
        x = moved.x();
        y = moved.y();
        z = WingedOctorokMotion.addSpeedToPosition(
                owner.zPosition[slot], owner.speedZ[slot], owner.speedZAccumulator, slot);
        owner.zPosition[slot] = z;
        owner.speedZ[slot] = (owner.speedZ[slot] - 1) & 0xFF;
        return true;
    }

    private boolean advanceLandingAndState() {

        landed = (z & 0x80) != 0;
        if (landed) {
            z = 0;
            owner.zPosition[slot] = 0;
            owner.speedZ[slot] = 0;
            owner.privateState2[slot] = 0;
        }

        stateTwoHandler = owner.state[slot] == 2;
        rockSpawn = null;
        jumped = false;
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
            default -> throw new IllegalStateException("Invalid Winged Octorok state: " + owner.state[slot]);
        }

        if (!stateTwoHandler
                && owner.privateCountdown2[slot] == 0
                && WingedOctorokMotion.withinSignedWindow(linkEntityX - x)
                && WingedOctorokMotion.withinSignedWindow(linkEntityY - y)
                && WingedOctorokMotion.swordAttackHeld(actionButtonAHeld, actionButtonBHeld, linkItemA, linkItemB)
                && owner.direction[slot]
                        != (WingedOctorokMotion.directionToLink(x, y, linkEntityX, linkEntityY) ^ 0x01)) {
            owner.speedZ[slot] = 0x18;
            WingedOctorokMotion.Vector vector =
                    WingedOctorokMotion.vectorTowardsLink(x, y, z, linkEntityX, linkEntityY, 0x10);
            owner.speedY[slot] = vector.y();
            owner.speedX[slot] = vector.x();
            owner.state[slot] = 2;
            jumped = true;
        }

        updated = EntitySnapshots.withMotion(entity, x, y, renderedVariant, entity.entityFlipAttribute(), z, -1, false);
        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.WingedOctorokMotion.Update run(
            WingedOctorokMotion owner, linksawakening.world.entity.motion.WingedOctorokInteractionFrame inputs) {
        if (inUse) {
            return new WingedOctorokStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        randomByteSupplier = null;
        backgroundCollision = null;
        transitionCountdown = 0;
        ignoreHitsCountdown = 0;
        actionButtonAHeld = false;
        actionButtonBHeld = false;
        linkItemA = 0;
        linkItemB = 0;
        slot = 0;
        wasInitialized = false;
        renderedVariant = 0;
        countdown = 0;
        x = 0;
        y = 0;
        moved = null;
        z = 0;
        landed = false;
        stateTwoHandler = false;
        rockSpawn = null;
        jumped = false;
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
            this.randomByteSupplier = null;
            this.backgroundCollision = null;
            this.moved = null;
            this.rockSpawn = null;
            this.updated = null;
            this.completedResult = null;
        }
    }
}
