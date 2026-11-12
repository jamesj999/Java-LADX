package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.world.entity.EntityRoomLoader;

/** EntitySpriteRomDecoding behavior on the shared EntitySpriteHandlerCatalog state. */
abstract class EntitySpriteRomDecoding extends EntitySpriteDynamicComposites {
    protected EntitySpriteRomDecoding(Rom romData) {
        super(romData);
    }

    /** Builds func_015_4AC9's body, optional weapon, and airborne shadow layers. */
    public EntitySpriteDefinition forMoblinKingState(int bodyVariant, int weaponVariant, boolean airborne) {
        if (bodyVariant < 0 || bodyVariant >= 14) {
            throw new IllegalArgumentException("Moblin King body variant must be 0..13");
        }
        if (weaponVariant < -1 || weaponVariant >= 4) {
            throw new IllegalArgumentException("Moblin King weapon variant must be -1..3");
        }
        EntitySpriteDefinition body = decodeRectangle(0xE4, 0x15, 0x4A15, 14, 3, 0);
        EntitySpriteDefinition weapon = decodeRectangle(0xE4, 0x15, 0x49E5, 4, 3, 0);
        EntitySpriteDefinition shadow = decodeRectangle(0xE4, 0x15, 0x4ABD, 1, 3, 0);
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(9);
        if (weaponVariant >= 0) {
            appendRectangleSprites(sprites, weapon.rectangleVariant(weaponVariant));
        }
        // func_015_4AC9 allocates weapon OAM first, so it wins overlaps with the body.
        appendRectangleSprites(sprites, body.rectangleVariant(bodyVariant));
        if (airborne) {
            appendRectangleSprites(sprites, shadow.rectangleVariant(0));
        }
        return EntitySpriteDefinition.dynamic(0xE4, 0x15, 0x4A15, 0, List.of(List.copyOf(sprites)));
    }

