package linksawakening.world.room;

import static linksawakening.world.room.RoomObjectFormat.*;

import linksawakening.world.transition.Warp;

final class RoomObjectStreamDecoder {
    private final Dependencies dependencies;

    RoomObjectStreamDecoder(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    void parseRoomObjectStream(int pos, boolean isIndoor) {
        while (pos < dependencies.romData().length - 1) {
            int firstByte = dependencies.owner().get().readRomByte(pos);
            if (firstByte == RoomConstants.ROOM_END) {
                break;
            }
            if ((firstByte & 0xFC) == 0xE0) {
                dependencies.owner().get().collectWarpRecord(pos);
                pos += 5;
                continue;
            }

            boolean isStrip = (firstByte & 0x80) != 0 && (firstByte & 0x10) == 0;
            int type;
            int location;
            if (isStrip) {
                if (pos + 2 >= dependencies.romData().length) {
                    break;
                }
                location = dependencies.owner().get().readRomByte(pos + 1);
                type = dependencies.owner().get().readRomByte(pos + 2);
                pos += 3;
                if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    dependencies.owner().get().expandMacro(type, location, firstByte);
                } else if (isIndoor
                        && type == OBJECT_HIDDEN_STAIRS_DOWN
                        && (dependencies.state().roomStatusFlags & ROOM_STATUS_EVENT_1) == 0) {
                    // ConfigureRoomObjects returns before copying concealed stairs.
                } else {
                    dependencies.owner().get().fillRoomWithConsecutiveObjects(type, location, firstByte);
                    dependencies.owner().get().recordStaircaseLocation(type, location, isIndoor);
                    if (dependencies.owner().get().isDoorObjectId(type)) {
                        dependencies.owner().get().recordStripDoorPositions(location, firstByte);
                    }
                }
            } else {
                if (pos + 1 >= dependencies.romData().length) {
                    break;
                }
                location = firstByte;
                type = dependencies.owner().get().readRomByte(pos + 1);
                pos += 2;
                if (isIndoor && type >= INDOOR_DOOR_MACRO_BASE && type <= INDOOR_DOOR_MACRO_LAST) {
                    dependencies.owner().get().expandIndoorDoorMacro(type, location);
                } else if (!isIndoor && type >= OBJECT_MACRO_BASE) {
                    dependencies.owner().get().expandMacro(type, location, 0);
                } else if (isIndoor
                        && type == OBJECT_HIDDEN_STAIRS_DOWN
                        && (dependencies.state().roomStatusFlags & ROOM_STATUS_EVENT_1) == 0) {
                    // ConfigureRoomObjects leaves the room-template floor in place.
                } else {
                    dependencies.owner().get().copyObjectToActiveRoomMap(type, location);
                    dependencies.owner().get().recordStaircaseLocation(type, location, isIndoor);
                    if (dependencies.owner().get().isDoorObjectId(type)) {
                        dependencies.state().doorPositions.add(location);
                    }
                }
            }
        }
    }

    void collectWarpRecord(int pos) {
        if (pos + 4 >= dependencies.romData().length) {
            return;
        }
        int category = dependencies.owner().get().readRomByte(pos) & 0x03;
        int destMap = dependencies.owner().get().readRomByte(pos + 1);
        int destRoom = dependencies.owner().get().readRomByte(pos + 2);
        int destX = dependencies.owner().get().readRomByte(pos + 3);
        int destY = dependencies.owner().get().readRomByte(pos + 4);
        dependencies.state().warps.add(new Warp(category, destMap, destRoom, destX, destY, -1));
    }

    void assignDoorPositionsToWarps() {
        int count = Math.min(
                dependencies.state().doorPositions.size(),
                dependencies.state().warps.size());
        for (int i = 0; i < count; i++) {
            int location = dependencies.state().doorPositions.get(i);
            Warp original = dependencies.state().warps.get(i);
            dependencies.state().warps.set(i, original.withTileLocation(location));
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
            if (dependencies.owner().get().isAreaIndexValid(areaIndex)) {
                dependencies.state().doorPositions.add(areaIndex - RoomConstants.ROOM_OBJECTS_BASE);
            }
            areaIndex += step;
        }
    }

    void fillRoomMapWithObject(int objectId) {
        for (int i = 0; i < RoomConstants.ACTIVE_ROOM_OBJECT_BYTES; i++) {
            int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + i;
            int column = areaIndex & 0x0F;
            if (column != 0 && column < 0x0B) {
                dependencies.state().roomObjectsArea[areaIndex] = objectId;
            }
        }
    }

    void copyObjectToActiveRoomMap(int objectId, int location) {
        int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        if (dependencies.owner().get().isAreaIndexValid(areaIndex)) {
            dependencies.state().roomObjectsArea[areaIndex] = objectId;
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
            if (dependencies.owner().get().isAreaIndexValid(areaIndex)) {
                dependencies.state().roomObjectsArea[areaIndex] = objectId;
            }
            areaIndex += step;
        }
    }

    boolean isAreaIndexValid(int areaIndex) {
        return areaIndex >= 0 && areaIndex < dependencies.state().roomObjectsArea.length;
    }

    void recordStaircaseLocation(int objectId, int location, boolean isIndoor) {
        if (RoomObjectParser.isStaircaseObject(objectId, isIndoor)) {
            dependencies.state().staircaseLocation = location & 0xFF;
        }
    }

    record Dependencies(
            byte[] romData, RoomObjectDecodeState state, java.util.function.Supplier<RoomObjectParser> owner) {}
}
