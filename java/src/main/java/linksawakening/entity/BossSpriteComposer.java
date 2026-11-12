package linksawakening.entity;

import static linksawakening.entity.EntitySpriteGeometry.*;
import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.world.entity.event.MoldormSegmentPosition;

/** Boss body, segment, weapon and shadow composition from decoded source display lists. */
final class BossSpriteComposer {
    private final EntitySpriteDecoder decoder;

    BossSpriteComposer(EntitySpriteDecoder decoder) {
        this.decoder = decoder;
    }

    /**
     * Builds Mini Moldorm's handler-generated head and delayed-history tail.
     * The ROM renders all three two-sprite pairs in one pass, with the latter
     * two pairs sourced from history rather than from the active entity
     * position.
     */
    public EntitySpriteDefinition forMiniMoldormState(
            int headVariant, int entityX, int entityY, int segment1X, int segment1Y, int segment2X, int segment2Y) {
        if (headVariant < 0 || headVariant > 7) {
            throw new IllegalArgumentException("Mini Moldorm head variant must be 0..7: " + headVariant);
        }
        EntitySpriteDefinition pairs = decoder.decodePair(ENTITY_MINI_MOLDORM, 0x04, 0x5A49, 10, 0);
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(6);
        appendDynamicPairAtOffset(sprites, pairs.variant(headVariant), 0, 0);
        appendDynamicPairAtOffset(
                sprites, pairs.variant(8), signedByte(segment1Y - entityY), signedByte(segment1X - entityX));
        appendDynamicPairAtOffset(
                sprites, pairs.variant(9), signedByte(segment2Y - entityY), signedByte(segment2X - entityX));
        return EntitySpriteDefinition.dynamic(ENTITY_MINI_MOLDORM, 0x04, 0x5A49, 0, List.of(List.copyOf(sprites)));
    }

    /** Builds Moldorm's eight-sprite head and delayed four-pair tail. */
    public EntitySpriteDefinition forMoldormState(
            int headVariant,
            int entityX,
            int entityY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter) {
        return forMoldormPresentation(
                headVariant, entityX, entityY, entityX, entityY, segments, privateState3, frameCounter);
    }

    /** Builds pre-update Moldorm OAM relative to the post-update entity position. */
    public EntitySpriteDefinition forMoldormPresentation(
            int headVariant,
            int headX,
            int headY,
            int renderBaseX,
            int renderBaseY,
            List<MoldormSegmentPosition> segments,
            int privateState3,
            int frameCounter) {
        if (headVariant < 0 || headVariant > 7) {
            throw new IllegalArgumentException("Moldorm head variant must be 0..7: " + headVariant);
        }
        if (segments == null || segments.size() != 4) {
            throw new IllegalArgumentException("Moldorm requires four tail history positions");
        }
        if (privateState3 < 0 || privateState3 > 5) {
            throw new IllegalArgumentException("Moldorm private state 3 must be 0..5: " + privateState3);
        }

        if (privateState3 == 5) {
            return EntitySpriteDefinition.unsupported(ENTITY_MOLDORM);
        }

        EntitySpriteDefinition head = decoder.decodeRectangle(ENTITY_MOLDORM, 0x04, 0x57F2, 8, 8, headVariant);
        EntitySpriteDefinition tail = decoder.decodePair(ENTITY_MOLDORM, 0x04, 0x58F2, 4, 0);
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(16);
        int headYOffset = signedByte(headY - renderBaseY);
        int headXOffset = signedByte(headX - renderBaseX);
        for (EntitySpriteDefinition.RectangleSprite sprite : head.rectangleVariant(headVariant)) {
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    signedByte(sprite.yOffset() + headYOffset),
                    signedByte(sprite.xOffset() + headXOffset),
                    sprite.oam(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true));
        }

