package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BoomerangMotion;

/** Reused, reentrancy-safe scratch for BoomerangBehavior.advanceBoomerangEntity; values are assigned in source order. */
final class BoomerangStep {
    private linksawakening.world.entity.EntityProjectileController owner;
    private linksawakening.world.entity.BoomerangFrame inputs;
    private linksawakening.world.entity.EntityProjectileControllerServices runtime;
    private int index;
    private linksawakening.world.entity.RoomEntity entity;
    private int frame;
    private int linkEntityX;
    private int linkEntityY;
    private int linkEntityZ;
    private int linkMotionState;
    private boolean handlerInteractive;
    private boolean timersInteractive;
    private int slot;
    private int variant;
    private linksawakening.world.entity.RoomEntity moved;
    private linksawakening.world.entity.RoomEntityObjectQuery intersectionQuery;
    private linksawakening.world.entity.RoomEntityObjectSample object;
    private boolean objectCollision;
    private boolean bushCollision;
    private linksawakening.world.entity.RoomEntity positioned;
    private boolean stateCollision;

    BoomerangStep() {}

    void run() {
        if (!prepareMotion()) return;
        if (!applyBushCollision()) return;
        if (!advanceOutboundOrReturn()) return;
    }

    private boolean prepareMotion() {

        runtime = owner.runtime;
        index = inputs.index();
        entity = inputs.entity();
        frame = inputs.frame();
        linkEntityX = inputs.linkEntityX();
        linkEntityY = inputs.linkEntityY();
        linkEntityZ = inputs.linkEntityZ();
        linkMotionState = inputs.linkMotionState();
        handlerInteractive = inputs.handlerInteractive();
        timersInteractive = inputs.timersInteractive();
        slot = entity.slot();
        if (timersInteractive) {
            runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).decrementTransitionCountdown(slot);
        }
        variant = runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .advanceSpriteVariant(slot, frame, handlerInteractive);
        if (!handlerInteractive) {
            runtime.slots()[index] = withVariant(entity, variant);
            return false;
        }
        owner.playBoomerangSoundIfDue(entity);

        // label_3B7B runs before the position update and uses the launch
        // vector for recoil. This pass also clears the outbound countdown on
        // a successful enemy/object collision, as the ROM does.
        owner.collideBoomerangWithEntities(entity, frame);
        moved = runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .advancePosition(entity);
        intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        objectCollision = object != null
                && owner.projectileObjectCollision(moved, object, frame, slot) == ProjectileObjectCollision.BLOCKED;
        bushCollision = object != null
                && !runtime.indoorRoom()
                && (object.objectId() == EntityRomConstants.OBJECT_BUSH
                        || object.objectId() == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS);
        positioned = objectCollision
                        && runtime.motions()
                                        .get(BoomerangMotion.class, BoomerangMotion::new)
                                        .transitionCountdown(slot)
                                != 0
                ? entity
                : moved;
        return true;
    }

    private boolean applyBushCollision() {

        if (bushCollision) {
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            runtime.events()
                    .boomerangObjectRequests
                    .add(new BoomerangObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
        }

        // BoomerangDestroyBushIfNeeded clears the source collision byte before
        // the state handler, so a bush does not also produce the wall-poke
        // branch in the same frame.
        stateCollision = objectCollision && !bushCollision;
        return true;
    }

    private boolean advanceOutboundOrReturn() {

        if (runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).state(slot) == 0) {
            if (runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .transitionCountdown(slot)
                    == 0) {
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setVectorTowardsLink(slot, positioned, linkEntityX, linkEntityY, 0x08);
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setState(slot, 1);
            } else if (stateCollision) {
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setTransitionCountdown(slot, 0);
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.SWORD_POKE,
                                positioned.x(),
                                (positioned.y() - positioned.z() + 0x03) & 0xFF));
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot,
                                EntityRomConstants.ENTITY_BOOMERANG,
                                0,
                                false,
                                EntityCombatEvent.SoundChannel.JINGLE,
                                0x07));
            }
        } else {
            if ((frame & 0x03) == 0) {
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setVectorTowardsLink(slot, positioned, linkEntityX, linkEntityY, 0x20);
            }
            if (((frame ^ slot) & 0x01) != 0
                    && linkEntityZ == 0
                    && linkMotionState < 0x02
                    // label_3B44 reads hActiveEntityPosX/VisualPosY, cached before
                    // movement. The vector above instead reads the updated WRAM.
                    && BoomerangMotion.overlapsLink(entity, linkEntityX, linkEntityY)) {
                runtime.clearEntity(slot);
                return false;
            }
        }
        runtime.slots()[index] = withPositionAndVariant(positioned, positioned.x(), positioned.y(), variant);
        return true;
    }

    private boolean inUse;

    void run(
            linksawakening.world.entity.EntityProjectileController owner,
            linksawakening.world.entity.BoomerangFrame inputs) {
        if (inUse) {
            new BoomerangStep().run(owner, inputs);
            return;
        }
        inUse = true;
        runtime = null;
        index = 0;
        entity = null;
        frame = 0;
        linkEntityX = 0;
        linkEntityY = 0;
        linkEntityZ = 0;
        linkMotionState = 0;
        handlerInteractive = false;
        timersInteractive = false;
        slot = 0;
        variant = 0;
        moved = null;
        intersectionQuery = null;
        object = null;
        objectCollision = false;
        bushCollision = false;
        positioned = null;
        stateCollision = false;
        this.owner = owner;
        this.inputs = inputs;
        try {
            run();
        } finally {
            inUse = false;
            this.owner = null;
            this.inputs = null;
            this.runtime = null;
            this.entity = null;
            this.moved = null;
            this.intersectionQuery = null;
            this.object = null;
            this.positioned = null;
        }
    }
}
