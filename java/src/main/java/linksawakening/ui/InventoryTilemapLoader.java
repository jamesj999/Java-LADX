package linksawakening.ui;

import linksawakening.rom.RomUtil;

public final class InventoryTilemapLoader {

    public static final int WIDTH = 20;
    public static final int HEIGHT = 18;

    private static final int INVENTORY_TILEMAP_BANK = 0x08;
    private static final int INVENTORY_TILEMAP_ADDR = 0x6748;
    private static final int INVENTORY_ATTRMAP_BANK = 0x24;
    private static final int INVENTORY_ATTRMAP_ADDR = 0x6354;
    private static final int INVENTORY_PALETTES_BANK = 0x20;
    private static final int INVENTORY_PALETTES_ADDR = 0x5D61;
    private static final int INVENTORY_BG_PALETTE_COUNT = 8;

    private final int[] baseTilemap;
    private final int[] attrmap;
    private final int[][] palettes;

    private InventoryTilemapLoader(int[] tilemap, int[] attrmap, int[][] palettes) {
        this.baseTilemap = tilemap;
        this.attrmap = attrmap;
        this.palettes = palettes;
    }

    public static InventoryTilemapLoader loadFromRom(byte[] romData) {
        int[] tilemap = RomUtil.decodeBackgroundFromRom(
            romData, INVENTORY_TILEMAP_BANK, INVENTORY_TILEMAP_ADDR, WIDTH, 0x7F);
        int[] attrmap = RomUtil.decodeBackgroundFromRom(
            romData, INVENTORY_ATTRMAP_BANK, INVENTORY_ATTRMAP_ADDR, WIDTH, 0x00);
        int[][] palettes = RomUtil.loadPalettesFromRom(
            romData, INVENTORY_PALETTES_BANK, INVENTORY_PALETTES_ADDR, INVENTORY_BG_PALETTE_COUNT);

        if (tilemap.length < WIDTH * HEIGHT) {
            tilemap = padTo(tilemap, WIDTH * HEIGHT, 0x7F);
        }
        if (attrmap.length < WIDTH * HEIGHT) {
            attrmap = padTo(attrmap, WIDTH * HEIGHT, 0x00);
        }
        return new InventoryTilemapLoader(tilemap, attrmap, palettes);
    }

    public int[] copyBaseTilemap() {
        int[] copy = new int[WIDTH * HEIGHT];
        System.arraycopy(baseTilemap, 0, copy, 0, Math.min(copy.length, baseTilemap.length));
        return copy;
    }

    public int[] copyAttrmap() {
        int[] copy = new int[WIDTH * HEIGHT];
        System.arraycopy(attrmap, 0, copy, 0, Math.min(copy.length, attrmap.length));
        return copy;
    }

    public int[] attrmap() {
        return attrmap;
    }

    public int[][] palettes() {
        return palettes;
    }

    private static int[] padTo(int[] source, int size, int filler) {
        int[] padded = new int[size];
        for (int i = 0; i < size; i++) {
            padded[i] = i < source.length ? source[i] : filler;
        }
        return padded;
    }
}
