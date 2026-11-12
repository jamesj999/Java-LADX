package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BoomerangMotion;

/** Focused behavior using the state owned by EntityProjectileController. */
final class PlayerProjectileDamage {
    static ProjectileObjectCollision projectileObjectCollision(
            EntityProjectileController owner, RoomEntity entity, RoomEntityObjectSample object, int frame, int slot) {
        var runtime = owner.runtime;
        int physics = object.physicsFlag() & 0xFF;
        if (physics == 0xFF) {
            // $7E05 redirects boomerangs to the ordinary collision branch;
            // other projectiles unload immediately.
            return entity.type() == EntityRomConstants.ENTITY_BOOMERANG
                    ? ProjectileObjectCollision.BLOCKED
                    : ProjectileObjectCollision.UNLOAD;
        }
        if (physics >= 0xD0 && physics <= 0xD3) {
            if (runtime.liftThrow().readThrownDirection(slot) == physics - 0xD0) {
                if (entity.z() != 0) {
                    runtime.physicsTables()
                            .writeEntityUnknownJ(slot, (runtime.physicsTables().readEntityUnknownJ(slot) + 1) & 0xFF);
                    return ProjectileObjectCollision.NONE;
                }
                return ProjectileObjectCollision.BLOCKED;
            }
            if (runtime.physicsTables().readEntityUnknownJ(slot) == 0) {
                return ProjectileObjectCollision.BLOCKED;
            }
            if ((frame & 0x03) == 0 || (!runtime.indoorRoom() && (frame & 0x01) == 0)) {
                return ProjectileObjectCollision.NONE;
            }
            runtime.physicsTables()
                    .writeEntityUnknownJ(slot, (runtime.physicsTables().readEntityUnknownJ(slot) - 1) & 0xFF);
            return ProjectileObjectCollision.NONE;
        }
        return BoomerangMotion.objectPhysicsCollides(physics)
                ? ProjectileObjectCollision.BLOCKED
                : ProjectileObjectCollision.NONE;
    }

    static boolean collidePlayerArrowWithEntities(EntityProjectileController owner, RoomEntity arrow, int frame) {
        var runtime = owner.runtime;
        boolean unload = false;
        int arrowSlot = arrow.slot();
        int attackType = runtime.bombInteractions().readPlayerArrowBombArrow(arrowSlot)
                ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                : EntityRomConstants.DAMAGE_TYPE_ARROW;
        int arrowVisualY = (arrow.y() - arrow.z()) & 0xFF;
        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == arrowSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = runtime.slots()[targetSlot];
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE)
                            != 0
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || target.spriteVariant() < 0) {
                continue;
            }

            if (((arrow.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - arrowVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if ((runtime.combatTables().readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                    != 0) {
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .markCollision(arrowSlot);
                continue;
            }
            if (runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            if (owner.ironMaskBlocksProjectile(
                    target,
                    runtime.motions()
                            .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                            .direction(arrowSlot))) {
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .markCollision(arrowSlot);
                continue;
            }
            unload |= !runtime.motions()
                    .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                    .collided(arrowSlot);
            if (attackType == EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW) {
                // BombArrowHandler selects DAMAGE_TYPE_BOMB_ARROW. The
                // shared damage helper deliberately treats this active-arrow
                // state as a harmless target transition rather than calling
                // ApplySwordDamagesToEnemy.
                runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x03);
            } else {
                owner.applyPlayerArrowDamage(arrow, target);
            }
        }
        return unload;
    }

    static boolean collideBouncingBombiteWithEntities(EntityProjectileController owner, RoomEntity source, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = source.slot();
        int sourceVisualY = (source.y() - source.z()) & 0xFF;
        int sourceSpeedX =
                runtime.motions().get(BombiteMotion.class, BombiteMotion::new).speedX(sourceSlot);
        int sourceSpeedY =
                runtime.motions().get(BombiteMotion.class, BombiteMotion::new).speedY(sourceSlot);
        boolean collided = false;
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
                    || ((source.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18
                    || target.spriteVariant() == -1) {
                continue;
            }

            collided = true;
            // GetEntityTransitionCountdown uses BC, which remains the active
            // source slot throughout func_003_75A2.
            runtime.physicsTables().writeEnemyTransitionCountdown(sourceSlot, 0);
            if (target.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE) {
                runtime.motions()
                        .get(BombiteMotion.class, BombiteMotion::new)
                        .enterBouncingLitFromEntityCollision(targetSlot, sourceSpeedX, sourceSpeedY);
                runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x40);
                runtime.bombInteractions().writeBombPrivateCountdown1(targetSlot, 0x08);
            }
        }
        return collided;
    }

    static void applyPlayerProjectileDamage(
            EntityProjectileController owner, RoomEntity target, int damageType, int speedX, int speedY) {
        owner.playerProjectileDamageFrameStep.run(owner, target, damageType, speedX, speedY);
    }
}
