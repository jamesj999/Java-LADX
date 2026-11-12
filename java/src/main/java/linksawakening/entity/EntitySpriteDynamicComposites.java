package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.rom.Rom;

/** EntitySpriteDynamicComposites behavior on the shared EntitySpriteHandlerCatalog state. */
abstract class EntitySpriteDynamicComposites extends EntitySpriteBossComposites {
    protected EntitySpriteDynamicComposites(Rom romData) {
        super(romData);
    }

    /** Decodes the bank-$03 pair shown immediately before bomb detonation. */
    public EntitySpriteDefinition forBombRightBeforeExploding() {
        return decodePair(ENTITY_BOMB, 0x03, 0x5484, 1, 0);
    }

    /** Decodes the bank-$03 rectangle list used for bomb explosion frames. */
    public EntitySpriteDefinition forBombExplosion() {
        return decodeRectangle(ENTITY_BOMB, 0x03, 0x6530, 4, 8, 0);
    }

    /** Decodes the bank-$03 {@code Data_003_5488} ordinary death display list. */
    public EntitySpriteDefinition forDeathEntity() {
        return decodeRectangle(0x00, 0x03, 0x5488, 4, 4, 0);
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
        return decodeRectangle(ENTITY_FLOATING_ITEM, 0x06, 0x7AEB, 2, 2, 0);
    }

    /** The green Zol list selected after Slime Eye has split its Zol. */
    public EntitySpriteDefinition forZolSlimeEye() {
        return decodePair(0x1B, 0x06, 0x7C11, 2, 0);
    }

