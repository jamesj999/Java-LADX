package linksawakening.render;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import linksawakening.world.RoomConstants;

public final class IndexedRenderer {
    private static final int ATTR_FLIP_X = 0x20;
    private static final int ATTR_FLIP_Y = 0x40;

    private IndexedRenderer() {
    }

    public static void clear(byte[] buffer) {
        for (int i = 0; i < buffer.length; i += 4) {
            buffer[i] = 0;
            buffer[i + 1] = 0;
            buffer[i + 2] = 0;
            buffer[i + 3] = (byte) 0xFF;
        }
    }

    public static void renderRoom(byte[] buffer, GPU gpu, int[] tileIds, int[] tileAttrs,
                                  int[][] palettes, int offsetX, int offsetY) {
        for (int y = 0; y < RoomConstants.ROOM_TILE_HEIGHT; y++) {
            for (int x = 0; x < RoomConstants.ROOM_TILE_WIDTH; x++) {
                int mapIndex = y * RoomConstants.ROOM_TILE_WIDTH + x;
                renderTile(buffer, gpu, tileIds[mapIndex], tileAttrs[mapIndex], palettes,
                    x * 8 + offsetX, y * 8 + offsetY, true);
            }
        }
    }

    public static void renderBackground(byte[] buffer, GPU gpu, int[] tilemap, int[] attrmap,
                                        int[][] palettes, int width, int height) {
        renderBackground(buffer, gpu, tilemap, attrmap, palettes, width, height, width, height, 0, 0);
    }

    public static void renderBackground(byte[] buffer, GPU gpu, int[] tilemap, int[] attrmap,
                                        int[][] palettes, int mapWidth, int mapHeight,
                                        int viewportWidth, int viewportHeight,
                                        int scrollX, int scrollY) {
        int fineX = Math.floorMod(scrollX, 8);
        int fineY = Math.floorMod(scrollY, 8);
        int baseTileX = Math.floorMod(scrollX / 8, mapWidth);
        int baseTileY = Math.floorMod(scrollY / 8, mapHeight);
        int tilesWide = viewportWidth + (fineX == 0 ? 0 : 1);
        int tilesHigh = viewportHeight + (fineY == 0 ? 0 : 1);
        for (int y = 0; y < tilesHigh; y++) {
            for (int x = 0; x < tilesWide; x++) {
                int mapX = (baseTileX + x) & (mapWidth - 1);
                int mapY = (baseTileY + y) & (mapHeight - 1);
                int mapIndex = mapY * mapWidth + mapX;
                renderTile(buffer, gpu, tilemap[mapIndex], attrmap[mapIndex], palettes,
                    x * 8 - fineX, y * 8 - fineY, false);
            }
        }
    }

    public static void renderBackgroundLineScroll(byte[] buffer, GPU gpu, int[] tilemap, int[] attrmap,
                                                  int[][] palettes, int mapWidth, int mapHeight,
                                                  int viewportWidth, int viewportHeight,
                                                  int[] lineScrollX, int scrollY) {
        int viewportPixelWidth = viewportWidth * 8;
        int viewportPixelHeight = viewportHeight * 8;
        for (int py = 0; py < viewportPixelHeight; py++) {
            int sourceY = Math.floorMod(scrollY + py, mapHeight * 8);
            int mapY = sourceY / 8;
            int tileY = sourceY & 0x07;
            int scrollX = lineScrollX[Math.min(py, lineScrollX.length - 1)];
            for (int px = 0; px < viewportPixelWidth; px++) {
                int sourceX = Math.floorMod(scrollX + px, mapWidth * 8);
                int mapX = sourceX / 8;
                int tileX = sourceX & 0x07;
                int mapIndex = mapY * mapWidth + mapX;
                renderBackgroundPixel(buffer, gpu, tilemap[mapIndex], attrmap[mapIndex], palettes,
                    px, py, tileX, tileY);
            }
        }
    }

    public static void renderBackgroundTiles(byte[] buffer, GPU gpu, int[] tilemap, int[] attrmap,
                                             int[][] palettes, int width, int height) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int mapIndex = y * width + x;
                renderTile(buffer, gpu, tilemap[mapIndex], attrmap[mapIndex], palettes,
                    x * 8, y * 8, false);
            }
        }
    }

    public static void drawSpriteTile(byte[] buffer, Tile tile, int screenX, int screenY,
                                      boolean flipX, boolean flipY, int[] palette) {
        if (tile == null) {
            return;
        }
        for (int ty = 0; ty < 8; ty++) {
            for (int tx = 0; tx < 8; tx++) {
                int px = screenX + tx;
                int py = screenY + ty;
                if (px < 0 || px >= Framebuffer.WIDTH || py < 0 || py >= Framebuffer.HEIGHT) {
                    continue;
                }
                int sx = flipX ? 7 - tx : tx;
                int sy = flipY ? 7 - ty : ty;
                int colorIndex = tile.getPixel(sx, sy);
                if (colorIndex == 0) {
                    continue;
                }
                writePixel(buffer, px, py, palette[colorIndex]);
            }
        }
    }

    public static void drawSpriteTile8x16(byte[] buffer, GPU gpu, int tileIndex, int screenX, int screenY,
                                          boolean flipX, boolean flipY, int[] palette) {
        int baseTileIndex = tileIndex & 0xFE;
        Tile top = gpu.getTile(flipY ? baseTileIndex + 1 : baseTileIndex);
        Tile bottom = gpu.getTile(flipY ? baseTileIndex : baseTileIndex + 1);
        drawSpriteTile(buffer, top, screenX, screenY, flipX, flipY, palette);
        drawSpriteTile(buffer, bottom, screenX, screenY + 8, flipX, flipY, palette);
    }

    public static int resolveSignedBgTileIndex(int tileId) {
        return tileId < 0x80 ? tileId + 0x100 : tileId;
    }

    private static void renderTile(byte[] buffer, GPU gpu, int tileId, int attributes,
                                   int[][] palettes, int screenX, int screenY,
                                   boolean skipOffscreen) {
        int tileIndex = resolveSignedBgTileIndex(tileId & 0xFF);
        int attr = attributes & 0xFF;
        int paletteIndex = attr & 0x07;
        boolean flipX = (attr & ATTR_FLIP_X) != 0;
        boolean flipY = (attr & ATTR_FLIP_Y) != 0;

        Tile tile = gpu.getTile(tileIndex);
        if (tile == null) {
            return;
        }
        if (skipOffscreen && (screenX + 8 <= 0 || screenX >= Framebuffer.WIDTH
            || screenY + 8 <= 0 || screenY >= Framebuffer.HEIGHT)) {
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

    private static void renderBackgroundPixel(byte[] buffer, GPU gpu, int tileId, int attributes,
                                              int[][] palettes, int screenX, int screenY,
                                              int tileX, int tileY) {
        int tileIndex = resolveSignedBgTileIndex(tileId & 0xFF);
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

    private static void writePixel(byte[] buffer, int x, int y, int color) {
        int offset = (y * Framebuffer.WIDTH + x) * 4;
        buffer[offset] = (byte) ((color >> 16) & 0xFF);
        buffer[offset + 1] = (byte) ((color >> 8) & 0xFF);
        buffer[offset + 2] = (byte) (color & 0xFF);
        buffer[offset + 3] = (byte) 0xFF;
    }
}
