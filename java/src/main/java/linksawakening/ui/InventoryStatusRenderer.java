package linksawakening.ui;

import linksawakening.gpu.GPU;
import linksawakening.render.HardwareFrame;
import linksawakening.render.IndexedRenderer;
import linksawakening.rom.Rom;
import linksawakening.rom.RomAddress;
import linksawakening.state.PlayerState;

/** RenderInventoryStatus in bank $20. */
final class InventoryStatusRenderer {
    private static final int LEFT = 0x53 - 8;
    private final Rom rom;
    private final GPU tiles = new GPU();

    InventoryStatusRenderer(Rom rom) {
        this.rom = rom;
        for (int i = 0; i < 0x400; i++) {
            tiles.writeVRAM(0x400 + i, (byte) rom.u8(new RomAddress(0x35, 0x4000 + i)));
        }
    }

    void render(byte[] buffer, int y, PlayerState player, int[][] palettes) {
        draw(
                y,
                player,
                (x, row, tile, attr) -> IndexedRenderer.drawSpriteTile8x16(
                        buffer,
                        tiles,
                        tile & 0xFE,
                        x,
                        row,
                        (attr & 0x20) != 0,
                        (attr & 0x40) != 0,
                        palettes[attr & 7]));
    }

    void submit(HardwareFrame frame, int y, PlayerState player, int[][] palettes) {
        draw(
                y,
                player,
                (x, row, tile, attr) -> frame.sprite8x16(
                        tiles, tile & 0xFE, x, row, (attr & 0x20) != 0, (attr & 0x40) != 0, palettes[attr & 7]));
    }

    private void draw(int y, PlayerState player, SpriteSink sink) {
        drawRow(sink, LEFT, y, pointer(0x6474 + player.equipmentLevels().tunicType() * 2), 4);
        sink.draw(LEFT + 32, y, 0x7C, 3);
        // Eleven sprites are emitted on the first line. The LCD selects the first ten.
        int hearts = Math.min(3, player.progression().heartPieces());
        drawRow(sink, LEFT + 40, y, pointer(0x64A2 + hearts * 2), 5);
        drawRow(sink, LEFT, y + 16, 0x64AA, 3);
        drawRow(sink, LEFT + 24, y + 16, 0x64B0 + player.progression().photoCount() * 4, 2);
        drawRow(sink, LEFT + 40, y + 16, 0x64E4, 5);
    }

    private int pointer(int address) {
        return read(address) | read(address + 1) << 8;
    }

    private void drawRow(SpriteSink sink, int x, int y, int address, int count) {
        for (int i = 0; i < count; i++, address += 2) {
            sink.draw(x + i * 8, y, read(address), read(address + 1));
        }
    }

    private int read(int address) {
        return rom.u8(new RomAddress(0x20, address));
    }

    private interface SpriteSink {
        void draw(int x, int y, int tile, int attributes);
    }
}
