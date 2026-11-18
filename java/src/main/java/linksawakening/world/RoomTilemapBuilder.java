package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Arrays;

public final class RoomTilemapBuilder {
    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_CAMERA_SHOP = 0xB5;

    private static final int INDOOR_OBJECT_TILEMAP_BANK = 0x08;
    private static final int INDOOR_OBJECT_TILEMAP_ADDR = 0x43B0;
    private static final int COLOR_DUNGEON_OBJECT_TILEMAP_BANK = 0x08;
    private static final int COLOR_DUNGEON_OBJECT_TILEMAP_ADDR = 0x4760;
    private static final int COLOR_DUNGEON_OBJECT_ATTR_BANK = 0x23;
    private static final int COLOR_DUNGEON_OBJECT_ATTR_ADDR = 0x6000;
    private static final int BG_ATTR_PTRS_INDOORS_A_BANK = 0x1A;
    private static final int BG_ATTR_PTRS_INDOORS_A_ADDR = 0x6076;
    private static final int BG_ATTR_PTRS_INDOORS_B_ADDR = 0x6276;
    private static final int INDOORS_A_ATTRMAPS_BANK = 0x23;
    private static final int INDOORS_B_ATTRMAPS_BANK = 0x24;

    private static final int OVERWORLD_TILEMAP_BANK = 0x1A;
    private static final int OVERWORLD_TILEMAP_ADDR = 0x6B1D;
    private static final int OVERWORLD_OBJ_ATTR_BANKS_BANK = 0x1A;
    private static final int OVERWORLD_OBJ_ATTR_BANKS_ADDR = 0x6476;
    private static final int OVERWORLD_OBJ_ATTR_PTRS_BANK = 0x1A;
    private static final int OVERWORLD_OBJ_ATTR_PTRS_ADDR = 0x5E76;
    private static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    private static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    private static final int OBJECT_CLOSED_GATE = 0xC2;
    private static final int OBJECT_CAVE_DOOR = 0xE3;

    private static final int GBC_OVERLAY_BANK_A = 0x26;
    private static final int GBC_OVERLAY_ADDR_A = 0x4000;
    private static final int GBC_OVERLAY_BANK_B = 0x27;
    private static final int GBC_OVERLAY_ADDR_B = 0x4000;
    private static final int GBC_OVERLAY_ROOM_SIZE = 0x50;
    private static final int GBC_OVERLAY_BANK_B_START_ROOM = 0xCC;

    private final byte[] romData;

    public RoomTilemapBuilder(byte[] romData) {
        this.romData = romData;
    }

    public RoomTilemap buildOverworld(int roomId, int[] roomObjectsArea) {
        int[] overlay = loadGbcOverlay(roomId);
        int[] renderValues = buildOverworldRenderValues(overlay, roomObjectsArea);
        return build(roomId, 0, false, roomObjectsArea, overlay, renderValues);
    }

    public RoomTilemap buildIndoor(int mapId, int roomId, int[] roomObjectsArea) {
        return build(roomId, mapId, true, roomObjectsArea, null, null);
    }

    private RoomTilemap build(int roomId, int mapId, boolean indoor, int[] roomObjectsArea,
                              int[] gbcOverlay, int[] renderValues) {
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];

        int objectTilemapOffset;
        int objectAttrOffset;
        boolean cameraShop = indoor && mapId == MAP_HOUSE && roomId == ROOM_CAMERA_SHOP;
        if (indoor && (mapId == MAP_COLOR_DUNGEON || cameraShop)) {
            objectTilemapOffset = RomBank.romOffset(
                COLOR_DUNGEON_OBJECT_TILEMAP_BANK, COLOR_DUNGEON_OBJECT_TILEMAP_ADDR);
            objectAttrOffset = mapId == MAP_COLOR_DUNGEON
                ? RomBank.romOffset(COLOR_DUNGEON_OBJECT_ATTR_BANK, COLOR_DUNGEON_OBJECT_ATTR_ADDR)
                : indoorObjectAttrOffset(mapId, roomId);
        } else {
            objectTilemapOffset = indoor
                ? RomBank.romOffset(INDOOR_OBJECT_TILEMAP_BANK, INDOOR_OBJECT_TILEMAP_ADDR)
                : RomBank.romOffset(OVERWORLD_TILEMAP_BANK, OVERWORLD_TILEMAP_ADDR);
            objectAttrOffset = indoor
                ? indoorObjectAttrOffset(mapId, roomId)
                : overworldObjectAttrOffset(roomId);
        }

