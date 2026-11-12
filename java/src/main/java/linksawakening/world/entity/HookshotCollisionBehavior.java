package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** Focused behavior using the state owned by EntityProjectileController. */
final class HookshotCollisionBehavior {
    static boolean collideHookshotWithEntities(
            EntityProjectileController owner, RoomEntity hookshot, HookshotChainMotion.State hookshotState, int frame) {
        var runtime = owner.runtime;
        boolean unmasked = false;
        int sourceSlot = hookshot.slot();
        // $7C39 moves WRAM, but $7C59's shared hit test still reads the
        // hActiveEntity coordinates cached at the beginning of this pass.
        int sourceVisualY = (hookshot.y() - hookshot.z()) & 0xFF;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || ((hookshot.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18
                    || target.spriteVariant() == -1
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            if (target.type() != EntityRomConstants.ENTITY_IRON_MASK
                    || ((runtime.motions()
                                            .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                            .direction(targetSlot)
                                    ^ 0x01)
                            != hookshotState.direction())
                    || runtime.motions()
                                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                    .maskRemovalState(targetSlot)
                            != 0) {
                continue;
            }

            runtime.motions()
                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                    .setMaskRemovalState(targetSlot, 1);
            runtime.motions()
                    .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                    .initializeFromMasked(
                            targetSlot,
                            runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .direction(targetSlot),
                            runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .speedX(targetSlot),
                            runtime.motions()
                                    .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                    .speedY(targetSlot));
            runtime.entityPlayerSpawns()
                    .spawnIronMasksMask(
                            hookshotState.x(), hookshotState.y(), hookshot.spriteVariant() & 0x01, sourceSlot);
            unmasked = true;
        }
        return unmasked;
    }
}
