package linksawakening.entity;

import static linksawakening.entity.EntitySpriteGeometry.*;
import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;

/** Transient entity effects and mixed projectile/object display-list composition. */
final class EntityEffectSprites {
    private final EntitySpriteDecoder decoder;

    EntityEffectSprites(EntitySpriteDecoder decoder) {
        this.decoder = decoder;
    }

    /** The bank-$03 fire pair rendered over entities in {@code BURNING} status. */
    public EntitySpriteDefinition forBurningEntity() {
        return decoder.decodePair(0x00, 0x03, 0x4C44, 2, 0);
    }

    /** EntityFallHandler's three single frames followed by Unknown020's pair. */
    public EntitySpriteDefinition forFallingEntity() {
        EntitySpriteDefinition singles = decoder.decodeSingle(0x00, 0x03, 0x4CAC, 3, 0);
        EntitySpriteDefinition pair = decoder.decodePair(0x00, 0x03, 0x4CB2, 1, 0);
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(4);
        for (EntitySpriteDefinition.Variant variant : singles.variants()) {
            variants.add(List.of(new EntitySpriteDefinition.DynamicSprite(
                    0, 0, variant.first(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true)));
        }
        List<EntitySpriteDefinition.DynamicSprite> finalPair = new ArrayList<>(2);
        appendDynamicPair(finalPair, pair.variant(0), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
        variants.add(List.copyOf(finalPair));
        return EntitySpriteDefinition.dynamic(0x00, 0x03, 0x4CAC, 0, variants);
    }

    /** Decodes the bank-$03 pair shown immediately before bomb detonation. */
    public EntitySpriteDefinition forBombRightBeforeExploding() {
        return decoder.decodePair(ENTITY_BOMB, 0x03, 0x5484, 1, 0);
    }

    /** Decodes the bank-$03 rectangle list used for bomb explosion frames. */
    public EntitySpriteDefinition forBombExplosion() {
        return decoder.decodeRectangle(ENTITY_BOMB, 0x03, 0x6530, 4, 8, 0);
    }

    /** Decodes the bank-$03 {@code Data_003_5488} ordinary death display list. */
    public EntitySpriteDefinition forDeathEntity() {
        return decoder.decodeRectangle(0x00, 0x03, 0x5488, 4, 4, 0);
    }

    /**
     * Decodes the bank-$03 {@code Data_003_54C8} power-recoil death display list.
     * The source's five four-entry groups are exposed as four variants by
     * concatenating groups 3 and 4 into the final eight-entry frame.
     */
    public EntitySpriteDefinition forPowerRecoilDeathEntity() {
        return decodePowerRecoilDeathRectangle();
    }

    /** Decodes Data_006_7AEB, the two-frame rectangle list after the main item sprite. */
    public EntitySpriteDefinition forFloatingItemOverlay() {
        return decoder.decodeRectangle(ENTITY_FLOATING_ITEM, 0x06, 0x7AEB, 2, 2, 0);
    }

    /** The green Zol list selected after Slime Eye has split its Zol. */
    public EntitySpriteDefinition forZolSlimeEye() {
        return decoder.decodePair(0x1B, 0x06, 0x7C11, 2, 0);
    }

    /**
     * Decodes Data_006_7ADD as a mixed pair definition. The bank-$06 handler
     * renders variants $00-$06 as single sprites, except that variant $05 of
     * ENTITY_FLOATING_ITEM_2 deliberately points at Data_006_7AD1 + 2. That
     * source address is inside the preceding Boo Buddy code; preserve its ROM
     * bytes rather than correcting the disassembly's source quirk.
     */
    EntitySpriteDefinition decodeFloatingItemMain(int entityType) {
        EntitySpriteDefinition singles = decoder.decodeSingle(entityType, 0x06, 0x7ADD, 7, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(7);
        for (EntitySpriteDefinition.Variant variant : singles.variants()) {
            variants.add(variant);
        }
        if (entityType == ENTITY_FLOATING_ITEM_2) {
            EntitySpriteDefinition quirk = decoder.decodePair(entityType, 0x06, 0x7AD3, 1, 0);
            variants.set(5, quirk.variant(0));
        }
        return new EntitySpriteDefinition(entityType, 0x06, 0x7ADD, EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Builds the mixed display list emitted by BombArrowHandler. The handler
     * draws the bomb's single sprite first, then the ordinary arrow pair, all
     * at the current arrow position.
     */
    public EntitySpriteDefinition forBombArrow() {
        EntitySpriteDefinition arrow = decoder.decodePair(ENTITY_ARROW, 0x03, 0x6BC6, 4, 0);
        EntitySpriteDefinition bomb = decoder.decodeSingle(ENTITY_BOMB, 0x03, 0x652E, 1, 0);
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
        return decoder.decodePair(ENTITY_MAGIC_ROD_FIREBALL, 0x03, 0x4C44, 2, 0);
    }

    /**
     * Returns the two-entry OAM pair emitted by MagicPowderSprinkle's torch
     * transition. The source selects Data_018_795E while lighting and
     * Data_018_7962 while restoring the unlit torch.
     */
    public EntitySpriteDefinition forMagicPowderTorchState(boolean lighting) {
        return decoder.decodePair(ENTITY_MAGIC_POWDER_SPRINKLE, 0x18, lighting ? 0x795E : 0x7962, 1, 0);
    }

    EntitySpriteDefinition decodeLiftableRock(EntityRoomLoader.RoomTable roomTable) {
        int intactAddress = roomTable == EntityRoomLoader.RoomTable.OVERWORLD ? 0x5398 : 0x53A0;
        EntitySpriteDefinition intact =
                decoder.decodePair(ENTITY_LIFTABLE_ROCK, 0x03, intactAddress, 2, LIFTABLE_ROCK_INTACT_ROCK_VARIANT);

        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(22);
        variants.add(dynamicPair(intact.variant(LIFTABLE_ROCK_INTACT_ROCK_VARIANT)));
        variants.add(dynamicPair(intact.variant(LIFTABLE_ROCK_INTACT_BUSH_VARIANT)));
        appendDynamicRectangleVariants(variants, 0x19, 0x7B10, 4, 4);
        appendDynamicRectangleVariants(variants, 0x19, 0x7B50, 8, 4);
        appendDynamicRectangleVariants(variants, 0x19, 0x7BD0, 8, 4);
        return EntitySpriteDefinition.dynamic(
                ENTITY_LIFTABLE_ROCK, 0x19, 0x7B50, LIFTABLE_ROCK_INTACT_ROCK_VARIANT, variants);
    }

    List<EntitySpriteDefinition.DynamicSprite> dynamicPair(EntitySpriteDefinition.Variant pair) {
        if (pair.second() == null) {
            throw new IllegalArgumentException("Liftable-rock pair must contain two OAM entries");
        }
        return List.of(
                new EntitySpriteDefinition.DynamicSprite(
                        0, 0, pair.first(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true),
                new EntitySpriteDefinition.DynamicSprite(
                        0, 0x08, pair.second(), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS, true));
    }

    void appendDynamicRectangleVariants(
            List<List<EntitySpriteDefinition.DynamicSprite>> destination,
            int bank,
            int address,
            int variantCount,
            int spriteCount) {
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangles =
                decoder.decodeRectangleVariants(ENTITY_LIFTABLE_ROCK, bank, address, variantCount, spriteCount, 0);
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

    EntitySpriteDefinition decodePowerRecoilDeathRectangle() {
        int entityType = 0x00;
        int bank = 0x03;
        int address = 0x54C8;
        List<List<EntitySpriteDefinition.RectangleSprite>> sourceVariants =
                decoder.decodeRectangleVariants(entityType, bank, address, 5, 4, 0);
        List<List<EntitySpriteDefinition.RectangleSprite>> variants = new ArrayList<>(sourceVariants.subList(0, 3));
        List<EntitySpriteDefinition.RectangleSprite> finalFrame = new ArrayList<>(sourceVariants.get(3));
        finalFrame.addAll(sourceVariants.get(4));
        variants.add(List.copyOf(finalFrame));
        return new EntitySpriteDefinition(
                entityType, bank, address, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), variants);
    }

    EntitySpriteDefinition hookshotChain(int entityType) {

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
}
