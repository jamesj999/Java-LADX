package linksawakening.app.screen;

import java.util.Arrays;
import java.util.Objects;
import linksawakening.render.GameFrameState;
import linksawakening.render.RenderScreen;

/**
 * Owns queued key edges and gameplay dispatch for one session. One instance is retained for the owning
 * session.
 */
public final class GameplayScreen implements Screen {
    private final GameplayScreenDriver driver;

    private int[] pendingKeys = new int[16];
    private int[] reusableKeys = new int[16];
    private int pendingCount;

    private boolean fileSaveRequested;

    public GameplayScreen(GameplayScreenDriver driver) {
        this.driver = Objects.requireNonNull(driver);
    }

    @Override
    public void tick() {
        // GLFW supplies separate events for a chord. The ROM checks the entire
        // held-button mask before dispatching gameplay ($00:0E46-0E82).
        if (driver.interceptFrameInput()) {
            pendingCount = 0;
            return;
        }
        int[] events = pendingKeys;
        int count = pendingCount;
        pendingKeys = reusableKeys == null ? new int[16] : reusableKeys;
        reusableKeys = null;
        pendingCount = 0;
        try {
            for (int i = 0; i < count; i += 2) driver.onKey(events[i], events[i + 1]);
        } finally {
            reusableKeys = events;
        }
        driver.tick();
    }

    @Override
    public void onKey(int key, int action) {
        if (pendingCount + 2 > pendingKeys.length) pendingKeys = Arrays.copyOf(pendingKeys, pendingKeys.length * 2);
        pendingKeys[pendingCount++] = key;
        pendingKeys[pendingCount++] = action;
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
        pendingCount = 0;
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
