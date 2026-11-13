package linksawakening.render;

import linksawakening.entity.Link;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.gpu.GPU;
import linksawakening.world.entity.event.HeartPieceVisual;
import linksawakening.world.transition.ScrollController;

/** Retains one story-pose role and immutable heart-piece OAM metadata across frames.  Hardware analogue: OBJ story poses, ground effects and heart meter. */
final class StoryLayer implements HardwareRenderLayer {
    enum Kind {
        SHADOW,
        GRASS,
        MARIN,
        TARIN,
        GOT_ITEM,
        HEART
    }

    private final Kind kind;
    private GameFrameState state;
    private HeartPieceVisual heart;
    private int[] heartPairs;

    StoryLayer(Kind kind) {
        this.kind = kind;
    }

    // Identity distinguishes cache generations or explicit sentinels; equal payloads must not coalesce.
    @SuppressWarnings("ReferenceEquality")
    void configure(GameFrameState state) {
        this.state = state;
        if (kind == Kind.HEART && state.roomInputs.heart != heart) {
            heart = state.roomInputs.heart;
            heartPairs = heart.spritePairs();
        }
    }

    @Override
    public void submit(HardwareFrame frame, GPU gpu) {
        var data = state.roomInputs;
        switch (kind) {
            case SHADOW -> submitLinkShadow(
                    state.link(), data.objectPalettes[0], state.scrollController(), state.frameCounter(), frame, gpu);
            case GRASS -> submitLinkGrass(
                    state.link(), data.objectPalettes, state.scrollController(), data.grassPalette, frame, gpu);
            case MARIN -> submitMarinBed(
                    state.link(),
                    data.entities.spriteTiles(),
                    data.objectPalettes,
                    state.scrollController(),
                    frame,
                    gpu);
            case TARIN -> submitTarinShield(state.link(), state.scrollController(), frame, gpu);
            case GOT_ITEM -> submitGotItem(state.link(), data.objectPalettes, state.scrollController(), frame, gpu);
            case HEART -> {
                int offset = Math.min(4, heart.pieces()) * 4;
                int y = state.dialogController().boxPosition()
                                == linksawakening.dialog.DialogController.BoxPosition.BOTTOM
                        ? 0x6B - 16
                        : 0x23 - 16;
                for (int half = 0; half < 2; half++) {
                    int attr = heartPairs[offset + half * 2 + 1];
                    OamDrawing.draw8x16(
                            RenderContext.indexed(gpu, frame),
                            gpu,
                            heartPairs[offset + half * 2],
                            0x8E - 8 + half * 8,
                            y,
                            (attr & 32) != 0,
                            (attr & 64) != 0,
                            data.objectPalettes[Math.min(attr & 7, data.objectPalettes.length - 1)]);
                }
            }
        }
    }

    private static final int[][] PAIR_TILES = {{0x40, 0x42}, {0x42, 0x40}, {0x44, 0x46}, {0x48, 0x4A}, {0x48, 0x4C}};
    private static final int SHIELD_TILE = 0x86;

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
