package linksawakening.world.entity;

import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;







import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM bomb explosion, damage and delayed presentation ordering. */
public final class EntityBombInteractions {
    private final RoomEntityRuntime runtime;
    EntityBombInteractions(RoomEntityRuntime runtime) { this.runtime = runtime; }

    void applyInitialBombArrowCandidateState(int slot, int romDirection) {
        int x = runtime.slots[slot].x() + bombArrowXOffset(romDirection);
        int y = runtime.slots[slot].y() + bombArrowYOffset(romDirection);
        runtime.slots[slot] = withPositionAndVariant(runtime.slots[slot], x, y,
            runtime.slots[slot].spriteVariant());
        runtime.bombPrivateCountdown3[slot] = 0x03;
        runtime.lastBombPlacementPlayedBump = !runtime.groundInteractionSideScrolling;
        if (runtime.groundInteractionSideScrolling) {
            runtime.slots[slot] = withZ(runtime.slots[slot], 0);
        }
        // ConvertToBombArrowIfNeeded zeros speeds, but BombEntityHandler
        // must still run gravity and floor contact on subsequent frames.
        runtime.thrownEntityMotion.startEnemyBomb(slot, 0, 0, 0);
        // Keep the placed-bomb bridge from restoring directional launch speed.
        runtime.placedBombMotionInitialized[slot] = true;
    }

    void markArrowAsBombArrow(int slot) {
        runtime.playerArrowBombArrow[slot] = true;
        RoomEntity arrow = runtime.slots[slot];
        if (runtime.spriteHandlers != null && arrow.loaded() && arrow.type() == runtime.ENTITY_ARROW) {
            runtime.slots[slot] = withDefinition(arrow, runtime.spriteHandlers.forBombArrow(),
                runtime.playerArrowMotion.direction(slot));
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
            int index, RoomEntity entity, List<EntityProjectileEvent> projectileEvents,
            int linkEntityX, int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        int slot = entity.slot();
        BombMotion.Decision decision = BombMotion.decide(runtime.enemyTransitionCountdown[slot] & 0xFF);
        if (decision.playExplosionSound()) {
            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                slot, runtime.ENTITY_BOMB, 0, false,
                EntityCombatEvent.SoundChannel.NOISE, 0x0C));
        }
        if (decision.countdownOverride().isPresent()) {
            runtime.enemyTransitionCountdown[slot] = decision.countdownOverride().getAsInt();
        }
        if (decision.phase() == BombMotion.Phase.EXPLOSION) {
            runtime.enemyPhysicsFlags[slot] = (runtime.enemyPhysicsFlags[slot] & 0xF0)
                | runtime.BOMB_EXPLOSION_PHYSICS_LOW_BITS;
        }

