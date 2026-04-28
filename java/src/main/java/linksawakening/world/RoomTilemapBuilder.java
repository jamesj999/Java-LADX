package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.Arrays;

public final class RoomTilemapBuilder {
    private static final int MAP_INDOORS_B_START = 0x06;
    private static final int MAP_INDOORS_B_END = 0x1A;

    private static final int INDOOR_OBJECT_TILEMAP_BANK = 0x08;
    private static final int INDOOR_OBJECT_TILEMAP_ADDR = 0x43B0;
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
        int[] renderValues = buildOverworldRenderValues(overlay);
        return build(roomId, 0, false, roomObjectsArea, overlay, renderValues);
    }

    public RoomTilemap buildIndoor(int mapId, int roomId, int[] roomObjectsArea) {
        return build(roomId, mapId, true, roomObjectsArea, null, null);
    }

    private RoomTilemap build(int roomId, int mapId, boolean indoor, int[] roomObjectsArea,
                              int[] gbcOverlay, int[] renderValues) {
        int[] tileIds = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];
        int[] tileAttrs = new int[RoomConstants.ROOM_TILE_WIDTH * RoomConstants.ROOM_TILE_HEIGHT];

        int objectTilemapOffset = indoor
            ? RomBank.romOffset(INDOOR_OBJECT_TILEMAP_BANK, INDOOR_OBJECT_TILEMAP_ADDR)
            : RomBank.romOffset(OVERWORLD_TILEMAP_BANK, OVERWORLD_TILEMAP_ADDR);

        int objectAttrOffset = indoor
            ? indoorObjectAttrOffset(mapId, roomId)
            : overworldObjectAttrOffset(roomId);

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
        boolean isIndoorsB = mapId >= MAP_INDOORS_B_START && mapId < MAP_INDOORS_B_END;
        int attrPtrsAddr = isIndoorsB ? BG_ATTR_PTRS_INDOORS_B_ADDR : BG_ATTR_PTRS_INDOORS_A_ADDR;
        int attrmapsBank = isIndoorsB ? INDOORS_B_ATTRMAPS_BANK : INDOORS_A_ATTRMAPS_BANK;
        int indoorAttrPtrsOffset = RomBank.romOffset(BG_ATTR_PTRS_INDOORS_A_BANK, attrPtrsAddr + roomId * 2);
        int attrPtrLo = Byte.toUnsignedInt(romData[indoorAttrPtrsOffset]);
        int attrPtrHi = Byte.toUnsignedInt(romData[indoorAttrPtrsOffset + 1]);
        int attrTableAddr = (attrPtrHi << 8) | attrPtrLo;
        if (attrTableAddr == 0) {
            return -1;
        }
        return RomBank.romOffset(attrmapsBank, attrTableAddr);
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

    private int[] buildOverworldRenderValues(int[] gbcOverlay) {
        int[] renderValues = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(renderValues, 0xFF);
        if (gbcOverlay == null) {
            return renderValues;
        }
        for (int oy = 0; oy < RoomConstants.OBJECTS_PER_COLUMN; oy++) {
            for (int ox = 0; ox < RoomConstants.OBJECTS_PER_ROW; ox++) {
                int overlayIndex = oy * RoomConstants.OBJECTS_PER_ROW + ox;
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + oy * RoomConstants.ROOM_OBJECT_ROW_STRIDE + ox;
                renderValues[areaIndex] = gbcOverlay[overlayIndex];
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
