package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.bombArrowXOffset;
import static linksawakening.world.entity.EntityMotionMath.bombArrowYOffset;
import static linksawakening.world.entity.EntityMotionMath.isWithinBombExplosionWindow;
import static linksawakening.world.entity.EntitySnapshots.withDeathPresentation;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.BombExplosionEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** ROM bomb explosion, damage and delayed presentation ordering. */
public final class EntityBombInteractions {
    final EntityBombState state;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntitySlotProperties slotProperties;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;
    final EntitySlotLifecycle lifecycle;

    EntityBombInteractions(
            EntityBombState state,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntity[] slots,
            EntitySlotLifecycle lifecycle) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.spriteHandlers = spriteHandlers;
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = enemyCombatTables;
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    void applyInitialBombArrowCandidateState(int slot, int romDirection) {
        int x = this.slots[slot].x() + bombArrowXOffset(romDirection);
        int y = this.slots[slot].y() + bombArrowYOffset(romDirection);
        this.slots[slot] = withPositionAndVariant(this.slots[slot], x, y, this.slots[slot].spriteVariant());
        state.writeBombPrivateCountdown3(slot, 0x03);
        state.writeLastBombPlacementPlayedBump(!this.physicsTables.readGroundInteractionSideScrolling());
        if (this.physicsTables.readGroundInteractionSideScrolling()) {
            this.slots[slot] = withZ(this.slots[slot], 0);
        }
        // ConvertToBombArrowIfNeeded zeros speeds, but BombEntityHandler
        // must still run gravity and floor contact on subsequent frames.
        this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).startEnemyBomb(slot, 0, 0, 0);
        // Keep the placed-bomb bridge from restoring directional launch speed.
        state.writePlacedBombMotionInitialized(slot, true);
    }

    void markArrowAsBombArrow(int slot) {
        state.writePlayerArrowBombArrow(slot, true);
        RoomEntity arrow = this.slots[slot];
        if (this.spriteHandlers != null && arrow.loaded() && arrow.type() == EntityRomConstants.ENTITY_ARROW) {
            this.slots[slot] = withDefinition(
                    arrow,
                    this.slotProperties.spriteViews().forBombArrow(),
                    this.motions
                            .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                            .direction(slot));
        }
    }

    void finalizePendingBombPresentations() {
        for (int slot = 0; slot < this.slots.length; slot++) {
            if (state.bombFinalPresentationPending(slot)) {
                this.lifecycle.clearEntity(slot);
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
        return EntityBombInteractions.bombCollisionBehaviorAdvanceBombEntity(
                this, index, entity, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
    }

    void queueBombExplosionInteractions(
            RoomEntity bomb,
            BombMotion.Decision decision,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        EntityBombInteractions.bombCollisionBehaviorQueueBombExplosionInteractions(
                this, bomb, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
    }

    void queueEnemyBombLinkCollision(
            RoomEntity bomb,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        EntityBombInteractions.bombCollisionBehaviorQueueEnemyBombLinkCollision(
                this, bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
    }

    void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        EntityBombInteractions.bombCollisionBehaviorApplyBombDamage(this, bomb, target);
    }

    public RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        return EntityBombInteractions.bombCollisionBehaviorApplyBombStunnedPostActiveWork(
                this, stunned, backgroundCollision, frame);
    }

    public int bombPrivateCountdown1(int slot) {
        return state.readBombPrivateCountdown1(slot);
    }

    public void setBombPrivateCountdown1(int slot, int value) {
        state.writeBombPrivateCountdown1(slot, value);
    }

    public int bombPrivateState4(int slot) {
        return state.readBombPrivateState4(slot);
    }

    static BombMotion.Decision bombCollisionBehaviorAdvanceBombEntity(
            EntityBombInteractions owner,
            int index,
            RoomEntity entity,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        var runtime = owner;
        int slot = entity.slot();
        BombMotion.Decision decision =
                BombMotion.decide(runtime.physicsTables.readEnemyTransitionCountdown(slot) & 0xFF);
        if (decision.playExplosionSound()) {
            runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                    slot, EntityRomConstants.ENTITY_BOMB, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x0C));
        }
        if (decision.countdownOverride().isPresent()) {
            runtime.physicsTables.writeEnemyTransitionCountdown(
                    slot, decision.countdownOverride().getAsInt());
        }
        if (decision.phase() == BombMotion.Phase.EXPLOSION) {
            runtime.physicsTables.writeEnemyPhysicsFlags(
                    slot,
                    (runtime.physicsTables.readEnemyPhysicsFlags(slot) & 0xF0)
                            | EntityRomConstants.BOMB_EXPLOSION_PHYSICS_LOW_BITS);
        }

        EntitySpriteDefinition definition =
                switch (decision.phase()) {
                    case NORMAL -> runtime.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
                    case WARNING -> runtime.spriteHandlers == null
                            ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BOMB)
                            : runtime.slotProperties.spriteViews().forBombRightBeforeExploding();
                    case EXPLOSION -> runtime.spriteHandlers == null
                            ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BOMB)
                            : runtime.slotProperties.spriteViews().forBombExplosion();
                };
        int variant = decision.explosionVariant().orElse(0);
        RoomEntity updated = withDefinition(entity, definition, variant);
        runtime.slots[index] = updated;
        owner.queueBombExplosionInteractions(
                updated, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
        if (decision.unloadAfterPresentation()) {
            owner.state.setBombFinalPresentationPending(slot, true);
        }
        return decision;
    }

    static void bombCollisionBehaviorQueueBombExplosionInteractions(
            EntityBombInteractions owner,
            RoomEntity bomb,
            BombMotion.Decision decision,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        var runtime = owner;
        if (decision.phase() != BombMotion.Phase.EXPLOSION) {
            return;
        }
        int bombSlot = bomb.slot();
        int countdown = runtime.physicsTables.readEnemyTransitionCountdown(bombSlot) & 0xFF;
        // BombExplosionHandler returns before every interaction for Tarin's
        // transformation explosion. All other bomb variants, including
        // enemy bombs, still run the shared destroyable-object checks.
        if (owner.state.readBombPrivateState4(bombSlot) == 0x4C) {
            return;
        }
        if (countdown < 0x0E || countdown > 0x16) {
            return;
        }

        int bombX = bomb.x() & 0xFF;
        int bombVisualY = (bomb.y() - bomb.z()) & 0xFF;
        runtime.events.pendingBombExplosionEvents.add(new BombExplosionEvent(
                bombSlot,
                BombExplosionEvent.OBJECT_TARGET,
                bombX,
                bombVisualY,
                countdown,
                BombExplosionEvent.DAMAGE_TYPE_BOMB));
        if (countdown != 0x12) {
            return;
        }
        if (owner.state.readBombPrivateState4(bombSlot) != 0) {
            owner.queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
            return;
        }
        runtime.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new).writeSwordMoblinAlertingSoundCounter(0x04);

        for (int targetSlot = runtime.slots.length - 1; targetSlot >= 0; targetSlot--) {
            RoomEntity target = runtime.slots[targetSlot];
            if (target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & (EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                                            | EntityRomConstants.ENTITY_PHYSICS_GRABBABLE))
                            != 0
                    || (runtime.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || !isWithinBombExplosionWindow(bombX, target.x())
                    || !isWithinBombExplosionWindow((target.y() - target.z()) & 0xFF, bombVisualY)) {
                continue;
            }
            runtime.events.pendingBombExplosionEvents.add(new BombExplosionEvent(
                    bombSlot, targetSlot, bombX, bombVisualY, countdown, BombExplosionEvent.DAMAGE_TYPE_BOMB));
            owner.applyBombDamage(bomb, target);
        }
    }

    static void bombCollisionBehaviorQueueEnemyBombLinkCollision(
            EntityBombInteractions owner,
            RoomEntity bomb,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        var runtime = owner;
        runtime.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new).writeSwordMoblinAlertingSoundCounter(0x04);
        int bombX = bomb.x() & 0xFF;
        int bombY = bomb.y() & 0xFF;
        if (!isWithinBombExplosionWindow(bombX, linkEntityX) || !isWithinBombExplosionWindow(bombY, linkEntityY)) {
            return;
        }

        boolean collisionProtected = projectileLinkState.invincibilityCounter() != 0;
        int linkDamage = runtime.enemyCombatTables == null
                ? 0x08
                : runtime.enemyCombatTables.contactDamageForGroup(
                        runtime.combatTables.readEnemyHealthGroup(bomb.slot()));
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
                collisionProtected ? (runtime.linkState.currentLinkSpeedX << 1) & 0xFF : (linkResponse.x() << 1) & 0xFF,
                collisionProtected ? (runtime.linkState.currentLinkSpeedY << 1) & 0xFF : (linkResponse.y() << 1) & 0xFF,
                collisionProtected ? 0 : 0x10));
    }

    static void bombCollisionBehaviorApplyBombDamage(EntityBombInteractions owner, RoomEntity bomb, RoomEntity target) {
        var runtime = owner;
        int targetSlot = target.slot();
        RomEnemyCombatTables.SwordDamageResult damageResult = runtime.enemyCombatTables == null
                ? null
                : runtime.enemyCombatTables.resolveAttackDamage(
                        target.type(),
                        runtime.combatTables.readEnemyHealthGroup(target.slot()),
                        EntityRomConstants.DAMAGE_TYPE_BOMB);
        int rawDamage =
                damageResult == null ? RoomEntityCombatRules.basicSwordDamage(target.type()) : damageResult.rawValue();

        if (rawDamage != 0) {
            int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            EntityCombatEvent.SoundChannel secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;

            if (rawDamage == 0xFE) {
                runtime.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0x60);
                runtime.combatTables.writeEnemyStunnedCountdown(targetSlot, 0);
                runtime.combatTables.writeEnemyFlashCountdown(targetSlot, 0);
                runtime.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                runtime.physicsTables.writeEnemyPhysicsFlags(
                        targetSlot, (runtime.physicsTables.readEnemyPhysicsFlags(targetSlot) + 2) & 0xFF);
                runtime.physicsTables.writeEntityOptions1Override(
                        targetSlot, runtime.slotProperties.options1(targetSlot) & 0xC2);
                runtime.slots[targetSlot] = withStatus(target, EntityStatus.BURNING);
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                runtime.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0);
                runtime.combatTables.writeEnemyStunnedCountdown(targetSlot, 0xFF);
                runtime.combatTables.writeEnemyFlashCountdown(targetSlot, 0);
                runtime.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                runtime.slots[targetSlot] = withStatus(target, EntityStatus.STUNNED);
            } else if (rawDamage == 0xFD) {
                // The ROM's FD action is dispatched by the target's own
                // entity-specific branch. Preserve it as a special result;
                // the bomb vector below still applies unconditionally.
            } else if (rawDamage < 0xF0) {
                runtime.combatTables.writeEnemyHealth(
                        targetSlot, Math.max(0, runtime.combatTables.readEnemyHealth(targetSlot) - enemyDamage));
                if (runtime.combatTables.readEnemyHealth(targetSlot) == 0) {
                    // ApplySwordDamagesToEnemy uses $2F for the ordinary
                    // dying countdown; $40 is the Piece-of-Power override.
                    runtime.combatTables.writeDyingCountdown(targetSlot, 0x2F);
                    runtime.combatTables.writePowerRecoilDeath(targetSlot, false);
                    runtime.slots[targetSlot] =
                            withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false);
                } else {
                    runtime.combatTables.writeEnemyFlashCountdown(targetSlot, 0x18);
                    runtime.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                }
            }

            runtime.events.pendingEntityEvents.add(
                    linksawakening.world.entity.event.EntityCombatEvent.builder(targetSlot, target.type())
                            .linkDamage(0)
                            .swordHit(false)
                            .enemyDamage(enemyDamage)
                            .enemySpecialAction(specialAction)
                            .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                            .soundId(0x03)
                            .secondarySoundChannel(secondaryChannel)
                            .secondarySoundId(secondarySoundId)
                            .swordPokeVfx(null)
                            .build());
        }

        runtime.motions
                .get(EnemyRecoilMotion.class, EnemyRecoilMotion::new)
                .configureFromSource(targetSlot, bomb.x(), bomb.y(), bomb.z(), target.x(), target.y(), 0x30);
    }

    static RoomEntity bombCollisionBehaviorApplyBombStunnedPostActiveWork(
            EntityBombInteractions owner,
            RoomEntity stunned,
            RoomEntityBackgroundCollision backgroundCollision,
            int frame) {
        var runtime = owner;
        int slot = stunned.slot();
        RoomEntity moved = stunned;
        if (runtime.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new).isActive(slot)) {
            moved = runtime.backgroundMechanics
                    .applyEnemyRecoilIfNeeded(moved, backgroundCollision)
                    .entity();
        }
        moved = runtime.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        moved,
                        runtime.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        runtime.physicsTables.readEntityGroundStatus(slot))
                .entity();
        runtime.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new).clear(slot);
        runtime.liftThrow.writeThrownMotionInitialized(slot, false);
        return withStatus(moved, stunned.status());
    }
}
