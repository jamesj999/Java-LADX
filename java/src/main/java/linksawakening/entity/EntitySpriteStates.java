package linksawakening.entity;

import static linksawakening.entity.EntitySpriteGeometry.*;
import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;

/** Entity state and room-dependent sprite selection with immutable registration data. */
final class EntitySpriteStates {
    private final EntitySpriteDecoder decoder;
    private final EntitySpriteRegistrationData registrations;

    EntitySpriteStates(EntitySpriteDecoder decoder, EntitySpriteRegistrationData registrations) {
        this.decoder = decoder;
        this.registrations = registrations;
    }

    private static final int[] COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES = {0x6688, 0x66E8, 0x6748};
    private static final int[] COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES = {0x67A8, 0x67D8, 0x6808};

    /** Selects the runtime-dependent display list used by entity $31. */
    public EntitySpriteDefinition forSwordShieldPickup(boolean beachSword) {
        return decoder.decodeSingle(ENTITY_SWORD_SHIELD_PICKUP, 0x03, beachSword ? 0x5B97 : 0x5B95, 1, 0);
    }

    /** Selects SirensInstrumentState1Handler's two projectile variants. */
    public EntitySpriteDefinition forKidPlaying() {
        return decoder.decodePair(0x71, 0x06, 0x606D, 4, 0);
    }

    public EntitySpriteDefinition forKidBall() {
        return decoder.decodeSingle(0x71, 0x06, 0x607D, 1, 0);
    }

    public EntitySpriteDefinition forInstrumentShard() {
        return decoder.decodePair(ENTITY_INSTRUMENT_OF_THE_SIRENS, 0x03, 0x5E8B, 2, 0);
    }

    /** Selects the perched pair or four-sprite flying layout used by the owl event. */
    public EntitySpriteDefinition forOwlEvent(boolean flying) {
        return flying
                ? decoder.decodeRectangle(0x41, 0x06, 0x6A3F, 1, 4, 0)
                : decoder.decodePair(0x41, 0x06, 0x6A37, 2, 0);
    }

    /**
     * Returns the chest display list selected by the source's two GBC room
     * exceptions. The alternate list contains two single-sprite entries;
     * every other room uses the normal 33-item list.
     */
    public EntitySpriteDefinition forChestState(int mapId, int roomId, int itemVariant) {
        if ((mapId & ~0xFF) != 0 || (roomId & ~0xFF) != 0) {
            throw new IllegalArgumentException("Chest map and room ids must be unsigned bytes");
        }
        if (itemVariant < 0 || itemVariant > 0x20) {
            throw new IllegalArgumentException("Chest item variant must be in range 0..0x20: " + itemVariant);
        }
        if ((mapId == 0x05 && roomId == 0xCE && itemVariant == 0x00)
                || (mapId == 0x06 && roomId == 0x1A && itemVariant == 0x01)) {
            return decoder.decodeSingle(ENTITY_CHEST_WITH_ITEM, 0x07, 0x7B53, 2, 0);
        }
        return decoder.decodeSingle(ENTITY_CHEST_WITH_ITEM, 0x07, 0x7B57, 0x21, 0);
    }

    /** Selects Pokey's bank-$15 body list for its current detached-segment count. */
    public EntitySpriteDefinition forPokeyState(int inertia) {
        if (inertia < 0) {
            throw new IllegalArgumentException("Pokey inertia cannot be negative: " + inertia);
        }
        if (inertia == 0) {
            return decoder.decodeRectangle(ENTITY_POKEY, 0x15, 0x4B17, 4, 6, 0);
        }
        if (inertia == 1) {
            return decoder.decodeRectangle(ENTITY_POKEY, 0x15, 0x4B77, 4, 4, 0);
        }
        return decoder.decodeRectangle(ENTITY_POKEY, 0x15, 0x4BB7, 4, 2, 0);
    }

    /** Selects Urchin's normal or credits bank-$15 display-list pair. */
    public EntitySpriteDefinition forUrchinState(boolean creditsGameplay) {
        return decoder.decodePair(ENTITY_URCHIN, 0x15, creditsGameplay ? 0x7393 : 0x7383, 4, 0);
    }

    /** Decodes Unknown044SpriteVariants at bank-$15:$4CD5 for detached segments. */
    public EntitySpriteDefinition forPokeySegment() {
        return decoder.decodePair(ENTITY_POKEY, 0x15, 0x4CD5, 1, 0);
    }

