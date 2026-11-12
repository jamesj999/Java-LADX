package linksawakening.world.room;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;
import linksawakening.world.transition.Warp;

public final class RoomObjectParser {
    private int readRomByte(int offset) {
        return new Rom(romData).u8(RomAddress.fromOffset(offset));
    }

    private static final int ROOM_TEMPLATE_BANK = RomSymbols.RoomTemplateTopRightBottomLeftOffsets.bank();
    private static final int[][] ROOM_TEMPLATE_TABLES = {
        {
            RomSymbols.RoomTemplateTopRightBottomLeftOffsets.address(),
            RomSymbols.RoomTemplateTopRightBottomLeftIds.address()
        },
        {RomSymbols.RoomTemplateRightBottomLeftOffsets.address(), RomSymbols.RoomTemplateRightBottomLeftIds.address()},
        {RomSymbols.RoomTemplateTopLeftBottomOffsets.address(), RomSymbols.RoomTemplateTopLeftBottomIds.address()},
        {RomSymbols.RoomTemplateLeftTopRightOffsets.address(), RomSymbols.RoomTemplateLeftTopRightIds.address()},
        {RomSymbols.RoomTemplateTopRightBottomOffsets.address(), RomSymbols.RoomTemplateTopRightBottomIds.address()},
        {RomSymbols.RoomTemplateBottomLeftOffsets.address(), RomSymbols.RoomTemplateBottomLeftIds.address()},
        {RomSymbols.RoomTemplateBottomRightOffsets.address(), RomSymbols.RoomTemplateBottomRightIds.address()},
        {RomSymbols.RoomTemplateTopRightOffsets.address(), RomSymbols.RoomTemplateTopRightIds.address()},
        {RomSymbols.RoomTemplateTopLeftOffsets.address(), RomSymbols.RoomTemplateTopLeftIds.address()},
    };

