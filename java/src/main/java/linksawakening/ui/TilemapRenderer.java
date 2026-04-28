package linksawakening.ui;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;

public final class TilemapRenderer {

    private TilemapRenderer() {
    }

    public static void render(
        byte[] displayBuffer,
        GPU gpu,
        int[] tileIds,
        int[] tileAttrs,
        int[][] palettes,
        int tileWidth,
        int tileHeight,
        int offsetX,
        int offsetY
    ) {
        for (int y = 0; y < tileHeight; y++) {
            for (int x = 0; x < tileWidth; x++) {
                int mapIndex = y * tileWidth + x;
                int tileIndex = resolveSignedBgTileIndex(tileIds[mapIndex] & 0xFF);
                int attributes = tileAttrs != null ? (tileAttrs[mapIndex] & 0xFF) : 0;
                int paletteIndex = attributes & 0x07;
                boolean flipX = (attributes & 0x20) != 0;
                boolean flipY = (attributes & 0x40) != 0;

                Tile tile = gpu.getTile(tileIndex);
                if (tile == null) {
                    continue;
                }

                int[] palette = palettes[Math.min(paletteIndex, palettes.length - 1)];

                int tileScreenX = x * 8 + offsetX;
                int tileScreenY = y * 8 + offsetY;

                if (tileScreenX + 8 <= 0 || tileScreenX >= Framebuffer.WIDTH
                    || tileScreenY + 8 <= 0 || tileScreenY >= Framebuffer.HEIGHT) {
                    continue;
                }

                for (int ty = 0; ty < 8; ty++) {
                    for (int tx = 0; tx < 8; tx++) {
                        int px = tileScreenX + tx;
                        int py = tileScreenY + ty;

                        if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                            continue;
                        }

                        int sourceX = flipX ? 7 - tx : tx;
                        int sourceY = flipY ? 7 - ty : ty;
                        int color = palette[tile.getPixel(sourceX, sourceY)];

                        int offset = (py * Framebuffer.WIDTH + px) * 4;
                        displayBuffer[offset]     = (byte) ((color >> 16) & 0xFF);
                        displayBuffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
                        displayBuffer[offset + 2] = (byte) (color & 0xFF);
                        displayBuffer[offset + 3] = (byte) 0xFF;
                    }
                }
            }
        }
    }

    private static int resolveSignedBgTileIndex(int tileId) {
        return tileId < 0x80 ? tileId + 0x100 : tileId;
    }
}
