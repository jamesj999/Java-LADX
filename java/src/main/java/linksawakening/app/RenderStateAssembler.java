package linksawakening.app;



import linksawakening.app.screen.Screen;

import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.ui.InventoryMenu;
import linksawakening.ui.InventoryController;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

import static linksawakening.world.room.RoomConstants.*;

public record RenderStateAssembler(
        GPU gpu,
        GameFrameSceneBuilder frameSceneBuilder,
        int[] currentTilemap,
        int[] currentAttrmap,
        int[][] bgPalettes,
        int[][] objPalettes,
        byte[] indexedDisplayBuffer,
        RoomSession roomSession,
        TransitionController transitionController,
        int frameCounter,
        ScrollController scrollController,
        int[] GREEN_OBJECTS_SPRITE_PALETTE,
        InventoryMenu inventoryMenu,
        InventoryController inventoryController,
        DialogController dialogController,
        CutsceneManager cutsceneManager,
        TransientVfxSystem transientVfxSystem,
        DroppableRupeeSystem droppableRupeeSystem,
        CutLeavesEffectRenderer cutLeavesEffectRenderer,
        Link link,
        Screen currentScreen) {
    public void composeFrameBuffer() {
        if (inventoryMenu != null && roomSession != null && roomSession.activeRoom() != null) {
            inventoryMenu.setWorldContext(roomSession.activeRoom().mapCategory() != Warp.CATEGORY_OVERWORLD,
                roomSession.activeRoom().mapId(), roomSession.dungeonProgressFlagsSnapshot());
            inventoryMenu.setDungeonContext(roomSession.currentDungeonItemFlagsSnapshot(),
                roomSession.dungeonMinimapRoomStatusSnapshot(), roomSession.indoorRoomPositionForSave(), frameCounter);
        }
        frameSceneBuilder.build(currentGameFrameState()).drawTo(indexedDisplayBuffer, gpu);
    }
    public GameFrameState currentGameFrameState() {
        return currentScreen.renderState(GameFrameState.empty()
            .withScreen(currentRenderScreen())
            .withBackground(currentTilemap, currentAttrmap, bgPalettes, objPalettes)
            .withCutsceneManager(cutsceneManager)
            .withIntroFrameSnapshot(cutsceneManager == null ? null : cutsceneManager.frameSnapshot())
            .withRoom(roomSession == null ? null : roomSession.renderSnapshot(), scrollController, transitionController)
            .withLink(link)
            .withTransientVfx(transientVfxSystem, cutLeavesEffectRenderer, GREEN_OBJECTS_SPRITE_PALETTE)
            .withDroppableRupees(droppableRupeeSystem)
            .withInventoryController(inventoryController)
            .withDialogController(dialogController)
            .withFrameCounter(frameCounter));
    }
    public RenderScreen currentRenderScreen() {
        return renderScreenFor(currentScreen);
    }
    public RenderScreen renderScreenFor(Screen screen) {
        return screen.renderScreen();
    }
}
