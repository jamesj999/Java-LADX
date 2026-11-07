package linksawakening.rom;

import java.util.HashMap;
import java.util.Map;

public final class RomUtil {

    public static final int BANK_SIZE = 0x4000;
    public static final int BG_MAP_ROW_WIDTH = 0x20;

    private RomUtil() {
    }

    public static int romOffset(int bank, int address) {
        return RomBank.romOffset(bank, address);
    }

    public static int decodeRgb555(int color) {
        int red = (color & 0x1F) * 255 / 31;
        int green = ((color >> 5) & 0x1F) * 255 / 31;
        int blue = ((color >> 10) & 0x1F) * 255 / 31;
        return (red << 16) | (green << 8) | blue;
    }

    public static int[][] loadPalettesFromRom(byte[] romData, int bank, int address, int paletteCount) {
        int[][] palettes = new int[paletteCount][4];
        int offset = romOffset(bank, address);

        for (int paletteIndex = 0; paletteIndex < paletteCount; paletteIndex++) {
            for (int colorIndex = 0; colorIndex < 4; colorIndex++) {
                int low = new Rom(romData).u8(RomAddress.fromOffset(offset++));
                int high = new Rom(romData).u8(RomAddress.fromOffset(offset++));
                palettes[paletteIndex][colorIndex] = decodeRgb555(low | (high << 8));
            }
        }

        return palettes;
    }

    public static int[] decodeBackgroundFromRom(byte[] romData, int bank, int address, int tilemapWidth, int filler) {
        int offset = romOffset(bank, address);
        Map<Integer, Integer> bytesByAddress = new HashMap<>();

        while (offset < romData.length) {
            int high = new Rom(romData).u8(RomAddress.fromOffset(offset));
            if (high == 0) {
                break;
            }

            int targetAddress = (high << 8) | new Rom(romData).u8(RomAddress.fromOffset(offset + 1));
            int options = new Rom(romData).u8(RomAddress.fromOffset(offset + 2));
            int amount = (options & 0x3F) + 1;
            boolean repeat = (options & 0x40) != 0;
            boolean vertical = (options & 0x80) != 0;
            offset += 3;

            int repeatedValue = repeat ? new Rom(romData).u8(RomAddress.fromOffset(offset)) : 0;
            for (int i = 0; i < amount; i++) {
                int value = repeat ? repeatedValue : new Rom(romData).u8(RomAddress.fromOffset(offset + i));
                bytesByAddress.put(targetAddress, value);
                targetAddress += vertical ? BG_MAP_ROW_WIDTH : 1;
            }

            offset += repeat ? 1 : amount;
        }

        if (bytesByAddress.isEmpty()) {
            return new int[0];
        }

        int minAddress = Integer.MAX_VALUE;
        int maxAddress = Integer.MIN_VALUE;
        for (int addressKey : bytesByAddress.keySet()) {
            minAddress = Math.min(minAddress, addressKey);
            maxAddress = Math.max(maxAddress, addressKey);
        }

        int[] bgRam = new int[maxAddress - minAddress + 1];
        for (int i = 0; i < bgRam.length; i++) {
            bgRam[i] = filler;
        }
        for (Map.Entry<Integer, Integer> entry : bytesByAddress.entrySet()) {
            bgRam[entry.getKey() - minAddress] = entry.getValue();
        }

        int rowCount = (bgRam.length + BG_MAP_ROW_WIDTH - 1) / BG_MAP_ROW_WIDTH;
        int[] clamped = new int[rowCount * tilemapWidth];
        for (int row = 0; row < rowCount; row++) {
            int sourceBase = row * BG_MAP_ROW_WIDTH;
            int targetBase = row * tilemapWidth;
            int copyLength = Math.min(tilemapWidth, Math.max(0, bgRam.length - sourceBase));
            System.arraycopy(bgRam, sourceBase, clamped, targetBase, copyLength);
        }

        return clamped;
    }
}
