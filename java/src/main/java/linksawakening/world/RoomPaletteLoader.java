package linksawakening.world;

import linksawakening.rom.RomBank;

public final class RoomPaletteLoader {
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_INDOORS_B_START = 0x06;
    private static final int MAP_INDOORS_B_END = 0x1A;

    private static final int INDOOR_PALETTE_MAPS_BANK = 0x21;
    private static final int INDOOR_PALETTE_MAPS_ADDR = 0x4413;
    private static final int INDOOR_PALETTE_MAPS_BASE_MAP = 0x0A;
    private static final int LAST_DUNGEON_PALETTE_MAP = 0x08;
    private static final int DUNGEON_PALETTES_A_ADDR = 0x43EF;
    private static final int DUNGEON_PALETTES_B_ADDR = 0x4401;
    private static final int MAP_TURTLE_ROCK = 0x07;
    private static final int TURTLE_ROCK_SIDE_SCROLL_PALETTE_ADDR = 0x6750;
    private static final int INTERIOR_PALETTES_BANK = 0x21;
    private static final int INTERIOR_PALETTES_ADDR = 0x443F;
    private static final int COLOR_DUNGEON_PALETTE_BANK = 0x21;
    private static final int COLOR_DUNGEON_PALETTE_ADDR = 0x67D0;
    // Data_021_73B0 is the initial Marin-house palette selected by
    // LoadRoomPalettes for map $10, room $A3 before Tarin's shield event.
    private static final int MARIN_HOUSE_INITIAL_PALETTE_ADDR = 0x73B0;
    private static final int MARIN_HOUSE_TRANSFORMED_PALETTE_ADDR = 0x74A0;

    private static final int OVERWORLD_PALETTE_MAP_BANK = 0x21;
    private static final int OVERWORLD_PALETTE_MAP_ADDR = 0x42EF;
    private static final int OVERWORLD_PALETTE_TABLE_BANK = 0x21;
    private static final int OVERWORLD_PALETTE_TABLE_ADDR = 0x42B1;
    private static final int OBJECT_PALETTES_ADDR = 0x5518;
    private static final int ROOM_OBJECT_PALETTES_OFFSET = 0x40;
    private static final int EAGLES_TOWER_PALETTE_ADDR = 0x5548;
    private static final int ROOM_MABE_VILLAGE_SQUARE = 0x92;
    private static final int MABE_VILLAGE_SQUARE_COLORS_ADDR = 0x56C8;

    private final byte[] romData;

    public RoomPaletteLoader(byte[] romData) {
        this.romData = romData;
    }

    public int[][] loadOverworld(int roomId) {
        return loadPaletteBlock(overworldPaletteOffset(roomId));
    }

    public int[][] loadOverworldObjectPalettes(int roomId) {
        int[][] palettes = composeObjectPalettes(overworldPaletteOffset(roomId));
        if (roomId == 0x0E) {
            palettes[5] = loadPalette(RomBank.romOffset(0x21, EAGLES_TOWER_PALETTE_ADDR));
        }
        if (roomId == ROOM_MABE_VILLAGE_SQUARE) {
            int[] source = loadPalette(RomBank.romOffset(
                OVERWORLD_PALETTE_TABLE_BANK, MABE_VILLAGE_SQUARE_COLORS_ADDR));
            palettes[7][1] = source[3];
            palettes[7][2] = source[1];
            palettes[7][3] = source[0];
        }
        return palettes;
    }

    public int[][] loadIndoor(int mapId, int roomId, int[][] fallbackPalettes) {
        return loadIndoor(mapId, roomId, fallbackPalettes, 0);
    }

