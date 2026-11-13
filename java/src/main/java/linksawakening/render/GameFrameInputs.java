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
import linksawakening.world.room.RoomRenderView;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;

/**
 * Read-only frame inputs; owned snapshots or same-thread reusable state.
 *
 * <p>Screen-specific optional data: room, Link and world controllers are absent on title/file screens;
 * introFrameSnapshot and fileMenuFrame exist only on their respective screens. Tile/attribute/palette
 * planes may be absent before a scene is loaded. VFX producer/renderer/palette and inventory/dialog
 * controllers may be omitted in focused captures. Layer assembly checks these capabilities before
 * submitting the corresponding pass; absence must not manufacture gameplay state.
 */
interface GameFrameInputs {
    RenderScreen screen();

    int[] tilemap();

    int[] attrmap();

    int[][] bgPalettes();

    int[][] objPalettes();

    CutsceneManager cutsceneManager();

    RoomRenderView room();

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
}
