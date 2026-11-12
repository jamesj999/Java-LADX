package linksawakening.rom;

import java.util.HashMap;
import java.util.Map;

public final class RomUtil {

    public static final int BANK_SIZE = RomBank.BANK_SIZE;
    public static final int BG_MAP_ROW_WIDTH = 0x20;

    private RomUtil() {}

    public static int[][] loadPalettesFromRom(Rom rom, RomAddress address, int paletteCount) {
        return RomBank.loadPalettes(rom, address, paletteCount);
    }

    public static int[] decodeBackgroundFromRom(Rom rom, RomAddress address, int tilemapWidth, int filler) {
        int offset = address.offset();
        Map<Integer, Integer> bytesByAddress = new HashMap<>();

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
