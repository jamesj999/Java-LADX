package linksawakening.render;

/** Builds ordered scene snapshots while retaining session-local composition scratch. */
public final class GameFrameSceneBuilder {
    private final HardwareFrame frame = new HardwareFrame(true);

    private DialogWindowPainter dialogWindowPainter;

    private FrameDrawObserver observer;
    private final WorldLayers world = new WorldLayers();
    private final FrameScene scene = FrameScene.borrowed(world.layers, frame);

    /** Refills a borrowed scene, valid until this builder's next build. Capture inputs are copied by test-side FrameSnapshots. */
    public FrameScene build(GameFrameState state) {
        world.fill(state, dialogWindowPainter);
        return scene.observedBy(observer);
    }
    /** Uses alternate owned glyphs/window submission; null restores ROM-backed dialog rendering. */
    void setDialogWindowPainter(DialogWindowPainter painter) {
        dialogWindowPainter = painter;
    }

    /** Attaches optional inspection to scenes built by this session-local builder. */
    void setDrawObserver(FrameDrawObserver observer) {
        this.observer = observer;
    }
}
