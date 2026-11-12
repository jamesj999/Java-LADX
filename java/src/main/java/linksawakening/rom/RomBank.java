package linksawakening.rom;

public final class RomBank {

    public static final int BANK_SIZE = 0x4000;
    private static final int BG_MAP_ROW_WIDTH = 0x20;

    private RomBank() {}

    public static int romOffset(int bank, int address) {
        if (bank < 0) {
            throw new IllegalArgumentException("Bank cannot be negative");
        }
        if (bank == 0) {
            return address;
        }
        return bank * BANK_SIZE + (address - BANK_SIZE);
    }

    public static int decodeRgb555(int color) {
        int red = (color & 0x1F) * 255 / 31;
        int green = ((color >> 5) & 0x1F) * 255 / 31;
        int blue = ((color >> 10) & 0x1F) * 255 / 31;
        return (red << 16) | (green << 8) | blue;
    }

    /** @deprecated Use {@link #loadPalettes(Rom, RomAddress, int)}. */
    @Deprecated(forRemoval = true)
    public static int[][] loadPalettes(byte[] romData, int bank, int address, int paletteCount) {
        return loadPalettes(Rom.unchecked(romData), RomAddress.fromOffset(romOffset(bank, address)), paletteCount);
    }

    public static int[][] loadPalettes(Rom rom, RomAddress address, int paletteCount) {
        int[][] palettes = new int[paletteCount][4];
        int offset = address.offset();

        for (int paletteIndex = 0; paletteIndex < paletteCount; paletteIndex++) {
            for (int colorIndex = 0; colorIndex < 4; colorIndex++) {
                int low = rom.u8(RomAddress.fromOffset(offset++));
                int high = rom.u8(RomAddress.fromOffset(offset++));
                palettes[paletteIndex][colorIndex] = decodeRgb555(low | (high << 8));
            }
        }

        return palettes;
    }

    /** @deprecated Use {@link #decodeBackground(Rom, RomAddress, int, int)}. */
    @Deprecated(forRemoval = true)
    public static int[] decodeBackground(byte[] romData, int bank, int address, int tilemapWidth, int filler) {
        return decodeBackground(
                Rom.unchecked(romData), RomAddress.fromOffset(romOffset(bank, address)), tilemapWidth, filler);
    }

    public static int[] decodeBackground(Rom rom, RomAddress address, int tilemapWidth, int filler) {
        int[] bgMap = decodeBackgroundMap(rom, address, filler);
        int rowCount = bgMap.length / BG_MAP_ROW_WIDTH;
        int[] clamped = new int[rowCount * tilemapWidth];
        for (int row = 0; row < rowCount; row++) {
            System.arraycopy(
                    bgMap,
                    row * BG_MAP_ROW_WIDTH,
                    clamped,
                    row * tilemapWidth,
                    Math.min(tilemapWidth, BG_MAP_ROW_WIDTH));
        }
        return clamped;
    }

    /** @deprecated Use {@link #decodeBackgroundMap(Rom, RomAddress, int)}. */
    @Deprecated(forRemoval = true)
    public static int[] decodeBackgroundMap(byte[] romData, int bank, int address, int filler) {
        return decodeBackgroundMap(Rom.unchecked(romData), RomAddress.fromOffset(romOffset(bank, address)), filler);
    }

    public static int[] decodeBackgroundMap(Rom rom, RomAddress address, int filler) {
        int[] bgMap = new int[BG_MAP_ROW_WIDTH * BG_MAP_ROW_WIDTH];
        for (int i = 0; i < bgMap.length; i++) {
            bgMap[i] = filler & 0xFF;
        }

        return decodeBackgroundMap(rom, address, bgMap);
    }

    /** Apply a ROM draw-command stream while preserving untouched VRAM cells. */
    /** @deprecated Use {@link #decodeBackgroundMap(Rom, RomAddress, int[])}. */
    @Deprecated(forRemoval = true)
    public static int[] decodeBackgroundMap(byte[] romData, int bank, int address, int[] initialMap) {
        return decodeBackgroundMap(Rom.unchecked(romData), RomAddress.fromOffset(romOffset(bank, address)), initialMap);
    }

    public static int[] decodeBackgroundMap(Rom rom, RomAddress address, int[] initialMap) {
        int[] bgMap = initialMap.clone();
        int offset = address.offset();
        while (offset < rom.size()) {
            int high = rom.u8(RomAddress.fromOffset(offset));
            if (high == 0) {
                break;
            }

            int targetAddress = (high << 8) | rom.u8(RomAddress.fromOffset(offset + 1));
            int options = rom.u8(RomAddress.fromOffset(offset + 2));
            int amount = (options & 0x3F) + 1;
            boolean repeat = (options & 0x40) != 0;
            boolean vertical = (options & 0x80) != 0;
            offset += 3;

            int repeatedValue = repeat ? rom.u8(RomAddress.fromOffset(offset)) : 0;
            for (int i = 0; i < amount; i++) {
                int value = repeat ? repeatedValue : rom.u8(RomAddress.fromOffset(offset + i));
                int bgOffset = targetAddress - 0x9800;
                if (bgOffset >= 0 && bgOffset < bgMap.length) {
                    bgMap[bgOffset] = value;
                }
                targetAddress += vertical ? BG_MAP_ROW_WIDTH : 1;
            }

            offset += repeat ? 1 : amount;
        }

        return bgMap;
    }
}
