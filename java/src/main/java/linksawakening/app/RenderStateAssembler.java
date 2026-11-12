package linksawakening.app;

import static linksawakening.world.room.RoomConstants.*;

import java.util.Objects;
import linksawakening.app.debug.*;
import linksawakening.app.gameplay.*;
import linksawakening.app.host.*;
import linksawakening.app.screen.*;
import linksawakening.app.screen.Screen;
import linksawakening.app.story.*;
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
import linksawakening.world.transition.*;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

public final class RenderStateAssembler {
    private final GameFrameSceneBuilder frameSceneBuilder = new GameFrameSceneBuilder();
    private final int[] greenObjectsSpritePalette = {0, 0, 0x0010A840, 0x00F8B888};
    private final RomServices rom;
    private final WorldServices world;
    private final PlayerServices player;
    private final UiServices ui;
    private final VisualServices visual;
    private final SessionState state;

    public RenderStateAssembler(
            RomServices rom,
            WorldServices world,
            PlayerServices player,
            UiServices ui,
            VisualServices visual,
            SessionState state) {
        this.rom = Objects.requireNonNull(rom);
        this.world = Objects.requireNonNull(world);
        this.player = Objects.requireNonNull(player);
        this.ui = Objects.requireNonNull(ui);
        this.visual = Objects.requireNonNull(visual);
        this.state = Objects.requireNonNull(state);
    }

    public GPU gpu() {
        return rom.gpu();
    }

    public GameFrameSceneBuilder frameSceneBuilder() {
        return frameSceneBuilder;
    }

    public int[] currentTilemap() {
        return state.scene().currentTilemap;
    }

    public int[] currentAttrmap() {
        return state.scene().currentAttrmap;
    }

    public int[][] bgPalettes() {
        return state.scene().bgPalettes;
    }

    public int[][] objPalettes() {
        return state.scene().objPalettes;
    }

    public byte[] indexedDisplayBuffer() {
        return visual.indexedDisplayBuffer();
    }

    public RoomSession roomSession() {
        return world.roomSession();
    }

    public TransitionController transitionController() {
        return world.transitionController();
    }

    public int frameCounter() {
        return state.clock().value();
    }

    public ScrollController scrollController() {
        return world.scrollController();
    }

    public int[] GREEN_OBJECTS_SPRITE_PALETTE() {
        return greenObjectsSpritePalette;
    }

    public InventoryMenu inventoryMenu() {
        return ui.inventoryMenu();
    }

    public InventoryController inventoryController() {
        return ui.inventoryController();
    }

    public DialogController dialogController() {
        return ui.dialogController();
    }

    public CutsceneManager cutsceneManager() {
        return ui.cutsceneManager();
    }

    public TransientVfxSystem transientVfxSystem() {
        return visual.transientVfxSystem();
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return visual.droppableRupeeSystem();
    }

    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return visual.cutLeavesEffectRenderer();
    }

    public Link link() {
        return player.link();
    }

    public Screen currentScreen() {
        return state.screens().currentScreen;
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
