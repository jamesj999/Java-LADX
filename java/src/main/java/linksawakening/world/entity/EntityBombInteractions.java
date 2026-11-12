package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

/** ROM bomb explosion, damage and delayed presentation ordering. */
public final class EntityBombInteractions {
    private final RoomEntityRuntime runtime;

    EntityBombInteractions(RoomEntityRuntime runtime) {
        this.runtime = runtime;
    }

    void applyInitialBombArrowCandidateState(int slot, int romDirection) {
        int x = runtime.slots[slot].x() + bombArrowXOffset(romDirection);
        int y = runtime.slots[slot].y() + bombArrowYOffset(romDirection);
        runtime.slots[slot] = withPositionAndVariant(runtime.slots[slot], x, y, runtime.slots[slot].spriteVariant());
        runtime.entityState.bombPrivateCountdown3[slot] = 0x03;
        runtime.entityState.lastBombPlacementPlayedBump = !runtime.entityState.groundInteractionSideScrolling;
        if (runtime.entityState.groundInteractionSideScrolling) {
            runtime.slots[slot] = withZ(runtime.slots[slot], 0);
        }
        // ConvertToBombArrowIfNeeded zeros speeds, but BombEntityHandler
        // must still run gravity and floor contact on subsequent frames.
        runtime.entityState.thrownEntityMotion.startEnemyBomb(slot, 0, 0, 0);
        // Keep the placed-bomb bridge from restoring directional launch speed.
        runtime.entityState.placedBombMotionInitialized[slot] = true;
    }

    void markArrowAsBombArrow(int slot) {
        runtime.entityState.playerArrowBombArrow[slot] = true;
        RoomEntity arrow = runtime.slots[slot];
        if (runtime.spriteHandlers != null && arrow.loaded() && arrow.type() == EntityRomConstants.ENTITY_ARROW) {
            runtime.slots[slot] = withDefinition(
                    arrow,
                    runtime.spriteHandlers.forBombArrow(),
                    runtime.entityState.playerArrowMotion.direction(slot));
        }
    }

    void finalizePendingBombPresentations() {
        for (int slot = 0; slot < runtime.slots.length; slot++) {
            if (runtime.bombFinalPresentationPending[slot]) {
                runtime.clearEntity(slot);
            }
        }
    }

