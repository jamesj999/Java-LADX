package linksawakening.world.room;

import static linksawakening.world.room.RoomObjectFormat.*;

import java.util.Arrays;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;

public final class RoomObjectParser {
    private final Dependencies dependencies;
    private final RoomObjectMacroDecoder roomObjectMacroDecoder;
    private final RoomObjectStreamDecoder roomObjectStreamDecoder;
    private final RoomObjectStatusPatches roomObjectStatusPatches;

    int readRomByte(int offset) {
        return dependencies.romData().u8(RomAddress.fromOffset(offset));
    }

    public RoomObjectParser(Rom romData) {
        this.dependencies = new Dependencies(romData, new RoomObjectDecodeState());
        roomObjectMacroDecoder = new RoomObjectMacroDecoder(
                new RoomObjectMacroDecoder.Dependencies(dependencies.romData(), dependencies.state(), () -> this));
        roomObjectStreamDecoder = new RoomObjectStreamDecoder(
                new RoomObjectStreamDecoder.Dependencies(dependencies.romData(), dependencies.state(), () -> this));
        roomObjectStatusPatches =
                new RoomObjectStatusPatches(new RoomObjectStatusPatches.Dependencies(dependencies.state(), () -> this));
    }

    public RoomObjectParseResult parseOverworld(int streamOffset, int floorObject, int roomStatusFlags) {
        reset(floorObject);
        dependencies.state().roomStatusFlags = roomStatusFlags;
        parseRoomObjectStream(streamOffset, false);
        applyChestStatus();
        applyOverworldClosedGateStatus();
        applyOverworldBombableCaveDoorStatus();
        assignDoorPositionsToWarps();
        return new RoomObjectParseResult(
                dependencies.state().roomObjectsArea,
                dependencies.state().warps,
                0,
                dependencies.state().staircaseLocation);
    }

    /** Mirrors LoadRoomObject's OBJECT_CLOSED_GATE status-bit replacement. */
    void applyOverworldClosedGateStatus() {
        roomObjectStatusPatches.applyOverworldClosedGateStatus();
    }

    void applyOverworldBombableCaveDoorStatus() {
        roomObjectStatusPatches.applyOverworldBombableCaveDoorStatus();
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate) {
        return parseIndoor(streamOffset, floorAndTemplate, 0);
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate, int roomStatusFlags) {
        return parseIndoor(streamOffset, floorAndTemplate, roomStatusFlags, -1);
    }

