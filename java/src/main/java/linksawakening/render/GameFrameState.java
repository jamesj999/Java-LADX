package linksawakening.render;

import java.util.Objects;
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
        withScreen(screen)
                .withBackground(tilemap, attrmap, bgPalettes, objPalettes)
                .withCutsceneManager(cutsceneManager)
                .withRoom(room, scrollController, transitionController)
                .withLink(link)
                .withTransientVfx(transientVfxSystem, cutLeavesEffectRenderer, transientVfxPalette)
                .withDroppableRupees(droppableRupeeSystem)
                .withInventoryController(inventoryController)
                .withDialogController(dialogController)
                .withFrameCounter(frameCounter)
                .withIntroFrameSnapshot(introFrameSnapshot)
                .withFileMenuFrame(fileMenuFrame);
    }

    public final OverworldInputs overworld = new OverworldInputs();
    /** Always-present overworld section; presence denotes intentionally omitted presentation components. */
    public static final class OverworldInputs {
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
        private boolean roomPresent;
        private boolean scrollControllerPresent;
        private boolean transitionControllerPresent;
        private boolean linkPresent;
        private boolean transientVfxSystemPresent;
        private boolean cutLeavesEffectRendererPresent;
        private boolean transientVfxPalettePresent;
        private boolean droppableRupeeSystemPresent;
        private boolean inventoryControllerPresent;
        private boolean dialogControllerPresent;

        public boolean hasRoom() {
            return roomPresent;
        }

        public boolean hasScrollController() {
            return scrollControllerPresent;
        }

        public boolean hasTransitionController() {
            return transitionControllerPresent;
        }

        public boolean hasLink() {
            return linkPresent;
        }

        public boolean hasTransientVfxSystem() {
            return transientVfxSystemPresent;
        }

        public boolean hasCutLeavesEffectRenderer() {
            return cutLeavesEffectRendererPresent;
        }

        public boolean hasTransientVfxPalette() {
            return transientVfxPalettePresent;
        }

        public boolean hasDroppableRupeeSystem() {
            return droppableRupeeSystemPresent;
        }

        public boolean hasInventoryController() {
            return inventoryControllerPresent;
        }

        public boolean hasDialogController() {
            return dialogControllerPresent;
        }
    }

    public final BackgroundInputs background = new BackgroundInputs();
    /** Always-present background section; presence denotes intentionally omitted presentation components. */
    public static final class BackgroundInputs {
        private int[] tilemap;
        private int[] attrmap;
        private int[][] bgPalettes;
        private int[][] objPalettes;
        private CutsceneManager cutsceneManager;
        private IntroFrameSnapshot introFrameSnapshot;
        private FileMenuFrameSnapshot fileMenuFrame;
        private boolean tilemapPresent;
        private boolean attrmapPresent;
        private boolean bgPalettesPresent;
        private boolean objPalettesPresent;
        private boolean cutsceneManagerPresent;
        private boolean introFrameSnapshotPresent;
        private boolean fileMenuFramePresent;

        public boolean hasTilemap() {
            return tilemapPresent;
        }

        public boolean hasAttrmap() {
            return attrmapPresent;
        }

        public boolean hasBgPalettes() {
            return bgPalettesPresent;
        }

        public boolean hasObjPalettes() {
            return objPalettesPresent;
        }

        public boolean hasCutsceneManager() {
            return cutsceneManagerPresent;
        }

        public boolean hasIntroFrameSnapshot() {
            return introFrameSnapshotPresent;
        }

        public boolean hasFileMenuFrame() {
            return fileMenuFramePresent;
        }
    }

    private RenderScreen screen = RenderScreen.TITLE;

    private int frameCounter;

    final RoomInputs roomInputs = new RoomInputs();

    @Override
    public RenderScreen screen() {
        return screen;
    }

    @Override
    public int[] tilemap() {
        return background.tilemap;
    }

    @Override
    public int[] attrmap() {
        return background.attrmap;
    }

    @Override
    public int[][] bgPalettes() {
        return background.bgPalettes;
    }

    @Override
    public int[][] objPalettes() {
        return background.objPalettes;
    }

    @Override
    public CutsceneManager cutsceneManager() {
        return background.cutsceneManager;
    }

    @Override
    public RoomRenderView room() {
        return overworld.room;
    }

    @Override
    public ScrollController scrollController() {
        return overworld.scrollController;
    }

    @Override
    public TransitionController transitionController() {
        return overworld.transitionController;
    }

    @Override
    public Link link() {
        return overworld.link;
    }

    @Override
    public TransientVfxSystem transientVfxSystem() {
        return overworld.transientVfxSystem;
    }

    @Override
    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return overworld.cutLeavesEffectRenderer;
    }

    @Override
    public int[] transientVfxPalette() {
        return overworld.transientVfxPalette;
    }

    @Override
    public DroppableRupeeSystem droppableRupeeSystem() {
        return overworld.droppableRupeeSystem;
    }

    @Override
    public InventoryController inventoryController() {
        return overworld.inventoryController;
    }

    @Override
    public DialogController dialogController() {
        return overworld.dialogController;
    }

    @Override
    public int frameCounter() {
        return frameCounter;
    }

    @Override
    public IntroFrameSnapshot introFrameSnapshot() {
        return background.introFrameSnapshot;
    }

    @Override
    public FileMenuFrameSnapshot fileMenuFrame() {
        return background.fileMenuFrame;
    }

    public GameFrameState withScreen(RenderScreen screen) {
        this.screen = screen;
        return this;
    }

    public GameFrameState withBackground(int[] tilemap, int[] attrmap, int[][] bgPalettes, int[][] objPalettes) {
        this.background.tilemap = tilemap;
        background.tilemapPresent = background.tilemap != null;
        this.background.attrmap = attrmap;
        background.attrmapPresent = background.attrmap != null;
        this.background.bgPalettes = bgPalettes;
        background.bgPalettesPresent = background.bgPalettes != null;
        this.background.objPalettes = objPalettes;
        background.objPalettesPresent = background.objPalettes != null;
        return this;
    }

    public GameFrameState withCutsceneManager(CutsceneManager cutsceneManager) {
        this.background.cutsceneManager = cutsceneManager;
        background.cutsceneManagerPresent = background.cutsceneManager != null;
        return this;
    }

    public GameFrameState withRoom(
            RoomRenderView room, ScrollController scrollController, TransitionController transitionController) {
        this.overworld.room = room;
        overworld.roomPresent = overworld.room != null;
        this.overworld.scrollController = scrollController;
        overworld.scrollControllerPresent = overworld.scrollController != null;
        this.overworld.transitionController = transitionController;
        overworld.transitionControllerPresent = overworld.transitionController != null;
        roomInputs.capture(overworld.room);
        return this;
    }

    public GameFrameState withLink(Link link) {
        this.overworld.link = link;
        overworld.linkPresent = overworld.link != null;
        return this;
    }

    public GameFrameState withTransientVfx(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            int[] transientVfxPalette) {
        this.overworld.transientVfxSystem = transientVfxSystem;
        overworld.transientVfxSystemPresent = overworld.transientVfxSystem != null;
        this.overworld.cutLeavesEffectRenderer = cutLeavesEffectRenderer;
        overworld.cutLeavesEffectRendererPresent = overworld.cutLeavesEffectRenderer != null;
        this.overworld.transientVfxPalette = transientVfxPalette;
        overworld.transientVfxPalettePresent = overworld.transientVfxPalette != null;
        return this;
    }

    public GameFrameState withDroppableRupees(DroppableRupeeSystem droppableRupeeSystem) {
        this.overworld.droppableRupeeSystem = droppableRupeeSystem;
        overworld.droppableRupeeSystemPresent = overworld.droppableRupeeSystem != null;
        return this;
    }

    public GameFrameState withInventoryController(InventoryController inventoryController) {
        this.overworld.inventoryController = inventoryController;
        overworld.inventoryControllerPresent = overworld.inventoryController != null;
        return this;
    }

    public GameFrameState withDialogController(DialogController dialogController) {
        this.overworld.dialogController = dialogController;
        overworld.dialogControllerPresent = overworld.dialogController != null;
        return this;
    }

    public GameFrameState withFrameCounter(int frameCounter) {
        this.frameCounter = frameCounter & 0xFF;
        return this;
    }

    public GameFrameState withIntroFrameSnapshot(IntroFrameSnapshot introFrameSnapshot) {
        this.background.introFrameSnapshot = introFrameSnapshot;
        background.introFrameSnapshotPresent = background.introFrameSnapshot != null;
        return this;
    }

    public GameFrameState withFileMenuFrame(FileMenuFrameSnapshot fileMenuFrame) {
        this.background.fileMenuFrame = fileMenuFrame;
        background.fileMenuFramePresent = background.fileMenuFrame != null;
        return this;
    }

    /** Restores empty-input defaults without allocation before a complete refill. */
    public GameFrameState reset() {
        screen = RenderScreen.TITLE;
        background.tilemap = null;
        background.tilemapPresent = false;
        background.attrmap = null;
        background.attrmapPresent = false;
        background.bgPalettes = null;
        background.bgPalettesPresent = false;
        background.objPalettes = null;
        background.objPalettesPresent = false;
        background.cutsceneManager = null;
        background.cutsceneManagerPresent = false;
        overworld.room = null;
        overworld.roomPresent = false;
        overworld.scrollController = null;
        overworld.scrollControllerPresent = false;
        overworld.transitionController = null;
        overworld.transitionControllerPresent = false;
        overworld.link = null;
        overworld.linkPresent = false;
        overworld.transientVfxSystem = null;
        overworld.transientVfxSystemPresent = false;
        overworld.cutLeavesEffectRenderer = null;
        overworld.cutLeavesEffectRendererPresent = false;
        overworld.transientVfxPalette = null;
        overworld.transientVfxPalettePresent = false;
        overworld.droppableRupeeSystem = null;
        overworld.droppableRupeeSystemPresent = false;
        overworld.inventoryController = null;
        overworld.inventoryControllerPresent = false;
        overworld.dialogController = null;
        overworld.dialogControllerPresent = false;
        frameCounter = 0;
        background.introFrameSnapshot = null;
        background.introFrameSnapshotPresent = false;
        background.fileMenuFrame = null;
        background.fileMenuFramePresent = false;
        roomInputs.capture(null);
        return this;
    }

    public GameFrameState copyFrom(GameFrameState source) {
        withScreen(source.screen())
                .withBackground(source.tilemap(), source.attrmap(), source.bgPalettes(), source.objPalettes())
                .withCutsceneManager(source.cutsceneManager())
                .withRoom(source.room(), source.scrollController(), source.transitionController())
                .withLink(source.link())
                .withTransientVfx(
                        source.transientVfxSystem(), source.cutLeavesEffectRenderer(), source.transientVfxPalette())
                .withDroppableRupees(source.droppableRupeeSystem())
                .withInventoryController(source.inventoryController())
                .withDialogController(source.dialogController())
                .withFrameCounter(source.frameCounter())
                .withIntroFrameSnapshot(source.introFrameSnapshot())
                .withFileMenuFrame(source.fileMenuFrame());
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
        overworld.room = null;
        overworld.roomPresent = overworld.room != null;
        roomInputs.set(tiles, attributes, palettes, entities, warp, heart, grassPalette);
        return this;
    }

    public GameFrameState withRoomControllers(ScrollController scroll, TransitionController transition) {
        overworld.scrollController = scroll;
        overworld.scrollControllerPresent = overworld.scrollController != null;
        overworld.transitionController = transition;
        overworld.transitionControllerPresent = overworld.transitionController != null;
        return this;
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof GameFrameState other)) return false;
        return Objects.equals(screen, other.screen)
                && Objects.equals(background.tilemap, other.background.tilemap)
                && Objects.equals(background.attrmap, other.background.attrmap)
                && Objects.equals(background.bgPalettes, other.background.bgPalettes)
                && Objects.equals(background.objPalettes, other.background.objPalettes)
                && Objects.equals(background.cutsceneManager, other.background.cutsceneManager)
                && Objects.equals(overworld.room, other.overworld.room)
                && Objects.equals(overworld.scrollController, other.overworld.scrollController)
                && Objects.equals(overworld.transitionController, other.overworld.transitionController)
                && Objects.equals(overworld.link, other.overworld.link)
                && Objects.equals(overworld.transientVfxSystem, other.overworld.transientVfxSystem)
                && Objects.equals(overworld.cutLeavesEffectRenderer, other.overworld.cutLeavesEffectRenderer)
                && Objects.equals(overworld.transientVfxPalette, other.overworld.transientVfxPalette)
                && Objects.equals(overworld.droppableRupeeSystem, other.overworld.droppableRupeeSystem)
                && Objects.equals(overworld.inventoryController, other.overworld.inventoryController)
                && Objects.equals(overworld.dialogController, other.overworld.dialogController)
                && frameCounter == other.frameCounter
                && Objects.equals(background.introFrameSnapshot, other.background.introFrameSnapshot)
                && Objects.equals(background.fileMenuFrame, other.background.fileMenuFrame);
    }

    @Override
    public int hashCode() {
        return frameCounter;
    }
}
