package linksawakening.cutscene;

import linksawakening.rom.RomBank;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Fixed data tables used by the opening sequence in bank 1 of the shipped ROM.
 *
 * <p>The adapter deliberately exposes records instead of file offsets so the
 * state machine cannot accidentally apply the bank-window formula twice or
 * interpret a signed OAM offset as an unsigned tile value.</p>
 */
public final class IntroRomData {
    public static final int INTRO_BANK = 0x01;
    public static final int INTRO_SHIP_TILES_ADDRESS = 0x7538;
    public static final int INTRO_SHIP_EXTRA_TILES_ADDRESS = 0x7550;
    public static final int SHIP_HEAVE_TABLE_ADDRESS = 0x7560;
    public static final int INTRO_LIGHTNING_TILES_ADDRESS = 0x75CB;
    public static final int LIGHTNING_ENTITY_POSITIONS_ADDRESS = 0x707B;
    public static final int LIGHTNING_ENTITY_STATUSES_ADDRESS = 0x7081;
    public static final int INTRO_MARIN_VARIANTS_ADDRESS = 0x764F;
    public static final int TITLE_TILE_MAP_POINTERS_ADDRESS = 0x7264;
    public static final int TITLE_ATTRIBUTE_MAP_POINTERS_ADDRESS = 0x732A;
    public static final int INTRO_SPARKLE_VARIANTS_ADDRESS = 0x77BD;
    public static final int TITLE_SPARKLE_X_POSITIONS_ADDRESS = 0x73C0;
    public static final int TITLE_SPARKLE_Y_POSITIONS_ADDRESS = 0x73C8;
    public static final int INTRO_VERTICAL_OFFSETS_ADDRESS = 0x7CF9;
    public static final int INTRO_COLOR_MODIFIER_BANK = 0x20;
    public static final int INTRO_COLOR_MODIFIER_ADDRESS = 0x6B9C;
    public static final int DX_FADE_IN_PALETTE_ADDRESS = 0x79A0;
    public static final int INERT_LINK_VARIANTS_ADDRESS = 0x7A27;
    public static final int TITLE_POST_BEACH_TILEMAP_ADDRESS = 0x7AE4;

    private static final int OAM_ENTRY_SIZE = 4;
    private static final int TITLE_ROW_COUNT = 7;
    private static final int TITLE_ROW_RECORD_SIZE = 3 + 16;
    private static final int POST_BEACH_TILEMAP_SIZE = 20 * 19;
    private static final int DX_FADE_PALETTE_ROW_COUNT = 16;

    private final byte[] romData;

    public IntroRomData(byte[] romData) {
        this.romData = Objects.requireNonNull(romData, "romData").clone();
    }

    public List<OamEntry> shipTiles() {
        return readOamEntries("IntroShipTiles", INTRO_SHIP_TILES_ADDRESS, 6);
    }

    public List<OamEntry> additionalShipTiles() {
        return readOamEntries("Data_001_7550", INTRO_SHIP_EXTRA_TILES_ADDRESS, 4);
    }

    public int[] shipHeaveTable() {
        return readUnsignedBytes("ShipHeaveTable", SHIP_HEAVE_TABLE_ADDRESS, 8);
    }

    public List<List<OamEntry>> lightningTiles() {
        List<OamEntry> entries = readOamEntries("IntroLightningTiles", INTRO_LIGHTNING_TILES_ADDRESS, 4 * 6);
        List<List<OamEntry>> frames = new ArrayList<>(4);
        for (int frame = 0; frame < 4; frame++) {
            frames.add(List.copyOf(entries.subList(frame * 6, (frame + 1) * 6)));
        }
        return List.copyOf(frames);
    }

    public int[] lightningEntityPositions() {
        return readUnsignedBytes("Data_001_707B", LIGHTNING_ENTITY_POSITIONS_ADDRESS, 6);
    }

    public int[] lightningEntityStatuses() {
        return readUnsignedBytes("Data_001_7081", LIGHTNING_ENTITY_STATUSES_ADDRESS, 6);
    }

    public List<SpritePair> marinVariants() {
        return readSpritePairs("IntroMarinSpriteVariants", INTRO_MARIN_VARIANTS_ADDRESS, 4);
    }

    public List<SpritePair> inertLinkVariants() {
        return readSpritePairs("InertLinkSpriteVariants", INERT_LINK_VARIANTS_ADDRESS, 2);
    }

    public List<SpritePair> sparkleVariants() {
        return readSpritePairs("IntroSparkleSpriteVariants", INTRO_SPARKLE_VARIANTS_ADDRESS, 8);
    }

    public int[] titleSparkleXPositions() {
        return readUnsignedBytes("Data_001_73C0", TITLE_SPARKLE_X_POSITIONS_ADDRESS, 8);
    }

    public int[] titleSparkleYPositions() {
        return readUnsignedBytes("Data_001_73C8", TITLE_SPARKLE_Y_POSITIONS_ADDRESS, 8);
    }

    public int[] introVerticalOffsets() {
        return readUnsignedBytes("IntroBGVerticalOffsetTable", INTRO_VERTICAL_OFFSETS_ADDRESS, 8);
    }

    public int[] introColorModifiers() {
        int offset = checkedOffset("IntroColorModifierTable", INTRO_COLOR_MODIFIER_BANK,
            INTRO_COLOR_MODIFIER_ADDRESS, 8);
        return readUnsignedBytesAt(offset, 8);
    }

