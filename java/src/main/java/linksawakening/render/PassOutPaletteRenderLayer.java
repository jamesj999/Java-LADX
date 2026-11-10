package linksawakening.render;

/** func_020_6A30 with e=8: saturating +1 red, +1 green, +1 blue per frame.
 * CopyLinkTunicPalette restores Link's unmodified palette after that operation.
 */
final class PassOutPaletteRenderLayer implements HardwareRenderLayer {
    private final RenderLayer layer;
    private final int frames;
    PassOutPaletteRenderLayer(RenderLayer layer, int frames) {
        this.layer = layer;
        this.frames = frames;
    }
    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        frame.withPaletteTransform(this::transform,
            () -> ((HardwareRenderLayer) layer).submit(frame, gpu));
    }
    private int transform(int rgb) {
        int result = 0;
        for (int shift = 0; shift <= 16; shift += 8) {
            int channel = Math.min(31, ((rgb >>> shift) & 255) / 8 + frames);
            result |= (channel << 3) << shift;
        }
        return result;
    }
}
