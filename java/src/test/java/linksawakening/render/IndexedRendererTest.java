package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class IndexedRendererTest {

    @Test
    void signedBgAddressingMapsLowTileIdsIntoSecondTileBlock() {
        assertEquals(0x100, IndexedRenderer.resolveSignedBgTileIndex(0x00));
        assertEquals(0x17F, IndexedRenderer.resolveSignedBgTileIndex(0x7F));
        assertEquals(0x80, IndexedRenderer.resolveSignedBgTileIndex(0x80));
        assertEquals(0xFF, IndexedRenderer.resolveSignedBgTileIndex(0xFF));
    }

    @Test
    void encodedBackgroundRenderingUsesSignedTileIds() {
        GPU gpu = new GPU();
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        int rawTileColor = 0x112233;
        int signedTileColor = 0x445566;
        int[][] palettes = {
            { 0, rawTileColor, signedTileColor, 0 }
        };

        writeSolidTile(gpu, 0x000, 1);
        writeSolidTile(gpu, 0x100, 2);

        IndexedRenderer.renderBackground(buffer, gpu, new int[] { 0x00 }, new int[] { 0x00 },
            palettes, 1, 1);

        assertEquals(signedTileColor, firstPixelColor(buffer));
    }

    @Test
    void backgroundLineScrollCanUseDifferentScrollForDifferentScreenRows() {
        GPU gpu = new GPU();
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        int leftColor = 0x112233;
        int rightColor = 0x445566;
        int[][] palettes = {
            { 0, leftColor, rightColor, 0 }
        };
        int[] tilemap = { 0x80, 0x81 };
        int[] attrmap = { 0x00, 0x00 };
        int[] lineScrollX = new int[Framebuffer.HEIGHT];

        writeSolidTile(gpu, 0x80, 1);
        writeSolidTile(gpu, 0x81, 2);
        lineScrollX[8] = 8;

        IndexedRenderer.renderBackgroundLineScroll(buffer, gpu, tilemap, attrmap,
            palettes, 2, 1, 2, 2, lineScrollX, 0);

        assertEquals(leftColor, pixelColor(buffer, 0, 0));
        assertEquals(rightColor, pixelColor(buffer, 0, 8));
    }

    @Test
    void sprite8x16RenderingDrawsTheFollowingTileAsTheBottomHalf() {
        GPU gpu = new GPU();
        byte[] buffer = new byte[Framebuffer.WIDTH * Framebuffer.HEIGHT * 4];
        int topColor = 0x112233;
        int bottomColor = 0x445566;
        int[] palette = { 0, topColor, bottomColor, 0 };

        writeSolidTile(gpu, 0x20, 1);
        writeSolidTile(gpu, 0x21, 2);

        IndexedRenderer.drawSpriteTile8x16(buffer, gpu, 0x20, 4, 6, false, false, palette);

        assertEquals(topColor, pixelColor(buffer, 4, 6));
        assertEquals(bottomColor, pixelColor(buffer, 4, 14));
    }

    private static void writeSolidTile(GPU gpu, int tileIndex, int colorIndex) {
        int low = (colorIndex & 0x01) != 0 ? 0xFF : 0x00;
        int high = (colorIndex & 0x02) != 0 ? 0xFF : 0x00;
        int base = tileIndex * GPU.TILE_DATA_SIZE;
        for (int y = 0; y < 8; y++) {
            gpu.writeVRAM(base + y * 2, (byte) low);
            gpu.writeVRAM(base + y * 2 + 1, (byte) high);
        }
    }

    private static int firstPixelColor(byte[] buffer) {
        return pixelColor(buffer, 0, 0);
    }

    private static int pixelColor(byte[] buffer, int x, int y) {
        int index = (y * Framebuffer.WIDTH + x) * 4;
        return (Byte.toUnsignedInt(buffer[index]) << 16)
            | (Byte.toUnsignedInt(buffer[index + 1]) << 8)
            | Byte.toUnsignedInt(buffer[index + 2]);
    }
}
