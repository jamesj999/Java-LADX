package linksawakening.world.room;

import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;

public final class RoomPaletteLoader {
    private int readRomByte(int offset) {
        return romData.u8(RomAddress.fromOffset(offset));
    }

    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_INDOORS_B_START = 0x06;
    private static final int MAP_INDOORS_B_END = 0x1A;

    private static final int INDOOR_PALETTE_MAPS_BANK = RomSymbols.IndoorPaletteMaps.bank();
    private static final int INDOOR_PALETTE_MAPS_ADDR = RomSymbols.IndoorPaletteMaps.address();
    private static final int INDOOR_PALETTE_MAPS_BASE_MAP = 0x0A;
    private static final int LAST_DUNGEON_PALETTE_MAP = 0x08;
    private static final int DUNGEON_PALETTES_A_ADDR = RomSymbols.DungeonPalettesA.address();
    private static final int DUNGEON_PALETTES_B_ADDR = RomSymbols.DungeonPalettesB.address();
    private static final int MAP_TURTLE_ROCK = 0x07;
    private static final int TURTLE_ROCK_SIDE_SCROLL_PALETTE_ADDR = RomSymbols.Data_021_6750.address();
    private static final int INTERIOR_PALETTES_BANK = RomSymbols.InteriorPalettes.bank();
    private static final int INTERIOR_PALETTES_ADDR = RomSymbols.InteriorPalettes.address();
    private static final int COLOR_DUNGEON_PALETTE_BANK = RomSymbols.Data_021_67D0.bank();
    private static final int COLOR_DUNGEON_PALETTE_ADDR = RomSymbols.Data_021_67D0.address();
    // Data_021_73B0 is the initial Marin-house palette selected by
    // LoadRoomPalettes for map $10, room $A3 before Tarin's shield event.
    private static final int MARIN_HOUSE_INITIAL_PALETTE_ADDR = RomSymbols.Data_021_73B0.address();
    private static final int MARIN_HOUSE_TRANSFORMED_PALETTE_ADDR = RomSymbols.Data_021_74A0.address();

    private static final int OVERWORLD_PALETTE_MAP_BANK = RomSymbols.OverworldPaletteMap.bank();
    private static final int OVERWORLD_PALETTE_MAP_ADDR = RomSymbols.OverworldPaletteMap.address();
    private static final int OVERWORLD_PALETTE_TABLE_BANK = RomSymbols.OverworldPalettes.bank();
    private static final int OVERWORLD_PALETTE_TABLE_ADDR = RomSymbols.OverworldPalettes.address();
    private static final int OBJECT_PALETTES_ADDR = RomSymbols.ObjectPalettes.address();
    private static final int ROOM_OBJECT_PALETTES_OFFSET = 0x40;
    private static final int EAGLES_TOWER_PALETTE_ADDR = RomSymbols.Data_021_5548.address();
    private static final int ROOM_MABE_VILLAGE_SQUARE = 0x92;
    private static final int MABE_VILLAGE_SQUARE_COLORS_ADDR = RomSymbols.Data_021_56C8.address();

    private final Rom romData;

    public RoomPaletteLoader(Rom romData) {
        this.romData = romData;
    }

    public int[][] loadOverworld(int roomId) {
        return loadPaletteBlock(overworldPaletteOffset(roomId));
    }

    public int[][] loadOverworldObjectPalettes(int roomId) {
        int[][] palettes = composeObjectPalettes(overworldPaletteOffset(roomId));
        if (roomId == 0x0E) {
            palettes[5] =
                    loadPalette(RomBank.romOffset(RomSymbols.OverworldPalettes.bank(), EAGLES_TOWER_PALETTE_ADDR));
        }
        if (roomId == ROOM_MABE_VILLAGE_SQUARE) {
            int[] source =
                    loadPalette(RomBank.romOffset(OVERWORLD_PALETTE_TABLE_BANK, MABE_VILLAGE_SQUARE_COLORS_ADDR));
            palettes[7][1] = source[3];
            palettes[7][2] = source[1];
            palettes[7][3] = source[0];
        }
        return palettes;
    }

