package linksawakening.entity;

import static linksawakening.entity.EntitySpriteGeometry.*;
import static linksawakening.entity.EntitySpriteHandlerCatalog.*;

import java.util.ArrayList;
import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;

/** Validated ROM display-list decoding for single, paired and rectangular OAM layouts. */
final class EntitySpriteDecoder {
    EntitySpriteDecoder(Rom romData) {
        if (romData == null) throw new IllegalArgumentException("ROM data cannot be null");
        this.romData = romData;
    }

    EntitySpriteDefinition decode(EntitySpriteRecipe recipe) {
        return switch (recipe.kind()) {
            case PAIR -> decodePair(
                    recipe.entityType(),
                    recipe.bank(),
                    recipe.address(),
                    recipe.variantCount(),
                    recipe.initialVariant(),
                    recipe.tileSource());
            case SINGLE -> decodeSingle(
                    recipe.entityType(),
                    recipe.bank(),
                    recipe.address(),
                    recipe.variantCount(),
                    recipe.initialVariant());
            case RECTANGLE -> decodeRectangle(
                    recipe.entityType(),
                    recipe.bank(),
                    recipe.address(),
                    recipe.variantCount(),
                    recipe.spriteCount(),
                    recipe.initialVariant());
            default -> throw new IllegalArgumentException("Recipe requires a specialized composer: " + recipe.kind());
        };
    }

    private final Rom romData;

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

    EntitySpriteDefinition decodePair(
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

    List<List<EntitySpriteDefinition.RectangleSprite>> decodeRectangleVariants(
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
                int yOffset = signedByte((byte) romData.u8(RomAddress.fromOffset(index)));
                int xOffset = signedByte((byte) romData.u8(RomAddress.fromOffset(index + 1)));
                sprites.add(new EntitySpriteDefinition.RectangleSprite(
                        yOffset,
                        xOffset,
                        new EntitySpriteDefinition.OamAttribute(
                                romData.u8(RomAddress.fromOffset(index + 2)),
                                romData.u8(RomAddress.fromOffset(index + 3)))));
            }
            rectangleVariants.add(List.copyOf(sprites));
        }
        return List.copyOf(rectangleVariants);
    }

    int validateDisplayList(int entityType, int bank, int address, int variantCount, int stride, int initialVariant) {
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
        if (offset < 0 || byteCount > romData.size() - offset) {
            throw new IllegalArgumentException(
                    "Display list exceeds ROM bounds at 0x" + Integer.toHexString(Math.max(offset, 0)));
        }
        return offset;
    }

    int readByte(int bank, int address) {
        int offset = RomBank.romOffset(bank, address);
        if (offset < 0 || offset >= romData.size()) {
            throw new IllegalArgumentException("ROM byte is outside the ROM at 0x" + Integer.toHexString(offset));
        }
        return romData.u8(RomAddress.fromOffset(offset));
    }
}
