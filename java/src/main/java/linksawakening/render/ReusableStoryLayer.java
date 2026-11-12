package linksawakening.render;

import linksawakening.gpu.GPU;
import linksawakening.world.entity.event.HeartPieceVisual;

/** Retains one story-pose role and immutable heart-piece OAM metadata across frames. */
final class ReusableStoryLayer implements HardwareRenderLayer {
    enum Kind {
        SHADOW,
        GRASS,
        MARIN,
        TARIN,
        GOT_ITEM,
        HEART
    }

    private final Kind kind;
    private ReusableGameFrameState state;
    private HeartPieceVisual heart;
    private int[] heartPairs;

    ReusableStoryLayer(Kind kind) {
        this.kind = kind;
    }

    void configure(ReusableGameFrameState state) {
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
            case SHADOW -> StorySprites.submitLinkShadow(
                    state.link(), data.objectPalettes[0], state.scrollController(), state.frameCounter(), frame, gpu);
            case GRASS -> StorySprites.submitLinkGrass(
                    state.link(), data.objectPalettes, state.scrollController(), data.grassPalette, frame, gpu);
            case MARIN -> StorySprites.submitMarinBed(
                    state.link(),
                    data.entities.spriteTiles(),
                    data.objectPalettes,
                    state.scrollController(),
                    frame,
                    gpu);
            case TARIN -> StorySprites.submitTarinShield(state.link(), state.scrollController(), frame, gpu);
            case GOT_ITEM -> StorySprites.submitGotItem(
                    state.link(), data.objectPalettes, state.scrollController(), frame, gpu);
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
}