    public int[][] loadIndoor(int mapId, int roomId, int[][] fallbackPalettes) {
        return loadIndoor(mapId, roomId, fallbackPalettes, 0);
    }

    public int[][] loadIndoor(int mapId, int roomId, int[][] fallbackPalettes, int tarinFlag) {
        return loadIndoor(mapId, roomId, fallbackPalettes, tarinFlag, 0);
    }

    public int[][] loadIndoor(int mapId, int roomId, int[][] fallbackPalettes, int tarinFlag, int shieldLevel) {
        return loadIndoor(mapId, roomId, fallbackPalettes, tarinFlag, shieldLevel, false);
    }

    public int[][] loadIndoor(
            int mapId, int roomId, int[][] fallbackPalettes, int tarinFlag, int shieldLevel, boolean sideScrolling) {
        int offset = indoorPaletteOffset(mapId, roomId, sideScrolling, tarinFlag, shieldLevel);
        return offset < 0 ? fallbackPalettes : loadPaletteBlock(offset);
    }

    public record TransitionPalettes(int[][] background, int[][] objects) {}

    /** $21:5185 and $21:425E: room/direction selectors update BG or OBJ 6/7. */
    public TransitionPalettes indoorTransition(
            int mapId,
            int roomId,
            int direction,
            int[][] baseBackground,
            int[][] baseObjects,
            int[][] previousBackground,
            int[][] previousObjects) {
        boolean entering = direction == 4;
        int[][] background = copyPalettes(entering || previousBackground == null ? baseBackground : previousBackground);
        int[][] objects = copyPalettes(entering || previousObjects == null ? baseObjects : previousObjects);
        int records = RomSymbols.IndoorSpritePaletteIndexData.offset();
        for (int i = 0; i < 0x2D; i++) {
            int record = records + i * 4;
            if ((readRomByte(record)) != mapId
                    || (readRomByte(record + 1)) != roomId
                    || (readRomByte(record + 2)) != direction) continue;
            int selector = readRomByte(record + 3);
            if ((selector & 0x80) == 0) break;
            int pointer = readPointer(
                    RomSymbols.TilemapPaletteTable.plus((selector & 0x3F) * 2).offset());
            int offset = RomBank.romOffset(RomSymbols.OverworldPalettes.bank(), pointer);
            if (entering || (selector & 0x40) == 0) background = loadPaletteBlock(offset);
            if (entering || (selector & 0x40) != 0) {
                objects[6] = loadPalette(offset + 0x40);
                objects[7] = loadPalette(offset + 0x48);
            }
            break;
        }
        return new TransitionPalettes(background, objects);
    }

