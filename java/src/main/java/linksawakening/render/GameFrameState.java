package linksawakening.render;

import linksawakening.cutscene.CutsceneManager;
import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.DroppableRupeeSystem;
import linksawakening.world.RoomRenderSnapshot;
import linksawakening.world.ScrollController;
import linksawakening.world.TransitionController;

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
    IntroFrameSnapshot introFrameSnapshot
) {
    public static GameFrameState empty() {
        return new GameFrameState(RenderScreen.TITLE, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null, 0, null);
    }

    public GameFrameState withScreen(RenderScreen screen) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes, cutsceneManager,
            room, scrollController, transitionController, link, transientVfxSystem,
            cutLeavesEffectRenderer, transientVfxPalette, droppableRupeeSystem,
            inventoryController, dialogController, frameCounter, introFrameSnapshot);
    }

    public GameFrameState withBackground(int[] tilemap, int[] attrmap, int[][] bgPalettes,
                                         int[][] objPalettes) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withCutsceneManager(CutsceneManager cutsceneManager) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withRoom(RoomRenderSnapshot room, ScrollController scrollController,
                                   TransitionController transitionController) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withLink(Link link) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withTransientVfx(TransientVfxSystem transientVfxSystem,
                                           CutLeavesEffectRenderer cutLeavesEffectRenderer,
                                           int[] transientVfxPalette) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withDroppableRupees(DroppableRupeeSystem droppableRupeeSystem) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withInventoryController(InventoryController inventoryController) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withDialogController(DialogController dialogController) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }

    public GameFrameState withFrameCounter(int frameCounter) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController,
            frameCounter & 0xFF, introFrameSnapshot);
    }

    public GameFrameState withIntroFrameSnapshot(IntroFrameSnapshot introFrameSnapshot) {
        return new GameFrameState(screen, tilemap, attrmap, bgPalettes, objPalettes,
            cutsceneManager, room, scrollController, transitionController, link,
            transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette,
            droppableRupeeSystem, inventoryController, dialogController, frameCounter,
            introFrameSnapshot);
    }
}
