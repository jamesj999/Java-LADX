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

/** Same-thread screen-discriminated inputs with persistent sections. Refill before build; captures use test-side FrameSnapshots. */
public final class GameFrameState {
    public GameFrameState() {}

    public static GameFrameState empty() {
        return new GameFrameState();
    }

    public final OverworldInputs overworld = new OverworldInputs();
    /** Always-present overworld section; presence denotes intentionally omitted presentation components. */
    public static final class OverworldInputs {
        private final Presence<RoomRenderView> room = new Presence<>();
        private final Presence<ScrollController> scrollController = new Presence<>();
        private final Presence<TransitionController> transitionController = new Presence<>();
        private final Presence<Link> link = new Presence<>();
        private final Presence<TransientVfxSystem> transientVfxSystem = new Presence<>();
        private final Presence<CutLeavesEffectRenderer> cutLeavesEffectRenderer = new Presence<>();
        private final Presence<int[]> transientVfxPalette = new Presence<>();
        private final Presence<DroppableRupeeSystem> droppableRupeeSystem = new Presence<>();
        private final Presence<InventoryController> inventoryController = new Presence<>();
        private final Presence<DialogController> dialogController = new Presence<>();

        public boolean hasRoom() {
            return room.present;
        }

        public boolean hasScrollController() {
            return scrollController.present;
        }

        public boolean hasTransitionController() {
            return transitionController.present;
        }

        public boolean hasLink() {
            return link.present;
        }

        public boolean hasTransientVfxSystem() {
            return transientVfxSystem.present;
        }

        public boolean hasCutLeavesEffectRenderer() {
            return cutLeavesEffectRenderer.present;
        }

        public boolean hasTransientVfxPalette() {
            return transientVfxPalette.present;
        }

        public boolean hasDroppableRupeeSystem() {
            return droppableRupeeSystem.present;
        }

        public boolean hasInventoryController() {
            return inventoryController.present;
        }

        public boolean hasDialogController() {
            return dialogController.present;
        }
    }

    public final BackgroundInputs background = new BackgroundInputs();
    /** Always-present background section; presence denotes intentionally omitted presentation components. */
    public static final class BackgroundInputs {
        private final Presence<int[]> tilemap = new Presence<>();
        private final Presence<int[]> attrmap = new Presence<>();
        private final Presence<int[][]> bgPalettes = new Presence<>();
        private final Presence<int[][]> objPalettes = new Presence<>();
        private final Presence<CutsceneManager> cutsceneManager = new Presence<>();
        private final Presence<IntroFrameSnapshot> introFrameSnapshot = new Presence<>();
        private final Presence<FileMenuFrameSnapshot> fileMenuFrame = new Presence<>();

        public boolean hasTilemap() {
            return tilemap.present;
        }

        public boolean hasAttrmap() {
            return attrmap.present;
        }

        public boolean hasBgPalettes() {
            return bgPalettes.present;
        }

        public boolean hasObjPalettes() {
            return objPalettes.present;
        }

        public boolean hasCutsceneManager() {
            return cutsceneManager.present;
        }

        public boolean hasIntroFrameSnapshot() {
            return introFrameSnapshot.present;
        }

        public boolean hasFileMenuFrame() {
            return fileMenuFrame.present;
        }
    }

    private RenderScreen screen = RenderScreen.TITLE;

    private int frameCounter;

    final RoomInputs roomInputs = new RoomInputs();

    public RenderScreen screen() {
        return screen;
    }

    public int[] tilemap() {
        return background.tilemap.value;
    }

    public int[] attrmap() {
        return background.attrmap.value;
    }

    public int[][] bgPalettes() {
        return background.bgPalettes.value;
    }

    public int[][] objPalettes() {
        return background.objPalettes.value;
    }

    public CutsceneManager cutsceneManager() {
        return background.cutsceneManager.value;
    }

    public RoomRenderView room() {
        return overworld.room.value;
    }

    public ScrollController scrollController() {
        return overworld.scrollController.value;
    }

    public TransitionController transitionController() {
        return overworld.transitionController.value;
    }

    public Link link() {
        return overworld.link.value;
    }

    public TransientVfxSystem transientVfxSystem() {
        return overworld.transientVfxSystem.value;
    }

    public CutLeavesEffectRenderer cutLeavesEffectRenderer() {
        return overworld.cutLeavesEffectRenderer.value;
    }

    public int[] transientVfxPalette() {
        return overworld.transientVfxPalette.value;
    }

    public DroppableRupeeSystem droppableRupeeSystem() {
        return overworld.droppableRupeeSystem.value;
    }

    public InventoryController inventoryController() {
        return overworld.inventoryController.value;
    }

    public DialogController dialogController() {
        return overworld.dialogController.value;
    }

    public int frameCounter() {
        return frameCounter;
    }

    public IntroFrameSnapshot introFrameSnapshot() {
        return background.introFrameSnapshot.value;
    }

    public FileMenuFrameSnapshot fileMenuFrame() {
        return background.fileMenuFrame.value;
    }

    public GameFrameState withScreen(RenderScreen screen) {
        this.screen = screen;
        return this;
    }

    public GameFrameState withBackground(int[] tilemap, int[] attrmap, int[][] bgPalettes, int[][] objPalettes) {
        background.tilemap.bind(tilemap);
        background.attrmap.bind(attrmap);
        background.bgPalettes.bind(bgPalettes);
        background.objPalettes.bind(objPalettes);
        return this;
    }

