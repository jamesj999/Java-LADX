package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** Aquatic display-list selection, including ROM {@code ZoraSpriteVariants}, {@code SandCrabSpriteVariants}. */
final class EntitySpriteAquaticRegistrations {
    /** ZoraSpriteVariants at $18:$49C0. */
    private static final RomAddress ZORA_SPRITE_VARIANTS = new RomAddress(0x18, 0x49C0);
    /** SandCrabSpriteVariants at $15:$7320. */
    private static final RomAddress SAND_CRAB_SPRITE_VARIANTS = new RomAddress(0x15, 0x7320);
    /** BlooperSpriteVariants at $07:$5BF1. */
    private static final RomAddress BLOOPER_SPRITE_VARIANTS = new RomAddress(0x07, 0x5BF1);
    /** WaterTektiteSpriteVariants at $07:$752D. */
    private static final RomAddress WATER_TEKTITE_SPRITE_VARIANTS = new RomAddress(0x07, 0x752D);

    private EntitySpriteAquaticRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ZORA) {
            return catalog.decodePair(entityType, ZORA_SPRITE_VARIANTS.bank(), ZORA_SPRITE_VARIANTS.address(), 5, 0);
        }
        if (entityType == ENTITY_SAND_CRAB) {
            return catalog.decodePair(
                    entityType, SAND_CRAB_SPRITE_VARIANTS.bank(), SAND_CRAB_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_URCHIN) {
            return catalog.forUrchinState(false);
        }
        if (entityType == ENTITY_BLOOPER) {
            return catalog.decodePair(
                    entityType, BLOOPER_SPRITE_VARIANTS.bank(), BLOOPER_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_WATER_TEKTITE) {
            return catalog.decodePair(
                    entityType, WATER_TEKTITE_SPRITE_VARIANTS.bank(), WATER_TEKTITE_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_FISH) {
            return catalog.decodeFish(entityType);
        }
        return null;
    }
}
