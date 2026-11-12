package linksawakening.world.entity.motion;

/** Per-invocation scratch for StalfosEvasiveMotion.advance; values are assigned in source order. */
final class StalfosEvasiveFrameStep {
    private final StalfosEvasiveMotion owner;
    private linksawakening.world.entity.motion.StalfosEvasiveFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private boolean actionButtonsHeld;
    private int transitionCountdown;
    private int ignoreHitsCountdown;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int mapId;
    private int slot;
    private boolean cloneBranchReady;
    private int frame;
    private linksawakening.world.entity.motion.StalfosEvasiveMotion.CloneRequest cloneRequest;
    private int x;
    private int y;
    private linksawakening.world.entity.motion.StalfosEvasiveMotion.MoveResult movement;
    private int distanceX;
    private int distanceY;
    private int variant;

    StalfosEvasiveFrameStep(StalfosEvasiveMotion owner, linksawakening.world.entity.motion.StalfosEvasiveFrame inputs) {
        this.owner = owner;
        this.inputs = inputs;
    }

    linksawakening.world.entity.motion.StalfosEvasiveMotion.Update run() {
        if (!prepareCloneBranch()) return completedResult;
        if (!advanceMovement()) return completedResult;

        return new StalfosEvasiveMotion.Update(
                StalfosEvasiveMotion.withPositionAndVariant(entity, x, y, entity.z(), variant),
                transitionCountdown,
                false,
                false,
                cloneRequest);
    }

    private linksawakening.world.entity.motion.StalfosEvasiveMotion.Update completedResult;

    private boolean prepareCloneBranch() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        actionButtonsHeld = inputs.actionButtonsHeld();
        transitionCountdown = inputs.transitionCountdown();
        ignoreHitsCountdown = inputs.ignoreHitsCountdown();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        mapId = inputs.mapId();
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            // A Gibdo burn conversion calls ConfigureNewEntity.attributes and
            // returns ACTIVE without running EntityInitHandlersTable.$1E.
            owner.initializeForActiveEntity(slot);
        }
        cloneBranchReady = false;
        if (owner.privateCountdown1[slot] > 0) {
            owner.privateCountdown1[slot]--;
            cloneBranchReady = owner.privateCountdown1[slot] == 1;
        }

        frame = frameCounter & 0xFF;
        if (owner.privateState1[slot] != 0) {
            {
                completedResult = owner.advanceFleeing(
                        entity, frame, transitionCountdown, ignoreHitsCountdown, backgroundCollision);
                return false;
            }
        }

        cloneRequest = null;
        if (cloneBranchReady) {
            // The source returns immediately when hMapId is below
            // MAP_ANGLERS_TUNNEL, before its inertia dispatch or movement.
            if (mapId < 0x03) {
                int variant = (frame >>> 3) & 0x01;
                {
                    completedResult = new StalfosEvasiveMotion.Update(
                            StalfosEvasiveMotion.withPositionAndVariant(
                                    entity, entity.x(), entity.y(), entity.z(), variant),
                            transitionCountdown,
                            false,
                            false,
                            null);
                    return false;
                }
            }
            StalfosEvasiveMotion.Vector vector = StalfosEvasiveMotion.vectorTowardsLink(
                    entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x18);
            cloneRequest = new StalfosEvasiveMotion.CloneRequest(
                    slot, entity.x(), entity.y(), entity.z(), vector.x(), vector.y());
        }
        return true;
    }

    private boolean advanceMovement() {

        if (owner.inertia[slot] != 0) {
            {
                completedResult = owner.advanceAirborne(entity, transitionCountdown, backgroundCollision, cloneRequest);
                return false;
            }
        }

        x = StalfosEvasiveMotion.addSpeedToPosition(entity.x(), owner.speedX[slot], owner.speedXAccumulator, slot);
        y = StalfosEvasiveMotion.addSpeedToPosition(entity.y(), owner.speedY[slot], owner.speedYAccumulator, slot);
        movement = StalfosEvasiveMotion.applyBackground(entity, x, y, backgroundCollision);
        x = movement.x();
        y = movement.y();
        if (movement.xBlocked()) {
            owner.speedX[slot] ^= 0xF0;
        }
        if (movement.yBlocked()) {
            owner.speedY[slot] ^= 0xF0;
        }

        distanceX = StalfosEvasiveMotion.signedByte(linkEntityX - entity.x());
        distanceY = StalfosEvasiveMotion.signedByte(linkEntityY - entity.y());
        if (actionButtonsHeld
                && StalfosEvasiveMotion.distanceInJumpWindow(distanceX)
                && StalfosEvasiveMotion.distanceInJumpWindow(distanceY)) {
            transitionCountdown = 0x08;
            owner.privateCountdown1[slot] = 0;
            owner.inertia[slot] = 1;
            owner.speedZ[slot] = 0x15;
            StalfosEvasiveMotion.Vector vector = StalfosEvasiveMotion.vectorTowardsLink(
                    entity.x(), entity.y(), entity.z(), linkEntityX, linkEntityY, 0x12);
            owner.speedX[slot] = (-vector.x()) & 0xFF;
            owner.speedY[slot] = (-vector.y()) & 0xFF;
            {
                completedResult = new StalfosEvasiveMotion.Update(
                        StalfosEvasiveMotion.withPositionAndVariant(entity, x, y, entity.z(), 0x02),
                        transitionCountdown,
                        false,
                        false,
                        cloneRequest);
                return false;
            }
        }

        // jr_015_4FCE: the initialization state or random low six bits forces
        // a new choice; otherwise the current fixed-point speed is retained.
        if (owner.state[slot] != 0 || (randomByteSupplier.getAsInt() & 0x2F) == 0) {
            owner.chooseRandomDirection(slot, randomByteSupplier);
        }
        variant = (frame >>> 3) & 0x01;
        return true;
    }
}
