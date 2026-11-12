package linksawakening.world.room;

import static linksawakening.world.room.RoomObjectFormat.*;

import linksawakening.rom.RomSymbols;

final class RoomObjectStatusPatches {
    private final Dependencies dependencies;

    RoomObjectStatusPatches(Dependencies dependencies) {
        this.dependencies = dependencies;
    }
    /** Mirrors LoadRoomObject's OBJECT_CLOSED_GATE status-bit replacement. */
    void applyOverworldClosedGateStatus() {
        if ((dependencies.state().roomStatusFlags & ROOM_STATUS_CHEST_OPEN) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (dependencies.state().roomObjectsArea[areaIndex] == OBJECT_CLOSED_GATE) {
                    dependencies.state().roomObjectsArea[areaIndex] = OBJECT_CAVE_DOOR;
                }
            }
        }
    }

    void applyOverworldBombableCaveDoorStatus() {
        if ((dependencies.state().roomStatusFlags & OW_ROOM_STATUS_OPENED) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (dependencies.state().roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_CAVE_DOOR) {
                    dependencies.state().roomObjectsArea[areaIndex] = OBJECT_ROCKY_CAVE_DOOR;
                }
            }
        }
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
            int location = dependencies.owner().get().indoorShutterLocation(targetRoomObjectsArea, direction);
            if (location < 0) {
                continue;
            }
            int baseIndex = RoomConstants.ROOM_OBJECTS_BASE + (location & 0xF0) + (location & 0x0F);
            for (int half = 0; half < 2; half++) {
                int tableIndex = direction + half * 4;
                int targetIndex = baseIndex + (dependencies.owner().get().readRomByte(offsetsOffset + tableIndex));
                targetRoomObjectsArea[targetIndex] = dependencies.owner().get().readRomByte(idsOffset + tableIndex);
            }
        }
    }

    void applyIndoorBombableBlockStatus(int mapId) {
        // ConfigureRoomObjects only admits the breakable-block replacement on
        // MAP_CAVE_B and later indoor maps, matching the source's map gate.
        if (mapId < 0x0A || (dependencies.state().roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (dependencies.state().roomObjectsArea[areaIndex] == OBJECT_BOMBABLE_BLOCK) {
                    dependencies.state().roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    void applyIndoorKeyholeBlockStatus() {
        if ((dependencies.state().roomStatusFlags & ROOM_STATUS_EVENT_3) == 0) {
            return;
        }
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (dependencies.state().roomObjectsArea[areaIndex] == OBJECT_KEYHOLE_BLOCK) {
                    dependencies.state().roomObjectsArea[areaIndex] = OBJECT_FLOOR_OD;
                }
            }
        }
    }

    void applyIndoorBombableWallStatus() {
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                int objectId = dependencies.state().roomObjectsArea[areaIndex];
                if ((objectId == 0x3F || objectId == 0x47) && (dependencies.state().roomStatusFlags & 0x04) != 0) {
                    dependencies.state().roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x40 || objectId == 0x48)
                        && (dependencies.state().roomStatusFlags & 0x08) != 0) {
                    dependencies.state().roomObjectsArea[areaIndex] = 0x3D;
                } else if ((objectId == 0x41 || objectId == 0x49)
                        && (dependencies.state().roomStatusFlags & 0x02) != 0) {
                    dependencies.state().roomObjectsArea[areaIndex] = 0x3E;
                } else if ((objectId == 0x42 || objectId == 0x4A)
                        && (dependencies.state().roomStatusFlags & 0x01) != 0) {
                    dependencies.state().roomObjectsArea[areaIndex] = 0x3E;
                }
            }
        }
    }
    /** Mirrors ConfigureRoomObjects' source-marker and collected-chest replacements. */
    void applyChestStatus() {
        boolean chestOpened = (dependencies.state().roomStatusFlags & ROOM_STATUS_CHEST_OPEN) != 0;
        for (int row = 0; row < RoomConstants.OBJECTS_PER_COLUMN; row++) {
            for (int column = 0; column < RoomConstants.OBJECTS_PER_ROW; column++) {
                int areaIndex = RoomConstants.ROOM_OBJECTS_BASE + row * RoomConstants.ROOM_OBJECT_ROW_STRIDE + column;
                if (!chestOpened && dependencies.state().roomObjectsArea[areaIndex] == OBJECT_CHEST_OPEN) {
                    dependencies.state().roomObjectsArea[areaIndex] = dependencies.state().floorObject;
                } else if (chestOpened && dependencies.state().roomObjectsArea[areaIndex] == OBJECT_CHEST_CLOSED) {
                    dependencies.state().roomObjectsArea[areaIndex] = OBJECT_CHEST_OPEN;
                }
            }
        }
    }

    record Dependencies(RoomObjectDecodeState state, java.util.function.Supplier<RoomObjectParser> owner) {}
}
