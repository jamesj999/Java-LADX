package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** SideView display-list selection, including ROM {@code GoombaSpriteVariants}. */
final class EntitySpriteSideViewRegistrations {
    /** GoombaSpriteVariants at $07:$65CE. */
    private static final RomAddress GOOMBA_SPRITE_VARIANTS = new RomAddress(0x07, 0x65CE);

    private EntitySpriteSideViewRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_GOOMBA) {
            return catalog.decodePair(
                    entityType, GOOMBA_SPRITE_VARIANTS.bank(), GOOMBA_SPRITE_VARIANTS.address(), 3, 0);
        }
        return null;
    }
}
