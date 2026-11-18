package linksawakening.render;

import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;

/** Shared tile-pair submission for source-compatible entity drawing. */
final class OamDrawing {
    static void draw8x16(
            RenderContext context, GPU gpu, int tile, int x, int y, boolean fx, boolean fy, int[] palette) {
        int base = tile & 254;
        context.oam()
                .append8x16(
                        gpu.getTile(fy ? base + 1 : base),
                        gpu.getTile(fy ? base : base + 1),
                        x,
                        y,
                        fx,
                        fy,
                        palette,
                        false);
    }

    static void draw8x16(
            RenderContext context,
            EntitySpriteTileSnapshot tiles,
            int tile,
            int x,
            int y,
            boolean fx,
            boolean fy,
            int[] palette) {
        int base = tile & 254;
        context.oam()
                .append8x16(
                        tiles.readOnlyTile(fy ? base + 1 : base),
                        tiles.readOnlyTile(fy ? base : base + 1),
                        x,
                        y,
                        fx,
                        fy,
                        palette,
                        false);
    }

    static void draw8(RenderContext context, Tile tile, int x, int y, boolean fx, boolean fy, int[] palette) {
        context.oam().append(tile, x, y, fx, fy, palette, false);
    }
}
