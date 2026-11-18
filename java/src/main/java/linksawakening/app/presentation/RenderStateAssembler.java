package linksawakening.app.presentation;

import java.util.Objects;
import linksawakening.app.screen.Screen;
import linksawakening.app.services.PlayerServices;
import linksawakening.app.services.RomServices;
import linksawakening.app.services.UiServices;
import linksawakening.app.services.VisualServices;
import linksawakening.app.services.WorldServices;
import linksawakening.app.state.SessionState;
import linksawakening.render.GameFrameSceneBuilder;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.world.transition.Warp;

/**
 * Assembles current scene and gameplay state for the shared renderer. One instance is retained for the
 * owning session.
 */
public final class RenderStateAssembler {
    private final GameFrameState inputs = new GameFrameState();
    private final byte[] dungeonProgress = new byte[8], dungeonItems = new byte[5], dungeonMinimap = new byte[64];
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

    public void composeFrameBuffer() {
        if (world.roomSession().hasRoom()) {
            var room = world.roomSession().rooms().requireActiveRoom();
            world.roomSession().dungeon().view().copyTo(dungeonProgress, dungeonItems, dungeonMinimap);
            ui.inventoryMenu()
                    .setWorldContext(room.mapCategory() != Warp.CATEGORY_OVERWORLD, room.mapId(), dungeonProgress);
            ui.inventoryMenu()
                    .setDungeonContext(
                            dungeonItems,
                            dungeonMinimap,
                            world.roomSession().rooms().indoorRoomPositionForSave(),
                            state.clock().value());
        }
        byte[] buffer = visual.indexedDisplayBuffer();
        Runnable draw = () -> frameSceneBuilder.build(currentGameFrameState()).drawTo(buffer, rom.gpu());
        var load = state.screens().fileLoad;
        if (load != null && load.beforeWorld() && !load.showSelection()) {
            java.util.Arrays.fill(buffer, (byte) 0xFF);
        } else if (state.screens().menuTransition != null) {
            state.screens().menuTransition.render(buffer, draw);
        } else {
            draw.run();
            if (load != null && !load.beforeWorld()) load.fadeRoom(buffer);
        }
    }

    /** Borrowed inputs, valid only until this assembler's next refill and next simulation tick. */
    public GameFrameState currentGameFrameState() {
        RenderScreen screen = currentRenderScreen();
        inputs.reset()
                .withScreen(screen)
                .withFrameCounter(state.clock().value())
                .withDialogController(ui.dialogController());
        switch (screen) {
            case OVERWORLD -> fillOverworld();
            case TITLE, CUTSCENE -> fillBackground();
            case FILE_MENU, FILE_SAVE, WORLD_MAP -> {
                /* The screen publishes its menu frame below. */
            }
        }
        if (screen != RenderScreen.OVERWORLD
                && screen != RenderScreen.WORLD_MAP
                && ui.dialogController().isActive()) fillRoomView();
        return state.screens().currentScreen().renderState(inputs);
    }

    public RenderScreen currentRenderScreen() {
        return renderScreenFor(state.screens().currentScreen());
    }

    public RenderScreen renderScreenFor(Screen screen) {
        return screen.renderScreen();
    }

    private void fillRoomView() {
        inputs.withRoom(
                world.roomSession().rooms().renderViewOrNull(), world.scrollController(), world.transitionController());
    }

    private void fillOverworld() {
        fillRoomView();
        inputs.withLink(player.link())
                .withTransientVfx(
                        visual.transientVfxSystem(), visual.cutLeavesEffectRenderer(), greenObjectsSpritePalette)
                .withDroppableRupees(visual.droppableRupeeSystem())
                .withInventoryController(ui.inventoryController());
    }

    private void fillBackground() {
        inputs.withBackground(
                        state.scene().currentTilemap(),
                        state.scene().currentAttrmap(),
                        state.scene().bgPalettes(),
                        state.scene().objPalettes())
                .withCutsceneManager(ui.cutsceneManager())
                .withIntroFrameSnapshot(ui.cutsceneManager().frameSnapshot());
    }
}
