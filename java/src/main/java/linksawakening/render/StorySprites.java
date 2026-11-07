package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.transition.ScrollController;

/** ROM story poses submitted without per-pose layer adapters. */
final class StorySprites {
    private StorySprites() {}
    private static final int[][] PAIR_TILES = {
        { 0x40, 0x42 }, { 0x42, 0x40 }, { 0x44, 0x46 },
        { 0x48, 0x4A }, { 0x48, 0x4C }
    };
    private static final int SHIELD_TILE = 0x86;

    static HardwareRenderLayer marinBed(Link link, EntitySpriteTileSnapshot tiles, int[][] palettes, ScrollController scrollController) {
        if (link == null || tiles == null || palettes == null || palettes.length < 8
            || scrollController == null) {
            throw new IllegalArgumentException("Marin wake-up render inputs are incomplete");
        }
        return (frame, gpu) -> {
            // Preserve OBJ source metadata used by dialog filtering and parity reports.
            frame.submittingLayer("MarinWakeUpBedRenderLayer");
            if (!link.isMarinWakeUpBedVisible()) {
                return;
            }
            int variant = link.marinWakeUpBedVariant();
            int originX = 0x38 - 0x08 - scrollController.screenShakeHorizontal();
            int originY = 0x34 - 0x10;
            boolean flipX = variant == 1;
            int[] pair = PAIR_TILES[variant];
            int palette = variant < 2 ? 7 : 0;
            // List order is OAM priority order; preserve the former painter order.
            new SpriteListRenderLayer(java.util.List.of(
                new SpriteListRenderLayer.Sprite(pair[0], originX, originY, palette, flipX, false, 16),
                new SpriteListRenderLayer.Sprite(pair[1], originX + 8, originY, palette, flipX, false, 16),
                new SpriteListRenderLayer.Sprite(0x4E, originX, originY, 6, false, false, 16),
                new SpriteListRenderLayer.Sprite(0x4E, originX + 8, originY, 6, true, false, 16)
            ), palettes, tiles).submit(frame, gpu);
        };
    }

    static HardwareRenderLayer tarinShield(Link link, ScrollController scrollController) {
        if (link == null || link.tarinShieldPalette() == null || scrollController == null) {
            throw new IllegalArgumentException("Tarin shield render inputs are incomplete");
        }
        return (frame, gpu) -> {
            // Preserve OBJ source metadata used by dialog filtering and parity reports.
            frame.submittingLayer("TarinShieldRenderLayer");
            // RenderActiveEntitySprite ($3CA4) centres a single 8px OAM column
            // within the 16px entity pose by adding four pixels to its X origin.
            int screenX = link.pixelX() + 4 - scrollController.screenShakeHorizontal();
            int screenY = link.pixelY() - 0x0C;
            new SpriteListRenderLayer(java.util.List.of(new SpriteListRenderLayer.Sprite(
                SHIELD_TILE, screenX, screenY, 0, false, false, 16)),
                new int[][] { link.tarinShieldPalette() }).submit(frame, gpu);
        };
    }

    static HardwareRenderLayer linkShadow(Link link, int[] palette, ScrollController scroll, int frameNumber) {
        return (frame, gpu) -> {
            // Preserve OBJ source metadata used by dialog filtering and parity reports.
            frame.submittingLayer("LinkShadowRenderLayer");
            if (link.romEntityZ() == 0 || (frameNumber & 1) != 0 || scroll.isActive()
                || link.romEntityY() + 0x0B >= 0x88) return;
            // bank2.asm $759B-$75AF writes wLinkOAMBuffer ($C000), OAM slot 0.
            frame.append8x16(gpu.getTile(0x26), gpu.getTile(0x27),
                link.pixelX() + 4 - scroll.screenShakeHorizontal(), link.pixelY() + 0x0B,
                false, false, palette, false);
        };
    }
}
