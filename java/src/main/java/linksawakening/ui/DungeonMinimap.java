package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;
import linksawakening.rom.RomUtil;

import java.util.Arrays;

/** The dungeon portion of the inventory window, prepared once on opening. */
final class DungeonMinimap {
    private static final int LEFT = 9;
    private static final int TOP = 7;
    private static final int WIDTH = 11;
    private static final int HEIGHT = 10;
    private final int[] tiles = new int[WIDTH * HEIGHT];
    private final int[] attributes = new int[tiles.length];
    private final int markerCell;

    DungeonMinimap(byte[] rom, int map, boolean sideView, int position, byte[] statuses,
                   byte[] items, boolean towerCollapsed) {
        Arrays.fill(tiles, 0x7F);
        Arrays.fill(attributes, 1);
        int index = map == 0xFF ? 15 : map;
        int layout = read(rom, 1, 0x4385 + index);
        int shape = layout >> 4;
        int mapAddress = map == 0xFF ? 0x66B9
            : map == 6 && towerCollapsed ? 0x6679 : 0x6479 + map * 64;
        for (int i = 0; i < 64; i++) {
            int tile = roomTile(rom, read(rom, 2, mapAddress + i), statuses[i] & 255,
                items[0] != 0, items[1] != 0);
            int cell = cellForPosition(i, shape);
            tiles[cell] = tile;
            attributes[cell] = tile == 0xED ? 6 : 1;
        }
        markerCell = position >= 0 && position < 64 ? cellForPosition(position, shape) : -1;

        int labelRow = (shape & 2) != 0 ? 7 : 8;
        put(9, labelRow, 0xBA, 1);
        put(10, labelRow, (map + 0xB1) & 255, 1);
        if (shape != 0) {
            // CreateMinimapTilemap selects the floor labels from three parallel tables.
            int label = shape * 8 + (layout & 3) * (shape == 3 ? 11 : 4);
            for (; read(rom, 1, 0x5418 + label) != 0xFF; label++) {
                int address = read(rom, 1, 0x5418 + label) * 256 + read(rom, 1, 0x545C + label);
                if (address >= 0x9C00) {
                    put(address & 31, (address - 0x9C00) / 32, read(rom, 1, 0x54A0 + label), 1);
                }
            }
        }
        if (!sideView) {
            int ptr = RomUtil.romOffset(1, 0x6DCA + 2 * index);
            int address = (rom[ptr] & 255) | (rom[ptr + 1] & 255) << 8;
            put(address & 31, (address - 0x9C00) / 32, 0xA3, 1);
        }

    }

    private static int roomTile(byte[] rom, int tile, int status, boolean map, boolean compass) {
        if (tile == 0x7D) return tile;
        boolean symbol = tile == 0xED || tile == 0xEE;
        if (!symbol || !compass) {
            if (!map) return 0x7D;
            if (symbol) tile = 0xEF;
        }
        if ((status & 0x80) == 0) return tile;
        if (tile == 0xED && (status & 0x10) == 0 || tile == 0xEE && (status & 0x20) == 0) {
            return tile;
        }
        return map ? 0xD0 + read(rom, 2, 0x66F9 + (status & 15)) : 0x7D;
    }

    private static int cellForPosition(int position, int shape) {
        int x = position % 8;
        int y = position / 8;
        int splitX = (shape & 1) != 0 && x >= 4 ? 1 : 0;
        int splitY = (shape & 2) != 0 && y < 4 ? 1 : 0;
        return (8 + y - splitY - TOP) * WIDTH + 11 + x + splitX - LEFT;
    }

    private void put(int x, int y, int tile, int palette) {
        int cell = (y - TOP) * WIDTH + x - LEFT;
        tiles[cell] = tile;
        attributes[cell] = palette;
    }

    private static int read(byte[] rom, int bank, int address) {
        return Byte.toUnsignedInt(rom[RomUtil.romOffset(bank, address)]);
    }

    void render(byte[] buffer, GPU gpu, InventoryTilemapLoader loader, int windowY, int frame, boolean showMarker) {
        TilemapRenderer.render(buffer, gpu, tiles, attributes, loader.palettes(), WIDTH, HEIGHT,
            LEFT * 8, windowY + TOP * 8);
        if (showMarker && markerCell >= 0 && windowY == 0) {
            int x = (LEFT + markerCell % WIDTH) * 8;
            int y = (TOP + markerCell / WIDTH) * 8;
            IndexedRenderer.drawSpriteTile(buffer, gpu.getTile(0x3E), x, y, false, false,
                loader.objectPalettes()[(frame & 8) == 0 ? 1 : 4]);
        }
    }
}
