package linksawakening.world;

/** Render state for $03:5E29/$20:6D0E and the fullscreen controller $07:6523. */
public record InstrumentWarpVisual(int backgroundFade, int objectFade, int commandBatches,
                                   int[] tiles, int[] attributes) {
    public InstrumentWarpVisual {
        tiles = tiles.clone();
        attributes = attributes.clone();
    }
    @Override public int[] tiles() { return tiles.clone(); }
    @Override public int[] attributes() { return attributes.clone(); }
}
