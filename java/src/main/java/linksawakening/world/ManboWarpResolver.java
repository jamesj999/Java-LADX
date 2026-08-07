package linksawakening.world;

import linksawakening.rom.RomBank;

/** Resolves the five-byte destination copied by {@code TeleportToManboPond}. */
public final class ManboWarpResolver {
    private static final int MANBO_WARP_TABLE_BANK = 0x14;
    private static final int MANBO_WARP_TABLE_ADDRESS = 0x4DF1;
    private static final int MANBO_WARP_TABLE_ENTRIES = 0x10;
    private static final int MANBO_WARP_RECORD_SIZE = 5;
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int FIRST_UNSUPPORTED_INDOOR_MAP = 0x0A;

    private final byte[] romData;

    public ManboWarpResolver(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
    }

    /**
     * Mirrors the source's {@code wIsIndoor}/{@code hMapId} branches. Indoor
     * maps below {@code $0A} use the ROM table; Color Dungeon uses its low
     * nibble, including the source's zero-filled records. Every other case
     * receives the fixed overworld pond warp.
     */
    public Warp resolve(int mapCategory, int mapId) {
        int normalizedMapId = mapId & 0xFF;
        if (mapCategory != Warp.CATEGORY_OVERWORLD
            && (normalizedMapId == MAP_COLOR_DUNGEON
                || normalizedMapId < FIRST_UNSUPPORTED_INDOOR_MAP)) {
            int tableIndex = normalizedMapId & 0x0F;
            if (tableIndex >= MANBO_WARP_TABLE_ENTRIES) {
                throw new IllegalStateException("Manbo warp table index out of range: "
                    + tableIndex);
            }
            int source = RomBank.romOffset(MANBO_WARP_TABLE_BANK,
                MANBO_WARP_TABLE_ADDRESS + tableIndex * MANBO_WARP_RECORD_SIZE);
            return new Warp(
                readByte(source),
                readByte(source + 1),
                readByte(source + 2),
                readByte(source + 3),
                readByte(source + 4),
                -1);
        }
        return new Warp(Warp.CATEGORY_OVERWORLD, 0x00, 0x45, 0x38, 0x60, 0x53);
    }

    private int readByte(int offset) {
        if (offset < 0 || offset >= romData.length) {
            throw new IllegalArgumentException("Manbo warp table exceeds ROM bounds");
        }
        return Byte.toUnsignedInt(romData[offset]);
    }
}
