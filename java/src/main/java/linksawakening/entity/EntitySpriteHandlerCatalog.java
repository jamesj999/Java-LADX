package linksawakening.entity;

import linksawakening.rom.RomBank;
import linksawakening.world.EntityRoomLoader;
import linksawakening.world.EntityStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * ROM-backed display-list decoder plus the small handler-to-list mapping
 * required by the first entity rendering increment.
 */
public final class EntitySpriteHandlerCatalog {

    private static final int ENTITY_BUTTERFLY = 0x6E;
    private static final int ENTITY_OCTOROK = 0x09;
    private static final int ENTITY_OCTOROK_ROCK = 0x0A;
    private static final int ENTITY_MOBLIN = 0x0B;
    private static final int ENTITY_MOBLIN_ARROW = 0x0C;
    private static final int ENTITY_TEKTITE = 0x0D;
    private static final int ENTITY_LEEVER = 0x0E;
    private static final int ENTITY_ANTI_FAIRY = 0x15;
    private static final int ENTITY_SPARK_COUNTER_CLOCKWISE = 0x16;
    private static final int ENTITY_SPARK_CLOCKWISE = 0x17;
    private static final int ENTITY_ZOL = 0x1B;
    private static final int ENTITY_GEL = 0x1C;
    private static final int ENTITY_HIDING_ZOL = 0x9B;
    private static final int ENTITY_STALFOS_AGGRESSIVE = 0x1A;
    private static final int ENTITY_GIBDO = 0x1F;
    private static final int ENTITY_PEAHAT = 0xA0;
    private static final int ENTITY_ARMOS_STATUE = 0x0F;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_KEESE = 0x19;
    private static final int ENTITY_HARDHAT_BEETLE = 0x20;
    private static final int ENTITY_SPIKE_TRAP = 0x27;
    private static final int ENTITY_PAIRODD = 0x57;
    private static final int ENTITY_PAIRODD_PROJECTILE = 0x58;
    private static final int ENTITY_WATER_TEKTITE = 0x99;
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
    private static final int ENTITY_COLOR_SHELL_RED = 0xE9;
    private static final int ENTITY_COLOR_SHELL_BLUE = 0xEB;

    private static final int[] COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES = {
        0x6688, 0x66E8, 0x6748
    };
    private static final int[] COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES = {
        0x67A8, 0x67D8, 0x6808
    };

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

        if (isColorShellType(entityType)) {
            // Room streams begin in INIT. The bank-$36 renderer uses its
            // inactive four-variant list until the first active handler tick.
            return forColorShellState(entityType, 0, EntityStatus.INIT);
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
        if (entityType == ENTITY_OCTOROK_ROCK) {
            return decodePair(entityType, 0x03, 0x6A1E, 2, 0);
        }
        if (entityType == ENTITY_MOBLIN) {
            return decodePair(entityType, 0x03, 0x5917, 8, 0);
        }
        if (entityType == ENTITY_MOBLIN_ARROW) {
            return decodePair(entityType, 0x03, 0x6BC6, 4, 0);
        }
        if (entityType == ENTITY_TEKTITE) {
            return decodePair(entityType, 0x06, 0x78B7, 2, 0);
        }
        if (entityType == ENTITY_LEEVER) {
            return decodePair(entityType, 0x04, 0x7EE5, 4, 0);
        }
        if (entityType == ENTITY_ANTI_FAIRY) {
            return decodePair(entityType, 0x06, 0x786E, 2, 0);
        }
        if (entityType == ENTITY_SPARK_COUNTER_CLOCKWISE
            || entityType == ENTITY_SPARK_CLOCKWISE) {
            return decodePair(entityType, 0x06, 0x6615, 2, 0);
        }
        if (entityType == ENTITY_ZOL) {
            return decodePair(entityType, 0x06, 0x7C09, 2, 0);
        }
        if (entityType == ENTITY_GEL) {
            return decodeSingle(entityType, 0x06, 0x7BFA, 2, 0);
        }
        if (entityType == ENTITY_HIDING_ZOL) {
            return decodeHidingZol(entityType);
        }
        if (entityType == ENTITY_STALFOS_AGGRESSIVE) {
            return decodePair(entityType, 0x06, 0x4AA8, 3, 0);
        }
        if (entityType == ENTITY_GIBDO) {
            return decodePair(entityType, 0x06, mapId == 0x07 ? 0x7E77 : 0x7E6F, 2, 0);
        }
        if (entityType == ENTITY_PEAHAT) {
            return decodePair(entityType, 0x07, 0x6701, 2, 0);
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
        if (entityType == ENTITY_SPIKE_TRAP) {
            return decodePair(entityType, 0x06, 0x74FA, 1, 0);
        }
        if (entityType == ENTITY_PAIRODD) {
            return decodePair(entityType, 0x04, 0x5DD1, 8, 0);
        }
        if (entityType == ENTITY_PAIRODD_PROJECTILE) {
            return decodePair(entityType, 0x04, 0x5EF4, 2, 0);
        }
        if (entityType == ENTITY_WATER_TEKTITE) {
            return decodePair(entityType, 0x07, 0x752D, 2, 0);
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
     * Selects the bank-$20 rectangle list used by bank-$36's
     * {@code func_036_69D9}. States below six while ACTIVE use the animated
     * eight-variant list; all other status/state combinations use the
     * four-variant inactive list.
     */
    public EntitySpriteDefinition forColorShellState(int entityType, int state,
                                                      EntityStatus status) {
        if (!isColorShellType(entityType)) {
            throw new IllegalArgumentException("Not a Color Shell entity type: 0x"
                + Integer.toHexString(entityType));
        }
        if (state < 0 || state > 0x0D) {
            throw new IllegalArgumentException("Color Shell state out of range: " + state);
        }
        if (status == null) {
            throw new IllegalArgumentException("Entity status cannot be null");
        }
        int color = entityType - ENTITY_COLOR_SHELL_RED;
        boolean activeList = status == EntityStatus.ACTIVE && state < 0x06;
        int address = (activeList ? COLOR_SHELL_ACTIVE_DISPLAY_ADDRESSES
                                  : COLOR_SHELL_INACTIVE_DISPLAY_ADDRESSES)[color];
        return decodeRectangle(entityType, 0x20, address, activeList ? 8 : 4, 3, 0);
    }

    /** The bank-$03 fire pair rendered over entities in {@code BURNING} status. */
    public EntitySpriteDefinition forBurningEntity() {
        return decodePair(0x00, 0x03, 0x4C44, 2, 0);
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
    private EntitySpriteDefinition decodeHidingZol(int entityType) {
        EntitySpriteDefinition pair = decodePair(entityType, 0x07, 0x729B, 4, 0);
        EntitySpriteDefinition single = decodeSingle(entityType, 0x07, 0x72AB, 1, 0);
        List<EntitySpriteDefinition.Variant> variants = new ArrayList<>(pair.variants());
        variants.set(1, new EntitySpriteDefinition.Variant(single.variant(0).first(), null));
        return new EntitySpriteDefinition(entityType, 0x07, 0x729B,
            EntitySpriteDefinition.Shape.PAIR, 0, variants);
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

    private static boolean isColorShellType(int entityType) {
        return entityType >= ENTITY_COLOR_SHELL_RED && entityType <= ENTITY_COLOR_SHELL_BLUE;
    }
}
