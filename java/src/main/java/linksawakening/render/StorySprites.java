package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.transition.ScrollController;

/** ROM story poses submitted without per-pose layer adapters. */
final class StorySprites {
    private StorySprites() {}

    private static final int[][] PAIR_TILES = {{0x40, 0x42}, {0x42, 0x40}, {0x44, 0x46}, {0x48, 0x4A}, {0x48, 0x4C}};
    private static final int SHIELD_TILE = 0x86;

    static HardwareRenderLayer marinBed(
            Link link, EntitySpriteTileSnapshot tiles, int[][] palettes, ScrollController scrollController) {
        if (link == null || tiles == null || palettes == null || palettes.length < 8 || scrollController == null) {
            throw new IllegalArgumentException("Marin wake-up render inputs are incomplete");
        }
        return (frame, gpu) -> submitMarinBed(link, tiles, palettes, scrollController, frame, gpu);
    }

    static void submitMarinBed(
            Link link,
            EntitySpriteTileSnapshot tiles,
            int[][] palettes,
            ScrollController scroll,
            HardwareFrame frame,
            linksawakening.gpu.GPU gpu) {
        frame.submittingLayer("MarinWakeUpBedRenderLayer");
        if (!link.animation().isMarinWakeUpBedVisible()) return;
        int variant = link.animation().marinWakeUpBedVariant();
        int x = 0x38 - 8 - scroll.screenShakeHorizontal(), y = 0x34 - 16;
        boolean flip = variant == 1;
        int[] pair = PAIR_TILES[variant], palette = palettes[variant < 2 ? 7 : 0];
        var context = RenderContext.indexed(gpu, frame);
        // SpriteListRenderLayer consumes the original four entries backwards.
        OamDrawing.draw8x16(context, tiles, 0x4E, x + 8, y, true, false, palettes[6]);
        OamDrawing.draw8x16(context, tiles, 0x4E, x, y, false, false, palettes[6]);
        OamDrawing.draw8x16(context, tiles, pair[1], x + 8, y, flip, false, palette);
        OamDrawing.draw8x16(context, tiles, pair[0], x, y, flip, false, palette);
    }

    static HardwareRenderLayer tarinShield(Link link, ScrollController scrollController) {
        if (link == null || link.animation().tarinShieldPalette() == null || scrollController == null)
            throw new IllegalArgumentException("Tarin shield render inputs are incomplete");
        return (frame, gpu) -> submitTarinShield(link, scrollController, frame, gpu);
    }

    static void submitTarinShield(Link link, ScrollController scroll, HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        frame.submittingLayer("TarinShieldRenderLayer");
        // RenderActiveEntitySprite ($3CA4) centers the single column by four pixels.
        OamDrawing.draw8x16(
                RenderContext.indexed(gpu, frame),
                gpu,
                SHIELD_TILE,
                link.position().pixelX() + 4 - scroll.screenShakeHorizontal(),
                link.position().pixelY() - 0x0C,
                false,
                false,
                link.animation().tarinShieldPalette());
    }

    /** HandleGotItemB $02:51F1-5243: one OAM entry, tile $8E, attributes $14. */
    static HardwareRenderLayer dialogGotItem(Link link, int[][] palettes, ScrollController scroll) {
        return (frame, gpu) -> submitGotItem(link, palettes, scroll, frame, gpu);
    }

    static void submitGotItem(
            Link link, int[][] palettes, ScrollController scroll, HardwareFrame frame, linksawakening.gpu.GPU gpu) {
        if (link.animation().presentedDialogGotItem() != 2) return;
        OamDrawing.draw8x16(
                RenderContext.indexed(gpu, frame),
                gpu,
                0x8E,
                link.position().pixelX() - scroll.screenShakeHorizontal(),
                link.position().pixelY() - link.animation().romEntityZ() - 0x0E,
                false,
                false,
                palettes[4]);
    }

    static HardwareRenderLayer linkGrass(Link link, int[][] palettes, ScrollController scroll, int paletteIndex) {
        return (frame, gpu) -> submitLinkGrass(link, palettes, scroll, paletteIndex, frame, gpu);
    }

    static HardwareRenderLayer linkShadow(Link link, int[] palette, ScrollController scroll, int frameNumber) {
        return (frame, gpu) -> submitLinkShadow(link, palette, scroll, frameNumber, frame, gpu);
    }

    static void submitLinkShadow(
            Link link,
            int[] palette,
            ScrollController scroll,
            int frameNumber,
            HardwareFrame frame,
            linksawakening.gpu.GPU gpu) {
        // Preserve OBJ source metadata used by dialog filtering and parity reports.
        frame.submittingLayer("LinkShadowRenderLayer");
        if (link.animation().romEntityZ() == 0
                || (frameNumber & 1) != 0
                || scroll.isActive()
                || link.position().romEntityY() + 0x0B >= 0x88) return;
        // bank2.asm $759B-$75AF writes wLinkOAMBuffer ($C000), OAM slot 0.
        frame.append8x16(
                gpu.getTile(0x26),
                gpu.getTile(0x27),
                link.position().pixelX() + 4 - scroll.screenShakeHorizontal(),
                link.position().pixelY() + 0x0B,
                false,
                false,
                palette,
                false);
    }

    static void submitLinkGrass(
            Link link,
            int[][] palettes,
            ScrollController scroll,
            int paletteIndex,
            HardwareFrame frame,
            linksawakening.gpu.GPU gpu) {
        frame.submittingLayer("LinkGrassRenderLayer");
        if (!link.motion().grassGroundEffectActive()) return;
        int offsetX = -scroll.screenShakeHorizontal(), offsetY = 0;
        if (scroll.isActive()) {
            offsetX += scroll.linkScreenX()
                    + (link.position().pixelX() - scroll.linkScreenX()) * scroll.offset() / scroll.target()
                    - link.position().pixelX();
            offsetY = scroll.linkScreenY()
                    + (link.position().pixelY() - scroll.linkScreenY()) * scroll.offset() / scroll.target()
                    - link.position().pixelY();
        }
        int[] palette = palettes[paletteIndex];
        boolean flip = link.animation().grassGroundEffectFlipX();
        // $02:787D writes reserved OAM slots0/1 before body slots2/3.
        // Appending after the body gives those overlays priority in the
        // current painter-order compositor.
        frame.append8x16(
                gpu.getTile(0x1A),
                gpu.getTile(0x1B),
                link.position().pixelX() - 1 + offsetX,
                link.position().pixelY() + 8 + offsetY,
                flip,
                false,
                palette,
                false);
        frame.append8x16(
                gpu.getTile(0x1A),
                gpu.getTile(0x1B),
                link.position().pixelX() + 7 + offsetX,
                link.position().pixelY() + 8 + offsetY,
                !flip,
                false,
                palette,
                false);
    }
}
