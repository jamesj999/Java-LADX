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
            if (!cleared && loadedSlot < MAX_ENTITIES) {
                int x = (location & 0x0F) * 0x10 + 0x08;
                int y = (location & 0xF0) + 0x10;
                EntitySpriteDefinition spriteDefinition = spriteHandlers
                    .forEntityType(type, table);
                slots.set(loadedSlot, new RoomEntity(
                    loadedSlot, sourceLoadOrder, type, x, y, EntityStatus.INIT,
                    spriteDefinition,
                    spriteDefinition.supported() ? spriteDefinition.initialVariant() : -1));
                loadedSlot++;
            }
            sourceLoadOrder++;
        }

        return new RoomEntitySnapshot(slots);
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
