package linksawakening.app.screen;

import java.util.ArrayList;
import java.util.List;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;

/** Gameplay screen lifecycle. Callbacks belong to its owning session. */
public final class GameplayScreen implements Screen {
    private final GameplayScreenDriver driver;

    private record KeyEvent(int key, int action) {}

    private final List<KeyEvent> pendingKeys = new ArrayList<>();
    private boolean fileSaveRequested;

    public GameplayScreen(GameplayScreenDriver driver) {
        this.driver = java.util.Objects.requireNonNull(driver);
    }

    @Override
    public void tick() {
        // GLFW supplies separate events for a chord. The ROM checks the entire
        // held-button mask before dispatching gameplay ($00:0E46-0E82).
        if (driver.interceptFrameInput()) {
            pendingKeys.clear();
            return;
        }
        List<KeyEvent> events = List.copyOf(pendingKeys);
        pendingKeys.clear();
        for (KeyEvent event : events) driver.onKey(event.key(), event.action());
        driver.tick();
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
        var snapshot = driver.gameOver();
        return snapshot == null
                ? state
                : GameFrameState.empty().withScreen(RenderScreen.FILE_SAVE).withFileMenuFrame(snapshot);
    }

    @Override
    public RenderScreen renderScreen() {
        return RenderScreen.OVERWORLD;
    }
}
