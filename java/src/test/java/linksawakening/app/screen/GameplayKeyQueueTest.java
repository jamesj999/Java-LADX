package linksawakening.app.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.render.GameFrameState;
import org.junit.jupiter.api.Test;

class GameplayKeyQueueTest {
    @Test
    void queuedEdgesSurviveGrowthAndNewEdgesWaitUntilNextFrame() {
        var driver = new RecordingDriver();
        var screen = new GameplayScreen(driver);
        driver.screen = screen;
        for (int i = 0; i < 40; i++) screen.onKey(i, i & 1);
        screen.tick();
        assertEquals(40, driver.keys.size());
        for (int i = 0; i < 40; i++) assertEquals(i + ":" + (i & 1), driver.keys.get(i));
        screen.tick();
        assertEquals("99:1", driver.keys.get(40));
        screen.clearPendingInput();
        screen.tick();
        assertEquals(41, driver.keys.size());
    }

    @Test
    void saveInterceptionDiscardsPendingEdgesBeforeDispatch() {
        var driver = new RecordingDriver();
        var screen = new GameplayScreen(driver);
        driver.screen = screen;
        screen.onKey(7, 1);
        driver.intercept = true;
        screen.tick();
        driver.intercept = false;
        screen.tick();
        assertTrue(driver.keys.isEmpty());
        assertEquals(1, driver.ticks);
    }

    private static final class RecordingDriver implements GameplayScreenDriver {
        private GameplayScreen screen;
        private final List<String> keys = new ArrayList<>();
        private boolean intercept;
        private int ticks;

        @Override
        public boolean interceptFrameInput() {
            return intercept;
        }

        @Override
        public void onKey(int key, int action) {
            keys.add(key + ":" + action);
            if (key == 0) screen.onKey(99, 1);
        }

        @Override
        public void tick() {
            ticks++;
        }

        @Override
        public GameFrameState renderState(GameFrameState state) {
            return state;
        }
    }
}
