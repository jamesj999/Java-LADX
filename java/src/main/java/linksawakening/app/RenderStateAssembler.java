package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import linksawakening.app.screen.Screen;
import linksawakening.cutscene.CutsceneManager;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.gpu.GPU;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.ui.InventoryController;
import linksawakening.ui.InventoryMenu;
import linksawakening.vfx.CutLeavesEffectRenderer;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.world.room.DroppableRupeeSystem;
import linksawakening.world.room.RoomSession;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

public final class RenderStateAssembler {
    private final linksawakening.app.SessionContext context;

    public RenderStateAssembler(linksawakening.app.SessionContext context) {
        this.context = java.util.Objects.requireNonNull(context);
    }

    public GPU gpu() {
        return context.renderer_gpu();
    }

    public GameFrameSceneBuilder frameSceneBuilder() {
        return context.renderer_frameSceneBuilder();
    }

    public int[] currentTilemap() {
        return context.renderer_currentTilemap();
    }

    public int[] currentAttrmap() {
        return context.renderer_currentAttrmap();
    }

    public int[][] bgPalettes() {
        return context.renderer_bgPalettes();
    }

    public int[][] objPalettes() {
        return context.renderer_objPalettes();
    }

    public byte[] indexedDisplayBuffer() {
        return context.renderer_indexedDisplayBuffer();
    }

    public RoomSession roomSession() {
        return context.renderer_roomSession();
    }

    public TransitionController transitionController() {
        return context.renderer_transitionController();
    }

    public int frameCounter() {
        return context.renderer_frameCounter();
    }

    public ScrollController scrollController() {
        return context.renderer_scrollController();
    }

    public int[] GREEN_OBJECTS_SPRITE_PALETTE() {
        return context.renderer_GREEN_OBJECTS_SPRITE_PALETTE();
    }

    public InventoryMenu inventoryMenu() {
        return context.renderer_inventoryMenu();
    }

    public InventoryController inventoryController() {
        return context.renderer_inventoryController();
    }

    public DialogController dialogController() {
        return context.renderer_dialogController();
    }

    public CutsceneManager cutsceneManager() {
        return context.renderer_cutsceneManager();
    }

    public TransientVfxSystem transientVfxSystem() {
        return context.renderer_transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return context.renderer_droppableRupeeSystem();
    }

    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return context.renderer_cutLeavesEffectRenderer();
    }

    public Link link() {
        return context.renderer_link();
    }

    public Screen currentScreen() {
        return context.renderer_currentScreen();
    }

    public void composeFrameBuffer() {
        if (inventoryMenu() != null && roomSession() != null && roomSession().activeRoom() != null) {
            inventoryMenu()
                    .setWorldContext(
                            roomSession().activeRoom().mapCategory() != Warp.CATEGORY_OVERWORLD,
                            roomSession().activeRoom().mapId(),
                            roomSession().dungeonProgressFlagsSnapshot());
            inventoryMenu()
                    .setDungeonContext(
                            roomSession().currentDungeonItemFlagsSnapshot(),
                            roomSession().dungeonMinimapRoomStatusSnapshot(),
                            roomSession().indoorRoomPositionForSave(),
                            frameCounter());
        }
        frameSceneBuilder().build(currentGameFrameState()).drawTo(indexedDisplayBuffer(), gpu());
    }

    public GameFrameState currentGameFrameState() {
        return currentScreen()
                .renderState(GameFrameState.empty()
                        .withScreen(currentRenderScreen())
                        .withBackground(currentTilemap(), currentAttrmap(), bgPalettes(), objPalettes())
                        .withCutsceneManager(cutsceneManager())
                        .withIntroFrameSnapshot(
                                cutsceneManager() == null
                                        ? null
                                        : cutsceneManager().frameSnapshot())
                        .withRoom(
                                roomSession() == null ? null : roomSession().renderSnapshot(),
                                scrollController(),
                                transitionController())
                        .withLink(link())
                        .withTransientVfx(
                                transientVfxSystem(), cutLeavesEffectRenderer(), GREEN_OBJECTS_SPRITE_PALETTE())
                        .withDroppableRupees(droppableRupeeSystem())
                        .withInventoryController(inventoryController())
                        .withDialogController(dialogController())
                        .withFrameCounter(frameCounter()));
    }

    public RenderScreen currentRenderScreen() {
        return renderScreenFor(currentScreen());
    }

    public RenderScreen renderScreenFor(Screen screen) {
        return screen.renderScreen();
    }
}
