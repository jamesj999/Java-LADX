package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.render.IndexedRenderer;
import linksawakening.rom.RomUtil;
import linksawakening.state.PlayerState;

/** Inventory status sprites from RenderInventoryStatus and the Select prompt in bank $20. */
final class InventoryStatusRenderer {
    private static final int LEFT = 0x53 - 8;
    private final byte[] rom;
    private final GPU tiles = new GPU();
    private final int[][] palettes;

    InventoryStatusRenderer(byte[] rom, int[][] palettes) {
        this.rom = rom;
        this.palettes = palettes;
        tiles.loadInventoryStatusTiles(rom);
    }

    void drawPrompt(byte[] buffer, int frame) {
        drawSprite(buffer, LEFT, 128, 0x4A, 0x26);
        if ((frame & 0x30) != 0) {
            for (int i = 0; i < 8; i++) {
                drawSprite(buffer, LEFT + 8 + i * 8, 128, 0x4C + i * 2, 6);
            }
        }
        drawSprite(buffer, LEFT + 72, 128, 0x4A, 0x26);
    }

    void drawPanel(byte[] buffer, int y, PlayerState player) {
        drawRow(buffer, LEFT, y, pointer(0x6474 + player.tunicType() * 2), 4);
        drawSprite(buffer, LEFT + 32, y, 0x7C, 3);
        // OAM emits eleven sprites on this line. The LCD selects the first ten.
        drawRow(buffer, LEFT + 40, y, pointer(0x64A2 + player.heartPieces() * 2), 5);
        drawRow(buffer, LEFT, y + 16, 0x64AA, 3);
        drawRow(buffer, LEFT + 24, y + 16, 0x64B0 + player.photoCount() * 4, 2);
        drawRow(buffer, LEFT + 40, y + 16, 0x64E4, 5);
    }

    private int pointer(int address) {
        int offset = RomUtil.romOffset(0x20, address);
        return (rom[offset] & 255) | (rom[offset + 1] & 255) << 8;
    }

    private void drawRow(byte[] buffer, int x, int y, int address, int count) {
        int offset = RomUtil.romOffset(0x20, address);
        for (int i = 0; i < count; i++, offset += 2) {
            drawSprite(buffer, x + i * 8, y, rom[offset] & 255, rom[offset + 1] & 255);
        }
    }

    private void drawSprite(byte[] buffer, int x, int y, int tile, int attributes) {
        boolean flipX = (attributes & 0x20) != 0;
        boolean flipY = (attributes & 0x40) != 0;
        int[] palette = palettes[attributes & 7];
        int top = tile & 0xFE;
        IndexedRenderer.drawSpriteTile(buffer, tiles.getTile(top + (flipY ? 1 : 0)), x, y,
            flipX, flipY, palette);
        IndexedRenderer.drawSpriteTile(buffer, tiles.getTile(top + (flipY ? 0 : 1)), x, y + 8,
            flipX, flipY, palette);
    }
}