    private static final int INDOOR_DOOR_MACRO_BANK = RomSymbols.HorizontalObjectOffsets.bank();
    private static final int HORIZONTAL_OFFSETS_ADDR = RomSymbols.HorizontalObjectOffsets.address();
    private static final int VERTICAL_OFFSETS_ADDR = RomSymbols.VerticalObjectOffsets.address();
    private static final int DUNGEON_ENTRANCE_OFFSETS_ADDR = RomSymbols.DungeonEntranceObjectOffsets.address();
    private static final int[][] INDOOR_DOOR_MACRO_TABLES = {
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.KeyDoorTopObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.KeyDoorBottomObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.KeyDoorLeftObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.KeyDoorRightObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.OpenDoorTopObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.OpenDoorBottomObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.OpenDoorLeftObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.OpenDoorRightObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.OpenDoorTopObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.OpenDoorBottomObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.OpenDoorLeftObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.OpenDoorRightObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.BossDoorObjectIds.address()},
        {VERTICAL_OFFSETS_ADDR, RomSymbols.StairsDoorObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.RevolvingDoorObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.OneWayArrowObjectIds.address()},
        {DUNGEON_ENTRANCE_OFFSETS_ADDR, RomSymbols.DungeonEntranceObjectIds.address()},
        {HORIZONTAL_OFFSETS_ADDR, RomSymbols.EntranceObjectIds.address()},
    };
    private static final int INDOOR_DOOR_MACRO_BASE = 0xEC;
    private static final int INDOOR_DOOR_MACRO_LAST = 0xFD;

    private static final int OBJECT_CLOSED_GATE = 0xC2;
    private static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    private static final int OBJECT_HOUSE_DOOR_VARIANT = 0xE2;
    private static final int OBJECT_CAVE_DOOR = 0xE3;
    private static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    private static final int OBJECT_BOMBABLE_BLOCK = 0xA9;
    private static final int OBJECT_KEYHOLE_BLOCK = 0xDE;
    private static final int OBJECT_FLOOR_OD = 0x0D;
    private static final int OBJECT_STAIRS_DOWN = 0xBE;
    private static final int OBJECT_HIDDEN_STAIRS_DOWN = 0xBF;
    private static final int OBJECT_STAIRS_UP = 0xCB;
    private static final int OBJECT_GROUND_STAIRS = 0xC6;
    private static final int OBJECT_DOOR_CB = 0xCB;
    private static final int OBJECT_DOOR_61 = 0x61;
    private static final int OBJECT_DOOR_C5 = 0xC5;

    private static final int OVERWORLD_MACROS_BANK = RomSymbols.CatfishMawObjectOffsets.bank();
    private static final int CATFISH_MAW_OBJECT_OFFSETS_ADDR = RomSymbols.CatfishMawObjectOffsets.address();
    private static final int CATFISH_MAW_OBJECT_IDS_ADDR = RomSymbols.CatfishMawObjectIds.address();
    private static final int TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR = RomSymbols.TwoDoorsHouseObjectOffsets.address();
    private static final int TWO_DOORS_HOUSE_OBJECT_IDS_ADDR = RomSymbols.TwoDoorsHouseObjectIds.address();
    private static final int LARGE_HOUSE_OBJECT_OFFSETS_ADDR = RomSymbols.LargeHouseObjectOffsets.address();
    private static final int LARGE_HOUSE_OBJECT_IDS_ADDR = RomSymbols.LargeHouseObjectIds.address();
    private static final int PALACE_DOOR_OBJECT_OFFSETS_ADDR = RomSymbols.PalaceDoorObjectOffsets.address();
    private static final int PALACE_DOOR_OBJECT_IDS_ADDR = RomSymbols.PalaceDoorObjectIds.address();
    private static final int STONE_PIG_HEAD_OBJECT_OFFSETS_ADDR = RomSymbols.StonePigHeadObjectOffsets.address();
    private static final int STONE_PIG_HEAD_OBJECT_IDS_INTACT_ADDR = RomSymbols.StonePigHeadObjectIds_Intact.address();
    private static final int STONE_PIG_HEAD_OBJECT_IDS_BLASTED_ADDR =
            RomSymbols.StonePigHeadObjectIds_Blasted.address();
    private static final int PALM_TREE_OBJECT_OFFSETS_ADDR = RomSymbols.PalmTreeObjectOffsets.address();
    private static final int PALM_TREE_OBJECT_IDS_ADDR = RomSymbols.PalmTreeObjectIds.address();
    private static final int WALLED_PIT_OBJECT_OFFSETS_ADDR = RomSymbols.WalledPitObjectOffsets.address();
    private static final int WALLED_PIT_OBJECT_IDS_ADDR = RomSymbols.WalledPitObjectIds.address();
    private static final int SMALL_HOUSE_OBJECT_OFFSETS_ADDR = RomSymbols.SmallHouseObjectOffsets.address();
    private static final int SMALL_HOUSE_OBJECT_IDS_ADDR = RomSymbols.SmallHouseObjectIds.address();

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
    private int floorObject;
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
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
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
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_CAVE_DOOR) {
                    roomObjectsArea[areaIndex] = OBJECT_ROCKY_CAVE_DOOR;
                }
            }
        }
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate) {
        return parseIndoor(streamOffset, floorAndTemplate, 0);
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate, int roomStatusFlags) {
        return parseIndoor(streamOffset, floorAndTemplate, roomStatusFlags, -1);
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate, int roomStatusFlags, int mapId) {
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
        applyIndoorKeyholeBlockStatus();
        applyIndoorBombableWallStatus();
        assignDoorPositionsToWarps();
        return new RoomObjectParseResult(roomObjectsArea, warps, shutterDoorMask, staircaseLocation);
    }

    int updatedRoomStatus() {
        return roomStatusFlags;
    }

    /** Applies one source indoor-door macro to an already decoded room buffer. */
    void applyIndoorDoorMacro(int[] targetRoomObjectsArea, int type, int location) {
        if (targetRoomObjectsArea == null
                || targetRoomObjectsArea.length < 0x100
                || type < INDOOR_DOOR_MACRO_BASE
                || type > INDOOR_DOOR_MACRO_LAST) {
            throw new IllegalArgumentException("Invalid indoor door macro target");
        }
        roomObjectsArea = targetRoomObjectsArea;
        applyMacroTable(
                location,
                INDOOR_DOOR_MACRO_BANK,
                INDOOR_DOOR_MACRO_TABLES[type - INDOOR_DOOR_MACRO_BASE][0],
                INDOOR_DOOR_MACRO_BANK,
                INDOOR_DOOR_MACRO_TABLES[type - INDOOR_DOOR_MACRO_BASE][1]);
    }

    /** Applies the final closed-shutter object pair written by {@code func_002_5C04}. */
    void applyIndoorShutterClosedState(int[] targetRoomObjectsArea, int shutterMask) {
        if (targetRoomObjectsArea == null || targetRoomObjectsArea.length < 0x100) {
            throw new IllegalArgumentException("Invalid indoor shutter target");
        }
        int offsetsOffset = RomSymbols.Data_002_5BF4.offset();
        int idsOffset = RomSymbols.Data_002_5BFC.offset();
        for (int direction = 0; direction < 4; direction++) {
            if ((shutterMask & (1 << direction)) == 0) {
                continue;
            }
            int location = indoorShutterLocation(targetRoomObjectsArea, direction);
            if (location < 0) {
                continue;
            }
            int baseIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
            for (int half = 0; half < 2; half++) {
                int tableIndex = direction + half * 4;
                int targetIndex = baseIndex + (readRomByte(offsetsOffset + tableIndex));
                targetRoomObjectsArea[targetIndex] = readRomByte(idsOffset + tableIndex);
            }
        }
    }

    /** Finds a shutter's source macro location from either its open or closed object pair. */
    int indoorShutterLocation(int[] targetRoomObjectsArea, int direction) {
        if (targetRoomObjectsArea == null || targetRoomObjectsArea.length < 0x100 || direction < 0 || direction >= 4) {
            throw new IllegalArgumentException("Invalid indoor shutter lookup");
        }
        int[] openIdsAddresses = {
            RomSymbols.OpenDoorTopObjectIds.address(),
            RomSymbols.OpenDoorBottomObjectIds.address(),
            RomSymbols.OpenDoorLeftObjectIds.address(),
            RomSymbols.OpenDoorRightObjectIds.address()
        };
        int openIdsOffset = RomBank.romOffset(INDOOR_DOOR_MACRO_BANK, openIdsAddresses[direction]);
        int closedIdsOffset = RomSymbols.Data_002_5BFC.offset();
        int openFirst = readRomByte(openIdsOffset);
        int openSecond = readRomByte(openIdsOffset + 1);
        int closedFirst = readRomByte(closedIdsOffset + direction);
        int closedSecond = readRomByte(closedIdsOffset + direction + 4);
        int pairOffset = direction < 2 ? 1 : RoomConstants.ROOM_OBJECT_ROW_STRIDE;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int index = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                int secondIndex = index + pairOffset;
                if (secondIndex >= targetRoomObjectsArea.length) {
                    continue;
                }
                int first = targetRoomObjectsArea[index] & 0xFF;
                int second = targetRoomObjectsArea[secondIndex] & 0xFF;
                if ((first == openFirst && second == openSecond) || (first == closedFirst && second == closedSecond)) {
                    return (row << 4) | column;
                }
            }
        }
        return -1;
    }

    private void applyIndoorBombableBlockStatus(int mapId) {
        // ConfigureRoomObjects only admits the breakable-block replacement on
        // MAP_CAVE_B and later indoor maps, matching the source's map gate.
        if (mapId < 0x0A || (roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_BLOCK) {
                    roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    private void applyIndoorKeyholeBlockStatus() {
        if ((roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (roomObjectsArea[areaIndex] == OBJECT_KEYHOLE_BLOCK) {
                    roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    private void applyIndoorBombableWallStatus() {
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                int objectId = roomObjectsArea[areaIndex];
                if ((objectId == 0x3F || objectId == 0x47) && (roomStatusFlags & 0x04) != 0) {
                    roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x40 || objectId == 0x48) && (roomStatusFlags & 0x08) != 0) {
                    roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x41 || objectId == 0x49) && (roomStatusFlags & 0x02) != 0) {
                    roomObjectsArea[areaIndex] = 0x3E;
                } else if ((objectId == 0x42 || objectId == 0x4A) && (roomStatusFlags & 0x01) != 0) {
                    roomObjectsArea[areaIndex] = 0x3E;
                }
            }
        }
    }

    /** Mirrors ConfigureRoomObjects' source-marker and collected-chest replacements. */
    private void applyChestStatus() {
        boolean chestOpened = (roomStatusFlags & ROOM_STATUS_CHEST_OPEN) != 0;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (!chestOpened && roomObjectsArea[areaIndex] == OBJECT_CHEST_OPEN) {
                    roomObjectsArea[areaIndex] = floorObject;
                } else if (chestOpened && roomObjectsArea[areaIndex] == OBJECT_CHEST_CLOSED) {
                    roomObjectsArea[areaIndex] = OBJECT_CHEST_OPEN;
                }
            }
        }
    }

    private void reset(int floorObject) {
        this.floorObject = floorObject;
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
            int firstByte = readRomByte(pos);
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
                location = readRomByte(pos + 1);
                type = readRomByte(pos + 2);
                pos += 3;
                if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    expandMacro(type, location, firstByte);
                } else if (isIndoor
                        && type == OBJECT_HIDDEN_STAIRS_DOWN
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
                type = readRomByte(pos + 1);
                pos += 2;
                if (isIndoor && type >= INDOOR_DOOR_MACRO_BASE && type <= INDOOR_DOOR_MACRO_LAST) {
                    expandIndoorDoorMacro(type, location);
                } else if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    expandMacro(type, location, 0);
                } else if (isIndoor
                        && type == OBJECT_HIDDEN_STAIRS_DOWN
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
            int statusBit =
                    switch (idx) {
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
        // Closed shutters also call the open-door loaders ($00:365B..371E).
        // Those loaders persist the directional minimap bits through $00:36C4.
        if (idx >= 4 && idx <= 11) {
            roomStatusFlags |= switch (idx & 3) {
                case 0 -> 4;
                case 1 -> 8;
                case 2 -> 2;
                default -> 1;
            };
        }
        if (idx == 16) roomStatusFlags |= 8; // Dungeon entrance, $00:37A2.
        if (idx == 17) roomStatusFlags |= 1; // Indoor entrance, $00:37CF.
        int[] table = INDOOR_DOOR_MACRO_TABLES[idx];
        applyMacroTable(location, INDOOR_DOOR_MACRO_BANK, table[0], INDOOR_DOOR_MACRO_BANK, table[1]);
    }

    private void applyMacroTable(int baseLocation, int offsetsBank, int offsetsAddr, int idsBank, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + baseLocation;
        int offsetsOffset = RomBank.romOffset(offsetsBank, offsetsAddr);
        int idsOffset = RomBank.romOffset(idsBank, idsAddr);

        while (offsetsOffset < romData.length && idsOffset < romData.length) {
            int offset = readRomByte(offsetsOffset++);
            if (offset == 0xFF) {
                break;
            }
            int objectId = readRomByte(idsOffset++);
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
        int category = readRomByte(pos) & 0x03;
        int destMap = readRomByte(pos + 1);
        int destRoom = readRomByte(pos + 2);
        int destX = readRomByte(pos + 3);
        int destY = readRomByte(pos + 4);
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
            case 1 -> copyOutdoorsMacroObjects(
                    location, TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR, TWO_DOORS_HOUSE_OBJECT_IDS_ADDR);
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
            default -> {}
        }
    }

    private void handleTreeMacro(int location, int objectData) {
        // TreeMacroHandler ($24:759D) adds in A; the padded-buffer
        // address is an eight-bit offset, including off-screen starts $F9/$FF.
        int areaIndex = (RoomConstants.ROOM_OBJECTS_BASE + location) & 0xFF;
        if ((areaIndex & 0x0F) == 0) {
            areaIndex = (areaIndex - RoomConstants.ROOM_OBJECT_ROW_STRIDE) & 0xFF;
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
            copyTreeObjectsToRoom((areaIndex + i * step) & 0xFF);
        }
    }

    private void copyTreeObjectsToRoom(int areaIndex) {
        if (!isAreaIndexValid(areaIndex) || !isAreaIndexValid(areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE + 1)) {
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
            int offset = readRomByte(offsetsOffset++);
            if (offset == 0xFF) {
                break;
            }
            int objectId = readRomByte(idsOffset++);
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
