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
    final EntityCombatResolverServices runtime;
    private final EntityCombatSlot slotScratch;
    private boolean slotScratchInUse;
    final boolean[] deferredLeeverContact = new boolean[RoomEntity.SLOT_COUNT];
    final int[] contactLinkX = new int[RoomEntity.SLOT_COUNT];
    final int[] contactLinkY = new int[RoomEntity.SLOT_COUNT];

    EntityCombatResolver(EntityCombatResolverServices runtime) {
        this.runtime = runtime;
        slotScratch = new EntityCombatSlot(runtime);
    }

    EntityCombatSlot acquireSlot() {
        if (slotScratchInUse) return new EntityCombatSlot(runtime);
        slotScratchInUse = true;
        return slotScratch;
    }

    void releaseSlot(EntityCombatSlot slot) {
        slot.reset(null, null, null);
        if (slot == slotScratch) slotScratchInUse = false;
    }

    boolean deferContactRecoil(int slot, int linkX, int linkY) {
        RoomEntity entity = runtime.slots()[slot];
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

    List<EntityCombatEvent> resolveCombat(EnemyCombatFrame inputs) {
        return EntityCombatPass.resolveCombat(this, inputs);
    }
}
