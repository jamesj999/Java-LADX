package linksawakening.app.screen;

import linksawakening.render.RenderScreen;
import java.util.function.BiConsumer;

/** Gameplay screen lifecycle. Callbacks belong to its owning session. */
public final class GameplayScreen implements Screen {
    private final java.util.function.Supplier<linksawakening.ui.FileMenuFrameSnapshot> gameOver;
    private final Runnable tick;
    private final BiConsumer<Integer, Integer> keys;

    public GameplayScreen(Runnable tick, BiConsumer<Integer, Integer> keys) {
        this(tick, keys, () -> null);
    }

    public GameplayScreen(Runnable tick, BiConsumer<Integer, Integer> keys,
            java.util.function.Supplier<linksawakening.ui.FileMenuFrameSnapshot> gameOver) {
        this.gameOver = gameOver;
        this.tick = tick;
        this.keys = keys;
    }

    @Override public void tick() { tick.run(); }
    @Override public void onKey(int key, int action) { keys.accept(key, action); }
    @Override public linksawakening.render.GameFrameState renderState(linksawakening.render.GameFrameState state) {
        var snapshot = gameOver.get();
        return snapshot == null ? state : linksawakening.render.GameFrameState.empty()
            .withScreen(RenderScreen.FILE_SAVE).withFileMenuFrame(snapshot);
    }
    @Override public RenderScreen renderScreen() { return RenderScreen.OVERWORLD; }
}
