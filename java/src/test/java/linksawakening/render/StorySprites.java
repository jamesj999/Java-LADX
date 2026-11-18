package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.world.transition.ScrollController;

/** Legacy single-layer fixtures delegate to the production story geometry. */
final class StorySprites {
    static HardwareRenderLayer marinBed(
            Link link, EntitySpriteTileSnapshot tiles, int[][] palettes, ScrollController scrollController) {
        if (link == null || tiles == null || palettes == null || palettes.length < 8 || scrollController == null) {
            throw new IllegalArgumentException("Marin wake-up render inputs are incomplete");
        }
        return (frame, gpu) -> StoryLayer.submitMarinBed(link, tiles, palettes, scrollController, frame, gpu);
    }

    static HardwareRenderLayer tarinShield(Link link, ScrollController scrollController) {
        if (link == null || link.animation().tarinShieldPalette() == null || scrollController == null)
            throw new IllegalArgumentException("Tarin shield render inputs are incomplete");
        return (frame, gpu) -> StoryLayer.submitTarinShield(link, scrollController, frame, gpu);
    }

    static HardwareRenderLayer dialogGotItem(Link link, int[][] palettes, ScrollController scroll) {
        return (frame, gpu) -> StoryLayer.submitGotItem(link, palettes, scroll, frame, gpu);
    }

    static HardwareRenderLayer linkGrass(Link link, int[][] palettes, ScrollController scroll, int paletteIndex) {
        return (frame, gpu) -> StoryLayer.submitLinkGrass(link, palettes, scroll, paletteIndex, frame, gpu);
    }

    static HardwareRenderLayer linkShadow(Link link, int[] palette, ScrollController scroll, int frameNumber) {
        return (frame, gpu) -> StoryLayer.submitLinkShadow(link, palette, scroll, frameNumber, frame, gpu);
    }
}