    /** Selects the two bank-$15 display-list pairs used by Stalfos Evasive. */
    public EntitySpriteDefinition forStalfosEvasiveState(int privateState1) {
        if (privateState1 < 0) {
            throw new IllegalArgumentException("Stalfos Evasive private state cannot be negative");
        }
        return privateState1 == 0
                ? decoder.decodePair(ENTITY_STALFOS_EVASIVE, 0x15, 0x4E7D, 3, 0)
                : decoder.decodePair(ENTITY_STALFOS_EVASIVE, 0x15, 0x4E8E, 2, 0);
    }

    /**
     * Selects Bush Crawler's bank-$07 display list for the lifted replacement
     * state. State $02 is the adjacent Bushcrawler2 list; state $01 is the
     * ordinary outdoor/indoor pair selected by the room table.
     */
    public EntitySpriteDefinition forBushCrawlerState(int privateState1, EntityRoomLoader.RoomTable roomTable) {
        if (privateState1 < 0 || privateState1 > 0x02) {
            throw new IllegalArgumentException("Bush Crawler private state out of range: " + privateState1);
        }
        if (privateState1 == 0x02) {
            return decoder.decodePair(ENTITY_BUSH_CRAWLER, 0x07, 0x400A, 2, 0);
        }
        return decoder.decode(registrations.lookup(ENTITY_BUSH_CRAWLER, roomTable, -1));
    }

