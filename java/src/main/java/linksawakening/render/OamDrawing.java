package linksawakening.render;

import java.util.function.IntFunction;
import linksawakening.gpu.GPU;
import linksawakening.gpu.Tile;
import linksawakening.gpu.EntitySpriteTileSnapshot;

/** Shared tile-pair submission for source-compatible entity drawing. */
final class OamDrawing {
    static void draw8x16(RenderContext context, GPU gpu, int tile, int x, int y,
                         boolean fx, boolean fy, int[] palette) {
        if (context.oam() == null) {
            IndexedRenderer.drawSpriteTile8x16(context.buffer(), gpu, tile, x, y, fx, fy, palette);
        } else submit(context.oam(), gpu::getTile, tile, x, y, fx, fy, palette);
    }
    static void draw8x16(RenderContext context, EntitySpriteTileSnapshot tiles, int tile, int x, int y,
                         boolean fx, boolean fy, int[] palette) {
        if (context.oam() == null) {
            IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, tile, x, y, fx, fy, palette);
        } else submit(context.oam(), tiles::tile, tile, x, y, fx, fy, palette);
    }
    static void draw8(RenderContext context, Tile tile, int x, int y,
                      boolean fx, boolean fy, int[] palette) {
        if (context.oam() == null) IndexedRenderer.drawSpriteTile(context.buffer(), tile, x, y, fx, fy, palette);
        else context.oam().append(tile, x, y, fx, fy, palette, false);
    }
    private static void submit(OamSink sink, IntFunction<Tile> tiles, int tile, int x, int y,
                               boolean fx, boolean fy, int[] palette) {
        int base = tile & 254;
        sink.append(tiles.apply(fy ? base + 1 : base), x, y, fx, fy, palette, false);
        sink.append(tiles.apply(fy ? base : base + 1), x, y + 8, fx, fy, palette, false);
    }
}
