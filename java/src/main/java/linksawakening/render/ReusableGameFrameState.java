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

/** Same-thread borrowed frame inputs. Refill before build; snapshot() explicitly captures ownership. */
public final class ReusableGameFrameState implements GameFrameInputs {
    private RenderScreen screen = RenderScreen.TITLE;
    private int[] tilemap;
    private int[] attrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private CutsceneManager cutsceneManager;
    private RoomRenderSnapshot room;
    private ScrollController scrollController;
    private TransitionController transitionController;
    private Link link;
    private TransientVfxSystem transientVfxSystem;
    private CutLeavesEffectRenderer cutLeavesEffectRenderer;
    private int[] transientVfxPalette;
    private DroppableRupeeSystem droppableRupeeSystem;
    private InventoryController inventoryController;
    private DialogController dialogController;
    private int frameCounter;
    private IntroFrameSnapshot introFrameSnapshot;
    private FileMenuFrameSnapshot fileMenuFrame;
    final ReusableRoomInputs roomInputs = new ReusableRoomInputs();

    @Override
    public RenderScreen screen() {
        return screen;
    }

    @Override
    public int[] tilemap() {
        return tilemap;
    }

    @Override
    public int[] attrmap() {
        return attrmap;
    }

    @Override
    public int[][] bgPalettes() {
        return bgPalettes;
    }

    @Override
    public int[][] objPalettes() {
        return objPalettes;
    }

    @Override
    public CutsceneManager cutsceneManager() {
        return cutsceneManager;
    }

    @Override
    public RoomRenderSnapshot room() {
        return room;
    }

    @Override
    public ScrollController scrollController() {
        return scrollController;
    }

    @Override
    public TransitionController transitionController() {
        return transitionController;
    }

    @Override
    public Link link() {
        return link;
    }

    @Override
    public TransientVfxSystem transientVfxSystem() {
        return transientVfxSystem;
    }

