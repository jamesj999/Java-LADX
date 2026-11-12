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

/** Focused behavior using the state owned by EntityProjectileController. */
final class BoomerangBehavior {
    static void advanceBoomerangEntity(EntityProjectileController owner, BoomerangFrame inputs) {
        var runtime = owner.runtime;
        int index = inputs.index();
        RoomEntity entity = inputs.entity();
        int frame = inputs.frame();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int linkEntityZ = inputs.linkEntityZ();
        int linkMotionState = inputs.linkMotionState();
        boolean handlerInteractive = inputs.handlerInteractive();
        boolean timersInteractive = inputs.timersInteractive();
        int slot = entity.slot();
        if (timersInteractive) {
            runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).decrementTransitionCountdown(slot);
        }
        int variant = runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .advanceSpriteVariant(slot, frame, handlerInteractive);
        if (!handlerInteractive) {
            runtime.slots()[index] = withVariant(entity, variant);
            return;
        }
        owner.playBoomerangSoundIfDue(entity);

        // label_3B7B runs before the position update and uses the launch
        // vector for recoil. This pass also clears the outbound countdown on
        // a successful enemy/object collision, as the ROM does.
        owner.collideBoomerangWithEntities(entity, frame);
        RoomEntity moved = runtime.motions()
                .get(BoomerangMotion.class, BoomerangMotion::new)
                .advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        boolean objectCollision = object != null
                && owner.projectileObjectCollision(moved, object, frame, slot) == ProjectileObjectCollision.BLOCKED;
        boolean bushCollision = object != null
                && !runtime.indoorRoom()
                && (object.objectId() == EntityRomConstants.OBJECT_BUSH
                        || object.objectId() == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS);
        RoomEntity positioned = objectCollision
                        && runtime.motions()
                                        .get(BoomerangMotion.class, BoomerangMotion::new)
                                        .transitionCountdown(slot)
                                != 0
                ? entity
                : moved;
        if (bushCollision) {
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            runtime.events()
                    .boomerangObjectRequests
                    .add(new BoomerangObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
        }

        // BoomerangDestroyBushIfNeeded clears the source collision byte before
        // the state handler, so a bush does not also produce the wall-poke
        // branch in the same frame.
        boolean stateCollision = objectCollision && !bushCollision;
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
                return;
            }
        }
        runtime.slots()[index] = withPositionAndVariant(positioned, positioned.x(), positioned.y(), variant);
    }

    static boolean collideBoomerangWithEntities(EntityProjectileController owner, RoomEntity boomerang, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = boomerang.slot();
        int sourceVisualY = (boomerang.y() - boomerang.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            boolean grabbable = (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0;
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (!grabbable && !RoomEntityCombatRules.supportsEnemyCollision(target.type()))
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((boomerang.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if (grabbable) {
                collided = true;
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setTransitionCountdown(sourceSlot, 0);
                // The source writes sourceSlot + 1 to the target's private
                // state five. The current runtime has no consumer for that
                // field, but the source-side transition is still observable.
                continue;
            }
            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                collided = true;
                runtime.motions()
                        .get(BoomerangMotion.class, BoomerangMotion::new)
                        .setTransitionCountdown(sourceSlot, 0);
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            collided = true;
            runtime.motions().get(BoomerangMotion.class, BoomerangMotion::new).setTransitionCountdown(sourceSlot, 0);
            if (owner.ironMaskBlocksProjectile(
                    target,
                    runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .direction(sourceSlot))) {
                continue;
            }
            owner.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_BOOMERANG,
                    runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions()
                            .get(BoomerangMotion.class, BoomerangMotion::new)
                            .speedY(sourceSlot));
        }
        return collided;
    }
}
