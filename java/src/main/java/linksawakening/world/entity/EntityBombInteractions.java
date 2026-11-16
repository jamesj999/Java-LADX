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
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntitySlotProperties slotProperties;
    final RomEnemyCombatTables enemyCombatTables;
    final EntitySlots slots;
    final EntitySlotLifecycle lifecycle;

    EntityBombInteractions(
            EntityBombState state,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            EntitySlots slots,
            EntitySlotLifecycle lifecycle) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.spriteHandlers = java.util.Objects.requireNonNull(spriteHandlers, "sprite source");
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = java.util.Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
    }

    void applyInitialBombArrowCandidateState(int slot, int romDirection) {
        int x = this.slots.get(slot).x() + bombArrowXOffset(romDirection);
        int y = this.slots.get(slot).y() + bombArrowYOffset(romDirection);
        this.slots.replace(
                slot,
                withPositionAndVariant(
                        this.slots.get(slot), x, y, this.slots.get(slot).spriteVariant()));
        state.writeBombPrivateCountdown3(slot, 0x03);
        state.writeLastBombPlacementPlayedBump(!this.physicsTables.readGroundInteractionSideScrolling());
        if (this.physicsTables.readGroundInteractionSideScrolling()) {
            this.slots.replace(slot, withZ(this.slots.get(slot), 0));
        }
        // ConvertToBombArrowIfNeeded zeros speeds, but BombEntityHandler
        // must still run gravity and floor contact on subsequent frames.
        var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        thrownEntityMotion.startEnemyBomb(slot, 0, 0, 0);
        // Keep the placed-bomb bridge from restoring directional launch speed.
        state.writePlacedBombMotionInitialized(slot, true);
    }

    void markArrowAsBombArrow(int slot) {
        state.writePlayerArrowBombArrow(slot, true);
        RoomEntity arrow = this.slots.get(slot);
        if (arrow.loaded() && arrow.type() == EntityRomConstants.ENTITY_ARROW) {
            var playerArrowMotion = this.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
            this.slots.replace(
                    slot,
                    this.spriteHandlers.present(
                            arrow,
                            this.slotProperties.spriteViews().forBombArrow(),
                            playerArrowMotion.direction(slot)));
        }
    }

    void finalizePendingBombPresentations() {
        for (int slot = 0; slot < this.slots.size(); slot++) {
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
        int slot = entity.slot();
        BombMotion.Decision decision = BombMotion.decide(this.physicsTables.readEnemyTransitionCountdown(slot) & 0xFF);
        if (decision.playExplosionSound()) {
            this.events.sound(slot, EntityRomConstants.ENTITY_BOMB, EntityCombatEvent.SoundChannel.NOISE, 0x0C);
        }
        if (decision.countdownOverride().isPresent()) {
            this.physicsTables.writeEnemyTransitionCountdown(
                    slot, decision.countdownOverride().getAsInt());
        }
        if (decision.phase() == BombMotion.Phase.EXPLOSION) {
            this.physicsTables.writeEnemyPhysicsFlags(
                    slot,
                    (this.physicsTables.readEnemyPhysicsFlags(slot) & 0xF0)
                            | EntityRomConstants.BOMB_EXPLOSION_PHYSICS_LOW_BITS);
        }

        EntitySpriteDefinition definition =
                switch (decision.phase()) {
                    case NORMAL -> this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
                    case WARNING -> this.slotProperties.spriteViews().forBombRightBeforeExploding();
                    case EXPLOSION -> this.slotProperties.spriteViews().forBombExplosion();
                };
        int variant = decision.explosionVariant().orElse(0);
        RoomEntity updated = withDefinition(entity, definition, variant);
        this.slots.replace(index, updated);
        this.queueBombExplosionInteractions(
                updated, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
        if (decision.unloadAfterPresentation()) {
            this.state.setBombFinalPresentationPending(slot, true);
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
        int countdown = this.physicsTables.readEnemyTransitionCountdown(bombSlot) & 0xFF;
        // BombExplosionHandler returns before every interaction for Tarin's
        // transformation explosion. All other bomb variants, including
        // enemy bombs, still run the shared destroyable-object checks.
        if (this.state.readBombPrivateState4(bombSlot) == 0x4C) {
            return;
        }
        if (countdown < 0x0E || countdown > 0x16) {
            return;
        }

        int bombX = bomb.x() & 0xFF;
        int bombVisualY = (bomb.y() - bomb.z()) & 0xFF;
        this.events.bombExplosion(new BombExplosionEvent(
                bombSlot,
                BombExplosionEvent.OBJECT_TARGET,
                bombX,
                bombVisualY,
                countdown,
                BombExplosionEvent.DAMAGE_TYPE_BOMB));
        if (countdown != 0x12) {
            return;
        }
        if (this.state.readBombPrivateState4(bombSlot) != 0) {
            this.queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
            return;
        }
        var moblinSwordMotion = this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
        moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(0x04);

        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            RoomEntity target = this.slots.get(targetSlot);
            if (target.status().value() < EntityStatus.ACTIVE.value()
                    || (this.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & (EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                                            | EntityRomConstants.ENTITY_PHYSICS_GRABBABLE))
                            != 0
                    || (this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || !isWithinBombExplosionWindow(bombX, target.x())
                    || !isWithinBombExplosionWindow((target.y() - target.z()) & 0xFF, bombVisualY)) {
                continue;
            }
            this.events.bombExplosion(new BombExplosionEvent(
                    bombSlot, targetSlot, bombX, bombVisualY, countdown, BombExplosionEvent.DAMAGE_TYPE_BOMB));
            this.applyBombDamage(bomb, target);
        }
    }

    void queueEnemyBombLinkCollision(
            RoomEntity bomb,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        var moblinSwordMotion = this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
        moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(0x04);
        int bombX = bomb.x() & 0xFF;
        int bombY = bomb.y() & 0xFF;
        if (!isWithinBombExplosionWindow(bombX, linkEntityX) || !isWithinBombExplosionWindow(bombY, linkEntityY)) {
            return;
        }

        boolean collisionProtected = projectileLinkState.invincibilityCounter() != 0;
        int linkDamage = this.enemyCombatTables.contactDamage(
                EntityRomConstants.ENTITY_BOMB, this.combatTables.readEnemyHealthGroup(bomb.slot()));
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
                        ? (this.linkState.readCurrentLinkSpeedX() << 1) & 0xFF
                        : (linkResponse.x() << 1) & 0xFF,
                collisionProtected
                        ? (this.linkState.readCurrentLinkSpeedY() << 1) & 0xFF
                        : (linkResponse.y() << 1) & 0xFF,
                collisionProtected ? 0 : 0x10));
    }

    void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        int targetSlot = target.slot();
        RomEnemyCombatTables.SwordDamageResult damageResult = this.enemyCombatTables.resolveAttackDamage(
                target.type(),
                this.combatTables.readEnemyHealthGroup(target.slot()),
                EntityRomConstants.DAMAGE_TYPE_BOMB);
        int rawDamage = damageResult.rawValue();

        if (rawDamage != 0) {
            int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            EntityCombatEvent.SoundChannel secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;

            if (rawDamage == 0xFE) {
                this.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0x60);
                this.combatTables.writeEnemyStunnedCountdown(targetSlot, 0);
                this.combatTables.writeEnemyFlashCountdown(targetSlot, 0);
                this.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                this.physicsTables.writeEnemyPhysicsFlags(
                        targetSlot, (this.physicsTables.readEnemyPhysicsFlags(targetSlot) + 2) & 0xFF);
                this.physicsTables.writeEntityOptions1Override(
                        targetSlot, this.slotProperties.options1(targetSlot) & 0xC2);
                this.slots.replace(targetSlot, withStatus(target, EntityStatus.BURNING));
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                this.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0);
                this.combatTables.writeEnemyStunnedCountdown(targetSlot, 0xFF);
                this.combatTables.writeEnemyFlashCountdown(targetSlot, 0);
                this.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                this.slots.replace(targetSlot, withStatus(target, EntityStatus.STUNNED));
            } else if (rawDamage == 0xFD) {
                // The ROM's FD action is dispatched by the target's own
                // entity-specific branch. Preserve it as a special result;
                // the bomb vector below still applies unconditionally.
            } else if (rawDamage < 0xF0) {
                this.combatTables.writeEnemyHealth(
                        targetSlot, Math.max(0, this.combatTables.readEnemyHealth(targetSlot) - enemyDamage));
                if (this.combatTables.readEnemyHealth(targetSlot) == 0) {
                    // ApplySwordDamagesToEnemy uses $2F for the ordinary
                    // dying countdown; $40 is the Piece-of-Power override.
                    this.combatTables.writeDyingCountdown(targetSlot, 0x2F);
                    this.combatTables.writePowerRecoilDeath(targetSlot, false);
                    this.slots.replace(
                            targetSlot, withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false));
                } else {
                    this.combatTables.writeEnemyFlashCountdown(targetSlot, 0x18);
                    this.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                }
            }

            this.events.combat(linksawakening.world.entity.event.EntityCombatEvent.builder(targetSlot, target.type())
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

        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        enemyRecoilMotion.configureFromSource(targetSlot, bomb.x(), bomb.y(), bomb.z(), target.x(), target.y(), 0x30);
    }

    public RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = stunned.slot();
        RoomEntity moved = stunned;
        var enemyRecoilMotion = this.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
        if (enemyRecoilMotion.isActive(slot)) {
            moved = this.backgroundMechanics
                    .applyEnemyRecoilIfNeeded(moved, backgroundCollision)
                    .entity();
        }
        var thrownEntityMotion = this.motions.get(ThrownEntityMotion.class, ThrownEntityMotion::new);
        moved = thrownEntityMotion
                .advance(
                        moved,
                        this.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        this.physicsTables.readEntityGroundStatus(slot))
                .entity();
        thrownEntityMotion.clear(slot);
        this.liftThrow.writeThrownMotionInitialized(slot, false);
        return withStatus(moved, stunned.status());
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
}
