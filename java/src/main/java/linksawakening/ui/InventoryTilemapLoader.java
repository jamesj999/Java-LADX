package linksawakening.ui;

import java.util.Arrays;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;
import linksawakening.rom.RomUtil;

/** Inventory tilemap loader behavior and state. */
public final class InventoryTilemapLoader {

    public static final int WIDTH = 20;
    public static final int HEIGHT = 18;

    private static final int INVENTORY_TILEMAP_BANK = RomSymbols.InventoryTilemap.bank();
    private static final int INVENTORY_TILEMAP_ADDR = RomSymbols.InventoryTilemap.address();
    private static final int INVENTORY_ATTRMAP_BANK = RomSymbols.InventoryAttrmap.bank();
    private static final int INVENTORY_ATTRMAP_ADDR = RomSymbols.InventoryAttrmap.address();
    private static final int INVENTORY_PALETTES_BANK = RomSymbols.InventoryPalettes.bank();
    private static final int INVENTORY_PALETTES_ADDR = RomSymbols.InventoryPalettes.address();
    private static final int INVENTORY_BG_PALETTE_COUNT = 8;
    private static final int INVENTORY_OBJECT_PALETTE_OFFSET = 0x40;
    private static final int INVENTORY_OBJECT_PALETTE_COUNT = 8;
    private static final int OCARINA_POPUP_BANK = RomSymbols.Data_020_604B.bank();
    private static final int OCARINA_POPUP_ADDR = RomSymbols.Data_020_604B.address();
    private static final int OCARINA_POPUP_BYTE_COUNT = 0xC0;

    private final int[] baseTilemap;
    private final int[] attrmap;
    private final int[][] palettes;
    private final int[][] objectPalettes;
    private final int[] ocarinaPopupOam;

    private InventoryTilemapLoader(
            int[] tilemap, int[] attrmap, int[][] palettes, int[][] objectPalettes, int[] ocarinaPopupOam) {
        this.baseTilemap = tilemap;
        this.attrmap = attrmap;
        this.palettes = palettes;
        this.objectPalettes = objectPalettes;
        this.ocarinaPopupOam = ocarinaPopupOam;
    }

    public static InventoryTilemapLoader loadFromRom(Rom romData) {
        int[] tilemap = RomUtil.decodeBackgroundFromRom(
                romData, new RomAddress(INVENTORY_TILEMAP_BANK, INVENTORY_TILEMAP_ADDR), WIDTH, 0x7F);
        int[] attrmap = RomUtil.decodeBackgroundFromRom(
                romData, new RomAddress(INVENTORY_ATTRMAP_BANK, INVENTORY_ATTRMAP_ADDR), WIDTH, 0x00);
        int[][] palettes = RomUtil.loadPalettesFromRom(
                romData, new RomAddress(INVENTORY_PALETTES_BANK, INVENTORY_PALETTES_ADDR), INVENTORY_BG_PALETTE_COUNT);
        int[][] objectPalettes = RomUtil.loadPalettesFromRom(
                romData,
                new RomAddress(INVENTORY_PALETTES_BANK, INVENTORY_PALETTES_ADDR + INVENTORY_OBJECT_PALETTE_OFFSET),
                INVENTORY_OBJECT_PALETTE_COUNT);
        int[] ocarinaPopupOam = new int[OCARINA_POPUP_BYTE_COUNT];
        int popupOffset = RomBank.romOffset(OCARINA_POPUP_BANK, OCARINA_POPUP_ADDR);
        if (popupOffset < 0 || popupOffset > romData.size() - ocarinaPopupOam.length) {
            throw new IllegalArgumentException("Ocarina popup data exceeds ROM bounds");
        }
        for (int index = 0; index < ocarinaPopupOam.length; index++) {
            ocarinaPopupOam[index] = romData.u8(RomAddress.fromOffset(popupOffset + index));
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

    /** @deprecated Use the Rom overload; retains legacy partial-buffer validation. */
    @Deprecated(forRemoval = true)
    public static InventoryTilemapLoader loadFromRom(byte[] romData) {
        return loadFromRom(romData == null ? null : new Rom(romData));
    }
    /** Replaces synchronous renderer scratch buffers, including zero padding. */
    void copyInto(int[] tiles, int[] attributes) {
        Arrays.fill(tiles, 0);
        Arrays.fill(attributes, 0);
        System.arraycopy(baseTilemap, 0, tiles, 0, Math.min(tiles.length, baseTilemap.length));
        System.arraycopy(attrmap, 0, attributes, 0, Math.min(attributes.length, attrmap.length));
    }
}
