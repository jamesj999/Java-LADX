package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Boss display-list selection, including ROM {@code Data_006_6E1E}, {@code ThreeOfAKindSpriteVariants}. */
final class EntitySpriteBossRegistrations {
    private static final int MAP_TURTLE_ROCK = 0x07;
    private static final int MAP_EAGLES_TOWER = 0x06;
    /** Data_006_527A, Data_006_523A and Data_006_51FA select the dungeon Hinox palette lists. */
    private static final RomAddress TURTLE_ROCK_HINOX = new RomAddress(0x06, 0x527A);

    private static final RomAddress EAGLES_TOWER_HINOX = new RomAddress(0x06, 0x523A);
    private static final RomAddress DEFAULT_HINOX = new RomAddress(0x06, 0x51FA);

    /** Data_006_6E1E at $06:$6E1E. */
    private static final RomAddress DATA_006_6E1E = new RomAddress(0x06, 0x6E1E);
    /** ThreeOfAKindSpriteVariants at $06:$4911. */
    private static final RomAddress THREE_OF_AKIND_SPRITE_VARIANTS = new RomAddress(0x06, 0x4911);
    /** Data_006_5523 at $06:$5523. */
    private static final RomAddress DATA_006_5523 = new RomAddress(0x06, 0x5523);

    private EntitySpriteBossRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == 0x5B) return catalog.forSlimeEyeState(false, 0, 0);
        if (entityType == ENTITY_MOLDORM) {
            return catalog.forMoldormState(
                    0,
                    0,
                    0,
                    List.of(
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0),
                            new MoldormSegmentPosition(0, 0)),
                    0,
                    0);
        }
        if (entityType == ENTITY_ROLLING_BONES) {
            return catalog.decodeRectangle(entityType, DATA_006_6E1E.bank(), DATA_006_6E1E.address(), 6, 4, 0);
        }
        if (entityType == ENTITY_ROLLING_BONES_BAR) {
            return catalog.forRollingBonesBar(0, 0x38);
        }
        if (entityType == ENTITY_THREE_OF_A_KIND) {
            return catalog.decodePair(
                    entityType, THREE_OF_AKIND_SPRITE_VARIANTS.bank(), THREE_OF_AKIND_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_ARMOS_KNIGHT) {
            return catalog.decodeRectangle(entityType, DATA_006_5523.bank(), DATA_006_5523.address(), 4, 8, 0);
        }
        if (entityType == ENTITY_HINOX) {
            int address = mapId == MAP_TURTLE_ROCK
                    ? TURTLE_ROCK_HINOX.address()
                    : mapId == MAP_EAGLES_TOWER ? EAGLES_TOWER_HINOX.address() : DEFAULT_HINOX.address();
            return catalog.decodeRectangle(entityType, 0x06, address, 4, 8, 0);
        }
        return null;
    }
}
