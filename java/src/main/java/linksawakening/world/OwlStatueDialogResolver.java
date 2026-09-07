package linksawakening.world;

import linksawakening.rom.RomBank;

/** OwlStatueEntityHandler and bank-$36 GetOwlStatueDialogId's ROM lookup. */
public final class OwlStatueDialogResolver {
    private static final int BANK = 0x36;
    private final byte[] romData;

    public OwlStatueDialogResolver(byte[] romData) {
        if (romData == null || romData.length < RomBank.romOffset(BANK, 0x7FFF) + 1) {
            throw new IllegalArgumentException("ROM is too small for owl statue hint tables");
        }
        this.romData = romData;
    }

    public int globalDialogId(int mapId, int roomId, boolean hasStoneBeak) {
        if ((roomId & ~0xFF) != 0 || !(mapId >= 0 && mapId < 8 || mapId == 0xFF)) {
            throw new IllegalArgumentException("Owl hint requires a dungeon map and byte room id");
        }
        if (!hasStoneBeak) return 0x110;
        if (mapId == 0xFF) return 0x263;
        int rooms = pointerAt(0x4A3C + mapId * 2);
        int index = 0;
        while (index < 3 && byteAt(rooms + index) != roomId) index++;
        // The source deliberately falls through to offset three on no match.
        return 0x200 | byteAt(pointerAt(0x4A14 + mapId * 2) + index);
    }

    private int pointerAt(int address) {
        return byteAt(address) | byteAt(address + 1) << 8;
    }

    private int byteAt(int address) {
        return Byte.toUnsignedInt(romData[RomBank.romOffset(BANK, address)]);
    }
}
