package linksawakening.render;

import linksawakening.gpu.GPU;

/** Retains per-line X/Y scroll while submitting indexed BG data. */
final class LineScrollBackgroundRenderLayer implements HardwareRenderLayer {
    private final int[] tiles, attributes, lineX, lineY;
    private final int[][] palettes;
    private final int scrollY;
    LineScrollBackgroundRenderLayer(int[] tiles, int[] attributes, int[][] palettes,
                                    int[] lineX, int scrollY, int[] lineY) {
        this.tiles = tiles; this.attributes = attributes; this.palettes = palettes;
        this.lineX = lineX; this.scrollY = scrollY; this.lineY = lineY;
    }
    @Override public void render(RenderContext context) {
        IndexedRenderer.renderBackgroundLineScroll(context.buffer(), context.gpu(), tiles, attributes,
            palettes, 32, 32, 20, 18, lineX, scrollY, lineY);
    }
    @Override public void submit(HardwareFrame frame, GPU gpu) {
        HardwareBackgroundRenderer.renderBackgroundLineScroll(frame, gpu, tiles, attributes,
            palettes, 32, 32, 20, 18, lineX, scrollY, lineY);
    }
}
