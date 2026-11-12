package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** VillageNpc display-list selection, including ROM {@code WitchRatSpriteVariants}, {@code Data_005_4780}. */
final class EntitySpriteVillageNpcRegistrations {
    /** Kid72SpriteVariants from azle.sym. */
    private static final RomAddress KID72_SPRITE_VARIANTS = new RomAddress(0x6, 0x605d);

    /** WitchRatSpriteVariants at $15:$788D. */
    private static final RomAddress WITCH_RAT_SPRITE_VARIANTS = new RomAddress(0x15, 0x788D);
    /** Data_005_4780 at $05:$4780. */
    private static final RomAddress DATA_005_4780 = new RomAddress(0x05, 0x4780);
    /** MarinIndoorSpriteVariants at $05:$4E0A. */
    private static final RomAddress MARIN_INDOOR_SPRITE_VARIANTS = new RomAddress(0x05, 0x4E0A);
    /** MarinCreditsSpriteVariants at $05:$4E2A. */
    private static final RomAddress MARIN_CREDITS_SPRITE_VARIANTS = new RomAddress(0x05, 0x4E2A);
    /** Tarin3SpriteVariants at $05:$4932. */
    private static final RomAddress TARIN3_SPRITE_VARIANTS = new RomAddress(0x05, 0x4932);
    /** Tarin1SpriteVariants at $05:$4912. */
    private static final RomAddress TARIN1_SPRITE_VARIANTS = new RomAddress(0x05, 0x4912);
    /** MarinAtTalTalAndInStoreSpriteVariants at $18:$5EB7. */
    private static final RomAddress MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS = new RomAddress(0x18, 0x5EB7);
    /** Kid71SpriteVariants at $06:$604D. */
    private static final RomAddress KID71_SPRITE_VARIANTS = new RomAddress(0x06, 0x604D);
    /** MadamMeowMeowSpriteVariants at $06:$5B56. */
    private static final RomAddress MADAM_MEOW_MEOW_SPRITE_VARIANTS = new RomAddress(0x06, 0x5B56);
    /** Data_006_5C51 at $06:$5C51. */
    private static final RomAddress DATA_006_5C51 = new RomAddress(0x06, 0x5C51);

    private EntitySpriteVillageNpcRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_WITCH_RAT) {
            return catalog.decodePair(
                    entityType, WITCH_RAT_SPRITE_VARIANTS.bank(), WITCH_RAT_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_WITCH) {
            return catalog.decodeRectangle(entityType, DATA_005_4780.bank(), DATA_005_4780.address(), 4, 4, 0);
        }
        if (entityType == ENTITY_MARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                    ? catalog.decodePair(
                            entityType,
                            MARIN_INDOOR_SPRITE_VARIANTS.bank(),
                            MARIN_INDOOR_SPRITE_VARIANTS.address(),
                            8,
                            6)
                    : catalog.decodePair(
                            entityType,
                            MARIN_CREDITS_SPRITE_VARIANTS.bank(),
                            MARIN_CREDITS_SPRITE_VARIANTS.address(),
                            11,
                            6);
        }
        if (entityType == ENTITY_TARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                            || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                    ? catalog.decodePair(
                            entityType, TARIN3_SPRITE_VARIANTS.bank(), TARIN3_SPRITE_VARIANTS.address(), 4, 0)
                    : catalog.decodePair(
                            entityType, TARIN1_SPRITE_VARIANTS.bank(), TARIN1_SPRITE_VARIANTS.address(), 12, 0);
        }
        if (entityType == ENTITY_MARIN_AT_TAL_TAL_HEIGHTS) {
            return catalog.decodePair(
                    entityType,
                    MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.bank(),
                    MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == ENTITY_KID_70 || entityType == ENTITY_KID_73) {
            return catalog.decodePair(entityType, KID71_SPRITE_VARIANTS.bank(), KID71_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_KID_71 || entityType == ENTITY_KID_72) {
            return catalog.decodePair(
                    entityType,
                    KID71_SPRITE_VARIANTS.bank(),
                    entityType == ENTITY_KID_71 ? KID71_SPRITE_VARIANTS.address() : KID72_SPRITE_VARIANTS.address(),
                    4,
                    0);
        }
        if (entityType == ENTITY_MADAM_MEOWMEOW) {
            return catalog.decodePair(
                    entityType,
                    MADAM_MEOW_MEOW_SPRITE_VARIANTS.bank(),
                    MADAM_MEOW_MEOW_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        if (entityType == 0x75) {
            // GrandmaUlriraEntityHandler $18:4D36/4D58 supplies this pair
            // list before PushLinkOutOfEntity_18 ($18:4D69).
            var variants = linksawakening.rom.RomSymbols.GrandmaUlrira1SpriteVariants;
            return catalog.decodePair(entityType, variants.bank(), variants.address(), 3, 0);
        }
        if (entityType == ENTITY_GRANDPA_ULRIRA) {
            return catalog.decodeRectangle(entityType, DATA_006_5C51.bank(), DATA_006_5C51.address(), 2, 4, 0);
        }
        if (entityType == ENTITY_MARIN_AT_THE_SHORE) {
            return catalog.decodePair(
                    entityType,
                    MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.bank(),
                    MARIN_AT_TAL_TAL_AND_IN_STORE_SPRITE_VARIANTS.address(),
                    8,
                    0);
        }
        return null;
    }
}
