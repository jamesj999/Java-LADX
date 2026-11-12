package linksawakening.world.room;

import linksawakening.rom.RomSymbols;

final class RoomObjectFormat {
    static final int ROOM_TEMPLATE_BANK = RomSymbols.RoomTemplateTopRightBottomLeftOffsets.bank();
    static final int[][] ROOM_TEMPLATE_TABLES = {
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
    static final int INDOOR_DOOR_MACRO_BANK = RomSymbols.HorizontalObjectOffsets.bank();
    static final int HORIZONTAL_OFFSETS_ADDR = RomSymbols.HorizontalObjectOffsets.address();
    static final int VERTICAL_OFFSETS_ADDR = RomSymbols.VerticalObjectOffsets.address();
    static final int DUNGEON_ENTRANCE_OFFSETS_ADDR = RomSymbols.DungeonEntranceObjectOffsets.address();
    static final int[][] INDOOR_DOOR_MACRO_TABLES = {
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
    static final int INDOOR_DOOR_MACRO_BASE = 0xEC;
    static final int INDOOR_DOOR_MACRO_LAST = 0xFD;
    static final int OBJECT_CLOSED_GATE = 0xC2;
    static final int OBJECT_ROCKY_CAVE_DOOR = 0xE1;
    static final int OBJECT_HOUSE_DOOR_VARIANT = 0xE2;
    static final int OBJECT_CAVE_DOOR = 0xE3;
    static final int OBJECT_BOMBABLE_CAVE_DOOR = 0xBA;
    static final int OBJECT_BOMBABLE_BLOCK = 0xA9;
    static final int OBJECT_KEYHOLE_BLOCK = 0xDE;
    static final int OBJECT_FLOOR_OD = 0x0D;
    static final int OBJECT_STAIRS_DOWN = 0xBE;
    static final int OBJECT_HIDDEN_STAIRS_DOWN = 0xBF;
    static final int OBJECT_STAIRS_UP = 0xCB;
    static final int OBJECT_GROUND_STAIRS = 0xC6;
    static final int OBJECT_DOOR_CB = 0xCB;
    static final int OBJECT_DOOR_61 = 0x61;
    static final int OBJECT_DOOR_C5 = 0xC5;
    static final int OVERWORLD_MACROS_BANK = RomSymbols.CatfishMawObjectOffsets.bank();
    static final int CATFISH_MAW_OBJECT_OFFSETS_ADDR = RomSymbols.CatfishMawObjectOffsets.address();
    static final int CATFISH_MAW_OBJECT_IDS_ADDR = RomSymbols.CatfishMawObjectIds.address();
    static final int TWO_DOORS_HOUSE_OBJECT_OFFSETS_ADDR = RomSymbols.TwoDoorsHouseObjectOffsets.address();
    static final int TWO_DOORS_HOUSE_OBJECT_IDS_ADDR = RomSymbols.TwoDoorsHouseObjectIds.address();
    static final int LARGE_HOUSE_OBJECT_OFFSETS_ADDR = RomSymbols.LargeHouseObjectOffsets.address();
    static final int LARGE_HOUSE_OBJECT_IDS_ADDR = RomSymbols.LargeHouseObjectIds.address();
    static final int PALACE_DOOR_OBJECT_OFFSETS_ADDR = RomSymbols.PalaceDoorObjectOffsets.address();
    static final int PALACE_DOOR_OBJECT_IDS_ADDR = RomSymbols.PalaceDoorObjectIds.address();
    static final int STONE_PIG_HEAD_OBJECT_OFFSETS_ADDR = RomSymbols.StonePigHeadObjectOffsets.address();
    static final int STONE_PIG_HEAD_OBJECT_IDS_INTACT_ADDR = RomSymbols.StonePigHeadObjectIds_Intact.address();
    static final int STONE_PIG_HEAD_OBJECT_IDS_BLASTED_ADDR = RomSymbols.StonePigHeadObjectIds_Blasted.address();
    static final int PALM_TREE_OBJECT_OFFSETS_ADDR = RomSymbols.PalmTreeObjectOffsets.address();
    static final int PALM_TREE_OBJECT_IDS_ADDR = RomSymbols.PalmTreeObjectIds.address();
    static final int WALLED_PIT_OBJECT_OFFSETS_ADDR = RomSymbols.WalledPitObjectOffsets.address();
    static final int WALLED_PIT_OBJECT_IDS_ADDR = RomSymbols.WalledPitObjectIds.address();
    static final int SMALL_HOUSE_OBJECT_OFFSETS_ADDR = RomSymbols.SmallHouseObjectOffsets.address();
    static final int SMALL_HOUSE_OBJECT_IDS_ADDR = RomSymbols.SmallHouseObjectIds.address();
    static final int ROOM_STATUS_DOOR_OPEN_UP = 0x04;
    static final int ROOM_STATUS_EVENT_1 = 0x10;
    static final int ROOM_STATUS_CHEST_OPEN = 0x10;
    static final int ROOM_STATUS_EVENT_3 = 0x40;
    static final int OW_ROOM_STATUS_OPENED = 0x04;
    static final int OBJECT_CHEST_CLOSED = 0xA0;
    static final int OBJECT_CHEST_OPEN = 0xA1;
    static final int OBJECT_TREE_TOP_LEFT = 0x25;
    static final int OBJECT_TREE_TOP_RIGHT = 0x26;
    static final int OBJECT_TREE_BOTTOM_LEFT = 0x27;
    static final int OBJECT_TREE_BOTTOM_RIGHT = 0x28;
    static final int OBJECT_TREE_OVERLAP_LEFT = 0x29;
    static final int OBJECT_TREE_OVERLAP_RIGHT = 0x2A;
    static final int OBJECT_TREE_BUSHES_BOTTOM_LEFT = 0x82;
    static final int OBJECT_TREE_BUSHES_BOTTOM_RIGHT = 0x83;
    static final int OBJECT_MACRO_BASE = 0xF5;
}
