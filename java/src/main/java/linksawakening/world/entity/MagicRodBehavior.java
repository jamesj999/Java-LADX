package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

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
        owner.magicRodFrameStep.run(owner, index, entity, frame, handlerInteractive, timersInteractive);
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
