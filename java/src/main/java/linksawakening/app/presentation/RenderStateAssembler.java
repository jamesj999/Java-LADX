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
import linksawakening.render.ReusableGameFrameState;
import linksawakening.world.transition.Warp;

/**
 * Assembles current scene and gameplay state for the shared renderer. One instance is retained for the
 * owning session.
 */
public final class RenderStateAssembler {
    private final ReusableGameFrameState reusableState = new ReusableGameFrameState();
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
        if (world.roomSession().rooms().activeRoom() != null) {
            world.roomSession().dungeon().view().copyTo(dungeonProgress, dungeonItems, dungeonMinimap);
            ui.inventoryMenu()
                    .setWorldContext(
                            world.roomSession().rooms().activeRoom().mapCategory() != Warp.CATEGORY_OVERWORLD,
                            world.roomSession().rooms().activeRoom().mapId(),
                            dungeonProgress);
            ui.inventoryMenu()
                    .setDungeonContext(
                            dungeonItems,
                            dungeonMinimap,
                            world.roomSession().rooms().indoorRoomPositionForSave(),
                            state.clock().value());
        }
        frameSceneBuilder.build(currentReusableGameFrameState()).drawTo(visual.indexedDisplayBuffer(), rom.gpu());
    }

    /** Borrowed inputs, valid only until this assembler's next refill and next simulation tick. */
    public ReusableGameFrameState currentReusableGameFrameState() {
        var intro = ui.cutsceneManager().frameSnapshot();
        reusableState
                .reset()
                .withScreen(currentRenderScreen())
                .withBackground(
                        state.scene().currentTilemap,
                        state.scene().currentAttrmap,
                        state.scene().bgPalettes,
                        state.scene().objPalettes)
                .withCutsceneManager(ui.cutsceneManager())
                .withRoom(
                        world.roomSession().rooms().renderView(),
                        world.scrollController(),
                        world.transitionController())
                .withLink(player.link())
                .withTransientVfx(
                        visual.transientVfxSystem(), visual.cutLeavesEffectRenderer(), greenObjectsSpritePalette)
                .withDroppableRupees(visual.droppableRupeeSystem())
                .withInventoryController(ui.inventoryController())
                .withDialogController(ui.dialogController())
                .withFrameCounter(state.clock().value())
                .withIntroFrameSnapshot(intro);
        return state.screens().currentScreen.renderState(reusableState);
    }

    public GameFrameState currentGameFrameState() {
        var intro = ui.cutsceneManager().frameSnapshot();
        return state.screens()
                .currentScreen
                .renderState(new GameFrameState(
                        currentRenderScreen(),
                        state.scene().currentTilemap,
                        state.scene().currentAttrmap,
                        state.scene().bgPalettes,
                        state.scene().objPalettes,
                        ui.cutsceneManager(),
                        world.roomSession().rooms().renderSnapshot(),
                        world.scrollController(),
                        world.transitionController(),
                        player.link(),
                        visual.transientVfxSystem(),
                        visual.cutLeavesEffectRenderer(),
                        greenObjectsSpritePalette,
                        visual.droppableRupeeSystem(),
                        ui.inventoryController(),
                        ui.dialogController(),
                        state.clock().value(),
                        intro,
                        null));
    }

    public RenderScreen currentRenderScreen() {
        return renderScreenFor(state.screens().currentScreen);
    }

    public RenderScreen renderScreenFor(Screen screen) {
        return screen.renderScreen();
    }
}