    public int[][] loadIndoor(int mapId, int roomId, int[][] fallbackPalettes,
                              int tarinFlag) {
        if (mapId == MAP_COLOR_DUNGEON) {
            return loadPaletteBlock(RomBank.romOffset(
                COLOR_DUNGEON_PALETTE_BANK, COLOR_DUNGEON_PALETTE_ADDR));
        }
        if (mapId < INDOOR_PALETTE_MAPS_BASE_MAP) {
            return fallbackPalettes;
        }
        if (mapId == 0x10 && roomId == 0xA3) {
            return loadPaletteBlock(RomBank.romOffset(INTERIOR_PALETTES_BANK,
                marinHousePaletteAddress(tarinFlag)));
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

    public int[][] loadIndoorObjectPalettes(int mapId, int roomId) {
        return loadIndoorObjectPalettes(mapId, roomId, false);
    }

    public int[][] loadIndoorObjectPalettes(int mapId, int roomId, boolean sideScrolling) {
        return loadIndoorObjectPalettes(mapId, roomId, sideScrolling, 0);
    }

    public int[][] loadIndoorObjectPalettes(int mapId, int roomId, boolean sideScrolling,
                                            int tarinFlag) {
        // palettes.asm also rewrites indoor room $AA according to wTunicType.
        // Preserve the ROM base row until tunic state is part of this loader's context.
        int paletteOffset = indoorPaletteOffset(mapId, roomId, sideScrolling, tarinFlag);
        return paletteOffset < 0 ? loadGlobalObjectPalettes() : composeObjectPalettes(paletteOffset);
    }

    private int overworldPaletteOffset(int roomId) {
        int map = RomBank.romOffset(OVERWORLD_PALETTE_MAP_BANK, OVERWORLD_PALETTE_MAP_ADDR);
        int paletteIndex = Byte.toUnsignedInt(romData[map + roomId]);
        int table = RomBank.romOffset(OVERWORLD_PALETTE_TABLE_BANK, OVERWORLD_PALETTE_TABLE_ADDR);
        return RomBank.romOffset(0x21, readPointer(table + paletteIndex * 2));
    }

    private int indoorPaletteOffset(int mapId, int roomId, boolean sideScrolling,
                                    int tarinFlag) {
        if (mapId == MAP_COLOR_DUNGEON) {
            return RomBank.romOffset(COLOR_DUNGEON_PALETTE_BANK, COLOR_DUNGEON_PALETTE_ADDR);
        }
        if (mapId == 0x10 && roomId == 0xA3) {
            return RomBank.romOffset(INTERIOR_PALETTES_BANK,
                marinHousePaletteAddress(tarinFlag));
        }
        if (mapId >= 0 && mapId <= LAST_DUNGEON_PALETTE_MAP) {
            if (sideScrolling && mapId == MAP_TURTLE_ROCK
                && usesTurtleRockSideScrollPalette(roomId)) {
                return RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK,
                    TURTLE_ROCK_SIDE_SCROLL_PALETTE_ADDR);
            }
            int tableAddress = sideScrolling
                ? DUNGEON_PALETTES_B_ADDR : DUNGEON_PALETTES_A_ADDR;
            int table = RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK,
                tableAddress + mapId * 2);
            return RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, readPointer(table));
        }
        if (mapId < INDOOR_PALETTE_MAPS_BASE_MAP) {
            return -1;
        }
        int mapsEntry = RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK,
            INDOOR_PALETTE_MAPS_ADDR + (mapId - INDOOR_PALETTE_MAPS_BASE_MAP) * 2);
        int mapPointer = readPointer(mapsEntry);
        if (mapPointer == 0) {
            return -1;
        }
        int paletteIndex = Byte.toUnsignedInt(romData[
            RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, mapPointer + roomId)]);
        int palettePointer = readPointer(RomBank.romOffset(INTERIOR_PALETTES_BANK,
            INTERIOR_PALETTES_ADDR + paletteIndex * 2));
        return palettePointer == 0 ? -1 : RomBank.romOffset(INTERIOR_PALETTES_BANK, palettePointer);
    }

    private static int marinHousePaletteAddress(int tarinFlag) {
        return tarinFlag == 1 || tarinFlag == 2
            ? MARIN_HOUSE_TRANSFORMED_PALETTE_ADDR
            : MARIN_HOUSE_INITIAL_PALETTE_ADDR;
    }

    private static boolean usesTurtleRockSideScrollPalette(int roomId) {
        return roomId >= 0x64 && roomId <= 0x67 || roomId == 0x6A || roomId == 0x6B;
    }

    private int[][] composeObjectPalettes(int roomPaletteOffset) {
        int[][] palettes = new int[8][];
        int[][] globals = loadGlobalObjectPalettes();
        System.arraycopy(globals, 0, palettes, 0, globals.length);
        palettes[6] = loadPalette(roomPaletteOffset + ROOM_OBJECT_PALETTES_OFFSET);
        palettes[7] = loadPalette(roomPaletteOffset + ROOM_OBJECT_PALETTES_OFFSET + 8);
        return palettes;
    }

    private int[][] loadGlobalObjectPalettes() {
        int[][] palettes = new int[6][];
        int offset = RomBank.romOffset(0x21, OBJECT_PALETTES_ADDR);
        for (int palette = 0; palette < palettes.length; palette++) {
            palettes[palette] = loadPalette(offset + palette * 8);
        }
        return palettes;
    }

    private int[] loadPalette(int offset) {
        int[] palette = new int[4];
        for (int color = 0; color < palette.length; color++) {
            int encoded = Byte.toUnsignedInt(romData[offset++])
                | (Byte.toUnsignedInt(romData[offset++]) << 8);
            palette[color] = RomBank.decodeRgb555(encoded);
        }
        return palette;
    }

    private int readPointer(int offset) {
        return Byte.toUnsignedInt(romData[offset])
            | (Byte.toUnsignedInt(romData[offset + 1]) << 8);
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
