package linksawakening.world;

import linksawakening.rom.RomBank;

public final class RoomPaletteLoader {
    private static final int MAP_INDOORS_B_START = 0x06;
    private static final int MAP_INDOORS_B_END = 0x1A;

    private static final int INDOOR_PALETTE_MAPS_BANK = 0x21;
    private static final int INDOOR_PALETTE_MAPS_ADDR = 0x4413;
    private static final int INDOOR_PALETTE_MAPS_BASE_MAP = 0x0A;
    private static final int INTERIOR_PALETTES_BANK = 0x21;
    private static final int INTERIOR_PALETTES_ADDR = 0x443F;

    private static final int OVERWORLD_PALETTE_MAP_BANK = 0x21;
    private static final int OVERWORLD_PALETTE_MAP_ADDR = 0x42EF;
    private static final int OVERWORLD_PALETTE_TABLE_BANK = 0x21;
    private static final int OVERWORLD_PALETTE_TABLE_ADDR = 0x42B1;

    private final byte[] romData;

    public RoomPaletteLoader(byte[] romData) {
        this.romData = romData;
    }

    public int[][] loadOverworld(int roomId) {
        int paletteMapOffset = RomBank.romOffset(OVERWORLD_PALETTE_MAP_BANK, OVERWORLD_PALETTE_MAP_ADDR);
        int paletteIndex = Byte.toUnsignedInt(romData[paletteMapOffset + roomId]);

        int paletteTableOffset = RomBank.romOffset(OVERWORLD_PALETTE_TABLE_BANK, OVERWORLD_PALETTE_TABLE_ADDR);
        int paletteOffset = paletteTableOffset + paletteIndex * 2;
        int palettePtrLo = Byte.toUnsignedInt(romData[paletteOffset]);
        int palettePtrHi = Byte.toUnsignedInt(romData[paletteOffset + 1]);
        int paletteAddr = (palettePtrHi << 8) | palettePtrLo;

        return loadPaletteBlock(RomBank.romOffset(0x21, paletteAddr));
    }

    public int[][] loadIndoor(int mapId, int roomId, int[][] fallbackPalettes) {
        if (mapId < INDOOR_PALETTE_MAPS_BASE_MAP) {
            return fallbackPalettes;
        }

        int mapsEntryOffset = RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK,
            INDOOR_PALETTE_MAPS_ADDR + (mapId - INDOOR_PALETTE_MAPS_BASE_MAP) * 2);
        int mapPtrLo = Byte.toUnsignedInt(romData[mapsEntryOffset]);
        int mapPtrHi = Byte.toUnsignedInt(romData[mapsEntryOffset + 1]);
        int mapPtr = (mapPtrHi << 8) | mapPtrLo;
        if (mapPtr == 0) {
            return fallbackPalettes;
        }

        int palIdxOffset = RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, mapPtr + roomId);
        int palIdx = Byte.toUnsignedInt(romData[palIdxOffset]);

        int interiorPtrOffset = RomBank.romOffset(INTERIOR_PALETTES_BANK, INTERIOR_PALETTES_ADDR + palIdx * 2);
        int interiorPtrLo = Byte.toUnsignedInt(romData[interiorPtrOffset]);
        int interiorPtrHi = Byte.toUnsignedInt(romData[interiorPtrOffset + 1]);
        int interiorPtr = (interiorPtrHi << 8) | interiorPtrLo;
        if (interiorPtr == 0) {
            return fallbackPalettes;
        }

        return loadPaletteBlock(RomBank.romOffset(INTERIOR_PALETTES_BANK, interiorPtr));
    }

    public static boolean isIndoorMapB(int mapId) {
        return mapId >= MAP_INDOORS_B_START && mapId < MAP_INDOORS_B_END;
    }

    private int[][] loadPaletteBlock(int paletteOffset) {
        int[][] palettes = new int[8][4];
        for (int p = 0; p < 8; p++) {
            for (int c = 0; c < 4; c++) {
                int colorOffset = paletteOffset + (p * 4 + c) * 2;
                if (colorOffset + 1 < romData.length) {
                    int low = Byte.toUnsignedInt(romData[colorOffset]);
                    int high = Byte.toUnsignedInt(romData[colorOffset + 1]);
                    palettes[p][c] = RomBank.decodeRgb555(low | (high << 8));
                }
            }
        }
        return palettes;
    }
}
