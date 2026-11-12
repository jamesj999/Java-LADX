package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** Plant display-list selection, including ROM {@code GopongaFlowerSpriteVariants}, {@code Data_006_6316}. */
final class EntitySpritePlantRegistrations {
    /** BushcrawlerIndoorSpriteVariants from azle.sym. */
    private static final RomAddress BUSHCRAWLER_INDOOR_SPRITE_VARIANTS = new RomAddress(0x7, 0x401a);
    /** BushcrawlerOutdoorSpriteVariants from azle.sym. */
    private static final RomAddress BUSHCRAWLER_OUTDOOR_SPRITE_VARIANTS = new RomAddress(0x7, 0x4012);

    /** GopongaFlowerSpriteVariants at $06:$63F4. */
    private static final RomAddress GOPONGA_FLOWER_SPRITE_VARIANTS = new RomAddress(0x06, 0x63F4);
    /** Data_006_6316 at $06:$6316. */
    private static final RomAddress DATA_006_6316 = new RomAddress(0x06, 0x6316);
    /** GopongaFlowerProjectileSpriteList at $06:$638F. */
    private static final RomAddress GOPONGA_FLOWER_PROJECTILE_SPRITE_LIST = new RomAddress(0x06, 0x638F);
    /** Data_036_6FC2 at $36:$6FC2. */
    private static final RomAddress DATA_036_6FC2 = new RomAddress(0x36, 0x6FC2);

    private EntitySpritePlantRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_GOPONGA_FLOWER) {
            return EntitySpriteRecipe.pair(
                    entityType, GOPONGA_FLOWER_SPRITE_VARIANTS.bank(), GOPONGA_FLOWER_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_GIANT_GOPONGA_FLOWER) {
            return EntitySpriteRecipe.rectangle(entityType, DATA_006_6316.bank(), DATA_006_6316.address(), 3, 8, 0);
        }
        if (entityType == ENTITY_GOPONGA_FLOWER_PROJECTILE) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    GOPONGA_FLOWER_PROJECTILE_SPRITE_LIST.bank(),
                    GOPONGA_FLOWER_PROJECTILE_SPRITE_LIST.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_POKEY) {
            return EntitySpriteRecipe.special(EntitySpriteRecipe.Kind.POKEY, entityType);
        }
        if (entityType == ENTITY_PIRANHA_PLANT) {
            return EntitySpriteRecipe.rectangle(entityType, DATA_036_6FC2.bank(), DATA_036_6FC2.address(), 6, 4, 0);
        }
        if (entityType == ENTITY_BUSH_CRAWLER) {
            return EntitySpriteRecipe.pair(
                    entityType,
                    BUSHCRAWLER_INDOOR_SPRITE_VARIANTS.bank(),
                    EntitySpriteGeometry.isIndoorRoomTable(roomTable)
                            ? BUSHCRAWLER_INDOOR_SPRITE_VARIANTS.address()
                            : BUSHCRAWLER_OUTDOOR_SPRITE_VARIANTS.address(),
                    2,
                    0,
                    EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
        }
        return null;
    }
}
