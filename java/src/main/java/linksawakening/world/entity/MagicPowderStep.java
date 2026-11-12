package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Reused, reentrancy-safe scratch for MagicPowderBehavior.advanceMagicPowderSprinkleEntity; values are assigned in source order. */
final class MagicPowderStep {
    private linksawakening.world.entity.EntityProjectileController owner;
    private int index;
    private linksawakening.world.entity.RoomEntity entity;
    private int frame;
    private int linkMotionState;
    private boolean handlerInteractive;
    private boolean timersInteractive;
    private linksawakening.world.entity.EntityProjectileControllerServices runtime;
    private int slot;
    private int transitionCountdown;
    private linksawakening.world.entity.RoomEntityObjectQuery intersectionQuery;
    private linksawakening.world.entity.RoomEntityObjectSample object;
    private int objectLeft;
    private int objectTop;
    private int location;
    private int objectId;

    MagicPowderStep() {}

    void run() {
        if (!prepareCountdowns()) return;
        if (!prepareObjectInteraction()) return;
        if (!applyTorchInteraction()) return;
    }

    private boolean prepareCountdowns() {

        runtime = owner.runtime;
        slot = entity.slot();
        if (timersInteractive) {
            runtime.lifecycle().decrementSlowTransitionCountdown(slot, frame);
        }

        // func_018_7B02 renders before ReturnIfNonInteractive_18, even when
        // the handler is paused. Zero means no sprinkle sprite, not unloading.
        transitionCountdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        runtime.slots()[index] =
                withVariant(entity, transitionCountdown == 0 ? -1 : (transitionCountdown >>> 2) & 0x07);
        if (!handlerInteractive) {
            return false;
        }

        if (owner.readMagicPowderState(slot) != 0) {
            // State one has no ordinary sprinkle rectangle: the source emits
            // its short torch pair only at the state transition boundaries.
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) != 0) {
                runtime.slots()[index] = withVariant(entity, -1);
                return false;
            }

            int objectLeft = entity.x() & 0xF0;
            int objectTop = entity.y() & 0xF0;
            int location = (objectTop | (objectLeft >>> 4)) & 0xFF;
            runtime.events()
                    .magicPowderObjectRequests
                    .add(new MagicPowderObjectRequest(
                            slot, location, objectLeft, objectTop, MagicPowderObjectAction.EXTINGUISH_TORCH));
            // Keep the palette state visible to later powder handlers in
            // this pass, before RoomSession applies their object requests.
            if (runtime.roomVariables().readEntityRoomId() != 0x74 && owner.readTorchPaletteTargetAddress() < 0x0C) {
                owner.writeTorchPaletteTargetAddress(owner.readTorchPaletteTargetAddress() + (4));
                if (linkMotionState != 0x05) {
                    owner.writeTorchPaletteTransitionActive(true);
                }
            }
            runtime.clearEntity(slot);
            return false;
        }

        if (transitionCountdown == 0) {
            runtime.clearEntity(slot);
            return false;
        }
        return true;
    }

    private boolean prepareObjectInteraction() {

        if (transitionCountdown != 0x07) {
            if (transitionCountdown < 0x10 && owner.readMagicPowderPrivateState4(slot) == 0) {
                owner.collideMagicPowderWithEntities(entity, frame);
            }
            return false;
        }

        intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        object = intersectionQuery == null
                // The shared query samples (X, Y-8). Powder's $797E-$7997
                // formula instead samples (X-1, Y-9), including byte wrapping.
                ? null
                : intersectionQuery.sample(withPositionAndVariant(
                        entity, (entity.x() - 1) & 0xFF, (entity.y() - 1) & 0xFF, entity.spriteVariant()));
        objectLeft = EntityMotionMath.magicPowderObjectLeft(entity);
        objectTop = EntityMotionMath.magicPowderObjectTop(entity);
        location = (objectTop | (objectLeft >>> 4)) & 0xFF;
        objectId = object == null ? 0xFF : object.objectId() & 0xFF;

        if (!runtime.indoorRoom()
                && (objectId == EntityRomConstants.OBJECT_BUSH
                        || objectId == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS)) {
            runtime.events()
                    .magicPowderObjectRequests
                    .add(new MagicPowderObjectRequest(
                            slot, location, objectLeft, objectTop, MagicPowderObjectAction.REVEAL));
            runtime.events()
                    .transientVfxRequests
                    .add(new TransientVfxRequest(TransientVfxType.POOF, entity.x(), (entity.y() - entity.z()) & 0xFF));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.MAGIC_POWDER_POOF_JINGLE_ID));
            runtime.clearEntity(slot);
            return false;
        }
        return true;
    }

    private boolean applyTorchInteraction() {

        if (runtime.indoorRoom() && objectId == EntityRomConstants.OBJECT_TORCH_UNLIT) {
            // This motion-state check belongs only to GBC torch ignition.
            if (linkMotionState == 0x05 || owner.readTorchPaletteTransitionActive()) {
                return false;
            }
            if (owner.readTorchPaletteTargetAddress() != 0) {
                owner.writeTorchPaletteTargetAddress((owner.readTorchPaletteTargetAddress() - 4) & 0xFF);
                owner.writeTorchPaletteTransitionActive(true);
            }
            owner.writeMagicPowderState(slot, 1);
            runtime.physicsTables()
                    .writeSlowTransitionCountdown(slot, EntityRomConstants.MAGIC_POWDER_SLOW_TRANSITION_COUNTDOWN);
            runtime.physicsTables().writeSlowTimerInitialized(slot, true);
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0);
            RoomEntity torch = withPositionAndVariant(entity, objectLeft, objectTop, 0);
            EntitySpriteDefinition torchDefinition = runtime.spriteHandlers() == null
                    ? entity.spriteDefinition()
                    : runtime.slotProperties().spriteViews().forMagicPowderTorchState(true);
            runtime.slots()[index] = withDefinition(torch, torchDefinition, 0);
            runtime.events()
                    .magicPowderObjectRequests
                    .add(new MagicPowderObjectRequest(
                            slot, location, objectLeft, objectTop, MagicPowderObjectAction.IGNITE_TORCH));
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x12));
            return false;
        }

        // label_018_7A4B reaches the common collision helper only during the
        // final sixteen transition ticks and only when private state 4 is 0.
        // $79DA returns for non-torch indoor objects. Outdoors, $79CF leaves
        // A=0 and jumps to the collision branch regardless of the object id.
        if (!runtime.indoorRoom() && owner.readMagicPowderPrivateState4(slot) == 0) {
            owner.collideMagicPowderWithEntities(entity, frame);
        }
        return true;
    }

    private boolean inUse;

    void run(
            linksawakening.world.entity.EntityProjectileController owner,
            int index,
            linksawakening.world.entity.RoomEntity entity,
            int frame,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {
        if (inUse) {
            new MagicPowderStep()
                    .run(owner, index, entity, frame, linkMotionState, handlerInteractive, timersInteractive);
            return;
        }
        inUse = true;
        runtime = null;
        slot = 0;
        transitionCountdown = 0;
        intersectionQuery = null;
        object = null;
        objectLeft = 0;
        objectTop = 0;
        location = 0;
        objectId = 0;
        this.owner = owner;
        this.index = index;
        this.entity = entity;
        this.frame = frame;
        this.linkMotionState = linkMotionState;
        this.handlerInteractive = handlerInteractive;
        this.timersInteractive = timersInteractive;
        try {
            run();
        } finally {
            inUse = false;
            this.owner = null;
            this.entity = null;
            this.runtime = null;
            this.intersectionQuery = null;
            this.object = null;
        }
    }
}
