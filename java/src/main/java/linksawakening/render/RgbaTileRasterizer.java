package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;

/** Legacy RGB background tile sampling for direct renderer compatibility. */
final class RgbaTileRasterizer {
    private static final int ATTR_FLIP_X = 0x20, ATTR_FLIP_Y = 0x40;

    private RgbaTileRasterizer() {}

    static void renderTile(
            byte[] buffer,
            GPU gpu,
            int tileId,
            int attributes,
            int[][] palettes,
            int screenX,
            int screenY,
            boolean skipOffscreen) {
        int tileIndex = IndexedRenderer.resolveSignedBgTileIndex(tileId & 0xFF);
        int attr = attributes & 0xFF;
        int paletteIndex = attr & 0x07;
        boolean flipX = (attr & ATTR_FLIP_X) != 0;
        boolean flipY = (attr & ATTR_FLIP_Y) != 0;

        Tile tile = gpu.getTile(tileIndex);
        if (tile == null) {
            return;
        }
        if (skipOffscreen
                && (screenX + 8 <= 0
                        || screenX >= Framebuffer.WIDTH
                        || screenY + 8 <= 0
                        || screenY >= Framebuffer.HEIGHT)) {
            return;
        }

        int[] palette = palettes[Math.min(paletteIndex, palettes.length - 1)];
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sourceX = flipX ? 7 - tx : tx;
                int sourceY = flipY ? 7 - ty : ty;
                writePixel(buffer, px, py, palette[tile.getPixel(sourceX, sourceY)]);
            }
        }
    }

    static void renderBackgroundPixel(
            byte[] buffer,
            GPU gpu,
            int tileId,
            int attributes,
            int[][] palettes,
            int screenX,
            int screenY,
            int tileX,
            int tileY) {
        int tileIndex = IndexedRenderer.resolveSignedBgTileIndex(tileId & 0xFF);
        int attr = attributes & 0xFF;
        int paletteIndex = attr & 0x07;
        boolean flipX = (attr & ATTR_FLIP_X) != 0;
        boolean flipY = (attr & ATTR_FLIP_Y) != 0;
        Tile tile = gpu.getTile(tileIndex);
        if (tile == null) {
            return;
        }
        int sourceX = flipX ? 7 - tileX : tileX;
        int sourceY = flipY ? 7 - tileY : tileY;
        int[] palette = palettes[Math.min(paletteIndex, palettes.length - 1)];
        writePixel(buffer, screenX, screenY, palette[tile.getPixel(sourceX, sourceY)]);
    }

    static void writePixel(byte[] buffer, int x, int y, int color) {
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        buffer[offset] = (byte) ((color >> 16) & 0xFF);
        buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
        buffer[offset + 2] = (byte) (color & 0xFF);
        buffer[offset + 3] = (byte) 0xFF;
    }
}
