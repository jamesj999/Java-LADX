package linksawakening.ui;

import linksawakening.rom.RomUtil;
import linksawakening.state.PlayerState;
import java.util.Arrays;

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
    private static final int INVENTORY_OBJECT_PALETTE_OFFSET = 0x40;
    private static final int INVENTORY_OBJECT_PALETTE_COUNT = 8;
    private static final int OCARINA_POPUP_BANK = 0x20;
    private static final int OCARINA_POPUP_ADDR = 0x604B;
    private static final int OCARINA_POPUP_BYTE_COUNT = 0xC0;

    private final int[] baseTilemap;
    private final int[] attrmap;
    private final int[][] palettes;
    private final int[][] objectPalettes;
    private final int[] ocarinaPopupOam;
    private final byte[] itemTiles;
    private final byte[] itemAttributes;

    private InventoryTilemapLoader(int[] tilemap, int[] attrmap, int[][] palettes,
                                   int[][] objectPalettes, int[] ocarinaPopupOam, byte[] romData) {
        this.baseTilemap = tilemap;
        this.attrmap = attrmap;
        this.palettes = palettes;
        this.objectPalettes = objectPalettes;
        this.ocarinaPopupOam = ocarinaPopupOam;
        int tiles = RomUtil.romOffset(0x20, 0x5C30);
        int attributes = RomUtil.romOffset(0x20, 0x5C14);
        itemTiles = Arrays.copyOfRange(romData, tiles, tiles + 14 * 6);
        itemAttributes = Arrays.copyOfRange(romData, attributes, attributes + 14 * 2);
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
        return new InventoryTilemapLoader(tilemap, attrmap, palettes, objectPalettes, ocarinaPopupOam, romData);
    }

    /** DrawInventorySlots and AdjustInventoryTilesForLevelsAndCounts share the same layout for all twelve slots. */
    void drawItem(int[] tilemap, int[] attrmap, int col, int row, int item, PlayerState player) {
        if (item < 0 || item >= itemTiles.length / 6) item = PlayerState.INVENTORY_EMPTY;
        int top = row * WIDTH + col;
        int bottom = top + WIDTH;
        for (int x = 0; x < 3; x++) {
            tilemap[top + x] = Byte.toUnsignedInt(itemTiles[item * 6 + x]);
            tilemap[bottom + x] = Byte.toUnsignedInt(itemTiles[item * 6 + 3 + x]);
        }
        attrmap[top] = itemAttributes[item * 2];
        attrmap[bottom] = itemAttributes[item * 2 + 1];
        switch (item) {
            case PlayerState.INVENTORY_POWER_BRACELET -> {
                tilemap[bottom + 2] = 0xB0 + player.powerBraceletLevel();
                if (player.powerBraceletLevel() == 2) {
                    attrmap[top] = 2;
                    attrmap[bottom] = 2;
                }
            }
            case PlayerState.INVENTORY_SWORD -> tilemap[bottom + 2] = 0xB0 + player.swordLevel();
            case PlayerState.INVENTORY_SHIELD -> tilemap[bottom + 2] = 0xB0 + player.shieldLevel();
            case PlayerState.INVENTORY_BOMBS -> writeCount(tilemap, bottom, player.bombCount());
            case PlayerState.INVENTORY_BOW -> writeCount(tilemap, bottom, player.arrowCount());
            case PlayerState.INVENTORY_MAGIC_POWDER -> {
                if (player.hasToadstool()) {
                    attrmap[top] = 2;
                    attrmap[bottom] = 3;
                } else {
                    writeCount(tilemap, bottom, player.magicPowderCount());
                }
            }
            case PlayerState.INVENTORY_OCARINA -> {
                if (player.ocarinaSongFlags() != 0) {
                    tilemap[bottom + 1] = 0xB1 + player.selectedSongIndex();
                }
            }
        }
    }

    private static void writeCount(int[] tilemap, int bottom, int count) {
        tilemap[bottom + 1] = 0xB0 + count / 10;
        tilemap[bottom + 2] = 0xB0 + count % 10;
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