    /**
     * Decodes Hiding Zol's mixed display path. The bank-$07 handler selects
     * the single-sprite list when the entity variant is $01 and the pair list
     * for variants $00, $02, and $03. A null second OAM entry represents that
     * single-sprite branch without inventing a second tile.
     */
    protected EntitySpriteDefinition decodeHidingZol(int entityType) {
        EntitySpriteDefinition pair = decodePair(entityType, 0x07, 0x729B, 4, 0);
        EntitySpriteDefinition single = decodeSingle(entityType, 0x07, 0x72AB, 1, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(pair.variants());
        variants.set(1, new EntitySpriteDefinition.Variant(single.variant(0).first(), null));
        return new EntitySpriteDefinition(entityType, 0x07, 0x729B, EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Decodes FishEntityHandler's five pair variants followed by its four
     * single-sprite jump variants. The handler selects the latter by
     * subtracting five from the active variant before rendering.
     */
    protected EntitySpriteDefinition decodeFish(int entityType) {
        EntitySpriteDefinition pair = decodePair(entityType, 0x15, 0x449F, 5, 0);
        EntitySpriteDefinition single = decodeSingle(entityType, 0x15, 0x44B3, 4, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(pair.variants());
        for (int variant = 0; variant < 4; variant++) {
            variants.add(single.variant(variant));
        }
        return new EntitySpriteDefinition(entityType, 0x15, 0x449F, EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Decodes bank-$07's Moblin Sword handler presentation. The handler does
     * not point at one ordinary display list: it emits a shared-tile warning,
     * a two-entry bank-$20-generated list, and the inline sword pair in
     * variant-dependent order.
     */
    protected EntitySpriteDefinition decodeMoblinSword(int entityType) {
        EntitySpriteDefinition body = decodePair(entityType, 0x07, 0x7A95, 8, 0);
        int[] dynamicY = {0x08, 0x0E, 0xF8, 0xF2, 0x00, 0x00, 0x00, 0x00};
        int[] dynamicX = {0x00, 0x00, 0xF9, 0xF9, 0xF8, 0xF2, 0x08, 0x0E};
        int[] tileSelector = {0x02, 0x02, 0x06, 0x06, 0x04, 0x04, 0x00, 0x00};
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(8);

        for (int variant = 0; variant < 8; variant++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(5);
            if (variant < 2) {
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                        (variant ^ 0x01) + 0x04,
                        -0x02,
                        new EntitySpriteDefinition.OamAttribute(0x86, 0x16),
                        EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
                        false));
            }
            if (variant == 2 || variant == 3) {
                appendMoblinSwordBody(sprites, body.variant(variant));
            }

            appendMoblinSwordGeneratedPair(sprites, dynamicY[variant], dynamicX[variant], tileSelector[variant]);

            if (variant != 2 && variant != 3) {
                appendMoblinSwordBody(sprites, body.variant(variant));
            }
            variants.add(List.copyOf(sprites));
        }

        return EntitySpriteDefinition.dynamic(entityType, 0x07, 0x7A95, 0, variants);
    }

    protected void appendMoblinSwordGeneratedPair(
            List<EntitySpriteDefinition.DynamicSprite> sprites, int yOffset, int xOffset, int tileSelector) {
        int tileAddress = 0x4A93 + tileSelector * 2;
        int attributeAddress = 0x4AA3 + tileSelector * 2;
        int firstTile = readByte(0x20, tileAddress);
        int secondTile = readByte(0x20, tileAddress + 1);
        int firstAttributes = readByte(0x20, attributeAddress);
        int secondAttributes = readByte(0x20, attributeAddress + 1);
        EntitySpriteDefinition.DynamicSprite.TileSource source = EntitySpriteDefinition.DynamicSprite.TileSource.GPU;
        // $20:4ACF rewinds BC to the OAM X byte and writes $F0 there,
        // hiding this sprite offscreen. It does not replace its tile number.
        if (firstTile != 0xFF)
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    signedByte(yOffset),
                    signedByte(xOffset),
                    new EntitySpriteDefinition.OamAttribute(firstTile, firstAttributes),
                    source,
                    false));
        sprites.add(new EntitySpriteDefinition.DynamicSprite(
                signedByte(yOffset),
                signedByte(xOffset + 0x08),
                new EntitySpriteDefinition.OamAttribute(secondTile, secondAttributes),
                source,
                false));
    }

    /**
     * Decodes Data_006_7ADD as a mixed pair definition. The bank-$06 handler
     * renders variants $00-$06 as single sprites, except that variant $05 of
     * ENTITY_FLOATING_ITEM_2 deliberately points at Data_006_7AD1 + 2. That
     * source address is inside the preceding Boo Buddy code; preserve its ROM
     * bytes rather than correcting the disassembly's source quirk.
     */
    protected EntitySpriteDefinition decodeFloatingItemMain(int entityType) {
        EntitySpriteDefinition singles = decodeSingle(entityType, 0x06, 0x7ADD, 7, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(7);
        for (EntitySpriteDefinition.Variant variant : singles.variants()) {
            variants.add(variant);
        }
        if (entityType == ENTITY_FLOATING_ITEM_2) {
            EntitySpriteDefinition quirk = decodePair(entityType, 0x06, 0x7AD3, 1, 0);
            variants.set(5, quirk.variant(0));
        }
        return new EntitySpriteDefinition(entityType, 0x06, 0x7ADD, EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Returns the display list selected by CreateFollowingNpcEntity handlers.
     * The caller supplies the matching follower entity type in the same form
     * used by the disassembly's dynamic entity spawner.
     */
    public EntitySpriteDefinition forFollowerEntityType(int entityType) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        return switch (entityType) {
            case ENTITY_BOW_WOW -> decodePair(entityType, 0x05, 0x401C, 7, 0);
            case ENTITY_MARIN_AT_THE_SHORE -> decodePair(entityType, 0x18, 0x59B8, 11, 0);
            case 0xD4 -> decodePair(entityType, 0x19, 0x5DF8, 6, 0);
            case 0xD5 -> decodePair(entityType, 0x19, 0x59BC, 8, 0);
            default -> EntitySpriteDefinition.unsupported(entityType);
        };
    }
}
