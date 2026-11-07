package linksawakening.render;

public final class BackgroundRenderLayer implements HardwareRenderLayer {
    private final int[] tilemap;
    private final int[] attrmap;
    private final int[][] palettes;
    private final int mapWidth;
    private final int mapHeight;
    private final int viewportWidth;
    private final int viewportHeight;
    private final int scrollX;
    private final int scrollY;
    private final int[] lineScrollX;

    private BackgroundRenderLayer(int[] tilemap, int[] attrmap, int[][] palettes,
                                  int mapWidth, int mapHeight,
                                  int viewportWidth, int viewportHeight,
                                  int scrollX, int scrollY, int[] lineScrollX) {
        this.tilemap = tilemap;
        this.attrmap = attrmap;
        this.palettes = palettes;
        this.mapWidth = mapWidth;
        this.mapHeight = mapHeight;
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;
        this.scrollX = scrollX;
        this.scrollY = scrollY;
        this.lineScrollX = lineScrollX;
    }

    public static BackgroundRenderLayer scrolling(int[] tilemap, int[] attrmap, int[][] palettes,
                                                  int mapWidth, int mapHeight,
                                                  int viewportWidth, int viewportHeight,
                                                  int scrollX, int scrollY) {
        return new BackgroundRenderLayer(tilemap, attrmap, palettes, mapWidth, mapHeight,
            viewportWidth, viewportHeight, scrollX, scrollY, null);
    }

    public static BackgroundRenderLayer lineScrolled(int[] tilemap, int[] attrmap, int[][] palettes,
                                                    int mapWidth, int mapHeight,
                                                    int viewportWidth, int viewportHeight,
                                                    int[] lineScrollX, int scrollY) {
        return new BackgroundRenderLayer(tilemap, attrmap, palettes, mapWidth, mapHeight,
            viewportWidth, viewportHeight, 0, scrollY, lineScrollX);
    }

    @Override
    public void render(RenderContext context) {
        if (lineScrollX != null) {
            IndexedRenderer.renderBackgroundLineScroll(context.buffer(), context.gpu(), tilemap, attrmap,
                palettes, mapWidth, mapHeight, viewportWidth, viewportHeight, lineScrollX, scrollY);
            return;
        }
        IndexedRenderer.renderBackground(context.buffer(), context.gpu(), tilemap, attrmap,
            palettes, mapWidth, mapHeight, viewportWidth, viewportHeight, scrollX, scrollY);
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {

        if (lineScrollX != null) {
            HardwareBackgroundRenderer.renderBackgroundLineScroll(frame, gpu, tilemap, attrmap,
                palettes, mapWidth, mapHeight, viewportWidth, viewportHeight, lineScrollX, scrollY);
            return;
        }
        HardwareBackgroundRenderer.renderBackground(frame, gpu, tilemap, attrmap,
            palettes, mapWidth, mapHeight, viewportWidth, viewportHeight, scrollX, scrollY);
        }

}
