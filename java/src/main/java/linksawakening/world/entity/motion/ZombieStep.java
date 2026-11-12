package linksawakening.world.entity.motion;

import java.util.Objects;
import linksawakening.world.entity.RoomEntityObjectSample;

/** Reused, reentrancy-safe scratch for ZombieMotion.advance; values are assigned in source order. */
final class ZombieStep {
    private ZombieMotion owner;
    private linksawakening.world.entity.motion.ZombieFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int transitionCountdown;
    private int physicsFlags;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityObjectQuery objectQuery;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int ignoreHitsCountdown;
    private int slot;
    private int countdown;
    private int nextPhysicsFlags;
    private int nextSpeedX;
    private int nextSpeedY;
    private linksawakening.world.entity.RoomEntity updated;
    private linksawakening.world.entity.motion.ZombieMotion.SpawnRequest spawnRequest;
    private boolean unloadRequested;
    private boolean appliesBackgroundInteraction;
    private int renderedVariant;
    private int variant;

    ZombieStep() {}

    linksawakening.world.entity.motion.ZombieMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!advanceMarkerOrZombie()) return completedResult;

        return owner.update(
                updated,
                countdown,
                nextPhysicsFlags,
                nextSpeedX,
                nextSpeedY,
                null,
                unloadRequested,
                appliesBackgroundInteraction);
    }

    private linksawakening.world.entity.motion.ZombieMotion.Update completedResult;

    private boolean advanceState0() {
        countdown = 0x30;
        owner.state[slot] = 1;

        return true;
    }

    private boolean advanceState1() {
        int animationSelector = countdown;
        if (countdown == 0) {
            countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x70;
            int vectorLength = (randomByteSupplier.getAsInt() & 0x07) + 0x05;
            ZombieMotion.Vector vector =
                    ZombieMotion.vectorTowardsLink(entity.x(), entity.y(), linkEntityX, linkEntityY, vectorLength);
            owner.speedX[slot] = vector.x();
            owner.speedY[slot] = vector.y();
            nextSpeedX = owner.speedX[slot];
            nextSpeedY = owner.speedY[slot];
            owner.state[slot] = 2;
            // ApplyVectorTowardsLink returns A = X speed, and both
            // ReloadSavedBank and IncrementEntityState preserve A.
            // The fall-through comparison at $6447 reads that byte.
            animationSelector = owner.speedX[slot];
        }
        variant = animationSelector >= 0x18 ? 1 : 2;

        return true;
    }

    private boolean advanceState2() {
        appliesBackgroundInteraction = true;
        ZombieMotion.PositionAndCollision moved = backgroundInteraction == null
                ? new ZombieMotion.PositionAndCollision(
                        ZombieMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot),
                        ZombieMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot),
                        0)
                : owner.moveWithBackground(entity, backgroundInteraction, ignoreHitsCountdown, frameCounter);
        int x = moved.x();
        int y = moved.y();
        int collisionMask = moved.collisionMask();
        updated = ZombieMotion.withPosition(entity, x, y);
        if ((collisionMask & 0x0F) != 0 || countdown == 0) {
            countdown = 0x30;
            owner.state[slot] = 3;
            nextPhysicsFlags |= ZombieMotion.PROJECTILE_NOCLIP_FLAG;
        }
        variant = 3 + ((frameCounter >>> 4) & 0x01);

        return true;
    }

    private boolean advanceState3() {
        if (countdown == 0) {
            unloadRequested = true;
        } else {
            variant = countdown >= 0x18 ? 2 : 1;
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
        randomByteSupplier = inputs.randomByteSupplier();
        objectQuery = inputs.objectQuery();
        backgroundInteraction = inputs.backgroundInteraction();
        ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        ZombieMotion.checkType(entity);
        Objects.requireNonNull(randomByteSupplier, "Zombie random-byte supplier");
        ZombieMotion.validateByte(transitionCountdown, "Zombie transition countdown");
        ZombieMotion.validateByte(physicsFlags, "Zombie physics flags");
        ZombieMotion.validateByte(ignoreHitsCountdown, "Zombie ignore-hits countdown");

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }

        countdown = transitionCountdown;
        nextPhysicsFlags = physicsFlags;
        nextSpeedX = owner.speedX[slot];
        nextSpeedY = owner.speedY[slot];
        updated = entity;
        spawnRequest = null;
        unloadRequested = false;
        appliesBackgroundInteraction = false;
        return true;
    }

    private boolean advanceMarkerOrZombie() {

        if (owner.privateState1[slot] == 0) {
            // The room-loaded entity is a permanently hidden spawn marker. The
            // handler still relocates it before testing the object physics.
            int variant = -1;
            if (countdown == 0) {
                int index = randomByteSupplier.getAsInt() & 0x07;
                int x = ZombieMotion.SPAWN_X[index];
                int y = ZombieMotion.SPAWN_Y[index];
                updated = ZombieMotion.withPositionAndVariant(entity, x, y, variant);
                RoomEntityObjectSample object = objectQuery == null ? null : objectQuery.sample(updated);
                if (object != null && ZombieMotion.isAllowedSpawnPhysics(object.physicsFlag())) {
                    countdown = (randomByteSupplier.getAsInt() & 0x3F) + 0x40;
                    spawnRequest = new ZombieMotion.SpawnRequest(x, y);
                }
            } else {
                updated = ZombieMotion.withVariant(entity, variant);
            }
            {
                completedResult = owner.update(
                        updated,
                        countdown,
                        nextPhysicsFlags,
                        nextSpeedX,
                        nextSpeedY,
                        spawnRequest,
                        false,
                        appliesBackgroundInteraction);
                return false;
            }
        }

        // $640F renders before any state handler writes the next stored frame.
        renderedVariant = owner.selectedVariant[slot];
        variant = renderedVariant;
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
            default -> throw new IllegalStateException("Invalid Zombie state: " + owner.state[slot]);
        }

        owner.selectedVariant[slot] = variant;
        updated = ZombieMotion.withVariant(updated, renderedVariant);
        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.ZombieMotion.Update run(
            ZombieMotion owner, linksawakening.world.entity.motion.ZombieFrame inputs) {
        if (inUse) {
            return new ZombieStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        frameCounter = 0;
        transitionCountdown = 0;
        physicsFlags = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        randomByteSupplier = null;
        objectQuery = null;
        backgroundInteraction = null;
        ignoreHitsCountdown = 0;
        slot = 0;
        countdown = 0;
        nextPhysicsFlags = 0;
        nextSpeedX = 0;
        nextSpeedY = 0;
        updated = null;
        spawnRequest = null;
        unloadRequested = false;
        appliesBackgroundInteraction = false;
        renderedVariant = 0;
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
            this.objectQuery = null;
            this.backgroundInteraction = null;
            this.updated = null;
            this.spawnRequest = null;
            this.completedResult = null;
        }
    }
}
