package linksawakening.entity;

import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.rom.RomBank;

/**
 * ROM-backed display-list decoder plus the small handler-to-list mapping
 * required by the first entity rendering increment.
 */

/** EntitySpriteDecodeGeometry behavior on the shared EntitySpriteHandlerCatalog state. */
abstract class EntitySpriteDecodeGeometry extends EntitySpriteRomDecoding {
    protected EntitySpriteDecodeGeometry(byte[] romData) {
        super(romData);
    }

    protected EntitySpriteDefinition decodePowerRecoilDeathRectangle() {
        int entityType = 0x00;
        int bank = 0x03;
        int address = 0x54C8;
        List<List<EntitySpriteDefinition.RectangleSprite>> sourceVariants =
                decodeRectangleVariants(entityType, bank, address, 5, 4, 0);
        List<List<EntitySpriteDefinition.RectangleSprite>> variants = new ArrayList<>(sourceVariants.subList(0, 3));
        List<EntitySpriteDefinition.RectangleSprite> finalFrame = new ArrayList<>(sourceVariants.get(3));
        finalFrame.addAll(sourceVariants.get(4));
        variants.add(List.copyOf(finalFrame));
        return new EntitySpriteDefinition(
                entityType, bank, address, EntitySpriteDefinition.Shape.RECTANGLE, 0, List.of(), variants);
    }

    protected List<List<EntitySpriteDefinition.RectangleSprite>> decodeRectangleVariants(
            int entityType, int bank, int address, int variantCount, int spriteCount, int initialVariant) {
        if (spriteCount <= 0) {
            throw new IllegalArgumentException("Rectangle display lists need sprites");
        }
        long strideLong = (long) spriteCount * 4;
        if (strideLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rectangle display list is too large");
        }
        int stride = (int) strideLong;
        int offset = validateDisplayList(entityType, bank, address, variantCount, stride, initialVariant);
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangleVariants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            List<EntitySpriteDefinition.RectangleSprite> sprites = new ArrayList<>(spriteCount);
            int variantOffset = offset + variant * stride;
            for (int sprite = 0; sprite < spriteCount; sprite++) {
                int index = variantOffset + sprite * 4;
                int yOffset = signedByte(romData[index]);
                int xOffset = signedByte(romData[index + 1]);
                sprites.add(new EntitySpriteDefinition.RectangleSprite(
                        yOffset,
                        xOffset,
                        new EntitySpriteDefinition.OamAttribute(
                                Byte.toUnsignedInt(romData[index + 2]), Byte.toUnsignedInt(romData[index + 3]))));
            }
            rectangleVariants.add(List.copyOf(sprites));
        }
        return List.copyOf(rectangleVariants);
    }

    protected int validateDisplayList(
            int entityType, int bank, int address, int variantCount, int stride, int initialVariant) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        if (bank < 0 || bank > 0xFF || address < 0x4000 || address > 0x7FFF) {
            throw new IllegalArgumentException("Display list address is outside a switchable ROM bank");
        }
        if (variantCount <= 0 || initialVariant < 0 || initialVariant >= variantCount) {
            throw new IllegalArgumentException("Invalid display list variant range");
        }
        long byteCount = (long) variantCount * stride;
        if (address + byteCount > 0x8000L) {
            throw new IllegalArgumentException("Display list crosses the selected ROM bank boundary");
        }
        int offset = RomBank.romOffset(bank, address);
        if (offset < 0 || byteCount > romData.length - offset) {
            throw new IllegalArgumentException(
                    "Display list exceeds ROM bounds at 0x" + Integer.toHexString(Math.max(offset, 0)));
        }
        return offset;
    }

    protected int readByte(int bank, int address) {
        int offset = RomBank.romOffset(bank, address);
        if (offset < 0 || offset >= romData.length) {
            throw new IllegalArgumentException("ROM byte is outside the ROM at 0x" + Integer.toHexString(offset));
        }
        return Byte.toUnsignedInt(romData[offset]);
    }

    /** Bank $19:43A9: center pair followed by four orbiting single sprites. */
    protected EntitySpriteDefinition decodeDungeonWarp() {
        EntitySpriteDefinition center = decodePair(0x61, 0x19, 0x4393, 2, 0);
        EntitySpriteDefinition dots = decodeSingle(0x61, 0x19, 0x43A5, 2, 0);
        List<List<EntitySpriteDefinition.DynamicSprite>> variants = new ArrayList<>(8);
        for (int phase = 0; phase < 8; phase++) {
            List<EntitySpriteDefinition.DynamicSprite> sprites = new ArrayList<>(6);
            appendDynamicPair(
                    sprites, center.variant(phase & 1), EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS);
            for (int orbit = 0; orbit < 8; orbit += 2) {
                int index = (phase + orbit) & 7;
                sprites.add(new EntitySpriteDefinition.DynamicSprite(
                        signedByte(readByte(0x19, 0x439B + index)),
                        signedByte(readByte(0x19, 0x439D + index)) + 4,
                        dots.variant(phase & 1).first(),
                        EntitySpriteDefinition.DynamicSprite.TileSource.ENTITY_SHEETS,
                        false));
            }
            variants.add(List.copyOf(sprites));
        }
        return EntitySpriteDefinition.dynamic(0x61, 0x19, 0x4393, 0, variants);
    }
}
