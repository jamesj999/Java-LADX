package linksawakening;

import linksawakening.render.RenderScreen;
import java.util.function.BiConsumer;

/** Gameplay screen lifecycle. Callbacks belong to its owning session. */
public final class GameplayScreen implements Screen {
    private final Runnable tick;
    private final BiConsumer<Integer, Integer> keys;

    public GameplayScreen(Runnable tick, BiConsumer<Integer, Integer> keys) {
        this.tick = tick;
        this.keys = keys;
    }

    @Override public void tick() { tick.run(); }
    @Override public void onKey(int key, int action) { keys.accept(key, action); }
    @Override public RenderScreen renderScreen() { return RenderScreen.OVERWORLD; }
}
