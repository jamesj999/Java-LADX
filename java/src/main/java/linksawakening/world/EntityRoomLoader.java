package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.rom.RomBank;

import java.util.ArrayList;
import java.util.List;

/** ROM-backed decoder for the four room entity pointer tables in bank $16. */
public final class EntityRoomLoader {

    public static final int MAX_ENTITIES = 0x10;

    private static final int ENTITY_POINTER_BANK = 0x16;
    private static final int ENTITY_STREAM_END = 0xFF;
    private static final int MAX_ROOM_ID = 0xFF;
    private static final int ENTITY_GHINI = 0x12;
    private static final int ENTITY_MOBLIN_SWORD = 0x14;
    private static final int ENTITY_DROPPABLE_SECRET_SEASHELL = 0x3D;
    private static final int ENTITY_KEY_DROP_POINT = 0x30;
    private static final int GHINI_INITIAL_Z = 0x10;
    private static final int ROOM_INDOOR_A_ANGLERS_TUNNEL_KEY_DROP = 0x69;
    private static final int ROOM_INDOOR_A_ANGLERS_TUNNEL_KEY_FALL = 0x7C;
    private static final int ROOM_INDOOR_A_CATFISHS_MAW_MSTALFOS_4 = 0x80;
    private static final int ROOM_INDOOR_A_QUICKSAND_CAVE = 0xF8;
    private static final int ROOM_INDOOR_B_MOUNTAIN_CAVE_ROOM_1 = 0x7A;

    public enum RoomTable {
        OVERWORLD(0x4000),
        INDOORS_A(0x4200),
        INDOORS_B(0x4400),
        COLOR_DUNGEON(0x4600);

        private final int pointerAddress;

        RoomTable(int pointerAddress) {
            this.pointerAddress = pointerAddress;
        }

        public int pointerAddress() {
            return pointerAddress;
        }
    }

    private final byte[] romData;
    private final EntitySpriteHandlerCatalog spriteHandlers;

    public EntityRoomLoader(byte[] romData) {
        if (romData == null) {
            throw new IllegalArgumentException("ROM data cannot be null");
        }
        this.romData = romData;
        this.spriteHandlers = new EntitySpriteHandlerCatalog(romData);
    }

    public RoomEntitySnapshot load(RoomTable table, int roomId) {
        return load(table, roomId, 0);
    }

    public RoomEntitySnapshot load(RoomTable table, int roomId, int clearedMask) {
        return load(table, roomId, clearedMask, -1);
    }

    public RoomEntitySnapshot load(RoomTable table, int roomId, int clearedMask, int mapId) {
        return load(table, roomId, clearedMask, mapId, null);
    }

    /** Loads a room while applying the source EntityInit room-status gates. */
    public RoomEntitySnapshot load(RoomTable table, int roomId, int clearedMask, int mapId,
                                   byte[] roomStatusTable) {
        return load(table, roomId, clearedMask, mapId, roomStatusTable, false);
    }

    /** Loads a room with the patched inventory-state init gates enabled. */
    public RoomEntitySnapshot load(RoomTable table, int roomId, int clearedMask, int mapId,
                                   byte[] roomStatusTable, boolean hasBirdKey) {
        if (table == null) {
            throw new IllegalArgumentException("Room entity table cannot be null");
        }
        if (roomId < 0 || roomId > MAX_ROOM_ID) {
            throw new IllegalArgumentException("Room id out of range: " + roomId);
        }
        if ((clearedMask & ~0xFF) != 0) {
            throw new IllegalArgumentException("Cleared entity mask must be an unsigned byte: "
                + clearedMask);
        }

        int pointerAddress = table.pointerAddress() + roomId * 2;
        int pointerOffset = checkedRomOffset(ENTITY_POINTER_BANK, pointerAddress, 2,
            "entity pointer");
        int streamAddress = Byte.toUnsignedInt(romData[pointerOffset])
            | (Byte.toUnsignedInt(romData[pointerOffset + 1]) << 8);
        if (streamAddress < 0x4000 || streamAddress > 0x7FFF) {
            throw new IllegalArgumentException("Invalid entity stream address: 0x"
                + Integer.toHexString(streamAddress));
        }

        int streamOffset = checkedRomOffset(ENTITY_POINTER_BANK, streamAddress, 1,
            "entity stream");
        List<RoomEntity> slots = new ArrayList<>(MAX_ENTITIES);
        for (int slot = 0; slot < MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }

        int sourceLoadOrder = 0;
        int loadedSlot = 0;
        while (true) {
            requireRange(streamOffset, 1, "entity stream location");
            int location = Byte.toUnsignedInt(romData[streamOffset++]);
            if (location == ENTITY_STREAM_END) {
                break;
            }

            requireRange(streamOffset, 1, "entity stream type");
            int type = Byte.toUnsignedInt(romData[streamOffset++]);
            boolean cleared = sourceLoadOrder < 8
                && (clearedMask & (1 << sourceLoadOrder)) != 0;
            boolean unloadedByInitialization = type == ENTITY_KEY_DROP_POINT
                && shouldUnloadKeyAtInit(table, roomId, roomStatusTable, hasBirdKey);
            if (!cleared && !unloadedByInitialization && loadedSlot < MAX_ENTITIES) {
                int x = (location & 0x0F) * 0x10 + 0x08;
                int y = (location & 0xF0) + 0x10;
                int[] initializedPosition = applyInitialPositionTransform(table, roomId, type, x, y);
                EntitySpriteDefinition spriteDefinition = spriteHandlers
                    .forEntityType(type, table, mapId);
                int spriteVariant = initialSpriteVariant(table, roomId, type, spriteDefinition,
                    initializedPosition[0], initializedPosition[1]);
                int initialZ = type == ENTITY_GHINI
                    ? GHINI_INITIAL_Z
                    : spriteDefinition.supported() && FloatingItemMotion.isFloatingItem(type)
                        ? FloatingItemMotion.initialZ(type) : 0;
                slots.set(loadedSlot, new RoomEntity(
                    loadedSlot, sourceLoadOrder, type, initializedPosition[0], initializedPosition[1],
                    EntityStatus.INIT,
                    spriteDefinition,
                    spriteVariant, 0, initialSpriteTileOffset(type), initialZ));
                loadedSlot++;
            }
            sourceLoadOrder++;
        }

        return new RoomEntitySnapshot(slots);
    }

