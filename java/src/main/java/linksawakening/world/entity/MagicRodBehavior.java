package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityProjectileController. */
final class MagicRodBehavior {
    static void advanceMagicRodFireballEntity(
            EntityProjectileController owner,
            int index,
            RoomEntity entity,
            int frame,
            boolean handlerInteractive,
            boolean timersInteractive) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        // The source calls func_003_75A2 before either the wall-fire branch or
        // ReturnIfNonInteractive. A fireball can therefore still deliver the
        // common target collision while Link is being transitioned.
        boolean unloadOnHit = owner.collideMagicRodFireballWithEntities(entity, frame);
        if (unloadOnHit) {
            // $7782-$7795 unloads a normal projectile hit. Blocking targets
            // and existing wall flames retain a nonzero collision byte.
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        int variant = runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .advanceSpriteVariant(slot, frame);
        if (runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .privateCountdown1(slot)
                != 0) {
            if (runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .tickFireTransition(slot, timersInteractive)) {
                runtime.clearEntity(slot);
                return;
            }
            EntitySpriteDefinition fireDefinition = runtime.spriteHandlers() == null
                    ? entity.spriteDefinition()
                    : runtime.spriteHandlers().forMagicRodFireState();
            runtime.slots()[index] = withDefinition(entity, fireDefinition, variant);
            return;
        }

        // ReturnIfNonInteractive_03 is after the normal sprite write and
        // before movement/object lookup in the ROM helper.
        if (!handlerInteractive) {
            runtime.slots()[index] = withVariant(entity, variant);
            return;
        }

        RoomEntity moved = runtime.motions()
                .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                .advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = runtime.physicsTables().readObjectIntersectionQuery() != null
                ? runtime.physicsTables().readObjectIntersectionQuery()
                : runtime.physicsTables().readObjectQuery();
        RoomEntityObjectSample object = intersectionQuery == null ? null : intersectionQuery.sample(moved);
        ProjectileObjectCollision objectCollision = object == null
                ? ProjectileObjectCollision.NONE
                : owner.projectileObjectCollision(moved, object, frame, slot);
        if (objectCollision == ProjectileObjectCollision.UNLOAD) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        // $7DF3-$7E00 restores BOTH coordinates from the active pre-movement
        // position before the fireball handler can clear a burnable collision.
        RoomEntity positioned = objectCollision == ProjectileObjectCollision.BLOCKED ? entity : moved;
        if (owner.isMagicRodBurnableObject(object)) {
            runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .setCollision(slot, false);
            int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
            runtime.events()
                    .magicRodObjectRequests
                    .add(new MagicRodObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
            runtime.slots()[index] = withVariant(positioned, variant);
            return;
        }

        if (runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .collided(slot)
                || objectCollision == ProjectileObjectCollision.BLOCKED) {
            // ArrowRenderAndMove writes privateCountdown1 after the normal
            // fireball display has already been rendered. Keep the normal
            // definition for this frame; the next handler tick switches to
            // FireSpriteVariants and decrements the $30 countdown.
            runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .beginFireTransition(slot);
        }
        runtime.slots()[index] = withVariant(positioned, variant);
    }

    static boolean collideMagicRodFireballWithEntities(
            EntityProjectileController owner, RoomEntity fireball, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = fireball.slot();
        int sourceVisualY = (fireball.y() - fireball.z()) & 0xFF;
        boolean unloadOnHit = false;
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
                    || ((fireball.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .setCollision(sourceSlot, true);
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            if (target.type() == EntityRomConstants.ENTITY_IRON_MASK
                    && runtime.motions()
                                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                    .maskRemovalState(targetSlot)
                            == 0
                    && (runtime.motions()
                                            .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                            .direction(targetSlot)
                                    ^ 0x01)
                            == runtime.motions()
                                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                                    .direction(sourceSlot)) {
                runtime.motions()
                        .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                        .setCollision(sourceSlot, true);
                continue;
            }

            unloadOnHit |= !runtime.motions()
                    .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                    .collided(sourceSlot);
            owner.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_MAGIC_ROD,
                    runtime.motions()
                            .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                            .speedX(sourceSlot),
                    runtime.motions()
                            .get(MagicRodFireballMotion.class, MagicRodFireballMotion::new)
                            .speedY(sourceSlot));
        }
        return unloadOnHit;
    }
}
