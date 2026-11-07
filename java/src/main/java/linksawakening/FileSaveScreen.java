package linksawakening;

import linksawakening.render.RenderScreen;
import java.util.function.BiConsumer;

/** FileSave screen lifecycle. Callbacks belong to its owning session. */
public final class FileSaveScreen implements Screen {
    private final Runnable tick;
    private final BiConsumer<Integer, Integer> keys;

    public FileSaveScreen(Runnable tick, BiConsumer<Integer, Integer> keys) {
        this.tick = tick;
        this.keys = keys;
    }

    @Override public void tick() { tick.run(); }
    @Override public void onKey(int key, int action) { keys.accept(key, action); }
    @Override public RenderScreen renderScreen() { return RenderScreen.FILE_SAVE; }
}
