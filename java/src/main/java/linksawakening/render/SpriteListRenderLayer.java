package linksawakening.render;

/** Draws a software OAM list in entry order, with transparent OBJ color zero. */
public final class SpriteListRenderLayer implements HardwareRenderLayer {
    public record Sprite(int tileIndex, int x, int y, int paletteIndex,
                         boolean flipX, boolean flipY, int height) {}

    private final Iterable<Sprite> sprites;
    private final int[][] palettes;
    private linksawakening.gpu.EntitySpriteTileSnapshot tiles;

    public SpriteListRenderLayer(Iterable<Sprite> sprites, int[][] palettes) {
        this.sprites = sprites;
        this.palettes = palettes;
    }

    public SpriteListRenderLayer(Iterable<Sprite> sprites, int[][] palettes,
                                 linksawakening.gpu.EntitySpriteTileSnapshot tiles) {
        this(sprites, palettes);
        this.tiles = tiles;
    }

    @Override public void submit(HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        var ordered = new java.util.ArrayList<Sprite>();
        sprites.forEach(ordered::add);
        for (int i = ordered.size() - 1; i >= 0; i--) {
            Sprite sprite = ordered.get(i);
            int[] palette = palettes[Math.min(sprite.paletteIndex(), palettes.length - 1)];
            if (sprite.height() == 16) {
                int base = sprite.tileIndex() & 0xFE;
                int top = sprite.flipY() ? base + 1 : base;
                int bottom = sprite.flipY() ? base : base + 1;
                frame.append8x16(tiles == null ? gpu.getTile(top) : tiles.tile(top),
                    tiles == null ? gpu.getTile(bottom) : tiles.tile(bottom), sprite.x(), sprite.y(),
                    sprite.flipX(), sprite.flipY(), palette, false);
            } else {
                frame.append(gpu.getTile(sprite.tileIndex()), sprite.x(), sprite.y(),
                    sprite.flipX(), sprite.flipY(), palette, false);
            }
        }
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
                if (tiles != null) {
                    IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, sprite.tileIndex(),
                        sprite.x(), sprite.y(), sprite.flipX(), sprite.flipY(), palettes[paletteIndex]);
                    continue;
                }
                IndexedRenderer.drawSpriteTile8x16(context.buffer(), context.gpu(), sprite.tileIndex(),
                    sprite.x(), sprite.y(), sprite.flipX(), sprite.flipY(), palettes[paletteIndex]);
            } else {
                IndexedRenderer.drawSpriteTile(context.buffer(), context.gpu().getTile(sprite.tileIndex()),
                    sprite.x(), sprite.y(), sprite.flipX(), sprite.flipY(), palettes[paletteIndex]);
            }
        }
    }
}
