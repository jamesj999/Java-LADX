package linksawakening.render;

/** Builds ordered scene snapshots while retaining session-local composition scratch. */
public final class GameFrameSceneBuilder {
    private final HardwareFrame frame = new HardwareFrame(true);

    private DialogWindowPainter dialogWindowPainter;
    /** Uses alternate owned glyphs/window submission; null restores ROM-backed dialog rendering. */
    void setDialogWindowPainter(DialogWindowPainter painter) {
        dialogWindowPainter = painter;
    }

    private FrameDrawObserver observer;
    private final WorldLayers reusable = new WorldLayers();
    private final FrameScene reusableScene = FrameScene.borrowed(reusable.layers, frame);

    /** Attaches optional inspection to scenes built by this session-local builder. */
    void setDrawObserver(FrameDrawObserver observer) {
        this.observer = observer;
    }

    /** Compatibility bridge for app callers already holding an immutable frame; new room snapshots retain defensive-copy cost. */
    public FrameScene buildReusable(GameFrameState state) {
        return build(state);
    }

    /** Refills a borrowed scene, valid until this builder's next build. Capture callers use immutable build(GameFrameState). */
    public FrameScene build(GameFrameState state) {
        reusable.fill(state, dialogWindowPainter);
        return reusableScene.observedBy(observer);
    }
}