    @Override
    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return cutLeavesEffectRenderer;
    }

    @Override
    public int[] transientVfxPalette() {
        return transientVfxPalette;
    }

    @Override
    public DroppableRupeeSystem droppableRupeeSystem() {
        return droppableRupeeSystem;
    }

    @Override
    public InventoryController inventoryController() {
        return inventoryController;
    }

    @Override
    public DialogController dialogController() {
        return dialogController;
    }

    @Override
    public int frameCounter() {
        return frameCounter;
    }

    @Override
    public IntroFrameSnapshot introFrameSnapshot() {
        return introFrameSnapshot;
    }

    @Override
    public FileMenuFrameSnapshot fileMenuFrame() {
        return fileMenuFrame;
    }

    public ReusableGameFrameState withScreen(RenderScreen screen) {
        this.screen = screen;
        return this;
    }

    public ReusableGameFrameState withBackground(
            int[] tilemap, int[] attrmap, int[][] bgPalettes, int[][] objPalettes) {
        this.tilemap = tilemap;
        this.attrmap = attrmap;
        this.bgPalettes = bgPalettes;
        this.objPalettes = objPalettes;
        return this;
    }

    public ReusableGameFrameState withCutsceneManager(CutsceneManager cutsceneManager) {
        this.cutsceneManager = cutsceneManager;
        return this;
    }

    public ReusableGameFrameState withRoom(
            RoomRenderSnapshot room, ScrollController scrollController, TransitionController transitionController) {
        this.room = room;
        this.scrollController = scrollController;
        this.transitionController = transitionController;
        roomInputs.capture(room);
        return this;
    }

    public ReusableGameFrameState withLink(Link link) {
        this.link = link;
        return this;
    }

    public ReusableGameFrameState withTransientVfx(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            int[] transientVfxPalette) {
        this.transientVfxSystem = transientVfxSystem;
        this.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        this.transientVfxPalette = transientVfxPalette;
        return this;
    }

    public ReusableGameFrameState withDroppableRupees(DroppableRupeeSystem droppableRupeeSystem) {
        this.droppableRupeeSystem = droppableRupeeSystem;
        return this;
    }

    public ReusableGameFrameState withInventoryController(InventoryController inventoryController) {
        this.inventoryController = inventoryController;
        return this;
    }

    public ReusableGameFrameState withDialogController(DialogController dialogController) {
        this.dialogController = dialogController;
        return this;
    }

    public ReusableGameFrameState withFrameCounter(int frameCounter) {
        this.frameCounter = frameCounter & 0xFF;
        return this;
    }

    public ReusableGameFrameState withIntroFrameSnapshot(IntroFrameSnapshot introFrameSnapshot) {
        this.introFrameSnapshot = introFrameSnapshot;
        return this;
    }

    public ReusableGameFrameState withFileMenuFrame(FileMenuFrameSnapshot fileMenuFrame) {
        this.fileMenuFrame = fileMenuFrame;
        return this;
    }

    /** Restores empty-input defaults without allocation before a complete refill. */
    public ReusableGameFrameState reset() {
        screen = RenderScreen.TITLE;
        tilemap = null;
        attrmap = null;
        bgPalettes = null;
        objPalettes = null;
        cutsceneManager = null;
        room = null;
        scrollController = null;
        transitionController = null;
        link = null;
        transientVfxSystem = null;
        cutLeavesEffectRenderer = null;
        transientVfxPalette = null;
        droppableRupeeSystem = null;
        inventoryController = null;
        dialogController = null;
        frameCounter = 0;
        introFrameSnapshot = null;
        fileMenuFrame = null;
        roomInputs.capture(null);
        return this;
    }

    public ReusableGameFrameState copyFrom(GameFrameState source) {
        this.screen = source.screen();
        this.tilemap = source.tilemap();
        this.attrmap = source.attrmap();
        this.bgPalettes = source.bgPalettes();
        this.objPalettes = source.objPalettes();
        this.cutsceneManager = source.cutsceneManager();
        this.room = source.room();
        this.scrollController = source.scrollController();
        this.transitionController = source.transitionController();
        this.link = source.link();
        this.transientVfxSystem = source.transientVfxSystem();
        this.cutLeavesEffectRenderer = source.cutLeavesEffectRenderer();
        this.transientVfxPalette = source.transientVfxPalette();
        this.droppableRupeeSystem = source.droppableRupeeSystem();
        this.inventoryController = source.inventoryController();
        this.dialogController = source.dialogController();
        this.frameCounter = source.frameCounter();
        this.introFrameSnapshot = source.introFrameSnapshot();
        this.fileMenuFrame = source.fileMenuFrame();
        roomInputs.capture(room);
        return this;
    }
    /** Borrows current room arrays/view without invoking defensive snapshot getters. The caller keeps these inputs valid until the scene has been drawn. */
    public ReusableGameFrameState withRoomData(
            int[] tiles,
            int[] attributes,
            int[][] palettes,
            linksawakening.world.entity.RoomEntitySnapshot entities,
            linksawakening.world.entity.event.InstrumentWarpVisual warp,
            linksawakening.world.entity.event.HeartPieceVisual heart,
            int grassPalette) {
        room = null;
        roomInputs.set(tiles, attributes, palettes, entities, warp, heart, grassPalette);
        return this;
    }

    public ReusableGameFrameState withRoomControllers(ScrollController scroll, TransitionController transition) {
        scrollController = scroll;
        transitionController = transition;
        return this;
    }
    /** Explicit owned form for capture tools; ordinary rendering uses this mutable object directly. */
    public GameFrameState snapshot() {
        return new GameFrameState(
                screen,
                tilemap,
                attrmap,
                bgPalettes,
                objPalettes,
                cutsceneManager,
                roomInputs.snapshot(),
                scrollController,
                transitionController,
                link,
                transientVfxSystem,
                cutLeavesEffectRenderer,
                transientVfxPalette,
                droppableRupeeSystem,
                inventoryController,
                dialogController,
                frameCounter,
                introFrameSnapshot,
                fileMenuFrame);
    }
}
