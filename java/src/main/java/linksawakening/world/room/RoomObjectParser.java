package linksawakening.world.room;

import static linksawakening.world.room.RoomObjectFormat.CATFISH_MAW_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.CATFISH_MAW_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.INDOOR_DOOR_MACRO_BANK;
import static linksawakening.world.room.RoomObjectFormat.INDOOR_DOOR_MACRO_BASE;
import static linksawakening.world.room.RoomObjectFormat.INDOOR_DOOR_MACRO_LAST;
import static linksawakening.world.room.RoomObjectFormat.INDOOR_DOOR_MACRO_TABLES;
import static linksawakening.world.room.RoomObjectFormat.LARGE_HOUSE_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.LARGE_HOUSE_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_BOMBABLE_BLOCK;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_BOMBABLE_CAVE_DOOR;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_CAVE_DOOR;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_CHEST_CLOSED;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_CHEST_OPEN;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_CLOSED_GATE;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_DOOR_61;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_DOOR_C5;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_DOOR_CB;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_FLOOR_OD;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_GROUND_STAIRS;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_HIDDEN_STAIRS_DOWN;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_HOUSE_DOOR_VARIANT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_KEYHOLE_BLOCK;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_MACRO_BASE;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_ROCKY_CAVE_DOOR;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_STAIRS_DOWN;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_STAIRS_UP;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_BOTTOM_LEFT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_BOTTOM_RIGHT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_BUSHES_BOTTOM_LEFT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_BUSHES_BOTTOM_RIGHT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_OVERLAP_LEFT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_OVERLAP_RIGHT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_TOP_LEFT;
import static linksawakening.world.room.RoomObjectFormat.OBJECT_TREE_TOP_RIGHT;
import static linksawakening.world.room.RoomObjectFormat.OVERWORLD_MACROS_BANK;
import static linksawakening.world.room.RoomObjectFormat.OW_ROOM_STATUS_OPENED;
import static linksawakening.world.room.RoomObjectFormat.PALACE_DOOR_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.PALACE_DOOR_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.PALM_TREE_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.PALM_TREE_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.ROOM_STATUS_CHEST_OPEN;
import static linksawakening.world.room.RoomObjectFormat.ROOM_STATUS_DOOR_OPEN_UP;
import static linksawakening.world.room.RoomObjectFormat.ROOM_STATUS_EVENT_1;
import static linksawakening.world.room.RoomObjectFormat.ROOM_STATUS_EVENT_3;
import static linksawakening.world.room.RoomObjectFormat.ROOM_TEMPLATE_BANK;
import static linksawakening.world.room.RoomObjectFormat.ROOM_TEMPLATE_TABLES;
import static linksawakening.world.room.RoomObjectFormat.SMALL_HOUSE_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.SMALL_HOUSE_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.STONE_PIG_HEAD_OBJECT_IDS_BLASTED_ADDR;
import static linksawakening.world.room.RoomObjectFormat.STONE_PIG_HEAD_OBJECT_IDS_INTACT_ADDR;
import static linksawakening.world.room.RoomObjectFormat.STONE_PIG_HEAD_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.TWO_DOORS_HOUSE_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.WALLED_PIT_OBJECT_IDS_ADDR;
import static linksawakening.world.room.RoomObjectFormat.WALLED_PIT_OBJECT_OFFSETS_ADDR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;
import linksawakening.world.transition.Warp;

/** LoadRoomObject: owns stream walking, macro expansion, status patching and decode scratch state. */
final class RoomObjectParser {
    private final Dependencies dependencies;

    int readRomByte(int offset) {
        return dependencies.romData().u8(RomAddress.fromOffset(offset));
    }

    public RoomObjectParser(Rom romData) {
        this.dependencies = new Dependencies(romData);
    }

    public RoomObjectParseResult parseOverworld(int streamOffset, int floorObject, int roomStatusFlags) {
        reset(floorObject);
        this.roomStatusFlags = roomStatusFlags;
        parseRoomObjectStream(streamOffset, false);
        applyChestStatus();
        applyOverworldClosedGateStatus();
        applyOverworldBombableCaveDoorStatus();
        assignDoorPositionsToWarps();
        return new RoomObjectParseResult(this.roomObjectsArea, this.warps, 0, this.staircaseLocation);
    }

