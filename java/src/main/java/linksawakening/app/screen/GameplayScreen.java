package linksawakening.app.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;
import linksawakening.ui.FileMenuFrameSnapshot;

/** Gameplay screen lifecycle. Callbacks belong to its owning session. */
public final class GameplayScreen implements Screen {
    private final Supplier<FileMenuFrameSnapshot> gameOver;
    private final Runnable tick;
    private final BiConsumer<Integer, Integer> keys;
    private final BooleanSupplier interceptFrameInput;

    private record KeyEvent(int key, int action) {}

    private final List<KeyEvent> pendingKeys = new ArrayList<>();
    private boolean fileSaveRequested;

    public GameplayScreen(Runnable tick, BiConsumer<Integer, Integer> keys) {
        this(tick, keys, () -> null);
    }

    public GameplayScreen(Runnable tick, BiConsumer<Integer, Integer> keys, Supplier<FileMenuFrameSnapshot> gameOver) {
        this(tick, keys, gameOver, () -> false);
    }

    public GameplayScreen(
            Runnable tick,
            BiConsumer<Integer, Integer> keys,
            Supplier<FileMenuFrameSnapshot> gameOver,
            BooleanSupplier interceptFrameInput) {
        this.interceptFrameInput = interceptFrameInput;
        this.gameOver = gameOver;
        this.tick = tick;
        this.keys = keys;
    }

    @Override
    public void tick() {
        // GLFW supplies separate events for a chord. The ROM checks the entire
        // held-button mask before dispatching gameplay ($00:0E46-0E82).
        if (interceptFrameInput.getAsBoolean()) {
            pendingKeys.clear();
            return;
        }
        List<KeyEvent> events = List.copyOf(pendingKeys);
        pendingKeys.clear();
        for (KeyEvent event : events) keys.accept(event.key(), event.action());
        tick.run();
    }

    @Override
    public void onKey(int key, int action) {
        pendingKeys.add(new KeyEvent(key, action));
    }

    public void requestFileSave() {
        fileSaveRequested = true;
    }

    public boolean consumeFileSaveRequest() {
        boolean requested = fileSaveRequested;
        fileSaveRequested = false;
        return requested;
    }

    public void clearPendingInput() {
        pendingKeys.clear();
        fileSaveRequested = false;
    }

    @Override
    public GameFrameState renderState(GameFrameState state) {
        var snapshot = gameOver.get();
        return snapshot == null
                ? state
                : GameFrameState.empty().withScreen(RenderScreen.FILE_SAVE).withFileMenuFrame(snapshot);
    }

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.OVERWORLD;
    }
}
