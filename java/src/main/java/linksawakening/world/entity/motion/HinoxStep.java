package linksawakening.world.entity.motion;

import static linksawakening.world.entity.motion.HinoxMotion.*;

/** HinoxEntityHandler and HinoxState1Handler (bank $06); reentrancy-safe frame scratch. */
final class HinoxStep {
    private HinoxMotion owner;
    private HinoxFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int transitionCountdown;
    private int frameCounter;
    private int linkX;
    private int linkY;
    private int linkZ;
    private int linkMotionState;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundInteraction backgroundInteraction;
    private int entityCollisionFlagsForFrame;
    private int flashCountdown;
    private int slot;
    private boolean linkMotionBlockedAtEntry;
    private int x;
    private int y;
    private int transition;
    private int frame;
    private int linkSpeedX;
    private int linkSpeedY;
    private int linkVelocityZ;
    private int airborne;
    private int linkDamage;
    private int jingle;
    private int wave;
    private boolean dust;
    private HinoxMotion.BombSpawn bomb;
    private int spriteVariant;

    HinoxStep() {}

    private HinoxMotion.Update run() {
        if (!prepareFrame()) return completedResult;
        if (!applyFlashBombTransition()) return completedResult;
        if (!advanceBehavior()) return completedResult;

        return result(spriteVariant, linkMotionBlockedAtEntry, linkX, linkY, 0, false, false);
    }

    private HinoxMotion.Update completedResult;

    private boolean advanceInitial() {
        if (((owner.entityCollisionFlags[slot] | entityCollisionFlagsForFrame) & 0x0F) != 0 || transition == 0) {
            transition = 0x10 | (randomByteSupplier.getAsInt() & 0x0F);
            owner.state[slot] = STATE_WANDER;
            owner.speedX[slot] = 0;
            owner.speedY[slot] = 0;
        }
        HinoxMotion.Position next = owner.move(entity, x, y, backgroundInteraction, slot);
        x = next.x();
        y = next.y();
        spriteVariant = owner.tickInertia(slot);

        return true;
    }

    private boolean advanceWander() {
        if (transition == 0) {
            owner.privateState3[slot] = (owner.privateState3[slot] + 1) & 0xFF;
            if (owner.privateState3[slot] == 2) {
                owner.privateState3[slot] = 0;
                if ((randomByteSupplier.getAsInt() & 0x01) == 0) {
                    owner.state[slot] = STATE_CHARGE_WINDUP;
                    transition = 0x30;
                    owner.speedX[slot] = 0;
                    owner.speedY[slot] = 0;
                    return true;
                }
            }
            transition = 0x20 | (randomByteSupplier.getAsInt() & 0x1F);
            owner.state[slot] = STATE_WANDER;
            owner.privateState1[slot] = (owner.privateState1[slot] + 1) & 0x03;
            int direction = owner.privateState1[slot] == 0
                    ? HinoxMotion.directionToLink(x, y, linkX, linkY)
                    : randomByteSupplier.getAsInt() & 0x03;
            owner.speedX[slot] = SPEED_X_BY_DIRECTION[direction];
            owner.speedY[slot] = SPEED_Y_BY_DIRECTION[direction];
        }
        spriteVariant = entity.spriteVariant();

        return true;
    }

    private boolean advanceChargeWindup() {
        if (transition == 0) {
            transition = 0x20;
            owner.state[slot] = STATE_CHARGE;
            HinoxMotion.Vector vector = HinoxMotion.vectorTowardsLink(x, y, entity.z(), linkX, linkY, 0x18);
            owner.speedX[slot] = vector.x();
            owner.speedY[slot] = vector.y();
        }
        // This branch advances inertia twice in the ROM; preserve both calls.
        owner.tickInertia(slot);
        spriteVariant = owner.tickInertia(slot);
        if ((frame & 0x0F) == 0) {
            jingle = 0x20;
        }

        return true;
    }

