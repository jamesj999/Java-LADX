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

/** Same-thread borrowed frame inputs. Refill before build; snapshot() explicitly captures ownership. */
public final class GameFrameState implements GameFrameInputs {
    public GameFrameState() {}

    public static GameFrameState empty() {
        return new GameFrameState();
    }

    public GameFrameState(
            RenderScreen screen,
            int[] tilemap,
            int[] attrmap,
            int[][] bgPalettes,
            int[][] objPalettes,
            CutsceneManager cutsceneManager,
            RoomRenderView room,
            ScrollController scrollController,
            TransitionController transitionController,
            Link link,
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            int[] transientVfxPalette,
            DroppableRupeeSystem droppableRupeeSystem,
            InventoryController inventoryController,
            DialogController dialogController,
            int frameCounter,
            IntroFrameSnapshot introFrameSnapshot,
            FileMenuFrameSnapshot fileMenuFrame) {
        this.screen = screen;
        this.tilemap = tilemap;
        this.attrmap = attrmap;
        this.bgPalettes = bgPalettes;
        this.objPalettes = objPalettes;
        this.cutsceneManager = cutsceneManager;
        this.room = room;
        this.scrollController = scrollController;
        this.transitionController = transitionController;
        this.link = link;
        this.transientVfxSystem = transientVfxSystem;
        this.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        this.transientVfxPalette = transientVfxPalette;
        this.droppableRupeeSystem = droppableRupeeSystem;
        this.inventoryController = inventoryController;
        this.dialogController = dialogController;
        this.frameCounter = frameCounter;
        this.introFrameSnapshot = introFrameSnapshot;
        this.fileMenuFrame = fileMenuFrame;
        roomInputs.capture(room);
    }

    private RenderScreen screen = RenderScreen.TITLE;
    private int[] tilemap;
    private int[] attrmap;
    private int[][] bgPalettes;
    private int[][] objPalettes;
    private CutsceneManager cutsceneManager;
    private RoomRenderView room;
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
    final RoomInputs roomInputs = new RoomInputs();

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
    public RoomRenderView room() {
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

    public GameFrameState withScreen(RenderScreen screen) {
        this.screen = screen;
        return this;
    }

    public GameFrameState withBackground(int[] tilemap, int[] attrmap, int[][] bgPalettes, int[][] objPalettes) {
        this.tilemap = tilemap;
        this.attrmap = attrmap;
        this.bgPalettes = bgPalettes;
        this.objPalettes = objPalettes;
        return this;
    }

    public GameFrameState withCutsceneManager(CutsceneManager cutsceneManager) {
        this.cutsceneManager = cutsceneManager;
        return this;
    }

    public GameFrameState withRoom(
            RoomRenderView room, ScrollController scrollController, TransitionController transitionController) {
        this.room = room;
        this.scrollController = scrollController;
        this.transitionController = transitionController;
        roomInputs.capture(room);
        return this;
    }

    public GameFrameState withLink(Link link) {
        this.link = link;
        return this;
    }

    public GameFrameState withTransientVfx(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            int[] transientVfxPalette) {
        this.transientVfxSystem = transientVfxSystem;
        this.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        this.transientVfxPalette = transientVfxPalette;
        return this;
    }

    public GameFrameState withDroppableRupees(DroppableRupeeSystem droppableRupeeSystem) {
        this.droppableRupeeSystem = droppableRupeeSystem;
        return this;
    }

    public GameFrameState withInventoryController(InventoryController inventoryController) {
        this.inventoryController = inventoryController;
        return this;
    }

    public GameFrameState withDialogController(DialogController dialogController) {
        this.dialogController = dialogController;
        return this;
    }

    public GameFrameState withFrameCounter(int frameCounter) {
        this.frameCounter = frameCounter & 0xFF;
        return this;
    }

    public GameFrameState withIntroFrameSnapshot(IntroFrameSnapshot introFrameSnapshot) {
        this.introFrameSnapshot = introFrameSnapshot;
        return this;
    }

    public GameFrameState withFileMenuFrame(FileMenuFrameSnapshot fileMenuFrame) {
        this.fileMenuFrame = fileMenuFrame;
        return this;
    }

    /** Restores empty-input defaults without allocation before a complete refill. */
    public GameFrameState reset() {
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

    public GameFrameState copyFrom(GameFrameState source) {
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
    public GameFrameState withRoomData(
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

    public GameFrameState withRoomControllers(ScrollController scroll, TransitionController transition) {
        scrollController = scroll;
        transitionController = transition;
        return this;
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof GameFrameState other)) return false;
        return java.util.Objects.equals(screen, other.screen)
                && java.util.Objects.equals(tilemap, other.tilemap)
                && java.util.Objects.equals(attrmap, other.attrmap)
                && java.util.Objects.equals(bgPalettes, other.bgPalettes)
                && java.util.Objects.equals(objPalettes, other.objPalettes)
                && java.util.Objects.equals(cutsceneManager, other.cutsceneManager)
                && java.util.Objects.equals(room, other.room)
                && java.util.Objects.equals(scrollController, other.scrollController)
                && java.util.Objects.equals(transitionController, other.transitionController)
                && java.util.Objects.equals(link, other.link)
                && java.util.Objects.equals(transientVfxSystem, other.transientVfxSystem)
                && java.util.Objects.equals(cutLeavesEffectRenderer, other.cutLeavesEffectRenderer)
                && java.util.Objects.equals(transientVfxPalette, other.transientVfxPalette)
                && java.util.Objects.equals(droppableRupeeSystem, other.droppableRupeeSystem)
                && java.util.Objects.equals(inventoryController, other.inventoryController)
                && java.util.Objects.equals(dialogController, other.dialogController)
                && frameCounter == other.frameCounter
                && java.util.Objects.equals(introFrameSnapshot, other.introFrameSnapshot)
                && java.util.Objects.equals(fileMenuFrame, other.fileMenuFrame);
    }

    @Override
    public int hashCode() {
        return frameCounter;
    }
}
