package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.ScrollController;

/** Renders LinksBedSpriteVariants and the GBC mattress from Marin's entity sheet. */
public final class MarinWakeUpBedRenderLayer implements RenderLayer {
    private static final int[][] PAIR_TILES = {
        { 0x40, 0x42 }, { 0x42, 0x40 }, { 0x44, 0x46 },
        { 0x48, 0x4A }, { 0x48, 0x4C }
    };
    private final Link link;
    private final EntitySpriteTileSnapshot tiles;
    private final int[][] palettes;
    private final ScrollController scrollController;

    public MarinWakeUpBedRenderLayer(Link link, EntitySpriteTileSnapshot tiles,
                                     int[][] palettes, ScrollController scrollController) {
        if (link == null || tiles == null || palettes == null || palettes.length < 8
            || scrollController == null) {
            throw new IllegalArgumentException("Marin wake-up render inputs are incomplete");
        }
        this.link = link;
        this.tiles = tiles;
        this.palettes = palettes;
        this.scrollController = scrollController;
    }

    @Override
    public void render(RenderContext context) {
        if (!link.isMarinWakeUpBedVisible()) {
            return;
        }
        int variant = link.marinWakeUpBedVariant();
        int originX = 0x38 - 0x08 - scrollController.screenShakeHorizontal();
        int originY = 0x34 - 0x10;
        IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, 0x4E,
            originX, originY + 8, true, false, palettes[6]);
        IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, 0x4E,
            originX, originY, false, false, palettes[6]);
        boolean flipX = variant == 1;
        int[] pair = PAIR_TILES[variant];
        IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, pair[1],
            originX + 8, originY, flipX, false, palettes[variant < 2 ? 7 : 0]);
        IndexedRenderer.drawSpriteTile8x16(context.buffer(), tiles, pair[0],
            originX, originY, flipX, false, palettes[variant < 2 ? 7 : 0]);
    }
}
