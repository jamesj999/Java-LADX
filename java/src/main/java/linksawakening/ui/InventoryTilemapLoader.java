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
    private static final int INVENTORY_OBJECT_PALETTE_OFFSET = 0x80;
    private static final int INVENTORY_OBJECT_PALETTE_COUNT = 8;
    private static final int OCARINA_POPUP_BANK = 0x20;
    private static final int OCARINA_POPUP_ADDR = 0x604B;
    private static final int OCARINA_POPUP_BYTE_COUNT = 0xC0;

    private final int[] baseTilemap;
    private final int[] attrmap;
    private final int[][] palettes;
    private final int[][] objectPalettes;
    private final int[] ocarinaPopupOam;

    private InventoryTilemapLoader(int[] tilemap, int[] attrmap, int[][] palettes,
                                   int[][] objectPalettes, int[] ocarinaPopupOam) {
        this.baseTilemap = tilemap;
        this.attrmap = attrmap;
        this.palettes = palettes;
        this.objectPalettes = objectPalettes;
        this.ocarinaPopupOam = ocarinaPopupOam;
    }

    public static InventoryTilemapLoader loadFromRom(byte[] romData) {
        int[] tilemap = RomUtil.decodeBackgroundFromRom(
            romData, INVENTORY_TILEMAP_BANK, INVENTORY_TILEMAP_ADDR, WIDTH, 0x7F);
        int[] attrmap = RomUtil.decodeBackgroundFromRom(
            romData, INVENTORY_ATTRMAP_BANK, INVENTORY_ATTRMAP_ADDR, WIDTH, 0x00);
        int[][] palettes = RomUtil.loadPalettesFromRom(
            romData, INVENTORY_PALETTES_BANK, INVENTORY_PALETTES_ADDR, INVENTORY_BG_PALETTE_COUNT);
        int[][] objectPalettes = RomUtil.loadPalettesFromRom(
            romData, INVENTORY_PALETTES_BANK,
            INVENTORY_PALETTES_ADDR + INVENTORY_OBJECT_PALETTE_OFFSET,
            INVENTORY_OBJECT_PALETTE_COUNT);
        int[] ocarinaPopupOam = new int[OCARINA_POPUP_BYTE_COUNT];
        int popupOffset = RomUtil.romOffset(OCARINA_POPUP_BANK, OCARINA_POPUP_ADDR);
        if (popupOffset < 0 || popupOffset > romData.length - ocarinaPopupOam.length) {
            throw new IllegalArgumentException("Ocarina popup data exceeds ROM bounds");
        }
        for (int index = 0; index < ocarinaPopupOam.length; index++) {
            ocarinaPopupOam[index] = Byte.toUnsignedInt(romData[popupOffset + index]);
        }

        if (tilemap.length < WIDTH * HEIGHT) {
            tilemap = padTo(tilemap, WIDTH * HEIGHT, 0x7F);
        }
        if (attrmap.length < WIDTH * HEIGHT) {
            attrmap = padTo(attrmap, WIDTH * HEIGHT, 0x00);
        }
        return new InventoryTilemapLoader(tilemap, attrmap, palettes, objectPalettes, ocarinaPopupOam);
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

    public int[][] objectPalettes() {
        return objectPalettes;
    }

    public int[] ocarinaPopupOam() {
        return ocarinaPopupOam.clone();
    }

    private static int[] padTo(int[] source, int size, int filler) {
        int[] padded = new int[size];
        for (int i = 0; i < size; i++) {
            padded[i] = i < source.length ? source[i] : filler;
        }
        return padded;
    }
}
