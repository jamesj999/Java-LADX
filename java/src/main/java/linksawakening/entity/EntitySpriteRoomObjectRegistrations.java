package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** RoomObject display-list selection, including ROM {@code OwlStatueSpriteVariants}, {@code ChestItemSpriteTable}. */
final class EntitySpriteRoomObjectRegistrations {
    /** OwlStatueSpriteVariants at $18:$5DF7. */
    private static final RomAddress OWL_STATUE_SPRITE_VARIANTS = new RomAddress(0x18, 0x5DF7);
    /** ChestItemSpriteTable at $07:$7B57. */
    private static final RomAddress CHEST_ITEM_SPRITE_TABLE = new RomAddress(0x07, 0x7B57);
    /** Unknown011SpriteVariants at $03:$5235. */
    private static final RomAddress UNKNOWN011_SPRITE_VARIANTS = new RomAddress(0x03, 0x5235);
    /** CrystalSwitchSpriteVariants at $15:$4320. */
    private static final RomAddress CRYSTAL_SWITCH_SPRITE_VARIANTS = new RomAddress(0x15, 0x4320);

    private EntitySpriteRoomObjectRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0x42 && mapId != 0x16) {
            // Ordinary owl: hActiveEntityVisualPosY + 1, not a world-position change.
            // South Face Shrine instead uses the separate four-sprite mural handler.
            EntitySpriteDefinition.Variant pair = catalog.decodePair(
                            entityType, OWL_STATUE_SPRITE_VARIANTS.bank(), OWL_STATUE_SPRITE_VARIANTS.address(), 1, 0)
                    .variant(0);
            return EntitySpriteDefinition.dynamic(
                    entityType,
                    OWL_STATUE_SPRITE_VARIANTS.bank(),
                    OWL_STATUE_SPRITE_VARIANTS.address(),
                    0,
                    List.of(List.of(
                            new EntitySpriteDefinition.DynamicSprite(
                                    1,
                                    0,
                                    pair.first(),
                                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                                    true),
                            new EntitySpriteDefinition.DynamicSprite(
                                    1,
                                    8,
                                    pair.second(),
                                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                                    true))));
        }
        if (entityType == ENTITY_CHEST_WITH_ITEM) {
            return catalog.decodeSingle(
                    entityType, CHEST_ITEM_SPRITE_TABLE.bank(), CHEST_ITEM_SPRITE_TABLE.address(), 0x21, 0);
        }
        if (entityType == 0x06) {
            // Unknown011SpriteVariants, used by the transient pushed block.
            return catalog.decodePair(
                    entityType, UNKNOWN011_SPRITE_VARIANTS.bank(), UNKNOWN011_SPRITE_VARIANTS.address(), 2, 1);
        }
        if (entityType == 0x61 && catalog.isIndoorRoomTable(roomTable)) {
            return catalog.decodeDungeonWarp();
        }
        if (entityType == ENTITY_CRYSTAL_SWITCH) {
            return catalog.decodePair(
                    entityType, CRYSTAL_SWITCH_SPRITE_VARIANTS.bank(), CRYSTAL_SWITCH_SPRITE_VARIANTS.address(), 1, 0);
        }
        return null;
    }
}