    /** Mirrors LoadRoomObject's OBJECT_CLOSED_GATE status-bit replacement. */
    void applyOverworldClosedGateStatus() {
        if ((this.roomStatusFlags & ROOM_STATUS_CHEST_OPEN) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (this.roomObjectsArea[areaIndex] == OBJECT_CLOSED_GATE) {
                    this.roomObjectsArea[areaIndex] = OBJECT_CAVE_DOOR;
                }
            }
        }
    }

    void applyOverworldBombableCaveDoorStatus() {
        if ((this.roomStatusFlags & OW_ROOM_STATUS_OPENED) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (this.roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_CAVE_DOOR) {
                    this.roomObjectsArea[areaIndex] = OBJECT_ROCKY_CAVE_DOOR;
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
        return new RoomObjectParseResult(
                this.roomObjectsArea, this.warps, this.shutterDoorMask, this.staircaseLocation);
    }

    int updatedRoomStatus() {
        return this.roomStatusFlags;
    }

    /** Applies one source indoor-door macro to an already decoded room buffer. */
    void applyIndoorDoorMacro(int[] targetRoomObjectsArea, int type, int location) {
        if (targetRoomObjectsArea == null
                || targetRoomObjectsArea.length < 0x100
                || type < INDOOR_DOOR_MACRO_BASE
                || type > INDOOR_DOOR_MACRO_LAST) {
            throw new IllegalArgumentException("Invalid indoor door macro target");
        }
        this.roomObjectsArea = targetRoomObjectsArea;
        applyMacroTable(
                location,
                INDOOR_DOOR_MACRO_BANK,
                INDOOR_DOOR_MACRO_TABLES[type - INDOOR_DOOR_MACRO_BASE][0],
                INDOOR_DOOR_MACRO_BANK,
                INDOOR_DOOR_MACRO_TABLES[type - INDOOR_DOOR_MACRO_BASE][1]);
    }

    /** Applies the final closed-shutter object pair written by the source handler. */
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
                int targetIndex = baseIndex + readRomByte(offsetsOffset + tableIndex);
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

    void applyIndoorBombableBlockStatus(int mapId) {
        // ConfigureRoomObjects only admits the breakable-block replacement on
        // MAP_CAVE_B and later indoor maps, matching the source's map gate.
        if (mapId < 0x0A || (this.roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (this.roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_BLOCK) {
                    this.roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    void applyIndoorKeyholeBlockStatus() {
        if ((this.roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (this.roomObjectsArea[areaIndex] == OBJECT_KEYHOLE_BLOCK) {
                    this.roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    void applyIndoorBombableWallStatus() {
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                int objectId = this.roomObjectsArea[areaIndex];
                if ((objectId == 0x3F || objectId == 0x47) && (this.roomStatusFlags & 0x04) != 0) {
                    this.roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x40 || objectId == 0x48) && (this.roomStatusFlags & 0x08) != 0) {
                    this.roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x41 || objectId == 0x49) && (this.roomStatusFlags & 0x02) != 0) {
                    this.roomObjectsArea[areaIndex] = 0x3E;
                } else if ((objectId == 0x42 || objectId == 0x4A) && (this.roomStatusFlags & 0x01) != 0) {
                    this.roomObjectsArea[areaIndex] = 0x3E;
                }
            }
        }
    }

    /** Mirrors ConfigureRoomObjects' source-marker and collected-chest replacements. */
    void applyChestStatus() {
        boolean chestOpened = (this.roomStatusFlags & ROOM_STATUS_CHEST_OPEN) != 0;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (!chestOpened && this.roomObjectsArea[areaIndex] == OBJECT_CHEST_OPEN) {
                    this.roomObjectsArea[areaIndex] = this.floorObject;
                } else if (chestOpened && this.roomObjectsArea[areaIndex] == OBJECT_CHEST_CLOSED) {
                    this.roomObjectsArea[areaIndex] = OBJECT_CHEST_OPEN;
                }
            }
        }
    }

    private void reset(int floorObject) {
        this.floorObject = floorObject;
        this.roomObjectsArea = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(this.roomObjectsArea, 0xFF);
        this.warps.clear();
        this.doorPositions.clear();
        this.shutterDoorMask = 0;
        this.staircaseLocation = -1;
        fillRoomMapWithObject(floorObject);
    }

    void parseRoomObjectStream(int pos, boolean isIndoor) {
        while (pos < dependencies.romData().size() - 1) {
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
                if (pos + 2 >= dependencies.romData().size()) {
                    break;
                }
                location = readRomByte(pos + 1);
                type = readRomByte(pos + 2);
                pos += 3;
                if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    expandMacro(type, location, firstByte);
                } else if (isIndoor
                        && type == OBJECT_HIDDEN_STAIRS_DOWN
                        && (this.roomStatusFlags & ROOM_STATUS_EVENT_1) == 0) {
                    // ConfigureRoomObjects returns before copying concealed stairs.
                } else {
                    fillRoomWithConsecutiveObjects(type, location, firstByte);
                    recordStaircaseLocation(type, location, isIndoor);
                    if (isDoorObjectId(type)) {
                        recordStripDoorPositions(location, firstByte);
                    }
                }
            } else {
                if (pos + 1 >= dependencies.romData().size()) {
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
                        && (this.roomStatusFlags & ROOM_STATUS_EVENT_1) == 0) {
                    // ConfigureRoomObjects leaves the room-template floor in place.
                } else {
                    copyObjectToActiveRoomMap(type, location);
                    recordStaircaseLocation(type, location, isIndoor);
                    if (isDoorObjectId(type)) {
                        this.doorPositions.add(location);
                    }
                }
            }
        }
    }

    void expandIndoorDoorMacro(int type, int location) {
        int doorMacroIndex = type - INDOOR_DOOR_MACRO_BASE;
        if (doorMacroIndex < 0 || doorMacroIndex >= INDOOR_DOOR_MACRO_TABLES.length) {
            return;
        }
        // ConfigureRoomObjects selects the matching open-door macro when the
        // directional status bit was synchronized by the adjacent room.
        if (doorMacroIndex < FIRST_SHUTTER_MACRO) {
            int statusBit = DOOR_STATUS_BITS[doorMacroIndex];
            if ((this.roomStatusFlags & statusBit) != 0) {
                doorMacroIndex += FIRST_OPEN_DOOR_MACRO;
            }
        } else if (doorMacroIndex == BOSS_DOOR_MACRO && (this.roomStatusFlags & 0x04) != 0) {
            // OBJECT_BOSS_DOOR is top-facing and reloads as OpenDoorTop once
            // ROOM_STATUS_DOOR_OPEN_UP has been synchronized.
            doorMacroIndex = FIRST_OPEN_DOOR_MACRO;
        }
        if (doorMacroIndex >= FIRST_SHUTTER_MACRO && doorMacroIndex < FIRST_OPEN_DOOR_MACRO) {
            // wC18A/wC18B use top, bottom, left, right in bits 0..3.
            this.shutterDoorMask |= 1 << (doorMacroIndex - FIRST_SHUTTER_MACRO);
        }
        // Closed shutters also call the open-door loaders ($00:365B..371E).
        // Those loaders persist the directional minimap bits through $00:36C4.
        if (doorMacroIndex >= FIRST_SHUTTER_MACRO && doorMacroIndex < BOSS_DOOR_MACRO) {
            this.roomStatusFlags |= DOOR_STATUS_BITS[doorMacroIndex & 3];
        }
        if (doorMacroIndex == 16) this.roomStatusFlags |= 8; // Dungeon entrance, $00:37A2.
        if (doorMacroIndex == 17) this.roomStatusFlags |= 1; // Indoor entrance, $00:37CF.
        int[] table = INDOOR_DOOR_MACRO_TABLES[doorMacroIndex];
        applyMacroTable(location, INDOOR_DOOR_MACRO_BANK, table[0], INDOOR_DOOR_MACRO_BANK, table[1]);
    }

    void applyMacroTable(int baseLocation, int offsetsBank, int offsetsAddr, int idsBank, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + baseLocation;
        int offsetsOffset = RomBank.romOffset(offsetsBank, offsetsAddr);
        int idsOffset = RomBank.romOffset(idsBank, idsAddr);

        while (offsetsOffset < dependencies.romData().size()
                && idsOffset < dependencies.romData().size()) {
            int offset = readRomByte(offsetsOffset++);
            if (offset == MACRO_END) {
                break;
            }
            int objectId = readRomByte(idsOffset++);
            int targetAreaIndex = baseAreaIndex + offset;
            if (isAreaIndexValid(targetAreaIndex)) {
                this.roomObjectsArea[targetAreaIndex] = objectId;
                if (isMacroExpansionDoorId(objectId)) {
                    this.doorPositions.add(targetAreaIndex - RoomConstants.ROOM_OBJECTS_BASE);
                }
            }
        }
    }

    void collectWarpRecord(int pos) {
        if (pos + 4 >= dependencies.romData().size()) {
            return;
        }
        int category = readRomByte(pos) & 0x03;
        int destMap = readRomByte(pos + 1);
        int destRoom = readRomByte(pos + 2);
        int destX = readRomByte(pos + 3);
        int destY = readRomByte(pos + 4);
        this.warps.add(new Warp(category, destMap, destRoom, destX, destY, -1));
    }

    void assignDoorPositionsToWarps() {
        int count = Math.min(this.doorPositions.size(), this.warps.size());
        for (int i = 0; i < count; i++) {
            int location = this.doorPositions.get(i);
            Warp original = this.warps.get(i);
            this.warps.set(i, original.withTileLocation(location));
        }
    }

    boolean isDoorObjectId(int objectId) {
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

    boolean isMacroExpansionDoorId(int objectId) {
        return objectId == OBJECT_ROCKY_CAVE_DOOR
                || objectId == OBJECT_HOUSE_DOOR_VARIANT
                || objectId == OBJECT_CAVE_DOOR;
    }

    void recordStripDoorPositions(int location, int objectData) {
        int count = objectData & 0x0F;
        if (count == 0) {
            return;
        }
        int step = (objectData & 0x40) != 0 ? RoomConstants.ROOM_OBJECT_ROW_STRIDE : 1;
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        for (int i = 0; i < count; i++) {
            if (isAreaIndexValid(areaIndex)) {
                this.doorPositions.add(areaIndex - RoomConstants.ROOM_OBJECTS_BASE);
            }
            areaIndex += step;
        }
    }

    void expandMacro(int macroType, int location, int objectData) {
        int macroIndex = macroType - OBJECT_MACRO_BASE;
        switch (macroIndex) {
            case 0 -> handleTreeMacro(location, objectData);
            case 1 -> copyOutdoorsMacroObjects(
                    location, TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR, TWO_DOORS_HOUSE_OBJECT_IDS_ADDR);
            case 2 -> copyOutdoorsMacroObjects(location, LARGE_HOUSE_OBJECT_OFFSETS_ADDR, LARGE_HOUSE_OBJECT_IDS_ADDR);
            case 3 -> copyOutdoorsMacroObjects(location, CATFISH_MAW_OBJECT_OFFSETS_ADDR, CATFISH_MAW_OBJECT_IDS_ADDR);
            case 4 -> copyOutdoorsMacroObjects(location, PALACE_DOOR_OBJECT_OFFSETS_ADDR, PALACE_DOOR_OBJECT_IDS_ADDR);
            case 5 -> {
                int pigIdsAddr = (this.roomStatusFlags & ROOM_STATUS_DOOR_OPEN_UP) == 0
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

    void handleTreeMacro(int location, int objectData) {
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

    void copyTreeObjectsToRoom(int areaIndex) {
        if (!isAreaIndexValid(areaIndex) || !isAreaIndexValid(areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE + 1)) {
            return;
        }
        int topLeftOffset = areaIndex;
        int topRightOffset = areaIndex + 1;
        int bottomLeftOffset = areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE;
        int bottomRightOffset = bottomLeftOffset + 1;

        int existingTopLeft = this.roomObjectsArea[topLeftOffset];
        this.roomObjectsArea[topLeftOffset] = existingTopLeft < 0x10 ? OBJECT_TREE_TOP_LEFT : OBJECT_TREE_OVERLAP_LEFT;

        int existingTopRight = this.roomObjectsArea[topRightOffset];
        this.roomObjectsArea[topRightOffset] =
                existingTopRight < 0x10 ? OBJECT_TREE_TOP_RIGHT : OBJECT_TREE_OVERLAP_RIGHT;

        int existingBottomLeft = this.roomObjectsArea[bottomLeftOffset];
        if (existingBottomLeft >= 0x8A) {
            this.roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_BUSHES_BOTTOM_LEFT;
        } else if (existingBottomLeft < 0x10) {
            this.roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_BOTTOM_LEFT;
        } else {
            this.roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_OVERLAP_RIGHT;
        }

        int existingBottomRight = this.roomObjectsArea[bottomRightOffset];
        if (existingBottomRight >= 0x8A) {
            this.roomObjectsArea[bottomRightOffset] = OBJECT_TREE_BUSHES_BOTTOM_RIGHT;
        } else if (existingBottomRight < 0x10) {
            this.roomObjectsArea[bottomRightOffset] = OBJECT_TREE_BOTTOM_RIGHT;
        } else {
            this.roomObjectsArea[bottomRightOffset] = OBJECT_TREE_OVERLAP_LEFT;
        }
    }

    void copyOutdoorsMacroObjects(int location, int offsetsAddr, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        int offsetsOffset = RomBank.romOffset(OVERWORLD_MACROS_BANK, offsetsAddr);
        int idsOffset = RomBank.romOffset(OVERWORLD_MACROS_BANK, idsAddr);

        while (offsetsOffset < dependencies.romData().size()
                && idsOffset < dependencies.romData().size()) {
            int offset = readRomByte(offsetsOffset++);
            if (offset == MACRO_END) {
                break;
            }
            int objectId = readRomByte(idsOffset++);
            int targetAreaIndex = baseAreaIndex + offset;
            if (isAreaIndexValid(targetAreaIndex)) {
                this.roomObjectsArea[targetAreaIndex] = objectId;
                if (isMacroExpansionDoorId(objectId)) {
                    this.doorPositions.add(targetAreaIndex - RoomConstants.ROOM_OBJECTS_BASE);
                }
            }
        }
    }

    void fillRoomMapWithObject(int objectId) {
        for (int i = 0; i < RoomConstants.ACTIVE_ROOM_OBJECT_BYTES; i++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + i;
            int column = areaIndex & 0x0F;
            if (column != 0 && column < 0x0B) {
                this.roomObjectsArea[areaIndex] = objectId;
            }
        }
    }

    void copyObjectToActiveRoomMap(int objectId, int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        if (isAreaIndexValid(areaIndex)) {
            this.roomObjectsArea[areaIndex] = objectId;
        }
    }

    void fillRoomWithConsecutiveObjects(int objectId, int location, int objectData) {
        int count = objectData & 0x0F;
        if (count == 0) {
            return;
        }
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        int step = (objectData & 0x40) != 0 ? RoomConstants.ROOM_OBJECT_ROW_STRIDE : 1;
        for (int i = 0; i < count; i++) {
            if (isAreaIndexValid(areaIndex)) {
                this.roomObjectsArea[areaIndex] = objectId;
            }
            areaIndex += step;
        }
    }

    boolean isAreaIndexValid(int areaIndex) {
        return areaIndex >= 0 && areaIndex < this.roomObjectsArea.length;
    }

    void recordStaircaseLocation(int objectId, int location, boolean isIndoor) {
        if (RoomObjectParser.isStaircaseObject(objectId, isIndoor)) {
            this.staircaseLocation = location & 0xFF;
        }
    }

    static boolean isStaircaseObject(int objectId, boolean isIndoor) {
        return isIndoor
                ? objectId == OBJECT_STAIRS_DOWN
                        || objectId == OBJECT_HIDDEN_STAIRS_DOWN
                        || objectId == OBJECT_STAIRS_UP
                : objectId == OBJECT_DOOR_C5 || objectId == OBJECT_GROUND_STAIRS;
    }

    record Dependencies(Rom romData) {
        Dependencies {
            Objects.requireNonNull(romData, "romData");
        }
    }

    private static final int FIRST_SHUTTER_MACRO = 4;
    private static final int FIRST_OPEN_DOOR_MACRO = 8;
    private static final int BOSS_DOOR_MACRO = 12;
    private static final int MACRO_END = 0xFF;
    private static final int[] DOOR_STATUS_BITS = {0x04, 0x08, 0x02, 0x01};

    int[] roomObjectsArea;
    int shutterDoorMask;
    int staircaseLocation = -1;
    int floorObject;
    final List<Warp> warps = new ArrayList<>();
    final List<Integer> doorPositions = new ArrayList<>();
    int roomStatusFlags;
}
