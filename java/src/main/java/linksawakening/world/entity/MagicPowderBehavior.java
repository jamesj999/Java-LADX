package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityProjectileController. */
final class MagicPowderBehavior {
    static void advanceMagicPowderSprinkleEntity(
            EntityProjectileController owner,
            int index,
            RoomEntity entity,
            int frame,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {
        new MagicPowderStep(owner, index, entity, frame, linkMotionState, handlerInteractive, timersInteractive).run();
    }

    static boolean collideMagicPowderWithEntities(EntityProjectileController owner, RoomEntity sprinkle, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = sprinkle.slot();
        int sourceVisualY = (sprinkle.y() - sprinkle.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            int targetPhysics = runtime.physicsTables().readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((sprinkle.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }
            if (!runtime.indoorRoom()
                    && (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) == 0
                    && target.type() == EntityRomConstants.ENTITY_TARIN
                    && runtime.motions()
                                    .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                                    .state(targetSlot)
                            == 0) {
                runtime.motions()
                        .get(TarinRaccoonMotion.class, TarinRaccoonMotion::new)
                        .startPowderTransformation(targetSlot);
                runtime.physicsTables().writeSlowTransitionCountdown(targetSlot, 0x7F);
                runtime.physicsTables().writeSlowTimerInitialized(targetSlot, true);
                runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x10);
                collided = true;
                continue;
            }
            if ((targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (runtime.combatTables().readEnemyHitboxFlags(targetSlot)
                                    & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            collided = true;
            owner.applyPlayerProjectileDamage(target, EntityRomConstants.DAMAGE_TYPE_MAGIC_POWDER, 0, 0);
        }
        return collided;
    }
}
