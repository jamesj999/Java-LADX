package linksawakening.ui;

import linksawakening.rom.RomBank;

import java.util.Objects;

/**
 * Fixed ROM tables used by the file-selection and New Game screens.
 *
 * <p>The file-menu state machine deals in logical table values, so this
 * adapter owns the bank-window address conversion and unsigned byte reads.</p>
 */
public final class FileMenuRomData {

    public static final int FILE_MENU_BANK = 0x01;
    public static final int SELECTION_CURSOR_Y_ADDRESS = 0x48E4;
    public static final int NAME_ENTRY_CHARACTER_TABLE_ADDRESS = 0x4BB5;
    public static final int NAME_CURSOR_Y_ADDRESS = 0x4B30;
    public static final int NAME_CURSOR_X_ADDRESS = 0x4B70;
    public static final int NAME_POSITION_X_ADDRESS = 0x4BB0;
    public static final int CODEPOINT_TO_TILE_MAP_BANK = 0x1C;
    public static final int CODEPOINT_TO_TILE_MAP_ADDRESS = 0x4641;

    private static final int SELECTION_CURSOR_Y_COUNT = 4;
    private static final int NAME_ENTRY_CHARACTER_COUNT = 0x40;
    private static final int NAME_CURSOR_COUNT = 0x40;
    private static final int NAME_POSITION_COUNT = 5;
    private static final int CODEPOINT_TO_TILE_MAP_COUNT = 0x100;

    private final byte[] romData;

    public FileMenuRomData(byte[] romData) {
        this.romData = Objects.requireNonNull(romData, "romData").clone();
    }

    public int[] selectionCursorYPositions() {
        return readUnsignedBytes("Data_001_48E4", FILE_MENU_BANK,
            SELECTION_CURSOR_Y_ADDRESS, SELECTION_CURSOR_Y_COUNT);
    }

    public int[] nameEntryCharacterTable() {
        return readUnsignedBytes("NameEntryCharacterTable", FILE_MENU_BANK,
            NAME_ENTRY_CHARACTER_TABLE_ADDRESS, NAME_ENTRY_CHARACTER_COUNT);
    }

    public int[] nameCursorYPositions() {
        return readUnsignedBytes("Data_001_4B30", FILE_MENU_BANK,
            NAME_CURSOR_Y_ADDRESS, NAME_CURSOR_COUNT);
    }

    public int[] nameCursorXPositions() {
        return readUnsignedBytes("Data_001_4B70", FILE_MENU_BANK,
            NAME_CURSOR_X_ADDRESS, NAME_CURSOR_COUNT);
    }

    public int[] namePositionXPositions() {
        return readUnsignedBytes("Data_001_4BB0", FILE_MENU_BANK,
            NAME_POSITION_X_ADDRESS, NAME_POSITION_COUNT);
    }

    public int[] codepointToTileMap() {
        return readUnsignedBytes("CodepointToTileMap", CODEPOINT_TO_TILE_MAP_BANK,
            CODEPOINT_TO_TILE_MAP_ADDRESS, CODEPOINT_TO_TILE_MAP_COUNT);
    }

    private int[] readUnsignedBytes(String label, int bank, int address, int length) {
        int offset = RomBank.romOffset(bank, address);
        if (length < 0 || offset < 0 || offset > romData.length - length) {
            throw new IllegalArgumentException(String.format(
                "%s truncated: bank $%02X address $%04X length %d (ROM length %d)",
                label, bank, address, length, romData.length));
        }
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = Byte.toUnsignedInt(romData[offset + index]);
        }
        return values;
    }
}
