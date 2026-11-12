package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
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
    private final SessionContext context;

    public RenderStateAssembler(SessionContext context) {
        this.context = Objects.requireNonNull(context);
    }

    public GPU gpu() {
        return context.gpu();
    }

    public GameFrameSceneBuilder frameSceneBuilder() {
        return context.frameSceneBuilder();
    }

    public int[] currentTilemap() {
        return context.currentTilemap();
    }

    public int[] currentAttrmap() {
        return context.currentAttrmap();
    }

    public int[][] bgPalettes() {
        return context.bgPalettes();
    }

    public int[][] objPalettes() {
        return context.objPalettes();
    }

    public byte[] indexedDisplayBuffer() {
        return context.indexedDisplayBuffer();
    }

    public RoomSession roomSession() {
        return context.roomSession();
    }

    public TransitionController transitionController() {
        return context.transitionController();
    }

    public int frameCounter() {
        return context.frameCounter();
    }

    public ScrollController scrollController() {
        return context.scrollController();
    }

    public int[] GREEN_OBJECTS_SPRITE_PALETTE() {
        return context.greenObjectsSpritePalette();
    }

    public InventoryMenu inventoryMenu() {
        return context.inventoryMenu();
    }

    public InventoryController inventoryController() {
        return context.inventoryController();
    }

    public DialogController dialogController() {
        return context.dialogController();
    }

    public CutsceneManager cutsceneManager() {
        return context.cutsceneManager();
    }

    public TransientVfxSystem transientVfxSystem() {
        return context.transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return context.droppableRupeeSystem();
    }

    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return context.cutLeavesEffectRenderer();
    }

    public Link link() {
        return context.link();
    }

    public Screen currentScreen() {
        return context.currentScreen();
    }

    public void composeFrameBuffer() {
        if (inventoryMenu() != null && roomSession().rooms().activeRoom() != null) {
            inventoryMenu()
                    .setWorldContext(
                            roomSession().rooms().activeRoom().mapCategory() != Warp.CATEGORY_OVERWORLD,
                            roomSession().rooms().activeRoom().mapId(),
                            roomSession().dungeon().dungeonProgressFlagsSnapshot());
            inventoryMenu()
                    .setDungeonContext(
                            roomSession().dungeon().currentDungeonItemFlagsSnapshot(),
                            roomSession().dungeon().dungeonMinimapRoomStatusSnapshot(),
                            roomSession().rooms().indoorRoomPositionForSave(),
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
                        .withRoom(roomSession().rooms().renderSnapshot(), scrollController(), transitionController())
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
