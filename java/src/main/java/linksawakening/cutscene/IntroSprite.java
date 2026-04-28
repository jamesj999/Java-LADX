package linksawakening.cutscene;

public record IntroSprite(int tileIndex, int x, int y, int paletteIndex,
                          boolean flipX, boolean flipY, int height) {

    public IntroSprite(int tileIndex, int x, int y, int paletteIndex,
                       boolean flipX, boolean flipY) {
        this(tileIndex, x, y, paletteIndex, flipX, flipY, 8);
    }

    static IntroSprite fromOam8x16(int tileIndex, int oamX, int oamY, int paletteIndex,
                                   boolean flipX, boolean flipY) {
        return new IntroSprite(tileIndex, oamX - 8, oamY - 16, paletteIndex, flipX, flipY, 16);
    }
}
