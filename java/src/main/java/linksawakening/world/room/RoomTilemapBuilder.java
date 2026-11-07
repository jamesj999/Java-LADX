package linksawakening.world.room;







import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomSymbols;

import linksawakening.rom.RomBank;

import java.util.Arrays;

public final class RoomTilemapBuilder {
    private int readRomByte(int offset) {
        return new Rom(romData).u8(RomAddress.fromOffset(offset));
    }

    private static final int MAP_COLOR_DUNGEON = 0xFF;
    private static final int MAP_HOUSE = 0x10;
    private static final int ROOM_CAMERA_SHOP = 0xB5;

    private static final int INDOOR_OBJECT_TILEMAP_BANK = RomSymbols.IndoorObjectsTilemapCGB.bank();
    private static final int INDOOR_OBJECT_TILEMAP_ADDR = RomSymbols.IndoorObjectsTilemapCGB.address();
    private static final int COLOR_DUNGEON_OBJECT_TILEMAP_BANK = RomSymbols.ColorDungeonObjectsTilemap.bank();
    private static final int COLOR_DUNGEON_OBJECT_TILEMAP_ADDR = RomSymbols.ColorDungeonObjectsTilemap.address();
    private static final int COLOR_DUNGEON_OBJECT_ATTR_BANK = RomSymbols.ColorDungeonBGAttributes.bank();
    private static final int COLOR_DUNGEON_OBJECT_ATTR_ADDR = RomSymbols.ColorDungeonBGAttributes.address();
    private static final int BG_ATTR_PTRS_INDOORS_A_BANK = RomSymbols.BGAttributesPointers_IndoorsA.bank();
    private static final int BG_ATTR_PTRS_INDOORS_A_ADDR = RomSymbols.BGAttributesPointers_IndoorsA.address();
    private static final int BG_ATTR_PTRS_INDOORS_B_ADDR = RomSymbols.BGAttributesPointers_IndoorsB.address();
    private static final int CAMERA_SHOP_ATTR_POINTER_OFFSET = 0x1FE;
    private static final int INDOORS_A_ATTRMAPS_BANK = RomSymbols.IndoorsAObjectsAttrmaps.bank();
    private static final int INDOORS_B_ATTRMAPS_BANK = RomSymbols.IndoorsBObjectsAttrmaps.bank();

    private static final int OVERWORLD_TILEMAP_BANK = RomSymbols.OverworldObjectsTilemapCGB.bank();
    private static final int OVERWORLD_TILEMAP_ADDR = RomSymbols.OverworldObjectsTilemapCGB.address();
    private static final int OVERWORLD_OBJ_ATTR_BANKS_BANK = RomSymbols.OverworldBGAttributesBanks.bank();
    private static final int OVERWORLD_OBJ_ATTR_BANKS_ADDR = RomSymbols.OverworldBGAttributesBanks.address();
    private static final int OVERWORLD_OBJ_ATTR_PTRS_BANK = RomSymbols.BGAttributesPointers_Overworld.bank();
    private static final int OVERWORLD_OBJ_ATTR_PTRS_ADDR = RomSymbols.BGAttributesPointers_Overworld.address();
    private static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    private static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    private static final int OBJECT_CLOSED_GATE = 0xC2;
    private static final int OBJECT_CAVE_DOOR = 0xE3;

    private static final int GBC_OVERLAY_BANK_A = RomSymbols.RoomGBCOverlaysA.bank();
    private static final int GBC_OVERLAY_ADDR_A = RomSymbols.RoomGBCOverlaysA.address();
    private static final int GBC_OVERLAY_BANK_B = RomSymbols.RoomGBCOverlaysB.bank();
    private static final int GBC_OVERLAY_ADDR_B = RomSymbols.RoomGBCOverlaysB.address();
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
                        int tileId = readRomByte(tileTableOffset + tileByteIndex);
                        int attrByte = 0;
                        if (attrTableOffset >= 0 && attrTableOffset + tileByteIndex < romData.length) {
                            attrByte = readRomByte(attrTableOffset + tileByteIndex);
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
        int attrBank = readRomByte(attrBankOffset);
        int attrPtrsOffset = RomBank.romOffset(OVERWORLD_OBJ_ATTR_PTRS_BANK, OVERWORLD_OBJ_ATTR_PTRS_ADDR + roomId * 2);
        int attrPtrLo = readRomByte(attrPtrsOffset);
        int attrPtrHi = readRomByte(attrPtrsOffset + 1);
        int attrTableAddr = (attrPtrHi << 8) | attrPtrLo;
        return RomBank.romOffset((attrBank | 0x20), attrTableAddr);
    }

    private int indoorObjectAttrOffset(int mapId, int roomId) {
        // GetBGAttributesAddressForObject ($1A:65B2) replaces the room
        // index with hMapId * 2 for indoor attribute groups. Room IDs only
        // select the explicit exceptions below; these are not room tables.
        int groupOffset = mapId * 2;
        boolean useB = mapId > 0x08;
        int attrmapsBank = useB ? INDOORS_B_ATTRMAPS_BANK : INDOORS_A_ATTRMAPS_BANK;
        if ((mapId == 0x0A && roomId == 0xFD)
            || (mapId == 0x11 && (roomId == 0xC0 || roomId == 0xC1))) {
            groupOffset = 0x1E;
        } else if (mapId == 0x0F && roomId == 0xA0) {
            groupOffset = 0;
        } else if (mapId == 0x1F && (roomId == 0xEB || roomId == 0xEC)) {
            groupOffset = 0x28;
        } else if (mapId == MAP_HOUSE && roomId == 0xE9) {
            groupOffset = 0x26;
        } else if (mapId == MAP_HOUSE && roomId == ROOM_CAMERA_SHOP) {
            groupOffset = CAMERA_SHOP_ATTR_POINTER_OFFSET;
        } else if (mapId == 0x16 && (roomId == 0x6F || roomId == 0x7F || roomId == 0x8F)) {
            // South Face Shrine keeps bank $23 but uses the indoors B pointer.
            attrmapsBank = INDOORS_A_ATTRMAPS_BANK;
        }
        int attrPtrsAddr = useB ? BG_ATTR_PTRS_INDOORS_B_ADDR : BG_ATTR_PTRS_INDOORS_A_ADDR;
        int pointer = RomBank.romOffset(BG_ATTR_PTRS_INDOORS_A_BANK, attrPtrsAddr + groupOffset);
        int attrTableAddr = readPointer(pointer);
        if (attrTableAddr == 0) {
            return -1;
        }
        return RomBank.romOffset(attrmapsBank, attrTableAddr);
    }

    private int readPointer(int offset) {
        return readRomByte(offset)
            | (readRomByte(offset + 1) << 8);
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
            overlay[i] = readRomByte(overlayOffset + i);
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
                            && renderValue == OBJECT_CLOSED_GATE
                        || objectId == 0xA1 && renderValue == 0xA0;
                    if (persistentReplacement) {
                        renderValue = objectId;
                        gbcOverlay[overlayIndex] = renderValue;
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
