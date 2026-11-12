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

/** Immutable scene-input updates shared by the frame-state record. */
interface GameFrameSceneUpdates {
    RenderScreen screen();

    int[] tilemap();

    int[] attrmap();

    int[][] bgPalettes();

    int[][] objPalettes();

    CutsceneManager cutsceneManager();

    RoomRenderSnapshot room();

    ScrollController scrollController();

    TransitionController transitionController();

    Link link();

    TransientVfxSystem transientVfxSystem();

    CutLeavesEffectRenderer cutLeavesEffectRenderer();

    int[] transientVfxPalette();

    DroppableRupeeSystem droppableRupeeSystem();

    InventoryController inventoryController();

    DialogController dialogController();

    int frameCounter();

    IntroFrameSnapshot introFrameSnapshot();

    FileMenuFrameSnapshot fileMenuFrame();

    default GameFrameState withScreen(RenderScreen screen) {
        return new GameFrameState(
                screen,
                tilemap(),
                attrmap(),
                bgPalettes(),
                objPalettes(),
                cutsceneManager(),
                room(),
                scrollController(),
                transitionController(),
                link(),
                transientVfxSystem(),
                cutLeavesEffectRenderer(),
                transientVfxPalette(),
                droppableRupeeSystem(),
                inventoryController(),
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withBackground(int[] tilemap, int[] attrmap, int[][] bgPalettes, int[][] objPalettes) {
        return new GameFrameState(
                screen(),
                tilemap,
                attrmap,
                bgPalettes,
                objPalettes,
                cutsceneManager(),
                room(),
                scrollController(),
                transitionController(),
                link(),
                transientVfxSystem(),
                cutLeavesEffectRenderer(),
                transientVfxPalette(),
                droppableRupeeSystem(),
                inventoryController(),
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withCutsceneManager(CutsceneManager cutsceneManager) {
        return new GameFrameState(
                screen(),
                tilemap(),
                attrmap(),
                bgPalettes(),
                objPalettes(),
                cutsceneManager,
                room(),
                scrollController(),
                transitionController(),
                link(),
                transientVfxSystem(),
                cutLeavesEffectRenderer(),
                transientVfxPalette(),
                droppableRupeeSystem(),
                inventoryController(),
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withRoom(
            RoomRenderSnapshot room, ScrollController scrollController, TransitionController transitionController) {
        return new GameFrameState(
                screen(),
                tilemap(),
                attrmap(),
                bgPalettes(),
                objPalettes(),
                cutsceneManager(),
                room,
                scrollController,
                transitionController,
                link(),
                transientVfxSystem(),
                cutLeavesEffectRenderer(),
                transientVfxPalette(),
                droppableRupeeSystem(),
                inventoryController(),
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }
}
