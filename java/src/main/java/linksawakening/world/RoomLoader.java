package linksawakening.world;

import linksawakening.rom.RomBank;

public final class RoomLoader {
    private static final int MAP_OVERWORLD = 0x00;

    private static final int OVERWORLD_ROOM_BANK = 0x09;
    private static final int OVERWORLD_ROOM_BANK_SECOND_HALF = 0x1A;
    private static final int OVERWORLD_ROOM_POINTERS_ADDR = 0x4000;
    private static final int ROOM_SECTION_OW_SECOND_HALF = 0x80;

    private final byte[] romData;
    private final RoomObjectParser parser;
    private final RoomTilemapBuilder tilemapBuilder;
    private final RoomPaletteLoader paletteLoader;

    public RoomLoader(byte[] romData) {
        this.romData = romData;
        this.parser = new RoomObjectParser(romData);
        this.tilemapBuilder = new RoomTilemapBuilder(romData);
        this.paletteLoader = new RoomPaletteLoader(romData);
    }

    public LoadedRoom loadOverworld(int roomId) {
        int roomPointerOffset = RomBank.romOffset(OVERWORLD_ROOM_BANK, OVERWORLD_ROOM_POINTERS_ADDR + roomId * 2);
        int roomLo = Byte.toUnsignedInt(romData[roomPointerOffset]);
        int roomHi = Byte.toUnsignedInt(romData[roomPointerOffset + 1]);
        int roomAddr = (roomHi << 8) | roomLo;

        int roomDataBank = roomId >= ROOM_SECTION_OW_SECOND_HALF
            ? OVERWORLD_ROOM_BANK_SECOND_HALF
            : OVERWORLD_ROOM_BANK;
        int roomDataOffset = RomBank.romOffset(roomDataBank, roomAddr);

        int animatedTilesGroup = Byte.toUnsignedInt(romData[roomDataOffset]);
        int floorObject = Byte.toUnsignedInt(romData[roomDataOffset + 1]);
        RoomObjectParseResult parsed = parser.parseOverworld(roomDataOffset + 2, floorObject, 0);
        int[] objects = parsed.roomObjectsArea();
        RoomTilemap tilemap = tilemapBuilder.buildOverworld(roomId, objects);

        return new LoadedRoom(
            roomId,
            Warp.CATEGORY_OVERWORLD,
            MAP_OVERWORLD,
            animatedTilesGroup,
            objects,
            tilemap.gbcOverlay(),
            tilemap.renderValues(),
            tilemap.tileIds(),
            tilemap.tileAttrs(),
            paletteLoader.loadOverworld(roomId),
            parsed.warps(),
            false
        );
    }

    public LoadedRoom loadIndoor(int mapId, int roomId, int[][] fallbackPalettes) {
        return loadIndoor(mapId, roomId, fallbackPalettes, Warp.CATEGORY_INDOOR);
    }

    public LoadedRoom loadIndoor(int mapId, int roomId, int[][] fallbackPalettes, int mapCategory) {
        RoomPointerTable pointerTable = IndoorRoomPointerTables.forMap(mapId);
        int roomPointerOffset = RomBank.romOffset(pointerTable.bank(), pointerTable.address() + roomId * 2);
        int roomLo = Byte.toUnsignedInt(romData[roomPointerOffset]);
        int roomHi = Byte.toUnsignedInt(romData[roomPointerOffset + 1]);
        int roomAddr = (roomHi << 8) | roomLo;
        int roomDataOffset = RomBank.romOffset(pointerTable.bank(), roomAddr);

        int animatedTilesGroup = Byte.toUnsignedInt(romData[roomDataOffset]);
        int floorAndTemplate = Byte.toUnsignedInt(romData[roomDataOffset + 1]);
        RoomObjectParseResult parsed = parser.parseIndoor(roomDataOffset + 2, floorAndTemplate);
        int[] objects = parsed.roomObjectsArea();
        RoomTilemap tilemap = tilemapBuilder.buildIndoor(mapId, roomId, objects);

        return new LoadedRoom(
            roomId,
            mapCategory,
            mapId,
            animatedTilesGroup,
            objects,
            null,
            null,
            tilemap.tileIds(),
            tilemap.tileAttrs(),
            paletteLoader.loadIndoor(mapId, roomId, fallbackPalettes),
            parsed.warps(),
            hasSouthEntrance(objects)
        );
    }

    private boolean hasSouthEntrance(int[] roomObjectsArea) {
        for (int col = 0; col < RoomConstants.OBJECTS_PER_ROW; col++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + 7 * RoomConstants.ROOM_OBJECT_ROW_STRIDE + col;
            int id = roomObjectsArea[areaIndex];
            if (id == 0xC1 || id == 0xC2) {
                return true;
            }
        }
        return false;
    }
}
