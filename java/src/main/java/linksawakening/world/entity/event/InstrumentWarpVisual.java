package linksawakening.world.entity.event;







/** Render state for $03:5E29/$20:6D0E and the fullscreen controller $07:6523. */
public record InstrumentWarpVisual(int backgroundFade, int objectFade, int commandBatches,
                                   int[] tiles, int[] attributes, int centerX, int centerY, int[] swirl) {
    public InstrumentWarpVisual(int bg, int obj, int batches, int[] tiles, int[] attrs) {
        this(bg, obj, batches, tiles, attrs, 0, 0, new int[0]);
    }
    public InstrumentWarpVisual {
        swirl = swirl.clone();
        tiles = tiles.clone();
        attributes = attributes.clone();
    }
    @Override public int[] swirl() { return swirl.clone(); }
    public int[][] backgroundPalettes(int[][] original) { return fadePalettes(original, false); }
    public int[][] objectPalettes(int[][] original) { return fadePalettes(original, true); }
    private int[][] fadePalettes(int[][] original, boolean objects) {
        int[][] result = new int[original.length][];
        for (int p = 0; p < original.length; p++) {
            int amount = objects ? (p < 4 ? objectFade : 0)
                : backgroundFade + (p >= 4 ? objectFade : 0);
            result[p] = original[p].clone();
            if (amount == 0) continue;
            for (int c = 0; c < result[p].length; c++) {
                int rgb = 0;
                for (int shift : new int[]{16, 8, 0}) {
                    rgb |= (Math.min(31, ((original[p][c] >> shift) & 255) / 8 + amount) << 3) << shift;
                }
                result[p][c] = rgb;
            }
        }
        return result;
    }
    @Override public int[] tiles() { return tiles.clone(); }
    @Override public int[] attributes() { return attributes.clone(); }
}
