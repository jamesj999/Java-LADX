package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** OverworldCreature display-list selection, including ROM {@code CrowSpriteVariants}, {@code BuzzBlobSpriteVariants}. */
final class EntitySpriteOverworldCreatureRegistrations {
    /** CrowSpriteVariants at $06:$5C89. */
    private static final RomAddress CROW_SPRITE_VARIANTS = new RomAddress(0x06, 0x5C89);
    /** BuzzBlobSpriteVariants at $18:$7729. */
    private static final RomAddress BUZZ_BLOB_SPRITE_VARIANTS = new RomAddress(0x18, 0x7729);
    /** OctorokSpriteVariants at $03:$57FB. */
    private static final RomAddress OCTOROK_SPRITE_VARIANTS = new RomAddress(0x03, 0x57FB);
    /** OctorokRockSpriteVariants at $03:$6A1E. */
    private static final RomAddress OCTOROK_ROCK_SPRITE_VARIANTS = new RomAddress(0x03, 0x6A1E);
    /** TektiteSpriteVariants at $06:$78B7. */
    private static final RomAddress TEKTITE_SPRITE_VARIANTS = new RomAddress(0x06, 0x78B7);
    /** LeeverSpriteVariants at $04:$7EE5. */
    private static final RomAddress LEEVER_SPRITE_VARIANTS = new RomAddress(0x04, 0x7EE5);
    /** WingedOctorokSpriteVariants at $07:$562D. */
    private static final RomAddress WINGED_OCTOROK_SPRITE_VARIANTS = new RomAddress(0x07, 0x562D);
    /** PincerSpriteVariants at $07:$542B. */
    private static final RomAddress PINCER_SPRITE_VARIANTS = new RomAddress(0x07, 0x542B);
    /** PeaHatSpriteVariants at $07:$6701. */
    private static final RomAddress PEA_HAT_SPRITE_VARIANTS = new RomAddress(0x07, 0x6701);

    private EntitySpriteOverworldCreatureRegistrations() {}

    static EntitySpriteRecipe lookup(int entityType, EntityRoomLoader.RoomTable roomTable, int mapId) {
        if (entityType == ENTITY_CROW) {
            return EntitySpriteRecipe.pair(
                    entityType, CROW_SPRITE_VARIANTS.bank(), CROW_SPRITE_VARIANTS.address(), 4, 2);
        }
        if (entityType == ENTITY_BUZZ_BLOB) {
            return EntitySpriteRecipe.pair(
                    entityType, BUZZ_BLOB_SPRITE_VARIANTS.bank(), BUZZ_BLOB_SPRITE_VARIANTS.address(), 9, 0);
        }
        if (entityType == ENTITY_OCTOROK) {
            return EntitySpriteRecipe.pair(
                    entityType, OCTOROK_SPRITE_VARIANTS.bank(), OCTOROK_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_OCTOROK_ROCK) {
            return EntitySpriteRecipe.pair(
                    entityType, OCTOROK_ROCK_SPRITE_VARIANTS.bank(), OCTOROK_ROCK_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_TEKTITE) {
            return EntitySpriteRecipe.pair(
                    entityType, TEKTITE_SPRITE_VARIANTS.bank(), TEKTITE_SPRITE_VARIANTS.address(), 2, 0);
        }
        if (entityType == ENTITY_LEEVER) {
            return EntitySpriteRecipe.pair(
                    entityType, LEEVER_SPRITE_VARIANTS.bank(), LEEVER_SPRITE_VARIANTS.address(), 4, 0);
        }
        if (entityType == ENTITY_WINGED_OCTOROK) {
            return EntitySpriteRecipe.pair(
                    entityType, WINGED_OCTOROK_SPRITE_VARIANTS.bank(), WINGED_OCTOROK_SPRITE_VARIANTS.address(), 8, 0);
        }
        if (entityType == ENTITY_PINCER) {
            return EntitySpriteRecipe.pair(
                    entityType, PINCER_SPRITE_VARIANTS.bank(), PINCER_SPRITE_VARIANTS.address(), 10, 0);
        }
        if (entityType == ENTITY_PEAHAT) {
            return EntitySpriteRecipe.pair(
                    entityType, PEA_HAT_SPRITE_VARIANTS.bank(), PEA_HAT_SPRITE_VARIANTS.address(), 2, 0);
        }
        return null;
    }
}
