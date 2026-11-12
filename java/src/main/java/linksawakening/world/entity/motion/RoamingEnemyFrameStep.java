package linksawakening.world.entity.motion;

import linksawakening.world.entity.EntityBackgroundCollisionResult;

/** Reused, reentrancy-safe scratch for RoamingEnemyMotion.advanceInternal; values are assigned in source order. */
final class RoamingEnemyFrameStep {
    private RoamingEnemyMotion owner;
    private linksawakening.world.entity.motion.RoamingEnemyFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int linkEntityX;
    private int linkEntityY;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int ignoreHitsCountdown;
    private int frameCounter;
    private boolean creditsGameplay;
    private int slot;
    private int x;
    private int y;
    private int variant;

    RoamingEnemyFrameStep() {}

    linksawakening.world.entity.motion.RoamingEnemyMotion.Update run() {
        if (!prepareTimersAndMotion()) return completedResult;
        if (!chooseDirectionAndAnimation()) return completedResult;

        return new RoamingEnemyMotion.Update(RoamingEnemyMotion.withPositionAndVariant(entity, x, y, variant), null);
    }

    private linksawakening.world.entity.motion.RoamingEnemyMotion.Update completedResult;

    private boolean prepareTimersAndMotion() {

        entity = inputs.entity();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundInteraction = inputs.backgroundInteraction();
        ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        frameCounter = inputs.frameCounter();
        creditsGameplay = inputs.creditsGameplay();

        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
        }
        if (owner.transitionCountdown[slot] > 0) {
            owner.transitionCountdown[slot]--;
        }

        if (owner.state[slot] != 0) {
            RoamingEnemyMotion.LaunchRequest launchRequest =
                    owner.launchRequestIfEligible(entity, linkEntityX, linkEntityY, creditsGameplay);
            if (launchRequest != null && entity.type() == RoamingEnemyMotion.ENTITY_OCTOROK) {
                {
                    completedResult = new RoamingEnemyMotion.Update(entity, launchRequest);
                    return false;
                }
            }
            if (owner.transitionCountdown[slot] == 0) {
                owner.transitionCountdown[slot] = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
                owner.state[slot] = 0;
                owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0x03;
                owner.direction[slot] = owner.privateState1[slot] == 0
                        ? RoamingEnemyMotion.directionToLink(entity.x(), entity.y(), linkEntityX, linkEntityY)
                        : randomByteSupplier.getAsInt() & 0x03;
                owner.speedX[slot] = RoamingEnemyMotion.speedXFor(entity.type(), owner.direction[slot]);
                owner.speedY[slot] = RoamingEnemyMotion.speedYFor(entity.type(), owner.direction[slot]);
            } else {
                // $03:7A30-7A64 clears collisions, then skips both wall
                // probes when paused (both speeds are zero).
                owner.collisionsTable[slot] = 0;
                owner.horizontallyCollidedObject[slot] = 0;
            }
            {
                completedResult = new RoamingEnemyMotion.Update(entity, launchRequest);
                return false;
            }
        }

        x = entity.x();
        y = entity.y();
        return true;
    }

    private boolean chooseDirectionAndAnimation() {

        if ((owner.collisionsTable[slot] & 0x0F) != 0 || owner.transitionCountdown[slot] == 0) {
            owner.transitionCountdown[slot] = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
            owner.state[slot] = 1;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
            owner.collisionsTable[slot] = 0;
        } else {
            int nextX = RoamingEnemyMotion.addSpeedToPosition(x, owner.speedX[slot], owner.speedXAccumulator, slot);
            if (nextX != x && backgroundInteraction != null) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                        entity, owner.direction[slot], nextX, y, ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    owner.collisionsTable[slot] |= result.collisionFlag();
                    owner.horizontallyCollidedObject[slot] = result.objectId();
                } else {
                    x = nextX;
                }
            } else {
                x = nextX;
            }

            int nextY = RoamingEnemyMotion.addSpeedToPosition(y, owner.speedY[slot], owner.speedYAccumulator, slot);
            if (nextY != y && backgroundInteraction != null) {
                EntityBackgroundCollisionResult result = backgroundInteraction.probe(
                        entity, owner.direction[slot], x, nextY, ignoreHitsCountdown, frameCounter);
                if (result.blocked()) {
                    owner.collisionsTable[slot] |= result.collisionFlag();
                    owner.verticallyCollidedObject[slot] = result.objectId();
                } else {
                    y = nextY;
                }
            } else {
                y = nextY;
            }
        }

        owner.inertia[slot] = (owner.inertia[slot] + 1) & 0xFF;
        variant = RoamingEnemyMotion.VARIANT_BY_DIRECTION[owner.direction[slot]] | ((owner.inertia[slot] >>> 3) & 0x01);
        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.RoamingEnemyMotion.Update run(
            RoamingEnemyMotion owner, linksawakening.world.entity.motion.RoamingEnemyFrame inputs) {
        if (inUse) {
            return new RoamingEnemyFrameStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        linkEntityX = 0;
        linkEntityY = 0;
        randomByteSupplier = null;
        backgroundInteraction = null;
        ignoreHitsCountdown = 0;
        frameCounter = 0;
        creditsGameplay = false;
        slot = 0;
        x = 0;
        y = 0;
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
            this.backgroundInteraction = null;
            this.completedResult = null;
        }
    }
}