    private boolean advanceCharge() {
        if (transition == 0) {
            owner.state[slot] = STATE_GRAB;
            transition = 0x4F;
            wave = 0x16;
        }
        HinoxMotion.Position next = owner.move(entity, x, y, backgroundInteraction, slot);
        x = next.x();
        y = next.y();
        if (owner.state[slot] == STATE_CHARGE
                && HinoxMotion.withinDistance(x, linkX, 0x18)
                && HinoxMotion.withinDistance(y, linkY, 0x18)
                && (linkMotionState & 0xFF) == 0) {
            owner.state[slot] = STATE_GRAB;
            transition = 0x4F;
            jingle = 0x16;
        }
        dust = (frame & 0x07) == 0;
        if (dust) {
            // State 3 writes hMultiPurpose0/1 for the later bomb
            // handler. Preserve those source registers per entity.
            owner.bombOriginX[slot] = x & 0xFF;
            owner.bombOriginY[slot] = (y + 0x0A) & 0xFF;
            owner.bombOriginWritten[slot] = true;
        }
        // This branch advances inertia twice in the ROM; preserve both calls.
        owner.tickInertia(slot);
        spriteVariant = owner.tickInertia(slot);
        if ((frame & 0x0F) == 0) {
            jingle = 0x20;
        }

        return true;
    }

    private boolean advanceGrab() {
        if (transition == 0) {
            owner.state[slot] = STATE_INITIAL;
            transition = 0;
            completedResult = result(spriteVariant, linkMotionBlockedAtEntry, linkX, linkY, linkZ, false, false);
            return false;
        } else if (transition == 0x20) {
            linkSpeedY = 0x20;
            linkSpeedX = (linkX & 0xFF) >= 0x50 ? 0xE0 : 0x20;
            linkVelocityZ = 0x10;
            airborne = 0x02;
            linkDamage = 0x08;
            jingle = 0x08;
            linkX = x;
            linkY = y;
            owner.privateCountdown1[slot] = 0x50;
            {
                completedResult =
                        result(entity.spriteVariant(), linkMotionBlockedAtEntry, linkX, linkY, linkZ, true, false);
                return false;
            }
        }
        return holdGrabbedLink();
    }

    private boolean advanceBomb() {
        if (transition == 0) {
            owner.state[slot] = STATE_INITIAL;
        } else if (transition == 0x10) {
            int originX = owner.bombOriginWritten[slot] ? owner.bombOriginX[slot] : x;
            int originY = owner.bombOriginWritten[slot] ? owner.bombOriginY[slot] : y;
            HinoxMotion.Vector vector =
                    HinoxMotion.vectorTowardsLink((originX - 0x0C) & 0xFF, originY, 0x10, linkX, linkY, 0x10);
            bomb = new HinoxMotion.BombSpawn(
                    ENTITY_BOMB, (originX - 0x0C) & 0xFF, originY, 0x10, vector.x(), vector.y(), 0x18, 0x40);
            jingle = 0x08;
        }
        spriteVariant = transition >= 0x10 ? 1 : 0;

        return true;
    }

    private boolean advanceDefault() {
        owner.state[slot] = STATE_INITIAL;
        owner.speedX[slot] = 0;
        owner.speedY[slot] = 0;
        spriteVariant = 0;

        return true;
    }

    private boolean prepareFrame() {

        entity = inputs.entity();
        transitionCountdown = inputs.transitionCountdown();
        frameCounter = inputs.frameCounter();
        linkX = inputs.linkX();
        linkY = inputs.linkY();
        linkZ = inputs.linkZ();
        linkMotionState = inputs.linkMotionState();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundInteraction = inputs.backgroundInteraction();
        entityCollisionFlagsForFrame = inputs.entityCollisionFlagsForFrame();
        flashCountdown = inputs.flashCountdown();
        HinoxMotion.validateSlot(entity.slot());
        if (!owner.initialized[entity.slot()]) {
            owner.initialize(entity.slot());
        }
        if (randomByteSupplier == null) {
            throw new IllegalArgumentException("Hinox random source cannot be null");
        }

        slot = entity.slot();
        linkMotionBlockedAtEntry = owner.privateCountdown1[slot] != 0;
        x = entity.x() & 0xFF;
        y = entity.y() & 0xFF;
        transition = transitionCountdown & 0xFF;
        frame = frameCounter & 0xFF;
        linkSpeedX = 0;
        linkSpeedY = 0;
        linkVelocityZ = 0;
        airborne = 0;
        linkDamage = 0;
        jingle = -1;
        wave = -1;
        dust = false;
        bomb = null;
        spriteVariant = entity.spriteVariant() < 0 ? 0 : entity.spriteVariant();
        return true;
    }

