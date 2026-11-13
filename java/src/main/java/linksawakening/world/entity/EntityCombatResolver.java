package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.List;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** ROM sword and contact combat resolution, sharing the owning room slot table. */
final class EntityCombatResolver {
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBackgroundMechanics backgroundMechanics;
    final EntityBombInteractions bombInteractions;
    final EntityBossRewards bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityRoomInspection roomInspection;
    final EntitySlotProperties slotProperties;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;
    final SlimeEyeController slimeEye;
    final EntityFrameRandom frameRandom;
    private final EntityCombatSlot slotScratch;
    private boolean slotScratchInUse;
    final boolean[] deferredLeeverContact = new boolean[RoomEntity.SLOT_COUNT];
    final int[] contactLinkX = new int[RoomEntity.SLOT_COUNT];
    final int[] contactLinkY = new int[RoomEntity.SLOT_COUNT];

    EntityCombatResolver(
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBackgroundMechanics backgroundMechanics,
            EntityBombInteractions bombInteractions,
            EntityBossRewards bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityRoomInspection roomInspection,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntity[] slots,
            SlimeEyeController slimeEye,
            EntityFrameRandom frameRandom) {
        this.spriteHandlers = spriteHandlers;
        this.backgroundMechanics = java.util.Objects.requireNonNull(backgroundMechanics, "backgroundMechanics");
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = enemyCombatTables;
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.slimeEye = java.util.Objects.requireNonNull(slimeEye, "slimeEye");
        this.frameRandom = java.util.Objects.requireNonNull(frameRandom, "frameRandom");
        slotScratch = new EntityCombatSlot(this);
    }

    EntityCombatSlot acquireSlot() {
        if (slotScratchInUse) return new EntityCombatSlot(this);
        slotScratchInUse = true;
        return slotScratch;
    }

    void releaseSlot(EntityCombatSlot slot) {
        slot.reset(null, null, null);
        if (slot == slotScratch) slotScratchInUse = false;
    }

    boolean deferContactRecoil(int slot, int linkX, int linkY) {
        RoomEntity entity = this.slots[slot];
        if (entity.type() != EntityRomConstants.ENTITY_LEEVER || entity.status() != EntityStatus.ACTIVE) return false;
        deferredLeeverContact[slot] = true;
        contactLinkX[slot] = linkX;
        contactLinkY[slot] = linkY;
        return true;
    }

    void clearDeferredContactRecoil() {
        java.util.Arrays.fill(deferredLeeverContact, false);
    }

    void finishLeeverContactRecoil(RoomEntity moved) {
        EnemyContactRecoil.finishLeeverContactRecoil(this, moved);
    }

    List<EntityCombatEvent> resolveCombat(EnemyCombatInputs inputs) {
        return EntityCombatPass.resolveCombat(this, inputs);
    }
}