    private static boolean shouldUnloadKeyAtInit(RoomTable table, int roomId,
                                                  byte[] roomStatusTable, boolean hasBirdKey) {
        if (roomStatusTable == null) {
            return false;
        }
        int currentStatus = roomStatusAt(roomStatusTable, roomId);
        if (table == RoomTable.INDOORS_A && roomId == ROOM_INDOOR_A_QUICKSAND_CAVE) {
            return (currentStatus & 0x10) != 0 || (currentStatus & 0x20) == 0;
        }
        if (table == RoomTable.INDOORS_B
            && roomId == ROOM_INDOOR_B_MOUNTAIN_CAVE_ROOM_1) {
            return hasBirdKey || (currentStatus & 0x10) != 0;
        }
        if (table == RoomTable.INDOORS_A
            && roomId == ROOM_INDOOR_A_ANGLERS_TUNNEL_KEY_FALL) {
            return (roomStatusAt(roomStatusTable, ROOM_INDOOR_A_ANGLERS_TUNNEL_KEY_DROP) & 0x10) == 0
                || (currentStatus & 0x10) != 0;
        }
        return false;
    }

    private static int roomStatusAt(byte[] roomStatusTable, int roomId) {
        return roomId >= 0 && roomId < roomStatusTable.length
            ? Byte.toUnsignedInt(roomStatusTable[roomId]) : 0;
    }

    /**
     * Port of the position-changing entity init handlers in bank 3. These
     * writes happen after the room stream is decoded and before the first
     * active handler runs, so keeping them in the loader preserves both room
     * rendering and subsequent collision coordinates.
     */
    private static int[] applyInitialPositionTransform(RoomTable table, int roomId, int type,
                                                        int x, int y) {
        if (table == RoomTable.OVERWORLD && isOverworldTreeOrPotDrop(type)) {
            x += 8;
            y += 8;
        }
        if (type == 0x39) {
            x += 8;
        }
        if (type == 0x43 || type == 0x7C) {
            x += 8;
            y += 8;
        }
        if (type == 0xC2) {
            y -= 3;
        }
        if (table == RoomTable.OVERWORLD && type == 0x3D
            && (roomId == 0xA4 || roomId == 0xD2)) {
            x += 8;
            y += 8;
        }
        return new int[] {x, y};
    }

    private static boolean isOverworldTreeOrPotDrop(int type) {
        return type == 0x2E || type == 0x2F || type == 0x32 || type == 0x33
            || type == 0x34 || type == 0x36 || type == 0x37 || type == 0x38;
    }

    private static int initialSpriteVariant(RoomTable table, int roomId, int type,
                                            EntitySpriteDefinition definition, int x, int y) {
        // EntityInitSecretSeashell immediately suppresses the display-list
        // variant while privateState3 is non-zero.  Keep the room snapshot
        // hidden even before the first ACTIVE handler tick.
        if (type == ENTITY_DROPPABLE_SECRET_SEASHELL) {
            return -1;
        }
        if (!definition.supported()) {
            return -1;
        }
        if (FloatingItemMotion.isFloatingItem(type)) {
            return FloatingItemMotion.initialVariant(type, x, y);
        }
        if (type == ENTITY_MOBLIN_SWORD) {
            // EntityInitMoblinSword chooses direction $00 when the active
            // X position has bit $10 set, otherwise direction $03, then
            // selects the corresponding bank-$03 variant before flipping
            // the stored direction for its handler.
            return (x & 0x10) != 0 ? 6 : 0;
        }
        if (type == ENTITY_KEY_DROP_POINT) {
            if (table == RoomTable.INDOORS_A && roomId == ROOM_INDOOR_A_QUICKSAND_CAVE) {
                return 2;
            }
            if (table == RoomTable.INDOORS_B
                && roomId == ROOM_INDOOR_B_MOUNTAIN_CAVE_ROOM_1) {
                return 4;
            }
        }
        return definition.initialVariant();
    }

    /** OctorokEntityHandler writes $30 to hActiveEntityTilesOffset. */
    private static int initialSpriteTileOffset(int type) {
        return type == 0x09 ? 0x30 : 0;
    }

    private int checkedRomOffset(int bank, int address, int length, String description) {
        int offset;
        try {
            offset = RomBank.romOffset(bank, address);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid " + description + " address", exception);
        }
        requireRange(offset, length, description);
        return offset;
    }

    private void requireRange(int offset, int length, String description) {
        if (offset < 0 || length < 0 || offset > romData.length - length) {
            throw new IllegalArgumentException("Truncated " + description + " at ROM offset 0x"
                + Integer.toHexString(Math.max(offset, 0)));
        }
    }
}
