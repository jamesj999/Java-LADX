package linksawakening.world.room;

import java.util.Arrays;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.rom.Rom;
import linksawakening.rom.RomSymbols;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.Warp;

/** VBlank work requested by the ROM's adjacent-room handlers. */
final class RoomScrollTileTransfers {
    private final int[] loadedEntitySheets = new int[4];
    private int worldTileset = 0x0F;
    private ScrollController.Preparation preparation = new ScrollController.Preparation(0, 0, 0);

    ScrollController.Preparation preparation() {
        return preparation;
    }

    void select(
            Rom rom,
            OverworldTilesetTable overworldTilesets,
            LoadedRoom room,
            EntitySpriteSelection selection,
            boolean adjacent,
            boolean hasSwitchBlocks) {
        if (!adjacent) {
            // Full world loads clear the sheet selectors and reset hWorldTileset to KEEP.
            Arrays.fill(loadedEntitySheets, 0);
            worldTileset = 0x0F;
        }
        boolean colorDungeon = room.mapId() == 0xFF;
        int backgroundTransfers = selectBackground(rom, overworldTilesets, room, colorDungeon);
        int entityTransfers = selectEntitySheets(selection, colorDungeon);
        // Color Dungeon requests its BG copy during Prepare, before SelectRoomTilesets.
        int roomTransfers = colorDungeon ? 4 : 0;
        preparation = new ScrollController.Preparation(
                roomTransfers, backgroundTransfers + entityTransfers, hasSwitchBlocks ? 2 : 0);
    }

    private int selectBackground(
            Rom rom, OverworldTilesetTable overworldTilesets, LoadedRoom room, boolean colorDungeon) {
        if (room.mapCategory() == Warp.CATEGORY_OVERWORLD) {
            int tileset = overworldTilesets.tilesetIdForRoom(room.roomId());
            if (!OverworldTilesetTable.shouldLoadRoomSpecificTileset(room.roomId(), tileset, worldTileset)) return 0;
            worldTileset = tileset;
            return 8; // $00:0694 finishes the outdoor BG copy after eight VBlanks.
        }
        int mapId = room.mapId();
        boolean indoorsB = mapId >= 0x06 && mapId < 0x1A;
        var table = colorDungeon
                ? RomSymbols.ColorDungeonTilesetsTable
                : RomSymbols.IndoorsTilesetsTable.plus(indoorsB ? 0x100 : 0);
        int tileset = rom.u8(table.plus(room.roomId()));
        if (tileset == worldTileset) return 0;
        worldTileset = tileset;
        return tileset == 0xFF ? 0 : 4; // $00:064C finishes the indoor BG copy.
    }

    private int selectEntitySheets(EntitySpriteSelection selection, boolean colorDungeon) {
        if (colorDungeon) return 1; // $00:06A9 loads the Color Dungeon variant in one transfer.
        if (selection == null || !selection.hasStandardSheets()) return 0;

        int changed = 0;
        int[] sheetValues = selection.sheetValues();
        for (int slot = 0; slot < loadedEntitySheets.length; slot++) {
            int sheet = sheetValues[slot];
            if (sheet == 0xFF || sheet == loadedEntitySheets[slot]) continue;
            loadedEntitySheets[slot] = sheet;
            changed++;
        }
        // $00:0DFE schedules at most two sheets, each requiring four VBlanks.
        return Math.min(changed, 2) * 4;
    }
}