    public BombMotion.Decision advanceBombEntity(
            int index,
            RoomEntity entity,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        int slot = entity.slot();
        BombMotion.Decision decision = BombMotion.decide(runtime.entityState.enemyTransitionCountdown[slot] & 0xFF);
        if (decision.playExplosionSound()) {
            runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, EntityRomConstants.ENTITY_BOMB, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
        }
        if (decision.countdownOverride().isPresent()) {
            runtime.entityState.enemyTransitionCountdown[slot] =
                    decision.countdownOverride().getAsInt();
        }
        if (decision.phase() == BombMotion.Phase.EXPLOSION) {
            runtime.entityState.enemyPhysicsFlags[slot] = (runtime.entityState.enemyPhysicsFlags[slot] & 0xF0)
                    | EntityRomConstants.BOMB_EXPLOSION_PHYSICS_LOW_BITS;
        }

        EntitySpriteDefinition definition =
                switch (decision.phase()) {
                    case NORMAL -> runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
                    case WARNING -> runtime.spriteHandlers == null
                            ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BOMB)
                            : runtime.spriteHandlers.forBombRightBeforeExploding();
                    case EXPLOSION -> runtime.spriteHandlers == null
                            ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BOMB)
                            : runtime.spriteHandlers.forBombExplosion();
                };
        int variant = decision.explosionVariant().orElse(0);
        RoomEntity updated = withDefinition(entity, definition, variant);
        runtime.slots[index] = updated;
        queueBombExplosionInteractions(
                updated, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
        if (decision.unloadAfterPresentation()) {
            runtime.bombFinalPresentationPending[slot] = true;
        }
        return decision;
    }

    void queueBombExplosionInteractions(
            RoomEntity bomb,
            BombMotion.Decision decision,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        if (decision.phase() != BombMotion.Phase.EXPLOSION) {
            return;
        }
        int bombSlot = bomb.slot();
        int countdown = runtime.entityState.enemyTransitionCountdown[bombSlot] & 0xFF;
        // BombExplosionHandler returns before every interaction for Tarin's
        // transformation explosion. All other bomb variants, including
        // enemy bombs, still run the shared destroyable-object checks.
        if (runtime.entityState.bombPrivateState4[bombSlot] == 0x4C) {
            return;
        }
        if (countdown < 0x0E || countdown > 0x16) {
            return;
        }

        int bombX = bomb.x() & 0xFF;
        int bombVisualY = (bomb.y() - bomb.z()) & 0xFF;
        runtime.entityState.events.pendingBombExplosionEvents.add(new BombExplosionEvent(
                bombSlot,
                BombExplosionEvent.OBJECT_TARGET,
                bombX,
                bombVisualY,
                countdown,
                BombExplosionEvent.DAMAGE_TYPE_BOMB));
        if (countdown != 0x12) {
            return;
        }
        if (runtime.entityState.bombPrivateState4[bombSlot] != 0) {
            queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
            return;
        }
        runtime.entityState.swordMoblinAlertingSoundCounter = 0x04;

        for (int targetSlot = runtime.slots.length - 1; targetSlot >= 0; targetSlot--) {
            RoomEntity target = runtime.slots[targetSlot];
            if (target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.entityState.enemyPhysicsFlags[targetSlot]
                                    & (EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                                            | EntityRomConstants.ENTITY_PHYSICS_GRABBABLE))
                            != 0
                    || (runtime.entityState.enemyHitboxFlags[targetSlot] & EntityRomConstants.HITFLAGS_IGNORE_HITS) != 0
                    || !isWithinBombExplosionWindow(bombX, target.x())
                    || !isWithinBombExplosionWindow((target.y() - target.z()) & 0xFF, bombVisualY)) {
                continue;
            }
            runtime.entityState.events.pendingBombExplosionEvents.add(new BombExplosionEvent(
                    bombSlot, targetSlot, bombX, bombVisualY, countdown, BombExplosionEvent.DAMAGE_TYPE_BOMB));
            applyBombDamage(bomb, target);
        }
    }

    void queueEnemyBombLinkCollision(
            RoomEntity bomb,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        runtime.entityState.swordMoblinAlertingSoundCounter = 0x04;
        int bombX = bomb.x() & 0xFF;
        int bombY = bomb.y() & 0xFF;
        if (!isWithinBombExplosionWindow(bombX, linkEntityX) || !isWithinBombExplosionWindow(bombY, linkEntityY)) {
            return;
        }

        boolean collisionProtected = projectileLinkState.invincibilityCounter() != 0;
        int linkDamage = runtime.enemyCombatTables == null
                ? 0x08
                : runtime.enemyCombatTables.contactDamageForGroup(runtime.entityState.enemyHealthGroup[bomb.slot()]);
        EnemyRecoilMotion.Vector linkResponse = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
                bomb.x(), bomb.y(), bomb.z(), linkEntityX, linkEntityY, 0x14);
        projectileEvents.add(new EntityProjectileEvent(
                bomb.slot(),
                EntityRomConstants.ENTITY_BOMB,
                EntityProjectileEvent.Kind.LINK_DAMAGE,
                0,
                collisionProtected ? 0 : linkDamage,
                collisionProtected ? EntityProjectileEvent.SoundChannel.NONE : EntityProjectileEvent.SoundChannel.WAVE,
                collisionProtected ? -1 : 0x03,
                false,
                false,
                0,
                0,
                collisionProtected
                        ? (runtime.entityState.linkState.currentLinkSpeedX << 1) & 0xFF
                        : (linkResponse.x() << 1) & 0xFF,
                collisionProtected
                        ? (runtime.entityState.linkState.currentLinkSpeedY << 1) & 0xFF
                        : (linkResponse.y() << 1) & 0xFF,
                collisionProtected ? 0 : 0x10));
    }

    void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        int targetSlot = target.slot();
        RomEnemyCombatTables.SwordDamageResult damageResult = runtime.enemyCombatTables == null
                ? null
                : runtime.enemyCombatTables.resolveAttackDamage(
                        target.type(),
                        runtime.entityState.enemyHealthGroup[target.slot()],
                        EntityRomConstants.DAMAGE_TYPE_BOMB);
        int rawDamage =
                damageResult == null ? RoomEntityCombatRules.basicSwordDamage(target.type()) : damageResult.rawValue();

        if (rawDamage != 0) {
            int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            EntityCombatEvent.SoundChannel secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;

            if (rawDamage == 0xFE) {
                runtime.entityState.enemyTransitionCountdown[targetSlot] = 0x60;
                runtime.entityState.enemyStunnedCountdown[targetSlot] = 0;
                runtime.entityState.enemyFlashCountdown[targetSlot] = 0;
                runtime.entityState.enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                runtime.entityState.enemyPhysicsFlags[targetSlot] =
                        (runtime.entityState.enemyPhysicsFlags[targetSlot] + 2) & 0xFF;
                runtime.entityState.entityOptions1Override[targetSlot] = runtime.options1(targetSlot) & 0xC2;
                runtime.slots[targetSlot] = withStatus(target, EntityStatus.BURNING);
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                runtime.entityState.enemyTransitionCountdown[targetSlot] = 0;
                runtime.entityState.enemyStunnedCountdown[targetSlot] = 0xFF;
                runtime.entityState.enemyFlashCountdown[targetSlot] = 0;
                runtime.entityState.enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                runtime.slots[targetSlot] = withStatus(target, EntityStatus.STUNNED);
            } else if (rawDamage == 0xFD) {
                // The ROM's FD action is dispatched by the target's own
                // entity-specific branch. Preserve it as a special result;
                // the bomb vector below still applies unconditionally.
            } else if (rawDamage < 0xF0) {
                runtime.entityState.enemyHealth[targetSlot] =
                        Math.max(0, runtime.entityState.enemyHealth[targetSlot] - enemyDamage);
                if (runtime.entityState.enemyHealth[targetSlot] == 0) {
                    // ApplySwordDamagesToEnemy uses $2F for the ordinary
                    // dying countdown; $40 is the Piece-of-Power override.
                    runtime.entityState.dyingCountdown[targetSlot] = 0x2F;
                    runtime.entityState.powerRecoilDeath[targetSlot] = false;
                    runtime.slots[targetSlot] =
                            withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false);
                } else {
                    runtime.entityState.enemyFlashCountdown[targetSlot] = 0x18;
                    runtime.entityState.enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                }
            }

            runtime.entityState.events.pendingEntityEvents.add(new EntityCombatEvent(
                    targetSlot,
                    target.type(),
                    0,
                    false,
                    enemyDamage,
                    specialAction,
                    EntityCombatEvent.SoundChannel.JINGLE,
                    0x03,
                    secondaryChannel,
                    secondarySoundId,
                    null));
        }

        runtime.entityState.enemyRecoilMotion.configureFromSource(
                targetSlot, bomb.x(), bomb.y(), bomb.z(), target.x(), target.y(), 0x30);
    }

    public RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = stunned.slot();
        RoomEntity moved = stunned;
        if (runtime.entityState.enemyRecoilMotion.isActive(slot)) {
            moved = runtime.backgroundMechanics
                    .applyEnemyRecoilIfNeeded(moved, backgroundCollision)
                    .entity();
        }
        moved = runtime.entityState
                .thrownEntityMotion
                .advance(
                        moved,
                        runtime.entityState.groundInteractionSideScrolling,
                        backgroundCollision,
                        frame,
                        runtime.entityState.entityGroundStatus[slot])
                .entity();
        runtime.entityState.thrownEntityMotion.clear(slot);
        runtime.entityState.thrownMotionInitialized[slot] = false;
        return withStatus(moved, stunned.status());
    }

    public int bombPrivateCountdown1(int slot) {
        return runtime.entityState.bombPrivateCountdown1[slot];
    }

    public void setBombPrivateCountdown1(int slot, int value) {
        runtime.entityState.bombPrivateCountdown1[slot] = value;
    }

    public int bombPrivateState4(int slot) {
        return runtime.entityState.bombPrivateState4[slot];
    }
}
