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
    private static final int ENTITY_DOG = 0x6F;
    private static final int ENTITY_KID_70 = 0x70;
    private static final int ENTITY_KID_73 = 0x73;
    private static final int ENTITY_CROW = 0x7A;
    private static final int ENTITY_MARIN = 0x3E;
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
        if ((entityType & ~0xFF) != 0) {
            throw new IllegalArgumentException("Entity type must be an unsigned byte: " + entityType);
        }
        if (roomTable == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }

        if (entityType == ENTITY_CROW) {
            return decodePair(entityType, 0x06, 0x5C89, 4, 2);
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
        return EntitySpriteDefinition.unsupported(entityType);
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
}
