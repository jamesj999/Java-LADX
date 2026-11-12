package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileEntityCollision;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.SwordBeamMotion;

/** Focused behavior using the state owned by EntityProjectileController. */
final class SwordBeamBehavior {
    static void advanceSwordBeamEntity(
            EntityProjectileController owner,
            int index,
            RoomEntity entity,
            int frame,
            int linkEntityX,
            int linkEntityY,
            int romLinkDirection,
            boolean handlerInteractive) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (runtime.motions().get(SwordBeamMotion.class, SwordBeamMotion::new).state(slot) == 0) {
            int x = (linkEntityX + SwordBeamMotion.firstHandlerOffsetX(romLinkDirection)) & 0xFF;
            int y = (linkEntityY + SwordBeamMotion.firstHandlerOffsetY(romLinkDirection)) & 0xFF;
            runtime.motions()
                    .get(SwordBeamMotion.class, SwordBeamMotion::new)
                    .initializeFirstHandler(slot, romLinkDirection);
            runtime.combatTables().writeEnemyFlashCountdown(slot, 0xFF);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_SWORD_BEAM,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            EntityRomConstants.SWORD_BEAM_JINGLE_ID));
            // The state-0 handler only applies the Link-relative launch
            // offset and enters state 1; it does not render the beam yet.
            runtime.slots()[index] = withPositionAndVariant(entity, x, y, -1);
            return;
        }

        int variant = runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .direction(slot);
        if (!handlerInteractive) {
            // ReturnIfNonInteractive_19 renders state 1, then returns before
            // damage, movement, object intersection, or VFX creation.
            runtime.slots()[index] = withPositionAndVariant(entity, entity.x(), entity.y(), variant);
            return;
        }

        ProjectileEntityCollision entityCollision = owner.collideSwordBeamWithEntities(entity, frame);
        RoomEntity moved = runtime.motions()
                .get(SwordBeamMotion.class, SwordBeamMotion::new)
                .advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        ProjectileObjectCollision objectCollision = object == null
                ? ProjectileObjectCollision.NONE
                : owner.projectileObjectCollision(moved, object, frame, slot);
        if (entityCollision.unload()
                || entityCollision.blocked()
                || objectCollision != ProjectileObjectCollision.NONE) {
            runtime.clearEntity(slot);
            // $45A0 clears the slot and falls through to the shared poke VFX.
            // Physics $FF unloads inside bank $03, bypassing this branch.
            if (objectCollision != ProjectileObjectCollision.UNLOAD
                    && (entityCollision.blocked() || objectCollision == ProjectileObjectCollision.BLOCKED)) {
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.SWORD_POKE, entity.x(), (entity.y() - entity.z() + 3) & 0xFF));
            }
            return;
        }

        if (((frame + 1) & 0x03) == 0) {
            runtime.events()
                    .transientVfxRequests
                    .add(new TransientVfxRequest(
                            TransientVfxType.SWORD_BEAM, entity.x(), (entity.y() - entity.z()) & 0xFF, variant));
        }
        runtime.slots()[index] = withPositionAndVariant(moved, moved.x(), moved.y(), variant);
    }

    static ProjectileEntityCollision collideSwordBeamWithEntities(
            EntityProjectileController owner, RoomEntity beam, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = beam.slot();
        int sourceVisualY = (beam.y() - beam.z()) & 0xFF;
        boolean unload = false;
        boolean blocked = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((beam.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            // $7656 forces a collision before reading the ignore-hits timer.
            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                blocked = true;
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            if (owner.ironMaskBlocksProjectile(
                    target,
                    runtime.motions()
                            .get(SwordBeamMotion.class, SwordBeamMotion::new)
                            .direction(sourceSlot))) {
                blocked = true;
                continue;
            }
            unload |= !blocked;
            owner.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_SWORD_BEAM,
                    runtime.motions()
                            .get(SwordBeamMotion.class, SwordBeamMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions()
                            .get(SwordBeamMotion.class, SwordBeamMotion::new)
                            .speedY(sourceSlot));
        }
        return new ProjectileEntityCollision(unload, blocked);
    }
}
