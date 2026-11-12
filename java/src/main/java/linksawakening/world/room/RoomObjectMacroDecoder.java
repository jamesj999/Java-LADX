package linksawakening.world.room;

import static linksawakening.world.room.RoomObjectFormat.*;

import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;

/** ConfigureRoomObjects door expansion and TreeMacroHandler ($24:759D) padded-buffer writes. */
final class RoomObjectMacroDecoder {
    // Door table groups in ConfigureRoomObjects: closed, shutters, open, then special entrances.
    private static final int FIRST_SHUTTER_MACRO = 4;
    private static final int FIRST_OPEN_DOOR_MACRO = 8;
    private static final int BOSS_DOOR_MACRO = 12;
    private static final int MACRO_END = 0xFF;
    // Top, bottom, left, right use ROOM_STATUS_DOOR_OPEN_* rather than direction bit order.
    private static final int[] DOOR_STATUS_BITS = {0x04, 0x08, 0x02, 0x01};

    private final Dependencies dependencies;

    RoomObjectMacroDecoder(Dependencies dependencies) {
        this.dependencies = dependencies;
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
            if ((dependencies.state().roomStatusFlags & statusBit) != 0) {
                doorMacroIndex += FIRST_OPEN_DOOR_MACRO;
            }
        } else if (doorMacroIndex == BOSS_DOOR_MACRO && (dependencies.state().roomStatusFlags & 0x04) != 0) {
            // OBJECT_BOSS_DOOR is top-facing and reloads as OpenDoorTop once
            // ROOM_STATUS_DOOR_OPEN_UP has been synchronized.
            doorMacroIndex = FIRST_OPEN_DOOR_MACRO;
        }
        if (doorMacroIndex >= FIRST_SHUTTER_MACRO && doorMacroIndex < FIRST_OPEN_DOOR_MACRO) {
            // wC18A/wC18B use top, bottom, left, right in bits 0..3.
            dependencies.state().shutterDoorMask |= 1 << (doorMacroIndex - FIRST_SHUTTER_MACRO);
        }
        // Closed shutters also call the open-door loaders ($00:365B..371E).
        // Those loaders persist the directional minimap bits through $00:36C4.
        if (doorMacroIndex >= FIRST_SHUTTER_MACRO && doorMacroIndex < BOSS_DOOR_MACRO) {
            dependencies.state().roomStatusFlags |= DOOR_STATUS_BITS[doorMacroIndex & 3];
        }
        if (doorMacroIndex == 16) dependencies.state().roomStatusFlags |= 8; // Dungeon entrance, $00:37A2.
        if (doorMacroIndex == 17) dependencies.state().roomStatusFlags |= 1; // Indoor entrance, $00:37CF.
        int[] table = INDOOR_DOOR_MACRO_TABLES[doorMacroIndex];
        dependencies
                .owner()
                .get()
                .applyMacroTable(location, INDOOR_DOOR_MACRO_BANK, table[0], INDOOR_DOOR_MACRO_BANK, table[1]);
    }

    void applyMacroTable(int baseLocation, int offsetsBank, int offsetsAddr, int idsBank, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + baseLocation;
        int offsetsOffset = RomBank.romOffset(offsetsBank, offsetsAddr);
        int idsOffset = RomBank.romOffset(idsBank, idsAddr);

        while (offsetsOffset < dependencies.romData().size()
                && idsOffset < dependencies.romData().size()) {
            int offset = dependencies.owner().get().readRomByte(offsetsOffset++);
            if (offset == MACRO_END) {
                break;
            }
            int objectId = dependencies.owner().get().readRomByte(idsOffset++);
            int targetAreaIndex = baseAreaIndex + offset;
            if (dependencies.owner().get().isAreaIndexValid(targetAreaIndex)) {
                dependencies.state().roomObjectsArea[targetAreaIndex] = objectId;
                if (dependencies.owner().get().isMacroExpansionDoorId(objectId)) {
                    dependencies.state().doorPositions.add(targetAreaIndex - RoomConstants.ROOM_OBJECTS_BASE);
                }
            }
        }
    }

    void expandMacro(int macroType, int location, int objectData) {
        int macroIndex = macroType - OBJECT_MACRO_BASE;
        switch (macroIndex) {
            case 0 -> dependencies.owner().get().handleTreeMacro(location, objectData);
            case 1 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(
                            location, TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR, TWO_DOORS_HOUSE_OBJECT_IDS_ADDR);
            case 2 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(location, LARGE_HOUSE_OBJECT_OFFSETS_ADDR, LARGE_HOUSE_OBJECT_IDS_ADDR);
            case 3 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(location, CATFISH_MAW_OBJECT_OFFSETS_ADDR, CATFISH_MAW_OBJECT_IDS_ADDR);
            case 4 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(location, PALACE_DOOR_OBJECT_OFFSETS_ADDR, PALACE_DOOR_OBJECT_IDS_ADDR);
            case 5 -> {
                int pigIdsAddr = (dependencies.state().roomStatusFlags & ROOM_STATUS_DOOR_OPEN_UP) == 0
                        ? STONE_PIG_HEAD_OBJECT_IDS_INTACT_ADDR
                        : STONE_PIG_HEAD_OBJECT_IDS_BLASTED_ADDR;
                dependencies
                        .owner()
                        .get()
                        .copyOutdoorsMacroObjects(location, STONE_PIG_HEAD_OBJECT_OFFSETS_ADDR, pigIdsAddr);
            }
            case 6 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(location, PALM_TREE_OBJECT_OFFSETS_ADDR, PALM_TREE_OBJECT_IDS_ADDR);
            case 7 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(location, WALLED_PIT_OBJECT_OFFSETS_ADDR, WALLED_PIT_OBJECT_IDS_ADDR);
            case 8 -> dependencies
                    .owner()
                    .get()
                    .copyOutdoorsMacroObjects(location, SMALL_HOUSE_OBJECT_OFFSETS_ADDR, SMALL_HOUSE_OBJECT_IDS_ADDR);
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
            dependencies.owner().get().copyTreeObjectsToRoom(areaIndex);
            return;
        }
        int count = objectData & 0x0F;
        if (count == 0) {
            return;
        }
        int step = (objectData & 0x40) != 0 ? RoomConstants.ROOM_OBJECT_ROW_STRIDE * 2 : 2;
        for (int i = 0; i < count; i++) {
            dependencies.owner().get().copyTreeObjectsToRoom((areaIndex + i * step) & 0xFF);
        }
    }

    void copyTreeObjectsToRoom(int areaIndex) {
        if (!dependencies.owner().get().isAreaIndexValid(areaIndex)
                || !dependencies.owner().get().isAreaIndexValid(areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE + 1)) {
            return;
        }
        int topLeftOffset = areaIndex;
        int topRightOffset = areaIndex + 1;
        int bottomLeftOffset = areaIndex + RoomConstants.ROOM_OBJECT_ROW_STRIDE;
        int bottomRightOffset = bottomLeftOffset + 1;

        int existingTopLeft = dependencies.state().roomObjectsArea[topLeftOffset];
        dependencies.state().roomObjectsArea[topLeftOffset] =
                existingTopLeft < 0x10 ? OBJECT_TREE_TOP_LEFT : OBJECT_TREE_OVERLAP_LEFT;

        int existingTopRight = dependencies.state().roomObjectsArea[topRightOffset];
        dependencies.state().roomObjectsArea[topRightOffset] =
                existingTopRight < 0x10 ? OBJECT_TREE_TOP_RIGHT : OBJECT_TREE_OVERLAP_RIGHT;

        int existingBottomLeft = dependencies.state().roomObjectsArea[bottomLeftOffset];
        if (existingBottomLeft >= 0x8A) {
            dependencies.state().roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_BUSHES_BOTTOM_LEFT;
        } else if (existingBottomLeft < 0x10) {
            dependencies.state().roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_BOTTOM_LEFT;
        } else {
            dependencies.state().roomObjectsArea[bottomLeftOffset] = OBJECT_TREE_OVERLAP_RIGHT;
        }

        int existingBottomRight = dependencies.state().roomObjectsArea[bottomRightOffset];
        if (existingBottomRight >= 0x8A) {
            dependencies.state().roomObjectsArea[bottomRightOffset] = OBJECT_TREE_BUSHES_BOTTOM_RIGHT;
        } else if (existingBottomRight < 0x10) {
            dependencies.state().roomObjectsArea[bottomRightOffset] = OBJECT_TREE_BOTTOM_RIGHT;
        } else {
            dependencies.state().roomObjectsArea[bottomRightOffset] = OBJECT_TREE_OVERLAP_LEFT;
        }
    }

    void copyOutdoorsMacroObjects(int location, int offsetsAddr, int idsAddr) {
        int baseAreaIndex = RoomConstants.ROOM_OBJECTS_BASE + location;
        int offsetsOffset = RomBank.romOffset(OVERWORLD_MACROS_BANK, offsetsAddr);
        int idsOffset = RomBank.romOffset(OVERWORLD_MACROS_BANK, idsAddr);

        while (offsetsOffset < dependencies.romData().size()
                && idsOffset < dependencies.romData().size()) {
            int offset = dependencies.owner().get().readRomByte(offsetsOffset++);
            if (offset == MACRO_END) {
                break;
            }
            int objectId = dependencies.owner().get().readRomByte(idsOffset++);
            int targetAreaIndex = baseAreaIndex + offset;
            if (dependencies.owner().get().isAreaIndexValid(targetAreaIndex)) {
                dependencies.state().roomObjectsArea[targetAreaIndex] = objectId;
                if (dependencies.owner().get().isMacroExpansionDoorId(objectId)) {
                    dependencies.state().doorPositions.add(targetAreaIndex - RoomConstants.ROOM_OBJECTS_BASE);
                }
            }
        }
    }

    record Dependencies(
            Rom romData, RoomObjectDecodeState state, java.util.function.Supplier<RoomObjectParser> owner) {}
}
