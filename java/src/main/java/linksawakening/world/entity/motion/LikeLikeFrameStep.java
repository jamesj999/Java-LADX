package linksawakening.world.entity.motion;

import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.LikeLikeEvent;

/** Reused, reentrancy-safe scratch for LikeLikeMotion.advance; values are assigned in source order. */
final class LikeLikeFrameStep {
    private LikeLikeMotion owner;
    private linksawakening.world.entity.motion.LikeLikeFrame inputs;
    private linksawakening.world.entity.RoomEntity entity;
    private int frameCounter;
    private int linkEntityX;
    private int linkEntityY;
    private int linkZ;
    private int linkMotionState;
    private boolean actionButtonsHeld;
    private int linkItemA;
    private int linkItemB;
    private int shieldLevel;
    private int slowTransitionCountdown;
    private java.util.function.IntSupplier randomByteSupplier;
    private linksawakening.world.entity.RoomEntityBackgroundCollision backgroundCollision;
    private int slot;
    private int renderedVariant;
    private int stolenInventorySlot;
    private int stolenShieldLevel;

    LikeLikeFrameStep() {}

    linksawakening.world.entity.motion.LikeLikeMotion.Update run() {
        if (!advanceFreeMotion()) return completedResult;
        if (!advanceSwallowing()) return completedResult;

        return new LikeLikeMotion.Update(
                entity,
                slowTransitionCountdown,
                new LikeLikeEvent(
                        slot,
                        LikeLikeEvent.Kind.CAPTURE,
                        entity.x(),
                        entity.y(),
                        stolenInventorySlot,
                        stolenShieldLevel),
                renderedVariant);
    }

    private linksawakening.world.entity.motion.LikeLikeMotion.Update completedResult;

    private boolean advanceFreeMotion() {

        entity = inputs.entity();
        frameCounter = inputs.frameCounter();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkZ = inputs.linkZ();
        linkMotionState = inputs.linkMotionState();
        actionButtonsHeld = inputs.actionButtonsHeld();
        linkItemA = inputs.linkItemA();
        linkItemB = inputs.linkItemB();
        shieldLevel = inputs.shieldLevel();
        slowTransitionCountdown = inputs.slowTransitionCountdown();
        randomByteSupplier = inputs.randomByteSupplier();
        backgroundCollision = inputs.backgroundCollision();
        slot = entity.slot();
        if (!owner.initialized[slot]) {
            owner.initialize(slot);
        }
        renderedVariant = owner.walkMotion.spriteVariant(slot);

        if (owner.privateState3[slot] == 0) {
            if (slowTransitionCountdown == 0
                    && linkZ == 0
                    && linkMotionState < LikeLikeMotion.LINK_MOTION_NON_INTERACTIVE
                    && RoomEntityCombatRules.collisionCadenceMatches(frameCounter, slot)
                    && RoomEntityCombatRules.overlapsLink(entity, linkEntityX, linkEntityY)) {
                owner.privateState3[slot] = 1;
                owner.inertia[slot] = 0;
            }
            {
                completedResult = new LikeLikeMotion.Update(
                        owner.walkMotion.advance(entity, randomByteSupplier, backgroundCollision),
                        slowTransitionCountdown,
                        null,
                        -1);
                return false;
            }
        }

        if (actionButtonsHeld) {
            owner.inertia[slot] = (owner.inertia[slot] + 1) & 0xFF;
            if (owner.inertia[slot] >= 0x08) {
                owner.privateState3[slot] = 0;
                slowTransitionCountdown = 0x15;
                {
                    completedResult = new LikeLikeMotion.Update(
                            entity,
                            slowTransitionCountdown,
                            new LikeLikeEvent(slot, LikeLikeEvent.Kind.RELEASE, entity.x(), entity.y(), -1, 0),
                            renderedVariant);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean advanceSwallowing() {

        stolenInventorySlot = -1;
        stolenShieldLevel = 0;
        if (owner.privateState1[slot] == 0) {
            if (linkItemB == LikeLikeMotion.INVENTORY_SHIELD) {
                if (shieldLevel < 2) {
                    owner.privateState1[slot] = shieldLevel & 0xFF;
                    stolenInventorySlot = 0;
                    stolenShieldLevel = shieldLevel & 0xFF;
                }
            } else if (linkItemA == LikeLikeMotion.INVENTORY_SHIELD && shieldLevel < 2) {
                owner.privateState1[slot] = shieldLevel & 0xFF;
                stolenInventorySlot = 1;
                stolenShieldLevel = shieldLevel & 0xFF;
            }
        }

        // $7E27-$7E55 hides Link and removes the shield before checking his
        // motion state. Only the position writes and animation calls are gated.
        if (linkMotionState != LikeLikeMotion.LINK_MOTION_DEFAULT) {
            {
                completedResult = new LikeLikeMotion.Update(
                        entity,
                        slowTransitionCountdown,
                        new LikeLikeEvent(
                                slot,
                                LikeLikeEvent.Kind.CAPTURE,
                                entity.x(),
                                entity.y(),
                                stolenInventorySlot,
                                stolenShieldLevel,
                                false),
                        renderedVariant);
                return false;
            }
        }

        // Both $7F05 calls use the same privateState4 counter as walking.
        // The handler rendered before these writes, so present the old frame.
        owner.walkMotion.advanceAnimation(slot);
        owner.walkMotion.advanceAnimation(slot);
        return true;
    }

    private boolean inUse;

    linksawakening.world.entity.motion.LikeLikeMotion.Update run(
            LikeLikeMotion owner, linksawakening.world.entity.motion.LikeLikeFrame inputs) {
        if (inUse) {
            return new LikeLikeFrameStep().run(owner, inputs);
        }
        inUse = true;
        entity = null;
        frameCounter = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        linkZ = 0;
        linkMotionState = 0;
        actionButtonsHeld = false;
        linkItemA = 0;
        linkItemB = 0;
        shieldLevel = 0;
        slowTransitionCountdown = 0;
        randomByteSupplier = null;
        backgroundCollision = null;
        slot = 0;
        renderedVariant = 0;
        stolenInventorySlot = 0;
        stolenShieldLevel = 0;
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
            this.completedResult = null;
        }
    }
}
