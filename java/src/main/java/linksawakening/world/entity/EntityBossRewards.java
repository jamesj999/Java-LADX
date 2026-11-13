package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.HeartContainerRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Boss destruction and source-shaped reward production. */
public final class EntityBossRewards {

    EnemyDropResolver.Result resolveDrop(EnemyDropResolver.Context context) {
        return state.readEnemyDropLowHealth() == null
                ? state.readEnemyDropResolver().resolve(context)
                : state.readEnemyDropResolver().resolve(context, state.readEnemyDropLowHealth());
    }

    final EntityBossState state;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupController pickups;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    final RoomEntity[] slots;

    final EntityLinkState linkState;

    EntityBossRewards(
            EntityBossState state,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupController pickups,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RoomEntity[] slots,
            EntityLinkState linkState) {
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");

        this.state = java.util.Objects.requireNonNull(state, "state");
        this.spriteHandlers = spriteHandlers;
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
    }

    /** ApplySwordDamagesToEnemy, $03:7361: preserve descending RNG order. */
    void cascadeGhiniDeath(RoomEntity killed, IntSupplier random) {
        if (killed.type() != 0x12) return;
        int[] drops = {0x2D, 0x2E, 0x38, 0x37};
        for (int slot = RoomEntity.SLOT_COUNT - 1; slot >= 0; slot--) {
            RoomEntity other = this.slots[slot];
            if (slot == killed.slot()
                    || other == null
                    || (other.type() != 0x10 && other.type() != 0x11)
                    || this.motions.get(GhiniMotion.class, GhiniMotion::new).state(slot) != 0
                    || other.status() == EntityStatus.DISABLED) continue;
            this.slots[slot] = withStatus(other, EntityStatus.DYING);
            this.combatTables.writeDyingCountdown(slot, 0x1F);
            state.writeDroppedItemBySlot(slot, drops[random.getAsInt() & 3]);
        }
        state.writeDroppedItemBySlot(killed.slot(), 0x2E);
    }

    void handleTerminalEnemyDeath(RoomEntity entity, IntSupplier randomByteSupplier) {
        BossDeathBehavior.handleTerminalEnemyDeath(this, entity, randomByteSupplier);
    }

    public void advanceRollingBonesDestruction(RoomEntity entity, IntSupplier randomByteSupplier) {
        BossDeathBehavior.advanceRollingBonesDestruction(this, entity, randomByteSupplier);
    }

    public void advanceMoblinKingDestruction(RoomEntity source, int frame, IntSupplier random) {
        BossDeathBehavior.advanceMoblinKingDestruction(this, source, frame, random);
    }

    public void advanceMoldormDestruction(RoomEntity source, int frameCounter) {
        MoldormDeathBehavior.advanceMoldormDestruction(this, source, frameCounter);
    }

    void advanceMoldormBossExplosion(RoomEntity source) {
        MoldormDeathBehavior.advanceMoldormBossExplosion(this, source);
    }

    void spawnMoldormHeartContainer(RoomEntity source) {
        MoldormDeathBehavior.spawnMoldormHeartContainer(this, source);
    }

    void createMoldormPoof(RoomEntity source, int x, int y) {
        this.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.POOF, x, y));
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x13));
    }

    public void advanceCollectedHeartContainer(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (this.physicsTables.readEnemyTransitionCountdown(slot) == 1) {
            this.events.pendingHeartContainerRewards.add(new HeartContainerRewardEvent(slot));
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        this.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        this.slots[slot] = withZ(held, linkZ & 0xFF);
    }

    void advanceBossKeyDropProducer(RoomEntity source, IntSupplier randomByteSupplier) {
        BossKeyDropBehavior.advanceBossKeyDropProducer(this, source, randomByteSupplier);
    }

    final BossKeyDropStep bossKeyDropStep = new BossKeyDropStep();

    void handleBowWowTargetContact(
            RoomEntity source, BowWowMotion.TargetContact contact, IntSupplier randomByteSupplier) {
        int targetSlot = contact.slot();
        if (targetSlot < 0 || targetSlot >= this.slots.length) {
            return;
        }
        RoomEntity target = this.slots[targetSlot];
        if (!target.loaded()) {
            return;
        }

        if (target.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
            if (this.physicsTables.readEntityPrivateState4(targetSlot) == 0
                    || this.linkState.dialogActive
                    || this.motions.get(BowWowMotion.class, BowWowMotion::new).privateCountdown2(source.slot()) != 0) {
                return;
            }
            this.motions.get(BowWowMotion.class, BowWowMotion::new).setTransitionCountdown(source.slot(), 0);
            this.motions.get(BowWowMotion.class, BowWowMotion::new).setPrivateCountdown2(source.slot(), 0x80);
            this.events.pendingDialogRequests.add(new DialogRequest(1, EntityRomConstants.DIALOG_BOW_WOW));
            return;
        }

        if (this.combatTables.readEnemyFlashCountdown(targetSlot) != 0) {
            return;
        }
        this.events.pendingEntityEvents.add(new EntityCombatEvent(
                source.slot(), source.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x03));
        if (target.type() == EntityRomConstants.ENTITY_KIKI_THE_MONKEY) {
            this.combatTables.writeEnemyFlashCountdown(targetSlot, 0x18);
            this.physicsTables.writeEntityInertia(
                    targetSlot, (this.physicsTables.readEntityInertia(targetSlot) + 1) & 0xFF);
            return;
        }
        this.handleTerminalEnemyDeath(target, randomByteSupplier);
    }
}
