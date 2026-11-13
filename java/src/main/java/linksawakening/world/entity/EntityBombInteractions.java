package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;

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
        return BombCollisionBehavior.advanceBombEntity(
                this, index, entity, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
    }

    void queueBombExplosionInteractions(
            RoomEntity bomb,
            BombMotion.Decision decision,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        BombCollisionBehavior.queueBombExplosionInteractions(
                this, bomb, decision, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
    }

    void queueEnemyBombLinkCollision(
            RoomEntity bomb,
            List<EntityProjectileEvent> projectileEvents,
            int linkEntityX,
            int linkEntityY,
            EnemyProjectileCollision.LinkState projectileLinkState) {
        BombCollisionBehavior.queueEnemyBombLinkCollision(
                this, bomb, projectileEvents, linkEntityX, linkEntityY, projectileLinkState);
    }

    void applyBombDamage(RoomEntity bomb, RoomEntity target) {
        BombCollisionBehavior.applyBombDamage(this, bomb, target);
    }

    public RoomEntity applyBombStunnedPostActiveWork(
            RoomEntity stunned, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        return BombCollisionBehavior.applyBombStunnedPostActiveWork(this, stunned, backgroundCollision, frame);
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