        EntitySpriteDefinition definition = switch (decision.phase()) {
            case NORMAL -> runtime.slotProperties.spriteDefinitionFor(runtime.ENTITY_BOMB);
            case WARNING -> runtime.spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(runtime.ENTITY_BOMB)
                : runtime.spriteHandlers.forBombRightBeforeExploding();
            case EXPLOSION -> runtime.spriteHandlers == null
                ? EntitySpriteDefinition.unsupported(runtime.ENTITY_BOMB)
                : runtime.spriteHandlers.forBombExplosion();
        };
        int variant = decision.explosionVariant().orElse(0);
        RoomEntity updated = withDefinition(entity, definition, variant);
        runtime.slots[index] = updated;
        queueBombExplosionInteractions(updated, decision, projectileEvents,
            linkEntityX, linkEntityY, projectileLinkState);
        if (decision.unloadAfterPresentation()) {
            runtime.bombFinalPresentationPending[slot] = true;
        }
        return decision;
    }

    void queueBombExplosionInteractions(
            RoomEntity bomb, BombMotion.Decision decision,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX, int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        if (decision.phase() != BombMotion.Phase.EXPLOSION) {
            return;
        }
        int bombSlot = bomb.slot();
        int countdown = runtime.enemyTransitionCountdown[bombSlot] & 0xFF;
        // BombExplosionHandler returns before every interaction for Tarin's
        // transformation explosion. All other bomb variants, including
        // enemy bombs, still run the shared destroyable-object checks.
        if (runtime.bombPrivateState4[bombSlot] == 0x4C) {
            return;
        }
        if (countdown < 0x0E || countdown > 0x16) {
            return;
        }

        int bombX = bomb.x() & 0xFF;
        int bombVisualY = (bomb.y() - bomb.z()) & 0xFF;
        runtime.pendingBombExplosionEvents.add(new BombExplosionEvent(bombSlot,
            BombExplosionEvent.OBJECT_TARGET, bombX, bombVisualY, countdown,
            BombExplosionEvent.DAMAGE_TYPE_BOMB));
        if (countdown != 0x12) {
            return;
        }
        if (runtime.bombPrivateState4[bombSlot] != 0) {
            queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY,
                projectileLinkState);
            return;
        }
        runtime.swordMoblinAlertingSoundCounter = 0x04;

        for (int targetSlot = runtime.slots.length - 1; targetSlot >= 0; targetSlot--) {
            RoomEntity target = runtime.slots[targetSlot];
            if (target.status().value() < EntityStatus.ACTIVE.value()
                || (runtime.enemyPhysicsFlags[targetSlot]
                    & (runtime.ENTITY_PHYSICS_PROJECTILE_NOCLIP | runtime.ENTITY_PHYSICS_GRABBABLE)) != 0
                || (runtime.enemyHitboxFlags[targetSlot] & runtime.HITFLAGS_IGNORE_HITS) != 0
                || !isWithinBombExplosionWindow(bombX, target.x())
                || !isWithinBombExplosionWindow(
                    (target.y() - target.z()) & 0xFF, bombVisualY)) {
                continue;
            }
            runtime.pendingBombExplosionEvents.add(new BombExplosionEvent(bombSlot, targetSlot,
                bombX, bombVisualY, countdown, BombExplosionEvent.DAMAGE_TYPE_BOMB));
            applyBombDamage(bomb, target);
        }
    }

    void queueEnemyBombLinkCollision(
            RoomEntity bomb, List<EntityProjectileEvent> projectileEvents,
            int linkEntityX, int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        runtime.swordMoblinAlertingSoundCounter = 0x04;
        int bombX = bomb.x() & 0xFF;
        int bombY = bomb.y() & 0xFF;
        if (!isWithinBombExplosionWindow(bombX, linkEntityX)
            || !isWithinBombExplosionWindow(bombY, linkEntityY)) {
            return;
        }

        boolean collisionProtected = projectileLinkState.invincibilityCounter() != 0;
        int linkDamage = runtime.enemyCombatTables == null
            ? 0x08 : runtime.enemyCombatTables.contactDamage(runtime.ENTITY_BOMB);
        EnemyRecoilMotion.Vector linkResponse = EnemyRecoilMotion.vectorTowardsLinkForLinkCollision(
            bomb.x(), bomb.y(), bomb.z(), linkEntityX, linkEntityY, 0x14);
        projectileEvents.add(new EntityProjectileEvent(
            bomb.slot(), runtime.ENTITY_BOMB, EntityProjectileEvent.Kind.LINK_DAMAGE, 0,
            collisionProtected ? 0 : linkDamage,
            collisionProtected
                ? EntityProjectileEvent.SoundChannel.NONE
                : EntityProjectileEvent.SoundChannel.WAVE,
            collisionProtected ? -1 : 0x03, false, false, 0, 0,
            collisionProtected
                ? (runtime.currentLinkSpeedX << 1) & 0xFF : (linkResponse.x() << 1) & 0xFF,
            collisionProtected
                ? (runtime.currentLinkSpeedY << 1) & 0xFF : (linkResponse.y() << 1) & 0xFF,
            collisionProtected ? 0 : 0x10));
    }

    void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        int targetSlot = target.slot();
        RomEnemyCombatTables.SwordDamageResult damageResult = runtime.enemyCombatTables == null
            ? null : runtime.enemyCombatTables.resolveAttackDamage(target.type(), runtime.DAMAGE_TYPE_BOMB);
        int rawDamage = damageResult == null
            ? RoomEntityCombatRules.basicSwordDamage(target.type())
            : damageResult.rawValue();

        if (rawDamage != 0) {
            int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            EntityCombatEvent.SoundChannel secondaryChannel =
                EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;

            if (rawDamage == 0xFE) {
                runtime.enemyTransitionCountdown[targetSlot] = 0x60;
                runtime.enemyStunnedCountdown[targetSlot] = 0;
                runtime.enemyFlashCountdown[targetSlot] = 0;
                runtime.enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                runtime.enemyPhysicsFlags[targetSlot] = (runtime.enemyPhysicsFlags[targetSlot] + 2) & 0xFF;
                runtime.entityOptions1Override[targetSlot] = runtime.options1(targetSlot) & 0xC2;
                runtime.slots[targetSlot] = withStatus(target, EntityStatus.BURNING);
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                runtime.enemyTransitionCountdown[targetSlot] = 0;
                runtime.enemyStunnedCountdown[targetSlot] = 0xFF;
                runtime.enemyFlashCountdown[targetSlot] = 0;
                runtime.enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                runtime.slots[targetSlot] = withStatus(target, EntityStatus.STUNNED);
            } else if (rawDamage == 0xFD) {
                // The ROM's FD action is dispatched by the target's own
                // entity-specific branch. Preserve it as a special result;
                // the bomb vector below still applies unconditionally.
            } else if (rawDamage < 0xF0) {
                runtime.enemyHealth[targetSlot] = Math.max(0,
                    runtime.enemyHealth[targetSlot] - enemyDamage);
                if (runtime.enemyHealth[targetSlot] == 0) {
                    // ApplySwordDamagesToEnemy uses $2F for the ordinary
                    // dying countdown; $40 is the Piece-of-Power override.
                    runtime.dyingCountdown[targetSlot] = 0x2F;
                    runtime.powerRecoilDeath[targetSlot] = false;
                    runtime.slots[targetSlot] = withDeathPresentation(
                        withStatus(target, EntityStatus.DYING), -1, false);
                } else {
                    runtime.enemyFlashCountdown[targetSlot] = 0x18;
                    runtime.enemyIgnoreHitsCountdown[targetSlot] = 0x0A;
                }
            }

            runtime.pendingEntityEvents.add(new EntityCombatEvent(
                targetSlot, target.type(), 0, false, enemyDamage, specialAction,
                EntityCombatEvent.SoundChannel.JINGLE, 0x03,
                secondaryChannel, secondarySoundId, null));
        }

        runtime.enemyRecoilMotion.configureFromSource(targetSlot,
            bomb.x(), bomb.y(), bomb.z(), target.x(), target.y(), 0x30);
    }

    public RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = stunned.slot();
        RoomEntity moved = stunned;
        if (runtime.enemyRecoilMotion.isActive(slot)) {
            moved = runtime.backgroundMechanics.applyEnemyRecoilIfNeeded(moved, backgroundCollision).entity();
        }
        moved = runtime.thrownEntityMotion.advance(
            moved, runtime.groundInteractionSideScrolling, backgroundCollision, frame,
            runtime.entityGroundStatus[slot]).entity();
        runtime.thrownEntityMotion.clear(slot);
        runtime.thrownMotionInitialized[slot] = false;
        return withStatus(moved, stunned.status());
    }
}