    /**
     * Builds the two-pass display list used while a normal Bush Crawler is
     * crawling. The handler first renders its room-specific pair, then the
     * four-tile Bushcrawler2 pair at the same active position.
     */
    public EntitySpriteDefinition forBushCrawlerCrawlState(EntityRoomLoader.RoomTable roomTable, int privateState4) {
        if (privateState4 < 0 || privateState4 > 1) {
            throw new IllegalArgumentException("Bush Crawler private state 4 must be 0 or 1: " + privateState4);
        }
        EntitySpriteDefinition normal = decoder.decode(registrations.lookup(ENTITY_BUSH_CRAWLER, roomTable, -1));
        EntitySpriteDefinition crawling = decoder.decodePair(ENTITY_BUSH_CRAWLER, 0x07, 0x400A, 2, 0);
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(2);
        for (int animation = 0; animation < 2; animation++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(4);
            appendDynamicPair(
                    sprites, normal.variant(privateState4), EntitySpriteDefinition.DynamicSprite.TileSource.GPU);
            appendDynamicPair(
                    sprites,
                    crawling.variant(animation),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(ENTITY_BUSH_CRAWLER, 0x07, normal.address(), 0, variants);
    }

    /**
     * Selects the bank-$03 Iron Mask display list. Private state 2 is zero
     * while the mask is intact and nonzero after the mask has been removed.
     */
    public EntitySpriteDefinition forIronMaskState(int privateState2) {
        if ((privateState2 & ~0xFF) != 0) {
            throw new IllegalArgumentException("Iron Mask private state must be an unsigned byte: " + privateState2);
        }
        return privateState2 == 0
                ? decoder.decodePair(ENTITY_IRON_MASK, 0x03, 0x4FCB, 8, 0)
                : decoder.decodePair(ENTITY_IRON_MASK, 0x03, 0x4FEB, 2, 0);
    }

    /**
     * Selects the bank-$20 rectangle list used by bank-$36's
     * {@code func_036_69D9}. States below six while ACTIVE use the animated
     * eight-variant list; all other status/state combinations use the
     * four-variant inactive list.
     */
    public EntitySpriteDefinition forColorShellState(int entityType, int state, EntityStatus status) {
        if (!isColorShellType(entityType)) {
            throw new IllegalArgumentException("Not a Color Shell entity type: 0x" + Integer.toHexString(entityType));
        }
        if (state < 0 || state > 0x0D) {
            throw new IllegalArgumentException("Color Shell state out of range: " + state);
        }
        if (status == null) {
            throw new IllegalArgumentException("Entity status cannot be null");
        }
        int color = entityType - ENTITY_COLOR_SHELL_RED;
        boolean activeList = status == EntityStatus.ACTIVE && state < 0x06;
        int address =
                (activeList ? COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES : COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES)[color];
        return decoder.decodeRectangle(entityType, 0x20, address, activeList ? 8 : 4, 3, 0);
    }

    /**
     * Decodes Hiding Zol's mixed display path. The bank-$07 handler selects
     * the single-sprite list when the entity variant is $01 and the pair list
     * for variants $00, $02, and $03. A null second OAM entry represents that
     * single-sprite branch without inventing a second tile.
     */
    EntitySpriteDefinition decodeHidingZol(int entityType) {
        EntitySpriteDefinition pair = decoder.decodePair(entityType, 0x07, 0x729B, 4, 0);
        EntitySpriteDefinition single = decoder.decodeSingle(entityType, 0x07, 0x72AB, 1, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(pair.variants());
        variants.set(1, new EntitySpriteDefinition.Variant(single.variant(0).first(), null));
        return new EntitySpriteDefinition(entityType, 0x07, 0x729B, EntitySpriteDefinition.Shape.PAIR, 0, variants);
    }

    /**
     * Decodes FishEntityHandler's five pair variants followed by its four
     * single-sprite jump variants. The handler selects the latter by
     * subtracting five from the active variant before rendering.
     */
    EntitySpriteDefinition decodeFish(int entityType) {
        EntitySpriteDefinition pair = decoder.decodePair(entityType, 0x15, 0x449F, 5, 0);
        EntitySpriteDefinition single = decoder.decodeSingle(entityType, 0x15, 0x44B3, 4, 0);
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
    EntitySpriteDefinition decodeMoblinSword(int entityType) {
        EntitySpriteDefinition body = decoder.decodePair(entityType, 0x07, 0x7A95, 8, 0);
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

    void appendMoblinSwordGeneratedPair(
            List<EntitySpriteDefinition.DynamicSprite> sprites, int yOffset, int xOffset, int tileSelector) {
        int tileAddress = 0x4A93 + tileSelector * 2;
        int attributeAddress = 0x4AA3 + tileSelector * 2;
        int firstTile = decoder.readByte(0x20, tileAddress);
        int secondTile = decoder.readByte(0x20, tileAddress + 1);
        int firstAttributes = decoder.readByte(0x20, attributeAddress);
        int secondAttributes = decoder.readByte(0x20, attributeAddress + 1);
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
     * Returns the display list selected by CreateFollowingNpcEntity handlers.
     * The caller supplies the matching follower entity type in the same form
     * used by the disassembly's dynamic entity spawner.
     */
    public EntitySpriteDefinition forFollowerEntityType(int entityType) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        return switch (entityType) {
            case ENTITY_BOW_WOW -> decoder.decodePair(entityType, 0x05, 0x401C, 7, 0);
            case ENTITY_MARIN_AT_THE_SHORE -> decoder.decodePair(entityType, 0x18, 0x59B8, 11, 0);
            case 0xD4 -> decoder.decodePair(entityType, 0x19, 0x5DF8, 6, 0);
            case 0xD5 -> decoder.decodePair(entityType, 0x19, 0x59BC, 8, 0);
            default -> EntitySpriteDefinition.unsupported(entityType);
        };
    }

    /** Bank $19:43A9: center pair followed by four orbiting single sprites. */
    EntitySpriteDefinition decodeDungeonWarp() {
        EntitySpriteDefinition center = decoder.decodePair(0x61, 0x19, 0x4393, 2, 0);
        EntitySpriteDefinition dots = decoder.decodeSingle(0x61, 0x19, 0x43A5, 2, 0);
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(8);
        for (int phase = 0; phase < 8; phase++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(6);
            appendDynamicPair(
                    sprites, center.variant(phase & 1), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
            for (int orbit = 0; orbit < 8; orbit += 2) {
                int index = (phase + orbit) & 7;
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                        signedByte(decoder.readByte(0x19, 0x439B + index)),
                        signedByte(decoder.readByte(0x19, 0x439D + index)) + 4,
                        dots.variant(phase & 1).first(),
                        EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                        false));
            }
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(0x61, 0x19, 0x4393, 0, variants);
    }

    EntitySpriteDefinition owlStatue(EntitySpriteRecipe recipe) {

        int entityType = recipe.entityType();

        // Ordinary owl: hActiveEntityVisualPosY + 1, not a world-position change.
        // South Face Shrine instead uses the separate four-sprite mural handler.
        EntitySpriteDefinition.Variant pair = decoder.decodePair(entityType, recipe.bank(), recipe.address(), 1, 0)
                .variant(0);
        return EntitySpriteDefinition.dynamic(
                entityType,
                recipe.bank(),
                recipe.address(),
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
}