        for (int oy = 0; oy < RoomConstants.OBJECTS_PER_COLUMN; oy++) {
            for (int ox = 0; ox < RoomConstants.OBJECTS_PER_ROW; ox++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + oy * RoomConstants.ROOM_OBJECT_ROW_STRIDE + ox;
                int objectId = roomObjectsArea[areaIndex];
                if (objectId >= 0x100) {
                    continue;
                }

                int overlayIndex = oy * RoomConstants.OBJECTS_PER_ROW + ox;
                int lookupValue = resolveRenderedObjectValue(areaIndex, overlayIndex, objectId, gbcOverlay, renderValues);
                int tileTableOffset = objectTilemapOffset + lookupValue * 4;
                int attrTableOffset = objectAttrOffset < 0 ? -1 : objectAttrOffset + lookupValue * 4;

                for (int ty = 0; ty < 2; ty++) {
                    for (int tx = 0; tx < 2; tx++) {
                        int tileByteIndex = ty * 2 + tx;
                        int tileId = Byte.toUnsignedInt(romData[tileTableOffset + tileByteIndex]);
                        int attrByte = 0;
                        if (attrTableOffset >= 0 && attrTableOffset + tileByteIndex < romData.length) {
                            attrByte = Byte.toUnsignedInt(romData[attrTableOffset + tileByteIndex]);
                        }

                        int mapX = ox * 2 + tx;
                        int mapY = oy * 2 + ty;
                        int mapIndex = mapY * RoomConstants.ROOM_TILE_WIDTH + mapX;
                        tileIds[mapIndex] = tileId;
                        tileAttrs[mapIndex] = attrByte;
                    }
                }
            }
        }

