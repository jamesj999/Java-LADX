package linksawakening.render;

import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomRenderSnapshot;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/** Immutable snapshot of inputs used to build a scene for the current frame. */
public record GameFrameState(
        RenderScreen screen,
        int[] tilemap,
        int[] attrmap,
        int[][] bgPalettes,
        int[][] objPalettes,
        CutsceneManager cutsceneManager,
        RoomRenderSnapshot room,
        ScrollController scrollController,
        TransitionController transitionController,
        Link link,
        TransientVfxSystem transientVfxSystem,
        CutLeavesEffectRenderer cutLeavesEffectRenderer,
        int[] transientVfxPalette,
        DroppableRupeeSystem droppableRupeeSystem,
        InventoryController inventoryController,
        DialogController dialogController,
        int frameCounter,
        IntroFrameSnapshot introFrameSnapshot,
        FileMenuFrameSnapshot fileMenuFrame)
        implements GameFrameStateUpdates {
    private static final GameFrameState EMPTY = new GameFrameState(
            RenderScreen.TITLE,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            0,
            null,
            null);

    public static GameFrameState empty() {
        return EMPTY;
    }
}