    /**
     * Builds the mixed display list emitted by BombArrowHandler. The handler
     * draws the bomb's single sprite first, then the ordinary arrow pair, all
     * at the current arrow position.
     */
    public EntitySpriteDefinition forBombArrow() {
        EntitySpriteDefinition arrow = decodePair(ENTITY_ARROW, 0x03, 0x6BC6, 4, 0);
        EntitySpriteDefinition bomb = decodeSingle(ENTITY_BOMB, 0x03, 0x652E, 1, 0);
        int[] bombXOffsets = {0x04, -0x04, 0x00, 0x00};
        int[] bombYOffsets = {-0x02, -0x02, -0x06, 0x04};
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(4);
        for (int direction = 0; direction < 4; direction++) {
            EntitySpriteDefinition.Variant arrowVariant = arrow.variant(direction);
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(3);
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    bombYOffsets[direction],
                    bombXOffsets[direction] + 0x04,
                    bomb.variant(0).first(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    false));
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    0, 0, arrowVariant.first(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, false));
            if (arrowVariant.second() != null) {
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                        0,
                        0x08,
                        arrowVariant.second(),
                        EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                        false));
            }
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(ENTITY_ARROW, 0x03, 0x6BC6, 0, variants);
    }

    /** Shared bank-$03 fire display used after a Magic Rod fireball hits a wall. */
    public EntitySpriteDefinition forMagicRodFireState() {
        return decodePair(ENTITY_MAGIC_ROD_FIREBALL, 0x03, 0x4C44, 2, 0);
    }

    /**
     * Returns the two-entry OAM pair emitted by MagicPowderSprinkle's torch
     * transition. The source selects Data_018_795E while lighting and
     * Data_018_7962 while restoring the unlit torch.
     */
    public EntitySpriteDefinition forMagicPowderTorchState(boolean lighting) {
        return decodePair(ENTITY_MAGIC_POWDER_SPRINKLE, 0x18, lighting ? 0x795E : 0x7962, 1, 0);
    }

    protected EntitySpriteDefinition decodeLiftableRock(EntityRoomLoader.RoomTable roomTable) {
        int intactAddress = roomTable == EntityRoomLoader.RoomTable.OVERWORLD ? 0x5398 : 0x53A0;
        EntitySpriteDefinition intact =
                decodePair(ENTITY_LIFTABLE_ROCK, 0x03, intactAddress, 2, LIFTABLE_ROCK_INTACT_ROCK_VARIANT);

        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(22);
        variants.add(dynamicPair(intact.variant(LIFTABLE_ROCK_INTACT_ROCK_VARIANT)));
        variants.add(dynamicPair(intact.variant(LIFTABLE_ROCK_INTACT_BUSH_VARIANT)));
        appendDynamicRectangleVariants(variants, 0x19, 0x7B10, 4, 4);
        appendDynamicRectangleVariants(variants, 0x19, 0x7B50, 8, 4);
        appendDynamicRectangleVariants(variants, 0x19, 0x7BD0, 8, 4);
        return EntitySpriteDefinition.dynamic(
                ENTITY_LIFTABLE_ROCK, 0x19, 0x7B50, LIFTABLE_ROCK_INTACT_ROCK_VARIANT, variants);
    }

    protected List<EntitySpriteDefinition.DynamicSprite> dynamicPair(EntitySpriteDefinition.Variant pair) {
        if (pair.second() == null) {
            throw new IllegalArgumentException("Liftable-rock pair must contain two OAM entries");
        }
        return List.of(
                new EntitySpriteDefinition.DynamicSprite(
                        0, 0, pair.first(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true),
                new EntitySpriteDefinition.DynamicSprite(
                        0, 0x08, pair.second(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
    }

    protected void appendDynamicRectangleVariants(
            List<List<EntitySpriteDefinition.DynamicSprite>> destination,
            int bank,
            int address,
            int variantCount,
            int spriteCount) {
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangles =
                decodeRectangleVariants(ENTITY_LIFTABLE_ROCK, bank, address, variantCount, spriteCount, 0);
        for (List<EntitySpriteDefinition.RectangleSprite> rectangle : rectangles) {
            List<EntitySpriteDefinition.DynamicSprite> dynamic = new ArrayList<>(rectangle.size());
            for (EntitySpriteDefinition.RectangleSprite sprite : rectangle) {
                dynamic.add(new EntitySpriteDefinition.DynamicSprite(
                        sprite.yOffset(),
                        sprite.xOffset(),
                        sprite.oam(),
                        EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                        false));
            }
            destination.add(List.copyOf(dynamic));
        }
    }

    public EntitySpriteDefinition decodePair(
            int entityType, int bank, int address, int variantCount, int initialVariant) {
        return decodePair(
                entityType,
                bank,
                address,
                variantCount,
                initialVariant,
                EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
    }

    protected EntitySpriteDefinition decodePair(
            int entityType,
            int bank,
            int address,
            int variantCount,
            int initialVariant,
            EntitySpriteDefinition.DynamicSprite.TileSource tileSource) {
        int offset = validateDisplayList(entityType, bank, address, variantCount, 4, initialVariant);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            int index = offset + variant * 4;
            variants.add(new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(
                            romData.u8(RomAddress.fromOffset(index)), romData.u8(RomAddress.fromOffset(index + 1))),
                    new EntitySpriteDefinition.OamAttribute(
                            romData.u8(RomAddress.fromOffset(index + 2)),
                            romData.u8(RomAddress.fromOffset(index + 3)))));
        }
        return new EntitySpriteDefinition(
                entityType, bank, address, EntitySpriteDefinition.Shape.PAIR, initialVariant, variants, tileSource);
    }

    public EntitySpriteDefinition decodeSingle(
            int entityType, int bank, int address, int variantCount, int initialVariant) {
        int offset = validateDisplayList(entityType, bank, address, variantCount, 2, initialVariant);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            int index = offset + variant * 2;
            variants.add(new EntitySpriteDefinition.Variant(
                    new EntitySpriteDefinition.OamAttribute(
                            romData.u8(RomAddress.fromOffset(index)), romData.u8(RomAddress.fromOffset(index + 1))),
                    null));
        }
        return new EntitySpriteDefinition(
                entityType, bank, address, EntitySpriteDefinition.Shape.SINGLE, initialVariant, variants);
    }

    public EntitySpriteDefinition decodeRectangle(
            int entityType, int bank, int address, int variantCount, int spriteCount, int initialVariant) {
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangleVariants =
                decodeRectangleVariants(entityType, bank, address, variantCount, spriteCount, initialVariant);
        return new EntitySpriteDefinition(
                entityType,
                bank,
                address,
                EntitySpriteDefinition.Shape.RECTANGLE,
                initialVariant,
                List.of(),
                rectangleVariants);
    }
}
