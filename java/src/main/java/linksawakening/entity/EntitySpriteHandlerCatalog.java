package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * ROM-backed display-list decoder plus the small handler-to-list mapping
 * required by the first entity rendering increment.
 */
public final class EntitySpriteHandlerCatalog {

    private static final int ENTITY_BUTTERFLY = 0x6E;
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;
    private static final int ENTITY_BOW_WOW = 0x6D;
    private static final int ENTITY_DOG = 0x6F;
    private static final int ENTITY_KID_70 = 0x70;
    private static final int ENTITY_KID_73 = 0x73;
    private static final int ENTITY_CROW = 0x7A;
    private static final int ENTITY_DROPPABLE_HEART = 0x2D;
    private static final int ENTITY_DROPPABLE_RUPEE = 0x2E;
    private static final int ENTITY_IRON_MASKS_MASK = 0x32;
    private static final int ENTITY_PIECE_OF_POWER = 0x33;
    private static final int ENTITY_GUARDIAN_ACORN = 0x34;
    private static final int ENTITY_HEART_PIECE = 0x35;
    private static final int ENTITY_HEART_CONTAINER = 0x36;
    private static final int ENTITY_DROPPABLE_ARROWS = 0x37;
    private static final int ENTITY_DROPPABLE_BOMBS = 0x38;
    private static final int ENTITY_SLEEPY_TOADSTOOL = 0x3A;
    private static final int ENTITY_DROPPABLE_MAGIC_POWDER = 0x3B;
    private static final int ENTITY_HIDING_SLIME_KEY = 0x3C;
    private static final int ENTITY_DROPPABLE_SECRET_SEASHELL = 0x3D;
    private static final int ENTITY_MARIN = 0x3E;
    private static final int ENTITY_GRANDPA_ULRIRA = 0x77;
    private static final int ENTITY_MARIN_AT_THE_SHORE = 0xC1;
    private static final int ENTITY_MARIN_AT_TAL_TAL_HEIGHTS = 0xC2;

    private final byte[] romData;

    public EntitySpriteHandlerCatalog(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
    }

    public EntitySpriteDefinition forEntityType(int entityType,
                                                 EntityRoomLoader.RoomTable roomTable) {
        return forEntityType(entityType, roomTable, -1);
    }

