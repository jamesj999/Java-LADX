package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BoomerangMotion;

/** Focused behavior using the state owned by EntityProjectileController. */
final class BoomerangBehavior {
    static void advanceBoomerangEntity(EntityProjectileController owner, BoomerangFrame inputs) {
        new BoomerangStep(owner, inputs).run();
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
