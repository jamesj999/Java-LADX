package linksawakening.render;

import linksawakening.cutscene.IntroSprite;

/** @deprecated Use the generic sprite-list layer for new sprite producers. */
@Deprecated
public final class CutsceneSpriteRenderLayer implements RenderLayer {
    private final Iterable<IntroSprite> sprites;
    private final int[][] palettes;

    public CutsceneSpriteRenderLayer(Iterable<IntroSprite> sprites, int[][] palettes) {
        this.sprites = sprites;
        this.palettes = palettes;
    }

    @Override
    public void render(RenderContext context) {
        java.util.List<SpriteListRenderLayer.Sprite> entries = new java.util.ArrayList<>();
        for (IntroSprite sprite : sprites) {
            entries.add(new SpriteListRenderLayer.Sprite(sprite.tileIndex(), sprite.x(), sprite.y(),
                sprite.paletteIndex(), sprite.flipX(), sprite.flipY(), sprite.height()));
        }
        new SpriteListRenderLayer(entries, palettes).render(context);
    }
}