        return new RoomTilemap(tileIds, tileAttrs, gbcOverlay, renderValues);
    }

    /** The middle two tiles of each door row change before the collision objects do. */
    void drawShutterFrame(ActiveRoom room, int direction, int location, boolean opening) {
        int x = (location & 15) * 2 + (direction < 2 ? 1 : 0);
        int y = (location >>> 4) * 2 + (direction < 2 ? 0 : 1);
        int source = 4 * (direction + (opening ? 4 : 0));
        int tiles = RomBank.romOffset(2, opening ? 0x59D4 : 0x5BC4);
        int objects = RomBank.romOffset(0x1A, opening ? 0x6640 : 0x6688);
        int quadrants = RomBank.romOffset(0x1A, opening ? 0x66A8 : 0x66F0);
        int attributes = room.mapId() == MAP_COLOR_DUNGEON
            ? RomBank.romOffset(COLOR_DUNGEON_OBJECT_ATTR_BANK, COLOR_DUNGEON_OBJECT_ATTR_ADDR)
            : indoorObjectAttrOffset(room.mapId(), room.roomId());
        for (int i = 0; i < 4; i++) {
            int tile = (y + i / 2) * RoomConstants.ROOM_TILE_WIDTH + x + i % 2;
            int object = Byte.toUnsignedInt(romData[objects + source + i]);
            int quadrant = Byte.toUnsignedInt(romData[quadrants + source + i]);
            room.tileIds()[tile] = Byte.toUnsignedInt(romData[tiles + source + i]);
            room.tileAttrs()[tile] = attributes < 0 ? 0 : Byte.toUnsignedInt(romData[attributes + object * 4 + quadrant]);
        }
    }

    private int overworldObjectAttrOffset(int roomId) {
        int attrBankOffset = RomBank.romOffset(OVERWORLD_OBJ_ATTR_BANKS_BANK, OVERWORLD_OBJ_ATTR_BANKS_ADDR + roomId);
        int attrBank = Byte.toUnsignedInt(romData[attrBankOffset]);
        int attrPtrsOffset = RomBank.romOffset(OVERWORLD_OBJ_ATTR_PTRS_BANK, OVERWORLD_OBJ_ATTR_PTRS_ADDR + roomId * 2);
        int attrPtrLo = Byte.toUnsignedInt(romData[attrPtrsOffset]);
        int attrPtrHi = Byte.toUnsignedInt(romData[attrPtrsOffset + 1]);
        int attrTableAddr = (attrPtrHi << 8) | attrPtrLo;
        return RomBank.romOffset(attrBank | 0x20, attrTableAddr);
    }

    private int indoorObjectAttrOffset(int mapId, int roomId) {
        // GetBGAttributesAddressForObject (1A:65B2) indexes by map, with
        // room-specific overrides for interiors that share a map ID.
        boolean dungeon = mapId <= 0x08;
        int table = dungeon ? BG_ATTR_PTRS_INDOORS_A_ADDR : BG_ATTR_PTRS_INDOORS_B_ADDR;
        int group = mapId;
        if (mapId == 0x0A && roomId == 0xFD || mapId == 0x11 && (roomId == 0xC0 || roomId == 0xC1)) {
            group = 0x0F;
        } else if (mapId == 0x0F && roomId == 0xA0) {
            group = 0;
        } else if (mapId == 0x1F && (roomId == 0xEB || roomId == 0xEC)) {
            group = 0x14;
        } else if (mapId == MAP_HOUSE && roomId == 0xE9) {
            group = 0x13;
        } else if (mapId == MAP_HOUSE && roomId == ROOM_CAMERA_SHOP) {
            group = 0xFF;
        }
        boolean southShrine = mapId == 0x16 && (roomId == 0x6F || roomId == 0x7F || roomId == 0x8F);
        int bank = dungeon || southShrine ? INDOORS_A_ATTRMAPS_BANK : INDOORS_B_ATTRMAPS_BANK;
        int address = readPointer(RomBank.romOffset(BG_ATTR_PTRS_INDOORS_A_BANK, table + group * 2));
        return address == 0 ? -1 : RomBank.romOffset(bank, address);
    }

    private int readPointer(int offset) {
        return Byte.toUnsignedInt(romData[offset])
            | (Byte.toUnsignedInt(romData[offset + 1]) << 8);
    }

    private int[] loadGbcOverlay(int roomId) {
        int bank;
        int baseAddr;
        int overlayRoomIndex;
        if (roomId < GBC_OVERLAY_BANK_B_START_ROOM) {
            bank = GBC_OVERLAY_BANK_A;
            baseAddr = GBC_OVERLAY_ADDR_A;
            overlayRoomIndex = roomId;
        } else {
            bank = GBC_OVERLAY_BANK_B;
            baseAddr = GBC_OVERLAY_ADDR_B;
            overlayRoomIndex = roomId - GBC_OVERLAY_BANK_B_START_ROOM;
        }

        int overlayOffset = RomBank.romOffset(bank, baseAddr + overlayRoomIndex * GBC_OVERLAY_ROOM_SIZE);
        int[] overlay = new int[GBC_OVERLAY_ROOM_SIZE];
        for (int i = 0; i < GBC_OVERLAY_ROOM_SIZE; i++) {
            overlay[i] = Byte.toUnsignedInt(romData[overlayOffset + i]);
        }
        return overlay;
    }

    private int[] buildOverworldRenderValues(int[] gbcOverlay, int[] roomObjectsArea) {
        int[] renderValues = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(renderValues, 0xFF);
        if (gbcOverlay == null) {
            return renderValues;
        }
        for (int oy = 0; oy < RoomConstants.OBJECTS_PER_COLUMN; oy++) {
            for (int ox = 0; ox < RoomConstants.OBJECTS_PER_ROW; ox++) {
                int overlayIndex = oy * RoomConstants.OBJECTS_PER_ROW + ox;
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + oy * RoomConstants.ROOM_OBJECT_ROW_STRIDE + ox;
                int renderValue = gbcOverlay[overlayIndex];
                // ConfigureRoomObjects performs persistent door replacements
                // before LoadRoomTilemap. SetupDestroyableObjectIfNeeded writes
                // the replacement into the mutable GBC object buffer too.
                if (roomObjectsArea != null
                    && areaIndex < roomObjectsArea.length) {
                    int objectId = roomObjectsArea[areaIndex];
                    boolean persistentReplacement =
                        objectId == OBJECT_ROCKY_CAVE_DOOR
                            && renderValue == OBJECT_BOMBABLE_CAVE_DOOR
                        || objectId == OBJECT_CAVE_DOOR
                            && renderValue == OBJECT_CLOSED_GATE;
                    if (persistentReplacement) {
                        renderValue = objectId;
                        gbcOverlay[overlayIndex] = objectId;
                    }
                }
                renderValues[areaIndex] = renderValue;
            }
        }
        return renderValues;
    }

    private int resolveRenderedObjectValue(int areaIndex, int overlayIndex, int objectId,
                                           int[] gbcOverlay, int[] renderValues) {
        if (renderValues != null && areaIndex >= 0 && areaIndex < renderValues.length) {
            int value = renderValues[areaIndex];
            if (value >= 0 && value <= 0xFF) {
                return value;
            }
        }
        if (gbcOverlay != null && overlayIndex >= 0 && overlayIndex < gbcOverlay.length) {
            return gbcOverlay[overlayIndex];
        }
        return objectId;
    }
}
