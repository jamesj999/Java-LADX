package linksawakening.world.room;

import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;

public final class OverworldTilesetTable {

    private static final int ROOM_OW_CAMERA_SHOP = 0x37;
    private static final int W_TILESET_KEEP = 0x0F;
    private static final int W_TILESET_CAMERA_SHOP = 0x1A;
    private static final int W_TILESET_NO_UPDATE = 0xFF;

    private final Rom rom;

    /** Uses the caller-owned typed ROM; no copy or bank-alignment check is introduced. */
    public OverworldTilesetTable(Rom romData) {
        this.rom = java.util.Objects.requireNonNull(romData, "data");
    }

    public int tilesetIdForRoom(int roomId) {
        int room = roomId & 0xFF;
        // The table is 8x8 over 2x2 room blocks: index = (roomY / 2) * 8 + (roomX / 2).
        // This mirrors SelectRoomTilesets at bank0.asm:$0D60-$0D6E.
        int index = ((room >> 2) & 0xF8) | ((room >> 1) & 0x07);
        return rom.u8(RomSymbols.OverworldTilesetsTable.plus(index));
    }

    public static boolean shouldLoadRoomSpecificTileset(int roomId, int tilesetId, int currentTilesetId) {
        if (tilesetId == W_TILESET_KEEP || tilesetId == W_TILESET_NO_UPDATE) {
            return false;
        }
        if (tilesetId == W_TILESET_CAMERA_SHOP && roomId != ROOM_OW_CAMERA_SHOP) {
            return false;
        }
        return currentTilesetId != tilesetId;
    }
}