        int visibleTailPairs = 4 - privateState3;
        int[] tailVariants = {0, 0, 1, 2 + ((frameCounter >> 3) & 0x01)};
        for (int index = 0; index < visibleTailPairs; index++) {
            MoldormSegmentPosition position = segments.get(index);
            EntitySpriteDefinition.Variant pair = tail.variant(tailVariants[index]);
            int paletteXor = index == 3 && (frameCounter & 0x04) != 0 ? 0x10 : 0;
            appendMoldormTailPair(
                    sprites,
                    pair,
                    signedByte(position.y() - renderBaseY),
                    signedByte(position.x() - renderBaseX),
                    paletteXor);
        }
        return EntitySpriteDefinition.dynamic(ENTITY_MOLDORM, 0x04, 0x57F2, 0, List.of(List.copyOf(sprites)));
    }

    /** Slime Eye bank-04 display lists followed by the ground shadow. */
    public EntitySpriteDefinition forSlimeEyeState(boolean half, int variant, int z) {
        var bodyAddress =
                half ? linksawakening.rom.RomSymbols.Data_004_4FAE : linksawakening.rom.RomSymbols.Data_004_4BDD;
        var shadowAddress =
                half ? linksawakening.rom.RomSymbols.Data_004_4FEE : linksawakening.rom.RomSymbols.Data_004_4D9D;
        int variants = half ? 2 : 7;
        if (variant < 0 || variant >= variants) return EntitySpriteDefinition.unsupported(0x5B);
        var body = decoder.decodeRectangle(
                0x5B, bodyAddress.bank(), bodyAddress.address(), variants, half ? 8 : 16, variant);
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>();
        for (var sprite : body.rectangleVariant(variant)) {
            if (sprite.oam().tile() == 0xFF) continue;
            sprites.add(new EntitySpriteDefinition.DynamicSprite(
                    sprite.yOffset(),
                    sprite.xOffset(),
                    sprite.oam(),
                    EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                    true));
        }
        if (z != 0) {
            var shadow =
                    decoder.decodeRectangle(0x5B, shadowAddress.bank(), shadowAddress.address(), 1, half ? 4 : 6, 0);
            for (var sprite : shadow.rectangleVariant(0))
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                        signedByte(sprite.yOffset() + z - (half ? 2 : 8)),
                        sprite.xOffset(),
                        sprite.oam(),
                        EntitySpriteDefinition.DynamicSprite.TileSource.GPU,
                        true));
        }
        return EntitySpriteDefinition.dynamic(
                0x5B, bodyAddress.bank(), bodyAddress.address(), 0, List.of(List.copyOf(sprites)));
    }

    /** Builds the repeated vertical bar pairs emitted by {@code func_006_6FEA}. */
    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY) {
        return forRollingBonesBar(privateState1, entityY, 0);
    }

    /** Builds the bar using the current animated pair selected by the handler. */
    public EntitySpriteDefinition forRollingBonesBar(int privateState1, int entityY, int spriteVariant) {
        if (privateState1 < 0 || privateState1 > 5) {
            throw new IllegalArgumentException("Rolling Bones bar private state must be 0..5: " + privateState1);
        }
        EntitySpriteDefinition pair = decoder.decodePair(ENTITY_ROLLING_BONES_BAR, 0x06, 0x6ED5, 2, 0);
        int selectedVariant = spriteVariant & 0x01;
        int pairCount = 6 - privateState1;
        List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(pairCount * 2);
        for (int index = 0; index < pairCount; index++) {
            int visualY = 0x20 + index * 0x10;
            appendDynamicPairAtOffset(sprites, pair.variant(selectedVariant), signedByte(visualY - entityY), 0);
        }
        return EntitySpriteDefinition.dynamic(ENTITY_ROLLING_BONES_BAR, 0x06, 0x6ED5, 0, List.of(List.copyOf(sprites)));
    }

    /** Builds func_015_4AC9's body, optional weapon, and airborne shadow layers. */
    public EntitySpriteDefinition forMoblinKingState(int bodyVariant, int weaponVariant, boolean airborne) {
        if (bodyVariant < 0 || bodyVariant >= 14) {
            throw new IllegalArgumentException("Moblin King body variant must be 0..13");
        }
        if (weaponVariant < -1 || weaponVariant >= 4) {
            throw new IllegalArgumentException("Moblin King weapon variant must be -1..3");
        }
        EntitySpriteDefinition body = decoder.decodeRectangle(0xE4, 0x15, 0x4A15, 14, 3, 0);
        EntitySpriteDefinition weapon = decoder.decodeRectangle(0xE4, 0x15, 0x49E5, 4, 3, 0);
        EntitySpriteDefinition shadow = decoder.decodeRectangle(0xE4, 0x15, 0x4ABD, 1, 3, 0);
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
}