    public GameFrameState withCutsceneManager(CutsceneManager cutsceneManager) {
        background.cutsceneManager.bind(cutsceneManager);
        return this;
    }

    public GameFrameState withRoom(
            RoomRenderView room, ScrollController scrollController, TransitionController transitionController) {
        overworld.room.bind(room);
        overworld.scrollController.bind(scrollController);
        overworld.transitionController.bind(transitionController);
        roomInputs.capture(overworld.room.value);
        return this;
    }

    public GameFrameState withLink(Link link) {
        overworld.link.bind(link);
        return this;
    }

    public GameFrameState withTransientVfx(
            TransientVfxSystem transientVfxSystem,
            CutLeavesEffectRenderer cutLeavesEffectRenderer,
            int[] transientVfxPalette) {
        overworld.transientVfxSystem.bind(transientVfxSystem);
        overworld.cutLeavesEffectRenderer.bind(cutLeavesEffectRenderer);
        overworld.transientVfxPalette.bind(transientVfxPalette);
        return this;
    }

    public GameFrameState withDroppableRupees(DroppableRupeeSystem droppableRupeeSystem) {
        overworld.droppableRupeeSystem.bind(droppableRupeeSystem);
        return this;
    }

    public GameFrameState withInventoryController(InventoryController inventoryController) {
        overworld.inventoryController.bind(inventoryController);
        return this;
    }

    public GameFrameState withDialogController(DialogController dialogController) {
        overworld.dialogController.bind(dialogController);
        return this;
    }

    public GameFrameState withFrameCounter(int frameCounter) {
        this.frameCounter = frameCounter & 0xFF;
        return this;
    }

    public GameFrameState withIntroFrameSnapshot(IntroFrameSnapshot introFrameSnapshot) {
        background.introFrameSnapshot.bind(introFrameSnapshot);
        return this;
    }

    public GameFrameState withFileMenuFrame(FileMenuFrameSnapshot fileMenuFrame) {
        background.fileMenuFrame.bind(fileMenuFrame);
        return this;
    }

    /** Restores empty-input defaults without allocation before a complete refill. */
    public GameFrameState reset() {
        screen = RenderScreen.TITLE;
        background.tilemap.bind(null);
        background.attrmap.bind(null);
        background.bgPalettes.bind(null);
        background.objPalettes.bind(null);
        background.cutsceneManager.bind(null);
        overworld.room.bind(null);
        overworld.scrollController.bind(null);
        overworld.transitionController.bind(null);
        overworld.link.bind(null);
        overworld.transientVfxSystem.bind(null);
        overworld.cutLeavesEffectRenderer.bind(null);
        overworld.transientVfxPalette.bind(null);
        overworld.droppableRupeeSystem.bind(null);
        overworld.inventoryController.bind(null);
        overworld.dialogController.bind(null);
        frameCounter = 0;
        background.introFrameSnapshot.bind(null);
        background.fileMenuFrame.bind(null);
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
        overworld.room.bind(null);
        roomInputs.set(tiles, attributes, palettes, entities, warp, heart, grassPalette);
        return this;
    }

    public GameFrameState withRoomControllers(ScrollController scroll, TransitionController transition) {
        overworld.scrollController.bind(scroll);
        overworld.transitionController.bind(transition);
        return this;
    }

    @Override
    public boolean equals(Object value) {
        if (!(value instanceof GameFrameState other)) return false;
        return Objects.equals(screen, other.screen)
                && (background.tilemap.value == other.background.tilemap.value)
                && (background.attrmap.value == other.background.attrmap.value)
                && (background.bgPalettes.value == other.background.bgPalettes.value)
                && (background.objPalettes.value == other.background.objPalettes.value)
                && Objects.equals(background.cutsceneManager.value, other.background.cutsceneManager.value)
                && Objects.equals(overworld.room.value, other.overworld.room.value)
                && Objects.equals(overworld.scrollController.value, other.overworld.scrollController.value)
                && Objects.equals(overworld.transitionController.value, other.overworld.transitionController.value)
                && Objects.equals(overworld.link.value, other.overworld.link.value)
                && Objects.equals(overworld.transientVfxSystem.value, other.overworld.transientVfxSystem.value)
                && Objects.equals(
                        overworld.cutLeavesEffectRenderer.value, other.overworld.cutLeavesEffectRenderer.value)
                && (overworld.transientVfxPalette.value == other.overworld.transientVfxPalette.value)
                && Objects.equals(overworld.droppableRupeeSystem.value, other.overworld.droppableRupeeSystem.value)
                && Objects.equals(overworld.inventoryController.value, other.overworld.inventoryController.value)
                && Objects.equals(overworld.dialogController.value, other.overworld.dialogController.value)
                && frameCounter == other.frameCounter
                && Objects.equals(background.introFrameSnapshot.value, other.background.introFrameSnapshot.value)
                && Objects.equals(background.fileMenuFrame.value, other.background.fileMenuFrame.value);
    }

    @Override
    public int hashCode() {
        return frameCounter;
    }
    /** Reusable explicit absence for intentionally omitted presentation values; never allocated per frame. */
    private static final class Presence<T> {
        private T value;
        private boolean present;

        void bind(T value) {
            this.value = value;
            present = value != null;
        }
    }
}
