package linksawakening.ui;

import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;

/**
 * Fixed ROM tables used by the file-selection and New Game screens.
 *
 * <p>The file-menu state machine deals in logical table values, so this
 * adapter owns the bank-window address conversion and unsigned byte reads.</p>
 */
public final class FileMenuRomData {

    public static final int FILE_MENU_BANK = RomSymbols.NameEntryCharacterTable.bank();
    public static final int SELECTION_CURSOR_Y_ADDRESS = RomSymbols.Data_001_48E4.address();
    public static final int NAME_ENTRY_CHARACTER_TABLE_ADDRESS = RomSymbols.NameEntryCharacterTable.address();
    public static final int NAME_CURSOR_Y_ADDRESS = RomSymbols.Data_001_4B30.address();
    public static final int NAME_CURSOR_X_ADDRESS = RomSymbols.Data_001_4B70.address();
    public static final int NAME_POSITION_X_ADDRESS = RomSymbols.Data_001_4BB0.address();
    public static final int CODEPOINT_TO_TILE_MAP_BANK = RomSymbols.CodepointToTileMap.bank();
    public static final int CODEPOINT_TO_TILE_MAP_ADDRESS = RomSymbols.CodepointToTileMap.address();

    private static final int SELECTION_CURSOR_Y_COUNT = 4;
    private static final int NAME_ENTRY_CHARACTER_COUNT = 0x40;
    private static final int NAME_CURSOR_COUNT = 0x40;
    private static final int NAME_POSITION_COUNT = 5;
    private static final int CODEPOINT_TO_TILE_MAP_COUNT = 0x100;

    private final Rom romData;
    private final Rom romView;

    public FileMenuRomData(Rom romData) {
        this.romData =
                new Rom(Objects.requireNonNull(romData, "romData").bytes(RomAddress.fromOffset(0), romData.size()));
        this.romView = romData;
    }

    public int[] deathDigitTiles() {
        return readUnsignedBytes("Data_001_4F3B", 1, 0x4F3B, 10);
    }

    public int[] quitOkTiles() {
        return readUnsignedBytes("FileMenuQuitOkTilemap.data", 1, 0x4E46, 14);
    }

    public int[] selectionCursorYPositions() {
        return readUnsignedBytes("Data_001_48E4", FILE_MENU_BANK, SELECTION_CURSOR_Y_ADDRESS, SELECTION_CURSOR_Y_COUNT);
    }

    public int[] nameEntryCharacterTable() {
        return readUnsignedBytes(
                "NameEntryCharacterTable",
                FILE_MENU_BANK,
                NAME_ENTRY_CHARACTER_TABLE_ADDRESS,
                NAME_ENTRY_CHARACTER_COUNT);
    }

    public int[] nameCursorYPositions() {
        return readUnsignedBytes("Data_001_4B30", FILE_MENU_BANK, NAME_CURSOR_Y_ADDRESS, NAME_CURSOR_COUNT);
    }

    public int[] nameCursorXPositions() {
        return readUnsignedBytes("Data_001_4B70", FILE_MENU_BANK, NAME_CURSOR_X_ADDRESS, NAME_CURSOR_COUNT);
    }

    public int[] namePositionXPositions() {
        return readUnsignedBytes("Data_001_4BB0", FILE_MENU_BANK, NAME_POSITION_X_ADDRESS, NAME_POSITION_COUNT);
    }

    public int[] codepointToTileMap() {
        return readUnsignedBytes(
                "CodepointToTileMap",
                CODEPOINT_TO_TILE_MAP_BANK,
                CODEPOINT_TO_TILE_MAP_ADDRESS,
                CODEPOINT_TO_TILE_MAP_COUNT);
    }

    private int[] readUnsignedBytes(String label, int bank, int address, int length) {
        int offset = RomBank.romOffset(bank, address);
        if (length < 0 || offset < 0 || offset > romData.size() - length) {
            throw new IllegalArgumentException(String.format(
                    "%s truncated: bank $%02X address $%04X length %d (ROM length %d)",
                    label, bank, address, length, romData.size()));
        }
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = romView.u8(RomAddress.fromOffset(offset + index));
        }
        return values;
    }

    /** @deprecated Use the Rom overload; retains legacy partial-buffer validation. */
    @Deprecated(forRemoval = true)
    public FileMenuRomData(byte[] romData) {
        this(romData == null ? null : new Rom(romData));
    }
}
