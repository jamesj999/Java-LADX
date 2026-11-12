package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.List;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;
import linksawakening.world.entity.EntityRoomLoader;

/** PlayerProjectile display-list selection, including ROM {@code EntityArrowSpriteVariants}, {@code BoomerangSpriteVariants}. */
final class EntitySpritePlayerProjectileRegistrations {
    /** EntityArrowSpriteVariants at $03:$6BC6. */
    private static final RomAddress ENTITY_ARROW_SPRITE_VARIANTS = new RomAddress(0x03, 0x6BC6);
    /** BoomerangSpriteVariants at $19:$4451. */
    private static final RomAddress BOOMERANG_SPRITE_VARIANTS = new RomAddress(0x19, 0x4451);
    /** MagicRodFireballSpriteVariants at $03:$69AA. */
    private static final RomAddress MAGIC_ROD_FIREBALL_SPRITE_VARIANTS = new RomAddress(0x03, 0x69AA);
    /** Data_018_7ABA at $18:$7ABA. */
    private static final RomAddress DATA_018_7ABA = new RomAddress(0x18, 0x7ABA);
    /** MusicalNoteEntityHandler at $05:$7EF8. */
    private static final RomAddress MUSICAL_NOTE_ENTITY_HANDLER = new RomAddress(0x05, 0x7EF8);
    /** Data_019_44FC at $19:$44FC. */
    private static final RomAddress DATA_019_44FC = new RomAddress(0x19, 0x44FC);

    private EntitySpritePlayerProjectileRegistrations() {}

    static EntitySpriteDefinition lookup(
            EntitySpriteDecodeState catalog, int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_ARROW) {
            return catalog.decodePair(
                    entityType, ENTITY_ARROW_SPRITE_VARIANTS.bank(), ENTITY_ARROW_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_BOOMERANG) {
            return catalog.decodePair(
                    entityType, BOOMERANG_SPRITE_VARIANTS.bank(), BOOMERANG_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_MAGIC_ROD_FIREBALL) {
            return catalog.decodePair(
                    entityType,
                    MAGIC_ROD_FIREBALL_SPRITE_VARIANTS.bank(),
                    MAGIC_ROD_FIREBALL_SPRITE_VARIANTS.address(),
                    2,
                    0);
        }
        if (entityType == ENTITY_MAGIC_POWDER_SPRINKLE) {
            return catalog.decodeRectangle(entityType, DATA_018_7ABA.bank(), DATA_018_7ABA.address(), 8, 3, 0);
        }
        if (entityType == ENTITY_MUSICAL_NOTE) {
            return catalog.decodeSingle(
                    entityType, MUSICAL_NOTE_ENTITY_HANDLER.bank(), MUSICAL_NOTE_ENTITY_HANDLER.address(), 1, 0);
        }
        if (entityType == ENTITY_SWORD_BEAM) {
            return catalog.decodeRectangle(entityType, DATA_019_44FC.bank(), DATA_019_44FC.address(), 4, 2, 0);
        }
        if (entityType == ENTITY_BOMB) {
            return catalog.decodeSingle(
                    entityType, RomSymbols.BombSprite.bank(), RomSymbols.BombSprite.address(), 1, 0);
        }
        if (entityType == ENTITY_HOOKSHOT_CHAIN) {
            // HookshotChainSpriteVariants is an inline handler list in
            // entities/18_hookshot_chain.asm, not a bank display-list label.
            return new EntitySpriteDefinition(
                    entityType,
                    -1,
                    -1,
                    EntitySpriteDefinition.Shape.PAIR,
                    0,
                    List.of(new EntitySpriteDefinition.Variant(
                            new EntitySpriteDefinition.OamAttribute(0x36, 0x00),
                            new EntitySpriteDefinition.OamAttribute(0x36, 0x20))));
        }
        return null;
    }
}
