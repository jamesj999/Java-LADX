package linksawakening.ui;

import linksawakening.gpu.Framebuffer;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;

public final class TilemapRenderer {
    private static final ThreadLocal<Region> LEGACY_REGION = ThreadLocal.withInitial(() -> new Region(0, 0, 0, 0));

    private TilemapRenderer() {}
    /** @deprecated Use the Region argument bundle overload. */
    @Deprecated(forRemoval = true)
    public static void render(
            byte[] displayBuffer,
            GPU gpu,
            int[] tileIds,
            int[] tileAttrs,
            int[][] palettes,
            int tileWidth,
            int tileHeight,
            int offsetX,
            int offsetY) {
        render(
                displayBuffer,
                gpu,
                tileIds,
                tileAttrs,
                palettes,
                LEGACY_REGION.get().set(tileWidth, tileHeight, offsetX, offsetY));
    }

    public static void render(
            byte[] displayBuffer, GPU gpu, int[] tileIds, int[] tileAttrs, int[][] palettes, Region arguments) {
        int tileWidth = arguments.tileWidth();
        int tileHeight = arguments.tileHeight();
        int offsetX = arguments.offsetX();
        int offsetY = arguments.offsetY();

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

                if (tileScreenX + 8 <= 0
                        || tileScreenX >= Framebuffer.WIDTH
                        || tileScreenY + 8 <= 0
                        || tileScreenY >= Framebuffer.HEIGHT) {
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
                        displayBuffer[offset] = (byte) ((color >> 16) & 0xFF);
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
    /** @deprecated Use the Region argument bundle overload. */
    @Deprecated(forRemoval = true)
    public static void submit(
            linksawakening.render.HardwareFrame frame,
            GPU gpu,
            int[] tileIds,
            int[] attrs,
            int[][] palettes,
            int width,
            int height,
            int offsetX,
            int offsetY) {
        submit(frame, gpu, tileIds, attrs, palettes, LEGACY_REGION.get().set(width, height, offsetX, offsetY));
    }

    public static void submit(
            linksawakening.render.HardwareFrame frame,
            GPU gpu,
            int[] tileIds,
            int[] attrs,
            int[][] palettes,
            Region arguments) {
        int width = arguments.tileWidth();
        int height = arguments.tileHeight();
        int offsetX = arguments.offsetX();
        int offsetY = arguments.offsetY();

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++) {
                int at = y * width + x, tile = tileIds[at] & 255;
                int attr = attrs == null ? 0 : attrs[at] & 255;
                frame.closedHudTile(
                        gpu.getTile(resolveSignedBgTileIndex(tile)),
                        tile,
                        attr,
                        x * 8 + offsetX,
                        y * 8 + offsetY,
                        palettes[Math.min(attr & 7, palettes.length - 1)]);
            }
    }

    /** Arguments shared by render operations. */
    public static final class Region {
        private int tileWidth;
        private int tileHeight;
        private int offsetX;
        private int offsetY;

        public Region(int tileWidth, int tileHeight, int offsetX, int offsetY) {
            set(tileWidth, tileHeight, offsetX, offsetY);
        }

        Region set(int tileWidth, int tileHeight, int offsetX, int offsetY) {
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            return this;
        }

        public int tileWidth() {
            return tileWidth;
        }

        public int tileHeight() {
            return tileHeight;
        }

        public int offsetX() {
            return offsetX;
        }

        public int offsetY() {
            return offsetY;
        }
    }
}
