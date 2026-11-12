package linksawakening.world.entity;
/** Descending allocation retains slot identity and the ROM hitbox reset at reuse. */
final class EntitySlotAllocator {
    private EntitySlotAllocator() {}

    static int findFreeSlot(RoomEntity[] slots, EntityCombatTables combat, int highestSlot) {
        int first = Math.min(highestSlot, slots.length - 1);
        for (int slot = first; slot >= 0; slot--) {
            if (!slots[slot].loaded()) {
                // Dynamic spawns can reuse a slot without passing through clearEntity.
                combat.writeEnemyHitboxFlags(slot, 0);
                return slot;
            }
        }
        return -1;
    }
}
