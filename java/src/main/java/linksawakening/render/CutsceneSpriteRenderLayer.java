package linksawakening.render;

import linksawakening.cutscene.IntroSprite;

public final class CutsceneSpriteRenderLayer implements RenderLayer {
    private final Iterable<IntroSprite> sprites;
    private final int[][] palettes;

    public CutsceneSpriteRenderLayer(Iterable<IntroSprite> sprites, int[][] palettes) {
        this.sprites = sprites;
        this.palettes = palettes;
    }

    @Override
    public void render(RenderContext context) {
        for (IntroSprite sprite : sprites) {
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