    public RoomObjectParseResult parseIndoor(int streamOffset, int floorAndTemplate, int roomStatusFlags, int mapId) {
        reset(floorAndTemplate & 0x0F);
        dependencies.state().roomStatusFlags = roomStatusFlags;
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
                dependencies.state().roomObjectsArea,
                dependencies.state().warps,
                dependencies.state().shutterDoorMask,
                dependencies.state().staircaseLocation);
    }

    int updatedRoomStatus() {
        return dependencies.state().roomStatusFlags;
    }

    /** Applies one source indoor-door macro to an already decoded room buffer. */
    void applyIndoorDoorMacro(int[] targetRoomObjectsArea, int type, int location) {
        if (targetRoomObjectsArea == null
                || targetRoomObjectsArea.length < 0x100
                || type < INDOOR_DOOR_MACRO_BASE
                || type > INDOOR_DOOR_MACRO_LAST) {
            throw new IllegalArgumentException("Invalid indoor door macro target");
        }
        dependencies.state().roomObjectsArea = targetRoomObjectsArea;
        applyMacroTable(
                location,
                INDOOR_DOOR_MACRO_BANK,
                INDOOR_DOOR_MACRO_TABLES[type - INDOOR_DOOR_MACRO_BASE][0],
                INDOOR_DOOR_MACRO_BANK,
                INDOOR_DOOR_MACRO_TABLES[type - INDOOR_DOOR_MACRO_BASE][1]);
    }

    /** Applies the final closed-shutter object pair written by the source handler. */
    void applyIndoorShutterClosedState(int[] targetRoomObjectsArea, int shutterMask) {
        roomObjectStatusPatches.applyIndoorShutterClosedState(targetRoomObjectsArea, shutterMask);
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
        roomObjectStatusPatches.applyIndoorBombableBlockStatus(mapId);
    }

    void applyIndoorKeyholeBlockStatus() {
        roomObjectStatusPatches.applyIndoorKeyholeBlockStatus();
    }

    void applyIndoorBombableWallStatus() {
        roomObjectStatusPatches.applyIndoorBombableWallStatus();
    }

    /** Mirrors ConfigureRoomObjects' source-marker and collected-chest replacements. */
    void applyChestStatus() {
        roomObjectStatusPatches.applyChestStatus();
    }

    private void reset(int floorObject) {
        dependencies.state().floorObject = floorObject;
        dependencies.state().roomObjectsArea = new int[RoomConstants.ROOM_OBJECTS_AREA_SIZE];
        Arrays.fill(dependencies.state().roomObjectsArea, 0xFF);
        dependencies.state().warps.clear();
        dependencies.state().doorPositions.clear();
        dependencies.state().shutterDoorMask = 0;
        dependencies.state().staircaseLocation = -1;
        fillRoomMapWithObject(floorObject);
    }

    void parseRoomObjectStream(int pos, boolean isIndoor) {
        roomObjectStreamDecoder.parseRoomObjectStream(pos, isIndoor);
    }

    void expandIndoorDoorMacro(int type, int location) {
        roomObjectMacroDecoder.expandIndoorDoorMacro(type, location);
    }

    void applyMacroTable(int baseLocation, int offsetsBank, int offsetsAddr, int idsBank, int idsAddr) {
        roomObjectMacroDecoder.applyMacroTable(baseLocation, offsetsBank, offsetsAddr, idsBank, idsAddr);
    }

    void collectWarpRecord(int pos) {
        roomObjectStreamDecoder.collectWarpRecord(pos);
    }

    void assignDoorPositionsToWarps() {
        roomObjectStreamDecoder.assignDoorPositionsToWarps();
    }

    boolean isDoorObjectId(int objectId) {
        return roomObjectStreamDecoder.isDoorObjectId(objectId);
    }

    boolean isMacroExpansionDoorId(int objectId) {
        return roomObjectStreamDecoder.isMacroExpansionDoorId(objectId);
    }

    void recordStripDoorPositions(int location, int objectData) {
        roomObjectStreamDecoder.recordStripDoorPositions(location, objectData);
    }

    void expandMacro(int macroType, int location, int objectData) {
        roomObjectMacroDecoder.expandMacro(macroType, location, objectData);
    }

    void handleTreeMacro(int location, int objectData) {
        roomObjectMacroDecoder.handleTreeMacro(location, objectData);
    }

    void copyTreeObjectsToRoom(int areaIndex) {
        roomObjectMacroDecoder.copyTreeObjectsToRoom(areaIndex);
    }

    void copyOutdoorsMacroObjects(int location, int offsetsAddr, int idsAddr) {
        roomObjectMacroDecoder.copyOutdoorsMacroObjects(location, offsetsAddr, idsAddr);
    }

    void fillRoomMapWithObject(int objectId) {
        roomObjectStreamDecoder.fillRoomMapWithObject(objectId);
    }

    void copyObjectToActiveRoomMap(int objectId, int location) {
        roomObjectStreamDecoder.copyObjectToActiveRoomMap(objectId, location);
    }

    void fillRoomWithConsecutiveObjects(int objectId, int location, int objectData) {
        roomObjectStreamDecoder.fillRoomWithConsecutiveObjects(objectId, location, objectData);
    }

    boolean isAreaIndexValid(int areaIndex) {
        return roomObjectStreamDecoder.isAreaIndexValid(areaIndex);
    }

    void recordStaircaseLocation(int objectId, int location, boolean isIndoor) {
        roomObjectStreamDecoder.recordStaircaseLocation(objectId, location, isIndoor);
    }

    static boolean isStaircaseObject(int objectId, boolean isIndoor) {
        return isIndoor
                ? objectId == OBJECT_STAIRS_DOWN
                        || objectId == OBJECT_HIDDEN_STAIRS_DOWN
                        || objectId == OBJECT_STAIRS_UP
                : objectId == OBJECT_DOOR_C5 || objectId == OBJECT_GROUND_STAIRS;
    }

    record Dependencies(Rom romData, RoomObjectDecodeState state) {}
}