    public List<TitleRow> titleRows() {
        int pointerTableOffset = checkedOffset(
            "TitleTileMap pointer table", INTRO_BANK, TITLE_TILE_MAP_POINTERS_ADDRESS, TITLE_ROW_COUNT * 2);
        int attributePointerTableOffset = checkedOffset(
            "TitleAttrMap pointer table", INTRO_BANK, TITLE_ATTRIBUTE_MAP_POINTERS_ADDRESS, TITLE_ROW_COUNT * 2);
        List<TitleRow> rows = new ArrayList<>(TITLE_ROW_COUNT);
        for (int row = 0; row < TITLE_ROW_COUNT; row++) {
            int tileAddress = readUnsignedWord(pointerTableOffset + row * 2);
            int attributeAddress = readUnsignedWord(attributePointerTableOffset + row * 2);
            int tileOffset = checkedOffset("TitleTileMap row " + row, INTRO_BANK,
                tileAddress, TITLE_ROW_RECORD_SIZE);
            int attributeOffset = checkedOffset("TitleAttrMap row " + row, INTRO_BANK,
                attributeAddress, TITLE_ROW_RECORD_SIZE);
            rows.add(new TitleRow(
                readBigEndianAddress(tileOffset),
                readBigEndianAddress(attributeOffset),
                readUnsignedBytesAt(tileOffset + 3, 16),
                readUnsignedBytesAt(attributeOffset + 3, 16)));
        }
        return List.copyOf(rows);
    }

    public int[] postBeachTilemap() {
        return readUnsignedBytes("TitleScreenPostBeachTilemap", TITLE_POST_BEACH_TILEMAP_ADDRESS,
            POST_BEACH_TILEMAP_SIZE);
    }

    public PaletteBlock dxFadeInPalette() {
        int offset = checkedOffset("DXFadeInPalette", INTRO_BANK, DX_FADE_IN_PALETTE_ADDRESS,
            DX_FADE_PALETTE_ROW_COUNT * 4 * 2);
        int[][] rows = new int[DX_FADE_PALETTE_ROW_COUNT][4];
        for (int row = 0; row < rows.length; row++) {
            for (int color = 0; color < rows[row].length; color++) {
                rows[row][color] = RomBank.decodeRgb555(readUnsignedWord(offset));
                offset += 2;
            }
        }
        return new PaletteBlock(rows);
    }

    private List<OamEntry> readOamEntries(String label, int address, int count) {
        int offset = checkedOffset(label, INTRO_BANK, address, count * OAM_ENTRY_SIZE);
        List<OamEntry> entries = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int entryOffset = offset + index * OAM_ENTRY_SIZE;
            entries.add(new OamEntry(
                signedByte(romData[entryOffset]),
                signedByte(romData[entryOffset + 1]),
                Byte.toUnsignedInt(romData[entryOffset + 2]),
                Byte.toUnsignedInt(romData[entryOffset + 3])));
        }
        return List.copyOf(entries);
    }

    private List<SpritePair> readSpritePairs(String label, int address, int count) {
        int offset = checkedOffset(label, INTRO_BANK, address, count * 4);
        List<SpritePair> pairs = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            int entryOffset = offset + index * 4;
            pairs.add(new SpritePair(
                Byte.toUnsignedInt(romData[entryOffset]),
                Byte.toUnsignedInt(romData[entryOffset + 1]),
                Byte.toUnsignedInt(romData[entryOffset + 2]),
                Byte.toUnsignedInt(romData[entryOffset + 3])));
        }
        return List.copyOf(pairs);
    }

    private int[] readUnsignedBytes(String label, int address, int length) {
        int offset = checkedOffset(label, INTRO_BANK, address, length);
        return readUnsignedBytesAt(offset, length);
    }

    private int[] readUnsignedBytesAt(int offset, int length) {
        int[] bytes = new int[length];
        for (int index = 0; index < length; index++) {
            bytes[index] = Byte.toUnsignedInt(romData[offset + index]);
        }
        return bytes;
    }

    private int checkedOffset(String label, int bank, int address, int length) {
        int offset = RomBank.romOffset(bank, address);
        if (length < 0 || offset < 0 || offset > romData.length - length) {
            throw new IllegalArgumentException(String.format(
                "%s truncated: bank $%02X address $%04X length %d (ROM length %d)",
                label, bank, address, length, romData.length));
        }
        return offset;
    }

    private int readUnsignedWord(int offset) {
        return Byte.toUnsignedInt(romData[offset])
            | (Byte.toUnsignedInt(romData[offset + 1]) << 8);
    }

    private int readBigEndianAddress(int offset) {
        return (Byte.toUnsignedInt(romData[offset]) << 8)
            | Byte.toUnsignedInt(romData[offset + 1]);
    }

    private static int signedByte(byte value) {
        return value;
    }

    public record OamEntry(int yOffset, int xOffset, int tileIndex, int attributes) {
    }

    public record SpritePair(int firstTileIndex, int firstAttributes,
                             int secondTileIndex, int secondAttributes) {
    }

    public record TitleRow(int tileTargetAddress, int attributeTargetAddress,
                           int[] tileBytes, int[] attributeBytes) {
        public TitleRow {
            tileBytes = tileBytes.clone();
            attributeBytes = attributeBytes.clone();
        }

        @Override
        public int[] tileBytes() {
            return tileBytes.clone();
        }

        @Override
        public int[] attributeBytes() {
            return attributeBytes.clone();
        }
    }

    public record PaletteBlock(int[][] rows) {
        public PaletteBlock {
            rows = copyRows(rows);
        }

        @Override
        public int[][] rows() {
            return copyRows(rows);
        }

        private static int[][] copyRows(int[][] source) {
            int[][] copy = new int[source.length][];
            for (int row = 0; row < source.length; row++) {
                copy[row] = source[row].clone();
            }
            return copy;
        }
    }
}
