package linksawakening.render;

/** Draws a software OAM list in entry order, with transparent OBJ color zero. */
public final class SpriteListRenderLayer implements RenderLayer {
    public record Sprite(int tileIndex, int x, int y, int paletteIndex,
                         boolean flipX, boolean flipY, int height) {}

    private final Iterable<Sprite> sprites;
    private final int[][] palettes;

    public SpriteListRenderLayer(Iterable<Sprite> sprites, int[][] palettes) {
        this.sprites = sprites;
        this.palettes = palettes;
    }

    @Override
    public void render(RenderContext context) {
        // Earlier OAM entries win when object pixels overlap.
        java.util.List<Sprite> ordered = new java.util.ArrayList<>();
        sprites.forEach(ordered::add);
        for (int index = ordered.size() - 1; index >= 0; index--) {
            Sprite sprite = ordered.get(index);
            int paletteIndex = Math.min(sprite.paletteIndex(), palettes.length - 1);
            if (sprite.height() == 16) {
                IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(), sprite.tileIndex(),
                    sprite.x(), sprite.y(), sprite.flipX(), sprite.flipY(), palettes[paletteIndex]);
            } else {
                IndexedRenderer.drawSpriteTile(context.buffer(), context.gpu().getTile(sprite.tileIndex()),
                    sprite.x(), sprite.y(), sprite.flipX(), sprite.flipY(), palettes[paletteIndex]);
            }
        }
    }
}
