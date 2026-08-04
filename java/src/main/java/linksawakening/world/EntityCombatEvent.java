package linksawakening.world;

/** A single room-entity combat result produced by the ROM collision pass. */
public record EntityCombatEvent(int slot, int type, int linkDamage, boolean swordHit) {

    public EntityCombatEvent {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        if ((type & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + type);
        }
        if (linkDamage < 0) {
            throw new IllegalArgumentException("Link damage cannot be negative: " + linkDamage);
        }
    }
}