    public EntitySpriteDefinition forEntityType(int entityType,
                                                 EntityRoomLoader.RoomTable roomTable,
                                                 int mapId) {
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        if (roomTable == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }

        if (entityType == ENTITY_CROW) {
            return decodePair(entityType, 0x06, 0x5C89, 4, 2);
        }
        if (entityType == ENTITY_BOW_WOW) {
            return decodePair(entityType, 0x05, 0x4000, 7, 0);
        }
        if (entityType == ENTITY_DOG) {
            return decodePair(entityType, 0x19, 0x48CA, 4, 2);
        }
        if (entityType == ENTITY_MARIN) {
            return roomTable == EntityRoomLoader.RoomTable.INDOORS_A
                || roomTable == EntityRoomLoader.RoomTable.INDOORS_B
                ? decodePair(entityType, 0x05, 0x4E0A, 8, 6)
                : decodePair(entityType, 0x05, 0x4E2A, 11, 6);
        }
        if (entityType == ENTITY_MARIN_AT_TAL_TAL_HEIGHTS) {
            return decodePair(entityType, 0x18, 0x5EB7, 8, 0);
        }
        if (entityType == ENTITY_KID_70 || entityType == ENTITY_KID_73) {
            return decodePair(entityType, 0x06, 0x604D, 4, 0);
        }
        if (entityType == ENTITY_BUTTERFLY) {
            return decodeSingle(entityType, 0x06, 0x6BBD, 2, 0);
        }
        if (entityType == ENTITY_KEESE) {
            return decodePair(entityType, 0x06, mapId == 0x0A ? 0x6710 : 0x6708, 2, 0);
        }
        if (entityType == ENTITY_OCTOROK) {
            return decodePair(entityType, 0x03, 0x57FB, 8, 0);
        }
        if (entityType == ENTITY_MOBLIN) {
            return decodePair(entityType, 0x03, 0x5917, 8, 0);
        }
        if (entityType == ENTITY_TEKTITE) {
            return decodePair(entityType, 0x06, 0x78B7, 2, 0);
        }
        if (entityType == ENTITY_ARMOS_STATUE) {
            return decodePair(entityType, 0x06, 0x7446, 2, 0);
        }
        if (entityType == ENTITY_GHINI) {
            return decodePair(entityType, 0x04, 0x5BFC, 2, 0);
        }
        if (entityType == ENTITY_HARDHAT_BEETLE) {
            return decodePair(entityType, 0x06, mapId == 0x0A ? 0x4F34 : 0x4F2C, 2, 0);
        }
        if (entityType == ENTITY_GRANDPA_ULRIRA) {
            return decodeRectangle(entityType, 0x06, 0x5C51, 2, 4, 0);
        }
        if (entityType == ENTITY_DROPPABLE_HEART) {
            return decodeSingle(entityType, 0x03, 0x5D36, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_RUPEE) {
            return decodeSingle(entityType, 0x03, 0x609C, 1, 0);
        }
        if (entityType == ENTITY_IRON_MASKS_MASK) {
            return decodePair(entityType, 0x03, 0x5B80, 2, 0);
        }
        if (entityType == ENTITY_PIECE_OF_POWER) {
            return decodePair(entityType, 0x03, 0x5B65, 2, 0);
        }
        if (entityType == ENTITY_GUARDIAN_ACORN) {
            return decodeSingle(entityType, 0x03, 0x5B5B, 1, 0);
        }
        if (entityType == ENTITY_HEART_PIECE) {
            return decodePair(entityType, 0x03, 0x5A4D, 1, 0);
        }
        if (entityType == ENTITY_HEART_CONTAINER) {
            return decodePair(entityType, 0x03, 0x59D8, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_ARROWS) {
            return decodePair(entityType, 0x03, 0x6079, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_BOMBS) {
            return decodeSingle(entityType, 0x03, 0x5FC0, 1, 0);
        }
        if (entityType == ENTITY_SLEEPY_TOADSTOOL) {
            return decodePair(entityType, 0x03, 0x5D47, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_MAGIC_POWDER) {
            return decodeSingle(entityType, 0x03, 0x6055, 1, 0);
        }
        if (entityType == ENTITY_HIDING_SLIME_KEY) {
            return decodeSingle(entityType, 0x03, 0x5FFB, 1, 0);
        }
        if (entityType == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return decodeSingle(entityType, 0x03, 0x5FD1, 1, 0);
        }
        if (entityType == ENTITY_MARIN_AT_THE_SHORE) {
            return decodePair(entityType, 0x18, 0x5EB7, 8, 0);
        }
        return EntitySpriteDefinition.unsupported(entityType);
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

    public EntitySpriteDefinition decodePair(int entityType, int bank, int address,
                                              int variantCount, int initialVariant) {
        int offset = validateDisplayList(entityType, bank, address, variantCount, 4,
            initialVariant);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            int index = offset + variant * 4;
            variants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(
                    Byte.toUnsignedInt(romData[index]), Byte.toUnsignedInt(romData[index + 1])),
                new EntitySpriteDefinition.OamAttribute(
                    Byte.toUnsignedInt(romData[index + 2]), Byte.toUnsignedInt(romData[index + 3]))));
        }
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.PAIR, initialVariant, variants);
    }

    public EntitySpriteDefinition decodeSingle(int entityType, int bank, int address,
                                                int variantCount, int initialVariant) {
        int offset = validateDisplayList(entityType, bank, address, variantCount, 2,
            initialVariant);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            int index = offset + variant * 2;
            variants.add(new EntitySpriteDefinition.Variant(
                new EntitySpriteDefinition.OamAttribute(
                    Byte.toUnsignedInt(romData[index]), Byte.toUnsignedInt(romData[index + 1])),
                null));
        }
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.SINGLE, initialVariant, variants);
    }

    public EntitySpriteDefinition decodeRectangle(int entityType, int bank, int address,
                                                   int variantCount, int spriteCount,
                                                   int initialVariant) {
        if (spriteCount <= 0) {
            throw new IllegalArgumentException("Rectangle display lists need sprites");
        }
        long strideLong = (long) spriteCount * 4;
        if (strideLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Rectangle display list is too large");
        }
        int stride = (int) strideLong;
        int offset = validateDisplayList(entityType, bank, address, variantCount, stride,
            initialVariant);
        List<List<EntitySpriteDefinition.RectangleSprite>> rectangleVariants =
            new ArrayList<>(variantCount);
        for (int variant = 0; variant < variantCount; variant++) {
            List<EntitySpriteDefinition.RectangleSprite> sprites = new ArrayList<>(spriteCount);
            int variantOffset = offset + variant * stride;
            for (int sprite = 0; sprite < spriteCount; sprite++) {
                int index = variantOffset + sprite * 4;
                int yOffset = signedByte(romData[index]);
                int xOffset = signedByte(romData[index + 1]);
                sprites.add(new EntitySpriteDefinition.RectangleSprite(
                    yOffset, xOffset,
                    new EntitySpriteDefinition.OamAttribute(
                        Byte.toUnsignedInt(romData[index + 2]),
                        Byte.toUnsignedInt(romData[index + 3]))));
            }
            rectangleVariants.add(List.copyOf(sprites));
        }
        return new EntitySpriteDefinition(entityType, bank, address,
            EntitySpriteDefinition.Shape.RECTANGLE, initialVariant, List.of(), rectangleVariants);
    }

    private int validateDisplayList(int entityType, int bank, int address,
                                    int variantCount, int stride, int initialVariant) {
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
            throw new IllegalArgumentException("Display list exceeds ROM bounds at 0x"
                + Integer.toHexString(Math.max(offset, 0)));
        }
        return offset;
    }

    private static int signedByte(byte value) {
        return value;
    }
}
