package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** Focused behavior using the state owned by EntityLiftThrowController. */
final class ThrownEntityCollisions {
    static RoomEntity advanceThrownEntity(
            EntityLiftThrowController owner,
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (!owner.readThrownMotionInitialized(slot)) {
            owner.writeThrownDirection(slot, romLinkDirection);
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .start(slot, romLinkDirection, entity.type(), sideScrolling);
            owner.writeThrownMotionInitialized(slot, true);
        }
        ThrownEntityMotion.Update update = runtime.motions()
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        entity,
                        sideScrolling,
                        backgroundCollision,
                        frame,
                        runtime.physicsTables().readEntityGroundStatus(slot));
        RoomEntity updated = update.entity();
        if (entity.type() == EntityRomConstants.ENTITY_GENIE && (update.blockedX() || update.blockedY())) {
            updated = runtime.genie().registerGenieJarImpact(updated);
            if (updated.status() == EntityStatus.ACTIVE) {
                return updated;
            }
        }
        if (entity.type() == 0x9D && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            runtime.slots()[slot] = RoomEntity.disabled(slot);
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            owner.writeThrownMotionInitialized(slot, false);
            owner.smashStatue(updated);
            return runtime.slots()[slot];
        }
        if (entity.type() == EntityRomConstants.ENTITY_LIFTABLE_ROCK
                && (update.blockedX() || update.blockedY() || updated.z() == 0)) {
            owner.collideThrownEntityWithEntities(updated, frame);
            runtime.slots()[slot] = RoomEntity.disabled(slot);
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            owner.writeThrownMotionInitialized(slot, false);
            runtime.spawnLiftableRockSmash(updated.x(), updated.y(), entity.spriteVariant());
            return runtime.slots()[slot];
        }
        if (runtime.motions()
                                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                                .speedX(slot)
                        == 0
                && runtime.motions()
                                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                                .speedY(slot)
                        == 0) {
            runtime.motions()
                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                    .clear(slot);
            owner.writeThrownMotionInitialized(slot, false);
            runtime.combatTables().writeEnemyStunnedCountdown(slot, 0xFF);
            updated = withStatus(updated, EntityStatus.STUNNED);
        }
        return updated;
    }

    static boolean sideViewPotBackgroundCollision(
            EntityLiftThrowController owner,
            RoomEntity original,
            RoomEntity moved,
            RoomEntityBackgroundCollision fallback,
            int frame) {
        var runtime = owner.runtime;
        int slot = original.slot();
        boolean blocked = false;
        if (runtime.motions()
                        .get(SideViewPotMotion.class, SideViewPotMotion::new)
                        .speedX(slot)
                != 0) {
            int direction = (runtime.motions()
                                            .get(SideViewPotMotion.class, SideViewPotMotion::new)
                                            .speedX(slot)
                                    & 0x80)
                            != 0
                    ? EntityBackgroundCollisionResult.LEFT
                    : EntityBackgroundCollisionResult.RIGHT;
            if (runtime.physicsTables().readBackgroundInteraction() != null) {
                blocked |= runtime.physicsTables()
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                original.y(),
                                runtime.combatTables().readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), original.y());
            }
        }
        if (runtime.motions()
                        .get(SideViewPotMotion.class, SideViewPotMotion::new)
                        .speedY(slot)
                != 0) {
            int direction = (runtime.motions()
                                            .get(SideViewPotMotion.class, SideViewPotMotion::new)
                                            .speedY(slot)
                                    & 0x80)
                            != 0
                    ? EntityBackgroundCollisionResult.UP
                    : EntityBackgroundCollisionResult.DOWN;
            if (runtime.physicsTables().readBackgroundInteraction() != null) {
                blocked |= runtime.physicsTables()
                        .readBackgroundInteraction()
                        .probe(
                                original,
                                direction,
                                moved.x(),
                                moved.y(),
                                runtime.combatTables().readEnemyIgnoreHitsCountdown(slot),
                                frame)
                        .blocked();
            } else if (fallback != null) {
                blocked |= fallback.blocks(original, direction, moved.x(), moved.y());
            }
        }
        return blocked;
    }

    static void collideThrownEntityWithEntities(EntityLiftThrowController owner, RoomEntity thrown, int frame) {
        var runtime = owner.runtime;
        int sourceSlot = thrown.slot();
        int sourceVisualY = (thrown.y() - thrown.z()) & 0xFF;
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
                    || target.spriteVariant() < 0
                    || unsignedByteAbs(thrown.x() - target.x()) >= 0x0C
                    || unsignedByteAbs(sourceVisualY - ((target.y() - target.z()) & 0xFF)) >= 0x0C
                    || (runtime.combatTables().readEnemyHitboxFlags(targetSlot)
                                    & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || runtime.combatTables().readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            runtime.projectiles()
                    .applyPlayerProjectileDamage(
                            target,
                            EntityRomConstants.DAMAGE_TYPE_THROW_AT,
                            runtime.motions()
                                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                                    .speedX(sourceSlot),
                            runtime.motions()
                                    .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                                    .speedY(sourceSlot));
        }
    }
}
