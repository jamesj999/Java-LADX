package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;
import linksawakening.rom.RomSymbols;

/** LoadInstrumentsBG ($01:6BEA) and the palette cycle at $20:5EB5. */
final class InventoryInstrumentPanel {
    private final TilemapRenderer.Region tileRegion = new TilemapRenderer.Region(0, 0, 0, 0);

    private final int[] attributeScratch = new int[4];
    private final int[][][] instrumentTiles = new int[8][2][];
    private int[][] paletteScratch;
    private final int[] cyclingPalette = new int[4];
    private final int[] positions = new int[8];
    private final int[][] cycleColors = new int[16][2];
    private final GPU tiles = new GPU();

    InventoryInstrumentPanel(Rom romData) {
        var data = romData;
        int[] numbers = new int[8];
        int[] instruments = new int[8];
        for (int i = 0; i < 8; i++) {
            positions[i] =
                    0x100 + data.u8(linksawakening.rom.RomAddress.fromOffset(RomSymbols.Data_001_6BCF.offset() + i));
            numbers[i] = data.u8(linksawakening.rom.RomAddress.fromOffset(RomSymbols.Data_001_6BD7.offset() + i));
            instruments[i] = data.u8(linksawakening.rom.RomAddress.fromOffset(RomSymbols.Data_001_6BDF.offset() + i));
        }
        for (int i = 0; i < 8; i++) {
            int tile = instruments[i];
            instrumentTiles[i][0] = new int[] {0x7C, 0x7C, 0x7C, numbers[i]};
            instrumentTiles[i][1] = new int[] {tile, tile + 1, tile + 0x10, tile + 0x11};
        }
        for (int i = 0; i < 32; i++) {
            int offset = RomSymbols.InventoryInstrumentCyclingColors.offset() + i * 2;
            cycleColors[i / 2][i % 2] = RomBank.decodeRgb555(data.u8(linksawakening.rom.RomAddress.fromOffset(offset))
                    | data.u8(linksawakening.rom.RomAddress.fromOffset(offset + 1)) << 8);
        }
        // Keep the inventory's ROM-loaded tiles separate from gameplay VRAM:
        // the menu must not overwrite the shared effects beneath its window.
        tiles.loadBaseTiles(romData);
        // CopySirenInstrumentTiles uses bank $0C without the CGB adjustment.
        int npcSource = RomSymbols.ColorDungeonNpcTiles.offset();
        for (int i = 0; i < 0x400; i++)
            tiles.writeVRAM(0x400 + i, (byte) data.u8(linksawakening.rom.RomAddress.fromOffset(npcSource + i)));
        int source = RomSymbols.SirenInstrumentsTiles.offset();
        for (int i = 0; i < 0x200; i++)
            tiles.writeVRAM(0xD00 + i, (byte) data.u8(linksawakening.rom.RomAddress.fromOffset(source + i)));
    }

    int[] tilemapForInstrument(int index, int flags) {
        return instrumentTileView(index, flags).clone();
    }

    private int[] instrumentTileView(int index, int flags) {
        return instrumentTiles[index][(flags & 2) == 0 ? 0 : 1];
    }

    /** Borrowed palette rows, consumed synchronously by render/submit before the next call. */
    int[][] framePalettes(int[][] base, int frame) {
        if (paletteScratch == null || paletteScratch.length != base.length) paletteScratch = new int[base.length][];
        System.arraycopy(base, 0, paletteScratch, 0, base.length);
        System.arraycopy(base[7], 0, cyclingPalette, 0, 4);
        int phase = (frame >>> 3) & 15;
        cyclingPalette[1] = cycleColors[phase][0];
        cyclingPalette[2] = cycleColors[phase][1];
        paletteScratch[7] = cyclingPalette;
        return paletteScratch;
    }

    int position(int index) {
        return positions[index];
    }

    int[][] palettes(int[][] base, int frame) {
        int[][] result = base.clone();
        result[7] = base[7].clone();
        int phase = (frame >>> 3) & 15;
        result[7][1] = cycleColors[phase][0];
        result[7][2] = cycleColors[phase][1];
        return result;
    }

    /** func_020_62DE: the blinking SELECT prompt between two arrows. */
    void renderNavigation(byte[] buffer, int[][] objectPalettes, int frame) {
        int y = 0x90 - 16;
        int x = 0x53 - 8;
        IndexedRenderer.drawSpriteTile8x16(buffer, tiles, 0x4A, x, y, true, false, objectPalettes[6]);
        if (((frame >>> 4) & 3) != 0) {
            for (int i = 0; i < 8; i++) {
                IndexedRenderer.drawSpriteTile8x16(
                        buffer, tiles, 0x4C + i * 2, x + (i + 1) * 8, y, false, false, objectPalettes[6]);
            }
        }
        IndexedRenderer.drawSpriteTile8x16(buffer, tiles, 0x4A, 0x9B - 8, y, true, false, objectPalettes[6]);
    }

    void render(byte[] buffer, int[] attributes, int[][] palettes, byte[] flags, int windowY) {
        for (int i = 0; i < 8; i++) {
            int row = positions[i] / 32;
            int col = positions[i] % 32;
            int at = row * 20 + col;
            int[] attrs = attributeScratch;
            attrs[0] = attributes[at];
            attrs[1] = attributes[at + 1];
            attrs[2] = attributes[at + 20];
            attrs[3] = attributes[at + 21];
            TilemapRenderer.render(
                    buffer,
                    tiles,
                    instrumentTileView(i, flags[i]),
                    attrs,
                    palettes,
                    tileRegion.set(2, 2, col * 8, row * 8 + windowY));
        }
    }

    void submit(
            linksawakening.render.HardwareFrame frameModel,
            int[] attributes,
            int[][] palettes,
            byte[] flags,
            int windowY) {
        for (int i = 0; i < 8; i++) {
            int row = positions[i] / 32;
            int col = positions[i] % 32;
            int at = row * 20 + col;
            int[] attrs = attributeScratch;
            attrs[0] = attributes[at];
            attrs[1] = attributes[at + 1];
            attrs[2] = attributes[at + 20];
            attrs[3] = attributes[at + 21];
            TilemapRenderer.submit(
                    frameModel,
                    tiles,
                    instrumentTileView(i, flags[i]),
                    attrs,
                    palettes,
                    tileRegion.set(2, 2, col * 8, row * 8 + windowY));
        }
    }

    void submitNavigation(linksawakening.render.HardwareFrame frameModel, int[][] objectPalettes, int frame) {
        int y = 0x90 - 16;
        int x = 0x53 - 8;
        frameModel.sprite8x16(tiles, 0x4A, x, y, true, false, objectPalettes[6]);
        if (((frame >>> 4) & 3) != 0) {
            for (int i = 0; i < 8; i++) {
                frameModel.sprite8x16(tiles, 0x4C + i * 2, x + (i + 1) * 8, y, false, false, objectPalettes[6]);
            }
        }
        frameModel.sprite8x16(tiles, 0x4A, 0x9B - 8, y, true, false, objectPalettes[6]);
    }
}
