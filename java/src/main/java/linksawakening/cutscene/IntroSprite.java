package linksawakening.cutscene;
/** Immutable intro sprite values. */
public record IntroSprite(int tileIndex, int x, int y, int paletteIndex, boolean flipX, boolean flipY, int height) {

    public IntroSprite(int tileIndex, int x, int y, int paletteIndex, boolean flipX, boolean flipY) {
        this(tileIndex, x, y, paletteIndex, flipX, flipY, 8);
    }
}
