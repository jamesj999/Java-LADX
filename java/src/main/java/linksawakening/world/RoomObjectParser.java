package linksawakening.world;

import linksawakening.rom.RomBank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RoomObjectParser {
    private static final int ROOM_TEMPLATE_BANK = 0x14;
    private static final int[][] ROOM_TEMPLATE_TABLES = {
        {0x4917, 0x4938},
        {0x4958, 0x4971},
        {0x4989, 0x49A4},
        {0x49BE, 0x49D7},
        {0x49EF, 0x4A0A},
        {0x4A24, 0x4A36},
        {0x4A47, 0x4A59},
        {0x4A6A, 0x4A7C},
        {0x4A8D, 0x4A9F},
    };

    private static final int INDOOR_DOOR_MACRO_BANK = 0x00;
    private static final int HORIZONTAL_OFFSETS_ADDR = 0x37E1;
    private static final int VERTICAL_OFFSETS_ADDR = 0x37E4;
    private static final int DUNGEON_ENTRANCE_OFFSETS_ADDR = 0x3789;
    private static final int[][] INDOOR_DOOR_MACRO_TABLES = {
        {HORIZONTAL_OFFSETS_ADDR, 0x35F8},
        {HORIZONTAL_OFFSETS_ADDR, 0x3613},
        {VERTICAL_OFFSETS_ADDR,   0x362E},
        {VERTICAL_OFFSETS_ADDR,   0x3649},
        {HORIZONTAL_OFFSETS_ADDR, 0x36B0},
        {HORIZONTAL_OFFSETS_ADDR, 0x36E8},
        {VERTICAL_OFFSETS_ADDR,   0x36FC},
        {VERTICAL_OFFSETS_ADDR,   0x3710},
        {HORIZONTAL_OFFSETS_ADDR, 0x36B0},
        {HORIZONTAL_OFFSETS_ADDR, 0x36E8},
        {VERTICAL_OFFSETS_ADDR,   0x36FC},
        {VERTICAL_OFFSETS_ADDR,   0x3710},
        {HORIZONTAL_OFFSETS_ADDR, 0x3724},
        {VERTICAL_OFFSETS_ADDR,   0x375C},
        {HORIZONTAL_OFFSETS_ADDR, 0x376B},
        {HORIZONTAL_OFFSETS_ADDR, 0x377A},
        {DUNGEON_ENTRANCE_OFFSETS_ADDR, 0x3796},
        {HORIZONTAL_OFFSETS_ADDR, 0x37B4},
    };
    private static final int INDOOR_DOOR_MACRO_BASE = 0xEC;
    private static final int INDOOR_DOOR_MACRO_LAST = 0xFD;

    private static final int OBJECT_CLOSED_GATE = 0xC2;
    private static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    private static final int OBJECT_HOUSE_DOOR_VARIANT = 0xE2;
    private static final int OBJECT_CAVE_DOOR = 0xE3;
    private static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    private static final int OBJECT_BOMBABLE_BLOCK = 0xA9;
    private static final int OBJECT_FLOOR_OD = 0x0D;
    private static final int OBJECT_STAIRS_DOWN = 0xBE;
    private static final int OBJECT_HIDDEN_STAIRS_DOWN = 0xBF;
    private static final int OBJECT_STAIRS_UP = 0xCB;
    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    private static final int OBJECT_DOOR_CB = 0xCB;
    private static final int OBJECT_DOOR_61 = 0x61;
    private static final int OBJECT_DOOR_C5 = 0xC5;

    private static final int OVERWORLD_MACROS_BANK = 0x24;
    private static final int CATFISH_MAW_OBJECT_OFFSETS_ADDR = 0x760B;
    private static final int CATFISH_MAW_OBJECT_IDS_ADDR = 0x7612;
    private static final int TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR = 0x762E;
    private static final int TWO_DOORS_HOUSE_OBJECT_IDS_ADDR = 0x763E;
    private static final int LARGE_HOUSE_OBJECT_OFFSETS_ADDR = 0x765A;
    private static final int LARGE_HOUSE_OBJECT_IDS_ADDR = 0x7664;
    private static final int PALACE_DOOR_OBJECT_OFFSETS_ADDR = 0x767A;
    private static final int PALACE_DOOR_OBJECT_IDS_ADDR = 0x7681;
    private static final int STONE_PIG_HEAD_OBJECT_OFFSETS_ADDR = 0x7694;
    private static final int STONE_PIG_HEAD_OBJECT_IDS_INTACT_ADDR = 0x7699;
    private static final int STONE_PIG_HEAD_OBJECT_IDS_BLASTED_ADDR = 0x769D;
    private static final int PALM_TREE_OBJECT_OFFSETS_ADDR = 0x76B7;
    private static final int PALM_TREE_OBJECT_IDS_ADDR = 0x76BC;
    private static final int WALLED_PIT_OBJECT_OFFSETS_ADDR = 0x76CD;
    private static final int WALLED_PIT_OBJECT_IDS_ADDR = 0x76DC;
    private static final int SMALL_HOUSE_OBJECT_OFFSETS_ADDR = 0x76F7;
    private static final int SMALL_HOUSE_OBJECT_IDS_ADDR = 0x76FE;

    private static final int ROOM_STATUS_DOOR_OPEN_UP = 0x04;
    private static final int ROOM_STATUS_EVENT_1 = 0x10;
    private static final int ROOM_STATUS_CHEST_OPEN = 0x10;
    private static final int ROOM_STATUS_EVENT_3 = 0x40;
    private static final int OW_ROOM_STATUS_OPENED = 0x04;
    private static final int OBJECT_CHEST_CLOSED = 0xA0;
    private static final int OBJECT_CHEST_OPEN = 0xA1;
    private static final int OBJECT_TREE_TOP_LEFT = 0x25;
    private static final int OBJECT_TREE_TOP_RIGHT = 0x26;
    private static final int OBJECT_TREE_BOTTOM_LEFT = 0x27;
    private static final int OBJECT_TREE_BOTTOM_RIGHT = 0x28;
    private static final int OBJECT_TREE_OVERLAP_LEFT = 0x29;
    private static final int OBJECT_TREE_OVERLAP_RIGHT = 0x2A;
    private static final int OBJECT_TREE_BUSHES_BOTTOM_LEFT = 0x82;
    private static final int OBJECT_TREE_BUSHES_BOTTOM_RIGHT = 0x83;
    private static final int OBJECT_MACRO_BASE = 0xF5;

    private final byte[] romData;
    private int[] roomObjectsArea;
    private int shutterDoorMask;
    private int staircaseLocation = -1;
    private final List<Warp> warps = new ArrayList<>();
    private final List<Integer> doorPositions = new ArrayList<>();
    private int roomStatusFlags;

    public RoomObjectParser(byte[] romData) {
        this.romData = romData;
    }

    public RoomObjectParseResult parseOverworld(int streamOffset, int floorObject, int roomStatusFlags) {
        reset(floorObject);
        this.roomStatusFlags = roomStatusFlags;
        parseRoomObjectStream(streamOffset, false);
        applyChestStatus();
        applyOverworldClosedGateStatus();
        applyOverworldBombableCaveDoorStatus();
        assignDoorPositionsToWarps();
        return new RoomObjectParseResult(roomObjectsArea, warps, 0, staircaseLocation);
    }

    /** Mirrors LoadRoomObject's OBJECT_CLOSED_GATE status-bit replacement. */
    private void applyOverworldClosedGateStatus() {
        if ((roomStatusFlags & ROOM_STATUS_CHEST_OPEN) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_CLOSED_GATE) {
                    roomObjectsArea[areaIndex] = OBJECT_CAVE_DOOR;
                }
            }
        }
    }

    private void applyOverworldBombableCaveDoorStatus() {
        if ((roomStatusFlags & OW_ROOM_STATUS_OPENED) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_CAVE_DOOR) {
                    roomObjectsArea[areaIndex] = OBJECT_ROCKY_CAVE_DOOR;
                }
            }
        }
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate) {
        return parseIndoor(streamOffset, floorAndTemplate, 0);
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate,
                                             int roomStatusFlags) {
        return parseIndoor(streamOffset, floorAndTemplate, roomStatusFlags, -1);
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate,
                                             int roomStatusFlags, int mapId) {
        reset(floorAndTemplate & 0x0F);
        this.roomStatusFlags = roomStatusFlags;
        int templateIdx = (floorAndTemplate >> 4) & 0x0F;
        if (templateIdx < ROOM_TEMPLATE_TABLES.length) {
            int[] table = ROOM_TEMPLATE_TABLES[templateIdx];
            applyMacroTable(0, ROOM_TEMPLATE_BANK, table[0], ROOM_TEMPLATE_BANK, table[1]);
        }
        parseRoomObjectStream(streamOffset, true);
        applyChestStatus();
        applyIndoorBombableBlockStatus(mapId);
        applyIndoorBombableWallStatus();
        assignDoorPositionsToWarps();
        return new RoomObjectParseResult(
            roomObjectsArea, warps, shutterDoorMask, staircaseLocation);
    }

    private void applyIndoorBombableBlockStatus(int mapId) {
        // ConfigureRoomObjects only admits the breakable-block replacement on
        // MAP_CAVE_B and later indoor maps, matching the source's map gate.
        if (mapId < 0x0A || (roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_BLOCK) {
                    roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    private void applyIndoorBombableWallStatus() {
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                int objectId = roomObjectsArea[areaIndex];
                if ((objectId == 0x3F || objectId == 0x47)
                    && (roomStatusFlags & 0x04) != 0) {
                    roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x40 || objectId == 0x48)
                    && (roomStatusFlags & 0x08) != 0) {
                    roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x41 || objectId == 0x49)
                    && (roomStatusFlags & 0x02) != 0) {
                    roomObjectsArea[areaIndex] = 0x3E;
                } else if ((objectId == 0x42 || objectId == 0x4A)
                    && (roomStatusFlags & 0x01) != 0) {
                    roomObjectsArea[areaIndex] = 0x3E;
                }
            }
        }
    }

    /** Mirrors ConfigureRoomObjects' replacement of a collected chest object. */
    private void applyChestStatus() {
        if ((roomStatusFlags & ROOM_STATUS_CHEST_OPEN) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE
                    + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_CHEST_CLOSED) {
                    roomObjectsArea[areaIndex] = OBJECT_CHEST_OPEN;
                }
            }
        }
    }

    private void reset(int floorObject) {
        roomObjectsArea = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(roomObjectsArea, 0xFF);
        warps.clear();
        doorPositions.clear();
        shutterDoorMask = 0;
        staircaseLocation = -1;
        fillRoomMapWithObject(floorObject);
    }

    private void parseRoomObjectStream(int pos, boolean isIndoor) {
        while (pos < romData.length - 1) {
            int firstByte = Byte.toUnsignedInt(romData[pos]);
            if (firstByte == RoomConstants.ROOM_END) {
                break;
            }
            if ((firstByte & 0xFC) == 0xE0) {
                collectWarpRecord(pos);
                pos += 5;
                continue;
            }

            boolean isStrip = (firstByte & 0x80) != 0 && (firstByte & 0x10) == 0;
            int type;
            int location;
            if (isStrip) {
                if (pos + 2 >= romData.length) {
                    break;
                }
                location = Byte.toUnsignedInt(romData[pos + 1]);
                type = Byte.toUnsignedInt(romData[pos + 2]);
                pos += 3;
                if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    expandMacro(type, location, firstByte);
                } else if (isIndoor && type == OBJECT_HIDDEN_STAIRS_DOWN
                    && (roomStatusFlags & ROOM_STATUS_EVENT_1) == 0) {
                    // ConfigureRoomObjects returns before copying concealed stairs.
                } else {
                    fillRoomWithConsecutiveObjects(type, location, firstByte);
                    recordStaircaseLocation(type, location, isIndoor);
                    if (isDoorObjectId(type)) {
                        recordStripDoorPositions(location, firstByte);
                    }
                }
            } else {
                if (pos + 1 >= romData.length) {
                    break;
                }
                location = firstByte;
                type = Byte.toUnsignedInt(romData[pos + 1]);
                pos += 2;
                if (isIndoor && type >= INDOOR_DOOR_MACRO_BASE && type <= INDOOR_DOOR_MACRO_LAST) {
                    expandIndoorDoorMacro(type, location);
                } else if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    expandMacro(type, location, 0);
                } else if (isIndoor && type == OBJECT_HIDDEN_STAIRS_DOWN
                    && (roomStatusFlags & ROOM_STATUS_EVENT_1) == 0) {
                    // ConfigureRoomObjects leaves the room-template floor in place.
                } else {
                    copyObjectToActiveRoomMap(type, location);
                    recordStaircaseLocation(type, location, isIndoor);
                    if (isDoorObjectId(type)) {
                        doorPositions.add(location);
                    }
                }
            }
        }
    }

    private void expandIndoorDoorMacro(int type, int location) {
        int idx = type - INDOOR_DOOR_MACRO_BASE;
        if (idx < 0 || idx >= INDOOR_DOOR_MACRO_TABLES.length) {
            return;
        }
        // ConfigureRoomObjects selects the matching open-door macro when the
        // directional status bit was synchronized by the adjacent room.
        if (idx < 4) {
            int statusBit = switch (idx) {
                case 0 -> 0x04;
                case 1 -> 0x08;
                case 2 -> 0x02;
                case 3 -> 0x01;
                default -> 0;
            };
            if ((roomStatusFlags & statusBit) != 0) {
                idx += 8;
            }
        } else if (idx == 12 && (roomStatusFlags & 0x04) != 0) {
            // OBJECT_BOSS_DOOR is top-facing and reloads as OpenDoorTop once
            // ROOM_STATUS_DOOR_OPEN_UP has been synchronized.
            idx = 8;
        }
        if (idx >= 4 && idx <= 7) {
            // wC18A/wC18B use top, bottom, left, right in bits 0..3.
            shutterDoorMask |= 1 << (idx - 4);
        }
        int[] table = INDOOR_DOOR_MACRO_TABLES[idx];
        applyMacroTable(location, INDOOR_DOOR_MACRO_BANK, table[0],
            INDOOR_DOOR_MACRO_BANK, table[1]);
    }

    private void applyMacroTable(int baseLocation,
                                 int offsetsBank, int offsetsAddr,
                                 int idsBank, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + baseLocation;
        int offsetsOffset = RomBank.romOffset(offsetsBank, offsetsAddr);
        int idsOffset = RomBank.romOffset(idsBank, idsAddr);

        while (offsetsOffset < romData.length && idsOffset < romData.length) {
            int offset = Byte.toUnsignedInt(romData[offsetsOffset++]);
            if (offset == 0xFF) {
                break;
            }
            int objectId = Byte.toUnsignedInt(romData[idsOffset++]);
            int targetAreaIndex = baseAreaIndex + offset;
            if (isAreaIndexValid(targetAreaIndex)) {
                roomObjectsArea[targetAreaIndex] = objectId;
                if (isMacroExpansionDoorId(objectId)) {
                    doorPositions.add(targetAreaIndex - RoomConstants.ROOM_OBJECTS_BASE);
                }
            }
        }
    }

    private void collectWarpRecord(int pos) {
        if (pos + 4 >= romData.length) {
            return;
        }
        int category = Byte.toUnsignedInt(romData[pos]) & 0x03;
        int destMap = Byte.toUnsignedInt(romData[pos + 1]);
        int destRoom = Byte.toUnsignedInt(romData[pos + 2]);
        int destX = Byte.toUnsignedInt(romData[pos + 3]);
        int destY = Byte.toUnsignedInt(romData[pos + 4]);
        warps.add(new Warp(category, destMap, destRoom, destX, destY, -1));
    }

    private void assignDoorPositionsToWarps() {
        int count = Math.min(doorPositions.size(), warps.size());
        for (int i = 0; i < count; i++) {
            int location = doorPositions.get(i);
            Warp original = warps.get(i);
            warps.set(i, original.withTileLocation(location));
        }
    }

    private boolean isDoorObjectId(int objectId) {
        return objectId == OBJECT_CLOSED_GATE
            || objectId == OBJECT_ROCKY_CAVE_DOOR
            || objectId == OBJECT_HOUSE_DOOR_VARIANT
            || objectId == OBJECT_CAVE_DOOR
            || objectId == OBJECT_BOMBABLE_CAVE_DOOR
            || objectId == OBJECT_GROUND_STAIRS
            || objectId == OBJECT_DOOR_CB
            || objectId == OBJECT_DOOR_61
            || objectId == OBJECT_DOOR_C5;
    }

    private boolean isMacroExpansionDoorId(int objectId) {
        return objectId == OBJECT_ROCKY_CAVE_DOOR
            || objectId == OBJECT_HOUSE_DOOR_VARIANT
            || objectId == OBJECT_CAVE_DOOR;
    }

    private void recordStripDoorPositions(int location, int objectData) {
        int count = objectData & 0x0F;
        if (count == 0) {
            return;
        }
        int step = (objectData & 0x40) != 0 ? RoomConstants.ROOM_OBJECT_ROW_STRIDE : 1;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        for (int i = 0; i < count; i++) {
            if (isAreaIndexValid(areaIndex)) {
                doorPositions.add(areaIndex - RoomConstants.ROOM_OBJECTS_BASE);
            }
            areaIndex += step;
        }
    }

    private void expandMacro(int macroType, int location, int objectData) {
        int macroIndex = macroType - OBJECT_MACRO_BASE;
        switch (macroIndex) {
            case 0 -> handleTreeMacro(location, objectData);
            case 1 -> copyOutdoorsMacroObjects(location, TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR, TWO_DOORS_HOUSE_OBJECT_IDS_ADDR);
            case 2 -> copyOutdoorsMacroObjects(location, LARGE_HOUSE_OBJECT_OFFSETS_ADDR, LARGE_HOUSE_OBJECT_IDS_ADDR);
            case 3 -> copyOutdoorsMacroObjects(location, CATFISH_MAW_OBJECT_OFFSETS_ADDR, CATFISH_MAW_OBJECT_IDS_ADDR);
            case 4 -> copyOutdoorsMacroObjects(location, PALACE_DOOR_OBJECT_OFFSETS_ADDR, PALACE_DOOR_OBJECT_IDS_ADDR);
            case 5 -> {
                int pigIdsAddr = (roomStatusFlags & ROOM_STATUS_DOOR_OPEN_UP) == 0
                    ? STONE_PIG_HEAD_OBJECT_IDS_INTACT_ADDR
                    : STONE_PIG_HEAD_OBJECT_IDS_BLASTED_ADDR;
                copyOutdoorsMacroObjects(location, STONE_PIG_HEAD_OBJECT_OFFSETS_ADDR, pigIdsAddr);
            }
            case 6 -> copyOutdoorsMacroObjects(location, PALM_TREE_OBJECT_OFFSETS_ADDR, PALM_TREE_OBJECT_IDS_ADDR);
            case 7 -> copyOutdoorsMacroObjects(location, WALLED_PIT_OBJECT_OFFSETS_ADDR, WALLED_PIT_OBJECT_IDS_ADDR);
            case 8 -> copyOutdoorsMacroObjects(location, SMALL_HOUSE_OBJECT_OFFSETS_ADDR, SMALL_HOUSE_OBJECT_IDS_ADDR);
            default -> {
            }
        }
    }

    private void handleTreeMacro(int location, int objectData) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        if ((areaIndex & 0x0F) == 0) {
            areaIndex -= RoomConstants.ROOM_OBJECT_ROW_STRIDE;
        }
        if (objectData == 0) {
            copyTreeObjectsToRoom(areaIndex);
            return;
        }
        int count = objectData & 0x0F;
        if (count == 0) {
            return;
        }
        int step = (objectData & 0x40) != 0 ? RoomConstants.ROOM_OBJECT_ROW_STRIDE * 2 : 2;
        for (int i = 0; i < count; i++) {
            copyTreeObjectsToRoom(areaIndex + i * step);
        }
    }

    private void copyTreeObjectsToRoom(int areaIndex) {
        if (!isAreaIndexValid(areaIndex)
            || !isAreaIndexValid(areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE + 1)) {
            return;
        }
        int topLeftOffset = areaIndex;
        int topRightOffset = areaIndex + 1;
        int bottomLeftOffset = areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE;
        int bottomRightOffset = bottomLeftOffset + 1;

        int existingTopLeft = roomObjectsArea[topLeftOffset];
        roomObjectsArea[topLeftOffset] = existingTopLeft < 0x10 ? OBJECT_TREE_TOP_LEFT : OBJECT_TREE_OVERLAP_LEFT;

        int existingTopRight = roomObjectsArea[topRightOffset];
        roomObjectsArea[topRightOffset] = existingTopRight < 0x10 ? OBJECT_TREE_TOP_RIGHT : OBJECT_TREE_OVERLAP_RIGHT;

        int existingBottomLeft = roomObjectsArea[bottomLeftOffset];
        if (existingBottomLeft >= 0x8A) {
            roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_BUSHES_BOTTOM_LEFT;
        } else if (existingBottomLeft < 0x10) {
            roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_BOTTOM_LEFT;
        } else {
            roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_OVERLAP_RIGHT;
        }

        int existingBottomRight = roomObjectsArea[bottomRightOffset];
        if (existingBottomRight >= 0x8A) {
            roomObjectsArea[bottomRightOffset] = OBJECT_TREE_BUSHES_BOTTOM_RIGHT;
        } else if (existingBottomRight < 0x10) {
            roomObjectsArea[bottomRightOffset] = OBJECT_TREE_BOTTOM_RIGHT;
        } else {
            roomObjectsArea[bottomRightOffset] = OBJECT_TREE_OVERLAP_LEFT;
        }
    }

    private void copyOutdoorsMacroObjects(int location, int offsetsAddr, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        int offsetsOffset = RomBank.romOffset(OVERWORLD_MACROS_BANK, offsetsAddr);
        int idsOffset = RomBank.romOffset(OVERWORLD_MACROS_BANK, idsAddr);

        while (offsetsOffset < romData.length && idsOffset < romData.length) {
            int offset = Byte.toUnsignedInt(romData[offsetsOffset++]);
            if (offset == 0xFF) {
                break;
            }
            int objectId = Byte.toUnsignedInt(romData[idsOffset++]);
            int targetAreaIndex = baseAreaIndex + offset;
            if (isAreaIndexValid(targetAreaIndex)) {
                roomObjectsArea[targetAreaIndex] = objectId;
                if (isMacroExpansionDoorId(objectId)) {
                    doorPositions.add(targetAreaIndex - RoomConstants.ROOM_OBJECTS_BASE);
                }
            }
        }
    }

    private void fillRoomMapWithObject(int objectId) {
        for (int i = 0; i < RoomConstants.ACTIVE_ROOM_OBJECT_BYTES; i++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + i;
            int column = areaIndex & 0x0F;
            if (column != 0 && column < 0x0B) {
                roomObjectsArea[areaIndex] = objectId;
            }
        }
    }

    private void copyObjectToActiveRoomMap(int objectId, int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        if (isAreaIndexValid(areaIndex)) {
            roomObjectsArea[areaIndex] = objectId;
        }
    }

    private void fillRoomWithConsecutiveObjects(int objectId, int location, int objectData) {
        int count = objectData & 0x0F;
        if (count == 0) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        int step = (objectData & 0x40) != 0 ? RoomConstants.ROOM_OBJECT_ROW_STRIDE : 1;
        for (int i = 0; i < count; i++) {
            if (isAreaIndexValid(areaIndex)) {
                roomObjectsArea[areaIndex] = objectId;
            }
            areaIndex += step;
        }
    }

    private boolean isAreaIndexValid(int areaIndex) {
        return areaIndex >= 0 && areaIndex < roomObjectsArea.length;
    }

    private void recordStaircaseLocation(int objectId, int location, boolean isIndoor) {
        if (isStaircaseObject(objectId, isIndoor)) {
            staircaseLocation = location & 0xFF;
        }
    }

    private static boolean isStaircaseObject(int objectId, boolean isIndoor) {
        return isIndoor
            ? objectId == OBJECT_STAIRS_DOWN
                || objectId == OBJECT_HIDDEN_STAIRS_DOWN
                || objectId == OBJECT_STAIRS_UP
            : objectId == OBJECT_DOOR_C5 || objectId == OBJECT_GROUND_STAIRS;
    }
}
