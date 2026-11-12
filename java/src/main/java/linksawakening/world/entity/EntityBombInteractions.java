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
    private final boolean[] bombFinalPresentationPending = new boolean[EntityRoomLoader.MAX_ENTITIES];

    private final int[] bombDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] playerArrowBombArrow = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateCountdown1 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateCountdown3 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] bombPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] placedBombMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] enemyBombMotionInitialized = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int bombArrowCooldown;
    private int latestDroppedBombEntityIndex = -1;
    private boolean lastBombPlacementPlayedBump;
    private final EntityBombInteractionsServices runtime;

    EntityBombInteractions(EntityBombInteractionsServices runtime) {
        this.runtime = runtime;
    }

    void applyInitialBombArrowCandidateState(int slot, int romDirection) {
        int x = runtime.slots()[slot].x() + bombArrowXOffset(romDirection);
        int y = runtime.slots()[slot].y() + bombArrowYOffset(romDirection);
        runtime.slots()[slot] =
                withPositionAndVariant(runtime.slots()[slot], x, y, runtime.slots()[slot].spriteVariant());
        this.writeBombPrivateCountdown3(slot, 0x03);
        this.writeLastBombPlacementPlayedBump(!runtime.physicsTables().readGroundInteractionSideScrolling());
        if (runtime.physicsTables().readGroundInteractionSideScrolling()) {
            runtime.slots()[slot] = withZ(runtime.slots()[slot], 0);
        }
        // ConvertToBombArrowIfNeeded zeros speeds, but BombEntityHandler
        // must still run gravity and floor contact on subsequent frames.
        runtime.motionSet().thrownEntityMotion.startEnemyBomb(slot, 0, 0, 0);
        // Keep the placed-bomb bridge from restoring directional launch speed.
        this.writePlacedBombMotionInitialized(slot, true);
    }

    void markArrowAsBombArrow(int slot) {
        this.writePlayerArrowBombArrow(slot, true);
        RoomEntity arrow = runtime.slots()[slot];
        if (runtime.spriteHandlers() != null && arrow.loaded() && arrow.type() == EntityRomConstants.ENTITY_ARROW) {
            runtime.slots()[slot] = withDefinition(
                    arrow,
                    runtime.spriteHandlers().forBombArrow(),
                    runtime.motionSet().playerArrowMotion.direction(slot));
        }
    }

    void finalizePendingBombPresentations() {
        for (int slot = 0; slot < runtime.slots().length; slot++) {
            if (bombFinalPresentationPending(slot)) {
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
        BombMotion.Decision decision =
                BombMotion.decide(runtime.physicsTables().readEnemyTransitionCountdown(slot) & 0xFF);
        if (decision.playExplosionSound()) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot,
                            EntityRomConstants.ENTITY_BOMB,
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            0x0C));
        }
        if (decision.countdownOverride().isPresent()) {
            runtime.physicsTables()
                    .writeEnemyTransitionCountdown(
                            slot, decision.countdownOverride().getAsInt());
        }
        if (decision.phase() == BombMotion.Phase.EXPLOSION) {
            runtime.physicsTables()
                    .writeEnemyPhysicsFlags(
                            slot,
                            (runtime.physicsTables().readEnemyPhysicsFlags(slot) & 0xF0)
                                    | EntityRomConstants.BOMB_EXPLOSION_PHYSICS_LOW_BITS);
        }

        EntitySpriteDefinition definition =
                switch (decision.phase()) {
                    case NORMAL -> runtime.slotProperties().spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB);
                    case WARNING -> runtime.spriteHandlers() == null
                            ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BOMB)
                            : runtime.spriteHandlers().forBombRightBeforeExploding();
                    case EXPLOSION -> runtime.spriteHandlers() == null
                            ? EntitySpriteDefinition.unsupported(EntityRomConstants.ENTITY_BOMB)
                            : runtime.spriteHandlers().forBombExplosion();
                };
        int variant = decision.explosionVariant().orElse(0);
        RoomEntity updated = withDefinition(entity, definition, variant);
        runtime.slots()[index] = updated;
        queueBombExplosionInteractions(
                updated, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
        if (decision.unloadAfterPresentation()) {
            setBombFinalPresentationPending(slot, true);
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
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(bombSlot) & 0xFF;
        // BombExplosionHandler returns before every interaction for Tarin's
        // transformation explosion. All other bomb variants, including
        // enemy bombs, still run the shared destroyable-object checks.
        if (this.readBombPrivateState4(bombSlot) == 0x4C) {
            return;
        }
        if (countdown < 0x0E || countdown > 0x16) {
            return;
        }

        int bombX = bomb.x() & 0xFF;
        int bombVisualY = (bomb.y() - bomb.z()) & 0xFF;
        runtime.events()
                .pendingBombExplosionEvents
                .add(new BombExplosionEvent(
                        bombSlot,
                        BombExplosionEvent.OBJECT_TARGET,
                        bombX,
                        bombVisualY,
                        countdown,
                        BombExplosionEvent.DAMAGE_TYPE_BOMB));
        if (countdown != 0x12) {
            return;
        }
        if (this.readBombPrivateState4(bombSlot) != 0) {
            queueEnemyBombLinkCollision(bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
            return;
        }
        runtime.motionSet().moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(0x04);

        for (int targetSlot = runtime.slots().length - 1; targetSlot >= 0; targetSlot--) {
            RoomEntity target = runtime.slots()[targetSlot];
            if (target.status().value() < EntityStatus.ACTIVE.value()
                    || (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot)
                                    & (EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP
                                            | EntityRomConstants.ENTITY_PHYSICS_GRABBABLE))
                            != 0
                    || (runtime.combatTables().readEnemyHitboxFlags(targetSlot)
                                    & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || !isWithinBombExplosionWindow(bombX, target.x())
                    || !isWithinBombExplosionWindow((target.y() - target.z()) & 0xFF, bombVisualY)) {
                continue;
            }
            runtime.events()
                    .pendingBombExplosionEvents
                    .add(new BombExplosionEvent(
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
        runtime.motionSet().moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(0x04);
        int bombX = bomb.x() & 0xFF;
        int bombY = bomb.y() & 0xFF;
        if (!isWithinBombExplosionWindow(bombX, linkEntityX) || !isWithinBombExplosionWindow(bombY, linkEntityY)) {
            return;
        }

        boolean collisionProtected = projectileLinkState.invincibilityCounter() != 0;
        int linkDamage = runtime.enemyCombatTables() == null
                ? 0x08
                : runtime.enemyCombatTables()
                        .contactDamageForGroup(runtime.combatTables().readEnemyHealthGroup(bomb.slot()));
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
                        ? (runtime.linkState().currentLinkSpeedX << 1) & 0xFF
                        : (linkResponse.x() << 1) & 0xFF,
                collisionProtected
                        ? (runtime.linkState().currentLinkSpeedY << 1) & 0xFF
                        : (linkResponse.y() << 1) & 0xFF,
                collisionProtected ? 0 : 0x10));
    }

    void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        int targetSlot = target.slot();
        RomEnemyCombatTables.SwordDamageResult damageResult = runtime.enemyCombatTables() == null
                ? null
                : runtime.enemyCombatTables()
                        .resolveAttackDamage(
                                target.type(),
                                runtime.combatTables().readEnemyHealthGroup(target.slot()),
                                EntityRomConstants.DAMAGE_TYPE_BOMB);
        int rawDamage =
                damageResult == null ? RoomEntityCombatRules.basicSwordDamage(target.type()) : damageResult.rawValue();

        if (rawDamage != 0) {
            int enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            int specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            EntityCombatEvent.SoundChannel secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
            int secondarySoundId = -1;

            if (rawDamage == 0xFE) {
                runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0x60);
                runtime.combatTables().writeEnemyStunnedCountdown(targetSlot, 0);
                runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0);
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                runtime.physicsTables()
                        .writeEnemyPhysicsFlags(
                                targetSlot, (runtime.physicsTables().readEnemyPhysicsFlags(targetSlot) + 2) & 0xFF);
                runtime.physicsTables().writeEntityOptions1Override(targetSlot, runtime.options1(targetSlot) & 0xC2);
                runtime.slots()[targetSlot] = withStatus(target, EntityStatus.BURNING);
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                runtime.physicsTables().writeEnemyTransitionCountdown(targetSlot, 0);
                runtime.combatTables().writeEnemyStunnedCountdown(targetSlot, 0xFF);
                runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0);
                runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                runtime.slots()[targetSlot] = withStatus(target, EntityStatus.STUNNED);
            } else if (rawDamage == 0xFD) {
                // The ROM's FD action is dispatched by the target's own
                // entity-specific branch. Preserve it as a special result;
                // the bomb vector below still applies unconditionally.
            } else if (rawDamage < 0xF0) {
                runtime.combatTables()
                        .writeEnemyHealth(
                                targetSlot,
                                Math.max(0, runtime.combatTables().readEnemyHealth(targetSlot) - enemyDamage));
                if (runtime.combatTables().readEnemyHealth(targetSlot) == 0) {
                    // ApplySwordDamagesToEnemy uses $2F for the ordinary
                    // dying countdown; $40 is the Piece-of-Power override.
                    runtime.combatTables().writeDyingCountdown(targetSlot, 0x2F);
                    runtime.combatTables().writePowerRecoilDeath(targetSlot, false);
                    runtime.slots()[targetSlot] =
                            withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false);
                } else {
                    runtime.combatTables().writeEnemyFlashCountdown(targetSlot, 0x18);
                    runtime.combatTables().writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                }
            }

            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
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

        runtime.motionSet()
                .enemyRecoilMotion
                .configureFromSource(targetSlot, bomb.x(), bomb.y(), bomb.z(), target.x(), target.y(), 0x30);
    }

    public RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = stunned.slot();
        RoomEntity moved = stunned;
        if (runtime.motionSet().enemyRecoilMotion.isActive(slot)) {
            moved = runtime.backgroundMechanics()
                    .applyEnemyRecoilIfNeeded(moved, backgroundCollision)
                    .entity();
        }
        moved = runtime.motionSet()
                .thrownEntityMotion
                .advance(
                        moved,
                        runtime.physicsTables().readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        runtime.physicsTables().readEntityGroundStatus(slot))
                .entity();
        runtime.motionSet().thrownEntityMotion.clear(slot);
        runtime.liftThrow().writeThrownMotionInitialized(slot, false);
        return withStatus(moved, stunned.status());
    }

    public int bombPrivateCountdown1(int slot) {
        return this.readBombPrivateCountdown1(slot);
    }

    public void setBombPrivateCountdown1(int slot, int value) {
        this.writeBombPrivateCountdown1(slot, value);
    }

    public int bombPrivateState4(int slot) {
        return this.readBombPrivateState4(slot);
    }

    int readBombDirection(int slot) {
        return bombDirection[slot];
    }

    int writeBombDirection(int slot, int value) {
        return bombDirection[slot] = value;
    }

    int[] storageBombDirection() {
        return bombDirection;
    }

    boolean readPlayerArrowBombArrow(int slot) {
        return playerArrowBombArrow[slot];
    }

    boolean writePlayerArrowBombArrow(int slot, boolean value) {
        return playerArrowBombArrow[slot] = value;
    }

    int readBombPrivateCountdown1(int slot) {
        return bombPrivateCountdown1[slot];
    }

    int writeBombPrivateCountdown1(int slot, int value) {
        return bombPrivateCountdown1[slot] = value;
    }

    int postBombPrivateCountdown1(int slot, int delta) {
        int previous = bombPrivateCountdown1[slot];
        bombPrivateCountdown1[slot] += delta;
        return previous;
    }

    int readBombPrivateCountdown3(int slot) {
        return bombPrivateCountdown3[slot];
    }

    int writeBombPrivateCountdown3(int slot, int value) {
        return bombPrivateCountdown3[slot] = value;
    }

    int postBombPrivateCountdown3(int slot, int delta) {
        int previous = bombPrivateCountdown3[slot];
        bombPrivateCountdown3[slot] += delta;
        return previous;
    }

    int readBombPrivateState4(int slot) {
        return bombPrivateState4[slot];
    }

    int writeBombPrivateState4(int slot, int value) {
        return bombPrivateState4[slot] = value;
    }

    boolean readPlacedBombMotionInitialized(int slot) {
        return placedBombMotionInitialized[slot];
    }

    boolean writePlacedBombMotionInitialized(int slot, boolean value) {
        return placedBombMotionInitialized[slot] = value;
    }

    boolean readEnemyBombMotionInitialized(int slot) {
        return enemyBombMotionInitialized[slot];
    }

    boolean writeEnemyBombMotionInitialized(int slot, boolean value) {
        return enemyBombMotionInitialized[slot] = value;
    }

    int readBombArrowCooldown() {
        return bombArrowCooldown;
    }

    int writeBombArrowCooldown(int value) {
        return bombArrowCooldown = value;
    }

    int postBombArrowCooldown(int delta) {
        int previous = bombArrowCooldown;
        bombArrowCooldown += delta;
        return previous;
    }

    int readLatestDroppedBombEntityIndex() {
        return latestDroppedBombEntityIndex;
    }

    int writeLatestDroppedBombEntityIndex(int value) {
        return latestDroppedBombEntityIndex = value;
    }

    boolean readLastBombPlacementPlayedBump() {
        return lastBombPlacementPlayedBump;
    }

    boolean writeLastBombPlacementPlayedBump(boolean value) {
        return lastBombPlacementPlayedBump = value;
    }

    boolean bombFinalPresentationPending(int slot) {
        return bombFinalPresentationPending[slot];
    }

    void setBombFinalPresentationPending(int slot, boolean value) {
        bombFinalPresentationPending[slot] = value;
    }

    void clearBombFinalPresentations() {
        java.util.Arrays.fill(bombFinalPresentationPending, false);
    }
}