    private static int[][] copyPalettes(int[][] source) {
        if (source == null) return null;
        int[][] result = new int[source.length][];
        for (int i = 0; i < source.length; i++) result[i] = source[i].clone();
        return result;
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

    public int[][] loadIndoorObjectPalettes(int mapId, int roomId, boolean sideScrolling, int tarinFlag) {
        return loadIndoorObjectPalettes(mapId, roomId, sideScrolling, tarinFlag, 0);
    }

    public int[][] loadIndoorObjectPalettes(
            int mapId, int roomId, boolean sideScrolling, int tarinFlag, int shieldLevel) {
        // palettes.asm also rewrites indoor room $AA according to wTunicType.
        // Preserve the ROM base row until tunic state is part of this loader's inputs.
        int paletteOffset = indoorPaletteOffset(mapId, roomId, sideScrolling, tarinFlag, shieldLevel);
        return paletteOffset < 0 ? loadGlobalObjectPalettes() : composeObjectPalettes(paletteOffset);
    }

    private int overworldPaletteOffset(int roomId) {
        int map = RomBank.romOffset(OVERWORLD_PALETTE_MAP_BANK, OVERWORLD_PALETTE_MAP_ADDR);
        int paletteIndex = readRomByte(map + roomId);
        int table = RomBank.romOffset(OVERWORLD_PALETTE_TABLE_BANK, OVERWORLD_PALETTE_TABLE_ADDR);
        return RomBank.romOffset(RomSymbols.OverworldPalettes.bank(), readPointer(table + paletteIndex * 2));
    }

    private int indoorPaletteOffset(int mapId, int roomId, boolean sideScrolling, int tarinFlag, int shieldLevel) {
        if (mapId == MAP_COLOR_DUNGEON) {
            return RomBank.romOffset(COLOR_DUNGEON_PALETTE_BANK, COLOR_DUNGEON_PALETTE_ADDR);
        }
        if (mapId == 0x10 && roomId == 0xA3 && usesMarinHouseSpecialPalette(tarinFlag, shieldLevel)) {
            return RomBank.romOffset(INTERIOR_PALETTES_BANK, marinHousePaletteAddress(tarinFlag));
        }
        if (mapId >= 0 && mapId <= LAST_DUNGEON_PALETTE_MAP) {
            if (sideScrolling && mapId == MAP_TURTLE_ROCK && usesTurtleRockSideScrollPalette(roomId)) {
                return RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, TURTLE_ROCK_SIDE_SCROLL_PALETTE_ADDR);
            }
            int tableAddress = sideScrolling ? DUNGEON_PALETTES_B_ADDR : DUNGEON_PALETTES_A_ADDR;
            int table = RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, tableAddress + mapId * 2);
            return RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, readPointer(table));
        }
        if (mapId < INDOOR_PALETTE_MAPS_BASE_MAP) {
            return -1;
        }
        int mapsEntry = RomBank.romOffset(
                INDOOR_PALETTE_MAPS_BANK, INDOOR_PALETTE_MAPS_ADDR + (mapId - INDOOR_PALETTE_MAPS_BASE_MAP) * 2);
        int mapPointer = readPointer(mapsEntry);
        if (mapPointer == 0) {
            return -1;
        }
        int paletteIndex = readRomByte(RomBank.romOffset(INDOOR_PALETTE_MAPS_BANK, mapPointer + roomId));
        int palettePointer =
                readPointer(RomBank.romOffset(INTERIOR_PALETTES_BANK, INTERIOR_PALETTES_ADDR + paletteIndex * 2));
        return palettePointer == 0 ? -1 : RomBank.romOffset(INTERIOR_PALETTES_BANK, palettePointer);
    }

    private static int marinHousePaletteAddress(int tarinFlag) {
        return tarinFlag == 1 || tarinFlag == 2
                ? MARIN_HOUSE_TRANSFORMED_PALETTE_ADDR
                : MARIN_HOUSE_INITIAL_PALETTE_ADDR;
    }

    private static boolean usesMarinHouseSpecialPalette(int tarinFlag, int shieldLevel) {
        return tarinFlag == 1 || tarinFlag == 2 || shieldLevel == 0;
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
        int offset = RomBank.romOffset(RomSymbols.OverworldPalettes.bank(), OBJECT_PALETTES_ADDR);
        for (int palette = 0; palette < palettes.length; palette++) {
            palettes[palette] = loadPalette(offset + palette * 8);
        }
        return palettes;
    }

    private int[] loadPalette(int offset) {
        int[] palette = new int[4];
        for (int color = 0; color < palette.length; color++) {
            int encoded = readRomByte(offset++) | (readRomByte(offset++) << 8);
            palette[color] = RomBank.decodeRgb555(encoded);
        }
        return palette;
    }

    private int readPointer(int offset) {
        return readRomByte(offset) | (readRomByte(offset + 1) << 8);
    }

    private int[][] loadPaletteBlock(int paletteOffset) {
        int[][] palettes = new int[8][4];
        for (int p = 0; p < 8; p++) {
            for (int c = 0; c < 4; c++) {
                int colorOffset = paletteOffset + (p * 4 + c) * 2;
                if (colorOffset + 1 < romData.size()) {
                    int low = readRomByte(colorOffset);
                    int high = readRomByte(colorOffset + 1);
                    palettes[p][c] = RomBank.decodeRgb555(low | (high << 8));
                }
            }
        }
        return palettes;
    }

    /** @deprecated Pass Rom; retained for main/java/linksawakening/entity/EntitySpriteCatalog.java. Legacy partial buffers retain their validation timing. */
    @Deprecated(forRemoval = true)
    public RoomPaletteLoader(byte[] romData) {
        this(romData == null ? null : new Rom(romData));
    }
}
