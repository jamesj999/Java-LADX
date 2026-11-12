package linksawakening.render;

import linksawakening.cutscene.IntroFrameSnapshot;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.ui.FileMenuFrameSnapshot;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;

/** Immutable gameplay and presentation updates shared by the frame-state record. */
interface GameFrameStateUpdates extends GameFrameSceneUpdates {
    default GameFrameState withLink(Link link) {
        return new GameFrameState(
                screen(),
                tilemap(),
                attrmap(),
                bgPalettes(),
                objPalettes(),
                cutsceneManager(),
                room(),
                scrollController(),
                transitionController(),
                link,
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

    default GameFrameState withTransientVfx(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            int[] transientVfxPalette) {
        return new GameFrameState(
                screen(),
                tilemap(),
                attrmap(),
                bgPalettes(),
                objPalettes(),
                cutsceneManager(),
                room(),
                scrollController(),
                transitionController(),
                link(),
                transientVfxSystem,
                cutLeavesEffectRenderer,
                transientVfxPalette,
                droppableRupeeSystem(),
                inventoryController(),
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withDroppableRupees(DroppableRupeeSystem droppableRupeeSystem) {
        return new GameFrameState(
                screen(),
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
                droppableRupeeSystem,
                inventoryController(),
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withInventoryController(InventoryController inventoryController) {
        return new GameFrameState(
                screen(),
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
                inventoryController,
                dialogController(),
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withDialogController(DialogController dialogController) {
        return new GameFrameState(
                screen(),
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
                dialogController,
                frameCounter(),
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withFrameCounter(int frameCounter) {
        return new GameFrameState(
                screen(),
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
                frameCounter & 0xFF,
                introFrameSnapshot(),
                fileMenuFrame());
    }

    default GameFrameState withIntroFrameSnapshot(IntroFrameSnapshot introFrameSnapshot) {
        return new GameFrameState(
                screen(),
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
                introFrameSnapshot,
                fileMenuFrame());
    }

    default GameFrameState withFileMenuFrame(FileMenuFrameSnapshot fileMenuFrame) {
        return new GameFrameState(
                screen(),
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
                fileMenuFrame);
    }
}
