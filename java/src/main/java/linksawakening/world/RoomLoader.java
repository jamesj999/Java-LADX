package linksawakening.world;

import linksawakening.entity.EntitySpriteCatalog;
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
    private final EntityRoomLoader entityLoader;
    private final EntitySpriteCatalog entitySpriteCatalog;

    public RoomLoader(byte[] romData) {
        this.romData = romData;
        this.parser = new RoomObjectParser(romData);
        this.tilemapBuilder = new RoomTilemapBuilder(romData);
        this.paletteLoader = new RoomPaletteLoader(romData);
        this.entityLoader = new EntityRoomLoader(romData);
        this.entitySpriteCatalog = new EntitySpriteCatalog(romData);
    }

    public LoadedRoom loadOverworld(int roomId) {
        return loadOverworld(roomId, 0);
    }

    LoadedRoom loadOverworld(int roomId, int clearedEntitiesMask) {
        return loadOverworld(roomId, clearedEntitiesMask, null);
    }

    LoadedRoom loadOverworld(int roomId, int clearedEntitiesMask,
                             byte[] overworldRoomStatus) {
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
        int roomStatusFlags = overworldRoomStatus == null || roomId < 0
            || roomId >= overworldRoomStatus.length
            ? 0 : Byte.toUnsignedInt(overworldRoomStatus[roomId]);
        RoomObjectParseResult parsed = parser.parseOverworld(
            roomDataOffset + 2, floorObject, roomStatusFlags);
        int[] objects = parsed.roomObjectsArea();
        RoomTilemap tilemap = tilemapBuilder.buildOverworld(roomId, objects);
        RoomEntitySnapshot entities = loadEntities(EntityRoomLoader.RoomTable.OVERWORLD,
            roomId, clearedEntitiesMask, MAP_OVERWORLD, overworldRoomStatus);

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
            false,
            entities
        );
    }

    public LoadedRoom loadIndoor(int mapId, int roomId, int[][] fallbackPalettes) {
        return loadIndoor(mapId, roomId, fallbackPalettes, Warp.CATEGORY_INDOOR);
    }

    public LoadedRoom loadIndoor(int mapId, int roomId, int[][] fallbackPalettes, int mapCategory) {
        return loadIndoor(mapId, roomId, fallbackPalettes, mapCategory, 0);
    }

    LoadedRoom loadIndoor(int mapId, int roomId, int[][] fallbackPalettes, int mapCategory,
                          int clearedEntitiesMask) {
        return loadIndoor(mapId, roomId, fallbackPalettes, mapCategory,
            clearedEntitiesMask, null);
    }

    LoadedRoom loadIndoor(int mapId, int roomId, int[][] fallbackPalettes, int mapCategory,
                          int clearedEntitiesMask, byte[] indoorRoomStatus) {
        RoomPointerTable pointerTable = IndoorRoomPointerTables.forMap(mapId);
        int roomPointerOffset = RomBank.romOffset(pointerTable.bank(), pointerTable.address() + roomId * 2);
        int roomLo = Byte.toUnsignedInt(romData[roomPointerOffset]);
        int roomHi = Byte.toUnsignedInt(romData[roomPointerOffset + 1]);
        int roomAddr = (roomHi << 8) | roomLo;
        int roomDataOffset = RomBank.romOffset(pointerTable.bank(), roomAddr);

        int animatedTilesGroup = Byte.toUnsignedInt(romData[roomDataOffset]);
        int floorAndTemplate = Byte.toUnsignedInt(romData[roomDataOffset + 1]);
        int roomStatusFlags = indoorRoomStatus == null || roomId < 0
            || roomId >= indoorRoomStatus.length
            ? 0 : Byte.toUnsignedInt(indoorRoomStatus[roomId]);
        RoomObjectParseResult parsed = parser.parseIndoor(
            roomDataOffset + 2, floorAndTemplate, roomStatusFlags, mapId);
        int[] objects = parsed.roomObjectsArea();
        RoomTilemap tilemap = tilemapBuilder.buildIndoor(mapId, roomId, objects);
        EntityRoomLoader.RoomTable entityTable = entityTableForIndoorMap(mapId);
        RoomEntitySnapshot entities = loadEntities(entityTable, roomId, clearedEntitiesMask,
            mapId, null);

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
            hasSouthEntrance(objects),
            entities
        );
    }

    private RoomEntitySnapshot loadEntities(EntityRoomLoader.RoomTable table, int roomId,
                                            int clearedEntitiesMask, int mapId,
                                            byte[] overworldRoomStatus) {
        return entityLoader.load(table, roomId, clearedEntitiesMask, mapId)
            .withSpriteSelection(entitySpriteCatalog.load(table, roomId, mapId,
                overworldRoomStatus));
    }

    private static EntityRoomLoader.RoomTable entityTableForIndoorMap(int mapId) {
        if (mapId == 0xFF) {
            return EntityRoomLoader.RoomTable.COLOR_DUNGEON;
        }
        return mapId >= 0x06 && mapId < 0x1A
            ? EntityRoomLoader.RoomTable.INDOORS_B
            : EntityRoomLoader.RoomTable.INDOORS_A;
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
