package linksawakening.rom;
/** Immutable copies of EquipmentTables ROM lookup data. */
public final class EquipmentTables {
    // LinkDirectionToMagicRodXOffset: original loader length.
    private static final int MAGIC_ROD_X_OFFSET_LENGTH = 8;
    private final byte[] magicRodXOffset;
    // LinkDirectionToMagicRodYOffset: original loader length.
    private static final int MAGIC_ROD_Y_OFFSET_LENGTH = 8;
    private final byte[] magicRodYOffset;
    // LinkDirectionToMagicRodTiles: original loader length.
    private static final int MAGIC_ROD_TILES_LENGTH = 16;
    private final int[] magicRodTiles;
    // LinkDirectionToMagicRodOAMAttributes: original loader length.
    private static final int MAGIC_ROD_ATTRIBUTES_LENGTH = 16;
    private final int[] magicRodAttributes;
    // LinkDirectionToLinkAnimationState2: original loader length.
    private static final int SHOVEL_ANIMATION_STATES_LENGTH = 8;
    private final int[] shovelAnimationStates;

    private EquipmentTables(Rom rom) {
        magicRodXOffset = rom.bytes(RomSymbols.LinkDirectionToMagicRodXOffset, MAGIC_ROD_X_OFFSET_LENGTH);
        magicRodYOffset = rom.bytes(RomSymbols.LinkDirectionToMagicRodYOffset, MAGIC_ROD_Y_OFFSET_LENGTH);
        magicRodTiles = rom.u8Table(RomSymbols.LinkDirectionToMagicRodTiles, MAGIC_ROD_TILES_LENGTH);
        magicRodAttributes = rom.u8Table(RomSymbols.LinkDirectionToMagicRodOAMAttributes, MAGIC_ROD_ATTRIBUTES_LENGTH);
        shovelAnimationStates =
                rom.u8Table(RomSymbols.LinkDirectionToLinkAnimationState2, SHOVEL_ANIMATION_STATES_LENGTH);
    }

    public static EquipmentTables load(Rom rom) {
        return new EquipmentTables(rom);
    }

    /** Signed Link-top-left X offset for one Magic Rod swing direction. */
    public int magicRodXOffset(int tableIndex) {
        return magicRodXOffset[tableIndex & 0x07];
    }

    /** Signed Link-top-left Y offset for one Magic Rod swing direction. */
    public int magicRodYOffset(int tableIndex) {
        return magicRodYOffset[tableIndex & 0x07];
    }

    /** Link-character tile used by one Magic Rod OAM slot. */
    public int magicRodTile(int tableIndex, int spriteSlot) {
        return magicRodTiles[(tableIndex & 0x07) * 2 + (spriteSlot & 0x01)];
    }

    /** OAM attributes used by one Magic Rod OAM slot. */
    public int magicRodAttribute(int tableIndex, int spriteSlot) {
        return magicRodAttributes[(tableIndex & 0x07) * 2 + (spriteSlot & 0x01)];
    }

    /**
     * Link animation state selected by the ROM shovel timer. Timer values
     * below $10 use phase zero; values from $10 onward use phase one.
     */
    public int shovelAnimationState(int timer, int romDirection) {
        if (romDirection < 0 || romDirection > 3) {
            throw new IllegalArgumentException("ROM shovel direction out of range: " + romDirection);
        }
        int phase = ((timer & 0xFF) >>> 4) & 0x01;
        return shovelAnimationStates[(romDirection & 0x03) * 2 + phase];
    }
}