    private boolean applyFlashBombTransition() {

        // HinoxEntityHandler performs this branch before selecting a private
        // state. A sword flash at exactly $03 is the ROM's bomb transition.
        if (owner.state[slot] < STATE_GRAB && (flashCountdown & 0xFF) == 0x03) {
            owner.state[slot] = STATE_BOMB;
            transition = 0x20;
            completedResult = result(spriteVariant, linkMotionBlockedAtEntry, linkX, linkY, linkZ, false, false);
            return false;
        }
        return true;
    }

    private boolean advanceBehavior() {

        return switch (owner.state[slot]) {
            case STATE_INITIAL -> advanceInitial();
            case STATE_WANDER -> advanceWander();
            case STATE_CHARGE_WINDUP -> advanceChargeWindup();
            case STATE_CHARGE -> advanceCharge();
            case STATE_GRAB -> advanceGrab();
            case STATE_BOMB -> advanceBomb();
            default -> advanceDefault();
        };
    }

    private boolean holdGrabbedLink() {
        boolean blocked = linkMotionBlockedAtEntry;
        int heldX = linkX;
        int heldY = linkY;
        int heldZ = linkZ;
        boolean heldPositionWritten = false;
        boolean applyHeldLinkPose = false;
        if (transition >= 0x20 && transition <= 0x4F) {
            int index = (transition - 0x20) >>> 2;
            index = Math.min(index, GRAB_LINK_X_OFFSETS.length - 1);
            heldX = (x + GRAB_LINK_X_OFFSETS[index]) & 0xFF;
            heldY = y;
            heldZ = GRAB_LINK_Z[index];
            heldPositionWritten = true;
            airborne = 0x02;
            applyHeldLinkPose = true;
        }
        spriteVariant = GRAB_BODY_VARIANT[Math.min(transition >>> 3, GRAB_BODY_VARIANT.length - 1)];
        {
            completedResult =
                    result(spriteVariant, blocked, heldX, heldY, heldZ, heldPositionWritten, applyHeldLinkPose);
            return false;
        }
    }

    private HinoxMotion.Update result(
            int variant, boolean blocked, int heldX, int heldY, int heldZ, boolean heldWritten, boolean heldPose) {
        return owner.update(new HinoxResult(
                entity,
                x,
                y,
                transition,
                variant,
                blocked,
                heldX,
                heldY,
                heldZ,
                linkSpeedX,
                linkSpeedY,
                linkVelocityZ,
                airborne,
                linkDamage,
                jingle,
                wave,
                dust,
                heldWritten,
                heldPose,
                bomb));
    }

    private boolean inUse;

    HinoxMotion.Update run(HinoxMotion owner, HinoxFrame inputs) {
        if (inUse) {
            return new HinoxStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        transitionCountdown = 0;
        frameCounter = 0;
        linkX = 0;
        linkY = 0;
        linkZ = 0;
        linkMotionState = 0;
        randomByteSupplier = null;
        backgroundInteraction = null;
        entityCollisionFlagsForFrame = 0;
        flashCountdown = 0;
        slot = 0;
        linkMotionBlockedAtEntry = false;
        x = 0;
        y = 0;
        transition = 0;
        frame = 0;
        linkSpeedX = 0;
        linkSpeedY = 0;
        linkVelocityZ = 0;
        airborne = 0;
        linkDamage = 0;
        jingle = 0;
        wave = 0;
        dust = false;
        bomb = null;
        spriteVariant = 0;
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
            this.bomb = null;
            this.completedResult = null;
        }
    }
}
